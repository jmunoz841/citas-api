package com.citas.api.infrastructure.adapters.in.web.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterios de aceptación de HU-001 de punta a punta: HTTP → casos de uso → JPA → MySQL 8.4 real
 * (Testcontainers, desechable) con la migración Flyway V1.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AuthApiIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withCommand("--default-time-zone=America/Bogota");

    private static final String PASSWORD = "Segura123";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    // ---------- Registro ----------

    @Test
    void ca01_registroExitosoCreaUserSinExponerLaContrasena() throws Exception {
        Map<String, Object> body = newUser();

        postJson("/api/v1/auth/register", body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(body.get("email")))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void ca02_laContrasenaSeGuardaComoHashBCrypt() throws Exception {
        Map<String, Object> body = newUser();
        postJson("/api/v1/auth/register", body).andExpect(status().isCreated());

        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class,
                body.get("email"));

        assertThat(hash).startsWith("$2").hasSize(60).doesNotContain(PASSWORD);
    }

    @Test
    void ca03_emailDuplicadoConOtrasMayusculasDevuelve409() throws Exception {
        Map<String, Object> first = newUser();
        postJson("/api/v1/auth/register", first).andExpect(status().isCreated());

        Map<String, Object> second = newUser();
        second.put("email", ((String) first.get("email")).toUpperCase());

        postJson("/api/v1/auth/register", second)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
        assertThat(countUsersByDocument((String) second.get("documentNumber"))).isZero();
    }

    @Test
    void ca04_documentoDuplicadoAunqueTengaPuntosDevuelve409() throws Exception {
        Map<String, Object> first = newUser();
        postJson("/api/v1/auth/register", first).andExpect(status().isCreated());

        String number = (String) first.get("documentNumber");
        Map<String, Object> second = newUser();
        second.put("documentNumber", number.substring(0, 3) + "." + number.substring(3));

        postJson("/api/v1/auth/register", second)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DOCUMENT_ALREADY_REGISTERED"));
    }

    @Test
    void ca05_camposInvalidosDevuelven400ConErroresPorCampo() throws Exception {
        Map<String, Object> body = newUser();
        body.put("email", "no-es-email");
        body.put("firstNames", "");

        postJson("/api/v1/auth/register", body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'firstNames')]").exists());
        assertThat(countUsersByDocument((String) body.get("documentNumber"))).isZero();
    }

    @Test
    void ca05_contrasenaQueNoCumpleLaPoliticaDevuelve400() throws Exception {
        for (String weak : new String[]{"Abc123", "solotexto", "12345678"}) {
            Map<String, Object> body = newUser();
            body.put("password", weak);

            postJson("/api/v1/auth/register", body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("password"));
        }
    }

    @Test
    void ca05_tipoDeDocumentoInexistenteDevuelve400() throws Exception {
        Map<String, Object> body = newUser();
        body.put("documentType", "XX");

        postJson("/api/v1/auth/register", body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("documentType"));
    }

    @Test
    void registroNoAceptaRolDesdeElCliente() throws Exception {
        Map<String, Object> body = newUser();
        body.put("roles", new String[]{"ADMIN"});

        postJson("/api/v1/auth/register", body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    // ---------- Login y acceso ----------

    @Test
    void ca06_loginCorrectoEmiteAccessYRefreshDistintos() throws Exception {
        Map<String, Object> user = registered();

        JsonNode tokens = login((String) user.get("email"), PASSWORD);

        assertThat(tokens.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(tokens.get("accessToken").asText()).isNotEqualTo(tokens.get("refreshToken").asText());
        assertThat(tokens.get("expiresIn").asLong()).isEqualTo(900);
        assertThat(tokens.get("refreshExpiresIn").asLong()).isEqualTo(7 * 24 * 3600);
    }

    @Test
    void ca07_credencialesInvalidasDevuelvenElMismo401() throws Exception {
        Map<String, Object> user = registered();

        String wrongPassword = postJson("/api/v1/auth/login", Map.of("email", user.get("email"), "password", "Otra12345"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String unknownEmail = postJson("/api/v1/auth/login", Map.of("email", "nadie@test.local", "password", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(json.readTree(wrongPassword).get("detail"))
                .isEqualTo(json.readTree(unknownEmail).get("detail"));
    }

    @Test
    void ca08_recursoProtegidoSoloConAccessTokenValido() throws Exception {
        Map<String, Object> user = registered();
        JsonNode tokens = login((String) user.get("email"), PASSWORD);

        mvc.perform(get("/api/v1/auth/session").header(HttpHeaders.AUTHORIZATION, bearer(tokens, "accessToken")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.get("email")))
                .andExpect(jsonPath("$.roles[0]").value("USER"));

        mvc.perform(get("/api/v1/auth/session"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get("/api/v1/auth/session").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/session").header(HttpHeaders.AUTHORIZATION, bearer(tokens, "refreshToken")))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Refresh y logout ----------

    @Test
    void ca09_refreshRotaElTokenYRevocaElAnterior() throws Exception {
        Map<String, Object> user = registered();
        JsonNode first = login((String) user.get("email"), PASSWORD);
        String oldRefresh = first.get("refreshToken").asText();

        JsonNode second = readJson(postJson("/api/v1/auth/refresh", Map.of("refreshToken", oldRefresh))
                .andExpect(status().isOk()));
        String newRefresh = second.get("refreshToken").asText();

        assertThat(newRefresh).isNotEqualTo(oldRefresh);
        Map<String, Object> oldRow = jdbc.queryForMap(
                "SELECT revoked_at, replaced_by_token_id FROM refresh_tokens WHERE token_hash = ?", sha256(oldRefresh));
        Long newId = jdbc.queryForObject("SELECT id FROM refresh_tokens WHERE token_hash = ?", Long.class,
                sha256(newRefresh));
        assertThat(oldRow.get("revoked_at")).isNotNull();
        assertThat(((Number) oldRow.get("replaced_by_token_id")).longValue()).isEqualTo(newId);

        postJson("/api/v1/auth/refresh", Map.of("refreshToken", oldRefresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void ca10_refreshInvalidoDevuelve401SinEmitirTokens() throws Exception {
        Map<String, Object> user = registered();
        JsonNode tokens = login((String) user.get("email"), PASSWORD);

        for (String invalid : new String[]{tokens.get("accessToken").asText(), "no.es.token", "basura"}) {
            postJson("/api/v1/auth/refresh", Map.of("refreshToken", invalid))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
                    .andExpect(jsonPath("$.accessToken").doesNotExist());
        }
    }

    @Test
    void ca11_logoutRevocaElRefreshToken() throws Exception {
        Map<String, Object> user = registered();
        String refresh = login((String) user.get("email"), PASSWORD).get("refreshToken").asText();

        postJson("/api/v1/auth/logout", Map.of("refreshToken", refresh)).andExpect(status().isNoContent());
        postJson("/api/v1/auth/logout", Map.of("refreshToken", refresh)).andExpect(status().isNoContent());

        postJson("/api/v1/auth/refresh", Map.of("refreshToken", refresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void refreshTokenSePersisteSoloComoHash() throws Exception {
        Map<String, Object> user = registered();
        String refresh = login((String) user.get("email"), PASSWORD).get("refreshToken").asText();

        Integer byHash = jdbc.queryForObject("SELECT COUNT(*) FROM refresh_tokens WHERE token_hash = ?",
                Integer.class, sha256(refresh));
        Integer byValue = jdbc.queryForObject("SELECT COUNT(*) FROM refresh_tokens WHERE token_hash = ?",
                Integer.class, refresh);

        assertThat(byHash).isEqualTo(1);
        assertThat(byValue).isZero();
    }

    // ---------- Transversal ----------

    @Test
    void jsonMalformadoDevuelve400() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{no json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void corsPermiteSoloElOrigenDelFrontend() throws Exception {
        mvc.perform(options("/api/v1/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5174")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5174"));

        mvc.perform(options("/api/v1/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthEsPublico() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    // ---------- Utilidades ----------

    private static Map<String, Object> newUser() {
        int n = SEQUENCE.incrementAndGet();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstNames", "Ana");
        body.put("lastNames", "Prueba");
        body.put("documentType", "CC");
        body.put("documentNumber", "900" + String.format("%05d", n));
        body.put("email", "usuario" + n + "@test.local");
        body.put("phone", "3001234567");
        body.put("password", PASSWORD);
        return body;
    }

    private Map<String, Object> registered() throws Exception {
        Map<String, Object> body = newUser();
        postJson("/api/v1/auth/register", body).andExpect(status().isCreated());
        return body;
    }

    private JsonNode login(String email, String password) throws Exception {
        return readJson(postJson("/api/v1/auth/login", Map.of("email", email, "password", password))
                .andExpect(status().isOk()));
    }

    private ResultActions postJson(String path, Object body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)));
    }

    private JsonNode readJson(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private int countUsersByDocument(String documentNumber) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE document_number = ?", Integer.class,
                documentNumber.replace(".", ""));
    }

    private static String bearer(JsonNode tokens, String field) {
        return "Bearer " + tokens.get(field).asText();
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
