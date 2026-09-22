package com.cadastro.sistema.service;

import com.cadastro.sistema.dto.user.CreateUserRequest;
import com.cadastro.sistema.dto.user.UpdateUserRequest;
import com.cadastro.sistema.dto.user.UserResponse;
import com.cadastro.sistema.entity.Role;
import com.cadastro.sistema.entity.User;
import com.cadastro.sistema.exception.ConflictException;
import com.cadastro.sistema.exception.NotFoundException;
import com.cadastro.sistema.repository.UserRepository;
import com.cadastro.sistema.security.AuthenticatedUser;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository repository;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
	}

	public List<UserResponse> findAll() {
		return repository.findAll(Sort.by("id")).stream().map(UserResponse::from).toList();
	}

	public UserResponse findById(Long id) {
		return UserResponse.from(load(id));
	}

	@Transactional
	public UserResponse create(CreateUserRequest request) {
		String email = normalize(request.email());
		if (repository.existsByEmail(email)) {
			throw emailInUse();
		}
		User user = new User(request.name().strip(), email, passwordEncoder.encode(request.password()), request.role());
		return UserResponse.from(repository.save(user));
	}

	@Transactional
	public UserResponse update(Long id, UpdateUserRequest request, AuthenticatedUser currentUser) {
		User user = load(id);
		if (currentUser.is(Role.OPERATOR)) {
			ensureOperatorMayEdit(user, request);
		}
		String email = normalize(request.email());
		if (repository.existsByEmailAndIdNot(email, id)) {
			throw emailInUse();
		}
		user.update(request.name().strip(), email, request.role());
		return UserResponse.from(repository.saveAndFlush(user));
	}

	@Transactional
	public void delete(Long id, AuthenticatedUser currentUser) {
		if (currentUser.id().equals(id)) {
			throw new ConflictException("Administrador não pode excluir a própria conta");
		}
		repository.delete(load(id));
	}

	private void ensureOperatorMayEdit(User target, UpdateUserRequest request) {
		if (target.isAdmin()) {
			throw new AccessDeniedException("Operador não pode alterar contas de administrador");
		}
		if (request.role() != target.getRole()) {
			throw new AccessDeniedException("Apenas administradores alteram perfis de acesso");
		}
	}

	private User load(Long id) {
		return repository.findById(id).orElseThrow(() -> new NotFoundException("Usuário %d não encontrado".formatted(id)));
	}

	private static String normalize(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}

	private static ConflictException emailInUse() {
		return new ConflictException("E-mail já cadastrado");
	}
}
