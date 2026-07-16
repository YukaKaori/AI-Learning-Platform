<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import {
  AppButton,
  AppCard,
  AppDialog,
  AppEmpty,
  AppIcon,
  AppInput,
  AppSkeleton,
  AppTooltip,
  StatTile,
  type IconName,
} from '@/components'
import { getWorkspaceSummary } from '@/api/modules/workspace'
import { createNote } from '@/api/modules/note'
import { createTask, deleteTask as apiDeleteTask, updateTask } from '@/api/modules/task'
import type { TaskDto } from '@/api/modules/task'
import { deleteStudySession } from '@/api/modules/calendar'
import type { StudySessionDto } from '@/api/modules/calendar'
import { useAsync } from '@/composables/useAsync'
import { useDuration } from '@/composables/useDuration'
import { useAuthStore } from '@/stores/auth'
import { useSubjectsStore } from '@/stores/subjects'
import { accentColor, subjectAccentOf, subjectIconOf } from '@/features/subjects/types'
import TaskFormDialog from '@/features/tasks/components/TaskFormDialog.vue'
import SessionFormDialog from '@/features/calendar/components/SessionFormDialog.vue'

const { t, d, locale } = useI18n()
const router = useRouter()
const authStore = useAuthStore()
const subjectsStore = useSubjectsStore()
const { formatMinutes } = useDuration()

onMounted(() => {
  void subjectsStore.load()
})

// --- Aggregate summary (one round trip, one loading state — D7) -----------------

const { data: summary, loading, error, reload } = useAsync(getWorkspaceSummary)

const showSkeleton = computed(() => loading.value && summary.value === null)

// Working copy of the task list — the one section the view mutates in place
// (optimistic toggle). Every mutation still ends in `reload()` so the whole
// aggregate (stats, suggestions, focus) stays consistent.
const tasks = ref<TaskDto[]>([])

watch(summary, (value) => {
  tasks.value = value ? [...value.upcomingTasks] : []
})

// --- Header: greeting, Today's Focus, quick actions -----------------------------

const userName = computed(
  () => authStore.user?.nickname || authStore.user?.username || '',
)

const greetingKey = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return 'workspace.greeting.morning'
  if (hour < 18) return 'workspace.greeting.afternoon'
  return 'workspace.greeting.evening'
})

const DAY = 86_400_000
const todayStart = new Date(new Date().setHours(0, 0, 0, 0)).getTime()

/** Honest, rule-based focus line: today's earliest-due task → due cards → plan-your-day. */
const focusLine = computed<string | null>(() => {
  const stats = summary.value?.stats
  if (!stats) return null
  const dueToday = tasks.value.find(
    (task) =>
      task.status !== 'done' &&
      task.dueAt !== null &&
      task.dueAt >= todayStart &&
      task.dueAt < todayStart + DAY,
  )
  if (dueToday) return t('workspace.focus.task', { title: dueToday.title })
  if (stats.dueCards > 0) return t('workspace.focus.review', { n: stats.dueCards })
  return t('workspace.focus.fresh')
})

const creatingNote = ref(false)

/** Same semantics as NotesView's "new note": create untitled, then open it. */
async function newNote() {
  if (creatingNote.value) return
  creatingNote.value = true
  try {
    const created = await createNote({ title: t('notes.untitled'), content: '' })
    await router.push({ name: 'notes', query: { note: created.id } })
  } catch (caught) {
    console.error(caught)
  } finally {
    creatingNote.value = false
  }
}

// --- Study-goal ring -------------------------------------------------------------

const goalPercent = computed(() => {
  const stats = summary.value?.stats
  if (!stats || stats.dailyGoalMinutes <= 0) return 0
  return Math.min(100, Math.round((stats.studiedTodayMinutes / stats.dailyGoalMinutes) * 100))
})

// --- Knowledge Growth mini-chart (7 real days from the aggregate) ----------------

/** ISO `yyyy-MM-dd` is a calendar bucket — parse as a local date, never via Date(string). */
function chartDate(iso: string): Date {
  const [year, month, day] = iso.split('-').map(Number)
  return new Date(year!, month! - 1, day!)
}

const weekBars = computed(() => {
  const days = summary.value?.weekActivity ?? []
  const max = days.reduce((m, day) => Math.max(m, day.minutes), 0)
  // Direct-label selectively: only the (most recent) busiest day.
  const lastMaxIndex = days.reduce(
    (index, day, i) => (max > 0 && day.minutes === max ? i : index),
    -1,
  )
  return days.map((day, i) => ({
    date: day.date,
    minutes: day.minutes,
    heightPercent: max > 0 ? Math.max(4, Math.round((day.minutes / max) * 100)) : 0,
    showLabel: i === lastMaxIndex,
  }))
})

const weekTotalMinutes = computed(
  () => summary.value?.weekActivity.reduce((sum, day) => sum + day.minutes, 0) ?? 0,
)

const weekdayFormat = computed(
  () => new Intl.DateTimeFormat(locale.value, { weekday: 'short' }),
)

function barTooltip(bar: { date: string; minutes: number }): string {
  return `${d(chartDate(bar.date), 'short')} · ${formatMinutes(bar.minutes)}`
}

// --- Subject enrichment (store cache, same pattern as the calendar) ---------------

function subjectAccent(subjectId: string | null): string {
  const subject = subjectsStore.byId(subjectId)
  return subject ? accentColor(subjectAccentOf(subject.color)) : 'var(--color-muted)'
}

function sessionLabel(session: StudySessionDto): string {
  return session.title || subjectsStore.byId(session.subjectId)?.name || t('calendar.session')
}

// --- Upcoming tasks: quick-add, toggle, shared dialog -----------------------------

const quickTitle = ref('')
const quickAdding = ref(false)

async function quickAdd() {
  const title = quickTitle.value.trim()
  if (!title || quickAdding.value) return
  quickAdding.value = true
  try {
    await createTask({ title })
    quickTitle.value = ''
    await reload()
  } catch (caught) {
    // Keep the typed title so nothing is lost (toast feedback lands in C2).
    console.error(caught)
  } finally {
    quickAdding.value = false
  }
}

async function toggleTask(task: TaskDto) {
  const previous = task.status
  const next = task.status === 'done' ? 'todo' : 'done'
  task.status = next // optimistic; reload() reconciles the aggregate
  try {
    await updateTask(task.id, { status: next })
    await reload()
  } catch (caught) {
    task.status = previous
    console.error(caught)
  }
}

const taskDialogOpen = ref(false)
const editingTask = ref<TaskDto | null>(null)

function openEditTask(task: TaskDto) {
  editingTask.value = task
  taskDialogOpen.value = true
}

const taskDeleteTarget = ref<TaskDto | null>(null)

async function confirmDeleteTask() {
  const task = taskDeleteTarget.value
  if (!task) return
  try {
    await apiDeleteTask(task.id)
    await reload()
  } catch (caught) {
    console.error(caught)
  } finally {
    taskDeleteTarget.value = null
  }
}

function isOverdue(task: TaskDto): boolean {
  return task.status !== 'done' && task.dueAt !== null && task.dueAt < Date.now()
}

// --- Sessions: "Start session" quick action + shared dialog -----------------------

const sessionDialogOpen = ref(false)
const editingSession = ref<StudySessionDto | null>(null)

function openCreateSession() {
  editingSession.value = null
  sessionDialogOpen.value = true
}

function openEditSession(session: StudySessionDto) {
  editingSession.value = session
  sessionDialogOpen.value = true
}

const sessionDeleteTarget = ref<StudySessionDto | null>(null)

async function confirmDeleteSession() {
  const session = sessionDeleteTarget.value
  if (!session) return
  try {
    await deleteStudySession(session.id)
    await reload()
  } catch (caught) {
    console.error(caught)
  } finally {
    sessionDeleteTarget.value = null
  }
}

// --- AI Suggestions — honest rule-based nudges from the real aggregate ------------
// (A server-side AI version is a documented future extension, not built here.)

interface Nudge {
  id: string
  icon: IconName
  text: string
  actionLabel: string
  run: () => void
}

const suggestions = computed<Nudge[]>(() => {
  const stats = summary.value?.stats
  if (!stats) return []
  const nudges: Nudge[] = []
  if (stats.studiedTodayMinutes === 0 && stats.streakDays > 0) {
    nudges.push({
      id: 'streak-risk',
      icon: 'flame',
      text: t('workspace.suggestions.streakRisk', { n: stats.streakDays }),
      actionLabel: t('workspace.suggestions.streakRiskAction'),
      run: openCreateSession,
    })
  }
  if (stats.dueCards > 0) {
    nudges.push({
      id: 'due-cards',
      icon: 'layers',
      text: t('workspace.suggestions.dueCards', { n: stats.dueCards }),
      actionLabel: t('workspace.suggestions.dueCardsAction'),
      run: () => router.push({ name: 'flashcards' }),
    })
  }
  const overdue = tasks.value.filter(isOverdue).length
  if (overdue > 0) {
    nudges.push({
      id: 'overdue',
      icon: 'alert-circle',
      text: t('workspace.suggestions.overdue', { n: overdue }),
      actionLabel: t('workspace.suggestions.overdueAction'),
      run: () => router.push({ name: 'calendar' }),
    })
  }
  if (stats.activeSubjects === 0) {
    nudges.push({
      id: 'no-subjects',
      icon: 'book-open',
      text: t('workspace.suggestions.noSubjects'),
      actionLabel: t('workspace.suggestions.noSubjectsAction'),
      run: () => router.push({ name: 'subjects' }),
    })
  }
  return nudges.slice(0, 3)
})
</script>

<template>
  <div class="workspace">
    <!-- Header renders immediately — the greeting needs no data. -->
    <header class="hero">
      <div class="hero-text">
        <h1 class="hero-title">{{ t(greetingKey, { name: userName }) }}</h1>
        <p v-if="focusLine" class="hero-focus">
          <AppIcon name="target" size="sm" aria-hidden="true" />
          {{ focusLine }}
        </p>
      </div>
      <div class="hero-actions">
        <AppButton
          variant="soft"
          size="sm"
          icon-left="notebook-pen"
          :loading="creatingNote"
          @click="newNote"
        >
          {{ t('workspace.actions.newNote') }}
        </AppButton>
        <AppButton
          variant="soft"
          size="sm"
          icon-left="bot"
          @click="router.push({ name: 'ai-tutor' })"
        >
          {{ t('workspace.actions.askAi') }}
        </AppButton>
        <AppButton size="sm" icon-left="play" @click="openCreateSession">
          {{ t('workspace.actions.startSession') }}
        </AppButton>
      </div>
    </header>

    <!-- Loading -->
    <div v-if="showSkeleton" aria-hidden="true">
      <div class="stat-row">
        <AppSkeleton v-for="n in 4" :key="n" variant="block" height="104px" />
      </div>
      <div class="grid">
        <div class="col-main">
          <AppSkeleton variant="block" height="180px" />
          <AppSkeleton variant="block" height="220px" />
          <AppSkeleton variant="block" height="200px" />
        </div>
        <div class="col-side">
          <AppSkeleton variant="block" height="240px" />
          <AppSkeleton variant="block" height="160px" />
          <AppSkeleton variant="block" height="180px" />
        </div>
      </div>
    </div>

    <!-- Error -->
    <AppEmpty v-else-if="error" icon="alert-circle" :title="t(error.messageKey)">
      <template #action>
        <AppButton size="sm" variant="soft" @click="reload">{{ t('common.retry') }}</AppButton>
      </template>
    </AppEmpty>

    <template v-else-if="summary">
      <div class="stat-row">
        <StatTile
          icon="flame"
          :label="t('workspace.stats.streak')"
          :value="t('workspace.stats.streakUnit', { n: summary.stats.streakDays })"
        />
        <StatTile icon="target" :label="t('workspace.stats.studyToday')">
          <span class="goal-tile">
            <svg class="goal-ring" viewBox="0 0 36 36" aria-hidden="true">
              <circle class="goal-ring-track" cx="18" cy="18" r="15.5" pathLength="100" />
              <circle
                v-if="goalPercent > 0"
                class="goal-ring-fill"
                :class="{ reached: goalPercent >= 100 }"
                cx="18"
                cy="18"
                r="15.5"
                pathLength="100"
                :stroke-dasharray="`${goalPercent} ${100 - goalPercent}`"
              />
            </svg>
            {{
              t('workspace.stats.goalProgress', {
                done: summary.stats.studiedTodayMinutes,
                goal: summary.stats.dailyGoalMinutes,
              })
            }}
          </span>
        </StatTile>
        <StatTile
          icon="layers"
          :label="t('workspace.stats.dueCards')"
          :value="t('workspace.stats.dueCardsUnit', { n: summary.stats.dueCards })"
        />
        <StatTile
          icon="book-open"
          :label="t('workspace.stats.activeSubjects')"
          :value="t('workspace.stats.activeSubjectsUnit', { n: summary.stats.activeSubjects })"
        />
      </div>

      <div class="grid">
        <div class="col-main">
          <!-- Continue learning -->
          <section class="section">
            <div class="section-head">
              <h2 class="section-title">{{ t('workspace.continueLearning.title') }}</h2>
              <RouterLink :to="{ name: 'subjects' }" class="section-link">
                {{ t('common.viewAll') }}
                <AppIcon name="arrow-right" size="sm" />
              </RouterLink>
            </div>
            <div v-if="summary.continueLearning.length > 0" class="continue-grid">
              <AppCard
                v-for="item in summary.continueLearning"
                :key="item.id"
                variant="flat"
                interactive
                class="continue-card"
                @click="router.push({ name: 'subject-detail', params: { id: item.id } })"
              >
                <div class="continue-head">
                  <span
                    class="continue-icon"
                    :style="{ color: accentColor(subjectAccentOf(item.color)) }"
                    aria-hidden="true"
                  >
                    <AppIcon :name="subjectIconOf(item.icon)" />
                  </span>
                  <span class="continue-progress">{{ item.progress }}%</span>
                </div>
                <h3 class="continue-name">{{ item.name }}</h3>
                <span class="continue-meta">
                  {{ t('workspace.continueLearning.lastActive', { time: d(item.lastActivityAt, 'short') }) }}
                </span>
                <div class="progress-track">
                  <div
                    class="progress-fill"
                    :style="{
                      width: `${item.progress}%`,
                      backgroundColor: accentColor(subjectAccentOf(item.color)),
                    }"
                  ></div>
                </div>
              </AppCard>
            </div>
            <AppCard v-else variant="flat">
              <div class="section-empty">
                <AppIcon name="book-open" class="section-empty-icon" aria-hidden="true" />
                <p class="section-empty-text">{{ t('workspace.continueLearning.empty') }}</p>
                <AppButton size="sm" variant="soft" @click="router.push({ name: 'subjects' })">
                  {{ t('workspace.continueLearning.emptyCta') }}
                </AppButton>
              </div>
            </AppCard>
          </section>

          <div class="two-col">
            <!-- Recent AI conversations -->
            <section class="section">
              <div class="section-head">
                <h2 class="section-title">{{ t('workspace.recentChats.title') }}</h2>
                <RouterLink :to="{ name: 'ai-tutor' }" class="section-link">
                  {{ t('common.viewAll') }}
                  <AppIcon name="arrow-right" size="sm" />
                </RouterLink>
              </div>
              <AppCard variant="flat" :padded="false">
                <div v-if="summary.recentConversations.length === 0" class="section-empty">
                  <AppIcon name="message-square" class="section-empty-icon" aria-hidden="true" />
                  <p class="section-empty-text">{{ t('workspace.recentChats.empty') }}</p>
                  <AppButton
                    size="sm"
                    variant="soft"
                    @click="router.push({ name: 'ai-tutor' })"
                  >
                    {{ t('workspace.recentChats.emptyCta') }}
                  </AppButton>
                </div>
                <ul v-else class="row-list">
                  <li
                    v-for="conv in summary.recentConversations"
                    :key="conv.id"
                    class="row row-clickable"
                    @click="router.push({ name: 'ai-tutor', params: { conversationId: conv.id } })"
                  >
                    <span class="row-icon"><AppIcon name="message-square" size="sm" /></span>
                    <span class="row-text">{{ conv.title }}</span>
                    <span class="row-meta">{{ d(conv.updatedAt, 'short') }}</span>
                  </li>
                </ul>
              </AppCard>
            </section>

            <!-- Recent notes -->
            <section class="section">
              <div class="section-head">
                <h2 class="section-title">{{ t('workspace.recentNotes.title') }}</h2>
                <RouterLink :to="{ name: 'notes' }" class="section-link">
                  {{ t('common.viewAll') }}
                  <AppIcon name="arrow-right" size="sm" />
                </RouterLink>
              </div>
              <AppCard variant="flat" :padded="false">
                <div v-if="summary.recentNotes.length === 0" class="section-empty">
                  <AppIcon name="notebook-pen" class="section-empty-icon" aria-hidden="true" />
                  <p class="section-empty-text">{{ t('workspace.recentNotes.empty') }}</p>
                  <AppButton size="sm" variant="soft" :loading="creatingNote" @click="newNote">
                    {{ t('workspace.recentNotes.emptyCta') }}
                  </AppButton>
                </div>
                <ul v-else class="row-list">
                  <li
                    v-for="note in summary.recentNotes"
                    :key="note.id"
                    class="row row-clickable"
                    @click="router.push({ name: 'notes', query: { note: note.id } })"
                  >
                    <span
                      class="row-dot"
                      :style="{ backgroundColor: subjectAccent(note.subjectId) }"
                    ></span>
                    <span class="row-text">{{ note.title }}</span>
                    <span class="row-meta">{{ d(note.updatedAt, 'short') }}</span>
                  </li>
                </ul>
              </AppCard>
            </section>
          </div>

          <!-- Knowledge growth — 7 real days, one hue, tooltip per mark -->
          <section class="section">
            <div class="section-head">
              <h2 class="section-title">{{ t('workspace.growth.title') }}</h2>
              <span v-if="weekTotalMinutes > 0" class="section-link">
                {{ t('workspace.growth.weekTotal', { time: formatMinutes(weekTotalMinutes) }) }}
              </span>
            </div>
            <AppCard variant="flat">
              <div v-if="weekTotalMinutes > 0" class="growth-chart">
                <div class="chart-bars">
                  <AppTooltip
                    v-for="bar in weekBars"
                    :key="bar.date"
                    :content="barTooltip(bar)"
                  >
                    <div class="bar-slot" tabindex="0" :aria-label="barTooltip(bar)">
                      <span v-if="bar.showLabel" class="bar-label">
                        {{ formatMinutes(bar.minutes) }}
                      </span>
                      <span
                        class="bar"
                        :class="{ zero: bar.minutes === 0 }"
                        :style="bar.minutes > 0 ? { height: `${bar.heightPercent}%` } : undefined"
                      ></span>
                    </div>
                  </AppTooltip>
                </div>
                <div class="chart-days">
                  <span v-for="bar in weekBars" :key="bar.date" class="chart-day">
                    {{ weekdayFormat.format(chartDate(bar.date)) }}
                  </span>
                </div>
              </div>
              <div v-else class="section-empty">
                <AppIcon name="trending-up" class="section-empty-icon" aria-hidden="true" />
                <p class="section-empty-text">{{ t('workspace.growth.empty') }}</p>
                <AppButton size="sm" variant="soft" @click="openCreateSession">
                  {{ t('workspace.growth.emptyCta') }}
                </AppButton>
              </div>
            </AppCard>
          </section>
        </div>

        <aside class="col-side">
          <!-- Upcoming tasks + inline quick-add -->
          <AppCard variant="flat" class="panel-card">
            <div class="panel-head">
              <h2 class="panel-title">{{ t('workspace.upcomingTasks.title') }}</h2>
              <RouterLink :to="{ name: 'calendar' }" class="section-link">
                {{ t('common.viewAll') }}
                <AppIcon name="arrow-right" size="sm" />
              </RouterLink>
            </div>
            <p v-if="tasks.length === 0" class="panel-empty">
              {{ t('workspace.upcomingTasks.empty') }}
            </p>
            <ul v-else class="task-list">
              <li v-for="task in tasks" :key="task.id" class="task-row">
                <button
                  type="button"
                  class="task-toggle"
                  :class="{ done: task.status === 'done' }"
                  :aria-label="task.status === 'done' ? t('tasks.markTodo') : t('tasks.markDone')"
                  @click="toggleTask(task)"
                >
                  <AppIcon :name="task.status === 'done' ? 'check-circle' : 'circle'" size="sm" />
                </button>
                <button type="button" class="task-main" @click="openEditTask(task)">
                  <span class="task-title" :class="{ done: task.status === 'done' }">
                    {{ task.title }}
                  </span>
                  <span v-if="task.dueAt" class="task-due" :class="{ overdue: isOverdue(task) }">
                    {{ d(task.dueAt, 'short') }}
                  </span>
                </button>
                <span
                  v-if="task.subjectId"
                  class="row-dot"
                  :style="{ backgroundColor: subjectAccent(task.subjectId) }"
                ></span>
              </li>
            </ul>
            <form class="quick-add" @submit.prevent="quickAdd">
              <AppInput
                v-model="quickTitle"
                size="sm"
                :placeholder="t('workspace.upcomingTasks.quickAddPlaceholder')"
              />
              <AppButton
                type="submit"
                size="sm"
                variant="soft"
                icon-left="plus"
                :loading="quickAdding"
                :disabled="!quickTitle.trim()"
                :aria-label="t('workspace.upcomingTasks.quickAdd')"
              />
            </form>
          </AppCard>

          <!-- Today's sessions -->
          <AppCard variant="flat" class="panel-card">
            <div class="panel-head">
              <h2 class="panel-title">{{ t('workspace.todaySessions.title') }}</h2>
            </div>
            <div v-if="summary.todaySessions.length === 0" class="panel-empty-block">
              <p class="panel-empty">{{ t('workspace.todaySessions.empty') }}</p>
              <AppButton size="sm" variant="soft" icon-left="plus" @click="openCreateSession">
                {{ t('workspace.todaySessions.emptyCta') }}
              </AppButton>
            </div>
            <ul v-else class="schedule-list">
              <li v-for="session in summary.todaySessions" :key="session.id">
                <button type="button" class="schedule-row" @click="openEditSession(session)">
                  <span
                    class="schedule-bar"
                    :style="{ backgroundColor: subjectAccent(session.subjectId) }"
                  ></span>
                  <span class="schedule-main">
                    <span class="schedule-title">{{ sessionLabel(session) }}</span>
                    <span class="schedule-time">
                      {{ d(session.startsAt, 'time') }} – {{ d(session.endsAt, 'time') }}
                    </span>
                  </span>
                </button>
              </li>
            </ul>
          </AppCard>

          <!-- AI suggestions — honest rule-based nudges -->
          <AppCard variant="glass" class="panel-card">
            <div class="panel-head">
              <h2 class="panel-title suggest-title">
                <AppIcon name="sparkles" size="sm" aria-hidden="true" />
                {{ t('workspace.suggestions.title') }}
              </h2>
            </div>
            <ul v-if="suggestions.length > 0" class="suggest-list">
              <li v-for="nudge in suggestions" :key="nudge.id" class="suggest-row">
                <AppIcon :name="nudge.icon" size="sm" class="suggest-icon" aria-hidden="true" />
                <div class="suggest-main">
                  <p class="suggest-text">{{ nudge.text }}</p>
                  <button type="button" class="suggest-action" @click="nudge.run()">
                    {{ nudge.actionLabel }}
                    <AppIcon name="arrow-right" size="sm" />
                  </button>
                </div>
              </li>
            </ul>
            <div v-else class="suggest-row">
              <AppIcon name="check-circle" size="sm" class="suggest-icon" aria-hidden="true" />
              <div class="suggest-main">
                <p class="suggest-text">{{ t('workspace.suggestions.allClear') }}</p>
                <button
                  type="button"
                  class="suggest-action"
                  @click="router.push({ name: 'ai-tutor' })"
                >
                  {{ t('workspace.suggestions.allClearAction') }}
                  <AppIcon name="arrow-right" size="sm" />
                </button>
              </div>
            </div>
          </AppCard>
        </aside>
      </div>
    </template>

    <TaskFormDialog
      v-model="taskDialogOpen"
      :task="editingTask"
      @saved="reload"
      @delete="(task) => (taskDeleteTarget = task)"
    />

    <SessionFormDialog
      v-model="sessionDialogOpen"
      :session="editingSession"
      @saved="reload"
      @delete="(session) => (sessionDeleteTarget = session)"
    />

    <AppDialog
      :model-value="taskDeleteTarget !== null"
      :title="t('tasks.deleteConfirm.title')"
      width="420px"
      @update:model-value="(open) => { if (!open) taskDeleteTarget = null }"
    >
      <p>{{ t('tasks.deleteConfirm.body', { title: taskDeleteTarget?.title ?? '' }) }}</p>
      <template #footer>
        <AppButton variant="soft" tone="secondary" @click="taskDeleteTarget = null">
          {{ t('common.cancel') }}
        </AppButton>
        <AppButton tone="danger" @click="confirmDeleteTask">{{ t('common.delete') }}</AppButton>
      </template>
    </AppDialog>

    <AppDialog
      :model-value="sessionDeleteTarget !== null"
      :title="t('calendar.sessionDeleteConfirm.title')"
      width="420px"
      @update:model-value="(open) => { if (!open) sessionDeleteTarget = null }"
    >
      <p>{{ t('calendar.sessionDeleteConfirm.body') }}</p>
      <template #footer>
        <AppButton variant="soft" tone="secondary" @click="sessionDeleteTarget = null">
          {{ t('common.cancel') }}
        </AppButton>
        <AppButton tone="danger" @click="confirmDeleteSession">{{ t('common.delete') }}</AppButton>
      </template>
    </AppDialog>
  </div>
</template>

<style scoped>
.workspace {
  max-width: 1400px;
  margin: 0 auto;
  padding: var(--space-8);
}

/* Hero */
.hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-4);
  flex-wrap: wrap;
  margin-bottom: var(--space-8);
}

.hero-title {
  margin: 0;
  font-family: var(--font-headline-family);
  font-size: var(--font-headline-size);
  font-weight: var(--font-headline-weight);
  letter-spacing: var(--font-headline-tracking);
}

.hero-focus {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin: var(--space-2) 0 0;
  font-size: var(--font-body-size);
  color: var(--color-text-secondary);
}

.hero-focus :deep(.app-icon) {
  color: var(--color-primary);
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
}

/* Stat row */
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-8);
}

.goal-tile {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
}

.goal-ring {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  transform: rotate(-90deg);
}

.goal-ring-track,
.goal-ring-fill {
  fill: none;
  stroke-width: 4;
}

.goal-ring-track {
  stroke: var(--color-muted-soft);
}

.goal-ring-fill {
  stroke: var(--color-primary);
  stroke-linecap: round;
  transition: stroke-dasharray var(--duration-slow) var(--ease-out);
}

.goal-ring-fill.reached {
  stroke: var(--color-success);
}

/* 12-col grid */
.grid {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: var(--space-6);
  align-items: start;
}

.col-main {
  grid-column: span 8;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-8);
}

.col-side {
  grid-column: span 4;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

/* Sections */
.section {
  min-width: 0;
}

.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-3);
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

a.section-link:hover {
  color: var(--color-primary);
}

/* Designed section empty state — icon, line, CTA */
.section-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-6) var(--space-4);
  text-align: center;
}

.section-empty-icon {
  color: var(--color-text-tertiary);
}

.section-empty-text {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
}

/* Continue learning */
.continue-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
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
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

/* Row lists (conversations, notes) */
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

/* Knowledge growth chart — thin marks, one hue, recessive baseline */
.growth-chart {
  display: flex;
  flex-direction: column;
}

.chart-bars {
  display: flex;
  align-items: stretch;
  height: 120px;
  border-bottom: 1px solid var(--color-border);
}

.bar-slot {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-1);
  border-radius: var(--radius-sm);
  cursor: default;
}

.bar-slot:focus-visible {
  outline: 2px solid var(--color-focus-ring);
  outline-offset: 2px;
}

.bar {
  width: min(24px, 60%);
  border-radius: 4px 4px 0 0;
  background-color: var(--color-primary);
  transition:
    height var(--duration-slow) var(--ease-out),
    background-color var(--duration-fast) var(--ease-out);
}

.bar-slot:hover .bar:not(.zero) {
  background-color: var(--color-primary-hover);
}

.bar.zero {
  height: 3px;
  background-color: var(--color-muted-soft);
}

.bar-label {
  font-size: var(--text-xs);
  font-variant-numeric: tabular-nums;
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.chart-days {
  display: flex;
  padding-top: var(--space-2);
}

.chart-day {
  flex: 1;
  min-width: 0;
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Side panel cards */
.panel-card :deep(.card-body) {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.panel-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-3);
}

.panel-title {
  margin: 0;
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-text);
}

.panel-empty {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
}

.panel-empty-block {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-3);
}

/* Upcoming tasks */
.task-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
}

.task-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) 0;
}

.task-row + .task-row {
  border-top: var(--border-width-sm) solid var(--color-border);
}

.task-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--color-text-tertiary);
  cursor: pointer;
  flex-shrink: 0;
  transition: color var(--duration-fast) var(--ease-out);
}

.task-toggle:hover,
.task-toggle.done {
  color: var(--color-primary);
}

.task-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 2px;
  padding: 0;
  border: none;
  background: transparent;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
}

.task-title {
  font-size: var(--text-sm);
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-title.done {
  color: var(--color-text-tertiary);
  text-decoration: line-through;
}

.task-due {
  font-size: var(--text-xs);
  font-variant-numeric: tabular-nums;
  color: var(--color-text-tertiary);
}

.task-due.overdue {
  color: var(--color-danger);
}

.quick-add {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.quick-add :deep(.app-input-field) {
  flex: 1;
  min-width: 0;
}

/* Today's sessions */
.schedule-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.schedule-row {
  display: flex;
  gap: var(--space-3);
  width: 100%;
  padding: var(--space-1) 0;
  border: none;
  background: transparent;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: background-color var(--duration-fast) var(--ease-out);
}

.schedule-row:hover {
  background-color: var(--color-surface-hover);
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
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-time {
  font-size: var(--text-xs);
  font-variant-numeric: tabular-nums;
  color: var(--color-text-tertiary);
}

/* AI suggestions */
.suggest-title {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
}

.suggest-title :deep(.app-icon) {
  color: var(--color-primary);
}

.suggest-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.suggest-row {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
}

.suggest-icon {
  margin-top: 2px;
  flex-shrink: 0;
  color: var(--color-text-tertiary);
}

.suggest-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.suggest-text {
  margin: 0;
  font-size: var(--text-sm);
  line-height: var(--leading-normal);
  color: var(--color-text-secondary);
}

.suggest-action {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  align-self: flex-start;
  padding: 0;
  border: none;
  background: transparent;
  font-family: inherit;
  font-size: var(--text-xs);
  font-weight: 500;
  color: var(--color-primary);
  cursor: pointer;
  transition: color var(--duration-fast) var(--ease-out);
}

.suggest-action:hover {
  color: var(--color-primary-hover);
}

/* Responsive */
@media (max-width: 1100px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .col-main,
  .col-side {
    grid-column: auto;
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
