<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useAppStore, type ThemeMode } from '@/stores/app'
import { SUPPORTED_LOCALES, type AppLocale } from '@/locales'

const { t } = useI18n()
const appStore = useAppStore()

const themeModes: ThemeMode[] = ['light', 'dark', 'system']

const localeLabels: Record<AppLocale, string> = {
  'zh-CN': '中文',
  'en-US': 'EN',
}
</script>

<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark" aria-hidden="true"></span>
        <span class="brand-name">{{ t('app.name') }}</span>
      </div>

      <nav class="nav">
        <RouterLink to="/" class="nav-item">{{ t('nav.home') }}</RouterLink>
      </nav>

      <div class="sidebar-footer">
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
    </aside>

    <main class="content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  height: 100%;
}

.sidebar {
  display: flex;
  flex-direction: column;
  width: var(--sidebar-width);
  flex-shrink: 0;
  padding: var(--space-5) var(--space-3);
  border-right: 1px solid var(--color-border);
  background-color: var(--color-surface);
  transition:
    background-color var(--duration-base) var(--ease-out),
    border-color var(--duration-base) var(--ease-out);
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
  letter-spacing: -0.01em;
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

.content {
  flex: 1;
  overflow-y: auto;
}
</style>
