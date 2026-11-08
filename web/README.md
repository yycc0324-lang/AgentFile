# AgentForge Web（Vue 3 + Vite + TypeScript）

M1 工作台前端。本目录于 2026-11-08 从 `/Library/Py_RT/Agents/web` 复制并纳入本仓库。

## 运行

```bash
npm install
npm run dev        # http://localhost:5173
npm run build
```

端口与代理已按本项目配置：

| 项 | 值 |
|---|---|
| Vite | 5173 |
| `/api` 代理 | http://localhost:8080（Java platform-api） |

## 页面

| 路由 | 页面 | 依赖的后端接口 |
|---|---|---|
| `/projects` | 项目列表 / 新建 | `GET/POST /api/projects` |
| `/projects/:id` | 项目详情 | `GET /api/projects/{id}` |
| `/projects/:id/conversations` | 会话列表 | `GET/POST /api/conversations` |
| `/projects/:id/conversations/:cid` | 聊天（引用面板 + SSE 时间线） | `POST /api/chat`、`POST /api/chat/stream` |
| `/ingest` | 数据接入 | ❌ `/api/projects/{id}/ingest` 未实现 |
| `/documents` | 文档列表 | ❌ `/api/projects/{id}/documents` 未实现 |
| `/traces/:traceId` | Trace 详情 | `GET /api/traces/{traceId}`、`GET /api/tasks/{id}/steps` |
| `/eval/datasets` | 评估集 | ❌ `/api/eval/*` 未实现 |
| `/eval/runs/:runId` | 评估运行详情 | ❌ `/api/eval/*` 未实现 |

标注 ❌ 的页面属于 M1 未完成后端，打开会 404，不是前端问题。

## 结构

```text
src/
├── api/            # http 封装 + 各领域接口
├── components/     # chat / conversation / layout / project / trace / ui
├── composables/    # useChat.ts（发送与状态）、useSse.ts（SSE 解析）
├── router/
├── stores/         # Pinia
├── styles/
├── types/
└── views/          # 9 个页面
```
