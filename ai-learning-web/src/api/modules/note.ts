import { api } from '@/api/http'

/** Mirror of NoteResponse.java. */
export interface NoteDto {
  id: string
  subjectId: string | null
  title: string
  content: string
  pinned: boolean
  updatedAt: number
}

/** `subjectId` must reference a subject owned by the caller. */
export interface CreateNotePayload {
  title: string
  content?: string
  pinned?: boolean
  subjectId?: string
}

/**
 * Partial update — omitted fields keep their value; `subjectId: ''` unlinks
 * the note from its subject.
 */
export interface UpdateNotePayload {
  title?: string
  content?: string
  pinned?: boolean
  subjectId?: string
}

export function listNotes() {
  return api.get<NoteDto[]>('/v1/notes')
}

export function getNote(id: string) {
  return api.get<NoteDto>(`/v1/notes/${id}`)
}

export function createNote(payload: CreateNotePayload) {
  return api.post<NoteDto>('/v1/notes', payload)
}

export function updateNote(id: string, payload: UpdateNotePayload) {
  return api.put<NoteDto>(`/v1/notes/${id}`, payload)
}

export function deleteNote(id: string) {
  return api.delete<void>(`/v1/notes/${id}`)
}
