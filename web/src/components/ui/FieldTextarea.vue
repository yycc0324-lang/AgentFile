<script setup lang="ts">
withDefaults(
  defineProps<{
    label?: string
    modelValue: string
    placeholder?: string
    rows?: number
  }>(),
  { rows: 3 },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()
</script>

<template>
  <label class="field">
    <span v-if="label" class="field__label">{{ label }}</span>
    <textarea
      class="field__input"
      :rows="rows"
      :value="modelValue"
      :placeholder="placeholder"
      @input="emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
    />
  </label>
</template>

<style scoped>
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field__label {
  font-size: 13px;
  color: var(--foreground-secondary);
}
.field__input {
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  font-size: 14px;
  font-family: inherit;
  color: var(--foreground-primary);
  background: var(--surface-primary);
  resize: vertical;
}
.field__input::placeholder {
  color: var(--foreground-muted);
}
.field__input:focus {
  outline: none;
  border-color: var(--accent-primary);
}
</style>
