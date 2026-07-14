import type { IconName } from '@/components'
import { iconRegistry } from '@/components/icons/registry'

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

/** The dialog's accent choices — the full fixed set, in display order. */
export const SUBJECT_ACCENTS: SubjectAccent[] = ['indigo', 'teal', 'amber', 'rose', 'violet']

/**
 * Narrows the backend's opaque `color` string to a known accent. Unknown or
 * missing values fall back to `indigo` so a subject always renders branded.
 */
export function subjectAccentOf(color: string | null | undefined): SubjectAccent {
  return (SUBJECT_ACCENTS as string[]).includes(color ?? '') ? (color as SubjectAccent) : 'indigo'
}

/** The dialog's icon choices — a curated subset of the icon registry. */
export const SUBJECT_ICONS: IconName[] = [
  'book-open',
  'brain',
  'code',
  'globe',
  'network',
  'graduation-cap',
  'palette',
  'chart-line',
  'compass',
  'zap',
]

/**
 * Narrows the backend's `icon` string to a registered icon name, falling back
 * to `book-open` for unknown or missing values.
 */
export function subjectIconOf(icon: string | null | undefined): IconName {
  return icon && icon in iconRegistry ? (icon as IconName) : 'book-open'
}

export const MATERIAL_TYPE_ICON: Record<MaterialType, IconName> = {
  pdf: 'file-text',
  markdown: 'file-text',
  video: 'video',
  article: 'book-open',
  link: 'link',
  document: 'file',
}
