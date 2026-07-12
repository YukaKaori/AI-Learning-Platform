<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { AppBadge, AppButton, AppCard, AppDialog, AppEmpty, AppPageHeader } from '@/components'
import { getSubject } from '@/features/subjects/mock'
import { accentColor } from '@/features/subjects/types'
import { cardsOf, mockDecks, totalDue } from './mock'

const { t } = useI18n()

const selectedDeckId = ref(mockDecks[0]?.id ?? '')
const selectedDeck = computed(() => mockDecks.find((d) => d.id === selectedDeckId.value))
const cards = computed(() => cardsOf(selectedDeckId.value))

// Review-mode preview: flip through the selected deck's cards.
const reviewOpen = ref(false)
const reviewIndex = ref(0)
const flipped = ref(false)
const reviewCard = computed(() => cards.value[reviewIndex.value])

watch(reviewOpen, () => {
  reviewIndex.value = 0
  flipped.value = false
})

function nextCard() {
  flipped.value = false
  reviewIndex.value = (reviewIndex.value + 1) % Math.max(cards.value.length, 1)
}

function deckAccent(subjectId?: string): string {
  const subject = subjectId ? getSubject(subjectId) : undefined
  return subject ? accentColor(subject.accent) : 'var(--color-muted)'
}

const stats = computed(() => [
  { key: 'total', value: mockDecks.reduce((sum, d) => sum + d.cardCount, 0) },
  { key: 'dueToday', value: totalDue() },
  { key: 'reviewed', value: 1284 },
  { key: 'retention', value: '92%' },
])
</script>

<template>
  <div class="page">
    <AppPageHeader :title="t('flashcards.title')" :subtitle="t('flashcards.subtitle')">
      <template #actions>
        <AppButton icon-left="plus" disabled>{{ t('flashcards.newDeck') }}</AppButton>
      </template>
    </AppPageHeader>

    <div class="stat-row">
      <AppCard v-for="stat in stats" :key="stat.key" variant="flat" class="stat-tile">
        <span class="stat-value">{{ stat.value }}</span>
        <span class="stat-label">{{ t(`flashcards.stats.${stat.key}`) }}</span>
      </AppCard>
    </div>

    <div class="layout">
      <section class="decks">
        <h2 class="section-title">{{ t('flashcards.decks') }}</h2>
        <button
          v-for="deck in mockDecks"
          :key="deck.id"
          type="button"
          class="deck"
          :class="{ active: deck.id === selectedDeckId }"
          @click="selectedDeckId = deck.id"
        >
          <span class="deck-bar" :style="{ backgroundColor: deckAccent(deck.subjectId) }"></span>
          <div class="deck-main">
            <span class="deck-name">{{ deck.name }}</span>
            <span class="deck-meta">{{ t('flashcards.cards', { n: deck.cardCount }) }}</span>
          </div>
          <AppBadge v-if="deck.dueCount > 0" tone="primary">{{ deck.dueCount }}</AppBadge>
        </button>
      </section>

      <section class="deck-detail">
        <template v-if="selectedDeck">
          <div class="detail-head">
            <div>
              <h2 class="detail-name">{{ selectedDeck.name }}</h2>
              <p v-if="selectedDeck.description" class="detail-desc">
                {{ selectedDeck.description }}
              </p>
            </div>
            <AppButton
              icon-left="play"
              :disabled="cards.length === 0"
              @click="reviewOpen = true"
            >
              {{ t('flashcards.review.start') }}
            </AppButton>
          </div>

          <h3 class="list-title">{{ t('flashcards.cardList') }}</h3>
          <AppEmpty v-if="cards.length === 0" :title="t('flashcards.empty')" icon="layers" />
          <ul v-else class="card-list">
            <li v-for="card in cards" :key="card.id" class="card-row">
              <div class="card-side">
                <span class="side-label">{{ t('flashcards.front') }}</span>
                <span class="side-text">{{ card.front }}</span>
              </div>
              <div class="card-side">
                <span class="side-label">{{ t('flashcards.back') }}</span>
                <span class="side-text muted">{{ card.back }}</span>
              </div>
            </li>
          </ul>
        </template>
      </section>
    </div>

    <AppDialog v-model="reviewOpen" :title="t('flashcards.review.title')" width="520px">
      <p class="review-note">{{ t('flashcards.review.placeholder') }}</p>
      <div
        v-if="reviewCard"
        class="review-card"
        :class="{ flipped }"
        role="button"
        tabindex="0"
        :aria-label="t('flashcards.review.flip')"
        @click="flipped = !flipped"
        @keydown.space.prevent="flipped = !flipped"
        @keydown.enter.prevent="flipped = !flipped"
      >
        <span class="review-side-label">
          {{ flipped ? t('flashcards.back') : t('flashcards.front') }}
        </span>
        <span class="review-text">{{ flipped ? reviewCard.back : reviewCard.front }}</span>
      </div>
      <template #footer>
        <div class="review-actions">
          <AppButton variant="soft" tone="secondary" @click="reviewOpen = false">
            {{ t('flashcards.review.close') }}
          </AppButton>
          <div class="review-actions-right">
            <AppButton variant="soft" icon-left="refresh" @click="flipped = !flipped">
              {{ t('flashcards.review.flip') }}
            </AppButton>
            <AppButton icon-right="arrow-right" @click="nextCard">
              {{ reviewIndex + 1 }} / {{ cards.length }}
            </AppButton>
          </div>
        </div>
      </template>
    </AppDialog>
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

.layout {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: var(--space-6);
  align-items: start;
}

.section-title {
  margin: 0 0 var(--space-3);
  font-size: var(--text-xs);
  font-weight: 600;
  letter-spacing: var(--tracking-wide);
  text-transform: uppercase;
  color: var(--color-text-tertiary);
}

.decks {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.deck {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-3);
  border: var(--border-width-sm) solid var(--color-border);
  border-radius: var(--radius-card);
  background-color: var(--color-surface);
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    border-color var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out);
}

.deck:hover {
  box-shadow: var(--shadow-sm);
}

.deck.active {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 1px var(--color-primary);
}

.deck-bar {
  width: 3px;
  align-self: stretch;
  border-radius: var(--radius-full);
  flex-shrink: 0;
}

.deck-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.deck-name {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.deck-meta {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

.deck-detail {
  min-width: 0;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

.detail-name {
  margin: 0 0 var(--space-1);
  font-size: var(--text-xl);
  font-weight: 600;
  letter-spacing: var(--tracking-tight);
}

.detail-desc {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.list-title {
  margin: 0 0 var(--space-3);
  font-size: var(--text-xs);
  font-weight: 600;
  letter-spacing: var(--tracking-wide);
  text-transform: uppercase;
  color: var(--color-text-tertiary);
}

.card-list {
  margin: 0;
  padding: 0;
  list-style: none;
  border: var(--border-width-sm) solid var(--color-border);
  border-radius: var(--radius-card);
  background-color: var(--color-surface);
  overflow: hidden;
}

.card-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
  padding: var(--space-4);
}

.card-row + .card-row {
  border-top: var(--border-width-sm) solid var(--color-border);
}

.card-side {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.side-label {
  font-size: var(--text-xs);
  font-weight: 600;
  letter-spacing: var(--tracking-wide);
  text-transform: uppercase;
  color: var(--color-text-tertiary);
}

.side-text {
  font-size: var(--text-sm);
  line-height: var(--leading-normal);
}

.side-text.muted {
  color: var(--color-text-secondary);
}

/* Review preview */
.review-note {
  margin: 0 0 var(--space-4);
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

.review-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  min-height: 220px;
  padding: var(--space-6);
  border: var(--border-width-sm) solid var(--color-border);
  border-radius: var(--radius-xl);
  background-color: var(--color-bg);
  text-align: center;
  cursor: pointer;
  transition:
    transform var(--duration-base) var(--ease-spring),
    border-color var(--duration-base) var(--ease-out);
}

.review-card:hover {
  border-color: var(--color-primary);
}

.review-card.flipped {
  transform: rotateX(360deg);
}

.review-side-label {
  font-size: var(--text-xs);
  font-weight: 600;
  letter-spacing: var(--tracking-wide);
  text-transform: uppercase;
  color: var(--color-text-tertiary);
}

.review-text {
  font-size: var(--text-lg);
  line-height: var(--leading-normal);
}

.review-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.review-actions-right {
  display: flex;
  gap: var(--space-2);
}

@media (max-width: 900px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .page {
    padding: var(--space-5);
  }

  .card-row {
    grid-template-columns: 1fr;
  }
}
</style>
