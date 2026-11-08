<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppShell from '@/components/layout/AppShell.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppModal from '@/components/ui/AppModal.vue'
import FieldInput from '@/components/ui/FieldInput.vue'
import FieldTextarea from '@/components/ui/FieldTextarea.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import ProjectCard from '@/components/project/ProjectCard.vue'
import { projectsApi } from '@/api'
import type { Project } from '@/types'

const projects = ref<Project[]>([])
const loading = ref(false)
const error = ref('')
const showCreate = ref(false)
const saving = ref(false)

const form = ref({ name: '', description: '', repoUrl: '' })

async function load() {
  loading.value = true
  error.value = ''
  try {
    projects.value = await projectsApi.list()
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.value = { name: '', description: '', repoUrl: '' }
}

async function create() {
  if (!form.value.name.trim()) return
  saving.value = true
  try {
    await projectsApi.create(form.value)
    showCreate.value = false
    resetForm()
    await load()
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <AppShell>
    <div class="page">
      <header class="page__header">
        <div>
          <h1>项目</h1>
          <p class="page__subtitle">管理你的 Agent 开发项目，配置代码仓库数据源</p>
        </div>
        <AppButton icon="plus" @click="showCreate = true">新建项目</AppButton>
      </header>

      <p v-if="error" class="page__error">{{ error }}</p>

      <div v-if="loading" class="page__hint">加载中…</div>

      <div v-else-if="projects.length" class="grid">
        <ProjectCard v-for="p in projects" :key="p.id" :project="p" />
      </div>

      <EmptyState
        v-else
        icon="folder"
        title="还没有项目"
        description="创建你的第一个项目，开始接入数据并构建智能 Agent。"
      >
        <AppButton icon="plus" @click="showCreate = true">新建项目</AppButton>
      </EmptyState>
    </div>

    <AppModal :open="showCreate" title="新建项目" @close="showCreate = false">
      <FieldInput v-model="form.name" label="项目名称" placeholder="例如：blog-agent" />
      <FieldTextarea
        v-model="form.description"
        label="项目描述"
        placeholder="简要描述这个项目的目标"
      />
      <FieldInput v-model="form.repoUrl" label="代码仓库" placeholder="https://github.com/org/repo" />

      <template #footer>
        <AppButton variant="ghost" @click="showCreate = false">取消</AppButton>
        <AppButton :disabled="saving || !form.name.trim()" @click="create">
          {{ saving ? '创建中…' : '创建' }}
        </AppButton>
      </template>
    </AppModal>
  </AppShell>
</template>

<style scoped>
.page {
  padding: 32px;
  max-width: 1200px;
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
.page__error {
  color: var(--error);
  font-size: 13px;
  margin-bottom: 16px;
}
.page__hint {
  color: var(--foreground-muted);
  font-size: 13px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}
</style>
