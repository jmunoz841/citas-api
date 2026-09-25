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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterios de aceptación de HU-015 (el ADMIN aprueba o rechaza citas especializadas) contra
 * MySQL 8.4 real con las migraciones V1 a V6. Escritas antes de la implementación (Red → Green).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AdminAppointmentApiIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withCommand("--default-time-zone=America/Bogota");

    private static final String ADMIN_EMAIL = "admin@citas.local";
    private static final String ADMIN_PASSWORD = "Admin.Lab2026";
    private static final String PASSWORD = "Segura123";
    private static final String BASE = "/api/v1/admin/appointments";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final LocalDate MANANA = LocalDate.now().plusDays(1);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    private String adminToken;
    private Long adminId;
    private String userToken;

    @BeforeEach
    void preparar() throws Exception {
        adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        adminId = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, ADMIN_EMAIL);
        userToken = login(registrarUsuario(), PASSWORD);
    }

    @Test
    void ca01_aprobarPasaAApprovedYRegistraHistorialDelAdmin() throws Exception {
        Solicitud solicitud = solicitarCitaEspecializada(30, "08:00");

        mvc.perform(post(BASE + "/" + solicitud.citaId() + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        assertThat(estado(solicitud.citaId())).isEqualTo("APPROVED");
        List<Map<String, Object>> historial = historial(solicitud.citaId());
        assertThat(historial).hasSize(2);
        assertThat(historial.get(1).get("status_code")).isEqualTo("APPROVED");
        assertThat(historial.get(1).get("source")).isEqualTo("ADMIN");
        assertThat(((Number) historial.get(1).get("actor_user_id")).longValue()).isEqualTo(adminId);
        // Aprobar conserva la ocupación: el slot sigue siendo del paciente.
        assertThat(slotsOcupados(solicitud.citaId())).isEqualTo(1);
    }

    @Test
    void ca02_rechazarConMotivoGuardaElMotivoYLiberaLosSlots() throws Exception {
        Solicitud solicitud = solicitarCitaEspecializada(60, "08:00");
        assertThat(slotsOcupados(solicitud.citaId())).isEqualTo(2);

        rechazar(solicitud.citaId(), "El especialista no atiende esta patología")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        assertThat(estado(solicitud.citaId())).isEqualTo("REJECTED");
        assertThat(slotsOcupados(solicitud.citaId())).isZero();
        Map<String, Object> ultimo = historial(solicitud.citaId()).get(1);
        assertThat(ultimo.get("status_code")).isEqualTo("REJECTED");
        assertThat(ultimo.get("source")).isEqualTo("ADMIN");
        assertThat(ultimo.get("reason")).isEqualTo("El especialista no atiende esta patología");

        // Los slots liberados se pueden volver a reservar.
        String otroUsuario = login(registrarUsuario(), PASSWORD);
        reservar(otroUsuario, solicitud.profesionalId(), solicitud.especialidadId(), "08:00")
                .andExpect(status().isCreated());
    }

    @Test
    void ca03_rechazarSinMotivoEsUnErrorDeValidacion() throws Exception {
        Solicitud solicitud = solicitarCitaEspecializada(30, "08:00");

        rechazar(solicitud.citaId(), "   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("reason"));
        mvc.perform(post(BASE + "/" + solicitud.citaId() + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reason"));

        assertThat(estado(solicitud.citaId())).isEqualTo("REQUESTED");
        assertThat(slotsOcupados(solicitud.citaId())).isEqualTo(1);
    }

    @Test
    void ca04_soloUnaCitaSolicitadaPuedeAprobarseORechazarse() throws Exception {
        Solicitud solicitud = solicitarCitaEspecializada(30, "08:00");
        rechazar(solicitud.citaId(), "Sin cupo").andExpect(status().isOk());

        mvc.perform(post(BASE + "/" + solicitud.citaId() + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
        rechazar(solicitud.citaId(), "Otra vez")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        assertThat(historial(solicitud.citaId())).hasSize(2);
    }

    @Test
    void ca04_unaCitaGeneralAprobadaNoPasaPorElAdmin() throws Exception {
        long generalId = jdbc.queryForObject("SELECT id FROM specialties WHERE is_general = TRUE", Long.class);
        Profesional prof = crearProfesional(generalId);
        crearBloque(prof, "08:00", "08:30");
        long citaId = idDe(reservar(userToken, prof.id(), generalId, "08:00").andExpect(status().isCreated()));

        mvc.perform(post(BASE + "/" + citaId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void aprobarYRechazarALaVezSoloResuelveUnaVez() throws Exception {
        Solicitud solicitud = solicitarCitaEspecializada(30, "08:00");

        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            List<Callable<MvcResult>> acciones = List.of(
                    () -> {
                        salida.await();
                        return mvc.perform(post(BASE + "/" + solicitud.citaId() + "/approve")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andReturn();
                    },
                    () -> {
                        salida.await();
                        return rechazar(solicitud.citaId(), "Sin cupo").andReturn();
                    });
            List<Future<MvcResult>> resultados = new ArrayList<>();
            acciones.forEach(accion -> resultados.add(hilos.submit(accion)));
            salida.countDown();

            List<Integer> estados = new ArrayList<>();
            for (Future<MvcResult> resultado : resultados) {
                estados.add(resultado.get(30, TimeUnit.SECONDS).getResponse().getStatus());
            }
            assertThat(estados).containsExactlyInAnyOrder(200, 409);
        } finally {
            hilos.shutdownNow();
        }
        assertThat(historial(solicitud.citaId())).hasSize(2);
    }

    @Test
    void unaCitaInexistenteDevuelve404() throws Exception {
        mvc.perform(post(BASE + "/999999/approve").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void elListadoMuestraSoloLasSolicitudesPendientes() throws Exception {
        Solicitud pendiente = solicitarCitaEspecializada(30, "08:00");
        Solicitud resuelta = solicitarCitaEspecializada(30, "09:00");
        rechazar(resuelta.citaId(), "Sin cupo").andExpect(status().isOk());

        String body = mvc.perform(get(BASE + "/requests").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode items = json.readTree(body).get("items");

        List<Long> ids = new ArrayList<>();
        items.forEach(item -> ids.add(item.get("id").asLong()));
        assertThat(ids).contains(pendiente.citaId()).doesNotContain(resuelta.citaId());
        JsonNode item = items.get(ids.indexOf(pendiente.citaId()));
        assertThat(item.get("status").asText()).isEqualTo("REQUESTED");
        assertThat(item.get("patientName").asText()).isEqualTo("Ana Paciente");
        assertThat(item.get("professionalName").asText()).startsWith("Mario Especialista");
        assertThat(item.get("specialtyName").asText()).startsWith("Especialidad ");
        assertThat(item.get("siteCode").asText()).isEqualTo("HIC");
        assertThat(item.get("date").asText()).isEqualTo(MANANA.toString());
        assertThat(item.get("startTime").asText()).isEqualTo("08:00");
    }

    @Test
    void ca05_soloElAdminPuedeVerYResolverSolicitudes() throws Exception {
        Solicitud solicitud = solicitarCitaEspecializada(30, "08:00");

        for (String token : List.of(userToken, solicitud.tokenProfesional())) {
            mvc.perform(get(BASE + "/requests").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isForbidden());
            mvc.perform(post(BASE + "/" + solicitud.citaId() + "/approve")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isForbidden());
            mvc.perform(post(BASE + "/" + solicitud.citaId() + "/reject")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsBytes(Map.of("reason", "No"))))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post(BASE + "/" + solicitud.citaId() + "/approve")).andExpect(status().isUnauthorized());

        assertThat(estado(solicitud.citaId())).isEqualTo("REQUESTED");
    }

    // ---------- Utilidades ----------

    record Profesional(Long id, String token) {
    }

    record Solicitud(Long citaId, Long profesionalId, Long especialidadId, String tokenProfesional) {
    }

    /** Especialidad nueva, profesional con agenda 08:00–10:00 y una cita REQUESTED del USER. */
    private Solicitud solicitarCitaEspecializada(int durationMinutes, String hora) throws Exception {
        long especialidad = crearEspecialidad(durationMinutes);
        Profesional prof = crearProfesional(especialidad);
        crearBloque(prof, "08:00", "10:00");
        long citaId = idDe(reservar(userToken, prof.id(), especialidad, hora)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REQUESTED")));
        return new Solicitud(citaId, prof.id(), especialidad, prof.token());
    }

    private ResultActions rechazar(long citaId, String reason) throws Exception {
        return mvc.perform(post(BASE + "/" + citaId + "/reject")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("reason", reason))));
    }

    private String estado(long citaId) {
        return jdbc.queryForObject("SELECT status_code FROM appointments WHERE id = ?", String.class, citaId);
    }

    private int slotsOcupados(long citaId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM slot_reservations WHERE appointment_id = ?",
                Integer.class, citaId);
    }

    private List<Map<String, Object>> historial(long citaId) {
        return jdbc.queryForList("SELECT status_code, source, actor_user_id, reason "
                + "FROM appointment_status_history WHERE appointment_id = ? ORDER BY id", citaId);
    }

    private ResultActions reservar(String token, Long professionalId, Long specialtyId, String startTime)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("professionalId", professionalId);
        body.put("specialtyId", specialtyId);
        body.put("siteCode", "HIC");
        body.put("date", MANANA.toString());
        body.put("startTime", startTime);
        return mvc.perform(post("/api/v1/appointments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(body)));
    }

    private long idDe(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(body).get("id").asLong();
    }

    private void crearBloque(Profesional prof, String start, String end) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("date", MANANA.toString());
        body.put("startTime", start);
        body.put("endTime", end);
        body.put("siteCode", "HIC");
        mvc.perform(post("/api/v1/professional/availability-blocks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + prof.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(body)))
                .andExpect(status().isCreated());
    }

    private long crearEspecialidad(int durationMinutes) throws Exception {
        int n = SEQUENCE.incrementAndGet();
        return idDe(mvc.perform(post("/api/v1/admin/specialties")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("name", "Especialidad " + n,
                                "durationMinutes", durationMinutes))))
                .andExpect(status().isCreated()));
    }

    private Profesional crearProfesional(long specialtyId) throws Exception {
        int n = SEQUENCE.incrementAndGet();
        String email = "especialista" + n + "@test.local";
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("firstNames", "Mario");
        request.put("lastNames", "Especialista " + n);
        request.put("documentType", "CC");
        request.put("documentNumber", "700" + String.format("%05d", n));
        request.put("email", email);
        request.put("phone", "3001234567");
        request.put("temporaryPassword", PASSWORD);
        request.put("professionalCode", "ESP-" + n);
        request.put("licenseNumber", "ESL-" + n);
        request.put("specialties", List.of(Map.of("specialtyId", specialtyId, "primary", true)));
        request.put("siteCodes", List.of("HIC"));

        long id = idDe(mvc.perform(post("/api/v1/admin/professionals")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(request)))
                .andExpect(status().isCreated()));
        return new Profesional(id, login(email, PASSWORD));
    }

    private String registrarUsuario() throws Exception {
        int n = SEQUENCE.incrementAndGet();
        String email = "solicitante" + n + "@test.local";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstNames", "Ana");
        body.put("lastNames", "Paciente");
        body.put("documentType", "CC");
        body.put("documentNumber", "800" + String.format("%05d", n));
        body.put("email", email);
        body.put("phone", "3001234567");
        body.put("password", PASSWORD);

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(body))).andExpect(status().isCreated());
        return email;
    }

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(body).get("accessToken").asText();
    }
}
