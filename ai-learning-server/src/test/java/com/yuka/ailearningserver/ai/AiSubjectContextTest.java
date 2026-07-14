package com.yuka.ailearningserver.ai;

import com.yuka.ailearningserver.ai.context.ContextHints;
import com.yuka.ailearningserver.ai.context.LearningContext;
import com.yuka.ailearningserver.ai.context.LearningContextService;
import com.yuka.ailearningserver.ai.dto.ConversationDetailResponse;
import com.yuka.ailearningserver.ai.dto.CreateConversationRequest;
import com.yuka.ailearningserver.ai.dto.SendMessageRequest;
import com.yuka.ailearningserver.ai.service.AiConversationService;
import com.yuka.ailearningserver.common.exception.BusinessException;
import com.yuka.ailearningserver.material.MaterialService;
import com.yuka.ailearningserver.material.dto.CreateMaterialRequest;
import com.yuka.ailearningserver.note.NoteService;
import com.yuka.ailearningserver.note.dto.CreateNoteRequest;
import com.yuka.ailearningserver.subject.SubjectErrorCode;
import com.yuka.ailearningserver.subject.SubjectService;
import com.yuka.ailearningserver.subject.dto.CreateSubjectRequest;
import com.yuka.ailearningserver.subject.dto.SubjectResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A10 — subject-aware AI context: conversations persist a validated
 * {@code subjectId} (with the {@code subjectName} snapshot taken from the
 * real subject), the context pipeline resolves subject
 * name/description/materials/notes server-side, and legacy hint-only callers
 * keep working when no subject id is involved.
 */
@SpringBootTest
@ActiveProfiles("test")
class AiSubjectContextTest {

    private static final Long USER = 1L;
    private static final Long OTHER_USER = 2L;

    @Autowired
    private AiConversationService conversationService;
    @Autowired
    private LearningContextService learningContextService;
    @Autowired
    private SubjectService subjectService;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private NoteService noteService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        for (String table : List.of("subjects", "learning_materials", "notes", "flashcard_decks",
                "flashcards", "ai_conversations", "ai_messages")) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
    }

    @Test
    void createLinksConversationToOwnedSubjectAndSnapshotsItsName() {
        SubjectResponse subject = subjectService.create(USER,
                new CreateSubjectRequest("Japanese", null, null, "JLPT N2 prep"));

        ConversationDetailResponse conversation = conversationService.create(USER,
                new CreateConversationRequest("kanji chat", null, subject.id()));
        assertThat(conversation.subjectId()).isEqualTo(subject.id());
        assertThat(conversation.subjectName()).isEqualTo("Japanese");
    }

    @Test
    void createWithoutSubjectIdKeepsTheLegacyNameHint() {
        ConversationDetailResponse conversation = conversationService.create(USER,
                new CreateConversationRequest(null, "Legacy subject", null));
        assertThat(conversation.subjectId()).isNull();
        assertThat(conversation.subjectName()).isEqualTo("Legacy subject");
    }

    @Test
    void createRejectsForeignAndMalformedSubjectIds() {
        SubjectResponse theirs = subjectService.create(OTHER_USER,
                new CreateSubjectRequest("Theirs", null, null, null));

        assertThatThrownBy(() -> conversationService.create(USER,
                new CreateConversationRequest(null, null, theirs.id())))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(SubjectErrorCode.SUBJECT_ACCESS_DENIED));
        assertThatThrownBy(() -> conversationService.create(USER,
                new CreateConversationRequest(null, null, "not-a-number")))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(SubjectErrorCode.SUBJECT_NOT_FOUND));
    }

    @Test
    void contextResolvesSubjectNameDescriptionMaterialsAndNotesServerSide() {
        SubjectResponse subject = subjectService.create(USER,
                new CreateSubjectRequest("Machine Learning", null, null, "Stanford course"));
        Long subjectId = Long.valueOf(subject.id());
        materialService.create(USER, subjectId,
                new CreateMaterialRequest("CS229 lecture notes", "link", null, "https://example.com/cs229"));
        materialService.create(USER, subjectId,
                new CreateMaterialRequest("Pattern Recognition book", "document", null, null));
        noteService.create(USER, new CreateNoteRequest("Gradient descent", "notes...", null, subject.id()));
        noteService.create(USER, new CreateNoteRequest("Unrelated note", "other...", null, null));

        LearningContext context = learningContextService.build(USER,
                new ContextHints(subjectId, "client-sent name", "client-sent description", null, null, null));

        assertThat(context.subjectName()).isEqualTo("Machine Learning"); // DB wins over the string hint
        assertThat(context.subjectDescription()).isEqualTo("Stanford course");
        assertThat(context.subjectMaterialTitles())
                .containsExactlyInAnyOrder("CS229 lecture notes", "Pattern Recognition book");
        assertThat(context.totalNotes()).isEqualTo(1); // scoped to the subject
        assertThat(context.recentNoteTitles()).containsExactly("Gradient descent");
    }

    @Test
    void contextFallsBackToStringHintsWhenNoSubjectResolves() {
        noteService.create(USER, new CreateNoteRequest("A note", null, null, null));

        // Stale id (subject deleted since it was persisted) — degrade, don't fail.
        LearningContext stale = learningContextService.build(USER,
                new ContextHints(424242L, "Hint name", "Hint description", null, null, null));
        assertThat(stale.subjectName()).isEqualTo("Hint name");
        assertThat(stale.subjectDescription()).isEqualTo("Hint description");
        assertThat(stale.subjectMaterialTitles()).isEmpty();
        assertThat(stale.totalNotes()).isEqualTo(1); // global scope

        // Legacy hint-only path (no id at all) behaves identically.
        LearningContext legacy = learningContextService.build(USER,
                new ContextHints(null, "Hint name", null, null, null, null));
        assertThat(legacy.subjectName()).isEqualTo("Hint name");
        assertThat(legacy.totalNotes()).isEqualTo(1);
    }

    @Test
    void streamReplyPersistsKeepsAndClearsTheSubjectLink() {
        SubjectResponse subject = subjectService.create(USER,
                new CreateSubjectRequest("Piano", null, null, null));
        String conversationId = conversationService.create(USER,
                new CreateConversationRequest(null, null, null)).id();
        Long id = Long.valueOf(conversationId);

        conversationService.streamReply(USER, id, new SendMessageRequest("hello", null, null, subject.id()));
        ConversationDetailResponse linked = conversationService.get(USER, id);
        assertThat(linked.subjectId()).isEqualTo(subject.id());
        assertThat(linked.subjectName()).isEqualTo("Piano");

        conversationService.streamReply(USER, id, new SendMessageRequest("again", null, null, null));
        assertThat(conversationService.get(USER, id).subjectId()).isEqualTo(subject.id()); // null = keep

        conversationService.streamReply(USER, id, new SendMessageRequest("unlink", null, null, ""));
        ConversationDetailResponse cleared = conversationService.get(USER, id);
        assertThat(cleared.subjectId()).isNull(); // "" = clear, and it must persist
        assertThat(cleared.subjectName()).isNull();
    }

    @Test
    void streamReplyRejectsAForeignSubjectBeforePersistingAnything() {
        SubjectResponse theirs = subjectService.create(OTHER_USER,
                new CreateSubjectRequest("Theirs", null, null, null));
        Long id = Long.valueOf(conversationService.create(USER,
                new CreateConversationRequest(null, null, null)).id());

        assertThatThrownBy(() -> conversationService.streamReply(USER, id,
                new SendMessageRequest("hi", null, null, theirs.id())))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(SubjectErrorCode.SUBJECT_ACCESS_DENIED));
        assertThat(conversationService.get(USER, id).messages()).isEmpty();
    }
}
