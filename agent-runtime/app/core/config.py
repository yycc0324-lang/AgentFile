"""
Agent Runtime 配置模块。

后续用途：
    读取根目录 .env 中的 LLM_API_KEY、EMBEDDING_API_KEY、
    QDRANT_URL 等配置。
"""

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """
    项目配置。

    pydantic-settings 会自动从环境变量和 .env 文件读取配置。
    """

    # env_file 指定读取哪些 .env 文件。
    # 顺序：后面的会覆盖前面的。
    model_config = SettingsConfigDict(
        env_file=("../.env", ".env"),
        env_file_encoding="utf-8",
        extra="ignore",
    )

    # LLM 配置。
    llm_api_key: str = ""
    llm_base_url: str = ""

    # Embedding 模型配置。
    embedding_api_key: str = ""
    embedding_base_url: str = ""

    # Qdrant 地址。
    qdrant_url: str = "http://127.0.0.1:6333"


# 全局配置对象。
# 其他模块可以直接：
#     from app.core.config import settings
settings = Settings()