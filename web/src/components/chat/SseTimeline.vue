<script setup lang="ts">
import type { TimelineStep } from '@/types'

defineProps<{ steps: TimelineStep[] }>()
</script>

<template>
  <div class="timeline">
    <div v-if="!steps.length" class="timeline__empty">尚无执行记录</div>
    <div v-for="(step, i) in steps" :key="step.key" class="timeline__step">
      <div class="timeline__rail">
        <span class="timeline__dot" :class="`timeline__dot--${step.status}`" />
        <span v-if="i < steps.length - 1" class="timeline__line" />
      </div>
      <div class="timeline__body">
        <span class="timeline__label">{{ step.label }}</span>
        <span class="timeline__sublabel">{{ step.sublabel }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.timeline {
  display: flex;
  flex-direction: column;
}
.timeline__empty {
  font-size: 12px;
  color: var(--foreground-muted);
}
.timeline__step {
  display: flex;
  gap: 12px;
}
.timeline__rail {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.timeline__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 4px;
  flex-shrink: 0;
  background: var(--foreground-muted);
}
.timeline__dot--running {
  background: var(--accent-primary);
  animation: blink 1.2s infinite;
}
.timeline__dot--success {
  background: var(--success);
}
.timeline__dot--failed {
  background: var(--error);
}
.timeline__line {
  width: 1px;
  flex: 1;
  min-height: 18px;
  background: var(--border-strong);
}
.timeline__body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding-bottom: 14px;
  min-width: 0;
}
.timeline__label {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--foreground-primary);
}
.timeline__sublabel {
  font-size: 12px;
  color: var(--foreground-muted);
}
@keyframes blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.3;
  }
}
</style>
