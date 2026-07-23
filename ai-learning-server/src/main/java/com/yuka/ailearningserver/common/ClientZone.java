package com.yuka.ailearningserver.common;

import java.time.DateTimeException;
import java.time.ZoneId;

/**
 * Resolves the caller's timezone for "which day is it?" computations.
 *
 * <p>Instants are stored and compared absolutely, so a card is due the moment
 * {@code due_at <= now} regardless of zone. Timezone only matters for
 * <em>day-bucketing</em> — the daily new-card cap, "reviewed today", and
 * retention-by-day. Clients send their IANA zone (e.g. {@code Asia/Shanghai})
 * in the {@link #HEADER} header; a missing or unparseable value falls back to
 * the server default so the request never fails on a bad header.
 */
public final class ClientZone {

    /** Request header carrying the caller's IANA timezone id. */
    public static final String HEADER = "X-Client-Timezone";

    private ClientZone() {
    }

    public static ZoneId resolve(String header) {
        if (header != null && !header.isBlank()) {
            try {
                return ZoneId.of(header.trim());
            } catch (DateTimeException ignored) {
                // fall through to the server default
            }
        }
        return ZoneId.systemDefault();
    }
}
