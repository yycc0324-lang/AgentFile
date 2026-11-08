import { http } from './http'
import type {
  Project,
  CreateProjectPayload,
  Conversation,
  CreateConversationPayload,
  ChatPayload,
  ChatResponse,
  Trace,
  DocumentItem,
  EvalDataset,
  EvalRun,
  MessageItem,
} from '@/types'

// ---------- Projects ----------
export const projectsApi = {
  list: () => http.get<Project[]>('/projects'),
  get: (id: number) => http.get<Project>(`/projects/${id}`),
  create: (payload: CreateProjectPayload) =>
    http.post<Project>('/projects', payload),
}

// ---------- Conversations ----------
export const conversationsApi = {
  listByProject: (projectId: number) =>
    http.get<Conversation[]>(`/projects/${projectId}/conversations`),
  create: (payload: CreateConversationPayload) =>
    http.post<Conversation>('/conversations', payload),
}

// ---------- Messages ----------
export const messagesApi = {
  listByConversation: (conversationId: number) =>
    http.get<MessageItem[]>(`/conversations/${conversationId}/messages`),
}

// ---------- Chat ----------
export const chatApi = {
  // Non-streaming chat
  send: (payload: ChatPayload) => http.post<ChatResponse>('/chat', payload),
  // Streaming chat: returns the raw fetch Response (SSE stream).
  stream: (payload: ChatPayload) => http.postRaw('/chat/stream', payload),
}

// ---------- Trace ----------
export const tracesApi = {
  get: (traceId: string) => http.get<Trace>(`/traces/${traceId}`),
  steps: (taskId: string) => http.get<unknown[]>(`/tasks/${taskId}/steps`),
}

// ---------- Ingest / Documents ----------
export const ingestApi = {
  ingest: (projectId: number) =>
    http.post<unknown>(`/projects/${projectId}/ingest`),
  documents: (projectId: number) =>
    http.get<DocumentItem[]>(`/projects/${projectId}/documents`),
}

// ---------- Evaluation ----------
export const evalApi = {
  datasets: () => http.get<EvalDataset[]>('/eval/datasets'),
  createDataset: (payload: Partial<EvalDataset>) =>
    http.post<EvalDataset>('/eval/datasets', payload),
  createRun: (payload: Record<string, unknown>) =>
    http.post<EvalRun>('/eval/runs', payload),
  getRun: (id: string) => http.get<EvalRun>(`/eval/runs/${id}`),
}
