import { mockSubjects } from '@/features/subjects/mock'
import type { Subject } from '@/features/subjects/types'

/**
 * Phase 5 demo fixtures. Real analytics are derived server-side from
 * study_sessions / tasks / flashcards; these shapes are the read models the
 * analytics API will eventually return.
 */

export interface DayActivity {
  /** Epoch ms at local midnight. */
  day: number
  minutes: number
}

export interface SubjectShare {
  subject: Subject
  minutes: number
}

export interface AnalyticsSummary {
  weekMinutes: number
  weekDelta: number
  streakDays: number
  taskCompletion: number
  aiChatsThisWeek: number
}

const DAY = 86_400_000

function dayStart(offset: number): number {
  const d = new Date()
  d.setHours(0, 0, 0, 0)
  return d.getTime() + offset * DAY
}

/** Deterministic pseudo-random so the demo is stable across reloads. */
function seeded(i: number): number {
  const x = Math.sin(i * 12.9898) * 43758.5453
  return x - Math.floor(x)
}

export const summary: AnalyticsSummary = {
  weekMinutes: 742,
  weekDelta: 12,
  streakDays: 12,
  taskCompletion: 68,
  aiChatsThisWeek: 9,
}

/** Last 7 days, oldest first. */
export const weeklyActivity: DayActivity[] = Array.from({ length: 7 }, (_, i) => {
  const offset = i - 6
  return {
    day: dayStart(offset),
    minutes: offset === 0 ? 62 : Math.round(40 + seeded(i + 3) * 110),
  }
})

/** Study-time share per subject, last 30 days — descending. */
export const subjectShares: SubjectShare[] = mockSubjects
  .map((subject, i) => ({
    subject,
    minutes: Math.round(subject.studyMinutes * (0.25 + seeded(i + 11) * 0.15)),
  }))
  .sort((a, b) => b.minutes - a.minutes)

/** 12 weeks × 7 days of study intensity (minutes), oldest week first. */
export const heatmap: DayActivity[][] = Array.from({ length: 12 }, (_, w) =>
  Array.from({ length: 7 }, (_, d) => {
    const offset = (11 - w) * -7 - (6 - d)
    const quiet = seeded(w * 7 + d) < 0.18
    return {
      day: dayStart(offset),
      minutes: quiet ? 0 : Math.round(seeded(w * 13 + d * 5) * 150),
    }
  }),
)

export const HEATMAP_MAX = 150
