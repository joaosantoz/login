package com.cadastro.sistema.controller;

import com.cadastro.sistema.dto.user.CreateUserRequest;
import com.cadastro.sistema.dto.user.UpdateUserRequest;
import com.cadastro.sistema.dto.user.UserResponse;
import com.cadastro.sistema.security.AuthenticatedUser;
import com.cadastro.sistema.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UserController {

	private final UserService service;

	public UserController(UserService service) {
		this.service = service;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
	public List<UserResponse> list() {
		return service.findAll();
	}

	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
		return service.findById(AuthenticatedUser.from(jwt).id());
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR') or @access.isSelf(authentication, #id)")
	public UserResponse get(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
		UserResponse created = service.create(request);
		var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
	public UserResponse update(
		@PathVariable Long id,
		@Valid @RequestBody UpdateUserRequest request,
		@AuthenticationPrincipal Jwt jwt
	) {
		return service.update(id, request, AuthenticatedUser.from(jwt));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
		service.delete(id, AuthenticatedUser.from(jwt));
		return ResponseEntity.noContent().build();
	}
}
