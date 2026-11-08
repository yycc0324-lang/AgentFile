<script setup lang="ts">
import AppIcon from './AppIcon.vue'

withDefaults(
  defineProps<{
    variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
    size?: 'sm' | 'md'
    icon?: string
    disabled?: boolean
    block?: boolean
  }>(),
  { variant: 'primary', size: 'md', disabled: false, block: false },
)
</script>

<template>
  <button
    class="btn"
    :class="[`btn--${variant}`, `btn--${size}`, { 'btn--block': block }]"
    :disabled="disabled"
  >
    <AppIcon v-if="icon" :name="icon" :size="size === 'sm' ? 14 : 16" />
    <span v-if="$slots.default"><slot /></span>
  </button>
</template>

<style scoped>
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px solid transparent;
  border-radius: var(--radius-lg);
  font-weight: 500;
  white-space: nowrap;
  transition:
    background 0.15s,
    border-color 0.15s,
    color 0.15s;
}
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn--md {
  height: 36px;
  padding: 0 16px;
  font-size: 14px;
}
.btn--sm {
  height: 30px;
  padding: 0 12px;
  font-size: 13px;
}
.btn--block {
  width: 100%;
}
.btn--primary {
  background: var(--accent-primary);
  color: var(--foreground-inverse);
}
.btn--primary:hover:not(:disabled) {
  background: var(--accent-strong);
}
.btn--secondary {
  background: var(--surface-primary);
  border-color: var(--border);
  color: var(--foreground-primary);
}
.btn--secondary:hover:not(:disabled) {
  border-color: var(--border-strong);
}
.btn--ghost {
  background: transparent;
  color: var(--foreground-secondary);
}
.btn--ghost:hover:not(:disabled) {
  background: var(--surface-secondary);
}
.btn--danger {
  background: var(--error);
  color: #fff;
}
</style>
