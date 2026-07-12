<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { AppButton, AppCard, AppEmpty, AppIcon, AppPageHeader, AppTag } from '@/components'
import { useDuration } from '@/composables/useDuration'
import { excerptOf } from '@/features/notes/types'
import { mockNotes } from '@/features/notes/mock'
import { getSubject, materialsOf } from './mock'
import { accentColor, MATERIAL_TYPE_ICON } from './types'

const { t, d } = useI18n()
const route = useRoute()
const router = useRouter()
const { formatMinutes } = useDuration()

const subject = computed(() => getSubject(String(route.params.id)))
const materials = computed(() => (subject.value ? materialsOf(subject.value.id) : []))
const relatedNotes = computed(() =>
  subject.value ? mockNotes.filter((n) => n.subjectId === subject.value!.id) : [],
)

const statusTone = { active: 'primary', completed: 'success', archived: 'secondary' } as const
</script>

<template>
  <div class="page">
    <template v-if="subject">
      <AppPageHeader :title="subject.name" :subtitle="subject.description">
        <template #breadcrumb>
          <RouterLink :to="{ name: 'subjects' }" class="back-link">
            <AppIcon name="arrow-left" size="sm" />
            {{ t('subjects.detail.back') }}
          </RouterLink>
        </template>
        <template #actions>
          <AppTag :tone="statusTone[subject.status]">
            {{ t(`subjects.status.${subject.status}`) }}
          </AppTag>
        </template>
      </AppPageHeader>

      <AppCard variant="flat" class="overview">
        <div class="overview-grid">
          <div class="overview-item">
            <span class="overview-label">{{ t('subjects.progress') }}</span>
            <div class="progress-row">
              <div class="progress-track">
                <div
                  class="progress-fill"
                  :style="{
                    width: `${subject.progress}%`,
                    backgroundColor: accentColor(subject.accent),
                  }"
                ></div>
              </div>
              <span class="overview-value">{{ subject.progress }}%</span>
            </div>
          </div>
          <div class="overview-item">
            <span class="overview-label">{{ t('profile.totalStudy') }}</span>
            <span class="overview-value">{{ formatMinutes(subject.studyMinutes) }}</span>
          </div>
          <div class="overview-item">
            <span class="overview-label">{{ t('subjects.detail.materials') }}</span>
            <span class="overview-value">{{ materials.length }}</span>
          </div>
        </div>
      </AppCard>

      <section class="section">
        <div class="section-head">
          <h2 class="section-title">{{ t('subjects.detail.materials') }}</h2>
          <AppButton variant="soft" size="sm" icon-left="plus" disabled>
            {{ t('subjects.detail.addMaterial') }}
          </AppButton>
        </div>

        <AppEmpty v-if="materials.length === 0" :title="t('subjects.detail.noMaterials')" />
        <ul v-else class="material-list">
          <li v-for="material in materials" :key="material.id" class="material-row">
            <span class="material-icon" :style="{ color: accentColor(subject.accent) }">
              <AppIcon :name="MATERIAL_TYPE_ICON[material.type]" />
            </span>
            <div class="material-main">
              <span class="material-title">{{ material.title }}</span>
              <span v-if="material.description" class="material-desc">
                {{ material.description }}
              </span>
            </div>
            <AppTag size="sm" tone="secondary">
              {{ t(`subjects.materialType.${material.type}`) }}
            </AppTag>
            <span class="material-date">{{ d(material.addedAt, 'short') }}</span>
          </li>
        </ul>
      </section>

      <section class="section">
        <div class="section-head">
          <h2 class="section-title">{{ t('subjects.detail.relatedNotes') }}</h2>
        </div>
        <AppEmpty v-if="relatedNotes.length === 0" :title="t('subjects.detail.noNotes')" />
        <div v-else class="note-grid">
          <AppCard
            v-for="note in relatedNotes"
            :key="note.id"
            variant="flat"
            interactive
            @click="router.push({ name: 'notes', query: { note: note.id } })"
          >
            <h3 class="note-title">{{ note.title }}</h3>
            <p class="note-excerpt">{{ excerptOf(note) }}</p>
            <span class="note-date">{{ t('notes.updated', { time: d(note.updatedAt, 'short') }) }}</span>
          </AppCard>
        </div>
      </section>
    </template>

    <AppEmpty v-else :title="t('subjects.empty')">
      <template #action>
        <AppButton variant="soft" @click="router.push({ name: 'subjects' })">
          {{ t('subjects.detail.back') }}
        </AppButton>
      </template>
    </AppEmpty>
  </div>
</template>

<style scoped>
.page {
  max-width: 960px;
  margin: 0 auto;
  padding: var(--space-8);
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  color: var(--color-text-tertiary);
  transition: color var(--duration-fast) var(--ease-out);
}

.back-link:hover {
  color: var(--color-text);
}

.overview {
  margin-bottom: var(--space-8);
}

.overview-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: var(--space-6);
}

.overview-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.overview-label {
  font-size: var(--text-xs);
  font-weight: 500;
  letter-spacing: var(--tracking-wide);
  text-transform: uppercase;
  color: var(--color-text-tertiary);
}

.overview-value {
  font-size: var(--text-lg);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--color-text);
}

.progress-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.progress-track {
  flex: 1;
  height: 6px;
  border-radius: var(--radius-full);
  background-color: var(--color-muted-soft);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: var(--radius-full);
  transition: width var(--duration-slow) var(--ease-out);
}

.section {
  margin-bottom: var(--space-10);
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}

.section-title {
  margin: 0;
  font-size: var(--font-title-size);
  font-weight: var(--font-title-weight);
  letter-spacing: var(--font-title-tracking);
}

.material-list {
  margin: 0;
  padding: 0;
  list-style: none;
  border: var(--border-width-sm) solid var(--color-border);
  border-radius: var(--radius-card);
  background-color: var(--color-surface);
  overflow: hidden;
}

.material-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  transition: background-color var(--duration-fast) var(--ease-out);
}

.material-row + .material-row {
  border-top: var(--border-width-sm) solid var(--color-border);
}

.material-row:hover {
  background-color: var(--color-surface-hover);
}

.material-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  background-color: color-mix(in srgb, currentColor 10%, transparent);
}

.material-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.material-title {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.material-desc {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.material-date {
  flex-shrink: 0;
  font-size: var(--text-xs);
  font-variant-numeric: tabular-nums;
  color: var(--color-text-tertiary);
}

.note-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: var(--space-4);
}

.note-title {
  margin: 0 0 var(--space-2);
  font-size: var(--text-base);
  font-weight: 600;
}

.note-excerpt {
  margin: 0 0 var(--space-3);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.note-date {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

@media (max-width: 640px) {
  .page {
    padding: var(--space-5);
  }

  .overview-grid {
    grid-template-columns: 1fr;
    gap: var(--space-4);
  }
}
</style>
