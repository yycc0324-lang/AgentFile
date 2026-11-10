"""数据接入：目录 / URL → 切块 → 向量化 → 写 Qdrant。"""

import hashlib
import uuid
from pathlib import Path

import httpx
from fastapi import APIRouter
from pydantic import BaseModel, Field
from qdrant_client.http import models as qm

from app.rag.chunker import build_embedding_text, chunk_text
from app.rag.embedder import embedder
from app.rag.store import store
from app.core.config import settings

router = APIRouter(prefix="/ingest", tags=["ingest"])


class IngestSource(BaseModel):
    type: str = Field(description="dir 或 url")
    uri: str


class IngestRequest(BaseModel):
    project_id: int
    sources: list[IngestSource]


class IngestChunk(BaseModel):
    chunk_id: str
    chunk_index: int
    content: str
    start_line: int | None = None
    end_line: int | None = None


class IngestDocument(BaseModel):
    title: str
    source_type: str
    source_uri: str
    content_type: str
    content_hash: str
    size_bytes: int
    chunks: list[IngestChunk]


class IngestResponse(BaseModel):
    documents: list[IngestDocument]


class SearchRequest(BaseModel):
    project_id: int
    query: str
    top_k: int = 5


@router.post("/run", response_model=IngestResponse, summary="执行数据接入")
def run_ingest(request: IngestRequest) -> IngestResponse:
    store.ensure_collection()

    documents: list[IngestDocument] = []
    for source in request.sources:
        if source.type == "dir":
            documents.extend(_load_dir(source.uri, request.project_id))
        elif source.type == "url":
            documents.extend(_load_url(source.uri, request.project_id))
        else:
            raise ValueError(f"不支持的来源类型: {source.type}")

    for doc in documents:
        _index_document(request.project_id, doc)

    return IngestResponse(documents=documents)


@router.post("/search", summary="调试用：按语义检索 chunk")
def search(request: SearchRequest) -> list[dict]:
    project_ids = [request.project_id]
    builtin = settings.builtin_project_id
    if builtin and builtin != request.project_id:#如果不在外置库就查内置
        project_ids.append(builtin)

    vector = embedder.embed([request.query])[0]
    hits = store.search(vector, project_ids, request.top_k)
    return [
        {
            "score": round(hit.score, 4),
            "source_uri": hit.payload.get("source_uri"),
            "start_line": hit.payload.get("start_line"),
            "end_line": hit.payload.get("end_line"),
            "content": (hit.payload.get("content") or "")[:200],
        }
        for hit in hits
    ]

# ------------------------------------------------------------------ 加载来源


def _load_dir(path: str, project_id: int) -> list[IngestDocument]:
    root = Path(path).expanduser().resolve()
    if not root.is_dir():
        return []

    documents: list[IngestDocument] = []
    for file in sorted(root.rglob("*.md")):
        text = file.read_text(encoding="utf-8", errors="ignore")
        documents.append(
            _build_document(
                title=file.stem,
                source_type="blog",
                source_uri=str(file),
                text=text,
                project_id=project_id,
            )
        )
    return documents


def _load_url(url: str, project_id: int) -> list[IngestDocument]:
    import trafilatura

    with httpx.Client(timeout=30, follow_redirects=True) as client:
        resp = client.get(url, headers={"User-Agent": "AgentForge/0.1"})
        resp.raise_for_status()
        html = resp.text

    text = trafilatura.extract(html, include_comments=False, include_tables=False) or ""
    return [
        _build_document(
            title=url,
            source_type="url",
            source_uri=url,
            text=text,
            project_id=project_id,
        )
    ]


def _build_document(
    title: str,
    source_type: str,
    source_uri: str,
    text: str,
    project_id: int,
) -> IngestDocument:
    content_hash = hashlib.sha256(text.encode("utf-8")).hexdigest()
    chunks = chunk_text(text)

    return IngestDocument(
        title=title,
        source_type=source_type,
        source_uri=source_uri,
        content_type="text/markdown",
        content_hash=content_hash,
        size_bytes=len(text.encode("utf-8")),
        chunks=[
            IngestChunk(
                # Qdrant 的 point id 必须是 UUID 或整数；带上 project_id，
                # 否则两个项目接入同一篇文章会生成同一个 id、互相覆盖。
                chunk_id=str(uuid.uuid5(
                    uuid.NAMESPACE_URL,
                    f"{project_id}:{content_hash}:{c['chunk_index']}",
                )),
                chunk_index=c["chunk_index"],
                content=c["content"],
                start_line=c["start_line"],
                end_line=c["end_line"],
            )
            for c in chunks
        ],
    )


def _index_document(project_id: int, doc: IngestDocument) -> None:
    if not doc.chunks:
        return

    # 同一项目下，同一个来源路径的旧向量先清掉。
    # 注意按 source_uri 删，不能按 document_hash：
    # 文章改了之后 hash 也变了，用新 hash 删不到旧点。
    store.client.delete(
        collection_name=store.collection,
        points_selector=qm.Filter(
            must=[
                qm.FieldCondition(key="project_id", match=qm.MatchValue(value=project_id)),
                qm.FieldCondition(key="source_uri", match=qm.MatchValue(value=doc.source_uri)),
            ]
        ),
    )

    texts = [
        build_embedding_text(doc.title, doc.source_uri, chunk.content)
        for chunk in doc.chunks
    ]
    vectors = embedder.embed(texts)

    points = [
        qm.PointStruct(
            id=chunk.chunk_id,
            vector=vector,
            payload={
                "project_id": project_id,
                "document_hash": doc.content_hash,
                "chunk_id": chunk.chunk_id,
                "source_type": doc.source_type,
                "source_uri": doc.source_uri,
                "title": doc.title,
                "content": chunk.content,
                "start_line": chunk.start_line,
                "end_line": chunk.end_line,
            },
        )
        for chunk, vector in zip(doc.chunks, vectors)
    ]
    store.upsert(points)
