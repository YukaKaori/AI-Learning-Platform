import { refreshTokenAfterUnauthorized } from '@/api/http'
import { tokenStorage } from '@/api/token-storage'
import type { SendMessagePayload } from '@/api/modules/ai'
import type { ChatMessage } from './types'

/**
 * Chat provider abstraction — the ONLY seam through which the chat UI talks
 * to a model. {@link ServerSseChatProvider} is the real DeepSeek-backed
 * implementation (via the server's AiService); the UI never changes based on
 * which provider is wired in.
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
  /** Cancels an in-flight {@link streamReply}, if any. No-op otherwise. */
  cancel?(): void
}

/** Structured payload of an `error` SSE event — mirrors ApiResponse.java's failure shape. */
export class AiStreamError extends Error {
  readonly code: number

  constructor(code: number, message: string) {
    super(message)
    this.name = 'AiStreamError'
    this.code = code
  }
}

interface SseEvent {
  event: string
  data: string
}

/** Minimal SSE frame parser for a `ReadableStream<Uint8Array>` — handles multi-line `data:` blocks. */
async function* parseSseStream(reader: ReadableStreamDefaultReader<Uint8Array>): AsyncGenerator<SseEvent> {
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  for (;;) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    let separator: number
    while ((separator = buffer.indexOf('\n\n')) !== -1) {
      const block = buffer.slice(0, separator)
      buffer = buffer.slice(separator + 2)
      yield parseEventBlock(block)
    }
  }
}

function parseEventBlock(block: string): SseEvent {
  let event = 'message'
  const dataLines: string[] = []
  for (const rawLine of block.split('\n')) {
    const line = rawLine.replace(/\r$/, '')
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).replace(/^ /, ''))
    }
  }
  return { event, data: dataLines.join('\n') }
}

/**
 * Real chat provider — POSTs the latest user message to the AI Tutor's
 * streaming endpoint and yields token deltas as they arrive over SSE.
 * Uses `fetch`/`ReadableStream` (not axios) since axios doesn't expose a
 * live stream for a POST body; the one 401 case reuses the same single-flight
 * refresh axios's interceptor uses (see `api/http.ts`).
 */
export class ServerSseChatProvider implements ChatProvider {
  readonly id = 'server-sse'

  private abortController: AbortController | null = null

  constructor(
    private readonly conversationId: string,
    private readonly context: Omit<SendMessagePayload, 'content'> = {},
  ) {}

  cancel() {
    this.abortController?.abort()
  }

  async *streamReply(history: ChatMessage[]): AsyncGenerator<string, void, undefined> {
    const lastUserMessage = [...history].reverse().find((message) => message.role === 'user')
    if (!lastUserMessage) return

    const url = `${import.meta.env.VITE_API_BASE_URL}/v1/ai/conversations/${this.conversationId}/messages`
    const payload: SendMessagePayload = {
      content: lastUserMessage.content,
      subjectName: this.context.subjectName,
      subjectDescription: this.context.subjectDescription,
      subjectId: this.context.subjectId,
    }
    const body = JSON.stringify(payload)

    this.abortController = new AbortController()
    let response = await this.post(url, body)
    if (response.status === 401) {
      await refreshTokenAfterUnauthorized()
      response = await this.post(url, body)
    }
    if (!response.ok || !response.body) {
      throw new AiStreamError(-1, `AI stream request failed (${response.status})`)
    }

    const reader = response.body.getReader()
    try {
      for await (const event of parseSseStream(reader)) {
        if (event.event === 'token') {
          yield event.data
        } else if (event.event === 'error') {
          throw parseStreamError(event.data)
        } else if (event.event === 'done') {
          return
        }
      }
    } finally {
      this.abortController = null
    }
  }

  private post(url: string, body: string): Promise<Response> {
    return fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${tokenStorage.getAccessToken() ?? ''}`,
      },
      body,
      signal: this.abortController?.signal,
    })
  }
}

function parseStreamError(data: string): AiStreamError {
  try {
    const parsed = JSON.parse(data) as { code?: number; message?: string }
    return new AiStreamError(parsed.code ?? -1, parsed.message ?? 'AI stream error')
  } catch {
    return new AiStreamError(-1, data || 'AI stream error')
  }
}
