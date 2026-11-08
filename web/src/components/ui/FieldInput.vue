<script setup lang="ts">
withDefaults(
  defineProps<{
    label?: string
    modelValue: string
    placeholder?: string
    type?: string
  }>(),
  { type: 'text' },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()
</script>

<template>
  <label class="field">
    <span v-if="label" class="field__label">{{ label }}</span>
    <input
      class="field__input"
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
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
  height: 40px;
  padding: 0 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  font-size: 14px;
  font-family: inherit;
  color: var(--foreground-primary);
  background: var(--surface-primary);
}
.field__input::placeholder {
  color: var(--foreground-muted);
}
.field__input:focus {
  outline: none;
  border-color: var(--accent-primary);
}
</style>
