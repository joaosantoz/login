package com.cadastro.sistema.service;

import com.cadastro.sistema.dto.auth.LoginRequest;
import com.cadastro.sistema.dto.auth.LoginResponse;
import com.cadastro.sistema.dto.user.UserResponse;
import com.cadastro.sistema.entity.User;
import com.cadastro.sistema.repository.UserRepository;
import com.cadastro.sistema.security.TokenService;
import com.cadastro.sistema.security.TokenService.IssuedToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final UserRepository repository;
	private final TokenService tokenService;

	public AuthService(AuthenticationManager authenticationManager, UserRepository repository, TokenService tokenService) {
		this.authenticationManager = authenticationManager;
		this.repository = repository;
		this.tokenService = tokenService;
	}

	public LoginResponse login(LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
			UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password())
		);
		User user = repository.findByEmail(authentication.getName()).orElseThrow();
		IssuedToken token = tokenService.issue(user);
		return new LoginResponse(token.value(), "Bearer", token.expiresAt(), UserResponse.from(user));
	}
}
