<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { AppButton, AppCard, AppIcon, AppTag, type IconName } from '@/components'
import { useAuthStore } from '@/stores/auth'
import { useDuration } from '@/composables/useDuration'
import { accentColor } from '@/features/subjects/types'
import { getSubject } from '@/features/subjects/mock'
import { tasksDueToday } from '@/features/tasks/mock'
import { sessionsOfDay } from '@/features/calendar/mock'
import { mockNotes } from '@/features/notes/mock'
import { mockConversations } from '@/features/ai-tutor/mock'
import { continueLearning, dashboardStats } from './mock'

const { t, d } = useI18n()
const router = useRouter()
const authStore = useAuthStore()
const { formatMinutes } = useDuration()

const userName = computed(
  () => authStore.user?.nickname || authStore.user?.username || '',
)

const greetingKey = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return 'workspace.greeting.morning'
  if (hour < 18) return 'workspace.greeting.afternoon'
  return 'workspace.greeting.evening'
})

interface StatTile {
  icon: IconName
  labelKey: string
  value: string
}

const stats = computed<StatTile[]>(() => [
  {
    icon: 'flame',
    labelKey: 'workspace.stats.streak',
    value: t('workspace.stats.streakUnit', { n: dashboardStats.streakDays }),
  },
  {
    icon: 'clock',
    labelKey: 'workspace.stats.studyToday',
    value: formatMinutes(dashboardStats.studiedTodayMinutes),
  },
  {
    icon: 'layers',
    labelKey: 'workspace.stats.dueCards',
    value: t('workspace.stats.dueCardsUnit', { n: dashboardStats.dueCards }),
  },
  {
    icon: 'book-open',
    labelKey: 'workspace.stats.activeSubjects',
    value: t('workspace.stats.activeSubjectsUnit', { n: dashboardStats.activeSubjects }),
  },
])

const goalPercent = computed(() =>
  Math.min(
    100,
    Math.round((dashboardStats.studiedTodayMinutes / dashboardStats.todayGoalMinutes) * 100),
  ),
)

const todayTasks = tasksDueToday().filter((task) => task.status !== 'done')
const todaySessions = sessionsOfDay(new Date(new Date().setHours(0, 0, 0, 0)).getTime())
const recentNotes = [...mockNotes].sort((a, b) => b.updatedAt - a.updatedAt).slice(0, 3)
const recentChats = [...mockConversations].sort((a, b) => b.updatedAt - a.updatedAt).slice(0, 3)

function subjectAccent(subjectId?: string): string {
  const subject = subjectId ? getSubject(subjectId) : undefined
  return subject ? accentColor(subject.accent) : 'var(--color-muted)'
}

function relativeLabel(epoch: number): string {
  return d(epoch, 'short')
}
</script>

<template>
  <div class="workspace">
    <div class="main">
      <header class="greeting">
        <h1 class="greeting-title">{{ t(greetingKey, { name: userName }) }}</h1>
        <p class="greeting-subtitle">{{ t('workspace.subtitle') }}</p>
      </header>

      <div class="stat-row">
        <AppCard v-for="stat in stats" :key="stat.labelKey" variant="flat" class="stat-tile">
          <span class="stat-icon"><AppIcon :name="stat.icon" /></span>
          <span class="stat-value">{{ stat.value }}</span>
          <span class="stat-label">{{ t(stat.labelKey) }}</span>
        </AppCard>
      </div>

      <section class="section">
        <div class="section-head">
          <h2 class="section-title">{{ t('workspace.continueLearning.title') }}</h2>
          <RouterLink :to="{ name: 'subjects' }" class="section-link">
            {{ t('common.viewAll') }}
            <AppIcon name="arrow-right" size="sm" />
          </RouterLink>
        </div>
        <div class="continue-grid">
          <AppCard
            v-for="subject in continueLearning"
            :key="subject.id"
            variant="flat"
            interactive
            class="continue-card"
            @click="router.push({ name: 'subject-detail', params: { id: subject.id } })"
          >
            <div class="continue-head">
              <span
                class="continue-icon"
                :style="{ color: accentColor(subject.accent) }"
                aria-hidden="true"
              >
                <AppIcon :name="subject.icon" />
              </span>
              <span class="continue-progress">{{ subject.progress }}%</span>
            </div>
            <h3 class="continue-name">{{ subject.name }}</h3>
            <span class="continue-meta">
              {{ t('workspace.continueLearning.lastStudied', { time: relativeLabel(subject.lastStudiedAt) }) }}
            </span>
            <div class="progress-track">
              <div
                class="progress-fill"
                :style="{
                  width: `${subject.progress}%`,
                  backgroundColor: accentColor(subject.accent),
                }"
              ></div>
            </div>
          </AppCard>
        </div>
      </section>

      <div class="two-col">
        <section class="section">
          <div class="section-head">
            <h2 class="section-title">{{ t('workspace.todayTasks.title') }}</h2>
            <RouterLink :to="{ name: 'calendar' }" class="section-link">
              {{ t('common.viewAll') }}
              <AppIcon name="arrow-right" size="sm" />
            </RouterLink>
          </div>
          <AppCard variant="flat" :padded="false">
            <p v-if="todayTasks.length === 0" class="empty-line">
              {{ t('workspace.todayTasks.empty') }}
            </p>
            <ul v-else class="row-list">
              <li v-for="task in todayTasks" :key="task.id" class="row">
                <span class="row-dot" :style="{ backgroundColor: subjectAccent(task.subjectId) }"></span>
                <span class="row-text">{{ task.title }}</span>
                <AppTag
                  v-if="task.priority === 'high'"
                  size="sm"
                  tone="warning"
                >{{ t('calendar.taskDue') }}</AppTag>
              </li>
            </ul>
          </AppCard>
        </section>

        <section class="section">
          <div class="section-head">
            <h2 class="section-title">{{ t('workspace.recentChats.title') }}</h2>
            <RouterLink :to="{ name: 'ai-tutor' }" class="section-link">
              {{ t('common.viewAll') }}
              <AppIcon name="arrow-right" size="sm" />
            </RouterLink>
          </div>
          <AppCard variant="flat" :padded="false">
            <p v-if="recentChats.length === 0" class="empty-line">
              {{ t('workspace.recentChats.empty') }}
            </p>
            <ul v-else class="row-list">
              <li
                v-for="conv in recentChats"
                :key="conv.id"
                class="row row-clickable"
                @click="router.push({ name: 'ai-tutor', params: { conversationId: conv.id } })"
              >
                <span class="row-icon"><AppIcon name="message-square" size="sm" /></span>
                <span class="row-text">{{ conv.title }}</span>
                <span class="row-meta">{{ relativeLabel(conv.updatedAt) }}</span>
              </li>
            </ul>
          </AppCard>
        </section>
      </div>

      <section class="section">
        <div class="section-head">
          <h2 class="section-title">{{ t('workspace.recentNotes.title') }}</h2>
          <RouterLink :to="{ name: 'notes' }" class="section-link">
            {{ t('common.viewAll') }}
            <AppIcon name="arrow-right" size="sm" />
          </RouterLink>
        </div>
        <div class="note-grid">
          <AppCard
            v-for="note in recentNotes"
            :key="note.id"
            variant="flat"
            interactive
            @click="router.push({ name: 'notes', query: { note: note.id } })"
          >
            <div class="note-head">
              <span
                class="row-dot"
                :style="{ backgroundColor: subjectAccent(note.subjectId) }"
              ></span>
              <h3 class="note-title">{{ note.title }}</h3>
            </div>
            <span class="row-meta">
              {{ t('notes.updated', { time: relativeLabel(note.updatedAt) }) }}
            </span>
          </AppCard>
        </div>
      </section>
    </div>

    <aside class="right-panel">
      <AppCard variant="flat" class="panel-card">
        <div class="goal-head">
          <h2 class="panel-title">{{ t('workspace.goal.title') }}</h2>
          <AppIcon name="target" size="sm" class="panel-title-icon" />
        </div>
        <div class="goal-value">
          {{
            t('workspace.goal.progress', {
              done: dashboardStats.studiedTodayMinutes,
              goal: dashboardStats.todayGoalMinutes,
            })
          }}
        </div>
        <div class="progress-track goal-track">
          <div class="progress-fill goal-fill" :style="{ width: `${goalPercent}%` }"></div>
        </div>
        <span v-if="goalPercent >= 100" class="goal-reached">
          <AppIcon name="check-circle" size="sm" />
          {{ t('workspace.goal.reached') }}
        </span>
      </AppCard>

      <AppCard variant="flat" class="panel-card">
        <h2 class="panel-title">{{ t('workspace.rightPanel.today') }}</h2>
        <p v-if="todaySessions.length === 0" class="empty-line no-pad">
          {{ t('workspace.rightPanel.todayEmpty') }}
        </p>
        <ul v-else class="schedule-list">
          <li v-for="session in todaySessions" :key="session.id" class="schedule-row">
            <span
              class="schedule-bar"
              :style="{ backgroundColor: subjectAccent(session.subjectId) }"
            ></span>
            <div class="schedule-main">
              <span class="schedule-title">
                {{ session.title || getSubject(session.subjectId || '')?.name }}
              </span>
              <span class="schedule-time">
                {{ d(session.startsAt, 'time') }} – {{ d(session.endsAt, 'time') }}
              </span>
            </div>
          </li>
        </ul>
      </AppCard>

      <AppCard variant="flat" class="panel-card">
        <h2 class="panel-title">{{ t('workspace.rightPanel.due') }}</h2>
        <div class="due-value">
          {{ t('workspace.stats.dueCardsUnit', { n: dashboardStats.dueCards }) }}
        </div>
        <AppButton
          variant="soft"
          size="sm"
          block
          icon-left="layers"
          @click="router.push({ name: 'flashcards' })"
        >
          {{ t('workspace.rightPanel.dueAction') }}
        </AppButton>
      </AppCard>

      <AppCard variant="glass" class="panel-card ask-ai">
        <span class="ask-ai-icon"><AppIcon name="sparkles" /></span>
        <h2 class="panel-title">{{ t('workspace.rightPanel.askAi') }}</h2>
        <p class="ask-ai-hint">{{ t('workspace.rightPanel.askAiHint') }}</p>
        <AppButton size="sm" block icon-left="bot" @click="router.push({ name: 'ai-tutor' })">
          {{ t('workspace.rightPanel.askAiAction') }}
        </AppButton>
      </AppCard>
    </aside>
  </div>
</template>

<style scoped>
.workspace {
  display: flex;
  gap: var(--space-8);
  max-width: 1400px;
  margin: 0 auto;
  padding: var(--space-8);
}

.main {
  flex: 1;
  min-width: 0;
}

.greeting {
  margin-bottom: var(--space-8);
}

.greeting-title {
  margin: 0;
  font-family: var(--font-headline-family);
  font-size: var(--font-headline-size);
  font-weight: var(--font-headline-weight);
  letter-spacing: var(--font-headline-tracking);
}

.greeting-subtitle {
  margin: var(--space-1) 0 0;
  font-size: var(--font-body-size);
  color: var(--color-text-secondary);
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-10);
}

.stat-tile :deep(.card-body) {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  margin-bottom: var(--space-2);
  border-radius: var(--radius-sm);
  color: var(--color-primary);
  background-color: var(--color-primary-soft);
}

.stat-value {
  font-size: var(--text-xl);
  font-weight: 650;
  font-variant-numeric: tabular-nums;
  letter-spacing: var(--tracking-tight);
}

.stat-label {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

.section {
  margin-bottom: var(--space-10);
  min-width: 0;
}

.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}

.section-title {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: 600;
  letter-spacing: var(--tracking-tight);
}

.section-link {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  transition: color var(--duration-fast) var(--ease-out);
}

.section-link:hover {
  color: var(--color-primary);
}

.continue-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--space-4);
}

.continue-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-3);
}

.continue-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: var(--radius-md);
  background-color: color-mix(in srgb, currentColor 12%, transparent);
}

.continue-progress {
  font-size: var(--text-xs);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-secondary);
}

.continue-name {
  margin: 0 0 var(--space-1);
  font-size: var(--text-base);
  font-weight: 600;
}

.continue-meta {
  display: block;
  margin-bottom: var(--space-3);
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

.progress-track {
  height: 4px;
  border-radius: var(--radius-full);
  background-color: var(--color-muted-soft);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: var(--radius-full);
  transition: width var(--duration-slow) var(--ease-out);
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-6);
}

.row-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
}

.row + .row {
  border-top: var(--border-width-sm) solid var(--color-border);
}

.row-clickable {
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-out);
}

.row-clickable:hover {
  background-color: var(--color-surface-hover);
}

.row-dot {
  width: 8px;
  height: 8px;
  flex-shrink: 0;
  border-radius: var(--radius-full);
}

.row-icon {
  display: flex;
  color: var(--color-text-tertiary);
}

.row-text {
  flex: 1;
  min-width: 0;
  font-size: var(--text-sm);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-meta {
  flex-shrink: 0;
  font-size: var(--text-xs);
  font-variant-numeric: tabular-nums;
  color: var(--color-text-tertiary);
}

.empty-line {
  margin: 0;
  padding: var(--space-6) var(--space-4);
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  text-align: center;
}

.empty-line.no-pad {
  padding: var(--space-2) 0 0;
  text-align: left;
}

.note-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: var(--space-4);
}

.note-head {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}

.note-title {
  margin: 0;
  font-size: var(--text-sm);
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Right panel — the workspace context column */
.right-panel {
  width: 300px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.panel-card :deep(.card-body) {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.panel-title {
  margin: 0;
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-text);
}

.goal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title-icon {
  color: var(--color-text-tertiary);
}

.goal-value {
  font-size: var(--text-xl);
  font-weight: 650;
  font-variant-numeric: tabular-nums;
  letter-spacing: var(--tracking-tight);
}

.goal-track {
  height: 6px;
}

.goal-fill {
  background-color: var(--color-primary);
}

.goal-reached {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-xs);
  color: var(--color-success);
}

.schedule-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.schedule-row {
  display: flex;
  gap: var(--space-3);
}

.schedule-bar {
  width: 3px;
  border-radius: var(--radius-full);
  flex-shrink: 0;
}

.schedule-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.schedule-title {
  font-size: var(--text-sm);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-time {
  font-size: var(--text-xs);
  font-variant-numeric: tabular-nums;
  color: var(--color-text-tertiary);
}

.due-value {
  font-size: var(--text-xl);
  font-weight: 650;
  font-variant-numeric: tabular-nums;
}

.ask-ai-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: var(--radius-md);
  color: var(--color-primary);
  background-color: var(--color-primary-soft);
}

.ask-ai-hint {
  margin: 0;
  font-size: var(--text-xs);
  line-height: var(--leading-normal);
  color: var(--color-text-secondary);
}

@media (max-width: 1280px) {
  .right-panel {
    display: none;
  }
}

@media (max-width: 900px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .two-col {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .workspace {
    padding: var(--space-5);
  }
}
</style>
