<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getSystemInfo, type SystemInfo } from '@/api/modules/system'
import { ApiError } from '@/api/types'

const { t, d } = useI18n()

const info = ref<SystemInfo | null>(null)
const loading = ref(true)
const errorKey = ref<string | null>(null)

async function load() {
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

onMounted(load)
</script>

<template>
  <div class="page">
    <header class="page-header">
      <h1 class="page-title">{{ t('home.title') }}</h1>
      <p class="page-subtitle">{{ t('home.subtitle') }}</p>
    </header>

    <section class="card">
      <h2 class="card-title">{{ t('home.status.title') }}</h2>

      <div v-if="loading" class="status">
        <span class="status-dot pending"></span>
        <span>{{ t('common.loading') }}…</span>
      </div>

      <div v-else-if="info" class="status-block">
        <div class="status">
          <span class="status-dot ok"></span>
          <span>{{ t('home.status.connected') }}</span>
        </div>
        <dl class="status-details">
          <div class="status-row">
            <dt>{{ t('home.status.profile') }}</dt>
            <dd>{{ info.activeProfiles.join(', ') }}</dd>
          </div>
          <div class="status-row">
            <dt>{{ t('home.status.serverTime') }}</dt>
            <dd>{{ d(new Date(info.serverTime), 'long') }}</dd>
          </div>
        </dl>
      </div>

      <div v-else class="status-block">
        <div class="status">
          <span class="status-dot error"></span>
          <span>{{ t('home.status.disconnected') }}</span>
        </div>
        <p class="status-error-detail">{{ errorKey ? t(errorKey) : '' }}</p>
        <el-button size="small" @click="load">{{ t('common.retry') }}</el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.page {
  max-width: 760px;
  margin: 0 auto;
  padding: var(--space-12) var(--space-8);
}

.page-header {
  margin-bottom: var(--space-8);
}

.page-title {
  margin: 0;
  font-size: var(--text-2xl);
  font-weight: 600;
  letter-spacing: -0.02em;
  line-height: var(--leading-tight);
}

.page-subtitle {
  margin: var(--space-2) 0 0;
  color: var(--color-text-secondary);
  font-size: var(--text-base);
}

.card {
  padding: var(--space-6);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background-color: var(--color-surface);
  box-shadow: var(--shadow-sm);
  transition:
    background-color var(--duration-base) var(--ease-out),
    border-color var(--duration-base) var(--ease-out);
}

.card-title {
  margin: 0 0 var(--space-4);
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
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
</style>
