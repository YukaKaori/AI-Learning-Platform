package com.yuka.ailearningserver.preference.dto;

import com.yuka.ailearningserver.preference.entity.UserPreference;

/**
 * The user's effective preferences. When no row exists yet, {@link #DEFAULTS}
 * is returned as-is — the defaults are an API contract (mirrored by the V4
 * column defaults), not merely a storage detail.
 */
public record PreferencesResponse(String theme, String locale, int dailyGoalMinutes) {

    public static final PreferencesResponse DEFAULTS = new PreferencesResponse("system", "zh-CN", 60);

    public static PreferencesResponse from(UserPreference preference) {
        return new PreferencesResponse(
                preference.getTheme(),
                preference.getLocale(),
                preference.getDailyGoalMinutes());
    }
}
