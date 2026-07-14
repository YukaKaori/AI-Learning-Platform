import { api } from '@/api/http'

export type SubjectStatus = 'active' | 'completed' | 'archived'

/**
 * Mirror of SubjectResponse.java. `color` is an opaque accent token name
 * resolved by the frontend (e.g. `indigo`); the count/minutes fields are
 * derived server-side. `lastStudiedAt` is null until a linked study session
 * has ended.
 */
export interface SubjectDto {
  id: string
  name: string
  color: string | null
  icon: string | null
  description: string | null
  status: SubjectStatus
  progress: number
  materialCount: number
  noteCount: number
  deckCount: number
  studyMinutes: number
  lastStudiedAt: number | null
  createdAt: number
  updatedAt: number
}

export interface CreateSubjectPayload {
  name: string
  color?: string
  icon?: string
  description?: string
}

export interface UpdateSubjectPayload {
  name?: string
  color?: string
  icon?: string
  description?: string
  status?: SubjectStatus
  progress?: number
}

export function listSubjects() {
  return api.get<SubjectDto[]>('/v1/subjects')
}

export function getSubject(id: string) {
  return api.get<SubjectDto>(`/v1/subjects/${id}`)
}

export function createSubject(payload: CreateSubjectPayload) {
  return api.post<SubjectDto>('/v1/subjects', payload)
}

export function updateSubject(id: string, payload: UpdateSubjectPayload) {
  return api.put<SubjectDto>(`/v1/subjects/${id}`, payload)
}

/**
 * Deletes the subject, soft-deletes its materials, and unlinks (nullifies
 * `subjectId` on) its notes, decks, tasks, sessions and conversations — the
 * delete dialog must state this explicitly (D2).
 */
export function deleteSubject(id: string) {
  return api.delete<void>(`/v1/subjects/${id}`)
}
