<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useAppStore, type ThemeMode } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import { SUPPORTED_LOCALES, type AppLocale } from '@/locales'

defineEmits<{ navigate: [] }>()

const { t } = useI18n()
const router = useRouter()
const appStore = useAppStore()
const authStore = useAuthStore()

async function handleLogout() {
  await authStore.logout()
  await router.replace({ name: 'login' })
}

const themeModes: ThemeMode[] = ['light', 'dark', 'system']

const localeLabels: Record<AppLocale, string> = {
  'zh-CN': '中文',
  'en-US': 'EN',
}
</script>

<template>
  <div class="app-sidebar">
    <div class="brand">
      <span class="brand-mark" aria-hidden="true"></span>
      <span class="brand-name">{{ t('app.name') }}</span>
    </div>

    <nav class="nav">
      <RouterLink to="/" class="nav-item" @click="$emit('navigate')">{{ t('nav.home') }}</RouterLink>
    </nav>

    <div class="sidebar-footer">
      <div v-if="authStore.user" class="user-row">
        <span class="user-name">{{ authStore.user.nickname || authStore.user.username }}</span>
        <button class="logout-button" type="button" @click="handleLogout">
          {{ t('auth.logout') }}
        </button>
      </div>
      <div class="control-group" role="radiogroup" :aria-label="t('common.theme.label')">
        <button
          v-for="mode in themeModes"
          :key="mode"
          class="control-chip"
          :class="{ active: appStore.themeMode === mode }"
          type="button"
          @click="appStore.setThemeMode(mode)"
        >
          {{ t(`common.theme.${mode}`) }}
        </button>
      </div>
      <div class="control-group" role="radiogroup" :aria-label="t('common.language')">
        <button
          v-for="loc in SUPPORTED_LOCALES"
          :key="loc"
          class="control-chip"
          :class="{ active: appStore.locale === loc }"
          type="button"
          @click="appStore.setLocale(loc)"
        >
          {{ localeLabels[loc] }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.app-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: var(--space-5) var(--space-3);
}

.brand {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 0 var(--space-2) var(--space-5);
}

.brand-mark {
  width: 20px;
  height: 20px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-hover));
}

.brand-name {
  font-size: var(--text-base);
  font-weight: 600;
  letter-spacing: var(--tracking-tight);
}

.nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.nav-item {
  padding: var(--space-2) var(--space-2);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
  transition:
    background-color var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out);
}

.nav-item:hover {
  background-color: var(--color-surface-hover);
  color: var(--color-text);
}

.nav-item.router-link-exact-active {
  background-color: var(--color-primary-soft);
  color: var(--color-primary);
  font-weight: 500;
}

.sidebar-footer {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.user-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-2);
  border-radius: var(--radius-md);
  background-color: var(--color-surface-hover);
}

.user-name {
  overflow: hidden;
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--color-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout-button {
  flex-shrink: 0;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--color-text-tertiary);
  font-family: inherit;
  font-size: var(--text-xs);
  cursor: pointer;
  transition: color var(--duration-fast) var(--ease-out);
}

.logout-button:hover {
  color: var(--color-danger);
}

.control-group {
  display: flex;
  gap: 2px;
  padding: 2px;
  border-radius: var(--radius-md);
  background-color: var(--color-surface-hover);
}

.control-chip {
  flex: 1;
  padding: var(--space-1) 0;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-text-tertiary);
  font-family: inherit;
  font-size: var(--text-xs);
  cursor: pointer;
  transition:
    background-color var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out);
}

.control-chip:hover {
  color: var(--color-text-secondary);
}

.control-chip.active {
  background-color: var(--color-surface);
  color: var(--color-text);
  box-shadow: var(--shadow-sm);
}
</style>
