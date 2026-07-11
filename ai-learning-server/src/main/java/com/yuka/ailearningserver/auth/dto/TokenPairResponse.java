package com.yuka.ailearningserver.auth.dto;

/** Result of a refresh-token rotation. */
public record TokenPairResponse(String accessToken, String refreshToken, long expiresIn) {
}
