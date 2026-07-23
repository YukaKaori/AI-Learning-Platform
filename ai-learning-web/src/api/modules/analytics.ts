import { api } from '@/api/http'

/**
 * Mirror of AnalyticsSummaryResponse.java. "This week" is the rolling 7-day
 * window ending today. `weekDeltaPercent` is null when the previous week has
 * no study time and `taskCompletionPercent` is null when there are no tasks —
 * render both as "—", never a fabricated zero.
 */
export interface AnalyticsSummaryDto {
  weekMinutes: number
  weekDeltaPercent: number | null
  streakDays: number
  taskCompletionPercent: number | null
  aiChatsThisWeek: number
  /** Graded flashcard reviews this week (Phase 15). */
  reviewsThisWeek: number
  /**
   * Of reviews recalled after a real interval this week, the % graded Hard or
   * better; null when there were none — render as "—", never a fabricated 0.
   */
  retentionPercent: number | null
}

/**
 * Mirror of ActivityDayResponse.java. `date` is an ISO local date
 * (`yyyy-MM-dd`) — a calendar bucket, not an instant, so the epoch-ms wire
 * convention deliberately doesn't apply.
 */
export interface ActivityDayDto {
  date: string
  minutes: number
  sessions: number
  /** Graded flashcard reviews on this day (Phase 15). */
  reviews: number
}

/**
 * Mirror of SubjectShareResponse.java. A null-id row carries the minutes of
 * sessions not linked to any (surviving) subject, so shares always sum to the
 * real total; percentages are computed client-side.
 */
export interface SubjectShareDto {
  subjectId: string | null
  subjectName: string | null
  color: string | null
  minutes: number
}

export function getAnalyticsSummary() {
  return api.get<AnalyticsSummaryDto>('/v1/analytics/summary')
}

/** Zero-filled per-day series; `days` is 1..90 (the heatmap fetches 84). */
export function getActivity(days = 30) {
  return api.get<ActivityDayDto[]>('/v1/analytics/activity', { params: { days } })
}

export function getSubjectShares(days = 30) {
  return api.get<SubjectShareDto[]>('/v1/analytics/subject-shares', { params: { days } })
}
