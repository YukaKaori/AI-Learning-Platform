import { createRouter, createWebHistory } from 'vue-router'
import { i18n } from '@/locales'
import AppLayout from '@/layouts/AppLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: AppLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/HomeView.vue'),
          meta: { titleKey: 'nav.home' },
        },
      ],
    },
  ],
})

// Auth guard is added in Phase 2 (beforeEach checking meta.requiresAuth against the auth store).

router.afterEach((to) => {
  const titleKey = to.meta.titleKey as string | undefined
  const appName = i18n.global.t('app.name')
  document.title = titleKey ? `${i18n.global.t(titleKey)} · ${appName}` : appName
})

export default router
