import { api } from '@/api/http'
import type { FlashcardDeckDto } from './flashcard'

/** Mirror of ConversationSummaryResponse.java. */
export interface ConversationSummaryDto {
  id: string
  title: string
  subjectName: string | null
  archived: boolean
  updatedAt: number
}

/** Mirror of MessageResponse.java. */
export interface AiMessageDto {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  createdAt: number
  truncated: boolean
}

/** Mirror of ConversationDetailResponse.java. */
export interface ConversationDetailDto extends ConversationSummaryDto {
  messages: AiMessageDto[]
}

export function listConversations() {
  return api.get<ConversationSummaryDto[]>('/v1/ai/conversations')
}

export function createConversation(payload: { title?: string; subjectName?: string } = {}) {
  return api.post<ConversationDetailDto>('/v1/ai/conversations', payload)
}

export function getConversation(id: string) {
  return api.get<ConversationDetailDto>(`/v1/ai/conversations/${id}`)
}

export function renameConversation(id: string, title: string) {
  return api.patch<ConversationSummaryDto>(`/v1/ai/conversations/${id}`, { title })
}

export function archiveConversation(id: string, archived: boolean) {
  return api.patch<ConversationSummaryDto>(`/v1/ai/conversations/${id}`, { archived })
}

export function deleteConversation(id: string) {
  return api.delete<void>(`/v1/ai/conversations/${id}`)
}

// --- Generation -------------------------------------------------------------

/** Client-supplied subject context — Subjects are mock-data-only, see docs/ai-engine.md. */
export interface SubjectContext {
  subjectName?: string
  subjectDescription?: string
}

export interface GenerationResult {
  content: string
}

export interface QuizQuestionDto {
  question: string
  options: string[]
  answer: string
  explanation: string
}

export interface QuizResultDto {
  questions: QuizQuestionDto[]
}

export interface StudyPlanResultDto {
  dailyTasks: string[]
  weeklyPlan: string
  reviewSchedule: string
  estimatedCompletion: string
  suggestions: string[]
}

export function generateExplain(payload: SubjectContext & { topic: string }) {
  return api.post<GenerationResult>('/v1/ai/generate/explain', payload)
}

export function generateSummary(payload: SubjectContext & { text: string }) {
  return api.post<GenerationResult>('/v1/ai/generate/summary', payload)
}

export function generateSuggestions(payload: SubjectContext) {
  return api.post<GenerationResult>('/v1/ai/generate/suggestions', payload)
}

export function generateQuiz(payload: SubjectContext & { text?: string }) {
  return api.post<QuizResultDto>('/v1/ai/generate/quiz', payload)
}

export function generateFlashcards(
  payload: SubjectContext & { text?: string; deckName?: string; deckDescription?: string },
) {
  return api.post<FlashcardDeckDto>('/v1/ai/generate/flashcards', payload)
}

export function generateStudyPlan(payload: {
  goal: string
  availableMinutesPerDay: number
  subjects?: string[]
}) {
  return api.post<StudyPlanResultDto>('/v1/ai/generate/study-plan', payload)
}

export type NoteAiAction =
  | 'explain'
  | 'rewrite'
  | 'continue'
  | 'simplify'
  | 'expand'
  | 'translate'
  | 'summarize'

export function noteAiAction(payload: SubjectContext & { action: NoteAiAction; text: string }) {
  return api.post<GenerationResult>('/v1/ai/notes/actions', {
    ...payload,
    action: payload.action.toUpperCase(),
  })
}

export function generateWeeklySummary(statsSnapshot: string) {
  return api.post<GenerationResult>('/v1/ai/analytics/weekly-summary', { statsSnapshot })
}

export function generateWeakPoints(statsSnapshot: string) {
  return api.post<GenerationResult>('/v1/ai/analytics/weak-points', { statsSnapshot })
}
