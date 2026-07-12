/**
 * Notes — markdown-first knowledge capture. Mirrors the backend `notes`
 * table; outline and excerpt are derived from content, never stored.
 */

export interface Note {
  id: string
  subjectId?: string
  title: string
  /** Raw markdown source. */
  content: string
  pinned: boolean
  /** Epoch ms. */
  updatedAt: number
}

export interface OutlineItem {
  /** Heading depth 1–3. */
  level: number
  text: string
}

/** Extract h1–h3 headings from markdown for the outline rail. */
export function outlineOf(note: Note): OutlineItem[] {
  const items: OutlineItem[] = []
  for (const line of note.content.split('\n')) {
    const match = /^(#{1,3})\s+(.+)$/.exec(line.trim())
    if (match) items.push({ level: match[1]!.length, text: match[2]! })
  }
  return items
}

/** First non-heading, non-empty line — the list-row preview. */
export function excerptOf(note: Note): string {
  for (const line of note.content.split('\n')) {
    const text = line.trim()
    if (text && !text.startsWith('#')) return text
  }
  return ''
}
