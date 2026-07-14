<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useSubjectsStore } from '@/stores/subjects'
import { accentColor, subjectAccentOf } from '../types'

/**
 * The one way to link an entity to a subject — shared by the note editor,
 * deck dialog and AI Tutor. `null` means "no subject"; clearing the select
 * emits `null` (callers translate that to the wire's `''` unlink sentinel
 * where needed). Fills its container's width.
 */
const props = withDefaults(
  defineProps<{
    modelValue: string | null
    /** Offer only active subjects (a linked non-active one stays selectable). */
    activeOnly?: boolean
    size?: 'small' | 'default'
    placeholder?: string
  }>(),
  {
    activeOnly: true,
    size: 'default',
    placeholder: undefined,
  },
)

const emit = defineEmits<{ 'update:modelValue': [id: string | null] }>()

const { t } = useI18n()
const subjectsStore = useSubjectsStore()

onMounted(() => {
  void subjectsStore.load()
})

const options = computed(() => {
  const list = props.activeOnly ? subjectsStore.activeSubjects : subjectsStore.subjects
  // A completed/archived subject that is already linked must stay visible.
  const current = subjectsStore.byId(props.modelValue)
  return current && !list.some((subject) => subject.id === current.id) ? [current, ...list] : list
})

function onChange(value: string | null | undefined) {
  emit('update:modelValue', value ? String(value) : null)
}
</script>

<template>
  <el-select
    :model-value="modelValue ?? undefined"
    :size="size"
    :placeholder="placeholder ?? t('subjects.picker.placeholder')"
    :loading="subjectsStore.loading"
    clearable
    class="subject-picker"
    @update:model-value="onChange"
  >
    <el-option v-for="subject in options" :key="subject.id" :value="subject.id" :label="subject.name">
      <span class="option">
        <span
          class="option-dot"
          :style="{ backgroundColor: accentColor(subjectAccentOf(subject.color)) }"
          aria-hidden="true"
        ></span>
        {{ subject.name }}
      </span>
    </el-option>
  </el-select>
</template>

<style scoped>
.subject-picker {
  width: 100%;
}

.option {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
}

.option-dot {
  width: 8px;
  height: 8px;
  border-radius: var(--radius-full);
  flex-shrink: 0;
}
</style>
