"""
工具注册表。

M1 当前只注册代码相关工具：
    search_code
    read_file
    list_files
"""

from app.tools.search_code import search_code
from app.tools.read_file import read_file
from app.tools.list_files import list_files


TOOLS = {
    "search_code": {
        "description": "根据关键词搜索代码文件，返回文件、行号和片段",
        "func": search_code,
    },
    "read_file": {
        "description": "读取指定文件的指定行范围",
        "func": read_file,
    },
    "list_files": {
        "description": "列出目录下的文件",
        "func": list_files,
    },
}


def run_tool(name: str, **kwargs):
    """根据工具名执行工具。"""
    tool = TOOLS.get(name)
    if tool is None:
        raise ValueError(f"未知工具: {name}")
    return tool["func"](**kwargs)