package com.yuka.ailearningserver.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuka.ailearningserver.ai.context.ContextHints;
import com.yuka.ailearningserver.ai.context.LearningContext;
import com.yuka.ailearningserver.ai.context.LearningContextService;
import com.yuka.ailearningserver.ai.dto.ConversationDetailResponse;
import com.yuka.ailearningserver.ai.dto.ConversationSummaryResponse;
import com.yuka.ailearningserver.ai.dto.CreateConversationRequest;
import com.yuka.ailearningserver.ai.dto.SendMessageRequest;
import com.yuka.ailearningserver.ai.dto.UpdateConversationRequest;
import com.yuka.ailearningserver.ai.entity.AiConversation;
import com.yuka.ailearningserver.ai.entity.AiMessage;
import com.yuka.ailearningserver.ai.entity.AiMessageRole;
import com.yuka.ailearningserver.ai.exception.AiErrorCode;
import com.yuka.ailearningserver.ai.mapper.AiConversationMapper;
import com.yuka.ailearningserver.ai.mapper.AiMessageMapper;
import com.yuka.ailearningserver.ai.prompt.PromptBuilder;
import com.yuka.ailearningserver.ai.prompt.PromptTemplate;
import com.yuka.ailearningserver.ai.provider.ChatRequest;
import com.yuka.ailearningserver.ai.provider.ChatRole;
import com.yuka.ailearningserver.ai.provider.ChatTurn;
import com.yuka.ailearningserver.ai.stream.RelayCallback;
import com.yuka.ailearningserver.ai.stream.SseRelay;
import com.yuka.ailearningserver.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Conversation CRUD plus the "send a message → stream the reply → persist
 * both sides" use-case. {@link SseRelay} owns the SSE mechanics; this class
 * only deals in plain text and persistence.
 */
@Service
public class AiConversationService {

    private static final String DEFAULT_TITLE = "新对话";
    private static final int TITLE_PREVIEW_LENGTH = 24;

    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final LearningContextService learningContextService;
    private final PromptBuilder promptBuilder;
    private final SseRelay sseRelay;

    public AiConversationService(AiConversationMapper conversationMapper, AiMessageMapper messageMapper,
                                 LearningContextService learningContextService, PromptBuilder promptBuilder,
                                 SseRelay sseRelay) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.learningContextService = learningContextService;
        this.promptBuilder = promptBuilder;
        this.sseRelay = sseRelay;
    }

    public List<ConversationSummaryResponse> list(Long userId) {
        return conversationMapper.selectList(new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getUserId, userId)
                        .orderByDesc(AiConversation::getUpdatedAt))
                .stream()
                .map(ConversationSummaryResponse::from)
                .toList();
    }

    public ConversationDetailResponse create(Long userId, CreateConversationRequest request) {
        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setTitle(request.title() != null && !request.title().isBlank()
                ? request.title() : DEFAULT_TITLE);
        conversation.setSubjectName(request.subjectName());
        conversation.setArchived(false);
        conversationMapper.insert(conversation);
        return ConversationDetailResponse.from(conversation, List.of());
    }

    public ConversationDetailResponse get(Long userId, Long conversationId) {
        AiConversation conversation = requireOwned(userId, conversationId);
        return ConversationDetailResponse.from(conversation, messagesOf(conversationId));
    }

    public ConversationSummaryResponse update(Long userId, Long conversationId, UpdateConversationRequest request) {
        AiConversation conversation = requireOwned(userId, conversationId);
        if (request.title() != null && !request.title().isBlank()) {
            conversation.setTitle(request.title());
        }
        if (request.archived() != null) {
            conversation.setArchived(request.archived());
        }
        conversationMapper.updateById(conversation);
        return ConversationSummaryResponse.from(conversation);
    }

    public void delete(Long userId, Long conversationId) {
        AiConversation conversation = requireOwned(userId, conversationId);
        conversationMapper.deleteById(conversation.getId());
    }

    public SseEmitter streamReply(Long userId, Long conversationId, SendMessageRequest request) {
        AiConversation conversation = requireOwned(userId, conversationId);

        persistMessage(conversationId, userId, AiMessageRole.USER, request.content(), false);
        List<AiMessage> history = messagesOf(conversationId);
        if (history.size() == 1) {
            conversation.setTitle(previewTitle(request.content()));
        }
        conversationMapper.updateById(conversation);

        ContextHints hints = new ContextHints(request.subjectName(), request.subjectDescription(), null, null, null);
        LearningContext context = learningContextService.build(userId, hints);
        List<ChatTurn> messages = promptBuilder.build(PromptTemplate.TUTOR, context,
                history.stream().map(AiConversationService::toChatTurn).toList(), null);

        return sseRelay.stream(new ChatRequest(messages), new RelayCallback() {
            @Override
            public void onFinished(String fullText, boolean cancelled) {
                if (!fullText.isBlank()) {
                    persistMessage(conversationId, userId, AiMessageRole.ASSISTANT, fullText, cancelled);
                    touchUpdatedAt(conversation);
                }
            }

            @Override
            public void onFailed(String partialText, Throwable error) {
                if (!partialText.isBlank()) {
                    persistMessage(conversationId, userId, AiMessageRole.ASSISTANT, partialText, true);
                    touchUpdatedAt(conversation);
                }
            }
        });
    }

    private List<AiMessage> messagesOf(Long conversationId) {
        return messageMapper.selectList(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId)
                .orderByAsc(AiMessage::getCreatedAt));
    }

    private AiMessage persistMessage(Long conversationId, Long userId, AiMessageRole role, String content,
                                      boolean truncated) {
        AiMessage message = new AiMessage();
        message.setConversationId(conversationId);
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content);
        message.setTruncated(truncated);
        messageMapper.insert(message);
        return message;
    }

    private void touchUpdatedAt(AiConversation conversation) {
        conversationMapper.updateById(conversation);
    }

    private AiConversation requireOwned(Long userId, Long conversationId) {
        AiConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException(AiErrorCode.CONVERSATION_NOT_FOUND);
        }
        if (!conversation.getUserId().equals(userId)) {
            throw new BusinessException(AiErrorCode.CONVERSATION_ACCESS_DENIED);
        }
        return conversation;
    }

    private static ChatTurn toChatTurn(AiMessage message) {
        ChatRole role = message.getRole() == AiMessageRole.ASSISTANT ? ChatRole.ASSISTANT : ChatRole.USER;
        return new ChatTurn(role, message.getContent());
    }

    private static String previewTitle(String content) {
        String trimmed = content.trim();
        return trimmed.length() > TITLE_PREVIEW_LENGTH
                ? trimmed.substring(0, TITLE_PREVIEW_LENGTH) + "…" : trimmed;
    }
}
