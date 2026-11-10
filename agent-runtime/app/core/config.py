"""
Agent Runtime 配置模块。

后续用途：
    读取根目录 .env 中的 LLM_API_KEY、EMBEDDING_API_KEY、
    QDRANT_URL 等配置。
"""

from pydantic_settings import BaseSettings, SettingsConfigDict
#BaseSettings：Pydantic 提供的配置基类，能自动从环境变量 / .env 文件读取数据，并按类型做校验

class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=("../.env", ".env"),
        env_file_encoding="utf-8",
        extra="ignore",
    )
    #内置知识库，0表示没有知识库
    builtin_project_id: int = 0

    # LLM
    llm_api_key: str = ""#同下
    llm_base_url: str = ""#这里basesetting读取env文件中的LLM_BASE_URL
    llm_model: str = "deepseek-chat"
    llm_timeout_seconds: int = 120

    # Agent
    agent_max_steps: int = 12

    # Embedding
    embedding_api_key: str = ""
    embedding_base_url: str = ""
    embedding_model: str = ""
    embedding_dim: int = 1024


    # Qdrant
    qdrant_url: str = "http://127.0.0.1:6333"
    qdrant_collection: str = "agentforge_chunks"


settings = Settings()