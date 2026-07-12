/**
 * Study calendar — time-anchored study sessions. The calendar read model
 * merges {@code study_sessions} (this package) with due-dated
 * {@code learning_tasks} (task package); tasks are not duplicated here.
 *
 * <p>Phase 5 is schema + entity only. Sessions are also the raw signal for
 * the analytics domain (study time, streaks).
 * Reserved error-code range: 160000–169999.
 */
package com.yuka.ailearningserver.calendar;
