"""
Agent 执行接口。

文件位置：
    agent-runtime/app/api/agent.py

作用类比：
    Spring Boot 里的 AgentController。

当前阶段：
    M1 规则版 Agent：
        Router -> search_code -> read_file -> 返回引用
"""

from fastapi import APIRouter

from app.agent.react_loop import run_react
from app.models.agent import AgentRunRequest, AgentRunResponse

router = APIRouter(prefix="/agent", tags=["agent"])


@router.post(
    "/run",
    response_model=AgentRunResponse,
    summary="执行 Agent",
    description="M1 规则版 Agent：代码仓库问答 + 工具调用 + 引用返回。",
)

#代码本质上这里才是重点，这里是agent接口，然后agent接口调用agent方法就是去调用agent/react_loop
def run_agent(request: AgentRunRequest) -> AgentRunResponse:
    """
    接收 Java 平台的任务请求，执行简单 ReAct 流程，返回结构化的答案。
    """
    result = run_react(request)#此处result定义好的return就是下方AgentRunResponse定义的接收，这个接收写在agent/router中,很优雅
    return AgentRunResponse(**result)

"""
假设有一个函数：


def say_hello(name, age):
    print(name, age)
有一个字典：


data = {
    "name": "张三",
    "age": 18
}
写法 A

say_hello(**data)
等价于：


say_hello(name="张三", age=18)
因为 **data 会把字典拆成关键字参数。


say_hello(data)
等价于：
say_hello({"name": "张三", "age": 18})
这里只传了一个参数，而且类型是字典，不是 name 和 age。
**data   -> 拆开，按名字匹配参数
data     -> 整个字典当成一个参数

"""