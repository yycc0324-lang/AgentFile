import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/projects',
    },
    {
      path: '/projects',
      name: 'projects',
      component: () => import('@/views/ProjectListView.vue'),
    },
    {
      path: '/projects/:projectId',
      name: 'project-detail',
      component: () => import('@/views/ProjectDetailView.vue'),
    },
    {
      path: '/projects/:projectId/conversations',
      name: 'conversations',
      component: () => import('@/views/ConversationListView.vue'),
    },
    {
      path: '/projects/:projectId/conversations/:conversationId',
      name: 'chat',
      component: () => import('@/views/ChatView.vue'),
    },
    {
      path: '/ingest',
      name: 'ingest',
      component: () => import('@/views/DataIngestView.vue'),
    },
    {
      path: '/documents',
      name: 'documents',
      component: () => import('@/views/DocumentListView.vue'),
    },
    {
      path: '/traces/:traceId',
      name: 'trace-detail',
      component: () => import('@/views/TraceDetailView.vue'),
    },
    {
      path: '/eval/datasets',
      name: 'eval-datasets',
      component: () => import('@/views/EvalDatasetsView.vue'),
    },
    {
      path: '/eval/runs/:runId',
      name: 'eval-run-detail',
      component: () => import('@/views/EvalRunDetailView.vue'),
    },
  ],
})

export default router
