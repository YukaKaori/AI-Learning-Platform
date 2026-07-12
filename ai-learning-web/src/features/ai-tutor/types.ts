/**
 * AI Tutor — conversation shapes shared by the chat UI and the provider
 * abstraction. Persistence (backend `ai_conversations` / `ai_messages`)
 * arrives in the AI phase together with AiService.
 */

export type ChatRole = 'user' | 'assistant'

export interface ChatMessage {
  id: string
  role: ChatRole
  content: string
  /** Epoch ms. */
  createdAt: number
  /** True while the assistant reply is still streaming in. */
  streaming?: boolean
}

export interface Conversation {
  id: string
  title: string
  /**
   * Snapshot of the subject name the chat was started from — not a real
   * subjectId FK. Subjects are still frontend-mock-only; see docs/ai-engine.md.
   */
  subjectName?: string
  archived?: boolean
  /** Epoch ms of the last message. */
  updatedAt: number
  messages: ChatMessage[]
}
