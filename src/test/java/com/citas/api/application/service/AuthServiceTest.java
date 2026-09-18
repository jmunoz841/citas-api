package com.citas.api.application.service;

import com.citas.api.application.port.in.AuthTokens;
import com.citas.api.application.port.in.LoginUseCase.LoginCommand;
import com.citas.api.application.port.in.RegisterUserUseCase.RegisterUserCommand;
import com.citas.api.application.port.out.PasswordHasherPort;
import com.citas.api.application.port.out.RefreshTokenRepositoryPort;
import com.citas.api.application.port.out.TokenProviderPort;
import com.citas.api.application.port.out.UserRepositoryPort;
import com.citas.api.domain.exception.DocumentAlreadyRegisteredException;
import com.citas.api.domain.exception.EmailAlreadyRegisteredException;
import com.citas.api.domain.exception.InvalidCredentialsException;
import com.citas.api.domain.exception.InvalidFieldException;
import com.citas.api.domain.exception.InvalidRefreshTokenException;
import com.citas.api.domain.model.auth.RefreshToken;
import com.citas.api.domain.model.user.Email;
import com.citas.api.domain.model.user.IdentityDocument;
import com.citas.api.domain.model.user.Role;
import com.citas.api.domain.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Casos de uso de HU-001 con puertos falsos en memoria (sin Spring ni base de datos).
 */
class AuthServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-18T15:00:00Z"), ZoneId.of("America/Bogota"));

    private InMemoryUsers users;
    private InMemoryRefreshTokens refreshTokens;
    private AuthService service;

    @BeforeEach
    void setUp() {
        users = new InMemoryUsers();
        refreshTokens = new InMemoryRefreshTokens();
        service = new AuthService(users, refreshTokens, new FakeHasher(), new FakeTokens(), CLOCK);
    }

    private static RegisterUserCommand command(String email, String documentNumber) {
        return new RegisterUserCommand("Ana", "Prueba", "CC", documentNumber, email, "3001234567", "Segura123");
    }

    @Test
    void registraUserConHashYSinGuardarLaContrasena() {
        User user = service.register(command("Ana@Example.com", "1.234"));

        assertThat(user.getId()).isNotNull();
        assertThat(user.getRoles()).containsExactly(Role.USER);
        assertThat(user.getEmail().value()).isEqualTo("ana@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashed:Segura123").isNotEqualTo("Segura123");
    }

    @Test
    void rechazaEmailDuplicadoSinImportarMayusculas() {
        service.register(command("ana@example.com", "1234"));

        assertThatThrownBy(() -> service.register(command("ANA@EXAMPLE.COM", "9999")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
        assertThat(users.byId).hasSize(1);
    }

    @Test
    void rechazaDocumentoDuplicadoNormalizado() {
        service.register(command("ana@example.com", "1234"));

        assertThatThrownBy(() -> service.register(command("otra@example.com", "1.234")))
                .isInstanceOf(DocumentAlreadyRegisteredException.class);
    }

    @Test
    void rechazaContrasenaDebilAntesDeTocarElRepositorio() {
        RegisterUserCommand weak = new RegisterUserCommand("Ana", "Prueba", "CC", "1234", "ana@example.com",
                "3001234567", "solotexto");

        assertThatThrownBy(() -> service.register(weak)).isInstanceOf(InvalidFieldException.class);
        assertThat(users.byId).isEmpty();
    }

    @Test
    void loginCorrectoEmiteTokensDistintosYPersisteSoloElHash() {
        service.register(command("ana@example.com", "1234"));

        AuthTokens tokens = service.login(new LoginCommand("ana@example.com", "Segura123"));

        assertThat(tokens.accessToken()).isNotEqualTo(tokens.refreshToken());
        assertThat(tokens.accessExpiresInSeconds()).isEqualTo(900);
        assertThat(refreshTokens.byId.values()).singleElement()
                .satisfies(t -> assertThat(t.getTokenHash()).isEqualTo("h:" + tokens.refreshToken()));
    }

    @Test
    void loginFallaIgualConEmailInexistenteOContrasenaIncorrecta() {
        service.register(command("ana@example.com", "1234"));

        assertThatThrownBy(() -> service.login(new LoginCommand("nadie@example.com", "Segura123")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> service.login(new LoginCommand("ana@example.com", "Otra12345")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(refreshTokens.byId).isEmpty();
    }

    @Test
    void refreshRotaElTokenYElAnteriorNoSePuedeReusar() {
        service.register(command("ana@example.com", "1234"));
        AuthTokens first = service.login(new LoginCommand("ana@example.com", "Segura123"));

        AuthTokens second = service.refresh(first.refreshToken());

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        RefreshToken old = refreshTokens.findByHash("h:" + first.refreshToken()).orElseThrow();
        RefreshToken current = refreshTokens.findByHash("h:" + second.refreshToken()).orElseThrow();
        assertThat(old.getRevokedAt()).isNotNull();
        assertThat(old.getReplacedByTokenId()).isEqualTo(current.getId());
        assertThatThrownBy(() -> service.refresh(first.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refreshRechazaAccessTokenYTokensDesconocidos() {
        service.register(command("ana@example.com", "1234"));
        AuthTokens tokens = service.login(new LoginCommand("ana@example.com", "Segura123"));

        assertThatThrownBy(() -> service.refresh(tokens.accessToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> service.refresh("refresh-1-desconocido"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void logoutRevocaYEsIdempotente() {
        service.register(command("ana@example.com", "1234"));
        AuthTokens tokens = service.login(new LoginCommand("ana@example.com", "Segura123"));

        service.logout(tokens.refreshToken());
        service.logout(tokens.refreshToken());
        service.logout("basura");

        assertThatThrownBy(() -> service.refresh(tokens.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    // ---------- Puertos falsos ----------

    private static final class InMemoryUsers implements UserRepositoryPort {
        final Map<Long, User> byId = new HashMap<>();
        final AtomicLong sequence = new AtomicLong();

        @Override
        public boolean existsByEmail(Email email) {
            return byId.values().stream().anyMatch(u -> u.getEmail().equals(email));
        }

        @Override
        public boolean existsByDocument(IdentityDocument document) {
            return byId.values().stream().anyMatch(u -> u.getDocument().equals(document));
        }

        @Override
        public User save(User user) {
            long id = user.getId() != null ? user.getId() : sequence.incrementAndGet();
            User saved = User.restore(id, user.getFirstNames(), user.getLastNames(), user.getDocument(),
                    user.getEmail(), user.getPhone(), user.getPasswordHash(), user.isActive(), user.getRoles());
            byId.put(id, saved);
            return saved;
        }

        @Override
        public Optional<User> findByEmail(Email email) {
            return byId.values().stream().filter(u -> u.getEmail().equals(email)).findFirst();
        }

        @Override
        public Optional<User> findById(Long id) {
            return Optional.ofNullable(byId.get(id));
        }
    }

    private static final class InMemoryRefreshTokens implements RefreshTokenRepositoryPort {
        final Map<Long, RefreshToken> byId = new HashMap<>();
        final AtomicLong sequence = new AtomicLong();

        @Override
        public RefreshToken save(RefreshToken token) {
            long id = token.getId() != null ? token.getId() : sequence.incrementAndGet();
            RefreshToken saved = RefreshToken.restore(id, token.getUserId(), token.getTokenHash(),
                    token.getIssuedAt(), token.getExpiresAt(), token.getRevokedAt(), token.getReplacedByTokenId());
            byId.put(id, saved);
            return saved;
        }

        @Override
        public Optional<RefreshToken> findByHashForUpdate(String tokenHash) {
            return findByHash(tokenHash);
        }

        Optional<RefreshToken> findByHash(String tokenHash) {
            return byId.values().stream().filter(t -> t.getTokenHash().equals(tokenHash)).findFirst()
                    .map(t -> RefreshToken.restore(t.getId(), t.getUserId(), t.getTokenHash(), t.getIssuedAt(),
                            t.getExpiresAt(), t.getRevokedAt(), t.getReplacedByTokenId()));
        }
    }

    private static final class FakeHasher implements PasswordHasherPort {
        @Override
        public String hash(String rawPassword) {
            return "hashed:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String passwordHash) {
            return passwordHash.equals("hashed:" + rawPassword);
        }
    }

    private static final class FakeTokens implements TokenProviderPort {
        private final AtomicLong counter = new AtomicLong();

        @Override
        public IssuedToken issueAccessToken(User user, Instant now) {
            return new IssuedToken("access-" + user.getId() + "-" + counter.incrementAndGet(), now.plusSeconds(900));
        }

        @Override
        public IssuedToken issueRefreshToken(Long userId, Instant now) {
            return new IssuedToken("refresh-" + userId + "-" + counter.incrementAndGet(),
                    now.plusSeconds(7 * 24 * 3600));
        }

        @Override
        public Optional<Long> parseRefreshToken(String refreshToken) {
            if (refreshToken == null || !refreshToken.startsWith("refresh-")) {
                return Optional.empty();
            }
            return Optional.of(Long.valueOf(refreshToken.split("-")[1]));
        }

        @Override
        public String hash(String token) {
            return "h:" + token;
        }
    }
}
