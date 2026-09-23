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
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterios de aceptación de HU-004 (afiliación opcional en el registro) contra MySQL 8.4 real
 * con las migraciones V1, V2 y V3 aplicadas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class RegisterAffiliationApiIntegrationTest {

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

    @Test
    void ca05_elCatalogoSoloOfreceLosPlanesSeleccionables() throws Exception {
        String body = mvc.perform(get("/api/v1/catalogs/insurance-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].epsName").exists())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode items = json.readTree(body).get("items");
        // Sembrados: 6 planes, pero uno está inactivo y otro pertenece a una EPS inactiva.
        assertThat(items).hasSize(4);
        assertThat(items.toString()).doesNotContain("Plan Descontinuado").doesNotContain("Plan Suspendido");
    }

    @Test
    void ca01_registroSinAfiliacionCreaLaCuentaYNingunaAfiliacion() throws Exception {
        Map<String, Object> body = newUser();

        postJson("/api/v1/auth/register", body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles[0]").value("USER"));

        assertThat(countAffiliations((String) body.get("email"))).isZero();
    }

    @Test
    void ca02_registroConPlanYRegimenCreaLaAfiliacionPorClaveForanea() throws Exception {
        long planId = anySelectablePlanId();
        Map<String, Object> body = newUser();
        body.put("insurancePlanId", planId);
        body.put("regimeCode", "CONTRIBUTIVO");

        postJson("/api/v1/auth/register", body).andExpect(status().isCreated());

        Map<String, Object> row = jdbc.queryForMap("""
                SELECT a.plan_id, a.regime_code FROM user_affiliations a
                JOIN users u ON u.id = a.user_id WHERE u.email = ?
                """, body.get("email"));
        assertThat(((Number) row.get("plan_id")).longValue()).isEqualTo(planId);
        assertThat(row.get("regime_code")).isEqualTo("CONTRIBUTIVO");
    }

    @Test
    void ca03_unPlanInexistenteDevuelve400YNoCreaUsuario() throws Exception {
        Map<String, Object> body = newUser();
        body.put("insurancePlanId", 999999);
        body.put("regimeCode", "CONTRIBUTIVO");

        postJson("/api/v1/auth/register", body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("insurancePlanId"));

        assertThat(countUsers((String) body.get("email"))).isZero();
    }

    @Test
    void ca03_unPlanInactivoNoEsSeleccionable() throws Exception {
        Long inactivo = jdbc.queryForObject("SELECT id FROM eps_plans WHERE name = 'Plan Descontinuado'", Long.class);
        Map<String, Object> body = newUser();
        body.put("insurancePlanId", inactivo);
        body.put("regimeCode", "CONTRIBUTIVO");

        postJson("/api/v1/auth/register", body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("insurancePlanId"));
        assertThat(countUsers((String) body.get("email"))).isZero();
    }

    @Test
    void ca03_unPlanDeUnaEpsInactivaTampocoEsSeleccionable() throws Exception {
        Long suspendido = jdbc.queryForObject("SELECT id FROM eps_plans WHERE name = 'Plan Suspendido'", Long.class);
        Map<String, Object> body = newUser();
        body.put("insurancePlanId", suspendido);
        body.put("regimeCode", "CONTRIBUTIVO");

        postJson("/api/v1/auth/register", body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("insurancePlanId"));
    }

    @Test
    void ca03_unRegimenInexistenteDevuelve400() throws Exception {
        Map<String, Object> body = newUser();
        body.put("insurancePlanId", anySelectablePlanId());
        body.put("regimeCode", "INVENTADO");

        postJson("/api/v1/auth/register", body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("regimeCode"));
        assertThat(countUsers((String) body.get("email"))).isZero();
    }

    @Test
    void ca04_laAfiliacionNoGuardaNombresDeEpsNiDePlan() throws Exception {
        Map<String, Object> body = newUser();
        body.put("insurancePlanId", anySelectablePlanId());
        body.put("regimeCode", "SUBSIDIADO");
        postJson("/api/v1/auth/register", body).andExpect(status().isCreated());

        // La tabla solo tiene claves: ninguna columna de texto con el nombre de la EPS o del plan.
        assertThat(jdbc.queryForList("SHOW COLUMNS FROM user_affiliations").stream()
                .map(column -> String.valueOf(column.get("Field"))).toList())
                .containsExactlyInAnyOrder("id", "user_id", "plan_id", "regime_code", "created_at", "updated_at");
    }

    @Test
    void ca06_soloElPlanOSoloElRegimenDevuelve400SenalandoElCampoQueFalta() throws Exception {
        Map<String, Object> soloPlan = newUser();
        soloPlan.put("insurancePlanId", anySelectablePlanId());
        postJson("/api/v1/auth/register", soloPlan)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("regimeCode"));

        Map<String, Object> soloRegimen = newUser();
        soloRegimen.put("regimeCode", "CONTRIBUTIVO");
        postJson("/api/v1/auth/register", soloRegimen)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("insurancePlanId"));

        assertThat(countUsers((String) soloPlan.get("email"))).isZero();
        assertThat(countUsers((String) soloRegimen.get("email"))).isZero();
    }

    // ---------- Utilidades ----------

    private long anySelectablePlanId() {
        return jdbc.queryForObject("SELECT id FROM eps_plans WHERE name = 'Plan Básico'", Long.class);
    }

    private int countUsers(String email) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
    }

    private int countAffiliations(String email) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM user_affiliations a JOIN users u ON u.id = a.user_id WHERE u.email = ?
                """, Integer.class, email);
    }

    private static Map<String, Object> newUser() {
        int n = SEQUENCE.incrementAndGet();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstNames", "Ana");
        body.put("lastNames", "Prueba");
        body.put("documentType", "CC");
        body.put("documentNumber", "700" + String.format("%05d", n));
        body.put("email", "afiliacion" + n + "@test.local");
        body.put("phone", "3001234567");
        body.put("password", PASSWORD);
        return body;
    }

    private ResultActions postJson(String path, Object body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)));
    }
}
