package com.citas.api.infrastructure.adapters.out.security;

import com.citas.api.domain.model.user.DocumentType;
import com.citas.api.domain.model.user.Email;
import com.citas.api.domain.model.user.IdentityDocument;
import com.citas.api.domain.model.user.Role;
import com.citas.api.domain.model.user.User;
import com.citas.api.infrastructure.config.JwtProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final Instant NOW = Instant.parse("2026-09-18T15:00:00Z");
    private static final JwtProperties PROPERTIES = new JwtProperties(
            "test-access-secret-0123456789-abcdefghij", "test-refresh-secret-0123456789-abcdefghij", 15, 7);
    private static final User USER = User.restore(42L, "Ana", "Prueba", new IdentityDocument(DocumentType.CC, "1234"),
            new Email("ana@example.com"), "3001234567", "$2a$10$hash", true, Set.of(Role.USER));

    private static JwtTokenProvider providerAt(Instant instant) {
        return new JwtTokenProvider(PROPERTIES, Clock.fixed(instant, ZoneId.of("America/Bogota")));
    }

    @Test
    void accessTokenLlevaUsuarioYRoles() {
        JwtTokenProvider provider = providerAt(NOW);
        String access = provider.issueAccessToken(USER, NOW).value();

        assertThat(provider.parseAccessToken(access)).hasValueSatisfying(user -> {
            assertThat(user.userId()).isEqualTo(42L);
            assertThat(user.email()).isEqualTo("ana@example.com");
            assertThat(user.roles()).containsExactly("USER");
        });
    }

    @Test
    void accessYRefreshNoSonIntercambiables() {
        JwtTokenProvider provider = providerAt(NOW);
        String access = provider.issueAccessToken(USER, NOW).value();
        String refresh = provider.issueRefreshToken(42L, NOW).value();

        assertThat(provider.parseRefreshToken(access)).isEmpty();
        assertThat(provider.parseAccessToken(refresh)).isEmpty();
        assertThat(provider.parseRefreshToken(refresh)).contains(42L);
    }

    @Test
    void tokenExpiradoSeRechaza() {
        String access = providerAt(NOW).issueAccessToken(USER, NOW).value();
        String refresh = providerAt(NOW).issueRefreshToken(42L, NOW).value();

        assertThat(providerAt(NOW.plusSeconds(16 * 60)).parseAccessToken(access)).isEmpty();
        assertThat(providerAt(NOW.plusSeconds(8L * 24 * 3600)).parseRefreshToken(refresh)).isEmpty();
    }

    @Test
    void tokenAlteradoOBasuraSeRechaza() {
        JwtTokenProvider provider = providerAt(NOW);
        String[] original = provider.issueAccessToken(USER, NOW).value().split("\\.");
        User otro = User.restore(43L, "Beto", "Prueba", new IdentityDocument(DocumentType.CC, "5678"),
                new Email("beto@example.com"), "3001234567", "$2a$10$hash", true, Set.of(Role.ADMIN));
        String[] ajeno = provider.issueAccessToken(otro, NOW).value().split("\\.");
        // Payload de otro usuario con la firma original: la firma ya no corresponde.
        String alterado = original[0] + "." + ajeno[1] + "." + original[2];

        assertThat(provider.parseAccessToken(alterado)).isEmpty();
        assertThat(provider.parseAccessToken("abc.def.ghi")).isEmpty();
        assertThat(provider.parseRefreshToken(null)).isEmpty();
    }

    @Test
    void hashEsSha256HexEnMinusculas() {
        assertThat(providerAt(NOW).hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void secretosDebilesOIgualesImpidenArrancar() {
        assertThatThrownBy(() -> new JwtProperties("corto", "test-refresh-secret-0123456789-abcdefghij", 15, 7))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtProperties("test-access-secret-0123456789-abcdefghij",
                "test-access-secret-0123456789-abcdefghij", 15, 7))
                .isInstanceOf(IllegalStateException.class);
    }
}
