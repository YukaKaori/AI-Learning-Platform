<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { AppButton, AppIcon, AppPageHeader, AppTag } from '@/components'
import { getSubject } from '@/features/subjects/mock'
import { accentColor } from '@/features/subjects/types'
import { mockTasks } from '@/features/tasks/mock'
import { sessionsOfDay } from './mock'

const { t, d } = useI18n()

type ViewMode = 'week' | 'month'
const mode = ref<ViewMode>('week')

const DAY = 86_400_000
function startOfDay(date: Date): number {
  const d = new Date(date)
  d.setHours(0, 0, 0, 0)
  return d.getTime()
}
function startOfWeek(anchor: number): number {
  const d = new Date(anchor)
  const diff = (d.getDay() + 6) % 7 // Monday-first
  return startOfDay(new Date(d.getTime() - diff * DAY))
}

const anchor = ref(startOfDay(new Date()))
const today = startOfDay(new Date())

const weekStart = computed(() => startOfWeek(anchor.value))
const weekDays = computed(() =>
  Array.from({ length: 7 }, (_, i) => weekStart.value + i * DAY),
)

const monthStart = computed(() => {
  const d = new Date(anchor.value)
  return new Date(d.getFullYear(), d.getMonth(), 1).getTime()
})
const monthGrid = computed(() => {
  const gridStart = startOfWeek(monthStart.value)
  return Array.from({ length: 42 }, (_, i) => gridStart + i * DAY)
})
const currentMonth = computed(() => new Date(monthStart.value).getMonth())

function shift(delta: number) {
  const d = new Date(anchor.value)
  if (mode.value === 'week') {
    anchor.value = startOfDay(new Date(d.getTime() + delta * 7 * DAY))
  } else {
    anchor.value = new Date(d.getFullYear(), d.getMonth() + delta, 1).getTime()
  }
}

function goToday() {
  anchor.value = today
}

function tasksOn(dayStart: number): typeof mockTasks {
  const end = dayStart + DAY
  return mockTasks.filter(
    (task) => task.dueAt !== undefined && task.dueAt >= dayStart && task.dueAt < end,
  )
}

function subjectAccent(subjectId?: string): string {
  const subject = subjectId ? getSubject(subjectId) : undefined
  return subject ? accentColor(subject.accent) : 'var(--color-muted)'
}

const weekdayLabels = computed(() =>
  weekDays.value.map((day) => new Intl.DateTimeFormat(undefined, { weekday: 'short' }).format(day)),
)
</script>

<template>
  <div class="page">
    <AppPageHeader :title="t('calendar.title')">
      <template #actions>
        <div class="toolbar">
          <div class="mode-switch">
            <button
              type="button"
              class="mode-btn"
              :class="{ active: mode === 'week' }"
              @click="mode = 'week'"
            >
              {{ t('calendar.week') }}
            </button>
            <button
              type="button"
              class="mode-btn"
              :class="{ active: mode === 'month' }"
              @click="mode = 'month'"
            >
              {{ t('calendar.month') }}
            </button>
          </div>
          <AppButton variant="soft" size="sm" @click="goToday">{{ t('calendar.today') }}</AppButton>
          <AppButton
            variant="ghost"
            tone="secondary"
            size="sm"
            icon-left="chevron-left"
            :aria-label="t('calendar.prev')"
            @click="shift(-1)"
          />
          <AppButton
            variant="ghost"
            tone="secondary"
            size="sm"
            icon-left="chevron-right"
            :aria-label="t('calendar.next')"
            @click="shift(1)"
          />
        </div>
      </template>
    </AppPageHeader>

    <!-- Week view -->
    <div v-if="mode === 'week'" class="week-grid">
      <div v-for="dayStart in weekDays" :key="dayStart" class="day-column" :class="{ today: dayStart === today }">
        <div class="day-head">
          <span class="day-weekday">{{ weekdayLabels[weekDays.indexOf(dayStart)] }}</span>
          <span class="day-number">{{ d(dayStart, 'short') }}</span>
        </div>

        <div class="day-body">
          <div
            v-for="session in sessionsOfDay(dayStart)"
            :key="session.id"
            class="session-block"
            :style="{ borderColor: subjectAccent(session.subjectId) }"
          >
            <span class="session-time">{{ d(session.startsAt, 'time') }}</span>
            <span class="session-title">
              {{ session.title || getSubject(session.subjectId || '')?.name }}
            </span>
          </div>

          <div v-for="task in tasksOn(dayStart)" :key="task.id" class="task-chip">
            <AppIcon name="list-todo" size="sm" />
            <span>{{ task.title }}</span>
          </div>

          <p
            v-if="sessionsOfDay(dayStart).length === 0 && tasksOn(dayStart).length === 0"
            class="day-empty"
          >
            {{ t('calendar.empty') }}
          </p>
        </div>
      </div>
    </div>

    <!-- Month view -->
    <div v-else class="month-grid">
      <span v-for="label in weekdayLabels" :key="label" class="month-weekday">{{ label }}</span>
      <div
        v-for="dayStart in monthGrid"
        :key="dayStart"
        class="month-cell"
        :class="{
          today: dayStart === today,
          outside: new Date(dayStart).getMonth() !== currentMonth,
        }"
      >
        <span class="month-day-number">{{ new Date(dayStart).getDate() }}</span>
        <div class="month-dots">
          <span
            v-for="session in sessionsOfDay(dayStart).slice(0, 3)"
            :key="session.id"
            class="month-dot"
            :style="{ backgroundColor: subjectAccent(session.subjectId) }"
          ></span>
          <AppTag v-if="tasksOn(dayStart).length > 0" size="sm" tone="warning">
            {{ tasksOn(dayStart).length }}
          </AppTag>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  max-width: 1280px;
  margin: 0 auto;
  padding: var(--space-8);
}

.toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.mode-switch {
  display: flex;
  gap: 2px;
  padding: 2px;
  border-radius: var(--radius-md);
  background-color: var(--color-surface-hover);
}

.mode-btn {
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

.mode-btn.active {
  background-color: var(--color-surface);
  color: var(--color-text);
  box-shadow: var(--shadow-sm);
}

/* Week view */
.week-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: var(--space-3);
}

.day-column {
  min-height: 320px;
  border: var(--border-width-sm) solid var(--color-border);
  border-radius: var(--radius-card);
  background-color: var(--color-surface);
  overflow: hidden;
}

.day-column.today {
  border-color: var(--color-primary);
}

.day-head {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: var(--space-3) 0;
  border-bottom: var(--border-width-sm) solid var(--color-border);
}

.day-weekday {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

.day-number {
  font-size: var(--text-sm);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.today .day-number {
  color: var(--color-primary);
}

.day-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-2);
}

.session-block {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: var(--space-2);
  border-left: 3px solid;
  border-radius: var(--radius-sm);
  background-color: var(--color-surface-hover);
}

.session-time {
  font-size: var(--text-xs);
  font-variant-numeric: tabular-nums;
  color: var(--color-text-tertiary);
}

.session-title {
  font-size: var(--text-xs);
  font-weight: 500;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-chip {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  background-color: var(--color-warning-soft);
  color: var(--color-warning);
  font-size: var(--text-xs);
}

.task-chip span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.day-empty {
  margin: var(--space-4) 0 0;
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  text-align: center;
}

/* Month view */
.month-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: var(--space-2);
}

.month-weekday {
  padding-bottom: var(--space-2);
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  text-align: center;
}

.month-cell {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  min-height: 90px;
  padding: var(--space-2);
  border: var(--border-width-sm) solid var(--color-border);
  border-radius: var(--radius-md);
  background-color: var(--color-surface);
}

.month-cell.today {
  border-color: var(--color-primary);
}

.month-cell.outside {
  opacity: 0.4;
}

.month-day-number {
  font-size: var(--text-xs);
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-secondary);
}

.today .month-day-number {
  color: var(--color-primary);
  font-weight: 600;
}

.month-dots {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}

.month-dot {
  width: 6px;
  height: 6px;
  border-radius: var(--radius-full);
}

@media (max-width: 900px) {
  .week-grid {
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
    overflow-x: auto;
  }
}

@media (max-width: 640px) {
  .page {
    padding: var(--space-5);
  }

  .month-cell {
    min-height: 56px;
  }
}
</style>
