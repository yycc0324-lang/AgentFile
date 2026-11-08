<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '@/components/layout/AppShell.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AppBadge from '@/components/ui/AppBadge.vue'
import StatCard from '@/components/ui/StatCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import TraceTree from '@/components/trace/TraceTree.vue'
import { tracesApi } from '@/api'
import type { Trace, SpanStatus } from '@/types'

const route = useRoute()
const router = useRouter()

const traceId = computed(() => String(route.params.traceId))
const trace = ref<Trace | null>(null)
const loading = ref(false)
const error = ref('')

const statusTone: Record<SpanStatus, 'success' | 'error' | 'info'> = {
  success: 'success',
  failed: 'error',
  running: 'info',
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    trace.value = await tracesApi.get(traceId.value)
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <AppShell>
    <div class="page">
      <button class="back" @click="router.back()">
        <AppIcon name="arrow-left" :size="16" />
        返回
      </button>

      <p v-if="error" class="page__error">{{ error }}</p>
      <div v-if="loading" class="page__hint">加载中…</div>

      <template v-else-if="trace">
        <header class="head">
          <div>
            <div class="head__title">
              <h1>Trace 详情</h1>
              <AppBadge :tone="statusTone[trace.status]">{{ trace.status }}</AppBadge>
            </div>
            <p class="head__id">{{ trace.traceId }}</p>
          </div>
        </header>

        <div class="stats">
          <StatCard label="总耗时" :value="`${trace.durationMs}ms`" />
          <StatCard label="Span 数" :value="trace.spanCount" />
          <StatCard label="Token 消耗" :value="trace.tokens" />
        </div>

        <section class="card">
          <h3>执行树</h3>
          <TraceTree v-if="trace.spans.length" :spans="trace.spans" />
          <EmptyState v-else icon="braces" title="暂无 Span 数据" />
        </section>
      </template>
    </div>
  </AppShell>
</template>

<style scoped>
.page {
  padding: 32px;
  max-width: 900px;
  width: 100%;
  margin: 0 auto;
  overflow-y: auto;
}
.back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  background: transparent;
  color: var(--foreground-secondary);
  font-size: 13px;
  padding: 0;
  margin-bottom: 20px;
}
.back:hover {
  color: var(--foreground-primary);
}
.page__error {
  color: var(--error);
  font-size: 13px;
}
.page__hint {
  color: var(--foreground-muted);
  font-size: 13px;
}
.head {
  margin-bottom: 24px;
}
.head__title {
  display: flex;
  align-items: center;
  gap: 12px;
}
.head__title h1 {
  font-size: 24px;
}
.head__id {
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--foreground-muted);
  margin-top: 4px;
}
.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.card {
  padding: 24px;
  border: 1px solid var(--border);
  border-radius: var(--radius-2xl);
}
.card h3 {
  font-size: 16px;
  margin-bottom: 16px;
}
</style>
