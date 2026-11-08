<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    tone?: 'success' | 'warning' | 'error' | 'neutral' | 'info'
    dot?: boolean
  }>(),
  { tone: 'neutral', dot: true },
)

const colors: Record<string, { bg: string; fg: string }> = {
  success: { bg: 'var(--success-bg)', fg: 'var(--success)' },
  warning: { bg: 'var(--warning-bg)', fg: 'var(--warning)' },
  error: { bg: 'var(--error-bg)', fg: 'var(--error)' },
  neutral: { bg: '#f1f2f4', fg: 'var(--foreground-secondary)' },
  info: { bg: 'var(--accent-tint)', fg: 'var(--accent-strong)' },
}

const style = computed(() => ({
  background: colors[props.tone].bg,
  color: colors[props.tone].fg,
}))
</script>

<template>
  <span class="badge" :style="style">
    <span v-if="dot" class="badge__dot" :style="{ background: colors[tone].fg }" />
    <slot />
  </span>
</template>

<style scoped>
.badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  line-height: 1;
}
.badge__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}
</style>
