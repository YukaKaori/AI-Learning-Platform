import { createRouter, createWebHistory } from 'vue-router'
import { i18n } from '@/locales'
import AppLayout from '@/layouts/AppLayout.vue'
import { installAuthGuards } from './guards'

declare module 'vue-router' {
  interface RouteMeta {
    titleKey?: string
    /** Route requires an authenticated session. */
    requiresAuth?: boolean
    /** Route is for unauthenticated visitors only (e.g. login). */
    guestOnly?: boolean
    /** Reserved for the RBAC phase — evaluated by the auth guards. */
    roles?: string[]
    permissions?: string[]
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { titleKey: 'auth.login.title', guestOnly: true },
    },
    {
      // Post-authentication welcome experience — the bridge into the
      // workspace. Full-bleed, so it lives outside AppLayout.
      path: '/welcome',
      name: 'welcome',
      component: () => import('@/views/WelcomeView.vue'),
      meta: { titleKey: 'welcome.title', requiresAuth: true },
    },
    {
      path: '/',
      component: AppLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/HomeView.vue'),
          meta: { titleKey: 'nav.home' },
        },
        {
          path: 'design-system',
          name: 'design-system',
          component: () => import('@/views/DesignSystemView.vue'),
          meta: { titleKey: 'designSystem.title' },
        },
      ],
    },
  ],
})

installAuthGuards(router)

router.afterEach((to) => {
  const titleKey = to.meta.titleKey as string | undefined
  const appName = i18n.global.t('app.name')
  document.title = titleKey ? `${i18n.global.t(titleKey)} · ${appName}` : appName
})

export default router
