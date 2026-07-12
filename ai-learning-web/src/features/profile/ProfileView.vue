<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { AppAvatar, AppButton, AppCard, AppPageHeader } from '@/components'
import { useAuthStore } from '@/stores/auth'
import { useDuration } from '@/composables/useDuration'
import { mockSubjects } from '@/features/subjects/mock'
import { mockNotes } from '@/features/notes/mock'
import { dashboardStats } from '@/features/workspace/mock'

const { t, d } = useI18n()
const authStore = useAuthStore()
const { formatMinutes } = useDuration()

const displayName = computed(
  () => authStore.user?.nickname || authStore.user?.username || '',
)

const totalStudyMinutes = computed(() =>
  mockSubjects.reduce((sum, s) => sum + s.studyMinutes, 0),
)

// Demo-only: real accounts don't expose a creation date to the client yet.
const memberSince = Date.now() - 96 * 86_400_000
</script>

<template>
  <div class="page">
    <AppPageHeader :title="t('profile.title')" />

    <AppCard variant="flat" class="identity">
      <AppAvatar :src="authStore.user?.avatar ?? undefined" :name="displayName" size="lg" />
      <div class="identity-main">
        <h2 class="identity-name">{{ displayName }}</h2>
        <span class="identity-since">{{ t('profile.memberSince', { date: d(memberSince, 'monthYear') }) }}</span>
      </div>
      <AppButton variant="soft" size="sm" icon-left="pencil" disabled>
        {{ t('profile.account.edit') }}
      </AppButton>
    </AppCard>

    <section class="section">
      <h2 class="section-title">{{ t('profile.learningOverview') }}</h2>
      <div class="stat-row">
        <AppCard variant="flat" class="stat-tile">
          <span class="stat-value">{{ formatMinutes(totalStudyMinutes) }}</span>
          <span class="stat-label">{{ t('profile.totalStudy') }}</span>
        </AppCard>
        <AppCard variant="flat" class="stat-tile">
          <span class="stat-value">{{ mockSubjects.length }}</span>
          <span class="stat-label">{{ t('profile.subjectsCount') }}</span>
        </AppCard>
        <AppCard variant="flat" class="stat-tile">
          <span class="stat-value">{{ mockNotes.length }}</span>
          <span class="stat-label">{{ t('profile.notesCount') }}</span>
        </AppCard>
        <AppCard variant="flat" class="stat-tile">
          <span class="stat-value">{{ dashboardStats.streakDays }}</span>
          <span class="stat-label">{{ t('profile.streak') }}</span>
        </AppCard>
      </div>
    </section>

    <section class="section">
      <h2 class="section-title">{{ t('profile.account.title') }}</h2>
      <AppCard variant="flat" :padded="false">
        <dl class="detail-list">
          <div class="detail-row">
            <dt>{{ t('profile.account.username') }}</dt>
            <dd>{{ authStore.user?.username }}</dd>
          </div>
          <div class="detail-row">
            <dt>{{ t('profile.account.email') }}</dt>
            <dd>{{ authStore.user?.email }}</dd>
          </div>
          <div class="detail-row">
            <dt>{{ t('profile.account.nickname') }}</dt>
            <dd>{{ authStore.user?.nickname || '—' }}</dd>
          </div>
        </dl>
      </AppCard>
    </section>
  </div>
</template>

<style scoped>
.page {
  max-width: 760px;
  margin: 0 auto;
  padding: var(--space-8);
}

.identity {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  margin-bottom: var(--space-8);
}

.identity :deep(.card-body) {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  width: 100%;
}

.identity-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.identity-name {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: 600;
}

.identity-since {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

.section {
  margin-bottom: var(--space-8);
}

.section-title {
  margin: 0 0 var(--space-4);
  font-size: var(--text-sm);
  font-weight: 600;
  letter-spacing: var(--tracking-wide);
  text-transform: uppercase;
  color: var(--color-text-tertiary);
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
}

.stat-tile :deep(.card-body) {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.stat-value {
  font-size: var(--text-lg);
  font-weight: 650;
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

.detail-list {
  margin: 0;
}

.detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) var(--space-4);
}

.detail-row + .detail-row {
  border-top: var(--border-width-sm) solid var(--color-border);
}

.detail-row dt {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
}

.detail-row dd {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--color-text);
}

@media (max-width: 640px) {
  .page {
    padding: var(--space-5);
  }

  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
