"""在 Markdown / 文档目录中按关键词搜索。"""

import re
from pathlib import Path
from typing import Any


def _matches(line: str, word: str) -> bool:
    """英文/数字词用单词边界匹配，避免 Agent 命中 User-Agent。"""
    if re.fullmatch(r"[A-Za-z0-9_]+", word):
        return re.search(
            rf"(?<![A-Za-z0-9_-]){re.escape(word)}(?![A-Za-z0-9_-])",
            line,
            re.IGNORECASE,
        ) is not None
    return word in line.lower()


def search_docs(query: str, blog_path: str | None, max_results: int = 5) -> list[dict[str, Any]]:
    """
    在 blog_path 目录下搜索 .md 文件。
    """
    if not blog_path:
        return []

    root = Path(blog_path).expanduser().resolve()
    if not root.exists() or not root.is_dir():
        return []

    words = [w.lower() for w in query.replace("？", " ").split() if len(w) >= 2]
    if not words:
        words = [query.lower()]

    results: list[dict[str, Any]] = []

    for path in root.rglob("*.md"):
        try:
            lines = path.read_text(encoding="utf-8", errors="ignore").splitlines()
        except Exception:
            continue

        for index, line in enumerate(lines, start=1):
            if any(_matches(line, word) for word in words):
                start = max(1, index - 2)
                end = min(len(lines), index + 2)
                snippet = "\n".join(lines[start - 1:end])
                results.append({
                    "file": str(path.relative_to(root)),
                    "line": index,
                    "snippet": snippet,
                })
                break

        if len(results) >= max_results:
            break

    return results