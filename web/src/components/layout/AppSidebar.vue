<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/ui/AppIcon.vue'
import { useAppStore } from '@/stores/app'

interface NavItem {
  key: string
  label: string
  icon: string
  to?: string
  disabled?: boolean
  section?: string
}

const route = useRoute()
const router = useRouter()
const store = useAppStore()

const navItems: NavItem[] = [
  { key: 'projects', label: '项目', icon: 'folder', to: '/projects' },
  { key: 'ingest', label: '数据接入', icon: 'database', to: '/ingest' },
  { key: 'documents', label: '文档', icon: 'file-text', to: '/documents' },
  { key: 'eval', label: '评估', icon: 'flask-conical', to: '/eval/datasets' },
  { key: 'tasks', label: '任务', icon: 'list-checks', disabled: true, section: 'M2 规划' },
  { key: 'prompts', label: 'Prompt', icon: 'braces', disabled: true },
  { key: 'models', label: '模型', icon: 'cpu', disabled: true },
]

function isActive(item: NavItem): boolean {
  if (!item.to) return false
  if (item.key === 'projects') return route.path.startsWith('/projects')
  if (item.key === 'eval') return route.path.startsWith('/eval')
  return route.path === item.to || route.path.startsWith(item.to + '/')
}

function navigate(item: NavItem) {
  if (item.disabled || !item.to) return
  router.push(item.to)
}

const initials = computed(() => store.user.name.charAt(0).toUpperCase())
</script>

<template>
  <aside class="sidebar">
    <div class="brand">
      <span class="brand__logo"><AppIcon name="zap" :size="22" /></span>
      <span class="brand__name">AgentForge</span>
    </div>

    <nav class="nav">
      <template v-for="item in navItems" :key="item.key">
        <div v-if="item.section" class="nav__section">{{ item.section }}</div>
        <button
          class="nav__item"
          :class="{ 'nav__item--active': isActive(item), 'nav__item--disabled': item.disabled }"
          :disabled="item.disabled"
          @click="navigate(item)"
        >
          <AppIcon :name="item.icon" :size="18" />
          <span>{{ item.label }}</span>
        </button>
      </template>
    </nav>

    <div class="user">
      <div class="user__avatar">{{ initials }}</div>
      <div class="user__info">
        <span class="user__name">{{ store.user.name }}</span>
        <span class="user__role">{{ store.user.role }}</span>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: var(--sidebar-width);
  height: 100%;
  background: var(--surface-secondary);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px;
}
.brand__logo {
  width: 22px;
  height: 22px;
  color: var(--accent-primary);
  display: flex;
}
.brand__name {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
}
.nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 12px;
  overflow-y: auto;
}
.nav__section {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--foreground-muted);
  padding: 8px 12px 4px;
}
.nav__item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border: none;
  border-radius: var(--radius-lg);
  background: transparent;
  color: var(--foreground-secondary);
  font-size: 14px;
  text-align: left;
}
.nav__item:hover:not(:disabled) {
  background: #eef0f3;
}
.nav__item--active {
  background: var(--accent-tint);
  color: var(--accent-primary);
}
.nav__item--disabled {
  color: var(--foreground-muted);
  cursor: not-allowed;
}
.user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px;
  border-top: 1px solid var(--border);
}
.user__avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--surface-inverse);
  color: var(--foreground-inverse);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
}
.user__info {
  display: flex;
  flex-direction: column;
}
.user__name {
  font-size: 13px;
  color: var(--foreground-primary);
}
.user__role {
  font-size: 11px;
  color: var(--foreground-muted);
}
</style>
