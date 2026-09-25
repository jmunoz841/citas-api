package com.citas.api.infrastructure.adapters.in.web.professional;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterios de aceptación de HU-010 (bloques de disponibilidad) contra MySQL 8.4 real con las
 * migraciones V1 a V5.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AvailabilityApiIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withCommand("--default-time-zone=America/Bogota");

    private static final String ADMIN_EMAIL = "admin@citas.local";
    private static final String ADMIN_PASSWORD = "Admin.Lab2026";
    private static final String PASSWORD = "Segura123";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final LocalDate MANANA = LocalDate.now().plusDays(1);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    private String adminToken;
    private String profToken;
    private Long profId;

    @BeforeEach
    void prepararProfesional() throws Exception {
        adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        Map<String, Object> creado = crearProfesional(List.of("HIC"));
        profId = (Long) creado.get("id");
        profToken = login((String) creado.get("email"), PASSWORD);
    }

    @Test
    void ca01_dosBloquesEnUnDiaGeneranSusSlotsDe30Minutos() throws Exception {
        crearBloque(profToken, MANANA, "08:00", "12:00", "HIC")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slots").value(8))
                .andExpect(jsonPath("$.siteCode").value("HIC"));

        crearBloque(profToken, MANANA, "14:00", "17:00", "HIC")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slots").value(6));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM availability_slots WHERE professional_id = ?",
                Integer.class, profId)).isEqualTo(14);
        // Los slots quedan alineados a :00 y :30.
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM availability_slots WHERE professional_id = ? AND MINUTE(start_at) NOT IN (0,30)",
                Integer.class, profId)).isZero();
    }

    @Test
    void ca02_unBloqueEnElPasadoSeRechaza() throws Exception {
        crearBloque(profToken, LocalDate.now().minusDays(1), "08:00", "12:00", "HIC")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("startTime"));
    }

    @Test
    void ca03_unBloqueQueSeCruzaConOtroSeRechazaAunEnOtraSede() throws Exception {
        asignarSedes(profId, List.of("HIC", "ICV"));
        crearBloque(profToken, MANANA, "08:00", "12:00", "HIC").andExpect(status().isCreated());

        crearBloque(profToken, MANANA, "11:00", "13:00", "HIC")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("startTime"));

        // Mismo horario en la otra sede: un profesional no puede estar en dos sitios a la vez.
        crearBloque(profToken, MANANA, "11:00", "13:00", "ICV")
                .andExpect(status().isBadRequest());
    }

    @Test
    void bloquesContiguosNoSeConsideranSolapados() throws Exception {
        crearBloque(profToken, MANANA, "08:00", "12:00", "HIC").andExpect(status().isCreated());

        crearBloque(profToken, MANANA, "12:00", "14:00", "HIC").andExpect(status().isCreated());
    }

    @Test
    void ca04_unaSedeNoAsignadaSeRechaza() throws Exception {
        crearBloque(profToken, MANANA, "08:00", "12:00", "ICV")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("siteCode"));
    }

    @Test
    void ca06_nadiePuedeTocarLosBloquesDeOtroProfesional() throws Exception {
        Long blockId = idDe(crearBloque(profToken, MANANA, "08:00", "12:00", "HIC")
                .andExpect(status().isCreated()));

        Map<String, Object> otro = crearProfesional(List.of("HIC"));
        String otroToken = login((String) otro.get("email"), PASSWORD);

        // Se responde 404, no 403: no se revela que el bloque existe.
        mvc.perform(patch("/api/v1/professional/availability-blocks/" + blockId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(bloque(MANANA, "09:00", "10:00", "HIC"))))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/professional/availability-blocks/" + blockId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otroToken))
                .andExpect(status().isNotFound());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM availability_blocks WHERE id = ?", Integer.class,
                blockId)).isEqualTo(1);
    }

    @Test
    void unUsuarioSinRolProfesionalNoAccedeALaAgenda() throws Exception {
        String userToken = tokenDeUnUserNuevo();

        mvc.perform(get("/api/v1/professional/availability-blocks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/professional/availability-blocks")).andExpect(status().isUnauthorized());
    }

    @Test
    void lasHorasDebenCaerEnPuntoOYMedia() throws Exception {
        crearBloque(profToken, MANANA, "08:15", "12:00", "HIC")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("startTime"));

        crearBloque(profToken, MANANA, "08:00", "12:45", "HIC")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("endTime"));
    }

    @Test
    void elFinDebeSerPosteriorAlInicio() throws Exception {
        crearBloque(profToken, MANANA, "12:00", "08:00", "HIC")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("endTime"));
    }

    @Test
    void editarUnBloqueRegeneraSusSlots() throws Exception {
        Long blockId = idDe(crearBloque(profToken, MANANA, "08:00", "12:00", "HIC")
                .andExpect(status().isCreated()));
        assertThat(contarSlots(blockId)).isEqualTo(8);

        mvc.perform(patch("/api/v1/professional/availability-blocks/" + blockId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + profToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(bloque(MANANA, "09:00", "10:30", "HIC"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots").value(3));

        assertThat(contarSlots(blockId)).isEqualTo(3);
    }

    @Test
    void eliminarUnBloqueSeLlevaSusSlots() throws Exception {
        Long blockId = idDe(crearBloque(profToken, MANANA, "08:00", "10:00", "HIC")
                .andExpect(status().isCreated()));

        mvc.perform(delete("/api/v1/professional/availability-blocks/" + blockId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + profToken))
                .andExpect(status().isNoContent());

        assertThat(contarSlots(blockId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM availability_blocks WHERE id = ?", Integer.class,
                blockId)).isZero();
    }

    @Test
    void elCalendarioPropioSeFiltraPorDiaYSede() throws Exception {
        asignarSedes(profId, List.of("HIC", "ICV"));
        crearBloque(profToken, MANANA, "08:00", "10:00", "HIC").andExpect(status().isCreated());
        crearBloque(profToken, MANANA.plusDays(1), "08:00", "10:00", "ICV").andExpect(status().isCreated());

        mvc.perform(get("/api/v1/professional/availability-blocks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + profToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));

        mvc.perform(get("/api/v1/professional/availability-blocks")
                        .param("date", MANANA.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + profToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].siteCode").value("HIC"));

        mvc.perform(get("/api/v1/professional/availability-blocks")
                        .param("siteCode", "ICV")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + profToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].siteCode").value("ICV"));
    }

    @Test
    void unProfesionalInactivoNoPuedePublicarAgenda() throws Exception {
        mvc.perform(patch("/api/v1/admin/professionals/" + profId + "/active")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("active", false))))
                .andExpect(status().isOk());

        crearBloque(profToken, MANANA, "08:00", "12:00", "HIC")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("professional"));
    }

    /** "Mi agenda" necesita las sedes, el estado y la especialidad principal del profesional. */
    @Test
    void elProfesionalConsultaSuPropioPerfil() throws Exception {
        asignarSedes(profId, List.of("HIC", "ICV"));

        mvc.perform(get("/api/v1/professional/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + profToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(profId))
                .andExpect(jsonPath("$.firstNames").value("Laura"))
                .andExpect(jsonPath("$.lastNames").value("Agenda"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.primarySpecialty.name").value(org.hamcrest.Matchers.startsWith("Agenda ")))
                .andExpect(jsonPath("$.sites.length()").value(2))
                .andExpect(jsonPath("$.sites[0].code").value("HIC"))
                .andExpect(jsonPath("$.sites[0].name").value("Hospital Internacional de Colombia"));

        mvc.perform(patch("/api/v1/admin/professionals/" + profId + "/active")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("active", false))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/professional/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + profToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(get("/api/v1/professional/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDeUnUserNuevo()))
                .andExpect(status().isForbidden());
    }

    // ---------- Utilidades ----------

    private int contarSlots(Long blockId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM availability_slots WHERE block_id = ?", Integer.class,
                blockId);
    }

    private Long idDe(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(body).get("id").asLong();
    }

    private static Map<String, Object> bloque(LocalDate date, String start, String end, String siteCode) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("date", date.toString());
        body.put("startTime", start);
        body.put("endTime", end);
        body.put("siteCode", siteCode);
        return body;
    }

    private ResultActions crearBloque(String token, LocalDate date, String start, String end, String siteCode)
            throws Exception {
        return mvc.perform(post("/api/v1/professional/availability-blocks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(bloque(date, start, end, siteCode))));
    }

    private void asignarSedes(Long professionalId, List<String> siteCodes) throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/admin/professionals/" + professionalId + "/sites")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("siteCodes", siteCodes))))
                .andExpect(status().isOk());
    }

    /** Crea una especialidad y un profesional asignado a las sedes indicadas. */
    private Map<String, Object> crearProfesional(List<String> siteCodes) throws Exception {
        int n = SEQUENCE.incrementAndGet();
        String specialtyBody = mvc.perform(post("/api/v1/admin/specialties")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("name", "Agenda " + n, "durationMinutes", 30))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long specialtyId = json.readTree(specialtyBody).get("id").asLong();

        String email = "agenda" + n + "@test.local";
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("firstNames", "Laura");
        request.put("lastNames", "Agenda");
        request.put("documentType", "CC");
        request.put("documentNumber", "400" + String.format("%05d", n));
        request.put("email", email);
        request.put("phone", "3001234567");
        request.put("temporaryPassword", PASSWORD);
        request.put("professionalCode", "AGD-" + n);
        request.put("licenseNumber", "AGL-" + n);
        request.put("specialties", List.of(Map.of("specialtyId", specialtyId, "primary", true)));
        request.put("siteCodes", siteCodes);

        String body = mvc.perform(post("/api/v1/admin/professionals")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        Map<String, Object> creado = new LinkedHashMap<>();
        creado.put("id", json.readTree(body).get("id").asLong());
        creado.put("email", email);
        return creado;
    }

    private String tokenDeUnUserNuevo() throws Exception {
        int n = SEQUENCE.incrementAndGet();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstNames", "Ana");
        body.put("lastNames", "Usuaria");
        body.put("documentType", "CC");
        body.put("documentNumber", "300" + String.format("%05d", n));
        body.put("email", "agendauser" + n + "@test.local");
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
}
