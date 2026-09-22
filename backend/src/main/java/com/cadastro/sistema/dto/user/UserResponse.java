package com.cadastro.sistema.dto.user;

import com.cadastro.sistema.entity.Role;
import com.cadastro.sistema.entity.User;

import java.time.Instant;

public record UserResponse(Long id, String name, String email, Role role, Instant createdAt, Instant updatedAt) {

	public static UserResponse from(User user) {
		return new UserResponse(
			user.getId(),
			user.getName(),
			user.getEmail(),
			user.getRole(),
			user.getCreatedAt(),
			user.getUpdatedAt()
		);
	}
}
