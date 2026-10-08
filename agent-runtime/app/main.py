"""
AgentForge Agent Runtime 启动入口。
作用类比：
    Spring Boot 的 @SpringBootApplication 启动类。
启动命令：
    uv run uvicorn app.main:app --reload --port 8000
"""

from fastapi import FastAPI

# 从 api 包中导入不同的路由模块。
# 一个 APIRouter 类似 Spring Boot 里的一个 @RestController。
from app.api.agent import router as agent_router
from app.api.health import router as health_router


# 创建 FastAPI 应用实例。
# app 这个变量名会被 uvicorn 使用：
#   uvicorn app.main:app
#                      ^^^
#                      这个 app 就是这里创建的 FastAPI 对象。
app = FastAPI(
    # 接口文档标题，会显示在 /docs 页面上。
    title="Forge Agent",
    # 版本号。
    version="0.1.0",
    # 项目描述，也会显示在 Swagger 文档中。
    description=(
        "AgentForge 的 Python Agent Runtime。"
        "负责 Router、ReAct Loop、工具调用、RAG、Memory、Sandbox 等能力。"
    ),
)


# 注册健康检查路由。
# 注册后，health_router 里面的接口会自动挂到 app 上。
app.include_router(health_router)

# 注册 Agent 执行路由。
# 注册后，agent_router 里面的接口会自动挂到 app 上。
app.include_router(agent_router)