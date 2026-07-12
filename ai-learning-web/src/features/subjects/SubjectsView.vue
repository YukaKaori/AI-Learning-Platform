<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { AppButton, AppPageHeader } from '@/components'
import SubjectCard from './components/SubjectCard.vue'
import { mockSubjects } from './mock'

const { t } = useI18n()
const router = useRouter()

function openSubject(id: string) {
  router.push({ name: 'subject-detail', params: { id } })
}
</script>

<template>
  <div class="page">
    <AppPageHeader :title="t('subjects.title')" :subtitle="t('subjects.subtitle')">
      <template #actions>
        <AppButton icon-left="plus" disabled>{{ t('subjects.add') }}</AppButton>
      </template>
    </AppPageHeader>

    <div class="grid">
      <SubjectCard
        v-for="subject in mockSubjects"
        :key="subject.id"
        :subject="subject"
        @open="openSubject"
      />
    </div>
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

@media (max-width: 640px) {
  .page {
    padding: var(--space-5);
  }
}
</style>
