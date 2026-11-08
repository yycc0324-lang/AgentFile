<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '@/components/layout/AppShell.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AppBadge from '@/components/ui/AppBadge.vue'
import StatCard from '@/components/ui/StatCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { evalApi } from '@/api'
import type { EvalRun } from '@/types'

const route = useRoute()
const router = useRouter()

const runId = computed(() => String(route.params.runId))
const run = ref<EvalRun | null>(null)
const loading = ref(false)
const error = ref('')

const metricEntries = computed(() => Object.entries(run.value?.metrics ?? {}))

async function load() {
  loading.value = true
  error.value = ''
  try {
    run.value = await evalApi.getRun(runId.value)
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
      <button class="back" @click="router.push('/eval/datasets')">
        <AppIcon name="arrow-left" :size="16" />
        返回评估集
      </button>

      <p v-if="error" class="page__error">{{ error }}</p>
      <div v-if="loading" class="page__hint">加载中…</div>

      <template v-else-if="run">
        <header class="head">
          <div class="head__title">
            <h1>{{ run.datasetName }}</h1>
            <AppBadge :tone="run.status === 'completed' ? 'success' : 'info'">
              {{ run.status }}
            </AppBadge>
          </div>
          <p class="head__meta">{{ run.model }} · {{ run.createdAt }}</p>
        </header>

        <div v-if="metricEntries.length" class="stats">
          <StatCard
            v-for="[key, value] in metricEntries"
            :key="key"
            :label="key"
            :value="value"
          />
        </div>

        <section class="card">
          <h3>样本结果</h3>
          <EmptyState
            v-if="!run.samples.length"
            icon="flask-conical"
            title="暂无样本结果"
          />
          <div v-else class="samples">
            <div v-for="(s, i) in run.samples" :key="i" class="sample">
              <div class="sample__top">
                <span class="sample__idx">#{{ i + 1 }}</span>
                <AppBadge
                  :tone="s.status === 'passed' ? 'success' : 'warning'"
                  :dot="true"
                >
                  {{ s.status === 'passed' ? '通过' : '待改进' }}
                </AppBadge>
              </div>
              <p class="sample__q">{{ s.question }}</p>
              <span class="sample__score">得分 {{ s.score }}</span>
            </div>
          </div>
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
.head__meta {
  font-size: 13px;
  color: var(--foreground-muted);
  margin-top: 4px;
}
.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
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
.samples {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.sample {
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.sample__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.sample__idx {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--foreground-muted);
}
.sample__q {
  font-size: 14px;
  color: var(--foreground-primary);
}
.sample__score {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--foreground-secondary);
}
</style>
