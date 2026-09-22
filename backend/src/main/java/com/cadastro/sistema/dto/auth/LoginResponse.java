package com.cadastro.sistema.dto.auth;

import com.cadastro.sistema.dto.user.UserResponse;

import java.time.Instant;

public record LoginResponse(String accessToken, String tokenType, Instant expiresAt, UserResponse user) {
}
