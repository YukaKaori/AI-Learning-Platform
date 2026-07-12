/**
 * Notes — markdown-first knowledge capture, optionally attached to a subject.
 *
 * <p>Phase 6 adds real CRUD ({@link com.yuka.ailearningserver.note.NoteService},
 * {@link com.yuka.ailearningserver.note.NoteController}). AI actions over note
 * text (rewrite, continue, simplify, expand, translate, summarize, generate
 * flashcards) live in the {@code ai} package and operate on text the frontend
 * already has in hand — they don't call back into this module.
 * Reserved error-code range: 130000–139999.
 */
package com.yuka.ailearningserver.note;
