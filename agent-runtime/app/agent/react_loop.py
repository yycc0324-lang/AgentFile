"""
当前 M1 代码版 Agent 执行流程。（完成）

当前只接入代码仓库：
    Router -> search_code -> read_file -> 返回代码引用

现在完成的是
M1 ReAct 循环：LLM 决定调用哪个工具，工具结果回填后继续推理。

    route
      └─> [LLM -> tool_calls -> 执行工具 -> role=tool 回填] * N
      └─> answer

没有工具调用时，模型的 content 就是最终答案。
"""

import uuid
import json
import time

from app.agent.router import route
from app.agent.prompts import build_system_prompt #提示词写到loop去简化Tools的写法
from app.clients.llm import llm_client
from app.core.config import settings
from app.tools.registry import TOOL_SCHEMAS, run_tool


# 单次工具返回太长会吃掉上下文，截断一下
MAX_TOOL_RESULT_CHARS = 4000
MAX_CITATIONS = 8

#定义：这里造出函数对象，所以和api中agent31行的关系中这里是first
def run_react(request) -> dict:#request 不是代码里 new 出来的，是 FastAPI 在收到请求时按类型注解自动构造的
    query = request.query#Pydantic把query默认必填所以可以不定义 了
    agent = route(query)#在M1阶段都默认调用一个root，不路由到其他Agent了
    trace_id = request.trace_id or f"trace-{uuid.uuid4().hex[:12]}"#链路追踪 ID
    show_citations = bool(request.show_citations)#是否返回引用来源，强制转 bool

    # 引用分两类收集：真读过的（read_evidence）和只是搜到的（search_hits）
    read_evidence: list[dict] = []
    search_hits: list[dict] = []
    steps: list[dict] = []#调用步骤

    if not llm_client.is_configured():
        raise RuntimeError(
            "LLM 未配置：请检查 .env 里的 LLM_API_KEY / LLM_BASE_URL / LLM_MODEL"
        )
    messages: list[dict] = [#符合Open AI的Agent对话流格式
        {"role": "system", "content": build_system_prompt(show_citations)},
        {"role": "user", "content": query},
    ]

    steps: list[dict] = [{
        "type": "route",
        "tool": agent,
        "args": {"query": query},
        "status": "success",
        "result_count": 1,
        "duration_ms": 0,
    }]
    #初始化累加器
    usage = {"prompt_tokens": 0, "completion_tokens": 0, "total_tokens": 0}
    content = ""
    started_at = time.perf_counter()

    for _ in range(max(1, settings.agent_max_steps)):
        response = llm_client.chat(messages, tools=TOOL_SCHEMAS)
        _add_usage(usage, response.get("usage"))

        choice = (response.get("choices") or [{}])[0]
        message = choice.get("message") or {}
        tool_calls = message.get("tool_calls") or []
        #防御式取值，任何一层缺失都用空 dict / 空 list 兜底，避免 KeyError。


        # 不再要工具 = 模型认为可以回答了
        if not tool_calls:
            content = (message.get("content") or "").strip()
            break

        # 把模型这一步的决策记进对话，下一步它才能看到自己调过什么
        messages.append({
            "role": "assistant",
            "content": message.get("content"),
            "tool_calls": tool_calls,
        })

        for call in tool_calls:#开始逐步调用执行工具
            function = call.get("function") or {}
            name = function.get("name") or ""
            arguments = _parse_arguments(function.get("arguments"))#模型给的参数是 JSON 字符串，_parse_arguments 负责解析成 dict（并处理解析失败的情况）

            tool_started = time.perf_counter()
            try:
                result = run_tool(name, arguments, request.repo_url)
                status, error = "success", None
            except Exception as exc:  # 工具挂了不该打断整个任务
                result, status, error = {"error": str(exc)}, "failed", str(exc)
            duration_ms = int((time.perf_counter() - tool_started) * 1000)

            steps.append({
                "type": "tool_call",
                "tool": name,
                "args": arguments,
                "status": status,
                "result_count": _result_count(result),
                "duration_ms": duration_ms,
            })

            if status == "success":
                if name == "read_file":
                    citation = _citation_from_read(arguments, result)
                    if citation:
                        read_evidence.append(citation)
                elif name == "search_code":
                    search_hits.extend(_citations_from_search(result))

            messages.append({
                "role": "tool",
                "tool_call_id": call.get("id"),
                "content": _clip(json.dumps(result, ensure_ascii=False)),
            })
    else:
        # for-else：循环跑满了还没 break
        content = "这个问题需要的步骤超过了单次执行上限，请拆得更具体一些再问。"

    if not content:
        content = "没有找到相关代码。"

    # 引用 = 读过的文件（每个文件一条）→ 不够 8 条再用搜索第一名补位
    citations = _finalize_citations(read_evidence, search_hits)

    usage["total_tokens"] = (
            usage["total_tokens"]
            or usage["prompt_tokens"] + usage["completion_tokens"]
    )#如果 API 没直接返回 total_tokens（或者为0），就用 prompt + completion 算出来

    steps.append({
        "type": "answer",
        "tool": "answer",
        "args": {"query": query},
        "status": "success",
        "result_count": len(citations),
        "duration_ms": int((time.perf_counter() - started_at) * 1000),
    })#最后一条 step 记录"生成答案"这一步。注意它的 duration_ms 是从 started_at 到现在的总耗时（整个请求的耗时），而不是单步耗时。result_count 是引用条数

    return {
        "agent": agent,
        "steps": steps,
        "content": content,
        "citations": citations if show_citations else [],
        "trace_id": trace_id,
        "usage": usage,
    }

# ------------------------------------------------------------------ helpers


def _parse_arguments(raw: str | None) -> dict:
    """模型给的 arguments 是 JSON 字符串，偶尔会坏掉。"""
    try:
        data = json.loads(raw or "{}")
        return data if isinstance(data, dict) else {}
    except (TypeError, ValueError):
        return {}


def _add_usage(total: dict, usage: dict | None) -> None:
    if not usage:
        return
    for key in ("prompt_tokens", "completion_tokens", "total_tokens"):
        total[key] += int(usage.get(key) or 0)


def _result_count(result) -> int:
    if isinstance(result, list):
        return len(result)
    if isinstance(result, dict):
        if result.get("error"):
            return 0
        return 1 if result.get("content") else 1
    return 0


def _citation_from_read(arguments: dict, result) -> dict | None:
    """模型真正读过的一段内容，才配叫引用。"""
    if not isinstance(result, dict) or not result.get("content"):
        return None
    return {
        "source_type": "code",
        "file": result.get("file") or arguments.get("file_path"),
        "line": result.get("start_line"),
        "snippet": _clip(result["content"], 500),
    }


def _citations_from_search(result) -> list[dict]:
    """搜索只贡献第 1 名，作为“没读文件就作答”时的兜底。"""
    if not isinstance(result, list):
        return []
    return [
        {
            "source_type": "code",
            "file": item.get("file"),
            "line": item.get("line"),
            "snippet": item.get("snippet", ""),
        }
        for item in result[:1]
    ]


def _finalize_citations(
    read_evidence: list[dict],
    search_hits: list[dict],
    limit: int = MAX_CITATIONS,
) -> list[dict]:
    """引用只认读过的文件：读过就只用读过的（同一文件一条）。
    一条都没读过时，才退回用搜索第一名兜底。"""
    if read_evidence:
        pool, cap = read_evidence, limit
    else:
        # 没读文件时搜索兜底，但最多 3 条，避免又变成搜索结果列表
        pool, cap = search_hits, min(limit, 3)

    merged: list[dict] = []
    seen_files: set[str] = set()

    for item in pool:
        file = item.get("file")
        if not file or file in seen_files:
            continue
        seen_files.add(file)
        merged.append(item)
        if len(merged) >= cap:
            break

    return merged


def _clip(text: str, limit: int = MAX_TOOL_RESULT_CHARS) -> str:
    if len(text) <= limit:
        return text
    return text[:limit] + "\n...(已截断)"