package com.citas.api.infrastructure.adapters.in.web.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CA de HU-002 por HTTP, JPA y MySQL real. Los secretos de prueba nunca se imprimen. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class PasswordRecoveryApiIntegrationTest {
    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final String OLD_PASSWORD = "Anterior" + 123;
    private static final String NEW_PASSWORD = "NuevaClave" + 123;

    @Container @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withCommand("--default-time-zone=America/Bogota");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    void ca01_laSolicitudTieneLaMismaRespuestaConEmailExistenteOInexistente() throws Exception {
        Map<String, Object> user = registered();
        requestReset((String) user.get("email")).andExpect(status().isNoContent());
        requestReset("nadie" + SEQUENCE.incrementAndGet() + "@test.local").andExpect(status().isNoContent());

        Long userId = jdbc.queryForObject("select id from users where email = ?", Long.class, user.get("email"));
        assertThat(jdbc.queryForObject("select count(*) from password_reset_tokens where user_id = ?", Integer.class, userId))
                .isEqualTo(1);
    }

    @Test
    void ca02_conTokenVigenteCambiaLaContrasenaYConsumeElToken() throws Exception {
        Map<String, Object> user = registered();
        String token = "synthetic-valid-reset-" + SEQUENCE.incrementAndGet();
        insertToken(user, token, LocalDateTime.now().plusMinutes(20));

        postJson("/api/v1/auth/password-resets", Map.of("token", token, "password", NEW_PASSWORD))
                .andExpect(status().isNoContent());
        login((String) user.get("email"), NEW_PASSWORD).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select used_at is not null from password_reset_tokens where token_hash = ?", Boolean.class,
                sha256(token))).isTrue();
    }

    @Test
    void ca03_unTokenYaUsadoNoSePuedeReutilizar() throws Exception {
        Map<String, Object> user = registered();
        String token = "synthetic-once-reset-" + SEQUENCE.incrementAndGet();
        insertToken(user, token, LocalDateTime.now().plusMinutes(20));
        postJson("/api/v1/auth/password-resets", Map.of("token", token, "password", NEW_PASSWORD)).andExpect(status().isNoContent());

        postJson("/api/v1/auth/password-resets", Map.of("token", token, "password", "OtraClave123"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PASSWORD_RESET_TOKEN"));
        login((String) user.get("email"), NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void ca04_tokenExpiradoOInexistenteNoModificaLaCuenta() throws Exception {
        Map<String, Object> user = registered();
        String expired = "synthetic-expired-reset-" + SEQUENCE.incrementAndGet();
        insertToken(user, expired, LocalDateTime.now().minusMinutes(1));

        for (String invalid : new String[]{expired, "synthetic-unknown-reset"}) {
            postJson("/api/v1/auth/password-resets", Map.of("token", invalid, "password", NEW_PASSWORD))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PASSWORD_RESET_TOKEN"));
        }
        login((String) user.get("email"), OLD_PASSWORD).andExpect(status().isOk());
    }

    private Map<String, Object> registered() throws Exception {
        int n = SEQUENCE.incrementAndGet();
        Map<String, Object> body = Map.of("firstNames", "Ana", "lastNames", "Prueba", "documentType", "CC",
                "documentNumber", "700" + String.format("%05d", n), "email", "recovery" + n + "@test.local",
                "phone", "3001234567", "password", OLD_PASSWORD);
        postJson("/api/v1/auth/register", body).andExpect(status().isCreated());
        return body;
    }

    private void insertToken(Map<String, Object> user, String token, LocalDateTime expiresAt) throws Exception {
        Long userId = jdbc.queryForObject("select id from users where email = ?", Long.class, user.get("email"));
        jdbc.update("insert into password_reset_tokens (user_id, token_hash, created_at, expires_at) values (?, ?, ?, ?)",
                userId, sha256(token), expiresAt.minusMinutes(30), expiresAt);
    }

    private org.springframework.test.web.servlet.ResultActions requestReset(String email) throws Exception {
        return postJson("/api/v1/auth/password-reset-requests", Map.of("email", email));
    }
    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return postJson("/api/v1/auth/login", Map.of("email", email, "password", password));
    }
    private org.springframework.test.web.servlet.ResultActions postJson(String path, Object body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)));
    }
    private static String sha256(String value) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
