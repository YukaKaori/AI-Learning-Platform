package com.yuka.ailearningserver.ai.controller;

import com.yuka.ailearningserver.ai.dto.ConversationDetailResponse;
import com.yuka.ailearningserver.ai.dto.ConversationSummaryResponse;
import com.yuka.ailearningserver.ai.dto.CreateConversationRequest;
import com.yuka.ailearningserver.ai.dto.SendMessageRequest;
import com.yuka.ailearningserver.ai.dto.UpdateConversationRequest;
import com.yuka.ailearningserver.ai.service.AiConversationService;
import com.yuka.ailearningserver.auth.security.AuthenticatedUser;
import com.yuka.ailearningserver.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/conversations")
public class AiChatController {

    private final AiConversationService conversationService;

    public AiChatController(AiConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public ApiResponse<List<ConversationSummaryResponse>> list(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.success(conversationService.list(principal.id()));
    }

    @PostMapping
    public ApiResponse<ConversationDetailResponse> create(@AuthenticationPrincipal AuthenticatedUser principal,
                                                           @Valid @RequestBody CreateConversationRequest request) {
        return ApiResponse.success(conversationService.create(principal.id(), request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ConversationDetailResponse> get(@AuthenticationPrincipal AuthenticatedUser principal,
                                                        @PathVariable Long id) {
        return ApiResponse.success(conversationService.get(principal.id(), id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ConversationSummaryResponse> update(@AuthenticationPrincipal AuthenticatedUser principal,
                                                            @PathVariable Long id,
                                                            @Valid @RequestBody UpdateConversationRequest request) {
        return ApiResponse.success(conversationService.update(principal.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long id) {
        conversationService.delete(principal.id(), id);
        return ApiResponse.success();
    }

    /**
     * Streams the assistant reply as {@code text/event-stream}. Event names:
     * {@code token} (data = raw text delta), {@code done} (data = finish reason),
     * {@code error} (data = the standard {@code ApiResponse} error envelope).
     */
    @PostMapping(value = "/{id}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long id,
                                  @Valid @RequestBody SendMessageRequest request) {
        return conversationService.streamReply(principal.id(), id, request);
    }
}
