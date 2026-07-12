<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { AppButton, AppCard, AppPageHeader } from '@/components'
import { useAppStore, type ThemeMode } from '@/stores/app'
import { SUPPORTED_LOCALES, type AppLocale } from '@/locales'
import { getSystemInfo, type SystemInfo } from '@/api/modules/system'
import { ApiError } from '@/api/types'

const { t, d } = useI18n()
const appStore = useAppStore()

const themeModes: ThemeMode[] = ['light', 'dark', 'system']
const localeLabels: Record<AppLocale, string> = {
  'zh-CN': '中文',
  'en-US': 'EN',
}

const info = ref<SystemInfo | null>(null)
const loading = ref(true)
const errorKey = ref<string | null>(null)

async function loadSystemInfo() {
  loading.value = true
  errorKey.value = null
  try {
    info.value = await getSystemInfo()
  } catch (error) {
    info.value = null
    errorKey.value = error instanceof ApiError ? error.messageKey : 'error.unknown'
  } finally {
    loading.value = false
  }
}

onMounted(loadSystemInfo)
</script>

<template>
  <div class="page">
    <AppPageHeader :title="t('settings.title')" />

    <section class="section">
      <h2 class="section-title">{{ t('settings.appearance.title') }}</h2>
      <AppCard variant="flat" class="control-card">
        <p class="control-desc">{{ t('settings.appearance.desc') }}</p>
        <div class="chip-group" role="radiogroup" :aria-label="t('common.theme.label')">
          <button
            v-for="mode in themeModes"
            :key="mode"
            type="button"
            class="chip"
            :class="{ active: appStore.themeMode === mode }"
            @click="appStore.setThemeMode(mode)"
          >
            {{ t(`common.theme.${mode}`) }}
          </button>
        </div>
      </AppCard>
    </section>

    <section class="section">
      <h2 class="section-title">{{ t('settings.language.title') }}</h2>
      <AppCard variant="flat" class="control-card">
        <p class="control-desc">{{ t('settings.language.desc') }}</p>
        <div class="chip-group" role="radiogroup" :aria-label="t('common.language')">
          <button
            v-for="loc in SUPPORTED_LOCALES"
            :key="loc"
            type="button"
            class="chip"
            :class="{ active: appStore.locale === loc }"
            @click="appStore.setLocale(loc)"
          >
            {{ localeLabels[loc] }}
          </button>
        </div>
      </AppCard>
    </section>

    <section class="section">
      <h2 class="section-title">{{ t('settings.system.title') }}</h2>
      <AppCard variant="flat">
        <div v-if="loading" class="status">
          <span class="status-dot pending"></span>
          <span>{{ t('common.loading') }}…</span>
        </div>
        <div v-else-if="info" class="status-block">
          <div class="status">
            <span class="status-dot ok"></span>
            <span>{{ t('settings.system.connected') }}</span>
          </div>
          <dl class="status-details">
            <div class="status-row">
              <dt>{{ t('settings.system.profile') }}</dt>
              <dd>{{ info.activeProfiles.join(', ') }}</dd>
            </div>
            <div class="status-row">
              <dt>{{ t('settings.system.serverTime') }}</dt>
              <dd>{{ d(new Date(info.serverTime), 'long') }}</dd>
            </div>
          </dl>
        </div>
        <div v-else class="status-block">
          <div class="status">
            <span class="status-dot error"></span>
            <span>{{ t('settings.system.disconnected') }}</span>
          </div>
          <p class="status-error-detail">{{ errorKey ? t(errorKey) : '' }}</p>
          <AppButton size="sm" variant="soft" @click="loadSystemInfo">
            {{ t('common.retry') }}
          </AppButton>
        </div>
      </AppCard>
    </section>

    <section class="section">
      <h2 class="section-title">{{ t('settings.about.title') }}</h2>
      <AppCard variant="flat">
        <div class="about-row">
          <span>{{ t('app.name') }}</span>
          <span class="about-version">{{ t('settings.about.version') }} 0.5.0</span>
        </div>
      </AppCard>
    </section>
  </div>
</template>

<style scoped>
.page {
  max-width: 640px;
  margin: 0 auto;
  padding: var(--space-8);
}

.section {
  margin-bottom: var(--space-6);
}

.section-title {
  margin: 0 0 var(--space-3);
  font-size: var(--text-sm);
  font-weight: 600;
  letter-spacing: var(--tracking-wide);
  text-transform: uppercase;
  color: var(--color-text-tertiary);
}

.control-card :deep(.card-body) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.control-desc {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.chip-group {
  display: flex;
  gap: 2px;
  padding: 2px;
  flex-shrink: 0;
  border-radius: var(--radius-md);
  background-color: var(--color-surface-hover);
}

.chip {
  padding: var(--space-1) var(--space-3);
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  font-family: inherit;
  font-size: var(--text-xs);
  font-weight: 500;
  color: var(--color-text-tertiary);
  cursor: pointer;
  transition:
    background-color var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out);
}

.chip.active {
  background-color: var(--color-surface);
  color: var(--color-text);
  box-shadow: var(--shadow-sm);
}

.status {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-base);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: var(--radius-full);
  flex-shrink: 0;
}

.status-dot.ok {
  background-color: var(--color-success);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-success) 20%, transparent);
}

.status-dot.error {
  background-color: var(--color-danger);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-danger) 20%, transparent);
}

.status-dot.pending {
  background-color: var(--color-text-tertiary);
  animation: pulse 1.2s ease-in-out infinite;
}

@keyframes pulse {
  50% {
    opacity: 0.3;
  }
}

.status-block {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  align-items: flex-start;
}

.status-details {
  margin: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.status-row {
  display: flex;
  justify-content: space-between;
  font-size: var(--text-sm);
}

.status-row dt {
  color: var(--color-text-tertiary);
}

.status-row dd {
  margin: 0;
  color: var(--color-text-secondary);
  font-family: var(--font-mono);
}

.status-error-detail {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
}

.about-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: var(--text-sm);
}

.about-version {
  color: var(--color-text-tertiary);
  font-family: var(--font-mono);
}

@media (max-width: 640px) {
  .page {
    padding: var(--space-5);
  }
}
</style>
