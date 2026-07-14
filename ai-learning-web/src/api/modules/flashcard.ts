import { api } from '@/api/http'

/** Mirror of DeckResponse.java. */
export interface FlashcardDeckDto {
  id: string
  subjectId: string | null
  name: string
  description: string | null
  cardCount: number
  dueCount: number
}

/** Mirror of CardResponse.java. */
export interface FlashcardDto {
  id: string
  deckId: string
  front: string
  back: string
  dueAt: number | null
}

/** `subjectId` must reference a subject owned by the caller. */
export interface CreateDeckPayload {
  name: string
  description?: string
  subjectId?: string
}

/**
 * Partial update — omitted fields keep their value; `subjectId: ''` unlinks
 * the deck from its subject.
 */
export interface UpdateDeckPayload {
  name?: string
  description?: string
  subjectId?: string
}

export function listDecks() {
  return api.get<FlashcardDeckDto[]>('/v1/flashcards/decks')
}

export function createDeck(payload: CreateDeckPayload) {
  return api.post<FlashcardDeckDto>('/v1/flashcards/decks', payload)
}

export function updateDeck(deckId: string, payload: UpdateDeckPayload) {
  return api.put<FlashcardDeckDto>(`/v1/flashcards/decks/${deckId}`, payload)
}

export function deleteDeck(deckId: string) {
  return api.delete<void>(`/v1/flashcards/decks/${deckId}`)
}

export function listCards(deckId: string) {
  return api.get<FlashcardDto[]>(`/v1/flashcards/decks/${deckId}/cards`)
}

export function createCard(deckId: string, payload: { front: string; back: string }) {
  return api.post<FlashcardDto>(`/v1/flashcards/decks/${deckId}/cards`, payload)
}

export function updateCard(cardId: string, payload: { front?: string; back?: string }) {
  return api.put<FlashcardDto>(`/v1/flashcards/cards/${cardId}`, payload)
}

export function deleteCard(cardId: string) {
  return api.delete<void>(`/v1/flashcards/cards/${cardId}`)
}
