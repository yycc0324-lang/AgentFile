<script setup lang="ts">
import AppIcon from './AppIcon.vue'

withDefaults(
  defineProps<{
    icon?: string
    title: string
    description?: string
  }>(),
  { icon: 'file-text' },
)
</script>

<template>
  <div class="empty">
    <AppIcon :name="icon" :size="36" />
    <h4>{{ title }}</h4>
    <p v-if="description">{{ description }}</p>
    <slot />
  </div>
</template>

<style scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 48px 24px;
  color: var(--foreground-muted);
  text-align: center;
}
.empty h4 {
  color: var(--foreground-primary);
  font-size: 16px;
}
.empty p {
  font-size: 13px;
  max-width: 320px;
}
</style>
