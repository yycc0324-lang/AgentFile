"""
Agent 执行接口。

文件位置：
    agent-runtime/app/api/agent.py

作用类比：
    Spring Boot 里的 AgentController。

当前阶段：
    M1 第一版，先返回 mock 数据。
    后续会在这里接入：
        Router
        ReAct Loop
        search_docs
        search_code
        read_file
        list_files
        RAG / Qdrant
        Trace
"""

import uuid

from fastapi import APIRouter

from app.models.agent import AgentRunRequest, AgentRunResponse, Citation


# 创建路由对象。
# prefix="/agent" 表示这个 router 下的接口统一以 /agent 开头。
# tags=["agent"] 表示 /docs 中这些接口归到 "agent" 分组。
router = APIRouter(prefix="/agent", tags=["agent"])


# @router.post 类似 Spring Boot 的 @PostMapping。
# 最终完整路径为：
#     /agent/run
@router.post(
    "/run",
    response_model=AgentRunResponse,#声明返回的数据格式必须为response_model
    summary="执行 Agent",
    description=(
        "M1 第一版先返回 mock 数据。"
        "后续会接入 Router、ReAct Loop、工具调用和 RAG。"
    ),
)
def run_agent(request: AgentRunRequest) -> AgentRunResponse:
    """
    Agent 执行入口。

    参数：
        request:
            FastAPI 会自动把请求体 JSON 转成 AgentRunRequest 对象。
            这相当于 Spring Boot 的 @RequestBody + @Valid。

    返回：
        AgentRunResponse:
            FastAPI 会自动把返回对象转成 JSON。
    """

    # uuid.uuid4() 生成一个随机 UUID。
    # .hex 转成十六进制字符串。
    # [:12] 取前 12 个字符，得到一个短 trace_id。
    #
    # 真实项目中，trace_id 通常由 Java 生成，或者 Python 生成后回传 Java。
    trace_id = f"trace-{uuid.uuid4().hex[:12]}"

    # 构造 mock 响应。
    # 现在还没有接任何模型或检索工具，只是为了验证：
    #     Java -> Python -> Java 的调用链路是通的。
    return AgentRunResponse(
        # 先把用户问题回显出来，证明 Python 收到了请求。
        content=(
            f"收到问题：{request.query}\n"
            "Agent Runtime 已启动。"
            "下一步会接入 Router、search_docs、search_code、"
            "read_file、list_files。"
        ),

        # 返回一条 mock 引用。
        citations=[
            Citation(
                source_type="system",
                file=None,
                line=None,
                snippet="M1 agent-runtime mock response",
            )
        ],

        # 返回 trace_id。
        trace_id=trace_id,

        # 当前没有真正调用模型，所以 token 都是 0。
        usage={
            "prompt_tokens": 0,
            "completion_tokens": 0,
        },
    )