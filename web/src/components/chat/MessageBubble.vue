<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import type { ChatMessage } from '@/types'

const props = defineProps<{ message: ChatMessage }>()

const isUser = computed(() => props.message.role === 'user')
const isError = computed(() => props.message.status === 'error')

const citationCount = computed(() => props.message.citations?.length ?? 0)

const metaText = computed(() => {
  const parts: string[] = []
  if (props.message.totalTokens != null && props.message.totalTokens > 0) {
    parts.push(`${props.message.totalTokens} tokens`)
  }
  if (props.message.latencyMs != null) {
    parts.push(`${props.message.latencyMs}ms`)
  }
  return parts.join(' · ')
})
</script>

<template>
  <div class="bubble-wrap" :class="{ 'bubble-wrap--user': isUser }">
    <div
      class="bubble"
      :class="{
        'bubble--user': isUser,
        'bubble--assistant': !isUser,
        'bubble--error': isError,
      }"
    >
      <template v-if="isUser">
        {{ message.content }}
      </template>
      <template v-else>
        <div v-if="message.status === 'streaming' && !message.content" class="bubble__streaming">
          <span class="dot" />
          <span class="dot" />
          <span class="dot" />
          <span class="bubble__streaming-label">正在生成回答</span>
        </div>
        <p v-else class="bubble__text">{{ message.content || '...' }}</p>
      </template>
    </div>

    <div v-if="isError && message.error" class="bubble__error-hint">
      <AppIcon name="alert-circle" :size="14" />
      <span>{{ message.error }}</span>
    </div>

    <div v-if="citationCount > 0 && !isUser" class="bubble__cite">
      <AppIcon name="link" :size="14" />
      <span>{{ citationCount }} 条引用</span>
    </div>

    <div v-if="metaText && !isUser && message.status === 'done'" class="bubble__meta">
      {{ metaText }}
    </div>
  </div>
</template>

<style scoped>
.bubble-wrap {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  max-width: 70%;
}
.bubble-wrap--user {
  align-items: flex-end;
}
.bubble {
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.bubble--user {
  background: var(--accent-primary);
  color: var(--foreground-inverse);
  border-radius: 12px 12px 4px 12px;
}
.bubble--assistant {
  background: var(--surface-secondary);
  color: var(--foreground-primary);
  border-radius: 12px 12px 12px 4px;
}
.bubble--error {
  background: var(--error-bg);
  color: var(--error);
}
.bubble__text {
  margin: 0;
}
.bubble__streaming {
  display: flex;
  align-items: center;
  gap: 6px;
}
.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--accent-primary);
  animation: pulse 1.2s infinite;
}
.dot:nth-child(2) {
  animation-delay: 0.2s;
}
.dot:nth-child(3) {
  animation-delay: 0.4s;
}
.bubble__streaming-label {
  font-size: 13px;
  color: var(--foreground-secondary);
}
@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.3;
  }
}
.bubble__cite {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: var(--radius-full);
  background: var(--accent-tint);
  color: var(--accent-strong);
  font-size: 12px;
}
.bubble__error-hint {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--error);
}
.bubble__meta {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--foreground-muted);
}
</style>
