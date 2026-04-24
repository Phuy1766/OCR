package vn.edu.congvan.auth.dto;

import java.time.Instant;

public record TokenPairResponse(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        UserDto user) {}
