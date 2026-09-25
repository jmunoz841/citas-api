package com.citas.api.infrastructure.adapters.in.web.booking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DuplicateKeyException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterios de aceptación de HU-012 (búsqueda), HU-013 (cita general) y HU-014 (cita
 * especializada), más los diferidos HU-009 CA-02 y HU-010 CA-05, contra MySQL 8.4 real con las
 * migraciones V1 a V6.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class BookingApiIntegrationTest {

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
    private String userToken;
    private Long userId;
    private Long generalId;

    @BeforeEach
    void preparar() throws Exception {
        adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String email = registrarUsuario();
        userToken = login(email, PASSWORD);
        userId = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        generalId = jdbc.queryForObject("SELECT id FROM specialties WHERE is_general = TRUE", Long.class);
    }

    // ---------- HU-012: consultar disponibilidad ----------

    @Test
    void hu012_ca01_treintaMinutosMuestraSoloSlotsLibres() throws Exception {
        long especialidad = crearEspecialidad(30);
        Profesional prof = crearProfesional(List.of(especialidad), List.of("HIC"));
        crearBloque(prof, MANANA, "08:00", "10:00", "HIC");
        reservar(userToken, prof.id(), especialidad, "HIC", MANANA, "08:30").andExpect(status().isCreated());

        assertThat(inicios(buscar(Map.of("specialtyId", especialidad))))
                .containsExactly("08:00", "09:00", "09:30");
    }

    @Test
    void hu012_ca02_sesentaMinutosConEl0830ReservadoSoloOfrece0900() throws Exception {
        long treinta = crearEspecialidad(30);
        long sesenta = crearEspecialidad(60);
        Profesional prof = crearProfesional(List.of(treinta, sesenta), List.of("HIC"));
        crearBloque(prof, MANANA, "08:00", "10:00", "HIC");
        reservar(userToken, prof.id(), treinta, "HIC", MANANA, "08:30").andExpect(status().isCreated());

        JsonNode items = buscar(Map.of("specialtyId", sesenta));
        assertThat(inicios(items)).containsExactly("09:00");
        assertThat(items.get(0).get("endTime").asText()).isEqualTo("10:00");
        assertThat(items.get(0).get("durationMinutes").asInt()).isEqualTo(60);
    }

    @Test
    void hu012_ca03_losResultadosCumplenTodosLosFiltros() throws Exception {
        long especialidad = crearEspecialidad(30);
        long otra = crearEspecialidad(30);
        Profesional hic = crearProfesional(List.of(especialidad), List.of("HIC"));
        Profesional icv = crearProfesional(List.of(especialidad), List.of("ICV"));
        Profesional otroServicio = crearProfesional(List.of(otra), List.of("HIC"));
        crearBloque(hic, MANANA, "08:00", "09:00", "HIC");
        crearBloque(icv, MANANA, "08:00", "09:00", "ICV");
        crearBloque(otroServicio, MANANA, "08:00", "09:00", "HIC");

        JsonNode porEspecialidad = buscar(Map.of("specialtyId", especialidad));
        assertThat(porEspecialidad).hasSize(4);
        porEspecialidad.forEach(item -> assertThat(item.get("specialtyId").asLong()).isEqualTo(especialidad));

        JsonNode porSede = buscar(Map.of("specialtyId", especialidad, "siteCode", "ICV"));
        assertThat(porSede).hasSize(2);
        porSede.forEach(item -> {
            assertThat(item.get("siteCode").asText()).isEqualTo("ICV");
            assertThat(item.get("professionalId").asLong()).isEqualTo(icv.id());
        });

        JsonNode porProfesional = buscar(Map.of("professionalId", hic.id()));
        assertThat(porProfesional).hasSize(2);
        porProfesional.forEach(item -> assertThat(item.get("professionalId").asLong()).isEqualTo(hic.id()));

        JsonNode especializadas = buscar(Map.of("type", "SPECIALIZED", "siteCode", "HIC"));
        especializadas.forEach(item -> assertThat(item.get("type").asText()).isEqualTo("SPECIALIZED"));
        assertThat(buscar(Map.of("type", "GENERAL", "professionalId", hic.id()))).isEmpty();
    }

    @Test
    void hu012_ca04_especialidadInactivaNoAparece() throws Exception {
        long especialidad = crearEspecialidad(30);
        Profesional prof = crearProfesional(List.of(especialidad), List.of("HIC"));
        crearBloque(prof, MANANA, "08:00", "09:00", "HIC");
        assertThat(buscar(Map.of("specialtyId", especialidad))).hasSize(2);

        activarEspecialidad(especialidad, false);

        assertThat(buscar(Map.of("specialtyId", especialidad))).isEmpty();
        reservar(userToken, prof.id(), especialidad, "HIC", MANANA, "08:00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("specialtyId"));
    }

    @Test
    void hu012_ca04_horariosPasadosNoAparecen() throws Exception {
        long especialidad = crearEspecialidad(30);
        Profesional prof = crearProfesional(List.of(especialidad), List.of("HIC"));
        LocalDate ayer = LocalDate.now().minusDays(1);
        bloqueEnElPasado(prof.id(), ayer);

        mvc.perform(get("/api/v1/availability")
                        .param("date", ayer.toString())
                        .param("specialtyId", String.valueOf(especialidad))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void hu009_ca02_profesionalInactivoNoApareceYNoSePuedeReservar() throws Exception {
        long especialidad = crearEspecialidad(30);
        Profesional prof = crearProfesional(List.of(especialidad), List.of("HIC"));
        crearBloque(prof, MANANA, "08:00", "09:00", "HIC");
        assertThat(buscar(Map.of("specialtyId", especialidad))).hasSize(2);

        mvc.perform(patch("/api/v1/admin/professionals/" + prof.id() + "/active")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("active", false))))
                .andExpect(status().isOk());

        assertThat(buscar(Map.of("specialtyId", especialidad))).isEmpty();
        reservar(userToken, prof.id(), especialidad, "HIC", MANANA, "08:00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("professionalId"));
    }

    /** El filtro de especialidad del modal de reserva necesita este catálogo (HU-012 CA-03). */
    @Test
    void hu012_elCatalogoDeEspecialidadesEsPublicoYSoloMuestraLasActivas() throws Exception {
        long activa = crearEspecialidad(60);
        long inactiva = crearEspecialidad(30);
        activarEspecialidad(inactiva, false);

        String body = mvc.perform(get("/api/v1/catalogs/specialties"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode items = json.readTree(body).get("items");

        List<Long> ids = new ArrayList<>();
        items.forEach(item -> ids.add(item.get("id").asLong()));
        assertThat(ids).contains(generalId, activa).doesNotContain(inactiva);
        JsonNode general = items.get(ids.indexOf(generalId));
        assertThat(general.get("name").asText()).isEqualTo("Medicina General");
        assertThat(general.get("durationMinutes").asInt()).isEqualTo(30);
        assertThat(general.get("type").asText()).isEqualTo("GENERAL");
        JsonNode especializada = items.get(ids.indexOf(activa));
        assertThat(especializada.get("durationMinutes").asInt()).isEqualTo(60);
        assertThat(especializada.get("type").asText()).isEqualTo("SPECIALIZED");
        assertThat(especializada.has("active")).isFalse();
    }

    @Test
    void laFechaEsObligatoriaYElTipoDebeSerValido() throws Exception {
        mvc.perform(get("/api/v1/availability").header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("date"));

        mvc.perform(get("/api/v1/availability")
                        .param("date", MANANA.toString())
                        .param("type", "URGENTE")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("type"));
    }

    // ---------- HU-013: reservar cita general ----------

    @Test
    void hu013_ca01_citaGeneralQuedaAprobadaOcupaElSlotYDejaHistorial() throws Exception {
        Profesional prof = crearProfesional(List.of(generalId), List.of("HIC"));
        crearBloque(prof, MANANA, "08:00", "09:00", "HIC");

        long citaId = idDe(reservar(userToken, prof.id(), generalId, "HIC", MANANA, "08:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.startTime").value("08:00"))
                .andExpect(jsonPath("$.endTime").value("08:30")));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM slot_reservations WHERE appointment_id = ?",
                Integer.class, citaId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT patient_user_id FROM appointments WHERE id = ?", Long.class,
                citaId)).isEqualTo(userId);
        Map<String, Object> historial = jdbc.queryForMap(
                "SELECT status_code, source, actor_user_id FROM appointment_status_history WHERE appointment_id = ?",
                citaId);
        assertThat(historial.get("status_code")).isEqualTo("APPROVED");
        assertThat(historial.get("source")).isEqualTo("USER");
        assertThat(((Number) historial.get("actor_user_id")).longValue()).isEqualTo(userId);
    }

    @Test
    void hu013_ca02_horarioOcupadoAlConfirmarSeRechazaConCodigoIdentificable() throws Exception {
        Profesional prof = crearProfesional(List.of(generalId), List.of("HIC"));
        crearBloque(prof, MANANA, "08:00", "09:00", "HIC");
        String otroUsuario = login(registrarUsuario(), PASSWORD);
        reservar(otroUsuario, prof.id(), generalId, "HIC", MANANA, "08:00").andExpect(status().isCreated());

        reservar(userToken, prof.id(), generalId, "HIC", MANANA, "08:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));

        // La transacción fallida no deja una cita huérfana.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM appointments WHERE professional_id = ?",
                Integer.class, prof.id())).isEqualTo(1);
    }

    /**
     * LOOP del instructor: dos reservas simultáneas sobre el mismo slot. La aplicación no mira si
     * el slot está libre; la clave primaria de slot_reservations deja pasar solo a una.
     */
    @Test
    void hu013_ca03_dobleReservaConcurrenteSoloUnaCreaLaCita() throws Exception {
        Profesional prof = crearProfesional(List.of(generalId), List.of("HIC"));
        crearBloque(prof, MANANA, "08:00", "09:00", "HIC");
        String otroUsuario = login(registrarUsuario(), PASSWORD);

        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            List<Future<MvcResult>> resultados = new ArrayList<>();
            for (String token : List.of(userToken, otroUsuario)) {
                Callable<MvcResult> reserva = () -> {
                    salida.await();
                    return reservar(token, prof.id(), generalId, "HIC", MANANA, "08:00").andReturn();
                };
                resultados.add(hilos.submit(reserva));
            }
            salida.countDown();

            List<Integer> estados = new ArrayList<>();
            for (Future<MvcResult> resultado : resultados) {
                estados.add(resultado.get(30, TimeUnit.SECONDS).getResponse().getStatus());
            }
            assertThat(estados).containsExactlyInAnyOrder(201, 409);
        } finally {
            hilos.shutdownNow();
        }

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM appointments WHERE professional_id = ?",
                Integer.class, prof.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM slot_reservations WHERE professional_id = ?",
                Integer.class, prof.id())).isEqualTo(1);
    }

    @Test
    void hu013_ca03_laClavePrimariaDeLaBaseImpideOcuparDosVecesElMismoSlot() throws Exception {
        Profesional prof = crearProfesional(List.of(generalId), List.of("HIC"));
        crearBloque(prof, MANANA, "08:00", "08:30", "HIC");
        long citaId = idDe(reservar(userToken, prof.id(), generalId, "HIC", MANANA, "08:00")
                .andExpect(status().isCreated()));
        long slotId = jdbc.queryForObject("SELECT slot_id FROM slot_reservations WHERE appointment_id = ?",
                Long.class, citaId);

        // Sin pasar por la aplicación: la base sola rechaza la segunda ocupación.
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO slot_reservations (slot_id, professional_id, appointment_id) VALUES (?, ?, ?)",
                slotId, prof.id(), citaId))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void hu013_ca04_horarioPasadoSeRechaza() throws Exception {
        Profesional prof = crearProfesional(List.of(generalId), List.of("HIC"));
        LocalDate ayer = LocalDate.now().minusDays(1);
        bloqueEnElPasado(prof.id(), ayer);

        reservar(userToken, prof.id(), generalId, "HIC", ayer, "08:00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("startTime"));
    }

    @Test
    void unHorarioQueNoExisteSeRechaza() throws Exception {
        Profesional prof = crearProfesional(List.of(generalId), List.of("HIC"));
        crearBloque(prof, MANANA, "08:00", "09:00", "HIC");

        reservar(userToken, prof.id(), generalId, "HIC", MANANA, "15:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));
    }

    // ---------- HU-014: solicitar cita especializada ----------

    @Test
    void hu014_ca01_citaEspecializadaQuedaSolicitadaYRetieneElSlot() throws Exception {
        long especialidad = crearEspecialidad(30);
        Profesional prof = crearProfesional(List.of(especialidad), List.of("HIC"));
        crearBloque(prof, MANANA, "08:00", "09:00", "HIC");

        long citaId = idDe(reservar(userToken, prof.id(), especialidad, "HIC", MANANA, "08:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REQUESTED")));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM slot_reservations WHERE appointment_id = ?",
                Integer.class, citaId)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT status_code FROM appointment_status_history WHERE appointment_id = ?", String.class,
                citaId)).isEqualTo("REQUESTED");
        assertThat(inicios(buscar(Map.of("specialtyId", especialidad)))).containsExactly("08:30");
    }

    @Test
    void hu014_ca02_sesentaMinutosRetieneDosSlotsQueDesaparecenDeLaBusqueda() throws Exception {
        long sesenta = crearEspecialidad(60);
        long treinta = crearEspecialidad(30);
        Profesional prof = crearProfesional(List.of(sesenta, treinta), List.of("HIC"));
        crearBloque(prof, MANANA, "08:00", "10:00", "HIC");

        long citaId = idDe(reservar(userToken, prof.id(), sesenta, "HIC", MANANA, "08:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.endTime").value("09:00")));

        assertThat(jdbc.queryForList(
                "SELECT TIME_FORMAT(s.start_at, '%H:%i') FROM slot_reservations r "
                        + "JOIN availability_slots s ON s.id = r.slot_id WHERE r.appointment_id = ? ORDER BY 1",
                String.class, citaId)).containsExactly("08:00", "08:30");
        assertThat(inicios(buscar(Map.of("specialtyId", treinta)))).containsExactly("09:00", "09:30");
        assertThat(inicios(buscar(Map.of("specialtyId", sesenta)))).containsExactly("09:00");
    }

    @Test
    void hu014_ca03_especialidadNoAsociadaAlProfesionalSeRechaza() throws Exception {
        long suya = crearEspecialidad(30);
        long ajena = crearEspecialidad(30);
        Profesional prof = crearProfesional(List.of(suya), List.of("HIC"));
        crearBloque(prof, MANANA, "08:00", "09:00", "HIC");

        reservar(userToken, prof.id(), ajena, "HIC", MANANA, "08:00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("specialtyId"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM appointments WHERE professional_id = ?",
                Integer.class, prof.id())).isZero();
    }

    @Test
    void unaSedeDondeNoAtiendeElProfesionalSeRechaza() throws Exception {
        long especialidad = crearEspecialidad(30);
        Profesional prof = crearProfesional(List.of(especialidad), List.of("HIC"));
        crearBloque(prof, MANANA, "08:00", "09:00", "HIC");

        reservar(userToken, prof.id(), especialidad, "ICV", MANANA, "08:00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("siteCode"));
    }

    // ---------- HU-010 CA-05: bloque con citas ----------

    @Test
    void hu010_ca05_unBloqueConCitasNoSeEditaNiSeElimina() throws Exception {
        long especialidad = crearEspecialidad(30);
        Profesional prof = crearProfesional(List.of(especialidad), List.of("HIC"));
        long bloqueId = crearBloque(prof, MANANA, "08:00", "10:00", "HIC");
        reservar(userToken, prof.id(), especialidad, "HIC", MANANA, "09:00").andExpect(status().isCreated());

        mvc.perform(patch("/api/v1/professional/availability-blocks/" + bloqueId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + prof.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(bloque(MANANA, "08:00", "12:00", "HIC"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BLOCK_HAS_APPOINTMENTS"));
        mvc.perform(delete("/api/v1/professional/availability-blocks/" + bloqueId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + prof.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BLOCK_HAS_APPOINTMENTS"));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM availability_slots WHERE block_id = ?",
                Integer.class, bloqueId)).isEqualTo(4);
    }

    @Test
    void hu010_ca05_laFkDeLaBaseImpideBorrarLosSlotsDeUnBloqueConCitas() throws Exception {
        long especialidad = crearEspecialidad(30);
        Profesional prof = crearProfesional(List.of(especialidad), List.of("HIC"));
        long bloqueId = crearBloque(prof, MANANA, "08:00", "09:00", "HIC");
        reservar(userToken, prof.id(), especialidad, "HIC", MANANA, "08:00").andExpect(status().isCreated());

        assertThatThrownBy(() -> jdbc.update("DELETE FROM availability_blocks WHERE id = ?", bloqueId))
                .hasMessageContaining("fk_sr_slot");
    }

    /**
     * Regresión: guardar un profesional borraba y reinsertaba sus asignaciones, y la FK RESTRICT
     * de bloques y citas lo impedía con un 500. Ahora se sincroniza por diferencia.
     */
    @Test
    void reasignarEspecialidadesYSedesDeUnProfesionalConCitasConservaLasQueSiguen() throws Exception {
        long primera = crearEspecialidad(30);
        long segunda = crearEspecialidad(30);
        Profesional prof = crearProfesional(List.of(primera), List.of("HIC"));
        crearBloque(prof, MANANA, "08:00", "09:00", "HIC");
        reservar(userToken, prof.id(), primera, "HIC", MANANA, "08:00").andExpect(status().isCreated());

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/admin/professionals/" + prof.id() + "/specialties")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("specialties", List.of(
                                Map.of("specialtyId", primera, "primary", false),
                                Map.of("specialtyId", segunda, "primary", true))))))
                .andExpect(status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/admin/professionals/" + prof.id() + "/sites")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("siteCodes", List.of("HIC", "ICV")))))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT specialty_id FROM professional_specialties "
                + "WHERE professional_id = ? AND is_primary = TRUE", Long.class, prof.id())).isEqualTo(segunda);
        assertThat(inicios(buscar(Map.of("specialtyId", segunda)))).containsExactly("08:30");
    }

    // ---------- Autorización ----------

    @Test
    void soloUnUserPuedeBuscarYReservar() throws Exception {
        long especialidad = crearEspecialidad(30);
        Profesional prof = crearProfesional(List.of(especialidad), List.of("HIC"));

        mvc.perform(get("/api/v1/availability").param("date", MANANA.toString()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/availability").param("date", MANANA.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + prof.token()))
                .andExpect(status().isForbidden());
        reservar(adminToken, prof.id(), especialidad, "HIC", MANANA, "08:00").andExpect(status().isForbidden());
    }

    // ---------- Utilidades ----------

    record Profesional(Long id, String token) {
    }

    private JsonNode buscar(Map<String, Object> filtros) throws Exception {
        var request = get("/api/v1/availability").param("date", MANANA.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken);
        filtros.forEach((clave, valor) -> request.param(clave, String.valueOf(valor)));
        String body = mvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(body).get("items");
    }

    private static List<String> inicios(JsonNode items) {
        List<String> inicios = new ArrayList<>();
        items.forEach(item -> inicios.add(item.get("startTime").asText()));
        return inicios;
    }

    private ResultActions reservar(String token, Long professionalId, Long specialtyId, String siteCode,
                                   LocalDate date, String startTime) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("professionalId", professionalId);
        body.put("specialtyId", specialtyId);
        body.put("siteCode", siteCode);
        body.put("date", date.toString());
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

    /** Un bloque de ayer, 08:00–09:00: la API no deja publicarlo, así que se inserta directo. */
    private void bloqueEnElPasado(Long professionalId, LocalDate dia) {
        jdbc.update("INSERT INTO availability_blocks (professional_id, site_code, start_at, end_at) "
                + "VALUES (?, 'HIC', ?, ?)", professionalId, dia.atTime(8, 0), dia.atTime(9, 0));
        Long bloqueId = jdbc.queryForObject(
                "SELECT id FROM availability_blocks WHERE professional_id = ? AND start_at = ?", Long.class,
                professionalId, dia.atTime(8, 0));
        for (int minutos : new int[]{0, 30}) {
            jdbc.update("INSERT INTO availability_slots (block_id, professional_id, start_at) VALUES (?, ?, ?)",
                    bloqueId, professionalId, dia.atTime(8, minutos));
        }
    }

    private static Map<String, Object> bloque(LocalDate date, String start, String end, String siteCode) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("date", date.toString());
        body.put("startTime", start);
        body.put("endTime", end);
        body.put("siteCode", siteCode);
        return body;
    }

    private long crearBloque(Profesional prof, LocalDate date, String start, String end, String siteCode)
            throws Exception {
        return idDe(mvc.perform(post("/api/v1/professional/availability-blocks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + prof.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(bloque(date, start, end, siteCode))))
                .andExpect(status().isCreated()));
    }

    private long crearEspecialidad(int durationMinutes) throws Exception {
        int n = SEQUENCE.incrementAndGet();
        return idDe(mvc.perform(post("/api/v1/admin/specialties")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("name", "Reserva " + n,
                                "durationMinutes", durationMinutes))))
                .andExpect(status().isCreated()));
    }

    private void activarEspecialidad(long specialtyId, boolean active) throws Exception {
        mvc.perform(patch("/api/v1/admin/specialties/" + specialtyId + "/active")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("active", active))))
                .andExpect(status().isOk());
    }

    /** Profesional con las especialidades dadas (la primera es la primaria) y su access token. */
    private Profesional crearProfesional(List<Long> specialtyIds, List<String> siteCodes) throws Exception {
        int n = SEQUENCE.incrementAndGet();
        List<Map<String, Object>> specialties = new ArrayList<>();
        for (int i = 0; i < specialtyIds.size(); i++) {
            specialties.add(Map.of("specialtyId", specialtyIds.get(i), "primary", i == 0));
        }
        String email = "reserva" + n + "@test.local";
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("firstNames", "Mario");
        request.put("lastNames", "Reserva " + n);
        request.put("documentType", "CC");
        request.put("documentNumber", "500" + String.format("%05d", n));
        request.put("email", email);
        request.put("phone", "3001234567");
        request.put("temporaryPassword", PASSWORD);
        request.put("professionalCode", "RSV-" + n);
        request.put("licenseNumber", "RSL-" + n);
        request.put("specialties", specialties);
        request.put("siteCodes", siteCodes);

        long id = idDe(mvc.perform(post("/api/v1/admin/professionals")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(request)))
                .andExpect(status().isCreated()));
        return new Profesional(id, login(email, PASSWORD));
    }

    private String registrarUsuario() throws Exception {
        int n = SEQUENCE.incrementAndGet();
        String email = "paciente" + n + "@test.local";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstNames", "Ana");
        body.put("lastNames", "Paciente");
        body.put("documentType", "CC");
        body.put("documentNumber", "600" + String.format("%05d", n));
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
