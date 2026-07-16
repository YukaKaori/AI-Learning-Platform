<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import {
  AppButton,
  AppCard,
  AppEmpty,
  AppIcon,
  AppLoading,
  AppPageHeader,
  AppSkeleton,
  AppTooltip,
  StatTile,
} from '@/components'
import { useAsync } from '@/composables/useAsync'
import { useDuration } from '@/composables/useDuration'
import { accentColor, subjectAccentOf } from '@/features/subjects/types'
import { generateWeakPoints, generateWeeklySummary } from '@/api/modules/ai'
import {
  getActivity,
  getAnalyticsSummary,
  getSubjectShares,
  type ActivityDayDto,
} from '@/api/modules/analytics'
import { parseIsoDate } from '@/utils/date'

/** 12 weeks × 7 — one zero-filled series feeds both the bar chart (last 7) and the heatmap. */
const HEATMAP_DAYS = 84
const SHARES_WINDOW_DAYS = 30

const { t, d, locale } = useI18n()
const router = useRouter()
const { formatMinutes } = useDuration()

const { data, loading, error, reload } = useAsync(async () => {
  const [summary, activity, shares] = await Promise.all([
    getAnalyticsSummary(),
    getActivity(HEATMAP_DAYS),
    getSubjectShares(SHARES_WINDOW_DAYS),
  ])
  return { summary, activity, shares }
})

const showSkeleton = computed(() => loading.value && data.value === null)

// --- Stat tiles -------------------------------------------------------------

/** Signed delta text; a null metric renders "—", never a fabricated 0. */
function deltaText(value: number | null): string {
  if (value === null) return '—'
  return value > 0 ? `+${value}%` : `${value}%`
}

const completionText = computed(() => {
  const percent = data.value?.summary.taskCompletionPercent
  return percent === null || percent === undefined ? '—' : `${percent}%`
})

// --- Weekly bar chart (last 7 days of the shared series) ----------------------

const weekActivity = computed(() => data.value?.activity.slice(-7) ?? [])

const weekBars = computed(() => {
  const days = weekActivity.value
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
  () => weekActivity.value.reduce((sum, day) => sum + day.minutes, 0),
)

const weekdayFormat = computed(
  () => new Intl.DateTimeFormat(locale.value, { weekday: 'short' }),
)

function barTooltip(bar: { date: string; minutes: number }): string {
  return `${d(parseIsoDate(bar.date), 'short')} · ${formatMinutes(bar.minutes)}`
}

// --- Study heatmap (12 week-columns × 7 days, oldest first) -------------------

const heatWeeks = computed<ActivityDayDto[][]>(() => {
  const days = data.value?.activity ?? []
  const weeks: ActivityDayDto[][] = []
  for (let i = 0; i < days.length; i += 7) {
    weeks.push(days.slice(i, i + 7))
  }
  return weeks
})

/** Intensity is relative to the user's own busiest day in the window. */
const heatMax = computed(
  () => data.value?.activity.reduce((m, day) => Math.max(m, day.minutes), 0) ?? 0,
)

function heatCellColor(minutes: number): string {
  if (minutes === 0 || heatMax.value === 0) return 'var(--color-muted-soft)'
  const intensity = Math.min(1, minutes / heatMax.value)
  const pct = Math.round(15 + intensity * 70)
  return `color-mix(in srgb, var(--color-primary) ${pct}%, var(--color-surface))`
}

const heatLegendSteps = [0, 0.25, 0.5, 0.75, 1]

// --- Subject shares (30-day window; null bucket = unlinked time) --------------

const shareRows = computed(() => {
  const shares = data.value?.shares ?? []
  const total = shares.reduce((sum, share) => sum + share.minutes, 0)
  return shares.map((share) => ({
    key: share.subjectId ?? 'unlinked',
    name: share.subjectId !== null ? share.subjectName! : t('analytics.subjectDistribution.unlinked'),
    color: share.subjectId !== null ? accentColor(subjectAccentOf(share.color)) : 'var(--color-muted)',
    minutes: share.minutes,
    widthPercent: total > 0 ? (share.minutes / total) * 100 : 0,
    percent: total > 0 ? Math.round((share.minutes / total) * 100) : 0,
  }))
})

// --- AI insights ------------------------------------------------------------
// Reuses the /v1/ai/analytics/* endpoints — the real stats snapshot is sent as
// a client-supplied text field, same pattern as the Subject-page AI actions
// (see docs/ai-engine.md for the scoping note).

const insightsLoading = ref(false)
const insightsError = ref(false)
const weeklySummaryText = ref<string | null>(null)
const weakPointsText = ref<string | null>(null)
const hasInsights = computed(() => weeklySummaryText.value !== null && weakPointsText.value !== null)

function statsSnapshotText(): string {
  const snapshot = data.value
  if (!snapshot) return ''
  const { summary } = snapshot
  const lines = [
    summary.weekDeltaPercent !== null
      ? t('analytics.ai.snapshotStudyTime', {
          time: formatMinutes(summary.weekMinutes),
          delta: deltaText(summary.weekDeltaPercent),
        })
      : t('analytics.ai.snapshotStudyTimeNoDelta', { time: formatMinutes(summary.weekMinutes) }),
    t('analytics.ai.snapshotStreak', { n: summary.streakDays }),
  ]
  if (summary.taskCompletionPercent !== null) {
    lines.push(t('analytics.ai.snapshotCompletion', { n: summary.taskCompletionPercent }))
  }
  lines.push(t('analytics.ai.snapshotAiUsage', { n: summary.aiChatsThisWeek }))
  lines.push(
    `${t('analytics.ai.snapshotDaily')}: ${weekActivity.value
      .map((day) => formatMinutes(day.minutes))
      .join(', ')}`,
  )
  if (shareRows.value.length > 0) {
    lines.push(
      `${t('analytics.ai.snapshotSubjects')}: ${shareRows.value
        .map((row) => `${row.name} ${formatMinutes(row.minutes)}`)
        .join(', ')}`,
    )
  }
  return lines.join('\n')
}

async function generateInsights() {
  if (insightsLoading.value || !data.value) return
  insightsLoading.value = true
  insightsError.value = false
  try {
    const snapshot = statsSnapshotText()
    const [weekly, weak] = await Promise.all([generateWeeklySummary(snapshot), generateWeakPoints(snapshot)])
    weeklySummaryText.value = weekly.content
    weakPointsText.value = weak.content
  } catch (caught) {
    console.error(caught)
    insightsError.value = true
  } finally {
    insightsLoading.value = false
  }
}
</script>

<template>
  <div class="page">
    <AppPageHeader :title="t('analytics.title')" :subtitle="t('analytics.subtitle')" />

    <!-- Loading -->
    <div v-if="showSkeleton" aria-hidden="true">
      <div class="stat-row">
        <AppSkeleton v-for="n in 4" :key="n" variant="block" height="104px" />
      </div>
      <div class="two-col">
        <AppSkeleton variant="block" height="280px" />
        <AppSkeleton variant="block" height="280px" />
      </div>
      <AppSkeleton variant="block" height="200px" />
    </div>

    <!-- Error -->
    <AppEmpty v-else-if="error" icon="alert-circle" :title="t(error.messageKey)">
      <template #action>
        <AppButton size="sm" variant="soft" @click="reload">{{ t('common.retry') }}</AppButton>
      </template>
    </AppEmpty>

    <template v-else-if="data">
      <div class="stat-row">
        <StatTile icon="clock" :label="t('analytics.stats.studyTime')">
          {{ formatMinutes(data.summary.weekMinutes) }}
          <span class="tile-delta">
            {{ t('analytics.stats.weekDelta', { delta: deltaText(data.summary.weekDeltaPercent) }) }}
          </span>
        </StatTile>
        <StatTile
          icon="flame"
          :label="t('analytics.stats.streak')"
          :value="t('analytics.stats.streakUnit', { n: data.summary.streakDays })"
        />
        <StatTile
          icon="check-circle"
          :label="t('analytics.stats.completion')"
          :value="completionText"
        />
        <StatTile
          icon="bot"
          :label="t('analytics.stats.aiUsage')"
          :value="t('analytics.stats.aiUsageUnit', { n: data.summary.aiChatsThisWeek })"
        />
      </div>

      <div class="two-col">
        <AppCard variant="flat" class="chart-card">
          <h2 class="chart-title">{{ t('analytics.weekly.title') }}</h2>
          <p class="chart-desc">{{ t('analytics.weekly.desc') }}</p>
          <div v-if="weekTotalMinutes > 0" class="week-chart">
            <div class="chart-bars">
              <AppTooltip v-for="bar in weekBars" :key="bar.date" :content="barTooltip(bar)">
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
                {{ weekdayFormat.format(parseIsoDate(bar.date)) }}
              </span>
            </div>
          </div>
          <div v-else class="chart-empty">
            <AppIcon name="trending-up" class="chart-empty-icon" aria-hidden="true" />
            <p class="chart-empty-text">{{ t('analytics.weekly.empty') }}</p>
            <AppButton size="sm" variant="soft" @click="router.push({ name: 'calendar' })">
              {{ t('analytics.weekly.emptyCta') }}
            </AppButton>
          </div>
        </AppCard>

        <AppCard variant="flat" class="chart-card">
          <h2 class="chart-title">{{ t('analytics.subjectDistribution.title') }}</h2>
          <p class="chart-desc">{{ t('analytics.subjectDistribution.desc') }}</p>
          <ul v-if="shareRows.length > 0" class="share-list">
            <li v-for="row in shareRows" :key="row.key" class="share-row">
              <span class="share-dot" :style="{ backgroundColor: row.color }"></span>
              <span class="share-name">{{ row.name }}</span>
              <div class="share-track">
                <AppTooltip :content="formatMinutes(row.minutes)" placement="top">
                  <div
                    class="share-fill"
                    :style="{ width: `${row.widthPercent}%`, backgroundColor: row.color }"
                  ></div>
                </AppTooltip>
              </div>
              <span class="share-value">{{ row.percent }}%</span>
            </li>
          </ul>
          <div v-else class="chart-empty">
            <AppIcon name="book-open" class="chart-empty-icon" aria-hidden="true" />
            <p class="chart-empty-text">{{ t('analytics.subjectDistribution.empty') }}</p>
          </div>
        </AppCard>
      </div>

      <AppCard variant="flat" class="chart-card heatmap-card">
        <div class="heatmap-head">
          <div>
            <h2 class="chart-title">{{ t('analytics.heatmap.title') }}</h2>
            <p class="chart-desc">{{ t('analytics.heatmap.desc') }}</p>
          </div>
          <div class="heatmap-legend">
            <span>{{ t('analytics.heatmap.less') }}</span>
            <span
              v-for="step in heatLegendSteps"
              :key="step"
              class="legend-swatch"
              :style="{ backgroundColor: heatCellColor(step * heatMax) }"
            ></span>
            <span>{{ t('analytics.heatmap.more') }}</span>
          </div>
        </div>
        <div class="heatmap-grid" role="img" :aria-label="t('analytics.heatmap.title')">
          <div v-for="(week, wi) in heatWeeks" :key="wi" class="heatmap-week">
            <AppTooltip
              v-for="cell in week"
              :key="cell.date"
              :content="`${d(parseIsoDate(cell.date), 'short')} · ${formatMinutes(cell.minutes)}`"
              placement="top"
            >
              <span class="heat-cell" :style="{ backgroundColor: heatCellColor(cell.minutes) }"></span>
            </AppTooltip>
          </div>
        </div>
      </AppCard>

      <AppCard variant="flat" class="chart-card insights-card">
        <div class="insights-head">
          <div>
            <h2 class="chart-title">{{ t('analytics.ai.title') }}</h2>
            <p class="chart-desc">{{ t('analytics.ai.desc') }}</p>
          </div>
          <AppButton
            size="sm"
            variant="soft"
            icon-left="sparkles"
            :loading="insightsLoading"
            @click="generateInsights"
          >
            {{ hasInsights ? t('analytics.ai.regenerate') : t('analytics.ai.generate') }}
          </AppButton>
        </div>

        <AppLoading v-if="insightsLoading" :label="t('analytics.ai.generating')" />
        <p v-else-if="insightsError" class="insights-error">{{ t('analytics.ai.error') }}</p>
        <AppEmpty v-else-if="!hasInsights" :title="t('analytics.ai.empty')" />
        <div v-else class="insights-grid">
          <div class="insight-block">
            <h3 class="insight-title">
              <AppIcon name="trending-up" size="sm" />
              {{ t('analytics.ai.weeklySummary') }}
            </h3>
            <p class="insight-text">{{ weeklySummaryText }}</p>
          </div>
          <div class="insight-block">
            <h3 class="insight-title">
              <AppIcon name="target" size="sm" />
              {{ t('analytics.ai.weakPoints') }}
            </h3>
            <p class="insight-text">{{ weakPointsText }}</p>
          </div>
        </div>
      </AppCard>
    </template>
  </div>
</template>

<style scoped>
.page {
  max-width: 1160px;
  margin: 0 auto;
  padding: var(--space-8);
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-8);
}

.tile-delta {
  display: block;
  margin-top: 2px;
  font-size: var(--text-xs);
  font-weight: 400;
  letter-spacing: normal;
  color: var(--color-text-tertiary);
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-5);
  margin-bottom: var(--space-5);
}

.chart-title {
  margin: 0 0 var(--space-1);
  font-size: var(--text-base);
  font-weight: 600;
}

.chart-desc {
  margin: 0 0 var(--space-6);
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

/* Designed chart empty state */
.chart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  min-height: 160px;
  text-align: center;
}

.chart-empty-icon {
  color: var(--color-text-tertiary);
}

.chart-empty-text {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
}

/* Weekly bar chart — thin marks, one hue, honest zero stubs */
.week-chart {
  display: flex;
  flex-direction: column;
}

.chart-bars {
  display: flex;
  align-items: stretch;
  height: 160px;
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
  width: min(28px, 60%);
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

/* Subject share list */
.share-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.share-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.share-dot {
  width: 8px;
  height: 8px;
  flex-shrink: 0;
  border-radius: var(--radius-full);
}

.share-name {
  width: 96px;
  flex-shrink: 0;
  font-size: var(--text-xs);
  color: var(--color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.share-track {
  flex: 1;
  height: 8px;
  border-radius: var(--radius-full);
  background-color: var(--color-muted-soft);
  overflow: hidden;
}

.share-fill {
  height: 100%;
  min-width: 2px;
  border-radius: var(--radius-full);
}

.share-value {
  width: 36px;
  flex-shrink: 0;
  text-align: right;
  font-size: var(--text-xs);
  font-variant-numeric: tabular-nums;
  color: var(--color-text-tertiary);
}

/* Heatmap */
.heatmap-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.heatmap-legend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  white-space: nowrap;
}

.legend-swatch {
  width: 10px;
  height: 10px;
  border-radius: 2px;
}

.heatmap-grid {
  display: flex;
  gap: 3px;
  overflow-x: auto;
  padding-bottom: var(--space-1);
}

.heatmap-week {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.heat-cell {
  display: block;
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

/* AI insights */
.insights-card {
  margin-top: var(--space-5);
}

.insights-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}

.insights-error {
  margin: 0;
  padding: var(--space-4) 0;
  font-size: var(--text-sm);
  color: var(--color-danger);
}

.insights-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-5);
}

.insight-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin: 0 0 var(--space-2);
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-primary);
}

.insight-text {
  margin: 0;
  font-size: var(--text-sm);
  line-height: var(--leading-normal);
  color: var(--color-text-secondary);
  white-space: pre-wrap;
  overflow-wrap: break-word;
}

@media (max-width: 900px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .two-col {
    grid-template-columns: 1fr;
  }

  .insights-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .page {
    padding: var(--space-5);
  }
}
</style>
