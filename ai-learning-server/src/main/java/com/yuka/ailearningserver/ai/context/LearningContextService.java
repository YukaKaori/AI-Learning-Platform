package com.yuka.ailearningserver.ai.context;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuka.ailearningserver.flashcard.entity.Flashcard;
import com.yuka.ailearningserver.flashcard.entity.FlashcardDeck;
import com.yuka.ailearningserver.flashcard.mapper.FlashcardDeckMapper;
import com.yuka.ailearningserver.flashcard.mapper.FlashcardMapper;
import com.yuka.ailearningserver.material.entity.LearningMaterial;
import com.yuka.ailearningserver.material.mapper.LearningMaterialMapper;
import com.yuka.ailearningserver.note.entity.Note;
import com.yuka.ailearningserver.note.mapper.NoteMapper;
import com.yuka.ailearningserver.subject.entity.Subject;
import com.yuka.ailearningserver.subject.mapper.SubjectMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Assembles {@link LearningContext} from real per-user data. When
 * {@link ContextHints#subjectId()} carries a resolved subject id, the
 * subject's name/description/material titles are loaded here and the note
 * count/titles are scoped to that subject; a subject that has since been
 * deleted degrades gracefully to the string hints. Without a subject id, the
 * caller-supplied name/description hints are used as-is (legacy hint-only
 * chat and the generation endpoints), alongside the user's global notes.
 */
@Component
public class LearningContextService {

    private static final int RECENT_NOTES_LIMIT = 5;
    private static final int MATERIAL_TITLES_LIMIT = 10;

    private final NoteMapper noteMapper;
    private final FlashcardDeckMapper flashcardDeckMapper;
    private final FlashcardMapper flashcardMapper;
    private final SubjectMapper subjectMapper;
    private final LearningMaterialMapper materialMapper;

    public LearningContextService(NoteMapper noteMapper, FlashcardDeckMapper flashcardDeckMapper,
                                  FlashcardMapper flashcardMapper, SubjectMapper subjectMapper,
                                  LearningMaterialMapper materialMapper) {
        this.noteMapper = noteMapper;
        this.flashcardDeckMapper = flashcardDeckMapper;
        this.flashcardMapper = flashcardMapper;
        this.subjectMapper = subjectMapper;
        this.materialMapper = materialMapper;
    }

    public LearningContext build(Long userId, ContextHints hints) {
        Subject subject = loadSubject(userId, hints.subjectId());
        List<String> materialTitles = subject == null ? List.of()
                : materialMapper.selectList(new LambdaQueryWrapper<LearningMaterial>()
                                .select(LearningMaterial::getTitle)
                                .eq(LearningMaterial::getUserId, userId)
                                .eq(LearningMaterial::getSubjectId, subject.getId())
                                .orderByDesc(LearningMaterial::getUpdatedAt)
                                .last("limit " + MATERIAL_TITLES_LIMIT))
                        .stream().map(LearningMaterial::getTitle).toList();

        LambdaQueryWrapper<Note> recentNotesQuery = noteScope(userId, subject)
                .orderByDesc(Note::getUpdatedAt)
                .last("limit " + RECENT_NOTES_LIMIT);
        List<Note> recentNotes = noteMapper.selectList(recentNotesQuery);
        long totalNotes = noteMapper.selectCount(noteScope(userId, subject));

        long totalDecks = flashcardDeckMapper.selectCount(new LambdaQueryWrapper<FlashcardDeck>()
                .eq(FlashcardDeck::getUserId, userId));
        long totalCards = flashcardMapper.selectCount(new LambdaQueryWrapper<Flashcard>()
                .eq(Flashcard::getUserId, userId));
        long dueCards = flashcardMapper.selectCount(new LambdaQueryWrapper<Flashcard>()
                .eq(Flashcard::getUserId, userId)
                .isNotNull(Flashcard::getDueAt)
                .le(Flashcard::getDueAt, LocalDateTime.now()));

        return new LearningContext(
                subject != null ? subject.getName() : hints.subjectName(),
                subject != null ? subject.getDescription() : hints.subjectDescription(),
                materialTitles,
                (int) totalNotes,
                recentNotes.stream().map(Note::getTitle).toList(),
                (int) totalDecks,
                (int) totalCards,
                (int) dueCards,
                hints.statsSnapshot(),
                hints.focusLabel(),
                hints.focusContent());
    }

    /**
     * The id arrives pre-validated (see {@link ContextHints}), but the subject
     * may have been deleted since it was persisted on a conversation — treat
     * missing or (defensively) foreign rows as "no subject" instead of failing
     * the chat.
     */
    private Subject loadSubject(Long userId, Long subjectId) {
        if (subjectId == null) {
            return null;
        }
        Subject subject = subjectMapper.selectById(subjectId);
        return subject != null && userId.equals(subject.getUserId()) ? subject : null;
    }

    private static LambdaQueryWrapper<Note> noteScope(Long userId, Subject subject) {
        LambdaQueryWrapper<Note> query = new LambdaQueryWrapper<Note>().eq(Note::getUserId, userId);
        if (subject != null) {
            query.eq(Note::getSubjectId, subject.getId());
        }
        return query;
    }
}
