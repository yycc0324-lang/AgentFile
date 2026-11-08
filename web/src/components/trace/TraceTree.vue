<script setup lang="ts">
import AppIcon from '@/components/ui/AppIcon.vue'
import type { Span, SpanType } from '@/types'

defineOptions({ name: 'TraceTree' })

defineProps<{ spans: Span[] }>()

const TYPE_ICON: Record<SpanType, string> = {
  llm: 'cpu',
  tool: 'braces',
  agent: 'zap',
}
</script>

<template>
  <div class="tree">
    <div v-for="span in spans" :key="span.id" class="node">
      <div class="node__row">
        <span class="node__icon" :class="`node__icon--${span.type}`">
          <AppIcon :name="TYPE_ICON[span.type] ?? 'zap'" :size="14" />
        </span>
        <span class="node__name">{{ span.name }}</span>
        <span class="node__type">{{ span.type }}</span>
        <span class="node__status" :class="`node__status--${span.status}`">
          {{ span.status }}
        </span>
        <span class="node__dur">{{ span.durationMs }}ms</span>
      </div>

      <p v-if="span.error" class="node__error">{{ span.error }}</p>

      <div v-if="span.children?.length" class="node__children">
        <TraceTree :spans="span.children" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.tree {
  display: flex;
  flex-direction: column;
}
.node {
  display: flex;
  flex-direction: column;
}
.node__row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: var(--radius-lg);
}
.node__row:hover {
  background: var(--surface-secondary);
}
.node__icon {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.node__icon--llm {
  background: var(--accent-tint);
  color: var(--accent-strong);
}
.node__icon--tool {
  background: var(--warning-bg);
  color: var(--warning);
}
.node__icon--agent {
  background: var(--success-bg);
  color: var(--success);
}
.node__name {
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--foreground-primary);
}
.node__type {
  font-size: 11px;
  color: var(--foreground-muted);
}
.node__status {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: var(--radius-full);
}
.node__status--success {
  background: var(--success-bg);
  color: var(--success);
}
.node__status--failed {
  background: var(--error-bg);
  color: var(--error);
}
.node__status--running {
  background: var(--accent-tint);
  color: var(--accent-strong);
}
.node__dur {
  margin-left: auto;
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--foreground-muted);
}
.node__error {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--error);
  padding: 0 10px 6px 42px;
}
.node__children {
  margin-left: 20px;
  padding-left: 10px;
  border-left: 1px solid var(--border);
}
</style>
