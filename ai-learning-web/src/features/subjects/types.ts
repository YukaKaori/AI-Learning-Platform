import type { IconName } from '@/components'

/**
 * Subject — the anchor of the learning domain. Mirrors the backend `subjects`
 * table (see docs/product-domain.md); ids are snowflake strings once the API
 * lands, so they are strings from day one.
 */

/** Named accent, resolved to the theme-aware `--accent-*` token. Fixed set — see tokens.css. */
export type SubjectAccent = 'indigo' | 'teal' | 'amber' | 'rose' | 'violet'

export type SubjectStatus = 'active' | 'completed' | 'archived'

export interface Subject {
  id: string
  name: string
  accent: SubjectAccent
  icon: IconName
  description: string
  status: SubjectStatus
  /** Completion 0–100. */
  progress: number
  /** Derived from study sessions on the backend; denormalized in mock data. */
  studyMinutes: number
  /** Epoch ms of the most recent study activity. */
  lastStudiedAt: number
}

export type MaterialType = 'pdf' | 'markdown' | 'video' | 'article' | 'link' | 'document'

export interface LearningMaterial {
  id: string
  subjectId: string
  title: string
  type: MaterialType
  description?: string
  sourceUrl?: string
  /** Epoch ms. */
  addedAt: number
}

/** CSS value for a subject accent — always the token, never a raw hex. */
export function accentColor(accent: SubjectAccent): string {
  return `var(--accent-${accent})`
}

export const MATERIAL_TYPE_ICON: Record<MaterialType, IconName> = {
  pdf: 'file-text',
  markdown: 'file-text',
  video: 'video',
  article: 'book-open',
  link: 'link',
  document: 'file',
}
