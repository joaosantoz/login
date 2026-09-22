package com.cadastro.sistema.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component("access")
public class AccessPolicy {

	public boolean isSelf(Authentication authentication, Long id) {
		return authentication.getPrincipal() instanceof Jwt jwt && jwt.getSubject().equals(String.valueOf(id));
	}
}
