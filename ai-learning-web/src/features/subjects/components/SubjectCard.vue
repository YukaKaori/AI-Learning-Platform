<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { AppCard, AppIcon, AppTag } from '@/components'
import { useDuration } from '@/composables/useDuration'
import type { SubjectDto } from '@/api/modules/subject'
import { accentColor, subjectAccentOf, subjectIconOf } from '../types'

const props = defineProps<{ subject: SubjectDto }>()

defineEmits<{ open: [id: string] }>()

const { t } = useI18n()
const { formatMinutes } = useDuration()

const statusTone = { active: 'primary', completed: 'success', archived: 'secondary' } as const

const accent = computed(() => accentColor(subjectAccentOf(props.subject.color)))
const icon = computed(() => subjectIconOf(props.subject.icon))
</script>

<template>
  <AppCard
    variant="flat"
    interactive
    class="subject-card"
    role="link"
    tabindex="0"
    @click="$emit('open', subject.id)"
    @keydown.enter="$emit('open', subject.id)"
  >
    <div class="head">
      <span class="icon-tile" :style="{ color: accent }" aria-hidden="true">
        <AppIcon :name="icon" size="lg" />
      </span>
      <AppTag :tone="statusTone[subject.status]" size="sm">
        {{ t(`subjects.status.${subject.status}`) }}
      </AppTag>
    </div>

    <h3 class="name">{{ subject.name }}</h3>
    <p class="description">{{ subject.description ?? '' }}</p>

    <div class="progress" :aria-label="t('subjects.progress')">
      <div class="progress-track">
        <div
          class="progress-fill"
          :style="{ width: `${subject.progress}%`, backgroundColor: accent }"
        ></div>
      </div>
      <span class="progress-value">{{ subject.progress }}%</span>
    </div>

    <div class="meta">
      <span class="meta-item">
        <AppIcon name="layers" size="sm" />
        {{ t('subjects.materials', { n: subject.materialCount }) }}
      </span>
      <span class="meta-item">
        <AppIcon name="clock" size="sm" />
        {{ t('subjects.studyTime', { time: formatMinutes(subject.studyMinutes) }) }}
      </span>
    </div>
  </AppCard>
</template>

<style scoped>
.subject-card {
  display: flex;
  flex-direction: column;
}

.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}

.icon-tile {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background-color: color-mix(in srgb, currentColor 12%, transparent);
}

.name {
  margin: 0 0 var(--space-1);
  font-size: var(--text-lg);
  font-weight: 600;
  letter-spacing: var(--tracking-tight);
  color: var(--color-text);
}

.description {
  margin: 0 0 var(--space-5);
  font-size: var(--text-sm);
  line-height: var(--leading-normal);
  color: var(--color-text-secondary);
  min-height: calc(2 * 1em * var(--leading-normal));
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.progress {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.progress-track {
  flex: 1;
  height: 5px;
  border-radius: var(--radius-full);
  background-color: var(--color-muted-soft);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: var(--radius-full);
  transition: width var(--duration-slow) var(--ease-out);
}

.progress-value {
  font-size: var(--text-xs);
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-secondary);
}

.meta {
  display: flex;
  gap: var(--space-4);
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}
</style>
