"""Qdrant 读写。"""

from qdrant_client import QdrantClient
from qdrant_client.http import models as qm

from app.core.config import settings


class VectorStore:
    def __init__(self) -> None:
        self.client = QdrantClient(url=settings.qdrant_url)
        self.collection = settings.qdrant_collection
        self.dim = settings.embedding_dim

    def ensure_collection(self) -> None:
        if not self.client.collection_exists(self.collection):
            self.client.create_collection(
                collection_name=self.collection,
                vectors_config=qm.VectorParams(
                    size=self.dim,
                    distance=qm.Distance.COSINE,
                ),
            )

    def upsert(self, points: list[qm.PointStruct]) -> None:
        if not points:
            return
        self.client.upsert(
            collection_name=self.collection,
            points=points,
            wait=True,
        )

    def search(
        self,
        vector: list[float],
        #project_id: int,
        project_ids: list[int], #现在接受内建知识库+外接知识库需要id->ids，list
        limit: int = 5,
    ) -> list:
        flt = qm.Filter(
            must=[
                qm.FieldCondition(
                    key="project_id",
                    match=qm.MatchAny(any=project_ids),
                )
            ]
        )

        # qdrant-client 新版本用 query_points，旧版本用 search
        if hasattr(self.client, "query_points"):
            result = self.client.query_points(
                collection_name=self.collection,
                query=vector,
                query_filter=flt,
                limit=limit,
                with_payload=True,
            )
            return result.points

        return self.client.search(
            collection_name=self.collection,
            query_vector=vector,
            query_filter=flt,
            limit=limit,
            with_payload=True,
        )


store = VectorStore()
