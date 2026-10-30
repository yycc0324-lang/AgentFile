"""在代码仓库中按关键词搜索代码。"""

import re #正则表达式
from pathlib import Path #python操作文件的类
from typing import Any

SKIP_DIRS = {  #跳过部分，意思是扫描到这些尾赘直接SKIP就可以
    ".git", ".idea", "node_modules", ".venv", "__pycache__",
    "target", "dist", "build", ".mvn",
}

TEXT_SUFFIXES = { #只搜索后缀
    ".java", ".py", ".yml", ".yaml", ".xml",
    ".js", ".ts", ".tsx", ".sql", ".properties",
}


def _query_tokens(query: str) -> list[str]:
    """优先提取英文/数字标识符；没有时回退到原查询。例如："ProjectService 这个类的代码在哪里？就提取ProjectService"""
    tokens = re.findall(r"[A-Za-z0-9_]{2,}", query)
    #re.findall(r"[A-Za-z0-9_]{2,}", query)：连续 2 个以上英文、数字、下划线，就当成一个 token

    if tokens:
        return [token.lower() for token in tokens]
    return [query.lower()]


def search_code(query: str, repo_url: str | None, max_results: int = 5) -> list[dict[str, Any]]:
    """
    在 repo_url 指向的本地目录中搜索关键词。

    当前 M1 只支持本地路径；Git URL 克隆放到后面。
    """
    if not repo_url:
        return []

    root = Path(repo_url).expanduser().resolve()
    if not root.exists() or not root.is_dir():
        return []

    tokens = _query_tokens(query)
    results: list[dict[str, Any]] = []

    for path in root.rglob("*"):
        if not path.is_file():
            continue
        if any(part in SKIP_DIRS for part in path.parts):
            continue
        if path.suffix.lower() not in TEXT_SUFFIXES:
            continue

        try:
            lines = path.read_text(encoding="utf-8", errors="ignore").splitlines()
        except Exception:
            continue

        best: dict[str, Any] | None = None

        for index, line in enumerate(lines, start=1):
            lower = line.lower()
            if not any(token in lower for token in tokens):
                continue

            score = 1
            for token in tokens:
                if re.search(
                    rf"\b(class|interface|enum|def|function)\s+{re.escape(token)}\b",
                    line,
                    re.IGNORECASE,
                ):
                    # class ProjectService / interface ProjectService / def foo
                    score += 100
                elif f"{token}(" in lower:
                    # 方法调用或函数定义
                    score += 10
                elif line.lstrip().startswith(("import ", "from ")) and token in lower:
                    # import 命中优先级较低
                    score += 1

            start = max(1, index - 2)
            end = min(len(lines), index + 2)
            candidate = {
                "file": str(path.relative_to(root)),
                "line": index,
                "snippet": "\n".join(lines[start - 1:end]),
                "score": score,
            }

            if best is None or candidate["score"] > best["score"]:
                best = candidate

        if best is not None:
            results.append(best)

    results.sort(key=lambda item: item["score"], reverse=True)
    return results[:max_results]
