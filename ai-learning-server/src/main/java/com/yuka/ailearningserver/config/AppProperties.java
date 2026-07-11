package com.yuka.ailearningserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Typed access to all custom {@code app.*} configuration.
 * Add new settings here instead of scattering {@code @Value} lookups.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, Security security) {

    public record Cors(List<String> allowedOrigins) {
    }

    public record Security(Jwt jwt, PasswordPolicy passwordPolicy) {

        /**
         * @param secret          HMAC-SHA256 signing key, at least 32 bytes. Injected via the
         *                        {@code JWT_SECRET} environment variable outside local dev.
         * @param issuer          {@code iss} claim, verified on every parse.
         * @param accessTokenTtl  lifetime of access tokens (short — minutes).
         * @param refreshTokenTtl lifetime of refresh tokens (long — days); also the maximum
         *                        length of a login session without re-authentication.
         */
        public record Jwt(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {
        }

        /**
         * Password policy applied when passwords are created or changed (registration and
         * password-reset arrive in a later phase; login never re-validates policy).
         */
        public record PasswordPolicy(int minLength) {
        }
    }
}
