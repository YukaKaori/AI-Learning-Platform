package com.yuka.ailearningserver.ai.dto;

import com.yuka.ailearningserver.ai.entity.AiConversation;
import com.yuka.ailearningserver.ai.entity.AiMessage;

import java.time.ZoneId;
import java.util.List;

public record ConversationDetailResponse(
        String id, String title, String subjectName, boolean archived, long updatedAt,
        List<MessageResponse> messages) {

    public static ConversationDetailResponse from(AiConversation conversation, List<AiMessage> messages) {
        return new ConversationDetailResponse(
                String.valueOf(conversation.getId()),
                conversation.getTitle(),
                conversation.getSubjectName(),
                Boolean.TRUE.equals(conversation.getArchived()),
                conversation.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                messages.stream().map(MessageResponse::from).toList());
    }
}
