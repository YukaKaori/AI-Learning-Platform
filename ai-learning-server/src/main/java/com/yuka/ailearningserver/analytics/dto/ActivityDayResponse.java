package com.yuka.ailearningserver.analytics.dto;

/**
 * One zero-filled day of study activity. {@code date} is an ISO local date
 * ({@code yyyy-MM-dd}) in the server default timezone — a calendar bucket,
 * not an instant, so the epoch-ms wire convention doesn't apply.
 */
public record ActivityDayResponse(String date, int minutes, int sessions) {
}
