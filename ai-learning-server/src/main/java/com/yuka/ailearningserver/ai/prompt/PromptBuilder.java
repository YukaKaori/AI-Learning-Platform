package com.yuka.ailearningserver.ai.prompt;

import com.yuka.ailearningserver.ai.context.LearningContext;
import com.yuka.ailearningserver.ai.provider.ChatRole;
import com.yuka.ailearningserver.ai.provider.ChatTurn;
import com.yuka.ailearningserver.ai.util.PromptSizeGuard;
import com.yuka.ailearningserver.config.AppProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a {@link PromptTemplate} + {@link LearningContext} + conversation
 * history + user input into the final message list sent to the provider.
 * Every AI use-case (chat and one-shot generation alike) goes through this —
 * no controller or service ever concatenates prompt strings itself.
 */
@Component
public class PromptBuilder {

    private static final int FOCUS_CONTENT_LIMIT = 4000;

    private final int maxPromptChars;

    public PromptBuilder(AppProperties properties) {
        this.maxPromptChars = properties.ai() != null ? properties.ai().maxPromptChars() : 24000;
    }

    public List<ChatTurn> build(PromptTemplate template, LearningContext context, List<ChatTurn> history,
                                 String userInput) {
        String contextBlock = renderContext(context);
        String system = contextBlock.isBlank()
                ? template.systemPrompt()
                : template.systemPrompt() + "\n\n## 学习背景\n" + contextBlock;

        List<ChatTurn> messages = new ArrayList<>();
        messages.add(new ChatTurn(ChatRole.SYSTEM, system));
        if (history != null) {
            messages.addAll(history);
        }
        if (userInput != null && !userInput.isBlank()) {
            messages.add(new ChatTurn(ChatRole.USER, userInput));
        }

        int totalChars = system.length() + messages.stream().mapToInt(m -> m.content().length()).sum();
        PromptSizeGuard.ensureWithinLimit(totalChars, maxPromptChars);
        return messages;
    }

    private String renderContext(LearningContext context) {
        if (context == null || context.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (context.subjectName() != null && !context.subjectName().isBlank()) {
            sb.append("- 当前学科：").append(context.subjectName());
            if (context.subjectDescription() != null && !context.subjectDescription().isBlank()) {
                sb.append("——").append(context.subjectDescription());
            }
            sb.append('\n');
        }
        if (context.subjectMaterialTitles() != null && !context.subjectMaterialTitles().isEmpty()) {
            sb.append("- 学科资料：").append(String.join("、", context.subjectMaterialTitles())).append('\n');
        }
        if (context.totalNotes() > 0) {
            sb.append("- 用户共有 ").append(context.totalNotes()).append(" 篇笔记");
            if (context.recentNoteTitles() != null && !context.recentNoteTitles().isEmpty()) {
                sb.append("，最近的笔记：").append(String.join("、", context.recentNoteTitles()));
            }
            sb.append('\n');
        }
        if (context.totalFlashcards() > 0) {
            sb.append("- 记忆卡片共 ").append(context.totalFlashcards()).append(" 张，其中 ")
                    .append(context.dueFlashcards()).append(" 张待复习\n");
        }
        if (context.statsSnapshot() != null && !context.statsSnapshot().isBlank()) {
            sb.append("- 学习统计：").append(context.statsSnapshot()).append('\n');
        }
        if (context.focusContent() != null && !context.focusContent().isBlank()) {
            String label = context.focusLabel() != null && !context.focusLabel().isBlank()
                    ? context.focusLabel() : "当前内容";
            sb.append("- ").append(label).append("：\n").append(truncate(context.focusContent())).append('\n');
        }
        return sb.toString();
    }

    private static String truncate(String text) {
        return text.length() > FOCUS_CONTENT_LIMIT ? text.substring(0, FOCUS_CONTENT_LIMIT) + "…" : text;
    }
}
