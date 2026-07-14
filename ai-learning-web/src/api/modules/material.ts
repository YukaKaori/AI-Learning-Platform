import { api } from '@/api/http'

export type MaterialType = 'pdf' | 'markdown' | 'video' | 'article' | 'link' | 'document'

/**
 * Mirror of MaterialResponse.java. Materials are metadata + external links
 * only this phase — `sizeBytes` stays null until file upload lands (D8).
 */
export interface MaterialDto {
  id: string
  subjectId: string
  title: string
  type: MaterialType
  description: string | null
  sourceUrl: string | null
  sizeBytes: number | null
  createdAt: number
}

export interface CreateMaterialPayload {
  title: string
  type: MaterialType
  description?: string
  sourceUrl?: string
}

export interface UpdateMaterialPayload {
  title?: string
  type?: MaterialType
  description?: string
  sourceUrl?: string
}

// Collection routes nest under the subject; item routes are flat — the same
// convention as flashcard decks/cards.

export function listMaterials(subjectId: string) {
  return api.get<MaterialDto[]>(`/v1/subjects/${subjectId}/materials`)
}

export function createMaterial(subjectId: string, payload: CreateMaterialPayload) {
  return api.post<MaterialDto>(`/v1/subjects/${subjectId}/materials`, payload)
}

export function updateMaterial(id: string, payload: UpdateMaterialPayload) {
  return api.put<MaterialDto>(`/v1/materials/${id}`, payload)
}

export function deleteMaterial(id: string) {
  return api.delete<void>(`/v1/materials/${id}`)
}
