<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import type { Citation, SourceType } from '@/types'

const props = defineProps<{ citations: Citation[] }>()

const SOURCE_META: Record<SourceType, { label: string; icon: string }> = {
  code: { label: '代码', icon: 'braces' },
  doc: { label: '文档', icon: 'file-text' },
  url: { label: '链接', icon: 'link' },
  crawl: { label: '抓取', icon: 'external-link' },
}

const count = computed(() => props.citations.length)
</script>

<template>
  <div class="cite-panel">
    <header class="cite-panel__header">
      <span class="cite-panel__title">引用来源</span>
      <span class="cite-panel__count">{{ count }} 条</span>
    </header>

    <div v-if="!count" class="cite-panel__empty">暂无引用来源</div>

    <div v-for="(c, i) in citations" :key="i" class="cite-card">
      <div class="cite-card__top">
        <span class="cite-card__tag">
          <AppIcon :name="SOURCE_META[c.sourceType]?.icon ?? 'link'" :size="13" />
          {{ SOURCE_META[c.sourceType]?.label ?? c.sourceType }}
        </span>
        <span class="cite-card__file">{{ c.file }}</span>
      </div>
      <p class="cite-card__snippet">{{ c.snippet }}</p>
    </div>
  </div>
</template>

<style scoped>
.cite-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.cite-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.cite-panel__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--foreground-primary);
}
.cite-panel__count {
  font-size: 12px;
  color: var(--foreground-muted);
}
.cite-panel__empty {
  font-size: 12px;
  color: var(--foreground-muted);
}
.cite-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  background: var(--surface-secondary);
}
.cite-card__top {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.cite-card__tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background: var(--accent-tint);
  color: var(--accent-strong);
  font-size: 11px;
  flex-shrink: 0;
}
.cite-card__file {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--foreground-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.cite-card__snippet {
  font-size: 12px;
  color: var(--foreground-muted);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
