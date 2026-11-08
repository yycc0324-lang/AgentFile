// Unified backend response envelope (see 前端设计与接口文档.md §1.2)
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// ---------- Project ----------
export interface Project {
  id: number
  name: string
  description: string
  repoUrl: string
  createdAt: string
  conversationCount?: number
  documentCount?: number
  evalRunCount?: number
}

export interface CreateProjectPayload {
  name: string
  description: string
  repoUrl: string
}

// ---------- Conversation ----------
export interface Conversation {
  id: number
  projectId: number
  title: string
  createdAt: string
  messageCount?: number
  preview?: string
}

export interface CreateConversationPayload {
  projectId: number
  title: string
}

// ---------- Message history ----------
export interface MessageItem {
  id: number
  conversationId: number
  role: 'user' | 'assistant' | 'system' | 'tool'
  content: string
  model?: string
  promptTokens?: number
  completionTokens?: number
  totalTokens?: number
  latencyMs?: number
  status: 'success' | 'error'
  errorMessage?: string
  createdAt: string
}

// ---------- Chat ----------
export type SourceType = 'code' | 'doc' | 'url' | 'crawl'

export interface Citation {
  sourceType: SourceType
  file: string
  line?: number
  snippet: string
}

export type MessageRole = 'user' | 'assistant'
export type MessageStatus = 'pending' | 'streaming' | 'done' | 'error'

export interface ChatMessage {
  id: number | string
  role: MessageRole
  content: string
  citations?: Citation[]
  status?: MessageStatus
  traceId?: string
  promptTokens?: number
  completionTokens?: number
  totalTokens?: number
  latencyMs?: number
  error?: string
}

export interface ChatPayload {
  projectId: number
  conversationId: number
  message: string
  showCitations: boolean
}

export interface ChatResponse {
  userMessageId: number
  assistantMessageId: number
  content: string
  citations: Citation[]
  traceId: string
  promptTokens: number
  completionTokens: number
  totalTokens: number
  latencyMs: number
}

// ---------- SSE ----------
export type SseEventType = 'route' | 'step' | 'answer' | 'done' | 'error'

export interface SseRouteData {
  agent: string
}

export interface SseStepData {
  type: 'route' | 'tool_call' | 'read_file' | 'answer'
  tool?: string
  args?: Record<string, unknown>
  status?: string
  resultCount?: number
}

export interface SseErrorData {
  message: string
}

export interface SseAnswerData {
  content: string
  citations: Citation[]
}

export interface SseDoneData {
  traceId: string
  usage?: {
    promptTokens?: number
    completionTokens?: number
    totalTokens?: number
  }
}

export type StepStatus = 'pending' | 'running' | 'success' | 'failed'

export interface TimelineStep {
  key: string
  event: SseEventType
  label: string
  sublabel: string
  status: StepStatus
}

// ---------- Trace ----------
export type SpanType = 'llm' | 'tool' | 'agent'
export type SpanStatus = 'success' | 'failed' | 'running'

export interface Span {
  id: string
  name: string
  type: SpanType
  status: SpanStatus
  durationMs: number
  input?: unknown
  output?: unknown
  error?: string
  children?: Span[]
}

export interface Trace {
  traceId: string
  status: SpanStatus
  durationMs: number
  spanCount: number
  tokens: number
  spans: Span[]
}

// ---------- Documents / Ingest ----------
export type DocumentStatus = 'success' | 'processing' | 'failed'

export interface DocumentItem {
  id: number
  name: string
  type: string
  size: string
  status: DocumentStatus
  ingestedAt: string
}

// ---------- Evaluation ----------
export interface EvalDataset {
  id: number
  name: string
  description: string
  sampleCount: number
  createdAt: string
}

export interface EvalSample {
  question: string
  score: number
  status: 'passed' | 'needs_improvement'
}

export interface EvalRun {
  id: string
  datasetName: string
  model: string
  status: string
  createdAt: string
  metrics: Record<string, number>
  samples: EvalSample[]
}
