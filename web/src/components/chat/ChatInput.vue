<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import AppIcon from '@/components/ui/AppIcon.vue'

const props = withDefaults(
  defineProps<{
    modelValue: string
    isStreaming?: boolean
    showCitations?: boolean
    canRegenerate?: boolean
    placeholder?: string
  }>(),
  { isStreaming: false, showCitations: true, canRegenerate: false, placeholder: '输入你的问题…' },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'update:showCitations', value: boolean): void
  (e: 'send'): void
  (e: 'stop'): void
  (e: 'regenerate'): void
}>()

const ta = ref<HTMLTextAreaElement | null>(null)

function resize() {
  const el = ta.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = `${Math.min(el.scrollHeight, 200)}px`
}

watch(
  () => props.modelValue,
  () => nextTick(resize),
  { immediate: true },
)

function onInput(e: Event) {
  emit('update:modelValue', (e.target as HTMLTextAreaElement).value)
  resize()
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
    e.preventDefault()
    if (!props.isStreaming && props.modelValue.trim()) emit('send')
  }
}
</script>

<template>
  <div class="chat-input">
    <textarea
      ref="ta"
      class="chat-input__area"
      :value="modelValue"
      :placeholder="placeholder"
      rows="1"
      @input="onInput"
      @keydown="onKeydown"
    />

    <div class="chat-input__bar">
      <div class="chat-input__left">
        <button
          class="chip"
          :class="{ 'chip--on': showCitations }"
          @click="emit('update:showCitations', !showCitations)"
        >
          <AppIcon name="link" :size="14" />
          <span>引用来源</span>
        </button>
        <button
          v-if="canRegenerate"
          class="chip"
          title="重新生成"
          @click="emit('regenerate')"
        >
          <AppIcon name="refresh-cw" :size="14" />
          <span>重新生成</span>
        </button>
      </div>

      <button
        v-if="isStreaming"
        class="chat-input__send chat-input__send--stop"
        title="停止"
        @click="emit('stop')"
      >
        <AppIcon name="square" :size="16" />
      </button>
      <button
        v-else
        class="chat-input__send"
        :disabled="!modelValue.trim()"
        title="发送"
        @click="emit('send')"
      >
        <AppIcon name="send" :size="16" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.chat-input {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-2xl);
  background: var(--surface-primary);
}
.chat-input:focus-within {
  border-color: var(--accent-primary);
}
.chat-input__area {
  border: none;
  outline: none;
  resize: none;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.6;
  color: var(--foreground-primary);
  background: transparent;
  max-height: 200px;
  overflow-y: auto;
}
.chat-input__area::placeholder {
  color: var(--foreground-muted);
}
.chat-input__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.chat-input__left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  background: var(--surface-secondary);
  color: var(--foreground-secondary);
  font-size: 12px;
}
.chip:hover {
  border-color: var(--border-strong);
}
.chip--on {
  background: var(--accent-tint);
  border-color: transparent;
  color: var(--accent-strong);
}
.chat-input__send {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: var(--radius-lg);
  background: var(--accent-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}
.chat-input__send:hover:not(:disabled) {
  background: var(--accent-strong);
}
.chat-input__send:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.chat-input__send--stop {
  background: var(--error);
}
</style>
