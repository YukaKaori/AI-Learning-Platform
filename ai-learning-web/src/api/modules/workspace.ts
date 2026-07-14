import { api } from '@/api/http'
import type { ConversationSummaryDto } from './ai'
import type { ActivityDayDto } from './analytics'
import type { StudySessionDto } from './calendar'
import type { TaskDto } from './task'

/**
 * Mirror of WorkspaceSummaryResponse.Stats. `dailyGoalMinutes` comes from
 * preferences (or its default 60); `dueCards` counts flashcards with
 * `dueAt ≤ now`; `activeSubjects` counts subjects in status `active`.
 */
export interface WorkspaceStatsDto {
  streakDays: number
  studiedTodayMinutes: number
  dailyGoalMinutes: number
  dueCards: number
  activeSubjects: number
}

/**
 * Mirror of WorkspaceSummaryResponse.ContinueLearningItem — an active subject
 * ranked by its most recent linked activity (epoch ms).
 */
export interface ContinueLearningItemDto {
  id: string
  name: string
  color: string | null
  icon: string | null
  progress: number
  lastActivityAt: number
}

/** Mirror of WorkspaceSummaryResponse.RecentNote — slim row, no content payload. */
export interface RecentNoteDto {
  id: string
  subjectId: string | null
  title: string
  updatedAt: number
}

/**
 * Mirror of WorkspaceSummaryResponse.java — everything the dashboard renders
 * in one round trip (one loading state). Section shapes are reused from their
 * owning modules; the workspace never redefines them.
 */
export interface WorkspaceSummaryDto {
  stats: WorkspaceStatsDto
  continueLearning: ContinueLearningItemDto[]
  upcomingTasks: TaskDto[]
  recentConversations: ConversationSummaryDto[]
  recentNotes: RecentNoteDto[]
  todaySessions: StudySessionDto[]
  weekActivity: ActivityDayDto[]
}

export function getWorkspaceSummary() {
  return api.get<WorkspaceSummaryDto>('/v1/workspace/summary')
}
