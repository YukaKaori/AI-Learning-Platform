<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { AppButton, AppEmpty, AppIcon, AppPageHeader, AppSkeleton } from '@/components'
import type { IconName } from '@/components'
import { useSubjectsStore } from '@/stores/subjects'
import SubjectCard from './components/SubjectCard.vue'
import SubjectFormDialog from './components/SubjectFormDialog.vue'
import { accentColor, type SubjectAccent } from './types'

const { t } = useI18n()
const router = useRouter()
const subjectsStore = useSubjectsStore()

onMounted(() => {
  void subjectsStore.load()
})

const showSkeleton = computed(() => subjectsStore.loading && !subjectsStore.loaded)

function openSubject(id: string) {
  router.push({ name: 'subject-detail', params: { id } })
}

const createOpen = ref(false)

// Suggested starters — one click creates a real subject the user can rename.
interface StarterChip {
  key: 'ml' | 'lang' | 'algo' | 'reading'
  icon: IconName
  accent: SubjectAccent
}

const starters: StarterChip[] = [
  { key: 'ml', icon: 'brain', accent: 'indigo' },
  { key: 'lang', icon: 'globe', accent: 'amber' },
  { key: 'algo', icon: 'network', accent: 'rose' },
  { key: 'reading', icon: 'book-open', accent: 'teal' },
]

const starterBusy = ref<StarterChip['key'] | null>(null)

async function createStarter(chip: StarterChip) {
  if (starterBusy.value) return
  starterBusy.value = chip.key
  try {
    const created = await subjectsStore.create({
      name: t(`subjects.starter.${chip.key}`),
      color: chip.accent,
      icon: chip.icon,
    })
    openSubject(created.id)
  } catch (error) {
    console.error(error)
  } finally {
    starterBusy.value = null
  }
}
</script>

<template>
  <div class="page">
    <AppPageHeader :title="t('subjects.title')" :subtitle="t('subjects.subtitle')">
      <template #actions>
        <AppButton icon-left="plus" @click="createOpen = true">{{ t('subjects.add') }}</AppButton>
      </template>
    </AppPageHeader>

    <div v-if="showSkeleton" class="grid" aria-hidden="true">
      <AppSkeleton v-for="n in 6" :key="n" variant="block" height="220px" />
    </div>

    <AppEmpty
      v-else-if="subjectsStore.error"
      icon="alert-circle"
      :title="t(subjectsStore.error.messageKey)"
    >
      <template #action>
        <AppButton size="sm" variant="soft" @click="subjectsStore.load(true)">
          {{ t('common.retry') }}
        </AppButton>
      </template>
    </AppEmpty>

    <div v-else-if="subjectsStore.subjects.length === 0" class="empty-state">
      <AppEmpty icon="graduation-cap" :title="t('subjects.empty')" :description="t('subjects.emptyDesc')">
        <template #action>
          <AppButton icon-left="plus" @click="createOpen = true">
            {{ t('subjects.createFirst') }}
          </AppButton>
        </template>
      </AppEmpty>
      <div class="starters">
        <span class="starters-label">{{ t('subjects.starters') }}</span>
        <div class="starter-row">
          <button
            v-for="chip in starters"
            :key="chip.key"
            type="button"
            class="starter-chip"
            :disabled="starterBusy !== null"
            :style="{ color: accentColor(chip.accent) }"
            @click="createStarter(chip)"
          >
            <AppIcon :name="starterBusy === chip.key ? 'loader' : chip.icon" size="sm" />
            <span class="starter-name">{{ t(`subjects.starter.${chip.key}`) }}</span>
          </button>
        </div>
      </div>
    </div>

    <div v-else class="grid">
      <SubjectCard
        v-for="subject in subjectsStore.subjects"
        :key="subject.id"
        :subject="subject"
        @open="openSubject"
      />
    </div>

    <SubjectFormDialog v-model="createOpen" @saved="(subject) => openSubject(subject.id)" />
  </div>
</template>

<style scoped>
.page {
  max-width: 1160px;
  margin: 0 auto;
  padding: var(--space-8);
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--space-5);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding-top: var(--space-8);
}

.starters {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
}

.starters-label {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

.starter-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--space-2);
}

.starter-chip {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border: var(--border-width-sm) solid var(--color-border);
  border-radius: var(--radius-full);
  background-color: var(--color-surface);
  font-family: inherit;
  font-size: var(--text-sm);
  cursor: pointer;
  transition:
    border-color var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out);
}

.starter-chip:hover:not(:disabled) {
  border-color: currentColor;
  box-shadow: var(--shadow-sm);
}

.starter-chip:disabled {
  opacity: 0.6;
  cursor: default;
}

.starter-name {
  color: var(--color-text-secondary);
}

@media (max-width: 640px) {
  .page {
    padding: var(--space-5);
  }
}
</style>
