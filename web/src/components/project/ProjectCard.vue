<script setup lang="ts">
import { useRouter } from 'vue-router'
import AppIcon from '@/components/ui/AppIcon.vue'
import type { Project } from '@/types'

const props = defineProps<{ project: Project }>()
const router = useRouter()

function open() {
  router.push(`/projects/${props.project.id}`)
}
</script>

<template>
  <div class="card" @click="open">
    <div class="card__top">
      <div class="card__icon">
        <AppIcon name="folder" :size="20" />
      </div>
      <AppIcon name="arrow-right" :size="18" class="card__arrow" />
    </div>
    <h4 class="card__name">{{ project.name }}</h4>
    <p class="card__desc">{{ project.description || '暂无描述' }}</p>
    <div class="card__meta">
      <div class="card__meta-row">
        <AppIcon name="git-branch" :size="14" />
        <span>{{ project.repoUrl || '未配置仓库' }}</span>
      </div>
      <div class="card__meta-row">
        <AppIcon name="file-text" :size="14" />
      </div>
    </div>
    <div class="card__footer">
      <span class="card__time">{{ project.createdAt }} 创建</span>
      <span class="card__count">{{ project.conversationCount ?? 0 }} 个会话</span>
    </div>
  </div>
</template>

<style scoped>
.card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px;
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  cursor: pointer;
  transition:
    border-color 0.15s,
    box-shadow 0.15s;
}
.card:hover {
  border-color: var(--border-strong);
  box-shadow: var(--shadow-sm);
}
.card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card__icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: var(--accent-tint);
  color: var(--accent-primary);
  display: flex;
  align-items: center;
  justify-content: center;
}
.card__arrow {
  color: var(--foreground-muted);
}
.card__name {
  font-size: 16px;
}
.card__desc {
  font-size: 14px;
  color: var(--foreground-secondary);
}
.card__meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.card__meta-row {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--foreground-muted);
  font-family: var(--font-mono);
  font-size: 12px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--foreground-muted);
}
</style>
