"""把长文本切成可检索的 chunk。"""

from typing import Any

MAX_CHARS = 600
OVERLAP_LINES = 2


def chunk_text(
    text: str,
    max_chars: int = MAX_CHARS,
    overlap_lines: int = OVERLAP_LINES,#相邻 chunk 之间重叠的行数
) -> list[dict[str, Any]]:
    """按行切块，块之间重叠 2 行，保证跨段的意思不被切断。"""
    if not text or not text.strip():
        return []

    lines = text.splitlines()
    chunks: list[dict[str, Any]] = []
    start = 0

    while start < len(lines):
        end = start
        size = 0
        while end < len(lines) and size < max_chars:
            size += len(lines[end]) + 1
            end += 1

        if end == start:
            end = start + 1

        content = "\n".join(lines[start:end]).strip()
        if content:
            chunks.append({
                "chunk_index": len(chunks),
                "content": content,
                "start_line": start + 1,
                "end_line": end,
            })

        if end >= len(lines):
            break
        start = max(start + 1, end - overlap_lines)

    return chunks


def build_embedding_text(title: str, source_uri: str, content: str) -> str:
    """只用于向量化，不落库。加上文件头，中文问题才容易和「这是哪个文件」对上。"""
    return f"文件: {source_uri}\n标题: {title}\n\n{content}"
