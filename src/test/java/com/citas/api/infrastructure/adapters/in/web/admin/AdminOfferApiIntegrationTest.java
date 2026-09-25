package com.citas.api.infrastructure.adapters.in.web.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterios de aceptación de HU-006 (especialidades), HU-008 (crear profesional) y HU-009
 * (activar/desactivar), contra MySQL 8.4 real con las migraciones V1 a V4.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AdminOfferApiIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withCommand("--default-time-zone=America/Bogota");

    private static final String ADMIN_EMAIL = "admin@citas.local";
    private static final String ADMIN_PASSWORD = "Admin.Lab2026";
    private static final String PASSWORD = "Segura123";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    private String adminToken;

    @BeforeEach
    void loginComoAdmin() throws Exception {
        adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    // ---------- HU-006: especialidades ----------

    @Test
    void ca01_adminCreaUnaEspecialidadActivaConDuracionValida() throws Exception {
        String name = "Cardiología " + SEQUENCE.incrementAndGet();

        postAdmin("/api/v1/admin/specialties", Map.of("name", name, "durationMinutes", 60))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.durationMinutes").value(60))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.general").value(false));
    }

    @Test
    void ca02_unaDuracionDistintaDe30O60SeRechaza() throws Exception {
        for (int duracion : new int[]{20, 45, 90, 0}) {
            postAdmin("/api/v1/admin/specialties",
                    Map.of("name", "Duración " + duracion + "-" + SEQUENCE.incrementAndGet(),
                            "durationMinutes", duracion))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("durationMinutes"));
        }
    }

    @Test
    void ca03_unUsuarioSinRolAdminNoPuedeGestionarEspecialidades() throws Exception {
        String userToken = tokenDeUnUserNuevo();

        mvc.perform(get("/api/v1/admin/specialties").header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(post("/api/v1/admin/specialties").header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("name", "X", "durationMinutes", 30))))
                .andExpect(status().isForbidden());
    }

    @Test
    void ca03_sinTokenLaAdministracionDevuelve401() throws Exception {
        mvc.perform(get("/api/v1/admin/specialties")).andExpect(status().isUnauthorized());
    }

    @Test
    void ca04_unaEspecialidadReferenciadaNoSeBorraPeroSePuedeDesactivar() throws Exception {
        long specialtyId = crearEspecialidad("Neurología " + SEQUENCE.incrementAndGet(), 60);
        crearProfesional(specialtyId);

        // No existe DELETE: el verbo ni siquiera está mapeado.
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/admin/specialties/" + specialtyId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isMethodNotAllowed());

        patchAdmin("/api/v1/admin/specialties/" + specialtyId + "/active", Map.of("active", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // La fila sigue existiendo y el profesional conserva su asignación.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM specialties WHERE id = ?", Integer.class, specialtyId))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM professional_specialties WHERE specialty_id = ?",
                Integer.class, specialtyId)).isEqualTo(1);
    }

    @Test
    void elNombreDeEspecialidadEsUnico() throws Exception {
        String name = "Dermatología " + SEQUENCE.incrementAndGet();
        crearEspecialidad(name, 30);

        postAdmin("/api/v1/admin/specialties", Map.of("name", name, "durationMinutes", 30))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SPECIALTY_NAME_ALREADY_REGISTERED"));
    }

    @Test
    void medicinaGeneralEstaSembradaComoLaUnicaEspecialidadGeneral() throws Exception {
        String body = mvc.perform(get("/api/v1/admin/specialties")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        List<JsonNode> generales = new java.util.ArrayList<>();
        json.readTree(body).get("items").forEach(item -> {
            if (item.get("general").asBoolean()) {
                generales.add(item);
            }
        });
        assertThat(generales).hasSize(1);
        assertThat(generales.get(0).get("name").asText()).isEqualTo("Medicina General");
        assertThat(generales.get(0).get("durationMinutes").asInt()).isEqualTo(30);
    }

    // ---------- HU-008: crear profesional ----------

    @Test
    void ca01_elProfesionalCreadoPuedeIniciarSesionYConservaSusAsignaciones() throws Exception {
        long specialtyId = crearEspecialidad("Pediatría " + SEQUENCE.incrementAndGet(), 30);
        Map<String, Object> request = profesional(specialtyId);

        String body = postAdmin("/api/v1/admin/professionals", request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.siteCodes.length()").value(2))
                .andExpect(jsonPath("$.temporaryPassword").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        long professionalId = json.readTree(body).get("id").asLong();
        assertThat(jdbc.queryForObject("SELECT role_code FROM user_roles WHERE user_id = ?", String.class,
                professionalId)).isEqualTo("PROFESSIONAL");

        // Puede iniciar sesión con su contraseña temporal.
        assertThat(login((String) request.get("email"), PASSWORD)).isNotBlank();
    }

    @Test
    void ca02_sinPrimariaOConVariasPrimariasSeRechaza() throws Exception {
        long unaId = crearEspecialidad("Oftalmología " + SEQUENCE.incrementAndGet(), 30);
        long otraId = crearEspecialidad("Otorrino " + SEQUENCE.incrementAndGet(), 60);

        Map<String, Object> sinPrimaria = profesional(unaId);
        sinPrimaria.put("specialties", List.of(Map.of("specialtyId", unaId, "primary", false)));
        postAdmin("/api/v1/admin/professionals", sinPrimaria)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("specialties"));

        Map<String, Object> dosPrimarias = profesional(unaId);
        dosPrimarias.put("specialties", List.of(
                Map.of("specialtyId", unaId, "primary", true),
                Map.of("specialtyId", otraId, "primary", true)));
        postAdmin("/api/v1/admin/professionals", dosPrimarias)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("specialties"));
    }

    @Test
    void ca03_sinSedesSeRechaza() throws Exception {
        long specialtyId = crearEspecialidad("Urología " + SEQUENCE.incrementAndGet(), 30);
        Map<String, Object> request = profesional(specialtyId);
        request.put("siteCodes", List.of());

        postAdmin("/api/v1/admin/professionals", request).andExpect(status().isBadRequest());
    }

    @Test
    void ca04_codigoMatriculaEmailYDocumentoSonUnicos() throws Exception {
        long specialtyId = crearEspecialidad("Endocrinología " + SEQUENCE.incrementAndGet(), 30);
        Map<String, Object> primero = profesional(specialtyId);
        postAdmin("/api/v1/admin/professionals", primero).andExpect(status().isCreated());

        Map<String, Object> mismoCodigo = profesional(specialtyId);
        mismoCodigo.put("professionalCode", primero.get("professionalCode"));
        postAdmin("/api/v1/admin/professionals", mismoCodigo)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROFESSIONAL_CODE_ALREADY_REGISTERED"));

        Map<String, Object> mismaMatricula = profesional(specialtyId);
        mismaMatricula.put("licenseNumber", primero.get("licenseNumber"));
        postAdmin("/api/v1/admin/professionals", mismaMatricula)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LICENSE_ALREADY_REGISTERED"));

        Map<String, Object> mismoEmail = profesional(specialtyId);
        mismoEmail.put("email", primero.get("email"));
        postAdmin("/api/v1/admin/professionals", mismoEmail)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));

        Map<String, Object> mismoDocumento = profesional(specialtyId);
        mismoDocumento.put("documentNumber", primero.get("documentNumber"));
        postAdmin("/api/v1/admin/professionals", mismoDocumento)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DOCUMENT_ALREADY_REGISTERED"));
    }

    @Test
    void ca05_unProfesionalNoPuedeAdministrarProfesionales() throws Exception {
        long specialtyId = crearEspecialidad("Reumatología " + SEQUENCE.incrementAndGet(), 30);
        Map<String, Object> request = profesional(specialtyId);
        postAdmin("/api/v1/admin/professionals", request).andExpect(status().isCreated());
        String profToken = login((String) request.get("email"), PASSWORD);

        mvc.perform(get("/api/v1/admin/professionals").header(HttpHeaders.AUTHORIZATION, "Bearer " + profToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unaEspecialidadInactivaNoSePuedeAsignar() throws Exception {
        long specialtyId = crearEspecialidad("Geriatría " + SEQUENCE.incrementAndGet(), 30);
        patchAdmin("/api/v1/admin/specialties/" + specialtyId + "/active", Map.of("active", false))
                .andExpect(status().isOk());

        postAdmin("/api/v1/admin/professionals", profesional(specialtyId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("specialties"));
    }

    @Test
    void unaSedeInexistenteSeRechaza() throws Exception {
        long specialtyId = crearEspecialidad("Nefrología " + SEQUENCE.incrementAndGet(), 30);
        Map<String, Object> request = profesional(specialtyId);
        request.put("siteCodes", List.of("XXX"));

        postAdmin("/api/v1/admin/professionals", request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("siteCodes"));
    }

    @Test
    void reasignarEspecialidadesYSedesReemplazaLasAnteriores() throws Exception {
        long primera = crearEspecialidad("Hematología " + SEQUENCE.incrementAndGet(), 30);
        long segunda = crearEspecialidad("Oncología " + SEQUENCE.incrementAndGet(), 60);
        long professionalId = crearProfesional(primera);

        putAdmin("/api/v1/admin/professionals/" + professionalId + "/specialties",
                Map.of("specialties", List.of(
                        Map.of("specialtyId", primera, "primary", false),
                        Map.of("specialtyId", segunda, "primary", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialties.length()").value(2));

        putAdmin("/api/v1/admin/professionals/" + professionalId + "/sites",
                Map.of("siteCodes", List.of("ICV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siteCodes.length()").value(1))
                .andExpect(jsonPath("$.siteCodes[0]").value("ICV"));

        assertThat(jdbc.queryForObject("SELECT specialty_id FROM professional_specialties "
                        + "WHERE professional_id = ? AND is_primary = 1", Long.class, professionalId))
                .isEqualTo(segunda);
    }

    // ---------- HU-009: activar y desactivar ----------

    @Test
    void ca01_ca03_desactivarYReactivarConservaDatosYAsignaciones() throws Exception {
        long specialtyId = crearEspecialidad("Psiquiatría " + SEQUENCE.incrementAndGet(), 60);
        long professionalId = crearProfesional(specialtyId);

        patchAdmin("/api/v1/admin/professionals/" + professionalId + "/active", Map.of("active", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.specialties.length()").value(1));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM professional_sites WHERE professional_id = ?",
                Integer.class, professionalId)).isEqualTo(2);

        patchAdmin("/api/v1/admin/professionals/" + professionalId + "/active", Map.of("active", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void unProfesionalInexistenteDevuelve404() throws Exception {
        patchAdmin("/api/v1/admin/professionals/999999/active", Map.of("active", false))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // ---------- Utilidades ----------

    private long crearEspecialidad(String name, int duration) throws Exception {
        String body = postAdmin("/api/v1/admin/specialties", Map.of("name", name, "durationMinutes", duration))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(body).get("id").asLong();
    }

    private long crearProfesional(long specialtyId) throws Exception {
        String body = postAdmin("/api/v1/admin/professionals", profesional(specialtyId))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(body).get("id").asLong();
    }

    private static Map<String, Object> profesional(long specialtyId) {
        int n = SEQUENCE.incrementAndGet();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstNames", "Carlos");
        body.put("lastNames", "Profesional");
        body.put("documentType", "CC");
        body.put("documentNumber", "600" + String.format("%05d", n));
        body.put("email", "profesional" + n + "@test.local");
        body.put("phone", "3001234567");
        body.put("temporaryPassword", PASSWORD);
        body.put("professionalCode", "PRO-" + n);
        body.put("licenseNumber", "MAT-" + n);
        body.put("specialties", List.of(Map.of("specialtyId", specialtyId, "primary", true)));
        body.put("siteCodes", List.of("HIC", "ICV"));
        return body;
    }

    private String tokenDeUnUserNuevo() throws Exception {
        int n = SEQUENCE.incrementAndGet();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstNames", "Ana");
        body.put("lastNames", "Usuaria");
        body.put("documentType", "CC");
        body.put("documentNumber", "500" + String.format("%05d", n));
        body.put("email", "usuaria" + n + "@test.local");
        body.put("phone", "3001234567");
        body.put("password", PASSWORD);

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(body))).andExpect(status().isCreated());
        return login((String) body.get("email"), PASSWORD);
    }

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(body).get("accessToken").asText();
    }

    private ResultActions postAdmin(String path, Object body) throws Exception {
        return mvc.perform(post(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)));
    }

    private ResultActions putAdmin(String path, Object body) throws Exception {
        return mvc.perform(put(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)));
    }

    private ResultActions patchAdmin(String path, Object body) throws Exception {
        return mvc.perform(patch(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)));
    }
}
