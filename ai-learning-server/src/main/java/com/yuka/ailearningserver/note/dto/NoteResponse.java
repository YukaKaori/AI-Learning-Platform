package com.yuka.ailearningserver.note.dto;

import com.yuka.ailearningserver.note.entity.Note;

import java.time.ZoneId;

public record NoteResponse(String id, String subjectId, String title, String content, boolean pinned,
                            long updatedAt) {

    public static NoteResponse from(Note note) {
        return new NoteResponse(
                String.valueOf(note.getId()),
                note.getSubjectId() != null ? String.valueOf(note.getSubjectId()) : null,
                note.getTitle(),
                note.getContent(),
                Boolean.TRUE.equals(note.getPinned()),
                note.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
}
