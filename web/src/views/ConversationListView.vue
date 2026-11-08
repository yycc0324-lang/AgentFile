<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '@/components/layout/AppShell.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AppModal from '@/components/ui/AppModal.vue'
import FieldInput from '@/components/ui/FieldInput.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import ConversationRow from '@/components/conversation/ConversationRow.vue'
import { conversationsApi, projectsApi } from '@/api'
import { useAppStore } from '@/stores/app'
import type { Conversation, Project } from '@/types'

const route = useRoute()
const router = useRouter()
const store = useAppStore()

const projectId = computed(() => Number(route.params.projectId))

const project = ref<Project | null>(null)
const conversations = ref<Conversation[]>([])
const loading = ref(false)
const error = ref('')
const showCreate = ref(false)
const newTitle = ref('')
const creating = ref(false)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [p, cs] = await Promise.all([
      projectsApi.get(projectId.value),
      conversationsApi.listByProject(projectId.value),
    ])
    project.value = p
    conversations.value = cs
    store.setProject(p)
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

async function create() {
  if (!newTitle.value.trim()) return
  creating.value = true
  try {
    const conv = await conversationsApi.create({
      projectId: projectId.value,
      title: newTitle.value.trim(),
    })
    showCreate.value = false
    newTitle.value = ''
    router.push(`/projects/${projectId.value}/conversations/${conv.id}`)
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    creating.value = false
  }
}

function select(conversation: Conversation) {
  store.setConversation(conversation)
  router.push(`/projects/${projectId.value}/conversations/${conversation.id}`)
}

onMounted(load)
</script>

<template>
  <AppShell>
    <div class="page">
      <button class="back" @click="router.push(`/projects/${projectId}`)">
        <AppIcon name="arrow-left" :size="16" />
        返回项目详情
      </button>

      <header class="page__header">
        <div>
          <h1>会话</h1>
          <p class="page__subtitle">{{ project?.name ?? '' }} · 会话列表</p>
        </div>
        <AppButton icon="plus" @click="showCreate = true">新建会话</AppButton>
      </header>

      <p v-if="error" class="page__error">{{ error }}</p>
      <div v-if="loading" class="page__hint">加载中…</div>

      <div v-else-if="conversations.length" class="list">
        <ConversationRow
          v-for="c in conversations"
          :key="c.id"
          :conversation="c"
          @select="select"
        />
      </div>

      <EmptyState
        v-else
        icon="message-square"
        title="还没有会话"
        description="发起一次新会话，开始与你的 Agent 对话。"
      >
        <AppButton icon="plus" @click="showCreate = true">新建会话</AppButton>
      </EmptyState>
    </div>

    <AppModal :open="showCreate" title="新建会话" @close="showCreate = false">
      <FieldInput v-model="newTitle" label="会话标题" placeholder="例如：修复登录接口 bug" />
      <template #footer>
        <AppButton variant="ghost" @click="showCreate = false">取消</AppButton>
        <AppButton :disabled="creating || !newTitle.trim()" @click="create">
          {{ creating ? '创建中…' : '创建' }}
        </AppButton>
      </template>
    </AppModal>
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
.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
