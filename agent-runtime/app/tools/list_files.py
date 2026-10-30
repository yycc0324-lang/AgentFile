"""列出目录下的文件。"""

from pathlib import Path
from typing import Any


def list_files(
    repo_url: str | None = None,
    max_depth: int = 2,
    max_entries: int = 100,
) -> list[dict[str, Any]]:
    """
    列出目录结构，限制深度和数量。
    """
    if not repo_url:
        return []

    root = Path(repo_url).expanduser().resolve()
    if not root.exists() or not root.is_dir():
        return []

    results: list[dict[str, Any]] = []

    for path in sorted(root.rglob("*")):
        if len(results) >= max_entries:
            break

        try:
            relative = path.relative_to(root)
        except ValueError:
            continue

        depth = len(relative.parts)
        if depth > max_depth:
            continue

        results.append({
            "name": str(relative),
            "type": "dir" if path.is_dir() else "file",
            "depth": depth,
        })

    return results