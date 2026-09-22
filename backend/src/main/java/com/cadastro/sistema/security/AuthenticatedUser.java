package com.cadastro.sistema.security;

import com.cadastro.sistema.entity.Role;
import org.springframework.security.oauth2.jwt.Jwt;

public record AuthenticatedUser(Long id, Role role) {

	public static AuthenticatedUser from(Jwt jwt) {
		return new AuthenticatedUser(Long.valueOf(jwt.getSubject()), Role.valueOf(jwt.getClaimAsString(TokenClaims.ROLE)));
	}

	public boolean is(Role expected) {
		return role == expected;
	}
}
