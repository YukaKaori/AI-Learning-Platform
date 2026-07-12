<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { AppCard, AppIcon, AppPageHeader, AppTooltip } from '@/components'
import { useDuration } from '@/composables/useDuration'
import { accentColor } from '@/features/subjects/types'
import { HEATMAP_MAX, heatmap, subjectShares, summary, weeklyActivity } from './mock'

const { t, d } = useI18n()
const { formatMinutes } = useDuration()

const weekMax = computed(() => Math.max(...weeklyActivity.map((day) => day.minutes), 1))
const shareTotal = computed(() => subjectShares.reduce((sum, s) => sum + s.minutes, 0))

function heatCellColor(minutes: number): string {
  if (minutes === 0) return 'var(--color-muted-soft)'
  const intensity = Math.min(1, minutes / HEATMAP_MAX)
  const pct = Math.round(15 + intensity * 70)
  return `color-mix(in srgb, var(--color-primary) ${pct}%, var(--color-surface))`
}

const heatLegendSteps = [0, 0.25, 0.5, 0.75, 1]
</script>

<template>
  <div class="page">
    <AppPageHeader :title="t('analytics.title')" :subtitle="t('analytics.subtitle')" />

    <div class="stat-row">
      <AppCard variant="flat" class="stat-tile">
        <span class="stat-icon"><AppIcon name="clock" /></span>
        <span class="stat-value">{{ formatMinutes(summary.weekMinutes) }}</span>
        <span class="stat-label">{{ t('analytics.stats.studyTime') }}</span>
      </AppCard>
      <AppCard variant="flat" class="stat-tile">
        <span class="stat-icon"><AppIcon name="flame" /></span>
        <span class="stat-value">{{ t('analytics.stats.streakUnit', { n: summary.streakDays }) }}</span>
        <span class="stat-label">{{ t('analytics.stats.streak') }}</span>
      </AppCard>
      <AppCard variant="flat" class="stat-tile">
        <span class="stat-icon"><AppIcon name="check-circle" /></span>
        <span class="stat-value">{{ summary.taskCompletion }}%</span>
        <span class="stat-label">{{ t('analytics.stats.completion') }}</span>
      </AppCard>
      <AppCard variant="flat" class="stat-tile">
        <span class="stat-icon"><AppIcon name="bot" /></span>
        <span class="stat-value">
          {{ t('analytics.stats.aiUsageUnit', { n: summary.aiChatsThisWeek }) }}
        </span>
        <span class="stat-label">{{ t('analytics.stats.aiUsage') }}</span>
      </AppCard>
    </div>

    <div class="two-col">
      <AppCard variant="flat" class="chart-card">
        <h2 class="chart-title">{{ t('analytics.weekly.title') }}</h2>
        <p class="chart-desc">{{ t('analytics.weekly.desc') }}</p>
        <div class="bar-chart" role="img" :aria-label="t('analytics.weekly.title')">
          <AppTooltip
            v-for="day in weeklyActivity"
            :key="day.day"
            :content="formatMinutes(day.minutes)"
            placement="top"
          >
            <div class="bar-column">
              <div class="bar-track">
                <div
                  class="bar-fill"
                  :style="{ height: `${(day.minutes / weekMax) * 100}%` }"
                ></div>
              </div>
              <span class="bar-label">{{ d(day.day, 'short') }}</span>
            </div>
          </AppTooltip>
        </div>
      </AppCard>

      <AppCard variant="flat" class="chart-card">
        <h2 class="chart-title">{{ t('analytics.subjectDistribution.title') }}</h2>
        <p class="chart-desc">{{ t('analytics.subjectDistribution.desc') }}</p>
        <ul class="share-list">
          <li v-for="share in subjectShares" :key="share.subject.id" class="share-row">
            <span
              class="share-dot"
              :style="{ backgroundColor: accentColor(share.subject.accent) }"
            ></span>
            <span class="share-name">{{ share.subject.name }}</span>
            <div class="share-track">
              <AppTooltip :content="formatMinutes(share.minutes)" placement="top">
                <div
                  class="share-fill"
                  :style="{
                    width: `${(share.minutes / shareTotal) * 100}%`,
                    backgroundColor: accentColor(share.subject.accent),
                  }"
                ></div>
              </AppTooltip>
            </div>
            <span class="share-value">
              {{ Math.round((share.minutes / shareTotal) * 100) }}%
            </span>
          </li>
        </ul>
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
            :style="{ backgroundColor: heatCellColor(step * HEATMAP_MAX) }"
          ></span>
          <span>{{ t('analytics.heatmap.more') }}</span>
        </div>
      </div>
      <div class="heatmap-grid" role="img" :aria-label="t('analytics.heatmap.title')">
        <div v-for="(week, wi) in heatmap" :key="wi" class="heatmap-week">
          <AppTooltip
            v-for="cell in week"
            :key="cell.day"
            :content="`${d(cell.day, 'short')} · ${formatMinutes(cell.minutes)}`"
            placement="top"
          >
            <span class="heat-cell" :style="{ backgroundColor: heatCellColor(cell.minutes) }"></span>
          </AppTooltip>
        </div>
      </div>
    </AppCard>
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

/* Weekly bar chart */
.bar-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-2);
  height: 160px;
}

.bar-column {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  flex: 1;
  height: 100%;
}

.bar-track {
  flex: 1;
  width: 100%;
  max-width: 32px;
  display: flex;
  align-items: flex-end;
}

.bar-fill {
  width: 100%;
  min-height: 4px;
  border-radius: var(--radius-sm) var(--radius-sm) 0 0;
  background-color: var(--color-primary);
  transition: height var(--duration-slow) var(--ease-out);
}

.bar-label {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
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

@media (max-width: 900px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .two-col {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .page {
    padding: var(--space-5);
  }
}
</style>
