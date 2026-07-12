package com.yuka.ailearningserver.ai.context;

import java.util.List;

/** Assembled, ready-to-render learning context for {@code PromptBuilder}. */
public record LearningContext(
        String subjectName,
        String subjectDescription,
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
