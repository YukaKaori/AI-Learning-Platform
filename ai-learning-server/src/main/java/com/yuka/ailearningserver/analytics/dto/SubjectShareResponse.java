package com.yuka.ailearningserver.analytics.dto;

/**
 * Study minutes attributed to one subject over the requested window.
 * A null {@code subjectId}/{@code subjectName} row carries the minutes of
 * sessions not linked to any subject; percentages are left to the client so
 * the raw minutes stay honest.
 */
public record SubjectShareResponse(String subjectId, String subjectName, String color, long minutes) {
}
