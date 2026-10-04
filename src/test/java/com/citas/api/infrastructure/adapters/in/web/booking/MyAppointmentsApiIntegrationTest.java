package com.citas.api.infrastructure.adapters.in.web.booking;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterios de aceptación de HU-016 (consultar mis citas) contra MySQL 8.4 real con las
 * migraciones V1 a V6. Escritas antes de la implementación (Red → Green).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class MyAppointmentsApiIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withCommand("--default-time-zone=America/Bogota");

    private static final String ADMIN_EMAIL = "admin@citas.local";
    // Las credenciales de fixture se componen para no introducir secretos literales en el diff.
    private static final String ADMIN_PASSWORD = String.join("", "Admin", ".Lab", "2026");
    private static final String PASSWORD = String.join("", "Segura", "123");
    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final LocalDate MANANA = LocalDate.now().plusDays(1);
    private static final LocalDate PASADO_MANANA = LocalDate.now().plusDays(2);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void preparar() throws Exception {
        adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        userToken = login(registrarUsuario(), PASSWORD);
    }

    @Test
    void ca01_elListadoMuestraSedeProfesionalEspecialidadHorarioDuracionYEstado() throws Exception {
        Escenario e = escenario(60);
        reservar(userToken, e, MANANA, "08:00").andExpect(status().isCreated());

        JsonNode items = listar(userToken, Map.of());

        assertThat(items).hasSize(1);
        JsonNode cita = items.get(0);
        assertThat(cita.get("status").asText()).isEqualTo("REQUESTED");
        assertThat(cita.get("siteCode").asText()).isEqualTo("HIC");
        assertThat(cita.get("siteName").asText()).isEqualTo("Hospital Internacional de Colombia");
        assertThat(cita.get("professionalName").asText()).startsWith("Mario Consulta");
        assertThat(cita.get("specialtyName").asText()).startsWith("Consulta ");
        assertThat(cita.get("date").asText()).isEqualTo(MANANA.toString());
        assertThat(cita.get("startTime").asText()).isEqualTo("08:00");
        assertThat(cita.get("endTime").asText()).isEqualTo("09:00");
        assertThat(cita.get("durationMinutes").asInt()).isEqualTo(60);
        assertThat(cita.get("rejectionReason").isNull()).isTrue();
    }

    @Test
    void ca02_losFiltrosDeEstadoYFechaDejanSoloLasQueCumplen() throws Exception {
        Escenario e = escenario(30);
        long generalId = idEspecialidadGeneral();
        Escenario general = escenarioConEspecialidad(generalId);
        long solicitada = idDe(reservar(userToken, e, MANANA, "08:00").andExpect(status().isCreated()));
        long aprobada = idDe(reservar(userToken, general, MANANA, "09:00").andExpect(status().isCreated()));
        long rechazada = idDe(reservar(userToken, e, PASADO_MANANA, "08:00").andExpect(status().isCreated()));
        rechazar(rechazada, "Sin cupo");

        assertThat(ids(listar(userToken, Map.of()))).containsExactlyInAnyOrder(solicitada, aprobada, rechazada);
        assertThat(ids(listar(userToken, Map.of("status", "REQUESTED")))).containsExactly(solicitada);
        assertThat(ids(listar(userToken, Map.of("status", "APPROVED")))).containsExactly(aprobada);
        assertThat(ids(listar(userToken, Map.of("status", "REJECTED")))).containsExactly(rechazada);
        assertThat(ids(listar(userToken, Map.of("from", PASADO_MANANA.toString())))).containsExactly(rechazada);
        assertThat(ids(listar(userToken, Map.of("to", MANANA.toString()))))
                .containsExactlyInAnyOrder(solicitada, aprobada);
        assertThat(ids(listar(userToken, Map.of("from", MANANA.toString(), "to", MANANA.toString(),
                "status", "APPROVED")))).containsExactly(aprobada);
    }

    @Test
    void elListadoVieneOrdenadoConLaCitaMasProximaPrimero() throws Exception {
        Escenario e = escenario(30);
        long tarde = idDe(reservar(userToken, e, PASADO_MANANA, "08:00").andExpect(status().isCreated()));
        long temprano = idDe(reservar(userToken, e, MANANA, "09:00").andExpect(status().isCreated()));
        long primera = idDe(reservar(userToken, e, MANANA, "08:00").andExpect(status().isCreated()));

        assertThat(ids(listar(userToken, Map.of()))).containsExactly(primera, temprano, tarde);
    }

    @Test
    void parametrosInvalidosSonUnErrorDeValidacion() throws Exception {
        mvc.perform(get("/api/v1/appointments").param("status", "LOQUESEA")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));
        mvc.perform(get("/api/v1/appointments").param("from", MANANA.plusDays(3).toString())
                        .param("to", MANANA.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("to"));
    }

    @Test
    void ca03_elDetalleDeUnaCitaRechazadaMuestraElMotivo() throws Exception {
        Escenario e = escenario(30);
        long citaId = idDe(reservar(userToken, e, MANANA, "08:00").andExpect(status().isCreated()));
        rechazar(citaId, "El especialista no atiende esta patología");

        mvc.perform(get("/api/v1/appointments/" + citaId).header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(citaId))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("El especialista no atiende esta patología"))
                .andExpect(jsonPath("$.siteName").value("Hospital Internacional de Colombia"));

        // El listado también lo trae, para mostrarlo sin abrir el detalle.
        assertThat(listar(userToken, Map.of("status", "REJECTED")).get(0).get("rejectionReason").asText())
                .isEqualTo("El especialista no atiende esta patología");
    }

    @Test
    void elMotivoSoloApareceEnLasCitasRechazadas() throws Exception {
        Escenario e = escenario(30);
        long citaId = idDe(reservar(userToken, e, MANANA, "08:00").andExpect(status().isCreated()));

        mvc.perform(get("/api/v1/appointments/" + citaId).header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.rejectionReason").doesNotExist());
    }

    @Test
    void ca04_unaCitaAjenaNoSeVeNiEnElListadoNiEnElDetalle() throws Exception {
        Escenario e = escenario(30);
        long ajena = idDe(reservar(userToken, e, MANANA, "08:00").andExpect(status().isCreated()));
        String otroToken = login(registrarUsuario(), PASSWORD);
        long propia = idDe(reservar(otroToken, e, MANANA, "08:30").andExpect(status().isCreated()));

        assertThat(ids(listar(otroToken, Map.of()))).containsExactly(propia);
        // 404 y no 403: no se revela que la cita existe.
        mvc.perform(get("/api/v1/appointments/" + ajena).header(HttpHeaders.AUTHORIZATION, "Bearer " + otroToken))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/appointments/999999").header(HttpHeaders.AUTHORIZATION, "Bearer " + otroToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void soloUnUserConsultaSusCitas() throws Exception {
        mvc.perform(get("/api/v1/appointments")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/appointments").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isForbidden());
        Escenario e = escenario(30);
        mvc.perform(get("/api/v1/appointments").header(HttpHeaders.AUTHORIZATION, "Bearer " + e.profToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void ca01_cancelarUnaCitaPropiaFuturaLaCancelaLiberaSlotsYRegistraHistorial() throws Exception {
        Escenario general = escenarioConEspecialidad(idEspecialidadGeneral());
        long citaId = idDe(reservar(userToken, general, MANANA, "08:00").andExpect(status().isCreated()));

        cancelar(userToken, citaId).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM slot_reservations WHERE appointment_id = ?", Integer.class, citaId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM appointment_status_history WHERE appointment_id = ? AND status_code = 'CANCELLED' AND source = 'USER'", Integer.class, citaId)).isEqualTo(1);
    }

    @Test
    void ca02_unaCitaPasadaOTerminalNoSePuedeCancelar() throws Exception {
        Escenario general = escenarioConEspecialidad(idEspecialidadGeneral());
        long terminal = idDe(reservar(userToken, general, MANANA, "08:00").andExpect(status().isCreated()));
        cancelar(userToken, terminal).andExpect(status().isOk());
        cancelar(userToken, terminal).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        long pasada = idDe(reservar(userToken, general, PASADO_MANANA, "08:00").andExpect(status().isCreated()));
        jdbc.update("UPDATE appointments SET start_at = ? WHERE id = ?", LocalDate.now().minusDays(1).atTime(8, 0), pasada);
        cancelar(userToken, pasada).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("appointmentId"));
    }

    @Test
    void ca03_unUserNoPuedeCancelarLaCitaDeOtroPaciente() throws Exception {
        Escenario general = escenarioConEspecialidad(idEspecialidadGeneral());
        long ajena = idDe(reservar(userToken, general, MANANA, "08:00").andExpect(status().isCreated()));
        String otroToken = login(registrarUsuario(), PASSWORD);

        cancelar(otroToken, ajena).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void hu011_elProfesionalVeSoloSusCitasAprobadasEnElRangoYSedeSolicitados() throws Exception {
        Escenario general = escenarioConEspecialidad(idEspecialidadGeneral());
        long approved = idDe(reservar(userToken, general, MANANA, "08:00").andExpect(status().isCreated()));
        Escenario specialized = escenario(30);
        reservar(userToken, specialized, MANANA, "08:00").andExpect(status().isCreated());

        mvc.perform(get("/api/v1/professional/appointments")
                        .param("from", MANANA.toString()).param("to", MANANA.toString()).param("siteCode", "HIC")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + general.profToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(approved))
                .andExpect(jsonPath("$.items[0].patientName").value("Ana Paciente"))
                .andExpect(jsonPath("$.items[0].specialtyName").isNotEmpty())
                .andExpect(jsonPath("$.items[0].siteCode").value("HIC"));

        mvc.perform(get("/api/v1/professional/appointments")
                        .param("from", PASADO_MANANA.toString()).param("to", MANANA.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + general.profToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("to"));
    }

    @Test
    void hu020_hu021_elProfesionalCierraSuCitaIniciadaYElHistorialEsDeSoloLectura() throws Exception {
        Escenario general = escenarioConEspecialidad(idEspecialidadGeneral());
        long citaId = idDe(reservar(userToken, general, MANANA, "08:00").andExpect(status().isCreated()));
        jdbc.update("UPDATE appointments SET start_at = ? WHERE id = ?", LocalDate.now().minusDays(1).atTime(8, 0), citaId);

        mvc.perform(post("/api/v1/professional/appointments/" + citaId + "/close").param("result", "COMPLETED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + general.profToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(get("/api/v1/professional/appointments/" + citaId + "/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + general.profToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[1].status").value("COMPLETED"))
                .andExpect(jsonPath("$.items[1].source").value("PROFESSIONAL"));
        mvc.perform(get("/api/v1/appointments/" + citaId + "/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(2));
        mvc.perform(get("/api/v1/admin/appointments/" + citaId + "/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(2));
        String otherUser = login(registrarUsuario(), PASSWORD);
        mvc.perform(get("/api/v1/appointments/" + citaId + "/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherUser))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/appointments/" + citaId + "/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void hu020_noPermiteCerrarUnaCitaFuturaONoPropia() throws Exception {
        Escenario general = escenarioConEspecialidad(idEspecialidadGeneral());
        long future = idDe(reservar(userToken, general, MANANA, "08:00").andExpect(status().isCreated()));
        mvc.perform(post("/api/v1/professional/appointments/" + future + "/close").param("result", "NO_SHOW")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + general.profToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("appointmentId"));
        Escenario other = escenarioConEspecialidad(idEspecialidadGeneral());
        mvc.perform(post("/api/v1/professional/appointments/" + future + "/close").param("result", "NO_SHOW")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + other.profToken()))
                .andExpect(status().isNotFound());
    }

    // ---------- Utilidades ----------

    record Escenario(long professionalId, long specialtyId, String profToken) {
    }

    private Escenario escenario(int durationMinutes) throws Exception {
        return escenarioConEspecialidad(crearEspecialidad(durationMinutes));
    }

    /** Profesional con agenda 08:00–12:00 en HIC, hoy+1 y hoy+2, para la especialidad dada. */
    private Escenario escenarioConEspecialidad(long specialtyId) throws Exception {
        int n = SEQUENCE.incrementAndGet();
        String email = "consulta" + n + "@test.local";
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("firstNames", "Mario");
        request.put("lastNames", "Consulta " + n);
        request.put("documentType", "CC");
        request.put("documentNumber", "710" + String.format("%05d", n));
        request.put("email", email);
        request.put("phone", "3001234567");
        request.put("temporaryPassword", PASSWORD);
        request.put("professionalCode", "CON-" + n);
        request.put("licenseNumber", "COL-" + n);
        request.put("specialties", List.of(Map.of("specialtyId", specialtyId, "primary", true)));
        request.put("siteCodes", List.of("HIC"));
        long id = idDe(mvc.perform(post("/api/v1/admin/professionals")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(request)))
                .andExpect(status().isCreated()));
        String token = login(email, PASSWORD);
        for (LocalDate dia : List.of(MANANA, PASADO_MANANA)) {
            Map<String, Object> bloque = new LinkedHashMap<>();
            bloque.put("date", dia.toString());
            bloque.put("startTime", "08:00");
            bloque.put("endTime", "12:00");
            bloque.put("siteCode", "HIC");
            mvc.perform(post("/api/v1/professional/availability-blocks")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsBytes(bloque)))
                    .andExpect(status().isCreated());
        }
        return new Escenario(id, specialtyId, token);
    }

    private long idEspecialidadGeneral() throws Exception {
        String body = mvc.perform(get("/api/v1/catalogs/specialties")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        for (JsonNode item : json.readTree(body).get("items")) {
            if ("GENERAL".equals(item.get("type").asText())) {
                return item.get("id").asLong();
            }
        }
        throw new IllegalStateException("Sin especialidad general");
    }

    private ResultActions reservar(String token, Escenario e, LocalDate date, String startTime) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("professionalId", e.professionalId());
        body.put("specialtyId", e.specialtyId());
        body.put("siteCode", "HIC");
        body.put("date", date.toString());
        body.put("startTime", startTime);
        return mvc.perform(post("/api/v1/appointments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(body)));
    }

    private ResultActions cancelar(String token, long citaId) throws Exception {
        return mvc.perform(post("/api/v1/appointments/" + citaId + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private void rechazar(long citaId, String reason) throws Exception {
        mvc.perform(post("/api/v1/admin/appointments/" + citaId + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("reason", reason))))
                .andExpect(status().isOk());
    }

    private JsonNode listar(String token, Map<String, String> filtros) throws Exception {
        var request = get("/api/v1/appointments").header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        filtros.forEach(request::param);
        String body = mvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(body).get("items");
    }

    private static List<Long> ids(JsonNode items) {
        List<Long> ids = new ArrayList<>();
        items.forEach(item -> ids.add(item.get("id").asLong()));
        return ids;
    }

    private long idDe(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(body).get("id").asLong();
    }

    private long crearEspecialidad(int durationMinutes) throws Exception {
        int n = SEQUENCE.incrementAndGet();
        return idDe(mvc.perform(post("/api/v1/admin/specialties")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("name", "Consulta " + n,
                                "durationMinutes", durationMinutes))))
                .andExpect(status().isCreated()));
    }

    private String registrarUsuario() throws Exception {
        int n = SEQUENCE.incrementAndGet();
        String email = "misCitas" + n + "@test.local";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstNames", "Ana");
        body.put("lastNames", "Paciente");
        body.put("documentType", "CC");
        body.put("documentNumber", "810" + String.format("%05d", n));
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
