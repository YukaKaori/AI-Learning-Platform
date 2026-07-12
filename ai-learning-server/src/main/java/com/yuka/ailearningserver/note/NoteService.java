package com.yuka.ailearningserver.note;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuka.ailearningserver.common.exception.BusinessException;
import com.yuka.ailearningserver.note.dto.CreateNoteRequest;
import com.yuka.ailearningserver.note.dto.NoteResponse;
import com.yuka.ailearningserver.note.dto.UpdateNoteRequest;
import com.yuka.ailearningserver.note.entity.Note;
import com.yuka.ailearningserver.note.mapper.NoteMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoteService {

    private final NoteMapper noteMapper;

    public NoteService(NoteMapper noteMapper) {
        this.noteMapper = noteMapper;
    }

    public List<NoteResponse> list(Long userId) {
        return noteMapper.selectList(new LambdaQueryWrapper<Note>()
                        .eq(Note::getUserId, userId)
                        .orderByDesc(Note::getPinned)
                        .orderByDesc(Note::getUpdatedAt))
                .stream()
                .map(NoteResponse::from)
                .toList();
    }

    public NoteResponse get(Long userId, Long id) {
        return NoteResponse.from(requireOwned(userId, id));
    }

    public NoteResponse create(Long userId, CreateNoteRequest request) {
        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(request.title());
        note.setContent(request.content());
        note.setPinned(Boolean.TRUE.equals(request.pinned()));
        noteMapper.insert(note);
        return NoteResponse.from(note);
    }

    public NoteResponse update(Long userId, Long id, UpdateNoteRequest request) {
        Note note = requireOwned(userId, id);
        if (request.title() != null && !request.title().isBlank()) {
            note.setTitle(request.title());
        }
        if (request.content() != null) {
            note.setContent(request.content());
        }
        if (request.pinned() != null) {
            note.setPinned(request.pinned());
        }
        noteMapper.updateById(note);
        return NoteResponse.from(note);
    }

    public void delete(Long userId, Long id) {
        Note note = requireOwned(userId, id);
        noteMapper.deleteById(note.getId());
    }

    private Note requireOwned(Long userId, Long id) {
        Note note = noteMapper.selectById(id);
        if (note == null) {
            throw new BusinessException(NoteErrorCode.NOTE_NOT_FOUND);
        }
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException(NoteErrorCode.NOTE_ACCESS_DENIED);
        }
        return note;
    }
}
