"""
当前 M1 代码版 Agent 执行流程。

当前只接入代码仓库：
    Router -> search_code -> read_file -> 返回代码引用
"""

import uuid

from app.agent.router import route
from app.tools.search_code import search_code
from app.tools.read_file import read_file


def run_react(request) -> dict:
    query = request.query
    agent = route(query)

    citations: list[dict] = []
    steps: list[dict] = []
    content = ""

    steps.append({
        "type": "route",
        "tool": agent,
        "args": {"query": query},
        "status": "success",
        "result_count": 1,
    })

    matches = search_code(query=query, repo_url=request.repo_url)

    steps.append({
        "type": "tool_call",
        "tool": "search_code",
        "args": {"query": query},
        "status": "success",
        "result_count": len(matches),
    })

    if matches:
        first = matches[0]
        start_line = max(1, first["line"] - 10)
        end_line = first["line"] + 20

        file_result = read_file(
            file_path=first["file"],
            repo_url=request.repo_url,
            start_line=start_line,
            end_line=end_line,
        )

        steps.append({
            "type": "tool_call",
            "tool": "read_file",
            "args": {
                "file": first["file"],
                "start_line": start_line,
                "end_line": end_line,
            },
            "status": "success",
            "result_count": 1,
        })

        citations.append({
            "source_type": "code",
            "file": first["file"],
            "line": first["line"],
            "snippet": first["snippet"],
        })
        content = (
            f"我在 {first['file']}:{first['line']} 找到了相关代码。\n\n"
            f"{file_result['content']}"
        )
    else:
        content = "没有在代码库中找到相关代码。"

    return {
        "agent": agent,
        "steps": steps,
        "content": content,
        "citations": citations,
        "trace_id": f"trace-{uuid.uuid4().hex[:12]}",
        "usage": {
            "prompt_tokens": 0,
            "completion_tokens": 0,
            "total_tokens": 0,
        },
    }
