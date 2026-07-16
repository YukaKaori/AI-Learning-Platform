import type { IconName } from '@/components'
import { iconRegistry } from '@/components/icons/registry'

/**
 * Subject presentation helpers — the UI-side vocabulary (accents, icons,
 * material-type icons) layered over the wire types in `api/modules/subject.ts`.
 */

/** Named accent, resolved to the theme-aware `--accent-*` token. Fixed set — see tokens.css. */
export type SubjectAccent = 'indigo' | 'teal' | 'amber' | 'rose' | 'violet'

export type MaterialType = 'pdf' | 'markdown' | 'video' | 'article' | 'link' | 'document'

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
