"""
OpenAI 兼容的 LLM 客户端。

支持：
    OpenAI
    DeepSeek
    其他 OpenAI 兼容服务
"""

import httpx

from app.core.config import settings#env->config->llm


class LLMClient:
    def __init__(self) -> None:
        self.base_url = settings.llm_base_url.rstrip("/")
        self.api_key = settings.llm_api_key
        self.model = settings.llm_model
        self.timeout = settings.llm_timeout_seconds

    def is_configured(self) -> bool:#校验是否配置齐了
        return bool(self.api_key and self.base_url and self.model)

    def chat(
        self,
        messages: list[dict],
        tools: list[dict] | None = None,
        temperature: float = 0.2,
    ) -> dict:
        """
        调用 /chat/completions。
        """
        if not self.is_configured():
            raise RuntimeError(
                "LLM 未配置，请设置 LLM_API_KEY / LLM_BASE_URL / LLM_MODEL"
            )

        payload = {#发送给LLM的数据包
            "model": self.model,
            "messages": messages,
            "temperature": temperature,
        }

        if tools:#如果传递了tool参数意味着要使用工具，payload中添加tools和tool_choice，因为是字典类型所以追加是没有问题的
            payload["tools"] = tools
            payload["tool_choice"] = "auto"

        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
        }

        last_error: Exception | None = None

        for _ in range(2):
            try:
                with httpx.Client(timeout=self.timeout) as client:
                    response = client.post(
                        f"{self.base_url}/chat/completions",
                        headers=headers,
                        json=payload,
                    )
                    response.raise_for_status()
                    return response.json()
            except Exception as exc:
                last_error = exc

        raise RuntimeError(f"LLM 调用失败: {last_error}")


llm_client = LLMClient()