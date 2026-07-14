package com.yuka.ailearningserver.analytics.dto;

/**
 * Headline analytics numbers. "This week" is the rolling 7-day window ending
 * today (server default timezone); the delta compares it to the 7 days before
 * that. {@code weekDeltaPercent} is null when the previous week has no study
 * time (no meaningful baseline), and {@code taskCompletionPercent} is null
 * when the user has no tasks — the frontend renders both as "—", never a
 * fabricated zero.
 */
public record AnalyticsSummaryResponse(
        int weekMinutes,
        Integer weekDeltaPercent,
        int streakDays,
        Integer taskCompletionPercent,
        int aiChatsThisWeek) {
}
