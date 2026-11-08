import { ref, onMounted } from 'vue'
import { chatApi, messagesApi } from '@/api'
import { useSse, type SseEvent } from './useSse'
import type {
  ChatMessage,
  Citation,
  TimelineStep,
  SseRouteData,
  SseStepData,
  SseAnswerData,
  SseDoneData,
  SseErrorData,
} from '@/types'

export function useChat(projectId: number, conversationId: number) {
  const { isStreaming, connect, abort } = useSse()

  const messages = ref<ChatMessage[]>([])
  const timeline = ref<TimelineStep[]>([])
  const citations = ref<Citation[]>([])
  const traceId = ref<string | null>(null)
  const error = ref<string | null>(null)
  const showCitations = ref(true)

  let localId = 0
  const nextId = () => `local-${++localId}`

  async function loadHistory() {
    if (messages.value.length > 0) return

    try {
      const history = await messagesApi.listByConversation(conversationId)

      messages.value = history
        .filter((m) => m.role === 'user' || m.role === 'assistant')
        .map((m) => ({
          id: m.id,
          role: m.role as 'user' | 'assistant',
          content: m.content,
          status: (m.status === 'error' ? 'error' : 'done') as ChatMessage['status'],
          error: m.errorMessage,
          promptTokens: m.promptTokens,
          completionTokens: m.completionTokens,
          totalTokens: m.totalTokens,
          latencyMs: m.latencyMs,
        }))
    } catch (e) {
      error.value = (e as Error).message
    }
  }

  onMounted(loadHistory)

  function pushStep(step: TimelineStep) {
    timeline.value.push(step)
  }

  function upsertStep(step: TimelineStep) {
    const existing = timeline.value.find((s) => s.key === step.key)
    if (!existing) timeline.value.push(step)
  }

  function markStep(key: string, status: TimelineStep['status'], sublabel?: string) {
    const step = timeline.value.find((s) => s.key === key)
    if (step) {
      step.status = status
      if (sublabel) step.sublabel = sublabel
    }
  }

  function handleEvent(e: SseEvent) {
    switch (e.event) {
      case 'route': {
        const d = e.data as unknown as SseRouteData
        pushStep({
          key: 'route',
          event: 'route',
          label: 'route',
          sublabel: d.agent ?? 'agent',
          status: 'success',
        })
        break
      }
      case 'step': {
        const d = e.data as unknown as SseStepData
        const tool = d.tool ?? d.type ?? 'tool_call'
        const resultText = d.resultCount != null ? ` · ${d.resultCount} 条结果` : ''
        pushStep({
          key: `${tool}-${timeline.value.length}`,
          event: 'step',
          label: tool,
          sublabel: `${describeTool(tool)}${resultText}`,
          status: 'success',
        })
        break
      }
      case 'answer': {
        const d = e.data as unknown as SseAnswerData
        upsertStep({
          key: 'answer',
          event: 'answer',
          label: 'answer',
          sublabel: '生成回答',
          status: 'running',
        })
        markStep('answer', 'success', '生成回答')
        const last = messages.value[messages.value.length - 1]
        if (last && last.role === 'assistant' && last.status === 'streaming') {
          last.content = d.content
        }
        if (d.citations?.length) citations.value = d.citations
        break
      }
      case 'done': {
        const d = e.data as unknown as SseDoneData
        traceId.value = d.traceId ?? null
        upsertStep({
          key: 'done',
          event: 'done',
          label: 'done',
          sublabel: '完成',
          status: 'running',
        })
        markStep('done', 'success', `完成 · ${d.traceId ?? ''}`)
        const last = messages.value[messages.value.length - 1]
        if (last && last.role === 'assistant') {
          last.status = 'done'
          last.traceId = d.traceId
          if (d.usage) {
            last.promptTokens = d.usage.promptTokens
            last.completionTokens = d.usage.completionTokens
            last.totalTokens = d.usage.totalTokens
          }
        }
        break
      }
      case 'error': {
        const d = e.data as unknown as SseErrorData
        error.value = d.message || 'Agent 执行失败'
        const last = messages.value[messages.value.length - 1]
        if (last && last.role === 'assistant') {
          last.status = 'error'
          last.error = error.value
        }
        break
      }
    }
  }

  function describeTool(tool: string): string {
    const map: Record<string, string> = {
      search_code: '搜索代码',
      search_docs: '搜索文档',
      read_file: '读取文件',
      list_files: '列出文件',
      answer: '生成回答',
      web_search: '网络搜索',
      crawl: '抓取页面',
    }
    return map[tool] ?? tool
  }

  async function send(content: string, appendUserMessage = true) {
    if (!content.trim() || isStreaming.value) return
    error.value = null
    traceId.value = null
    citations.value = []
    timeline.value = []

    if (appendUserMessage) {
      messages.value.push({ id: nextId(), role: 'user', content, status: 'done' })
    }

    const assistantMsg: ChatMessage = {
      id: nextId(),
      role: 'assistant',
      content: '',
      status: 'streaming',
    }
    messages.value.push(assistantMsg)

    try {
      const response = await chatApi.stream({
        projectId,
        conversationId,
        message: content,
        showCitations: showCitations.value,
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }

      await connect(
        response,
        handleEvent,
        () => {
          if (assistantMsg.status === 'streaming') assistantMsg.status = 'done'
        },
        (err) => {
          error.value = err.message
          assistantMsg.status = 'error'
          assistantMsg.error = err.message
        },
      )
    } catch (err) {
      error.value = (err as Error).message
      assistantMsg.status = 'error'
      assistantMsg.error = (err as Error).message
    }
  }

  function stop() {
    abort()
    const last = messages.value[messages.value.length - 1]
    if (last && last.role === 'assistant' && last.status === 'streaming') {
      last.status = 'done'
    }
  }

  return {
    messages,
    timeline,
    citations,
    traceId,
    error,
    isStreaming,
    showCitations,
    send,
    stop,
  }
}
