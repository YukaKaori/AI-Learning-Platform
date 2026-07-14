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
  /** Linked subject id (real FK since Phase 7; see docs/ai-engine.md). */
  subjectId?: string
  /** Display snapshot of the linked subject's name, taken when linked. */
  subjectName?: string
  archived?: boolean
  /** Epoch ms of the last message. */
  updatedAt: number
  messages: ChatMessage[]
}
