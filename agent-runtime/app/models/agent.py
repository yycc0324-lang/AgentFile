"""
Agent 接口的请求 / 响应模型。
作用类比：
    Spring Boot 里的 DTO + @Valid 参数校验。

FastAPI 使用 Pydantic 模型自动完成：
    1. JSON 转 Python 对象
    2. 字段类型校验
    3. 缺失字段校验
    4. 自动生成 OpenAPI / Swagger 文档
"""

from pydantic import BaseModel, Field


class AgentRunRequest(BaseModel):
    """
    Agent 执行请求。

    Java 平台调用 Python /agent/run 时发送的 JSON 会映射到这个模型。
    """

    # task_id：Java 创建的一次 Agent 任务 ID。
    # 类型是 str，因为 Java 侧的 task ID 可能使用字符串传递。
    task_id: str = Field(
        description="Java 平台创建的 task ID",
        examples=["5001"],
    )

    # project_id：当前任务属于哪个项目。
    project_id: int = Field(
        description="项目 ID",
        examples=[1],
    )

    # conversation_id：当前任务属于哪个会话。
    conversation_id: int = Field(
        description="会话 ID",
        examples=[101],
    )

    # message_id：触发这个任务的用户消息 ID。
    # 后续 Java 会把它写入 task.trigger_message_id。
    message_id: int = Field(
        description="触发任务的用户消息 ID",
        examples=[1001],
    )

    # query：用户真正问的问题。
    query: str = Field(
        description="用户问题",
        examples=["订单服务的入口在哪里？"],
    )

    # show_citations：用户是否希望答案中展示引用文献。
    # 默认 True。
    show_citations: bool = Field(
        default=True,
        description="是否展示引用文献",
    )

    # stream：是否希望 Python 以流式方式返回。
    # M1 第一版可以先不真正实现流式。
    stream: bool = Field(
        default=True,
        description="是否流式返回",
    )


class Citation(BaseModel):
    """
    引用文献。

    当 Agent 从博客、代码库或爬虫内容中检索到答案时，
    会通过这个模型返回引用信息。
    """

    # 引用来源类型：
    # doc / code / url / crawl 等。
    source_type: str | None = Field(
        default=None,
        description="引用来源类型，例如 doc / code / url",
    )

    # 文件路径，例如：
    # src/main/java/com/example/OrderController.java
    file: str | None = Field(
        default=None,
        description="引用文件路径",
    )

    # 行号，代码引用时使用。
    line: int | None = Field(
        default=None,
        description="引用行号",
    )

    # 引用片段。
    snippet: str | None = Field(
        default=None,
        description="引用内容片段",
    )


class AgentRunResponse(BaseModel):
    """
    Agent 执行响应。

    Python 处理完用户问题后，返回给 Java 平台的结构。
    """

    # 最终回答内容。
    content: str = Field(
        description="Agent 最终回答",
    )

    # 引用列表。
    # default_factory=list 表示默认是空列表。
    citations: list[Citation] = Field(
        default_factory=list,
        description="引用文献列表",
    )

    # trace_id：这次 Agent 执行的调用链 ID。
    trace_id: str = Field(
        description="调用链 Trace ID",
    )

    # token 用量。
    # 例如：
    # {
    #     "prompt_tokens": 3200,
    #     "completion_tokens": 180
    # }
    usage: dict[str, int] = Field(
        default_factory=dict,
        description="token 用量",
    )