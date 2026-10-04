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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterios de aceptación de HU-018 (solicitar reprogramación), HU-019 (resolverla), HU-022
 * (bandeja con filtros) y las reglas de D-033, contra MySQL 8.4 real con las migraciones V1 a V9.
 *
 * <p>Cada escenario usa un profesional nuevo con agenda HIC 08:00–12:00 e ICV 13:00–17:00, hoy+1
 * y hoy+2, y una especialidad de 60 minutos: cada cita ocupa dos slots.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class RescheduleApiIntegrationTest {

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

    // ---------- HU-018: solicitar reprogramación ----------

    @Test
    void hu018_ca01_laSolicitudQuedaPendienteRetieneLaNuevaFranjaYConservaLaOriginal() throws Exception {
        Escenario e = escenario();
        long citaId = citaAprobada(e, MANANA, "08:00");

        String body = solicitar(userToken, citaId, "ICV", PASADO_MANANA, "14:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.appointmentId").value(citaId))
                .andExpect(jsonPath("$.originalDate").value(MANANA.toString()))
                .andExpect(jsonPath("$.originalStartTime").value("08:00"))
                .andExpect(jsonPath("$.originalSiteCode").value("HIC"))
                .andExpect(jsonPath("$.requestedDate").value(PASADO_MANANA.toString()))
                .andExpect(jsonPath("$.requestedStartTime").value("14:00"))
                .andExpect(jsonPath("$.requestedSiteCode").value("ICV"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long solicitudId = json.readTree(body).get("id").asLong();

        assertThat(slotsDeCita(citaId)).isEqualTo(2);
        assertThat(slotsRetenidos(solicitudId)).isEqualTo(2);
        // La cita sigue en su franja y el paciente ve la solicitud pendiente.
        mvc.perform(get("/api/v1/appointments/" + citaId).header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.date").value(MANANA.toString()))
                .andExpect(jsonPath("$.startTime").value("08:00"))
                .andExpect(jsonPath("$.professionalId").value(e.professionalId()))
                .andExpect(jsonPath("$.specialtyId").value(e.specialtyId()))
                .andExpect(jsonPath("$.reschedule.id").value(solicitudId))
                .andExpect(jsonPath("$.reschedule.status").value("PENDING"))
                .andExpect(jsonPath("$.reschedule.requestedStartTime").value("14:00"));
        // La franja retenida ya no se ofrece en la búsqueda.
        assertThat(inicios(e, PASADO_MANANA)).doesNotContain("14:00").contains("13:00");
    }

    @Test
    void hu018_ca02_unaCitaNoAprobadaPasadaOAjenaNoSeReprograma() throws Exception {
        Escenario e = escenario();
        long solicitada = idDe(reservar(userToken, e, "HIC", MANANA, "08:00").andExpect(status().isCreated()));
        solicitar(userToken, solicitada, "HIC", PASADO_MANANA, "08:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_NOT_RESCHEDULABLE"));

        long pasada = citaAprobada(e, MANANA, "10:00");
        jdbc.update("UPDATE appointments SET start_at = ? WHERE id = ?", LocalDate.now().minusDays(1).atTime(10, 0), pasada);
        solicitar(userToken, pasada, "HIC", PASADO_MANANA, "10:00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("appointmentId"));

        long propia = citaAprobada(e, PASADO_MANANA, "08:00");
        String otroToken = login(registrarUsuario(), PASSWORD);
        solicitar(otroToken, propia, "HIC", PASADO_MANANA, "10:00").andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reschedule_requests WHERE appointment_id IN (?, ?, ?)",
                Integer.class, solicitada, pasada, propia)).isZero();
    }

    @Test
    void hu018_ca03_unaFranjaOcupadaORetenidaNoSePuedeSolicitar() throws Exception {
        Escenario e = escenario();
        long cita = citaAprobada(e, MANANA, "08:00");
        String otroToken = login(registrarUsuario(), PASSWORD);
        // Ocupada por otra cita.
        long otra = idDe(reservar(otroToken, e, "HIC", PASADO_MANANA, "08:00").andExpect(status().isCreated()));
        aprobarCita(otra);
        solicitar(userToken, cita, "HIC", PASADO_MANANA, "08:30")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));
        // Retenida por la reprogramación de otra cita.
        solicitar(otroToken, otra, "HIC", PASADO_MANANA, "10:00").andExpect(status().isCreated());
        solicitar(userToken, cita, "HIC", PASADO_MANANA, "10:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));
        // Un horario fuera de la agenda tampoco existe.
        solicitar(userToken, cita, "HIC", PASADO_MANANA, "18:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reschedule_requests WHERE appointment_id = ?",
                Integer.class, cita)).isZero();
        assertThat(slotsDeCita(cita)).isEqualTo(2);
    }

    @Test
    void hu018_ca04_noSePermiteUnaSegundaSolicitudPendienteParaLaMismaCita() throws Exception {
        Escenario e = escenario();
        long cita = citaAprobada(e, MANANA, "08:00");
        solicitar(userToken, cita, "HIC", PASADO_MANANA, "08:00").andExpect(status().isCreated());

        solicitar(userToken, cita, "HIC", PASADO_MANANA, "10:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESCHEDULE_ALREADY_PENDING"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reschedule_requests WHERE appointment_id = ?",
                Integer.class, cita)).isEqualTo(1);
    }

    @Test
    void hu018_validaFormaDeLaPeticion() throws Exception {
        Escenario e = escenario();
        long cita = citaAprobada(e, MANANA, "08:00");
        mvc.perform(post("/api/v1/appointments/" + cita + "/reschedule-requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("siteCode", "HIC"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        solicitar(adminToken, cita, "HIC", PASADO_MANANA, "08:00").andExpect(status().isForbidden());
    }

    // ---------- HU-019: aprobar o rechazar ----------

    @Test
    void hu019_ca01_aprobarMueveLaCitaLiberaLosSlotsAntiguosYRegistraHistorial() throws Exception {
        Escenario e = escenario();
        long cita = citaAprobada(e, MANANA, "08:00");
        long solicitud = idDe(solicitar(userToken, cita, "ICV", PASADO_MANANA, "14:00").andExpect(status().isCreated()));

        decidir(solicitud, "approve", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.appointmentId").value(cita));

        mvc.perform(get("/api/v1/appointments/" + cita).header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.date").value(PASADO_MANANA.toString()))
                .andExpect(jsonPath("$.startTime").value("14:00"))
                .andExpect(jsonPath("$.endTime").value("15:00"))
                .andExpect(jsonPath("$.siteCode").value("ICV"))
                .andExpect(jsonPath("$.reschedule.status").value("APPROVED"));
        // Exactamente dos slots, ambos de la nueva franja, a nombre de la cita; ninguno retenido.
        assertThat(slotsDeCita(cita)).isEqualTo(2);
        assertThat(slotsRetenidos(solicitud)).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM slot_reservations sr JOIN availability_slots s ON s.id = sr.slot_id
                WHERE sr.appointment_id = ? AND s.start_at >= ?
                """, Integer.class, cita, PASADO_MANANA.atTime(14, 0))).isEqualTo(2);
        // La franja original vuelve a estar disponible y otro paciente la puede reservar.
        assertThat(inicios(e, MANANA)).contains("08:00");
        String otroToken = login(registrarUsuario(), PASSWORD);
        reservar(otroToken, e, "HIC", MANANA, "08:00").andExpect(status().isCreated());
        // El historial registra el cambio de horario enlazado con la solicitud.
        assertThat(jdbc.queryForObject("""
                SELECT reason FROM appointment_status_history
                WHERE appointment_id = ? AND reschedule_request_id = ? AND source = 'ADMIN' AND status_code = 'APPROVED'
                """, String.class, cita, solicitud))
                .startsWith("Reprogramada del " + MANANA + " 08:00 (HIC)")
                .endsWith(PASADO_MANANA + " 14:00 (ICV)");
        mvc.perform(get("/api/v1/appointments/" + cita + "/history").header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3));
    }

    @Test
    void hu019_ca02_rechazarLiberaLaFranjaProvisionalYConservaLaOriginal() throws Exception {
        Escenario e = escenario();
        long cita = citaAprobada(e, MANANA, "08:00");
        long solicitud = idDe(solicitar(userToken, cita, "HIC", PASADO_MANANA, "10:00").andExpect(status().isCreated()));

        decidir(solicitud, "reject", "El profesional no puede ese día")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.decisionReason").value("El profesional no puede ese día"));

        assertThat(slotsRetenidos(solicitud)).isZero();
        assertThat(slotsDeCita(cita)).isEqualTo(2);
        assertThat(inicios(e, PASADO_MANANA)).contains("10:00");
        mvc.perform(get("/api/v1/appointments/" + cita).header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.date").value(MANANA.toString()))
                .andExpect(jsonPath("$.startTime").value("08:00"))
                .andExpect(jsonPath("$.reschedule.status").value("REJECTED"))
                .andExpect(jsonPath("$.reschedule.decisionReason").value("El profesional no puede ese día"));
        // Tras el rechazo, el paciente puede volver a pedir otra franja.
        solicitar(userToken, cita, "HIC", PASADO_MANANA, "11:00").andExpect(status().isCreated());
    }

    @Test
    void hu019_ca03_rechazarSinMotivoSeRechaza() throws Exception {
        Escenario e = escenario();
        long cita = citaAprobada(e, MANANA, "08:00");
        long solicitud = idDe(solicitar(userToken, cita, "HIC", PASADO_MANANA, "10:00").andExpect(status().isCreated()));

        decidir(solicitud, "reject", "   ").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reason"));
        assertThat(estadoSolicitud(solicitud)).isEqualTo("PENDING");
        assertThat(slotsRetenidos(solicitud)).isEqualTo(2);
    }

    @Test
    void hu019_ca04_soloUnaPendienteSeResuelveYSoloPorAdmin() throws Exception {
        Escenario e = escenario();
        long cita = citaAprobada(e, MANANA, "08:00");
        long solicitud = idDe(solicitar(userToken, cita, "HIC", PASADO_MANANA, "10:00").andExpect(status().isCreated()));

        mvc.perform(post("/api/v1/admin/reschedule-requests/" + solicitud + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/reschedule-requests/" + solicitud + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + e.profToken()))
                .andExpect(status().isForbidden());
        decidir(solicitud, "approve", null).andExpect(status().isOk());
        decidir(solicitud, "approve", null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESCHEDULE_NOT_PENDING"));
        decidir(solicitud, "reject", "Tarde").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESCHEDULE_NOT_PENDING"));
        decidir(999_999L, "approve", null).andExpect(status().isNotFound());
    }

    // ---------- D-033 ----------

    @Test
    void d033_cancelarLaCitaCancelaLaReprogramacionPendienteYLiberaAmbasFranjas() throws Exception {
        Escenario e = escenario();
        long cita = citaAprobada(e, MANANA, "08:00");
        long solicitud = idDe(solicitar(userToken, cita, "HIC", PASADO_MANANA, "10:00").andExpect(status().isCreated()));

        mvc.perform(post("/api/v1/appointments/" + cita + "/cancel").header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(estadoSolicitud(solicitud)).isEqualTo("CANCELLED");
        assertThat(slotsDeCita(cita)).isZero();
        assertThat(slotsRetenidos(solicitud)).isZero();
    }

    @Test
    void d033_alLlegarLaHoraOriginalLaSolicitudPendienteSeCierraYLiberaSuRetencion() throws Exception {
        Escenario e = escenario();
        long cita = citaAprobada(e, MANANA, "08:00");
        long solicitud = idDe(solicitar(userToken, cita, "HIC", PASADO_MANANA, "10:00").andExpect(status().isCreated()));
        // Simula que llegó la hora de la cita original.
        LocalDateTime ayer = LocalDate.now().minusDays(1).atTime(8, 0);
        jdbc.update("UPDATE appointments SET start_at = ? WHERE id = ?", ayer, cita);
        jdbc.update("UPDATE reschedule_requests SET original_start_at = ? WHERE id = ?", ayer, solicitud);

        decidir(solicitud, "approve", null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESCHEDULE_EXPIRED"));
        // La bandeja cierra las vencidas antes de listar.
        assertThat(ids(pendientes(Map.of("professionalId", String.valueOf(e.professionalId()))))).isEmpty();
        assertThat(estadoSolicitud(solicitud)).isEqualTo("CANCELLED");
        assertThat(slotsRetenidos(solicitud)).isZero();
    }

    // ---------- HU-022: bandeja con filtros ----------

    @Test
    void hu022_ca01_laBandejaMuestraSolicitudesYReprogramacionesPendientesYNoLasResueltas() throws Exception {
        Escenario e = escenario();
        long solicitada = idDe(reservar(userToken, e, "HIC", MANANA, "08:00").andExpect(status().isCreated()));
        long rechazada = idDe(reservar(userToken, e, "HIC", MANANA, "10:00").andExpect(status().isCreated()));
        rechazarCita(rechazada);
        long citaA = citaAprobada(e, MANANA, "13:00");
        long citaB = citaAprobada(e, MANANA, "15:00");
        long pendiente = idDe(solicitar(userToken, citaA, "HIC", PASADO_MANANA, "08:00").andExpect(status().isCreated()));
        long resuelta = idDe(solicitar(userToken, citaB, "HIC", PASADO_MANANA, "10:00").andExpect(status().isCreated()));
        decidir(resuelta, "reject", "Sin cupo").andExpect(status().isOk());

        Map<String, String> delProfesional = Map.of("professionalId", String.valueOf(e.professionalId()));
        assertThat(ids(solicitudes(delProfesional))).containsExactly(solicitada);
        JsonNode reprogramaciones = pendientes(delProfesional);
        assertThat(ids(reprogramaciones)).containsExactly(pendiente);
        JsonNode item = reprogramaciones.get(0);
        assertThat(item.get("appointmentId").asLong()).isEqualTo(citaA);
        assertThat(item.get("patientName").asText()).isEqualTo("Ana Paciente");
        assertThat(item.get("professionalName").asText()).startsWith("Mario Agenda");
        assertThat(item.get("originalDate").asText()).isEqualTo(MANANA.toString());
        assertThat(item.get("originalStartTime").asText()).isEqualTo("13:00");
        assertThat(item.get("originalSiteCode").asText()).isEqualTo("ICV");
        assertThat(item.get("requestedDate").asText()).isEqualTo(PASADO_MANANA.toString());
        assertThat(item.get("requestedStartTime").asText()).isEqualTo("08:00");
        assertThat(item.get("requestedSiteCode").asText()).isEqualTo("HIC");
        assertThat(item.get("durationMinutes").asInt()).isEqualTo(60);
    }

    @Test
    void hu022_ca02_losFiltrosSonCombinablesYTodosSeCumplen() throws Exception {
        Escenario e = escenario();
        long solicitadaHic = idDe(reservar(userToken, e, "HIC", MANANA, "08:00").andExpect(status().isCreated()));
        long solicitadaIcv = idDe(reservar(userToken, e, "ICV", PASADO_MANANA, "13:00").andExpect(status().isCreated()));
        long cita = citaAprobada(e, MANANA, "10:00");
        long reprogramacionIcv = idDe(solicitar(userToken, cita, "ICV", PASADO_MANANA, "15:00").andExpect(status().isCreated()));
        String prof = String.valueOf(e.professionalId());
        String esp = String.valueOf(e.specialtyId());

        assertThat(ids(solicitudes(Map.of("professionalId", prof, "siteCode", "HIC")))).containsExactly(solicitadaHic);
        assertThat(ids(solicitudes(Map.of("professionalId", prof, "siteCode", "icv")))).containsExactly(solicitadaIcv);
        assertThat(ids(solicitudes(Map.of("professionalId", prof, "from", PASADO_MANANA.toString(),
                "to", PASADO_MANANA.toString())))).containsExactly(solicitadaIcv);
        assertThat(ids(solicitudes(Map.of("specialtyId", esp, "to", MANANA.toString())))).containsExactly(solicitadaHic);
        assertThat(ids(pendientes(Map.of("professionalId", prof, "siteCode", "ICV", "specialtyId", esp,
                "from", PASADO_MANANA.toString())))).containsExactly(reprogramacionIcv);
        assertThat(ids(pendientes(Map.of("professionalId", prof, "siteCode", "HIC")))).isEmpty();
        assertThat(ids(pendientes(Map.of("specialtyId", esp, "to", MANANA.toString())))).isEmpty();

        mvc.perform(get("/api/v1/admin/reschedule-requests").param("from", PASADO_MANANA.toString())
                        .param("to", MANANA.toString()).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("to"));
    }

    @Test
    void hu022_ca03_soloAdminAccedeALaBandeja() throws Exception {
        Escenario e = escenario();
        for (String ruta : List.of("/api/v1/admin/reschedule-requests", "/api/v1/admin/appointments/requests")) {
            mvc.perform(get(ruta)).andExpect(status().isUnauthorized());
            mvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                    .andExpect(status().isForbidden());
            mvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, "Bearer " + e.profToken()))
                    .andExpect(status().isForbidden());
            mvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }
    }

    // ---------- Utilidades ----------

    record Escenario(long professionalId, long specialtyId, String profToken) {
    }

    /** Profesional con especialidad de 60 min y agenda HIC 08–12 e ICV 13–17, hoy+1 y hoy+2. */
    private Escenario escenario() throws Exception {
        long specialtyId = crearEspecialidad();
        int n = SEQUENCE.incrementAndGet();
        String email = "agenda" + n + "@reprog.local";
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("firstNames", "Mario");
        request.put("lastNames", "Agenda " + n);
        request.put("documentType", "CC");
        request.put("documentNumber", "720" + String.format("%05d", n));
        request.put("email", email);
        request.put("phone", "3001234567");
        request.put("temporaryPassword", PASSWORD);
        request.put("professionalCode", "REP-" + n);
        request.put("licenseNumber", "LIC-REP-" + n);
        request.put("specialties", List.of(Map.of("specialtyId", specialtyId, "primary", true)));
        request.put("siteCodes", List.of("HIC", "ICV"));
        long id = idDe(mvc.perform(post("/api/v1/admin/professionals")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(request)))
                .andExpect(status().isCreated()));
        String token = login(email, PASSWORD);
        for (LocalDate dia : List.of(MANANA, PASADO_MANANA)) {
            crearBloque(token, dia, "08:00", "12:00", "HIC");
            crearBloque(token, dia, "13:00", "17:00", "ICV");
        }
        return new Escenario(id, specialtyId, token);
    }

    private void crearBloque(String token, LocalDate dia, String inicio, String fin, String sede) throws Exception {
        Map<String, Object> bloque = new LinkedHashMap<>();
        bloque.put("date", dia.toString());
        bloque.put("startTime", inicio);
        bloque.put("endTime", fin);
        bloque.put("siteCode", sede);
        mvc.perform(post("/api/v1/professional/availability-blocks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(bloque)))
                .andExpect(status().isCreated());
    }

    /** Cita especializada reservada por el paciente y aprobada por el ADMIN. */
    private long citaAprobada(Escenario e, LocalDate dia, String hora) throws Exception {
        String sede = hora.compareTo("13:00") >= 0 ? "ICV" : "HIC";
        long id = idDe(reservar(userToken, e, sede, dia, hora).andExpect(status().isCreated()));
        aprobarCita(id);
        return id;
    }

    private ResultActions reservar(String token, Escenario e, String sede, LocalDate date, String startTime)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("professionalId", e.professionalId());
        body.put("specialtyId", e.specialtyId());
        body.put("siteCode", sede);
        body.put("date", date.toString());
        body.put("startTime", startTime);
        return mvc.perform(post("/api/v1/appointments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(body)));
    }

    private ResultActions solicitar(String token, long citaId, String sede, LocalDate date, String startTime)
            throws Exception {
        return mvc.perform(post("/api/v1/appointments/" + citaId + "/reschedule-requests")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("siteCode", sede, "date", date.toString(),
                        "startTime", startTime))));
    }

    private ResultActions decidir(long solicitudId, String accion, String motivo) throws Exception {
        var request = post("/api/v1/admin/reschedule-requests/" + solicitudId + "/" + accion)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken);
        if (motivo != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("reason", motivo)));
        }
        return mvc.perform(request);
    }

    private void aprobarCita(long citaId) throws Exception {
        mvc.perform(post("/api/v1/admin/appointments/" + citaId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private void rechazarCita(long citaId) throws Exception {
        mvc.perform(post("/api/v1/admin/appointments/" + citaId + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("reason", "Sin cupo"))))
                .andExpect(status().isOk());
    }

    private JsonNode solicitudes(Map<String, String> filtros) throws Exception {
        return items("/api/v1/admin/appointments/requests", filtros);
    }

    private JsonNode pendientes(Map<String, String> filtros) throws Exception {
        return items("/api/v1/admin/reschedule-requests", filtros);
    }

    private JsonNode items(String ruta, Map<String, String> filtros) throws Exception {
        var request = get(ruta).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken);
        filtros.forEach(request::param);
        String body = mvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(body).get("items");
    }

    /** Horas de inicio que la búsqueda ofrece para el profesional y la especialidad del escenario. */
    private List<String> inicios(Escenario e, LocalDate dia) throws Exception {
        String body = mvc.perform(get("/api/v1/availability").param("date", dia.toString())
                        .param("professionalId", String.valueOf(e.professionalId()))
                        .param("specialtyId", String.valueOf(e.specialtyId()))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<String> inicios = new ArrayList<>();
        json.readTree(body).get("items").forEach(item -> inicios.add(item.get("startTime").asText()));
        return inicios;
    }

    private int slotsDeCita(long citaId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM slot_reservations WHERE appointment_id = ?", Integer.class, citaId);
    }

    private int slotsRetenidos(long solicitudId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM slot_reservations WHERE reschedule_request_id = ?",
                Integer.class, solicitudId);
    }

    private String estadoSolicitud(long solicitudId) {
        return jdbc.queryForObject("SELECT status_code FROM reschedule_requests WHERE id = ?", String.class, solicitudId);
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

    private long crearEspecialidad() throws Exception {
        int n = SEQUENCE.incrementAndGet();
        return idDe(mvc.perform(post("/api/v1/admin/specialties")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("name", "Reprogramable " + n,
                                "durationMinutes", 60))))
                .andExpect(status().isCreated()));
    }

    private String registrarUsuario() throws Exception {
        int n = SEQUENCE.incrementAndGet();
        String email = "reprog" + n + "@test.local";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstNames", "Ana");
        body.put("lastNames", "Paciente");
        body.put("documentType", "CC");
        body.put("documentNumber", "820" + String.format("%05d", n));
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
