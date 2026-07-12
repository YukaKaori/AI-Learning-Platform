import { mockSubjects } from '@/features/subjects/mock'
import { totalDue } from '@/features/flashcards/mock'
import type { Subject } from '@/features/subjects/types'

/**
 * Phase 5 demo aggregates for the learning dashboard — the shape of the
 * future workspace read-model endpoint (backend `workspace` package).
 */

export interface DashboardStats {
  streakDays: number
  studiedTodayMinutes: number
  todayGoalMinutes: number
  dueCards: number
  activeSubjects: number
}

export const dashboardStats: DashboardStats = {
  streakDays: 12,
  studiedTodayMinutes: 62,
  todayGoalMinutes: 90,
  dueCards: totalDue(),
  activeSubjects: mockSubjects.filter((s) => s.status === 'active').length,
}

/** Active subjects, most recently studied first — the "continue learning" rail. */
export const continueLearning: Subject[] = mockSubjects
  .filter((s) => s.status === 'active')
  .sort((a, b) => b.lastStudiedAt - a.lastStudiedAt)
  .slice(0, 3)
