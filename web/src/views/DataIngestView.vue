<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppShell from '@/components/layout/AppShell.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AppBadge from '@/components/ui/AppBadge.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { projectsApi, ingestApi } from '@/api'
import type { Project, DocumentItem, DocumentStatus } from '@/types'

const projects = ref<Project[]>([])
const selectedId = ref<number | null>(null)
const documents = ref<DocumentItem[]>([])
const ingesting = ref(false)
const message = ref('')
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

async function loadProjects() {
  projects.value = await projectsApi.list()
}

async function loadDocuments() {
  if (selectedId.value == null) return
  documents.value = await ingestApi.documents(selectedId.value)
}

async function selectProject(e: Event) {
  selectedId.value = Number((e.target as HTMLSelectElement).value)
  message.value = ''
  error.value = ''
  await loadDocuments()
}

async function ingest() {
  if (selectedId.value == null) return
  ingesting.value = true
  message.value = ''
  error.value = ''
  try {
    await ingestApi.ingest(selectedId.value)
    message.value = '数据接入任务已提交，正在后台处理…'
    await loadDocuments()
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    ingesting.value = false
  }
}

onMounted(loadProjects)
</script>

<template>
  <AppShell>
    <div class="page">
      <header class="page__header">
        <div>
          <h1>数据接入</h1>
          <p class="page__subtitle">将代码仓库接入项目，构建代码知识库</p>
        </div>
      </header>

      <section class="card">
        <h3>接入配置</h3>
        <div class="form">
          <label class="field">
            <span class="field__label">选择项目</span>
            <select class="field__select" :value="selectedId ?? ''" @change="selectProject">
              <option value="" disabled>请选择项目</option>
              <option v-for="p in projects" :key="p.id" :value="p.id">{{ p.name }}</option>
            </select>
          </label>
          <AppButton
            icon="upload"
            :disabled="ingesting || selectedId == null"
            @click="ingest"
          >
            {{ ingesting ? '接入中…' : '开始接入' }}
          </AppButton>
        </div>
        <p v-if="message" class="msg msg--ok">
          <AppIcon name="info" :size="14" />
          {{ message }}
        </p>
        <p v-if="error" class="msg msg--err">
          <AppIcon name="alert-circle" :size="14" />
          {{ error }}
        </p>
      </section>

      <section class="card">
        <h3>文档列表</h3>
        <EmptyState
          v-if="selectedId == null"
          icon="database"
          title="尚未选择项目"
          description="请先在上方选择一个项目，然后开始接入数据。"
        />
        <EmptyState
          v-else-if="!documents.length"
          icon="file-text"
          title="暂无文档"
          description="点击「开始接入」后，文档将出现在这里。"
        />
        <div v-else class="docs">
          <div v-for="d in documents" :key="d.id" class="doc">
            <AppIcon name="file-text" :size="18" />
            <span class="doc__name">{{ d.name }}</span>
            <span class="doc__type">{{ d.type }}</span>
            <span class="doc__size">{{ d.size }}</span>
            <AppBadge :tone="statusTone[d.status]">{{ statusLabel[d.status] }}</AppBadge>
          </div>
        </div>
      </section>
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
.page__header {
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
.card {
  padding: 24px;
  border: 1px solid var(--border);
  border-radius: var(--radius-2xl);
  margin-bottom: 20px;
}
.card h3 {
  font-size: 16px;
  margin-bottom: 16px;
}
.form {
  display: flex;
  align-items: flex-end;
  gap: 16px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  max-width: 360px;
}
.field__label {
  font-size: 13px;
  color: var(--foreground-secondary);
}
.field__select {
  height: 40px;
  padding: 0 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  font-size: 14px;
  font-family: inherit;
  color: var(--foreground-primary);
  background: var(--surface-primary);
}
.msg {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  margin-top: 14px;
}
.msg--ok {
  color: var(--success);
}
.msg--err {
  color: var(--error);
}
.docs {
  display: flex;
  flex-direction: column;
}
.doc {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid var(--border);
  color: var(--foreground-secondary);
}
.doc__name {
  flex: 1;
  min-width: 0;
  color: var(--foreground-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.doc__type,
.doc__size {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--foreground-muted);
}
</style>
