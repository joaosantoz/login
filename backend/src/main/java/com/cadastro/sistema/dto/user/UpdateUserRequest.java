package com.cadastro.sistema.dto.user;

import com.cadastro.sistema.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
	@NotBlank @Size(max = 120) String name,
	@NotBlank @Email @Size(max = 254) String email,
	@NotNull Role role
) {
}
