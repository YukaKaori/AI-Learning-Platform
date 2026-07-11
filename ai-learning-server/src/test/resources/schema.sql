-- H2 (MySQL mode) schema for tests — Flyway is disabled on the test profile,
-- so the tables exercised by the suite are mirrored here. Keep in sync with
-- db/migration/V1__create_user_tables.sql.

DROP TABLE IF EXISTS users;
CREATE TABLE users
(
    id            BIGINT       NOT NULL,
    username      VARCHAR(32)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nickname      VARCHAR(64)  NULL,
    avatar        VARCHAR(512) NULL,
    status        TINYINT      NOT NULL DEFAULT 0,
    last_login_at DATETIME     NULL,
    last_login_ip VARCHAR(45)  NULL,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NOT NULL,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE (username),
    UNIQUE (email)
);

DROP TABLE IF EXISTS refresh_tokens;
CREATE TABLE refresh_tokens
(
    id             BIGINT       NOT NULL,
    user_id        BIGINT       NOT NULL,
    token_hash     CHAR(64)     NOT NULL,
    expires_at     DATETIME     NOT NULL,
    revoked_at     DATETIME     NULL,
    replaced_by_id BIGINT       NULL,
    client_ip      VARCHAR(45)  NULL,
    user_agent     VARCHAR(255) NULL,
    created_at     DATETIME     NOT NULL,
    updated_at     DATETIME     NOT NULL,
    deleted        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE (token_hash)
);
