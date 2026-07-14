import { defineStore } from 'pinia'
import * as subjectApi from '@/api/modules/subject'
import type { CreateSubjectPayload, SubjectDto, UpdateSubjectPayload } from '@/api/modules/subject'
import { toApiError, type ApiError } from '@/api/types'

/**
 * Cross-view subject cache — the single client-side source of subject data.
 * Views enrich foreign keys (`note.subjectId`, `deck.subjectId`, …) through
 * `byId` instead of fetching per row; mutations write through the API and
 * update the cache in place, so every consumer stays consistent.
 *
 * `load()` follows the useAsync/D3 state contract (`loading`/`error` +
 * reload-on-retry); it lives in a store rather than a composable because the
 * cache outlives any single view.
 */
export const useSubjectsStore = defineStore('subjects', {
  state: () => ({
    subjects: [] as SubjectDto[],
    /** True once a load has succeeded — `load()` is a no-op from then on. */
    loaded: false,
    loading: false,
    error: null as ApiError | null,
  }),

  getters: {
    byId(state): (id: string | null | undefined) => SubjectDto | undefined {
      return (id) => (id ? state.subjects.find((subject) => subject.id === id) : undefined)
    },
    activeSubjects(state): SubjectDto[] {
      return state.subjects.filter((subject) => subject.status === 'active')
    },
  },

  actions: {
    /**
     * Loads the list once; pass `force` to re-fetch (the error-retry path
     * always forces). Concurrent calls coalesce onto the in-flight request.
     */
    async load(force = false): Promise<void> {
      if (this.loading || (this.loaded && !force)) return
      this.loading = true
      this.error = null
      try {
        this.subjects = await subjectApi.listSubjects()
        this.loaded = true
      } catch (caught) {
        this.error = toApiError(caught)
      } finally {
        this.loading = false
      }
    },

    /** Re-fetches one subject (fresh derived counts) and syncs the cache. */
    async refresh(id: string): Promise<SubjectDto> {
      const fresh = await subjectApi.getSubject(id)
      const index = this.subjects.findIndex((subject) => subject.id === id)
      if (index >= 0) {
        this.subjects[index] = fresh
      } else {
        this.subjects.unshift(fresh)
      }
      return fresh
    },

    async create(payload: CreateSubjectPayload): Promise<SubjectDto> {
      const created = await subjectApi.createSubject(payload)
      this.subjects.unshift(created)
      return created
    },

    async update(id: string, payload: UpdateSubjectPayload): Promise<SubjectDto> {
      const updated = await subjectApi.updateSubject(id, payload)
      const index = this.subjects.findIndex((subject) => subject.id === id)
      if (index >= 0) {
        this.subjects[index] = updated
      }
      return updated
    },

    /**
     * Deletes per D2: the subject's materials go with it; linked notes,
     * decks, tasks, sessions and conversations are kept and unlinked.
     */
    async remove(id: string): Promise<void> {
      await subjectApi.deleteSubject(id)
      this.subjects = this.subjects.filter((subject) => subject.id !== id)
    },
  },
})
