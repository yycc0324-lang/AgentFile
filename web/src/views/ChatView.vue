<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '@/components/layout/AppShell.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import ConversationItem from '@/components/conversation/ConversationItem.vue'
import ChatPanel from '@/components/chat/ChatPanel.vue'
import { conversationsApi, projectsApi } from '@/api'
import { useAppStore } from '@/stores/app'
import type { Conversation, Project } from '@/types'

const route = useRoute()
const router = useRouter()
const store = useAppStore()

const projectId = computed(() => Number(route.params.projectId))
const conversationId = computed(() => Number(route.params.conversationId))

const project = ref<Project | null>(null)
const conversations = ref<Conversation[]>([])
const error = ref('')

async function load() {
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
  }
}

const activeConversation = computed(() =>
  conversations.value.find((c) => c.id === conversationId.value),
)

function select(c: Conversation) {
  store.setConversation(c)
  router.push(`/projects/${projectId.value}/conversations/${c.id}`)
}

async function newConversation() {
  try {
    const conv = await conversationsApi.create({
      projectId: projectId.value,
      title: '新会话',
    })
    await load()
    select(conv)
  } catch (e) {
    error.value = (e as Error).message
  }
}

onMounted(load)
</script>

<template>
  <AppShell>
    <div class="chat-layout">
      <aside class="conv">
        <header class="conv__header">
          <span class="conv__title">{{ project?.name ?? '会话' }}</span>
          <button class="conv__add" title="新建会话" @click="newConversation">
            <AppIcon name="plus" :size="16" />
          </button>
        </header>

        <div class="conv__list">
          <ConversationItem
            v-for="c in conversations"
            :key="c.id"
            :conversation="c"
            :active="c.id === conversationId"
            @select="select"
          />
          <p v-if="!conversations.length" class="conv__empty">暂无会话</p>
        </div>
      </aside>

      <ChatPanel
        :key="conversationId"
        :project-id="projectId"
        :conversation-id="conversationId"
        :title="activeConversation?.title"
      />

      <p v-if="error" class="chat-error">{{ error }}</p>
    </div>
  </AppShell>
</template>

<style scoped>
.chat-layout {
  flex: 1;
  min-height: 0;
  display: flex;
  position: relative;
}
.conv {
  width: var(--conv-panel-width);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}
.conv__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--border);
}
.conv__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--foreground-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.conv__add {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: var(--radius-lg);
  background: var(--accent-tint);
  color: var(--accent-strong);
  display: flex;
  align-items: center;
  justify-content: center;
}
.conv__list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.conv__empty {
  font-size: 13px;
  color: var(--foreground-muted);
  padding: 16px;
  text-align: center;
}
.chat-error {
  position: absolute;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--error-bg);
  color: var(--error);
  font-size: 12px;
  padding: 6px 12px;
  border-radius: var(--radius-full);
}
</style>
