<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { AppButton, AppIcon, AppSearch, AppTag } from '@/components'
import { getSubject } from '@/features/subjects/mock'
import { accentColor } from '@/features/subjects/types'
import { mockNotes } from './mock'
import { excerptOf, outlineOf } from './types'

const { t, d } = useI18n()
const route = useRoute()

const search = ref('')
const notes = computed(() => {
  const query = search.value.trim().toLowerCase()
  const list = [...mockNotes].sort(
    (a, b) => Number(b.pinned) - Number(a.pinned) || b.updatedAt - a.updatedAt,
  )
  return query ? list.filter((n) => n.title.toLowerCase().includes(query)) : list
})

const selectedId = ref<string | null>(
  typeof route.query.note === 'string' ? route.query.note : (mockNotes[0]?.id ?? null),
)
const selected = computed(() => mockNotes.find((n) => n.id === selectedId.value) ?? null)
const outline = computed(() => (selected.value ? outlineOf(selected.value) : []))

watch(
  () => route.query.note,
  (id) => {
    if (typeof id === 'string' && id) selectedId.value = id
  },
)

function subjectOf(subjectId?: string) {
  return subjectId ? getSubject(subjectId) : undefined
}
</script>

<template>
  <div class="notes">
    <aside class="list-panel">
      <div class="list-head">
        <h1 class="list-title">{{ t('notes.title') }}</h1>
        <AppButton size="sm" variant="soft" icon-left="plus" disabled>
          {{ t('notes.newNote') }}
        </AppButton>
      </div>
      <div class="list-search">
        <AppSearch v-model="search" :placeholder="t('notes.searchPlaceholder')" />
      </div>
      <p v-if="notes.length === 0" class="list-empty">{{ t('notes.empty') }}</p>
      <ul v-else class="note-list">
        <li v-for="note in notes" :key="note.id">
          <button
            type="button"
            class="note-item"
            :class="{ active: note.id === selectedId }"
            @click="selectedId = note.id"
          >
            <div class="note-item-head">
              <AppIcon v-if="note.pinned" name="star" size="sm" class="pin-icon" :label="t('notes.pinned')" />
              <span class="note-item-title">{{ note.title }}</span>
            </div>
            <span class="note-item-excerpt">{{ excerptOf(note) }}</span>
            <div class="note-item-meta">
              <span
                v-if="subjectOf(note.subjectId)"
                class="note-subject"
                :style="{ color: accentColor(subjectOf(note.subjectId)!.accent) }"
              >
                {{ subjectOf(note.subjectId)!.name }}
              </span>
              <span class="note-date">{{ d(note.updatedAt, 'short') }}</span>
            </div>
          </button>
        </li>
      </ul>
    </aside>

    <section class="editor">
      <div v-if="!selected" class="editor-empty">
        <AppIcon name="notebook-pen" size="lg" />
        <p>{{ t('notes.noSelection') }}</p>
      </div>
      <template v-else>
        <header class="editor-head">
          <h2 class="editor-title">{{ selected.title }}</h2>
          <div class="editor-meta">
            <AppTag
              v-if="subjectOf(selected.subjectId)"
              size="sm"
              tone="secondary"
            >{{ subjectOf(selected.subjectId)!.name }}</AppTag>
            <span class="editor-date">
              {{ t('notes.updated', { time: d(selected.updatedAt, 'long') }) }}
            </span>
          </div>
        </header>

        <div class="editor-notice" role="note">
          <AppIcon name="info" size="sm" />
          <div>
            <strong>{{ t('notes.editorPlaceholder.title') }}</strong>
            <span>{{ t('notes.editorPlaceholder.desc') }}</span>
          </div>
        </div>

        <div class="editor-content">{{ selected.content }}</div>
      </template>
    </section>

    <aside class="outline-panel">
      <h3 class="outline-title">{{ t('notes.outline') }}</h3>
      <p v-if="outline.length === 0" class="outline-empty">{{ t('notes.outlineEmpty') }}</p>
      <ul v-else class="outline-list">
        <li
          v-for="(item, index) in outline"
          :key="index"
          class="outline-item"
          :style="{ paddingLeft: `${(item.level - 1) * 12}px` }"
        >
          {{ item.text }}
        </li>
      </ul>
    </aside>
  </div>
</template>

<style scoped>
.notes {
  display: flex;
  height: 100%;
  min-height: 0;
}

/* Notes list */
.list-panel {
  display: flex;
  flex-direction: column;
  width: 300px;
  flex-shrink: 0;
  border-right: var(--border-width-sm) solid var(--color-border);
  background-color: var(--color-surface);
}

.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-5) var(--space-4) var(--space-3);
}

.list-title {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: 600;
  letter-spacing: var(--tracking-tight);
}

.list-search {
  padding: 0 var(--space-4) var(--space-3);
}

.list-empty {
  margin: 0;
  padding: var(--space-8) var(--space-4);
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  text-align: center;
}

.note-list {
  flex: 1;
  margin: 0;
  padding: 0 var(--space-2) var(--space-4);
  list-style: none;
  overflow-y: auto;
}

.note-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  width: 100%;
  padding: var(--space-3) var(--space-2);
  border: none;
  border-radius: var(--radius-md);
  background: transparent;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-out);
}

.note-item:hover {
  background-color: var(--color-surface-hover);
}

.note-item.active {
  background-color: var(--color-primary-soft);
}

.note-item-head {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  min-width: 0;
}

.pin-icon {
  flex-shrink: 0;
  color: var(--color-warning);
}

.note-item-title {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-item.active .note-item-title {
  color: var(--color-primary);
}

.note-item-excerpt {
  font-size: var(--text-xs);
  color: var(--color-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.note-item-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.note-subject {
  font-size: var(--text-xs);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-date {
  flex-shrink: 0;
  font-size: var(--text-xs);
  font-variant-numeric: tabular-nums;
  color: var(--color-text-tertiary);
}

/* Editor (read-only until the editor phase) */
.editor {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  padding: var(--space-10) var(--space-12);
}

.editor-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  height: 100%;
  color: var(--color-text-tertiary);
  font-size: var(--text-sm);
}

.editor-head {
  max-width: 720px;
  margin: 0 auto var(--space-5);
}

.editor-title {
  margin: 0 0 var(--space-2);
  font-family: var(--font-headline-family);
  font-size: var(--font-headline-size);
  font-weight: var(--font-headline-weight);
  letter-spacing: var(--font-headline-tracking);
}

.editor-meta {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.editor-date {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

.editor-notice {
  display: flex;
  gap: var(--space-2);
  max-width: 720px;
  margin: 0 auto var(--space-6);
  padding: var(--space-3) var(--space-4);
  border: var(--border-width-sm) solid var(--color-border);
  border-radius: var(--radius-md);
  background-color: var(--color-muted-soft);
  font-size: var(--text-xs);
  color: var(--color-text-secondary);
}

.editor-notice strong {
  display: block;
  color: var(--color-text);
}

.editor-content {
  max-width: 720px;
  margin: 0 auto;
  font-size: var(--text-base);
  line-height: 1.8;
  color: var(--color-text);
  white-space: pre-wrap;
  overflow-wrap: break-word;
}

/* Outline rail */
.outline-panel {
  width: 220px;
  flex-shrink: 0;
  padding: var(--space-8) var(--space-5);
  border-left: var(--border-width-sm) solid var(--color-border);
  overflow-y: auto;
}

.outline-title {
  margin: 0 0 var(--space-3);
  font-size: var(--text-xs);
  font-weight: 600;
  letter-spacing: var(--tracking-wide);
  text-transform: uppercase;
  color: var(--color-text-tertiary);
}

.outline-empty {
  margin: 0;
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

.outline-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.outline-item {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1100px) {
  .outline-panel {
    display: none;
  }
}

@media (max-width: 900px) {
  .list-panel {
    width: 260px;
  }

  .editor {
    padding: var(--space-6);
  }
}

@media (max-width: 640px) {
  .list-panel {
    display: none;
  }
}
</style>
