package com.yuka.ailearningserver.ai.dto;

import com.yuka.ailearningserver.ai.entity.AiConversation;

import java.time.ZoneId;

public record ConversationSummaryResponse(
        String id, String title, String subjectName, boolean archived, long updatedAt) {

    public static ConversationSummaryResponse from(AiConversation conversation) {
        return new ConversationSummaryResponse(
                String.valueOf(conversation.getId()),
                conversation.getTitle(),
                conversation.getSubjectName(),
                Boolean.TRUE.equals(conversation.getArchived()),
                conversation.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
}
