package com.yuka.ailearningserver.ai.context;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuka.ailearningserver.flashcard.entity.Flashcard;
import com.yuka.ailearningserver.flashcard.entity.FlashcardDeck;
import com.yuka.ailearningserver.flashcard.mapper.FlashcardDeckMapper;
import com.yuka.ailearningserver.flashcard.mapper.FlashcardMapper;
import com.yuka.ailearningserver.note.entity.Note;
import com.yuka.ailearningserver.note.mapper.NoteMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Assembles {@link LearningContext} from the real data this phase adds
 * (notes, flashcards — both scoped to {@code userId}) plus whatever the
 * caller supplied via {@link ContextHints} for the parts that aren't real
 * yet (subjects, analytics stats). See the class-level note on {@code
 * ContextHints} for why subjects aren't looked up here.
 */
@Component
public class LearningContextService {

    private static final int RECENT_NOTES_LIMIT = 5;

    private final NoteMapper noteMapper;
    private final FlashcardDeckMapper flashcardDeckMapper;
    private final FlashcardMapper flashcardMapper;

    public LearningContextService(NoteMapper noteMapper, FlashcardDeckMapper flashcardDeckMapper,
                                  FlashcardMapper flashcardMapper) {
        this.noteMapper = noteMapper;
        this.flashcardDeckMapper = flashcardDeckMapper;
        this.flashcardMapper = flashcardMapper;
    }

    public LearningContext build(Long userId, ContextHints hints) {
        List<Note> recentNotes = noteMapper.selectList(new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId)
                .orderByDesc(Note::getUpdatedAt)
                .last("limit " + RECENT_NOTES_LIMIT));
        long totalNotes = noteMapper.selectCount(new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId));
        long totalDecks = flashcardDeckMapper.selectCount(new LambdaQueryWrapper<FlashcardDeck>()
                .eq(FlashcardDeck::getUserId, userId));
        long totalCards = flashcardMapper.selectCount(new LambdaQueryWrapper<Flashcard>()
                .eq(Flashcard::getUserId, userId));
        long dueCards = flashcardMapper.selectCount(new LambdaQueryWrapper<Flashcard>()
                .eq(Flashcard::getUserId, userId)
                .isNotNull(Flashcard::getDueAt)
                .le(Flashcard::getDueAt, LocalDateTime.now()));

        return new LearningContext(
                hints.subjectName(),
                hints.subjectDescription(),
                (int) totalNotes,
                recentNotes.stream().map(Note::getTitle).toList(),
                (int) totalDecks,
                (int) totalCards,
                (int) dueCards,
                hints.statsSnapshot(),
                hints.focusLabel(),
                hints.focusContent());
    }
}
