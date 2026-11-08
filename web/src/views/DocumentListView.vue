<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppShell from '@/components/layout/AppShell.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AppBadge from '@/components/ui/AppBadge.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { projectsApi, ingestApi } from '@/api'
import type { Project, DocumentItem, DocumentStatus } from '@/types'

const projects = ref<Project[]>([])
const selectedId = ref<number | null>(null)
const documents = ref<DocumentItem[]>([])
const loading = ref(false)
const error = ref('')

const statusTone: Record<DocumentStatus, 'success' | 'info' | 'error'> = {
  success: 'success',
  processing: 'info',
  failed: 'error',
}

const statusLabel: Record<DocumentStatus, string> = {
  success: '已接入',
  processing: '处理中',
  failed: '失败',
}

async function loadDocuments() {
  if (selectedId.value == null) return
  loading.value = true
  error.value = ''
  try {
    documents.value = await ingestApi.documents(selectedId.value)
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

async function selectProject(e: Event) {
  selectedId.value = Number((e.target as HTMLSelectElement).value)
  await loadDocuments()
}

onMounted(async () => {
  projects.value = await projectsApi.list()
  if (projects.value.length) {
    selectedId.value = projects.value[0].id
    await loadDocuments()
  }
})
</script>

<template>
  <AppShell>
    <div class="page">
      <header class="page__header">
        <div>
          <h1>文档</h1>
          <p class="page__subtitle">查看已接入项目知识库的文档</p>
        </div>
        <label class="picker">
          <select class="picker__select" :value="selectedId ?? ''" @change="selectProject">
            <option v-for="p in projects" :key="p.id" :value="p.id">{{ p.name }}</option>
          </select>
        </label>
      </header>

      <p v-if="error" class="page__error">{{ error }}</p>
      <div v-if="loading" class="page__hint">加载中…</div>

      <EmptyState
        v-else-if="!documents.length"
        icon="file-text"
        title="暂无文档"
        description="前往「数据接入」页接入项目数据后，文档将在这里展示。"
      />

      <div v-else class="table">
        <div class="table__head">
          <span>名称</span>
          <span>类型</span>
          <span>大小</span>
          <span>状态</span>
          <span>接入时间</span>
        </div>
        <div v-for="d in documents" :key="d.id" class="table__row">
          <span class="cell-name">
            <AppIcon name="file-text" :size="16" />
            {{ d.name }}
          </span>
          <span class="cell-mono">{{ d.type }}</span>
          <span class="cell-mono">{{ d.size }}</span>
          <span><AppBadge :tone="statusTone[d.status]">{{ statusLabel[d.status] }}</AppBadge></span>
          <span class="cell-mono">{{ d.ingestedAt }}</span>
        </div>
      </div>
    </div>
  </AppShell>
</template>

<style scoped>
.page {
  padding: 32px;
  max-width: 1100px;
  width: 100%;
  margin: 0 auto;
  overflow-y: auto;
}
.page__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
}
.page__header h1 {
  font-size: 24px;
}
.page__subtitle {
  color: var(--foreground-secondary);
  font-size: 14px;
  margin-top: 4px;
}
.picker__select {
  height: 40px;
  padding: 0 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  font-size: 14px;
  font-family: inherit;
  color: var(--foreground-primary);
  background: var(--surface-primary);
}
.page__error {
  color: var(--error);
  font-size: 13px;
}
.page__hint {
  color: var(--foreground-muted);
  font-size: 13px;
}
.table {
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  overflow: hidden;
}
.table__head,
.table__row {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr 1.4fr;
  gap: 12px;
  align-items: center;
  padding: 14px 20px;
}
.table__head {
  background: var(--surface-secondary);
  font-size: 12px;
  color: var(--foreground-secondary);
  border-bottom: 1px solid var(--border);
}
.table__row {
  border-bottom: 1px solid var(--border);
  font-size: 13px;
  color: var(--foreground-primary);
}
.table__row:last-child {
  border-bottom: none;
}
.cell-name {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cell-mono {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--foreground-secondary);
}
</style>
