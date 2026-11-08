import { defineStore } from 'pinia'
import type { Project, Conversation } from '@/types'

interface UserInfo {
  name: string
  role: string
}

export const useAppStore = defineStore('app', {
  state: () => ({
    user: {
      name: 'yicheng',
      role: '管理员',
    } as UserInfo,
    currentProject: null as Project | null,
    currentConversation: null as Conversation | null,
    sidebarCollapsed: false,
  }),
  getters: {
    userName: (state) => state.user.name,
    projectId: (state) => state.currentProject?.id ?? null,
    conversationId: (state) => state.currentConversation?.id ?? null,
  },
  actions: {
    setProject(project: Project | null) {
      this.currentProject = project
      if (project === null) this.currentConversation = null
    },
    setConversation(conversation: Conversation | null) {
      this.currentConversation = conversation
    },
  },
})
