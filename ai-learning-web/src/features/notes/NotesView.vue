<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import {
  AppButton,
  AppDialog,
  AppEmpty,
  AppIcon,
  AppInput,
  AppSearch,
  AppSkeleton,
  AppTag,
  AppTooltip,
} from '@/components'
import type { IconName } from '@/components'
import { createNote, deleteNote as apiDeleteNote, listNotes, updateNote } from '@/api/modules/note'
import type { NoteDto } from '@/api/modules/note'
import { generateFlashcards, noteAiAction, type NoteAiAction } from '@/api/modules/ai'
import { useAsync } from '@/composables/useAsync'
import { getSubject } from '@/features/subjects/mock'
import { accentColor } from '@/features/subjects/types'
import { excerptOf, outlineOf, type Note } from './types'

const { t, d } = useI18n()
const route = useRoute()
const router = useRouter()

function toLocalNote(dto: NoteDto): Note {
  return {
    id: dto.id,
    subjectId: dto.subjectId ?? undefined,
    title: dto.title,
    content: dto.content,
    pinned: dto.pinned,
    updatedAt: dto.updatedAt,
  }
}

// Local working copy the editor mutates; rebuilt whenever a load completes.
const notes = ref<Note[]>([])

const { data: noteList, loading, error, reload } = useAsync(listNotes)

watch(noteList, (list) => {
  if (!list) return
  notes.value = list.map(toLocalNote)
  if (!selectedId.value && notes.value.length > 0) {
    selectedId.value = notes.value[0]!.id
  }
})

const search = ref('')
const filteredNotes = computed(() => {
  const query = search.value.trim().toLowerCase()
  const list = [...notes.value].sort(
    (a, b) => Number(b.pinned) - Number(a.pinned) || b.updatedAt - a.updatedAt,
  )
  return query ? list.filter((n) => n.title.toLowerCase().includes(query)) : list
})

const selectedId = ref<string | null>(typeof route.query.note === 'string' ? route.query.note : null)
const selected = computed(() => notes.value.find((n) => n.id === selectedId.value) ?? null)
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

// --- Persistence --------------------------------------------------------

const saving = ref(false)

async function saveNote() {
  const note = selected.value
  if (!note) return
  saving.value = true
  try {
    const updated = await updateNote(note.id, {
      title: note.title,
      content: note.content,
      pinned: note.pinned,
    })
    note.title = updated.title
    note.content = updated.content
    note.pinned = updated.pinned
    note.updatedAt = updated.updatedAt
  } catch (error) {
    console.error(error)
  } finally {
    saving.value = false
  }
}

async function createNewNote() {
  try {
    const created = await createNote({ title: t('notes.untitled'), content: '' })
    notes.value.unshift(toLocalNote(created))
    selectedId.value = created.id
  } catch (error) {
    console.error(error)
  }
}

async function togglePin() {
  const note = selected.value
  if (!note) return
  note.pinned = !note.pinned
  await saveNote()
}

const deleteTarget = ref<Note | null>(null)

async function confirmDeleteNote() {
  const note = deleteTarget.value
  if (!note) return
  try {
    await apiDeleteNote(note.id)
    notes.value = notes.value.filter((n) => n.id !== note.id)
    if (selectedId.value === note.id) {
      selectedId.value = notes.value[0]?.id ?? null
    }
  } catch (error) {
    console.error(error)
  } finally {
    deleteTarget.value = null
  }
}

// --- AI toolbar ----------------------------------------------------------

const contentEditor = ref<HTMLTextAreaElement | null>(null)
const aiBusyAction = ref<NoteAiAction | 'flashcards' | null>(null)

interface AiActionResult {
  action: NoteAiAction
  text: string
  range: { start: number; end: number } | null
}

const aiResult = ref<AiActionResult | null>(null)
const flashcardsResult = ref<{ deckName: string; cardCount: number } | null>(null)

const noteActionKeys: NoteAiAction[] = [
  'explain',
  'rewrite',
  'continue',
  'simplify',
  'expand',
  'translate',
  'summarize',
]

const actionIcons: Record<NoteAiAction, IconName> = {
  explain: 'info',
  rewrite: 'pencil',
  continue: 'arrow-right',
  simplify: 'minus',
  expand: 'plus',
  translate: 'globe',
  summarize: 'file-text',
}

function currentSelectionRange(): { start: number; end: number } | null {
  const el = contentEditor.value
  if (!el || el.selectionStart === el.selectionEnd) return null
  return { start: el.selectionStart, end: el.selectionEnd }
}

async function runNoteAction(action: NoteAiAction) {
  const note = selected.value
  if (!note || aiBusyAction.value) return
  const range = currentSelectionRange()
  const text = range ? note.content.slice(range.start, range.end) : note.content
  if (!text.trim()) return

  aiBusyAction.value = action
  try {
    const result = await noteAiAction({
      action,
      text,
      subjectName: subjectOf(note.subjectId)?.name,
    })
    aiResult.value = { action, text: result.content, range }
  } catch (error) {
    console.error(error)
  } finally {
    aiBusyAction.value = null
  }
}

function applyAiResult() {
  const note = selected.value
  const result = aiResult.value
  if (!note || !result) return
  if (result.action === 'continue') {
    const separator = note.content === '' || note.content.endsWith('\n') ? '' : '\n'
    note.content = note.content + separator + result.text
  } else if (result.range) {
    note.content = note.content.slice(0, result.range.start) + result.text + note.content.slice(result.range.end)
  } else {
    note.content = result.text
  }
  aiResult.value = null
  saveNote()
}

async function generateFlashcardsFromNote() {
  const note = selected.value
  if (!note || aiBusyAction.value) return
  const range = currentSelectionRange()
  const text = range ? note.content.slice(range.start, range.end) : note.content
  if (!text.trim()) return

  aiBusyAction.value = 'flashcards'
  try {
    const deck = await generateFlashcards({
      text,
      subjectName: subjectOf(note.subjectId)?.name,
      deckName: note.title,
    })
    flashcardsResult.value = { deckName: deck.name, cardCount: deck.cardCount }
  } catch (error) {
    console.error(error)
  } finally {
    aiBusyAction.value = null
  }
}

function goToFlashcards() {
  flashcardsResult.value = null
  router.push({ name: 'flashcards' })
}
</script>

<template>
  <div class="notes">
    <aside class="list-panel">
      <div class="list-head">
        <h1 class="list-title">{{ t('notes.title') }}</h1>
        <AppButton size="sm" variant="soft" icon-left="plus" @click="createNewNote">
          {{ t('notes.newNote') }}
        </AppButton>
      </div>
      <div class="list-search">
        <AppSearch v-model="search" :placeholder="t('notes.searchPlaceholder')" />
      </div>
      <div v-if="loading" class="list-state">
        <AppSkeleton :lines="6" />
      </div>
      <AppEmpty v-else-if="error" icon="alert-circle" :title="t(error.messageKey)">
        <template #action>
          <AppButton size="sm" variant="soft" @click="reload">{{ t('common.retry') }}</AppButton>
        </template>
      </AppEmpty>
      <p v-else-if="filteredNotes.length === 0" class="list-empty">{{ t('notes.empty') }}</p>
      <ul v-else class="note-list">
        <li v-for="note in filteredNotes" :key="note.id">
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
          <AppInput v-model="selected.title" class="editor-title-input" size="lg" @blur="saveNote" />
          <div class="editor-meta">
            <AppTag
              v-if="subjectOf(selected.subjectId)"
              size="sm"
              tone="secondary"
            >{{ subjectOf(selected.subjectId)!.name }}</AppTag>
            <span class="editor-date">
              {{ saving ? t('notes.saving') : t('notes.updated', { time: d(selected.updatedAt, 'long') }) }}
            </span>
            <div class="editor-head-actions">
              <AppTooltip :content="selected.pinned ? t('notes.unpin') : t('notes.pin')">
                <AppButton
                  variant="ghost"
                  :tone="selected.pinned ? 'warning' : 'secondary'"
                  size="sm"
                  icon-left="star"
                  :aria-label="selected.pinned ? t('notes.unpin') : t('notes.pin')"
                  @click="togglePin"
                />
              </AppTooltip>
              <AppTooltip :content="t('notes.deleteNote')">
                <AppButton
                  variant="ghost"
                  tone="danger"
                  size="sm"
                  icon-left="trash"
                  :aria-label="t('notes.deleteNote')"
                  @click="deleteTarget = selected"
                />
              </AppTooltip>
            </div>
          </div>
        </header>

        <div class="ai-toolbar">
          <AppTooltip v-for="action in noteActionKeys" :key="action" :content="t(`notes.ai.${action}`)">
            <AppButton
              variant="ghost"
              tone="secondary"
              size="sm"
              :icon-left="actionIcons[action]"
              :loading="aiBusyAction === action"
              :disabled="aiBusyAction !== null"
              :aria-label="t(`notes.ai.${action}`)"
              @click="runNoteAction(action)"
            />
          </AppTooltip>
          <span class="ai-toolbar-divider"></span>
          <AppTooltip :content="t('notes.ai.generateFlashcards')">
            <AppButton
              variant="ghost"
              tone="secondary"
              size="sm"
              icon-left="layers"
              :loading="aiBusyAction === 'flashcards'"
              :disabled="aiBusyAction !== null"
              :aria-label="t('notes.ai.generateFlashcards')"
              @click="generateFlashcardsFromNote"
            />
          </AppTooltip>
        </div>

        <textarea
          ref="contentEditor"
          v-model="selected.content"
          class="editor-content"
          :placeholder="t('notes.contentPlaceholder')"
          @blur="saveNote"
        ></textarea>
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

    <AppDialog
      :model-value="aiResult !== null"
      :title="aiResult ? t(`notes.ai.${aiResult.action}`) : ''"
      width="560px"
      @update:model-value="(open) => { if (!open) aiResult = null }"
    >
      <p class="ai-result-text">{{ aiResult?.text }}</p>
      <template #footer>
        <AppButton variant="soft" tone="secondary" @click="aiResult = null">
          {{ t('common.cancel') }}
        </AppButton>
        <AppButton v-if="aiResult?.action === 'continue'" @click="applyAiResult">
          {{ t('notes.ai.insert') }}
        </AppButton>
        <AppButton v-else-if="aiResult?.action !== 'explain'" @click="applyAiResult">
          {{ t('notes.ai.apply') }}
        </AppButton>
      </template>
    </AppDialog>

    <AppDialog
      :model-value="flashcardsResult !== null"
      :title="t('notes.ai.flashcardsCreated')"
      width="420px"
      @update:model-value="(open) => { if (!open) flashcardsResult = null }"
    >
      <p v-if="flashcardsResult">
        {{ t('notes.ai.flashcardsCreatedBody', { count: flashcardsResult.cardCount, deck: flashcardsResult.deckName }) }}
      </p>
      <template #footer>
        <AppButton variant="soft" tone="secondary" @click="flashcardsResult = null">
          {{ t('common.cancel') }}
        </AppButton>
        <AppButton @click="goToFlashcards">{{ t('notes.ai.viewFlashcards') }}</AppButton>
      </template>
    </AppDialog>

    <AppDialog
      :model-value="deleteTarget !== null"
      :title="t('notes.deleteConfirm.title')"
      width="420px"
      @update:model-value="(open) => { if (!open) deleteTarget = null }"
    >
      <p>{{ t('notes.deleteConfirm.body', { title: deleteTarget?.title ?? '' }) }}</p>
      <template #footer>
        <AppButton variant="soft" tone="secondary" @click="deleteTarget = null">
          {{ t('common.cancel') }}
        </AppButton>
        <AppButton tone="danger" @click="confirmDeleteNote">{{ t('common.delete') }}</AppButton>
      </template>
    </AppDialog>
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

.list-state {
  padding: var(--space-4);
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

/* Editor */
.editor {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: var(--space-10) var(--space-12);
  overflow: hidden;
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
  width: 100%;
  margin: 0 auto var(--space-3);
  flex-shrink: 0;
}

.editor-title-input {
  margin-bottom: var(--space-2);
}

.editor-title-input :deep(input) {
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
  flex: 1;
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

.editor-head-actions {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.ai-toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  max-width: 720px;
  width: 100%;
  margin: 0 auto var(--space-4);
  padding-bottom: var(--space-3);
  border-bottom: var(--border-width-sm) solid var(--color-border);
  flex-shrink: 0;
}

.ai-toolbar-divider {
  width: var(--border-width-sm);
  height: 20px;
  margin: 0 var(--space-1);
  background-color: var(--color-border);
}

.editor-content {
  flex: 1;
  max-width: 720px;
  width: 100%;
  margin: 0 auto;
  padding: 0;
  border: none;
  background: transparent;
  resize: none;
  font-family: inherit;
  font-size: var(--text-base);
  line-height: 1.8;
  color: var(--color-text);
  overflow-y: auto;
}

.editor-content:focus {
  outline: none;
}

.editor-content::placeholder {
  color: var(--color-text-tertiary);
}

.ai-result-text {
  margin: 0;
  font-size: var(--text-sm);
  line-height: var(--leading-normal);
  white-space: pre-wrap;
  overflow-wrap: break-word;
  max-height: 50vh;
  overflow-y: auto;
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
