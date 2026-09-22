package com.cadastro.sistema.security;

import com.cadastro.sistema.config.JwtProperties;
import com.cadastro.sistema.entity.User;
import com.cadastro.sistema.security.TokenClaims;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class TokenService {

	private final JwtEncoder encoder;
	private final JwtProperties properties;
	private final Clock clock = Clock.systemUTC();

	public TokenService(JwtEncoder encoder, JwtProperties properties) {
		this.encoder = encoder;
		this.properties = properties;
	}

	public IssuedToken issue(User user) {
		Instant now = clock.instant();
		Instant expiresAt = now.plus(properties.ttl());
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(properties.issuer())
			.subject(String.valueOf(user.getId()))
			.issuedAt(now)
			.expiresAt(expiresAt)
			.claim(TokenClaims.NAME, user.getName())
			.claim(TokenClaims.EMAIL, user.getEmail())
			.claim(TokenClaims.ROLE, user.getRole().name())
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new IssuedToken(value, expiresAt);
	}

	public record IssuedToken(String value, Instant expiresAt) {
	}
}
