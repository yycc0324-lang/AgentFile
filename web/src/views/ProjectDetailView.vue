<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '@/components/layout/AppShell.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import StatCard from '@/components/ui/StatCard.vue'
import { projectsApi } from '@/api'
import { useAppStore } from '@/stores/app'
import type { Project } from '@/types'

const route = useRoute()
const router = useRouter()
const store = useAppStore()

const project = ref<Project | null>(null)
const loading = ref(false)
const error = ref('')

const projectId = computed(() => Number(route.params.projectId))

async function load() {
  loading.value = true
  error.value = ''
  try {
    project.value = await projectsApi.get(projectId.value)
    store.setProject(project.value)
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
      <button class="back" @click="router.push('/projects')">
        <AppIcon name="arrow-left" :size="16" />
        返回项目列表
      </button>

      <p v-if="error" class="page__error">{{ error }}</p>
      <div v-if="loading" class="page__hint">加载中…</div>

      <template v-else-if="project">
        <header class="head">
          <div class="head__icon"><AppIcon name="folder" :size="24" /></div>
          <div class="head__main">
            <h1>{{ project.name }}</h1>
            <p class="head__desc">{{ project.description || '暂无描述' }}</p>
          </div>
          <div class="head__actions">
            <AppButton
              variant="secondary"
              icon="database"
              @click="router.push('/ingest')"
            >
              数据接入
            </AppButton>
            <AppButton
              icon="message-square"
              @click="router.push(`/projects/${project.id}/conversations`)"
            >
              发起会话
            </AppButton>
          </div>
        </header>

        <div class="stats">
          <StatCard label="会话数" :value="project.conversationCount ?? 0" />
          <StatCard label="文档数" :value="project.documentCount ?? 0" />
          <StatCard label="评估运行" :value="project.evalRunCount ?? 0" />
        </div>

        <section class="card">
          <h3>数据源配置</h3>
          <div class="row">
            <AppIcon name="git-branch" :size="16" />
            <span class="row__label">代码仓库</span>
            <span class="row__value">{{ project.repoUrl || '未配置' }}</span>
          </div>
          <div class="row">
            <AppIcon name="calendar" :size="16" />
            <span class="row__label">创建时间</span>
            <span class="row__value">{{ project.createdAt }}</span>
          </div>
        </section>
      </template>
    </div>
  </AppShell>
</template>

<style scoped>
.page {
  padding: 32px;
  max-width: 1000px;
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
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
}
.head__icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: var(--accent-tint);
  color: var(--accent-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.head__main {
  flex: 1;
  min-width: 0;
}
.head__main h1 {
  font-size: 24px;
}
.head__desc {
  color: var(--foreground-secondary);
  font-size: 14px;
  margin-top: 4px;
}
.head__actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
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
.row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-top: 1px solid var(--border);
  color: var(--foreground-secondary);
}
.row:first-of-type {
  border-top: none;
}
.row__label {
  width: 100px;
  flex-shrink: 0;
}
.row__value {
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--foreground-primary);
}
</style>
