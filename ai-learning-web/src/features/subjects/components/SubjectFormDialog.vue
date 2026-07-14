<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { AppButton, AppDialog, AppIcon, AppInput } from '@/components'
import type { IconName } from '@/components'
import type { SubjectDto, SubjectStatus } from '@/api/modules/subject'
import { toApiError } from '@/api/types'
import { useSubjectsStore } from '@/stores/subjects'
import {
  accentColor,
  SUBJECT_ACCENTS,
  SUBJECT_ICONS,
  subjectAccentOf,
  subjectIconOf,
  type SubjectAccent,
} from '../types'

/**
 * Create/edit dialog for a subject. Create mode when `subject` is null;
 * edit mode (adds status + progress) when it is set. Writes through the
 * subjects store so every cached consumer updates; emits `saved` with the
 * resulting DTO.
 */
const props = defineProps<{
  modelValue: boolean
  subject?: SubjectDto | null
}>()

const emit = defineEmits<{
  'update:modelValue': [open: boolean]
  saved: [subject: SubjectDto]
}>()

const { t } = useI18n()
const subjectsStore = useSubjectsStore()

const STATUSES: SubjectStatus[] = ['active', 'completed', 'archived']

const form = reactive({
  name: '',
  description: '',
  accent: 'indigo' as SubjectAccent,
  icon: 'book-open' as IconName,
  status: 'active' as SubjectStatus,
  progress: '0',
})

const saving = ref(false)
const errorKey = ref<string | null>(null)

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    errorKey.value = null
    form.name = props.subject?.name ?? ''
    form.description = props.subject?.description ?? ''
    form.accent = subjectAccentOf(props.subject?.color)
    form.icon = props.subject ? subjectIconOf(props.subject.icon) : 'book-open'
    form.status = props.subject?.status ?? 'active'
    form.progress = String(props.subject?.progress ?? 0)
  },
)

function close() {
  emit('update:modelValue', false)
}

function progressValue(): number | undefined {
  const parsed = Number(form.progress)
  if (!Number.isFinite(parsed)) return undefined
  return Math.min(100, Math.max(0, Math.round(parsed)))
}

async function submit() {
  const name = form.name.trim()
  if (!name || saving.value) return
  saving.value = true
  errorKey.value = null
  try {
    const saved = props.subject
      ? await subjectsStore.update(props.subject.id, {
          name,
          // '' clears the description column (partial update applies non-null).
          description: form.description.trim(),
          color: form.accent,
          icon: form.icon,
          status: form.status,
          progress: progressValue(),
        })
      : await subjectsStore.create({
          name,
          description: form.description.trim() || undefined,
          color: form.accent,
          icon: form.icon,
        })
    emit('saved', saved)
    close()
  } catch (caught) {
    errorKey.value = toApiError(caught).messageKey
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <AppDialog
    :model-value="modelValue"
    :title="subject ? t('subjects.dialog.editTitle') : t('subjects.dialog.createTitle')"
    width="480px"
    @update:model-value="(open) => emit('update:modelValue', open)"
  >
    <div class="form">
      <AppInput
        v-model="form.name"
        :label="t('subjects.dialog.name')"
        :placeholder="t('subjects.dialog.namePlaceholder')"
      />

      <div class="form-field">
        <label class="form-label">{{ t('subjects.dialog.description') }}</label>
        <textarea
          v-model="form.description"
          class="form-textarea"
          rows="3"
          :placeholder="t('subjects.dialog.descriptionPlaceholder')"
        ></textarea>
      </div>

      <div class="form-field">
        <span class="form-label">{{ t('subjects.dialog.color') }}</span>
        <div class="swatch-row" role="radiogroup" :aria-label="t('subjects.dialog.color')">
          <button
            v-for="accent in SUBJECT_ACCENTS"
            :key="accent"
            type="button"
            class="swatch"
            :class="{ selected: form.accent === accent }"
            :style="{ backgroundColor: accentColor(accent) }"
            role="radio"
            :aria-checked="form.accent === accent"
            :aria-label="accent"
            @click="form.accent = accent"
          ></button>
        </div>
      </div>

      <div class="form-field">
        <span class="form-label">{{ t('subjects.dialog.icon') }}</span>
        <div class="icon-row" role="radiogroup" :aria-label="t('subjects.dialog.icon')">
          <button
            v-for="icon in SUBJECT_ICONS"
            :key="icon"
            type="button"
            class="icon-choice"
            :class="{ selected: form.icon === icon }"
            :style="form.icon === icon ? { color: accentColor(form.accent) } : undefined"
            role="radio"
            :aria-checked="form.icon === icon"
            :aria-label="icon"
            @click="form.icon = icon"
          >
            <AppIcon :name="icon" />
          </button>
        </div>
      </div>

      <template v-if="subject">
        <div class="form-field">
          <span class="form-label">{{ t('subjects.dialog.status') }}</span>
          <div class="status-row" role="radiogroup" :aria-label="t('subjects.dialog.status')">
            <AppButton
              v-for="status in STATUSES"
              :key="status"
              size="sm"
              :variant="form.status === status ? 'solid' : 'outline'"
              :tone="form.status === status ? 'primary' : 'secondary'"
              role="radio"
              :aria-checked="form.status === status"
              @click="form.status = status"
            >
              {{ t(`subjects.status.${status}`) }}
            </AppButton>
          </div>
        </div>

        <AppInput
          v-model="form.progress"
          type="number"
          :label="t('subjects.dialog.progress')"
        />
      </template>

      <p v-if="errorKey" class="form-error" role="alert">{{ t(errorKey) }}</p>
    </div>

    <template #footer>
      <AppButton variant="soft" tone="secondary" @click="close">{{ t('common.cancel') }}</AppButton>
      <AppButton :loading="saving" :disabled="!form.name.trim()" @click="submit">
        {{ subject ? t('common.save') : t('common.create') }}
      </AppButton>
    </template>
  </AppDialog>
</template>

<style scoped>
.form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.form-label {
  font-size: var(--font-label-size);
  font-weight: var(--font-label-weight);
  color: var(--color-text-secondary);
}

.form-textarea {
  padding: var(--space-2) var(--space-3);
  border: var(--border-width-sm) solid var(--color-border);
  border-radius: var(--radius-input);
  background-color: var(--color-surface);
  font-family: inherit;
  font-size: var(--text-sm);
  line-height: var(--leading-normal);
  color: var(--color-text);
  resize: vertical;
  transition:
    border-color var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out);
}

.form-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-soft);
}

.form-textarea::placeholder {
  color: var(--color-text-tertiary);
}

.swatch-row {
  display: flex;
  gap: var(--space-2);
}

.swatch {
  width: 26px;
  height: 26px;
  border: 2px solid transparent;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: box-shadow var(--duration-fast) var(--ease-out);
}

.swatch.selected {
  box-shadow:
    0 0 0 2px var(--color-surface),
    0 0 0 4px currentColor;
}

.swatch:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.icon-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.icon-choice {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: var(--border-width-sm) solid var(--color-border);
  border-radius: var(--radius-md);
  background-color: var(--color-surface);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition:
    border-color var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out),
    background-color var(--duration-fast) var(--ease-out);
}

.icon-choice:hover {
  border-color: var(--color-primary);
}

.icon-choice.selected {
  border-color: currentColor;
  background-color: color-mix(in srgb, currentColor 10%, transparent);
}

.status-row {
  display: flex;
  gap: var(--space-2);
}

.form-error {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--color-danger);
}
</style>
