"""读取指定文件的指定行范围。"""

from pathlib import Path
from typing import Any


def read_file(
    file_path: str,
    repo_url: str | None = None,
    start_line: int = 1,
    end_line: int = 80,
) -> dict[str, Any]:
    """
    读取文件内容并带行号返回。
    """
    base = Path(repo_url or ".").expanduser().resolve()
    path = Path(file_path)

    if not path.is_absolute():
        path = (base / path).resolve()

    if not path.exists() or not path.is_file():
        return {
            "file": file_path,
            "start_line": start_line,
            "end_line": end_line,
            "content": "",
            "error": "文件不存在",
        }

    try:
        lines = path.read_text(encoding="utf-8", errors="ignore").splitlines()
    except Exception as exc:
        return {
            "file": file_path,
            "start_line": start_line,
            "end_line": end_line,
            "content": "",
            "error": str(exc),
        }

    start = max(1, start_line)
    end = min(len(lines), end_line)

    numbered = [
        f"{i:>4}: {lines[i - 1]}"
        for i in range(start, end + 1)
    ]

    return {
        "file": file_path,
        "start_line": start,
        "end_line": end,
        "content": "\n".join(numbered),
    }