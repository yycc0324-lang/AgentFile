"""调用 OpenAI 兼容的 /embeddings 接口（硅基流动 BGE-M3）。"""

import httpx

from app.core.config import settings

BATCH_SIZE = 16


class Embedder:
    def __init__(self) -> None:
        self.base_url = (settings.embedding_base_url or "").rstrip("/")
        self.api_key = settings.embedding_api_key
        self.model = settings.embedding_model
        self.dim = settings.embedding_dim

    def is_configured(self) -> bool:
        return bool(self.api_key and self.base_url and self.model)

    def embed(self, texts: list[str]) -> list[list[float]]:
        if not texts:
            return []
        if not self.is_configured():
            raise RuntimeError(
                "Embedding 未配置：请检查 EMBEDDING_API_KEY / EMBEDDING_BASE_URL / EMBEDDING_MODEL"
            )

        vectors: list[list[float]] = []
        with httpx.Client(timeout=60) as client:
            for i in range(0, len(texts), BATCH_SIZE):
                batch = texts[i:i + BATCH_SIZE]
                resp = client.post(
                    f"{self.base_url}/embeddings",
                    headers={"Authorization": f"Bearer {self.api_key}"},
                    json={
                        "model": self.model,
                        "input": batch,
                        "encoding_format": "float",
                    },
                )
                resp.raise_for_status()
                data = sorted(resp.json()["data"], key=lambda d: d["index"])
                vectors.extend(d["embedding"] for d in data)

        if vectors and len(vectors[0]) != self.dim:
            raise RuntimeError(
                f"Embedding 维度不一致：期望 {self.dim}，实际 {len(vectors[0])}"
            )
        return vectors


embedder = Embedder()
