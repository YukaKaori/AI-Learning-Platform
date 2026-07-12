import type { ChatMessage } from './types'

/**
 * Chat provider abstraction — the ONLY seam through which the chat UI talks
 * to a model. The next phase adds a backend-SSE provider (Claude/OpenAI/…
 * behind the server's AiService); the UI must not change when it does.
 */
export interface ChatProvider {
  /** Stable identifier, e.g. 'mock', 'server-sse'. */
  readonly id: string
  /**
   * Stream the assistant reply for the given history as text chunks.
   * Consumers append chunks as they arrive; the generator completing means
   * the reply is finished. Abort by breaking out of the loop.
   */
  streamReply(history: ChatMessage[]): AsyncGenerator<string, void, undefined>
}

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

/**
 * Demo provider: types out a canned, i18n-supplied reply so the streaming UI
 * path (chunk append, auto-scroll, busy state) is real before any model is.
 */
export class MockChatProvider implements ChatProvider {
  readonly id = 'mock'

  /** Builds the canned reply from the user's question; injected so the provider stays i18n-agnostic. */
  constructor(private readonly replyFor: (question: string) => string) {}

  async *streamReply(history: ChatMessage[]): AsyncGenerator<string, void, undefined> {
    const question = [...history].reverse().find((m) => m.role === 'user')?.content ?? ''
    const reply = this.replyFor(question)
    // Chunk on grapheme-ish boundaries so CJK text streams naturally too.
    const chunks = reply.match(/\S{1,3}\s*|\s+/g) ?? [reply]
    await sleep(350)
    for (const chunk of chunks) {
      yield chunk
      await sleep(18)
    }
  }
}
