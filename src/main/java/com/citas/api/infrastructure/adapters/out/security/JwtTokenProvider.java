package com.citas.api.infrastructure.adapters.out.security;

import com.citas.api.application.port.out.TokenProviderPort;
import com.citas.api.domain.model.user.Role;
import com.citas.api.domain.model.user.User;
import com.citas.api.infrastructure.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT HS256. Access y refresh se firman con secretos distintos y llevan {@code typ} distinto,
 * por lo que un token nunca es aceptado en el lugar del otro (CA-08, CA-10).
 */
@Component
public class JwtTokenProvider implements TokenProviderPort {

    static final String TYPE_CLAIM = "typ";
    static final String ACCESS_TYPE = "access";
    static final String REFRESH_TYPE = "refresh";
    private static final String ISSUER = "citas-api";

    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private final Clock clock;

    public JwtTokenProvider(JwtProperties properties, Clock clock) {
        this.accessKey = Keys.hmacShaKeyFor(properties.accessSecret().getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(properties.refreshSecret().getBytes(StandardCharsets.UTF_8));
        this.accessTtl = Duration.ofMinutes(properties.accessMinutes());
        this.refreshTtl = Duration.ofDays(properties.refreshDays());
        this.clock = clock;
    }

    @Override
    public IssuedToken issueAccessToken(User user, Instant now) {
        Instant expiresAt = now.plus(accessTtl);
        String token = Jwts.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(user.getId()))
                .id(UUID.randomUUID().toString())
                .claim(TYPE_CLAIM, ACCESS_TYPE)
                .claim("email", user.getEmail().value())
                .claim("roles", user.getRoles().stream().map(Role::name).sorted().toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(accessKey)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    @Override
    public IssuedToken issueRefreshToken(Long userId, Instant now) {
        Instant expiresAt = now.plus(refreshTtl);
        String token = Jwts.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .claim(TYPE_CLAIM, REFRESH_TYPE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(refreshKey)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    @Override
    public Optional<Long> parseRefreshToken(String refreshToken) {
        return parse(refreshToken, refreshKey, REFRESH_TYPE).map(claims -> Long.valueOf(claims.getSubject()));
    }

    /** Lee un access token válido; vacío si la firma, el tipo o la expiración no son válidos. */
    public Optional<AuthenticatedUser> parseAccessToken(String accessToken) {
        return parse(accessToken, accessKey, ACCESS_TYPE).map(claims -> {
            List<?> roles = claims.get("roles", List.class);
            Set<String> roleNames = roles == null ? Set.of()
                    : roles.stream().map(String::valueOf).collect(Collectors.toUnmodifiableSet());
            return new AuthenticatedUser(Long.valueOf(claims.getSubject()), claims.get("email", String.class),
                    roleNames);
        });
    }

    @Override
    public String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private Optional<Claims> parse(String token, SecretKey key, String expectedType) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(ISSUER)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!expectedType.equals(claims.get(TYPE_CLAIM, String.class)) || claims.getSubject() == null) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
