"""
Router：当前项目只接入代码仓库，因此固定路由到 code_agent。

后续重新接入文档/博客时，再恢复 knowledge_agent 分支。
"""


def route(query: str) -> str:
    """当前 M1 代码版：所有问题都交给 code_agent。"""
    return "code_agent"
