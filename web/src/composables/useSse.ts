import { ref } from 'vue'
import type { SseEventType } from '@/types'

export interface SseEvent {
  event: SseEventType
  data: Record<string, unknown>
}

/**
 * Minimal Server-Sent Events parser over a fetch ReadableStream.
 *
 * Because `EventSource` only supports GET and our chat endpoint is POST,
 * we consume `response.body.getReader()` and parse the stream manually
 * (see 前端设计与接口文档.md §9).
 */
export function useSse() {
  const isStreaming = ref(false)
  let reader: ReadableStreamDefaultReader<Uint8Array> | null = null
  let buffer = ''

  function parseEvents(chunk: string): SseEvent[] {
    buffer += chunk
    const events: SseEvent[] = []
    // SSE events are separated by a blank line.
    const blocks = buffer.split(/\r?\n\r?\n/)
    buffer = blocks.pop() ?? ''

    for (const block of blocks) {
      let event: SseEventType = 'answer'
      let dataRaw = ''
      for (const line of block.split(/\r?\n/)) {
        if (line.startsWith('event:')) {
          event = line.slice(6).trim() as SseEventType
        } else if (line.startsWith('data:')) {
          dataRaw = line.slice(5).trim()
        }
      }
      if (!dataRaw) continue
      try {
        events.push({ event, data: JSON.parse(dataRaw) })
      } catch {
        // Ignore keep-alive / malformed lines.
      }
    }
    return events
  }

  async function connect(
    response: Response,
    onEvent: (e: SseEvent) => void,
    onDone?: () => void,
    onError?: (err: Error) => void,
  ) {
    if (!response.body) {
      onError?.(new Error('响应没有可读流'))
      return
    }
    isStreaming.value = true
    reader = response.body.getReader()
    const decoder = new TextDecoder()

    try {
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        const text = decoder.decode(value, { stream: true })
        for (const event of parseEvents(text)) {
          onEvent(event)
        }
      }
    } catch (err) {
      onError?.(err as Error)
    } finally {
      isStreaming.value = false
      reader = null
      onDone?.()
    }
  }

  function abort() {
    reader?.cancel().catch(() => {})
    reader = null
    isStreaming.value = false
  }

  return { isStreaming, connect, abort }
}
