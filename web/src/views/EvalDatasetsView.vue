<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppShell from '@/components/layout/AppShell.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AppModal from '@/components/ui/AppModal.vue'
import FieldInput from '@/components/ui/FieldInput.vue'
import FieldTextarea from '@/components/ui/FieldTextarea.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { evalApi } from '@/api'
import type { EvalDataset } from '@/types'

const router = useRouter()

const datasets = ref<EvalDataset[]>([])
const loading = ref(false)
const error = ref('')
const showCreate = ref(false)
const saving = ref(false)

const form = ref({ name: '', description: '' })

async function load() {
  loading.value = true
  error.value = ''
  try {
    datasets.value = await evalApi.datasets()
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

async function create() {
  if (!form.value.name.trim()) return
  saving.value = true
  try {
    await evalApi.createDataset(form.value)
    showCreate.value = false
    form.value = { name: '', description: '' }
    await load()
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    saving.value = false
  }
}

async function startRun(dataset: EvalDataset) {
  try {
    const run = await evalApi.createRun({ datasetId: dataset.id })
    router.push(`/eval/runs/${run.id}`)
  } catch (e) {
    error.value = (e as Error).message
  }
}

onMounted(load)
</script>

<template>
  <AppShell>
    <div class="page">
      <header class="page__header">
        <div>
          <h1>评估集</h1>
          <p class="page__subtitle">管理评估数据集，运行模型评测</p>
        </div>
        <AppButton icon="plus" @click="showCreate = true">新建评估集</AppButton>
      </header>

      <p v-if="error" class="page__error">{{ error }}</p>
      <div v-if="loading" class="page__hint">加载中…</div>

      <div v-else-if="datasets.length" class="grid">
        <div v-for="d in datasets" :key="d.id" class="dataset">
          <div class="dataset__top">
            <div class="dataset__icon"><AppIcon name="flask-conical" :size="20" /></div>
            <AppIcon name="chevron-right" :size="16" class="dataset__chevron" />
          </div>
          <h4 class="dataset__name">{{ d.name }}</h4>
          <p class="dataset__desc">{{ d.description || '暂无描述' }}</p>
          <div class="dataset__footer">
            <span>{{ d.sampleCount }} 个样本</span>
            <span>{{ d.createdAt }}</span>
          </div>
          <AppButton size="sm" variant="secondary" block @click="startRun(d)">
            发起评估运行
          </AppButton>
        </div>
      </div>

      <EmptyState
        v-else
        icon="flask-conical"
        title="还没有评估集"
        description="创建评估集来评测你的 Agent 回答质量。"
      >
        <AppButton icon="plus" @click="showCreate = true">新建评估集</AppButton>
      </EmptyState>
    </div>

    <AppModal :open="showCreate" title="新建评估集" @close="showCreate = false">
      <FieldInput v-model="form.name" label="名称" placeholder="例如：常见问题集" />
      <FieldTextarea v-model="form.description" label="描述" placeholder="简要描述这个评估集" />
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
.page__error {
  color: var(--error);
  font-size: 13px;
}
.page__hint {
  color: var(--foreground-muted);
  font-size: 13px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}
.dataset {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px;
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  transition: border-color 0.15s, box-shadow 0.15s;
}
.dataset:hover {
  border-color: var(--border-strong);
  box-shadow: var(--shadow-sm);
}
.dataset__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.dataset__icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: var(--accent-tint);
  color: var(--accent-primary);
  display: flex;
  align-items: center;
  justify-content: center;
}
.dataset__chevron {
  color: var(--foreground-muted);
}
.dataset__name {
  font-size: 16px;
}
.dataset__desc {
  font-size: 14px;
  color: var(--foreground-secondary);
}
.dataset__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--foreground-muted);
  margin-bottom: 4px;
}
</style>
