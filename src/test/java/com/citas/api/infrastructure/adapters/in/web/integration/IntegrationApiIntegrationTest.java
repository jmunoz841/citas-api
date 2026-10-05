package com.citas.api.infrastructure.adapters.in.web.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterios de aceptación de HU-023 (recordatorios), HU-024 (webhook de cambio de estado) y HU-025
 * (resumen diario) contra MySQL 8.4 real con las migraciones V1 a V10. Un servidor HTTP embebido hace
 * de webhook de n8n: guarda cada evento recibido y responde con el código que pida la prueba.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class IntegrationApiIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withCommand("--default-time-zone=America/Bogota");

    // Valores ficticios de prueba, compuestos para no dejar patrones de secreto literales.
    private static final String API_KEY = String.join("-", "test", "integration", "key", "0123456789abcdefghij");
    private static final String WEBHOOK_SECRET = String.join("-", "test", "webhook", "secret", "0123456789abcdefghij");
    private static final String ADMIN_EMAIL = "admin@citas.local";
    private static final String ADMIN_PASSWORD = String.join("", "Admin", ".Lab", "2026");
    private static final String PASSWORD = String.join("", "Segura", "123");
    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final LocalDate MANANA = LocalDate.now().plusDays(1);
    private static final LocalDate PASADO_MANANA = LocalDate.now().plusDays(2);
    private static final LocalDate EN_TRES_DIAS = LocalDate.now().plusDays(3);
    private static final LocalDate EN_CINCO_DIAS = LocalDate.now().plusDays(5);

    private static final LinkedBlockingQueue<Received> WEBHOOK = new LinkedBlockingQueue<>();
    private static final AtomicInteger WEBHOOK_STATUS = new AtomicInteger(200);
    private static final HttpServer N8N = startFakeN8n();

    record Received(Map<String, String> headers, byte[] body) {
    }

    @DynamicPropertySource
    static void integration(DynamicPropertyRegistry registry) {
        registry.add("app.integration.api-key", () -> API_KEY);
        registry.add("app.integration.webhook-secret", () -> WEBHOOK_SECRET);
        registry.add("app.integration.webhook-url",
                () -> "http://localhost:" + N8N.getAddress().getPort() + "/webhook/citas-status");
    }

    @AfterAll
    static void stopFakeN8n() {
        N8N.stop(0);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    private String adminToken;
    private String userToken;
    private String userEmail;

    @BeforeEach
    void preparar() throws Exception {
        WEBHOOK_STATUS.set(200);
        adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        userEmail = registrarUsuario();
        userToken = login(userEmail, PASSWORD);
    }

    // ---------- Credencial de servicio (D-042) ----------

    @Test
    void soloLaCredencialDeServicioAccedeYNoSirveFueraDeIntegraciones() throws Exception {
        mvc.perform(get("/api/v1/integrations/reminders")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/integrations/reminders").header("X-Api-Key", "clave-equivocada"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/integrations/reminders").header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/integrations/daily-summary").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/integrations/reminders").header("X-Api-Key", API_KEY)).andExpect(status().isOk());
        // La clave no abre el resto de la API (privilegio mínimo).
        mvc.perform(get("/api/v1/admin/appointments/requests").header("X-Api-Key", API_KEY))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/appointments").header("X-Api-Key", API_KEY)).andExpect(status().isUnauthorized());
    }

    // ---------- HU-023: recordatorios ----------

    @Test
    void hu023_ca01_unaCitaAprobadaDentroDeLaVentanaSeOfreceConLosDatosDelCorreo() throws Exception {
        Escenario e = escenario(MANANA);
        long cita = citaAprobada(e, MANANA, "08:00");

        JsonNode item = recordatorio(36, cita);

        assertThat(item).as("la cita aprobada de mañana está en la ventana de 36 h").isNotNull();
        assertThat(item.get("patientFirstNames").asText()).isEqualTo("Ana");
        assertThat(item.get("patientEmail").asText()).isEqualTo(userEmail);
        assertThat(item.get("professionalName").asText()).startsWith("Mario Integra");
        assertThat(item.get("specialtyName").asText()).startsWith("Integrable");
        assertThat(item.get("siteCode").asText()).isEqualTo("HIC");
        assertThat(item.get("siteName").asText()).isEqualTo("Hospital Internacional de Colombia");
        assertThat(item.get("siteAddress").asText()).isNotBlank();
        assertThat(item.get("date").asText()).isEqualTo(MANANA.toString());
        assertThat(item.get("startTime").asText()).isEqualTo("08:00");
        assertThat(item.get("endTime").asText()).isEqualTo("09:00");
    }

    @Test
    void hu023_ca02_seExcluyenOtrosEstadosLasCitasFueraDeVentanaYLasYaRecordadas() throws Exception {
        Escenario e = escenario(MANANA, EN_TRES_DIAS);
        long solicitada = idDe(reservar(userToken, e, "HIC", MANANA, "09:00").andExpect(status().isCreated()));
        long cancelada = citaAprobada(e, MANANA, "10:00");
        mvc.perform(post("/api/v1/appointments/" + cancelada + "/cancel").header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk());
        long lejana = citaAprobada(e, EN_TRES_DIAS, "08:00");
        long recordada = citaAprobada(e, MANANA, "11:00");

        assertThat(recordatorio(36, recordada)).isNotNull();
        marcarEnviado(recordada).andExpect(status().isOk()).andExpect(jsonPath("$.alreadySent").value(false));
        // Repetir el registro no duplica nada (WF-001 puede reintentar).
        marcarEnviado(recordada).andExpect(status().isOk()).andExpect(jsonPath("$.alreadySent").value(true));

        List<Long> ids = ids(recordatorios(36));
        assertThat(ids).doesNotContain(solicitada, cancelada, lejana, recordada);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM appointment_reminders WHERE appointment_id = ?",
                Integer.class, recordada)).isEqualTo(1);
    }

    @Test
    void hu023_soloSeMarcanCitasAprobadasYLaVentanaSeValida() throws Exception {
        Escenario e = escenario(MANANA);
        long solicitada = idDe(reservar(userToken, e, "HIC", MANANA, "08:00").andExpect(status().isCreated()));

        marcarEnviado(solicitada).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REMINDER_NOT_APPLICABLE"));
        marcarEnviado(999_999L).andExpect(status().isNotFound());
        for (String hours : List.of("0", "73")) {
            mvc.perform(get("/api/v1/integrations/reminders").param("hours", hours).header("X-Api-Key", API_KEY))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("hours"));
        }
    }

    @Test
    void hu023_unaCitaReprogramadaVuelveAOfrecerseEnSuNuevoHorario() throws Exception {
        Escenario e = escenario(MANANA, PASADO_MANANA);
        long cita = citaAprobada(e, MANANA, "08:00");
        marcarEnviado(cita).andExpect(status().isOk());
        assertThat(recordatorio(72, cita)).isNull();

        long solicitud = idDe(mvc.perform(post("/api/v1/appointments/" + cita + "/reschedule-requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("siteCode", "HIC", "date", PASADO_MANANA.toString(),
                                "startTime", "08:00"))))
                .andExpect(status().isCreated()));
        mvc.perform(post("/api/v1/admin/reschedule-requests/" + solicitud + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        JsonNode item = recordatorio(72, cita);
        assertThat(item).isNotNull();
        assertThat(item.get("date").asText()).isEqualTo(PASADO_MANANA.toString());
    }

    // ---------- HU-024: webhook de cambio de estado ----------

    @Test
    void hu024_ca01_aprobarYRechazarUnaCitaEspecializadaEnviaEventosFirmados() throws Exception {
        Escenario e = escenario(MANANA);
        long aprobada = idDe(reservar(userToken, e, "HIC", MANANA, "08:00").andExpect(status().isCreated()));
        long rechazada = idDe(reservar(userToken, e, "HIC", MANANA, "10:00").andExpect(status().isCreated()));

        aprobarCita(aprobada);
        JsonNode evento = esperarEvento("SPECIALIZED_APPROVED", aprobada);
        assertThat(evento.get("appointment").get("status").asText()).isEqualTo("APPROVED");
        assertThat(evento.get("appointment").get("siteName").asText()).isEqualTo("Hospital Internacional de Colombia");
        assertThat(evento.get("appointment").get("date").asText()).isEqualTo(MANANA.toString());
        assertThat(evento.get("appointment").get("startTime").asText()).isEqualTo("08:00");
        assertThat(evento.get("patient").get("email").asText()).isEqualTo(userEmail);
        assertThat(evento.get("patient").get("firstNames").asText()).isEqualTo("Ana");
        assertThat(evento.get("reason").isNull()).isTrue();
        assertThat(evento.get("eventId").asText()).isNotBlank();

        mvc.perform(post("/api/v1/admin/appointments/" + rechazada + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("reason", "Sin cupo para esa patología"))))
                .andExpect(status().isOk());
        JsonNode rechazo = esperarEvento("SPECIALIZED_REJECTED", rechazada);
        assertThat(rechazo.get("appointment").get("status").asText()).isEqualTo("REJECTED");
        assertThat(rechazo.get("reason").asText()).isEqualTo("Sin cupo para esa patología");
    }

    @Test
    void hu024_ca01_reprogramacionYCancelacionTambienSeNotifican() throws Exception {
        Escenario e = escenario(MANANA, PASADO_MANANA);
        long cita = citaAprobada(e, MANANA, "08:00");
        long aprobada = solicitarReprogramacion(cita, PASADO_MANANA, "09:00");
        mvc.perform(post("/api/v1/admin/reschedule-requests/" + aprobada + "/approve")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isOk());
        JsonNode movida = esperarEvento("RESCHEDULE_APPROVED", cita);
        assertThat(movida.get("appointment").get("date").asText()).isEqualTo(PASADO_MANANA.toString());
        assertThat(movida.get("appointment").get("startTime").asText()).isEqualTo("09:00");
        assertThat(movida.get("previous").get("date").asText()).isEqualTo(MANANA.toString());
        assertThat(movida.get("previous").get("startTime").asText()).isEqualTo("08:00");

        long rechazada = solicitarReprogramacion(cita, PASADO_MANANA, "11:00");
        mvc.perform(post("/api/v1/admin/reschedule-requests/" + rechazada + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("reason", "El profesional no puede"))))
                .andExpect(status().isOk());
        JsonNode rechazo = esperarEvento("RESCHEDULE_REJECTED", cita);
        assertThat(rechazo.get("reason").asText()).isEqualTo("El profesional no puede");
        assertThat(rechazo.get("requested").get("startTime").asText()).isEqualTo("11:00");
        assertThat(rechazo.get("appointment").get("startTime").asText()).isEqualTo("09:00");

        mvc.perform(post("/api/v1/appointments/" + cita + "/cancel").header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk());
        JsonNode cancelada = esperarEvento("APPOINTMENT_CANCELLED", cita);
        assertThat(cancelada.get("appointment").get("status").asText()).isEqualTo("CANCELLED");
    }

    @Test
    void hu024_ca02_siN8nFallaLaTransicionSeCompletaIgual() throws Exception {
        Escenario e = escenario(MANANA);
        long cita = idDe(reservar(userToken, e, "HIC", MANANA, "08:00").andExpect(status().isCreated()));
        WEBHOOK_STATUS.set(503);

        aprobarCita(cita);

        assertThat(jdbc.queryForObject("SELECT status_code FROM appointments WHERE id = ?", String.class, cita))
                .isEqualTo("APPROVED");
        // El intento sí ocurrió: n8n respondió 503 y la API solo lo registró.
        assertThat(esperarEvento("SPECIALIZED_APPROVED", cita)).isNotNull();
    }

    // ---------- HU-025: resumen diario ----------

    @Test
    void hu025_ca01_ca02_elResumenCuentaPorSedeEstadoYEspecialidadSinDatosPersonales() throws Exception {
        Escenario e = escenario(EN_CINCO_DIAS);
        citaAprobada(e, EN_CINCO_DIAS, "08:00");
        reservar(userToken, e, "HIC", EN_CINCO_DIAS, "10:00").andExpect(status().isCreated());
        long cancelada = citaAprobada(e, EN_CINCO_DIAS, "14:00");
        mvc.perform(post("/api/v1/appointments/" + cancelada + "/cancel").header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk());

        String body = mvc.perform(get("/api/v1/integrations/daily-summary").param("date", EN_CINCO_DIAS.toString())
                        .header("X-Api-Key", API_KEY))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode resumen = json.readTree(body);

        assertThat(resumen.get("date").asText()).isEqualTo(EN_CINCO_DIAS.toString());
        assertThat(resumen.get("total").asLong()).isEqualTo(3);
        assertThat(resumen.get("byStatus").get("APPROVED").asLong()).isEqualTo(1);
        assertThat(resumen.get("byStatus").get("REQUESTED").asLong()).isEqualTo(1);
        assertThat(resumen.get("byStatus").get("CANCELLED").asLong()).isEqualTo(1);
        JsonNode hic = sede(resumen, "HIC");
        JsonNode icv = sede(resumen, "ICV");
        assertThat(hic.get("total").asLong()).isEqualTo(2);
        assertThat(icv.get("total").asLong()).isEqualTo(1);
        assertThat(icv.get("byStatus").get("CANCELLED").asLong()).isEqualTo(1);
        assertThat(resumen.get("specialties").get(0).get("total").asLong()).isEqualTo(3);
        assertThat(resumen.get("pendingRequests").asLong()).isGreaterThanOrEqualTo(1);
        // CA-02: ni emails ni nombres de pacientes.
        assertThat(body).doesNotContain("@").doesNotContain("Ana").doesNotContain("Paciente");
    }

    // ---------- Utilidades ----------

    record Escenario(long professionalId, long specialtyId) {
    }

    /** Profesional con especialidad de 60 min y agenda HIC 08–12 e ICV 13–17 en los días dados. */
    private Escenario escenario(LocalDate... dias) throws Exception {
        int n = SEQUENCE.incrementAndGet();
        long specialtyId = idDe(mvc.perform(post("/api/v1/admin/specialties")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("name", "Integrable " + n, "durationMinutes", 60))))
                .andExpect(status().isCreated()));
        String email = "integra" + n + "@n8n.local";
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("firstNames", "Mario");
        request.put("lastNames", "Integra " + n);
        request.put("documentType", "CC");
        request.put("documentNumber", "730" + String.format("%05d", n));
        request.put("email", email);
        request.put("phone", "3001234567");
        request.put("temporaryPassword", PASSWORD);
        request.put("professionalCode", "INT-" + n);
        request.put("licenseNumber", "LIC-INT-" + n);
        request.put("specialties", List.of(Map.of("specialtyId", specialtyId, "primary", true)));
        request.put("siteCodes", List.of("HIC", "ICV"));
        long id = idDe(mvc.perform(post("/api/v1/admin/professionals")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(request)))
                .andExpect(status().isCreated()));
        String token = login(email, PASSWORD);
        for (LocalDate dia : dias) {
            crearBloque(token, dia, "08:00", "12:00", "HIC");
            crearBloque(token, dia, "13:00", "17:00", "ICV");
        }
        return new Escenario(id, specialtyId);
    }

    private void crearBloque(String token, LocalDate dia, String inicio, String fin, String sede) throws Exception {
        mvc.perform(post("/api/v1/professional/availability-blocks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("date", dia.toString(), "startTime", inicio,
                                "endTime", fin, "siteCode", sede))))
                .andExpect(status().isCreated());
    }

    private long citaAprobada(Escenario e, LocalDate dia, String hora) throws Exception {
        String sede = hora.compareTo("13:00") >= 0 ? "ICV" : "HIC";
        long id = idDe(reservar(userToken, e, sede, dia, hora).andExpect(status().isCreated()));
        aprobarCita(id);
        return id;
    }

    private void aprobarCita(long citaId) throws Exception {
        mvc.perform(post("/api/v1/admin/appointments/" + citaId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private ResultActions reservar(String token, Escenario e, String sede, LocalDate date, String startTime)
            throws Exception {
        return mvc.perform(post("/api/v1/appointments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("professionalId", e.professionalId(),
                        "specialtyId", e.specialtyId(), "siteCode", sede, "date", date.toString(),
                        "startTime", startTime))));
    }

    private long solicitarReprogramacion(long cita, LocalDate dia, String hora) throws Exception {
        return idDe(mvc.perform(post("/api/v1/appointments/" + cita + "/reschedule-requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("siteCode", "HIC", "date", dia.toString(),
                                "startTime", hora))))
                .andExpect(status().isCreated()));
    }

    private ResultActions marcarEnviado(long citaId) throws Exception {
        return mvc.perform(post("/api/v1/integrations/reminders/" + citaId + "/sent").header("X-Api-Key", API_KEY));
    }

    private JsonNode recordatorios(int horas) throws Exception {
        String body = mvc.perform(get("/api/v1/integrations/reminders").param("hours", String.valueOf(horas))
                        .header("X-Api-Key", API_KEY))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(body).get("items");
    }

    private JsonNode recordatorio(int horas, long citaId) throws Exception {
        for (JsonNode item : recordatorios(horas)) {
            if (item.get("appointmentId").asLong() == citaId) {
                return item;
            }
        }
        return null;
    }

    private static List<Long> ids(JsonNode items) {
        List<Long> ids = new ArrayList<>();
        items.forEach(item -> ids.add(item.get("appointmentId").asLong()));
        return ids;
    }

    private static JsonNode sede(JsonNode resumen, String code) {
        for (JsonNode site : resumen.get("sites")) {
            if (code.equals(site.get("siteCode").asText())) {
                return site;
            }
        }
        throw new AssertionError("Sin sede " + code);
    }

    /**
     * Espera el evento del tipo y la cita dados (la entrega es asíncrona, tras el commit) y verifica que
     * la firma HMAC corresponda exactamente al cuerpo recibido.
     */
    private JsonNode esperarEvento(String tipo, long citaId) throws Exception {
        long limite = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < limite) {
            Received recibido = WEBHOOK.poll(500, TimeUnit.MILLISECONDS);
            if (recibido == null) {
                continue;
            }
            JsonNode evento = json.readTree(recibido.body());
            if (tipo.equals(evento.get("eventType").asText()) && evento.get("appointment").get("id").asLong() == citaId) {
                assertThat(recibido.headers().get("x-citas-signature")).isEqualTo("sha256=" + hmac(recibido.body()));
                assertThat(recibido.headers().get("x-citas-event")).isEqualTo(tipo);
                assertThat(recibido.headers().get("x-citas-token")).isEqualTo(WEBHOOK_SECRET);
                assertThat(recibido.headers().get("x-citas-event-id")).isEqualTo(evento.get("eventId").asText());
                assertThat(new String(recibido.body(), StandardCharsets.UTF_8))
                        .doesNotContain("documentNumber").doesNotContain("Token").doesNotContain("password");
                return evento;
            }
        }
        throw new AssertionError("No llegó el evento " + tipo + " de la cita " + citaId);
    }

    private static String hmac(byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body));
    }

    private static HttpServer startFakeN8n() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/webhook/citas-status", exchange -> {
                Map<String, String> headers = new LinkedHashMap<>();
                exchange.getRequestHeaders().forEach((name, values) -> headers.put(name.toLowerCase(), values.get(0)));
                WEBHOOK.add(new Received(headers, exchange.getRequestBody().readAllBytes()));
                exchange.sendResponseHeaders(WEBHOOK_STATUS.get(), -1);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private long idDe(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(body).get("id").asLong();
    }

    private String registrarUsuario() throws Exception {
        int n = SEQUENCE.incrementAndGet();
        String email = "paciente" + n + "@n8n.local";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstNames", "Ana");
        body.put("lastNames", "Paciente");
        body.put("documentType", "CC");
        body.put("documentNumber", "830" + String.format("%05d", n));
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
