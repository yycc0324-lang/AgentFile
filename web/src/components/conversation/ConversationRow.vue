<script setup lang="ts">
import AppIcon from '@/components/ui/AppIcon.vue'
import type { Conversation } from '@/types'

defineProps<{ conversation: Conversation }>()

const emit = defineEmits<{
  (e: 'select', conversation: Conversation): void
}>()
</script>

<template>
  <div class="row" @click="emit('select', conversation)">
    <div class="row__icon">
      <AppIcon name="message-square" :size="18" />
    </div>
    <div class="row__col">
      <span class="row__title">{{ conversation.title }}</span>
      <span class="row__preview">{{ conversation.preview || '暂无消息预览' }}</span>
    </div>
    <div class="row__right">
      <span class="row__time">{{ conversation.createdAt }}</span>
      <span class="row__count">{{ conversation.messageCount ?? 0 }} 条消息</span>
    </div>
  </div>
</template>

<style scoped>
.row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  cursor: pointer;
  transition: border-color 0.15s;
}
.row:hover {
  border-color: var(--border-strong);
}
.row__icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--surface-secondary);
  color: var(--foreground-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.row__col {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-width: 0;
}
.row__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--foreground-primary);
}
.row__preview {
  font-size: 13px;
  color: var(--foreground-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.row__right {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-end;
  flex-shrink: 0;
}
.row__time {
  font-size: 12px;
  color: var(--foreground-muted);
}
.row__count {
  font-size: 12px;
  color: var(--foreground-secondary);
}
</style>
