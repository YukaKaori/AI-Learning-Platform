<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { AppButton, AppIcon, AppSearch, AppTooltip } from '@/components'
import { getSubject } from '@/features/subjects/mock'
import { accentColor } from '@/features/subjects/types'
import { mockConversations } from './mock'
import { MockChatProvider, type ChatProvider } from './provider'
import type { ChatMessage, Conversation } from './types'

const { t, d } = useI18n()
const route = useRoute()
const router = useRouter()

// Local, mutable copy of the demo conversations — persistence is a later phase.
const conversations = ref<Conversation[]>(
  mockConversations.map((c) => ({ ...c, messages: [...c.messages] })),
)

const search = ref('')
const filtered = computed(() => {
  const query = search.value.trim().toLowerCase()
  const list = [...conversations.value].sort((a, b) => b.updatedAt - a.updatedAt)
  return query ? list.filter((c) => c.title.toLowerCase().includes(query)) : list
})

const activeId = ref<string | null>(
  typeof route.params.conversationId === 'string' ? route.params.conversationId : null,
)
const active = computed(() => conversations.value.find((c) => c.id === activeId.value) ?? null)

watch(
  () => route.params.conversationId,
  (id) => {
    activeId.value = typeof id === 'string' && id ? id : null
  },
)

function select(id: string | null) {
  router.replace({ name: 'ai-tutor', params: { conversationId: id ?? '' } })
}

// --- Provider seam: swap MockChatProvider for the server-SSE provider in the AI phase.
const provider: ChatProvider = new MockChatProvider((question) =>
  t('aiTutor.mockReply', { question }),
)

const draft = ref('')
const busy = ref(false)
const thread = ref<HTMLElement | null>(null)

async function scrollToEnd() {
  await nextTick()
  thread.value?.scrollTo({ top: thread.value.scrollHeight })
}

watch(active, scrollToEnd)

async function send() {
  const content = draft.value.trim()
  if (!content || busy.value) return
  draft.value = ''

  let conversation = active.value
  if (!conversation) {
    conversation = {
      id: `conv-${Date.now()}`,
      title: content.length > 24 ? `${content.slice(0, 24)}…` : content,
      updatedAt: Date.now(),
      messages: [],
    }
    conversations.value.unshift(conversation)
    select(conversation.id)
    activeId.value = conversation.id
  }

  conversation.messages.push({
    id: `msg-${Date.now()}`,
    role: 'user',
    content,
    createdAt: Date.now(),
  })
  conversation.updatedAt = Date.now()
  await scrollToEnd()

  const reply: ChatMessage = {
    id: `msg-${Date.now() + 1}`,
    role: 'assistant',
    content: '',
    createdAt: Date.now(),
    streaming: true,
  }
  conversation.messages.push(reply)
  busy.value = true
  try {
    for await (const chunk of provider.streamReply(conversation.messages)) {
      reply.content += chunk
      await scrollToEnd()
    }
  } finally {
    reply.streaming = false
    conversation.updatedAt = Date.now()
    busy.value = false
  }
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
    event.preventDefault()
    send()
  }
}

const suggestionKeys = ['explain', 'quiz', 'plan'] as const

function useSuggestion(key: (typeof suggestionKeys)[number]) {
  draft.value = t(`aiTutor.suggestions.${key}`)
}

function conversationAccent(conversation: Conversation): string {
  const subject = conversation.subjectId ? getSubject(conversation.subjectId) : undefined
  return subject ? accentColor(subject.accent) : 'var(--color-muted)'
}
</script>

<template>
  <div class="tutor">
    <aside class="conv-panel">
      <div class="conv-head">
        <h1 class="conv-title">{{ t('aiTutor.title') }}</h1>
        <AppButton size="sm" variant="soft" icon-left="plus" @click="select(null)">
          {{ t('aiTutor.newChat') }}
        </AppButton>
      </div>
      <div class="conv-search">
        <AppSearch v-model="search" :placeholder="t('aiTutor.searchPlaceholder')" />
      </div>
      <p v-if="filtered.length === 0" class="conv-empty">{{ t('aiTutor.listEmpty') }}</p>
      <ul v-else class="conv-list">
        <li v-for="conv in filtered" :key="conv.id">
          <button
            type="button"
            class="conv-item"
            :class="{ active: conv.id === activeId }"
            @click="select(conv.id)"
          >
            <span class="conv-dot" :style="{ backgroundColor: conversationAccent(conv) }"></span>
            <span class="conv-item-title">{{ conv.title }}</span>
            <span class="conv-item-date">{{ d(conv.updatedAt, 'short') }}</span>
          </button>
        </li>
      </ul>
    </aside>

    <section class="chat">
      <div ref="thread" class="thread">
        <div v-if="!active" class="thread-empty">
          <span class="empty-icon"><AppIcon name="sparkles" size="lg" /></span>
          <h2 class="empty-title">{{ t('aiTutor.emptyThread.title') }}</h2>
          <p class="empty-desc">{{ t('aiTutor.emptyThread.desc') }}</p>
          <div class="suggestions">
            <button
              v-for="key in suggestionKeys"
              :key="key"
              type="button"
              class="suggestion"
              @click="useSuggestion(key)"
            >
              {{ t(`aiTutor.suggestions.${key}`) }}
            </button>
          </div>
        </div>

        <template v-else>
          <div
            v-for="message in active.messages"
            :key="message.id"
            class="message"
            :class="message.role"
          >
            <span class="message-avatar" aria-hidden="true">
              <AppIcon :name="message.role === 'user' ? 'user' : 'bot'" size="sm" />
            </span>
            <div class="message-body">
              <span class="message-author">
                {{ message.role === 'user' ? t('aiTutor.you') : t('aiTutor.assistant') }}
              </span>
              <div class="message-content">
                {{ message.content }}<span v-if="message.streaming" class="caret"></span>
              </div>
            </div>
          </div>
          <p v-if="busy" class="streaming-note" role="status">{{ t('aiTutor.streaming') }}</p>
        </template>
      </div>

      <div class="composer">
        <div class="composer-box">
          <textarea
            v-model="draft"
            class="composer-input"
            :placeholder="t('aiTutor.input.placeholder')"
            rows="1"
            @keydown="onKeydown"
          ></textarea>
          <div class="composer-actions">
            <AppTooltip :content="t('aiTutor.input.attach')">
              <AppButton
                variant="ghost"
                tone="secondary"
                size="sm"
                icon-left="paperclip"
                disabled
                :aria-label="t('aiTutor.input.attach')"
              />
            </AppTooltip>
            <AppButton
              size="sm"
              icon-left="send"
              :disabled="!draft.trim()"
              :loading="busy"
              :aria-label="t('aiTutor.input.send')"
              @click="send"
            />
          </div>
        </div>
        <p class="composer-hint">
          <span>{{ t('aiTutor.input.hint') }}</span>
          <span class="phase-note">{{ t('aiTutor.phaseNote') }}</span>
        </p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.tutor {
  display: flex;
  height: 100%;
  min-height: 0;
}

/* Conversation list */
.conv-panel {
  display: flex;
  flex-direction: column;
  width: 290px;
  flex-shrink: 0;
  border-right: var(--border-width-sm) solid var(--color-border);
  background-color: var(--color-surface);
}

.conv-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-5) var(--space-4) var(--space-3);
}

.conv-title {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: 600;
  letter-spacing: var(--tracking-tight);
}

.conv-search {
  padding: 0 var(--space-4) var(--space-3);
}

.conv-empty {
  margin: 0;
  padding: var(--space-8) var(--space-4);
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  text-align: center;
}

.conv-list {
  flex: 1;
  margin: 0;
  padding: 0 var(--space-2) var(--space-4);
  list-style: none;
  overflow-y: auto;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  width: 100%;
  padding: var(--space-2) var(--space-2);
  border: none;
  border-radius: var(--radius-md);
  background: transparent;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-out);
}

.conv-item:hover {
  background-color: var(--color-surface-hover);
}

.conv-item.active {
  background-color: var(--color-primary-soft);
}

.conv-dot {
  width: 7px;
  height: 7px;
  flex-shrink: 0;
  border-radius: var(--radius-full);
}

.conv-item-title {
  flex: 1;
  min-width: 0;
  font-size: var(--text-sm);
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-item.active .conv-item-title {
  color: var(--color-primary);
  font-weight: 500;
}

.conv-item-date {
  flex-shrink: 0;
  font-size: var(--text-xs);
  font-variant-numeric: tabular-nums;
  color: var(--color-text-tertiary);
}

/* Thread */
.chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.thread {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-8);
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.thread-empty {
  margin: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  max-width: 420px;
}

.empty-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  margin-bottom: var(--space-4);
  border-radius: var(--radius-xl);
  color: var(--color-primary);
  background-color: var(--color-primary-soft);
}

.empty-title {
  margin: 0 0 var(--space-2);
  font-size: var(--text-xl);
  font-weight: 600;
  letter-spacing: var(--tracking-tight);
}

.empty-desc {
  margin: 0 0 var(--space-6);
  font-size: var(--text-sm);
  line-height: var(--leading-normal);
  color: var(--color-text-secondary);
}

.suggestions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--space-2);
}

.suggestion {
  padding: var(--space-2) var(--space-3);
  border: var(--border-width-sm) solid var(--color-border);
  border-radius: var(--radius-full);
  background-color: var(--color-surface);
  font-family: inherit;
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition:
    border-color var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out);
}

.suggestion:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  box-shadow: var(--shadow-sm);
}

.message {
  display: flex;
  gap: var(--space-3);
  max-width: 760px;
  width: 100%;
  margin: 0 auto;
}

.message-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  border-radius: var(--radius-full);
  color: var(--color-text-secondary);
  background-color: var(--color-muted-soft);
}

.message.assistant .message-avatar {
  color: var(--color-primary);
  background-color: var(--color-primary-soft);
}

.message-body {
  min-width: 0;
  flex: 1;
}

.message-author {
  display: block;
  margin-bottom: var(--space-1);
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--color-text-tertiary);
}

.message-content {
  font-size: var(--text-base);
  line-height: var(--leading-normal);
  color: var(--color-text);
  white-space: pre-wrap;
  overflow-wrap: break-word;
}

.caret {
  display: inline-block;
  width: 2px;
  height: 1em;
  margin-left: 2px;
  vertical-align: text-bottom;
  background-color: var(--color-primary);
  animation: blink 1s steps(2) infinite;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

.streaming-note {
  max-width: 760px;
  width: 100%;
  margin: calc(var(--space-4) * -1) auto 0;
  padding-left: 42px;
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

/* Composer */
.composer {
  padding: var(--space-4) var(--space-8) var(--space-6);
}

.composer-box {
  display: flex;
  align-items: flex-end;
  gap: var(--space-2);
  max-width: 760px;
  margin: 0 auto;
  padding: var(--space-3);
  border: var(--border-width-sm) solid var(--color-border);
  border-radius: var(--radius-xl);
  background-color: var(--color-surface);
  box-shadow: var(--shadow-md);
  transition:
    border-color var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out);
}

.composer-box:focus-within {
  border-color: var(--color-primary);
  box-shadow:
    var(--shadow-md),
    0 0 0 3px var(--color-primary-soft);
}

.composer-input {
  flex: 1;
  min-height: 24px;
  max-height: 160px;
  padding: var(--space-1) var(--space-2);
  border: none;
  background: transparent;
  resize: none;
  font-family: inherit;
  font-size: var(--text-base);
  line-height: var(--leading-normal);
  color: var(--color-text);
}

.composer-input:focus {
  outline: none;
}

.composer-input::placeholder {
  color: var(--color-text-tertiary);
}

.composer-actions {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.composer-hint {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
  max-width: 760px;
  margin: var(--space-2) auto 0;
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

.phase-note {
  text-align: right;
}

@media (max-width: 900px) {
  .conv-panel {
    display: none;
  }

  .thread {
    padding: var(--space-5);
  }

  .composer {
    padding: var(--space-3) var(--space-5) var(--space-5);
  }
}
</style>
