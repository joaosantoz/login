package com.cadastro.sistema.config;

import com.cadastro.sistema.entity.Role;
import com.cadastro.sistema.entity.User;
import com.cadastro.sistema.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {

	private final UserRepository repository;
	private final PasswordEncoder passwordEncoder;
	private final String password;

	public DemoDataInitializer(
		UserRepository repository,
		PasswordEncoder passwordEncoder,
		@Value("${app.seed.password}") String password
	) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.password = password;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (repository.count() > 0) {
			return;
		}
		String hash = passwordEncoder.encode(password);
		repository.saveAll(List.of(
			new User("Ana Administradora", "admin@sistema.local", hash, Role.ADMIN),
			new User("Otávio Operador", "operador@sistema.local", hash, Role.OPERATOR),
			new User("Clara Cliente", "cliente@sistema.local", hash, Role.CLIENT)
		));
	}
}
