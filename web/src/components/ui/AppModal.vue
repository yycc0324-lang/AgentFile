<script setup lang="ts">
import AppIcon from './AppIcon.vue'

defineProps<{
  open: boolean
  title: string
  width?: number
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="open" class="overlay" @click.self="emit('close')">
        <div class="dialog" :style="{ width: `${width ?? 520}px` }">
          <header class="dialog__header">
            <h3>{{ title }}</h3>
            <button class="dialog__close" aria-label="关闭" @click="emit('close')">
              <AppIcon name="x" :size="20" />
            </button>
          </header>
          <div class="dialog__body">
            <slot />
          </div>
          <footer v-if="$slots.footer" class="dialog__footer">
            <slot name="footer" />
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(10, 10, 10, 0.4);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 140px;
  z-index: 1000;
}
.dialog {
  background: var(--surface-primary);
  border-radius: var(--radius-2xl);
  box-shadow: var(--shadow-md);
  padding: 28px;
}
.dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.dialog__header h3 {
  font-size: 18px;
}
.dialog__close {
  border: none;
  background: transparent;
  color: var(--foreground-muted);
  padding: 4px;
  border-radius: 6px;
  display: flex;
}
.dialog__close:hover {
  background: var(--surface-secondary);
}
.dialog__body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
