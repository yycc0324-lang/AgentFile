<script setup lang="ts">
import AppIcon from '@/components/ui/AppIcon.vue'
import type { Conversation } from '@/types'

defineProps<{
  conversation: Conversation
  active?: boolean
}>()

const emit = defineEmits<{
  (e: 'select', conversation: Conversation): void
}>()
</script>

<template>
  <button
    class="item"
    :class="{ 'item--active': active }"
    @click="emit('select', conversation)"
  >
    <AppIcon name="message-square" :size="18" />
    <div class="item__col">
      <span class="item__title">{{ conversation.title }}</span>
      <span class="item__meta">{{ conversation.createdAt }}</span>
    </div>
    <AppIcon name="chevron-right" :size="16" class="item__chevron" />
  </button>
</template>

<style scoped>
.item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: none;
  border-radius: 10px;
  background: transparent;
  width: 100%;
  color: var(--foreground-muted);
  text-align: left;
}
.item:hover {
  background: #eef0f3;
}
.item--active {
  background: var(--accent-tint);
  color: var(--accent-primary);
}
.item__col {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}
.item__title {
  font-size: 14px;
  color: var(--foreground-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.item__meta {
  font-size: 12px;
  color: var(--foreground-muted);
}
.item__chevron {
  color: var(--foreground-muted);
}
</style>
