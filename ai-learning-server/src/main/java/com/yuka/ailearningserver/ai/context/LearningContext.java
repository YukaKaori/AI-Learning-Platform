package com.yuka.ailearningserver.ai.context;

import java.util.List;

/**
 * Assembled, ready-to-render learning context for {@code PromptBuilder}.
 * When a subject was resolved server-side, {@code subjectMaterialTitles} is
 * populated and the note count/titles are scoped to that subject; otherwise
 * materials are empty and notes are the user's most recent overall.
 */
public record LearningContext(
        String subjectName,
        String subjectDescription,
        List<String> subjectMaterialTitles,
        int totalNotes,
        List<String> recentNoteTitles,
        int totalFlashcardDecks,
        int totalFlashcards,
        int dueFlashcards,
        String statsSnapshot,
        String focusLabel,
        String focusContent) {

    public boolean isEmpty() {
        return (subjectName == null || subjectName.isBlank())
                && totalNotes == 0
                && totalFlashcards == 0
                && (statsSnapshot == null || statsSnapshot.isBlank())
                && (focusContent == null || focusContent.isBlank());
    }
}
