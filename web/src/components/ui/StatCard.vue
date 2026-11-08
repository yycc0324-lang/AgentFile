<script setup lang="ts">
defineProps<{
  label: string
  value: string | number
  hint?: string
}>()
</script>

<template>
  <div class="stat">
    <span class="stat__label">{{ label }}</span>
    <span class="stat__value">{{ value }}</span>
    <span v-if="hint" class="stat__hint">{{ hint }}</span>
  </div>
</template>

<style scoped>
.stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 20px;
  border-radius: var(--radius-xl);
  background: var(--surface-secondary);
}
.stat__label {
  font-size: 13px;
  color: var(--foreground-secondary);
}
.stat__value {
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 600;
  color: var(--foreground-primary);
}
.stat__hint {
  font-size: 12px;
  color: var(--foreground-muted);
}
</style>
