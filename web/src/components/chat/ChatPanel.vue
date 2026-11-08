<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useChat } from '@/composables/useChat'
import AppIcon from '@/components/ui/AppIcon.vue'
import MessageBubble from './MessageBubble.vue'
import ChatInput from './ChatInput.vue'
import SseTimeline from './SseTimeline.vue'
import CitationPanel from './CitationPanel.vue'

const props = defineProps<{
  projectId: number
  conversationId: number
  title?: string
}>()

const router = useRouter()

const {
  messages,
  timeline,
  citations,
  traceId,
  error,
  isStreaming,
  showCitations,
  send,
  stop,
} = useChat(props.projectId, props.conversationId)

const draft = ref('')
const scrollArea = ref<HTMLElement | null>(null)

const canRegenerate = computed(() =>
  messages.value.some((m) => m.role === 'assistant' && m.status === 'done'),
)

function scrollToBottom() {
  const el = scrollArea.value
  if (el) el.scrollTop = el.scrollHeight
}

watch(messages, scrollToBottom, { deep: true })

function onSend() {
  const content = draft.value
  if (!content.trim() || isStreaming.value) return
  draft.value = ''
  send(content)
}

function onRegenerate() {
  const lastUser = [...messages.value].reverse().find((m) => m.role === 'user')
  // 重新生成时不再追加一条用户消息，避免出现重复的 user 气泡。
  if (lastUser) send(lastUser.content, false)
}
</script>

<template>
  <div class="panel">
    <section class="main">
      <header class="main__header">
        <div>
          <h2 class="main__title">{{ title || '会话' }}</h2>
          <span v-if="traceId" class="main__trace">
            trace
            <a :href="`/traces/${traceId}`" @click.prevent="router.push(`/traces/${traceId}`)">
              {{ traceId }}
            </a>
          </span>
        </div>
      </header>

      <div ref="scrollArea" class="messages">
        <MessageBubble v-for="m in messages" :key="m.id" :message="m" />
      </div>

      <div class="input-wrap">
        <ChatInput
          v-model="draft"
          v-model:show-citations="showCitations"
          :is-streaming="isStreaming"
          :can-regenerate="canRegenerate"
          @send="onSend"
          @stop="stop"
          @regenerate="onRegenerate"
        />
      </div>
    </section>

    <aside class="right">
      <section class="right__section">
        <h4>执行时间线</h4>
        <SseTimeline :steps="timeline" />
      </section>
      <section class="right__section">
        <h4>引用来源</h4>
        <CitationPanel :citations="citations" />
      </section>
      <p v-if="error" class="right__error">
        <AppIcon name="alert-circle" :size="14" />
        {{ error }}
      </p>
    </aside>
  </div>
</template>

<style scoped>
.panel {
  flex: 1;
  min-width: 0;
  display: flex;
  height: 100%;
}
.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.main__header {
  padding: 16px 24px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}
.main__title {
  font-size: 16px;
}
.main__trace {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--foreground-muted);
  margin-top: 4px;
}
.main__trace a {
  color: var(--accent-strong);
}
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.input-wrap {
  padding: 16px 24px;
  border-top: 1px solid var(--border);
  flex-shrink: 0;
}
.right {
  width: var(--right-panel-width);
  border-left: 1px solid var(--border);
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
  flex-shrink: 0;
}
.right__section h4 {
  font-size: 13px;
  color: var(--foreground-secondary);
  margin-bottom: 12px;
}
.right__error {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--error);
}
</style>
