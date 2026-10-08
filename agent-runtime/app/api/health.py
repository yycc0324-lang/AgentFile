from fastapi import APIRouter


#负责文档分为health类
router = APIRouter(tags=["health"])


@router.get("/health")
def health() -> dict:
    return {
        "status": "ok",
        "service": "agent-runtime",
    }