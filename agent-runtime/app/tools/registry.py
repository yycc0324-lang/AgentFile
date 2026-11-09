"""
工具注册表。

M1 当前只注册代码相关工具：
    search_code
    read_file
    list_files
"""
import inspect #调用工具前，动态检查工具函数的签名，以确保传入的参数正确
from typing import Any#简化写法，直接AnyType
from app.tools.search_code import search_code
from app.tools.read_file import read_file
from app.tools.list_files import list_files


TOOLS: dict[str, dict[str, Any]] = {
    "search_code": {
        "description": "根据关键词搜索代码文件，返回文件、行号和上下文片段",
        "func": search_code,
    },
    "read_file": {
        "description": "读取指定文件的指定行范围，返回带行号的代码",
        "func": read_file,
    },
    "list_files": {
        "description": "列出目录下的文件和子目录，用于了解项目结构",
        "func": list_files,
    },
}

# 注意：这里没有 repo_url。它是服务端注入的运行上下文，不交给模型决定。
TOOL_SCHEMAS: list[dict[str, Any]] = [
    {
        "type": "function",
        "function": {
            "name": "search_code",
            "description": (
                "在代码仓库里按关键词搜索，返回最相关的若干 文件+行号+片段。"
                "适合定位类、方法、配置项的位置。参数用英文标识符命中率最高。"
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {
                        "type": "string",
                        "description": "搜索关键词，例如 ProjectService、Flyway、/api/chat",
                    },
                    "max_results": {
                        "type": "integer",
                        "description": "最多返回多少条，默认 5",
                    },
                },
                "required": ["query"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "read_file",
            "description": (
                "读取指定文件的指定行范围，返回带行号的文本。"
                "file_path 用 search_code 返回的相对路径；一般读命中行前后各 20 行。"
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "file_path": {
                        "type": "string",
                        "description": "相对仓库根目录的文件路径",
                    },
                    "start_line": {
                        "type": "integer",
                        "description": "起始行号，从 1 开始，默认 1",
                    },
                    "end_line": {
                        "type": "integer",
                        "description": "结束行号（含），默认 80",
                    },
                },
                "required": ["file_path"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "list_files",
            "description": "列出仓库目录结构，用于确认模块划分或找文件位置。",
            "parameters": {
                "type": "object",
                "properties": {
                    "max_depth": {
                        "type": "integer",
                        "description": "目录深度，默认 2",
                    },
                    "max_entries": {
                        "type": "integer",
                        "description": "最多返回多少项，默认 100",
                    },
                },
                "required": [],
            },
        },
    },
]
def run_tool(
    name: str,
    arguments: dict[str, Any] | None = None,#详情见上方
    repo_url: str | None = None, #不写死在这里
) -> Any:
    """按工具名执行工具。模型传什么都在这里洗一遍。"""
    tool = TOOLS.get(name)
    if tool is None:
        raise ValueError(f"未知工具: {name}")#报错吧，本质上只有在编译阶段会从产生这个error

    func = tool["func"]
    args = dict(arguments or {})

    # 模型偶尔会把 repo_url 塞进参数里，直接丢掉，统一用服务端注入的值
    args.pop("repo_url", None)
    args.pop("blog_path", None)

    if "repo_url" in inspect.signature(func).parameters:
        args["repo_url"] = repo_url

    return func(**args)