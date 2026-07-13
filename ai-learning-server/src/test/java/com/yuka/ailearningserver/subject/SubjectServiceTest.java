package com.yuka.ailearningserver.subject;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuka.ailearningserver.ai.entity.AiConversation;
import com.yuka.ailearningserver.ai.mapper.AiConversationMapper;
import com.yuka.ailearningserver.calendar.entity.StudySession;
import com.yuka.ailearningserver.calendar.mapper.StudySessionMapper;
import com.yuka.ailearningserver.common.exception.BusinessException;
import com.yuka.ailearningserver.material.MaterialService;
import com.yuka.ailearningserver.material.dto.CreateMaterialRequest;
import com.yuka.ailearningserver.material.dto.MaterialResponse;
import com.yuka.ailearningserver.material.entity.LearningMaterial;
import com.yuka.ailearningserver.material.mapper.LearningMaterialMapper;
import com.yuka.ailearningserver.note.entity.Note;
import com.yuka.ailearningserver.note.mapper.NoteMapper;
import com.yuka.ailearningserver.subject.dto.CreateSubjectRequest;
import com.yuka.ailearningserver.subject.dto.SubjectResponse;
import com.yuka.ailearningserver.subject.dto.UpdateSubjectRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Subject/Material behavior: per-user isolation, derived aggregates, and the
 * delete policy (materials soft-cascade, linked content is unlinked).
 */
@SpringBootTest
@ActiveProfiles("test")
class SubjectServiceTest {

    private static final Long USER = 1L;
    private static final Long OTHER_USER = 2L;

    @Autowired
    private SubjectService subjectService;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private NoteMapper noteMapper;
    @Autowired
    private StudySessionMapper sessionMapper;
    @Autowired
    private AiConversationMapper conversationMapper;
    @Autowired
    private LearningMaterialMapper materialMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        for (String table : List.of("subjects", "learning_materials", "notes", "flashcard_decks",
                "learning_tasks", "study_sessions", "ai_conversations")) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
    }

    @Test
    void crudRoundTrip() {
        SubjectResponse created = subjectService.create(USER,
                new CreateSubjectRequest("Machine Learning", "indigo", "brain", "Foundations"));
        assertThat(created.status()).isEqualTo("active");
        assertThat(created.progress()).isZero();

        SubjectResponse updated = subjectService.update(USER, Long.valueOf(created.id()),
                new UpdateSubjectRequest(null, null, null, null, "completed", 80));
        assertThat(updated.status()).isEqualTo("completed");
        assertThat(updated.progress()).isEqualTo(80);

        assertThat(subjectService.list(USER)).hasSize(1);
        subjectService.delete(USER, Long.valueOf(created.id()));
        assertThat(subjectService.list(USER)).isEmpty();
    }

    @Test
    void crossUserAccessIsDenied() {
        SubjectResponse subject = subjectService.create(USER,
                new CreateSubjectRequest("Private", null, null, null));
        assertThatThrownBy(() -> subjectService.get(OTHER_USER, Long.valueOf(subject.id())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not belong");
        assertThatThrownBy(() -> materialService.listBySubject(OTHER_USER, Long.valueOf(subject.id())))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void derivedCountsAndStudyMinutes() {
        SubjectResponse subject = subjectService.create(USER,
                new CreateSubjectRequest("Japanese", "rose", "languages", null));
        Long subjectId = Long.valueOf(subject.id());

        materialService.create(USER, subjectId,
                new CreateMaterialRequest("NHK Easy", "link", null, "https://example.com"));
        insertNote(USER, subjectId);

        StudySession session = new StudySession();
        session.setUserId(USER);
        session.setSubjectId(subjectId);
        session.setStartsAt(LocalDateTime.now().minusMinutes(45));
        session.setEndsAt(LocalDateTime.now());
        sessionMapper.insert(session);

        SubjectResponse loaded = subjectService.get(USER, subjectId);
        assertThat(loaded.materialCount()).isEqualTo(1);
        assertThat(loaded.noteCount()).isEqualTo(1);
        assertThat(loaded.deckCount()).isZero();
        assertThat(loaded.studyMinutes()).isEqualTo(45);
        assertThat(loaded.lastStudiedAt()).isNotNull();
    }

    @Test
    void deleteSoftCascadesMaterialsAndUnlinksEverythingElse() {
        SubjectResponse subject = subjectService.create(USER,
                new CreateSubjectRequest("Algorithms", "teal", "binary", null));
        Long subjectId = Long.valueOf(subject.id());

        MaterialResponse material = materialService.create(USER, subjectId,
                new CreateMaterialRequest("Visualgo", "link", null, "https://visualgo.net"));
        Note note = insertNote(USER, subjectId);

        AiConversation conversation = new AiConversation();
        conversation.setUserId(USER);
        conversation.setTitle("Quicksort chat");
        conversation.setSubjectId(subjectId);
        conversation.setSubjectName("Algorithms");
        conversation.setArchived(false);
        conversationMapper.insert(conversation);

        subjectService.delete(USER, subjectId);

        // Material is soft-deleted (gone from reads, row retained with deleted=1).
        assertThat(materialMapper.selectById(material.id() != null ? Long.valueOf(material.id()) : null)).isNull();
        Integer materialDeleted = jdbcTemplate.queryForObject(
                "SELECT deleted FROM learning_materials WHERE id = ?", Integer.class, Long.valueOf(material.id()));
        assertThat(materialDeleted).isEqualTo(1);

        // Note and conversation survive but lose the subject link.
        Note keptNote = noteMapper.selectById(note.getId());
        assertThat(keptNote).isNotNull();
        assertThat(keptNote.getSubjectId()).isNull();
        AiConversation keptConversation = conversationMapper.selectById(conversation.getId());
        assertThat(keptConversation).isNotNull();
        assertThat(keptConversation.getSubjectId()).isNull();
        assertThat(keptConversation.getSubjectName()).isEqualTo("Algorithms");
    }

    private Note insertNote(Long userId, Long subjectId) {
        Note note = new Note();
        note.setUserId(userId);
        note.setSubjectId(subjectId);
        note.setTitle("linked note");
        note.setContent("body");
        note.setPinned(false);
        noteMapper.insert(note);
        return note;
    }
}
