package com.citas.api.infrastructure.adapters.in.web.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterios de aceptación de HU-005 contra MySQL 8.4 real (Testcontainers) con las
 * migraciones Flyway V1 y V2 aplicadas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class CatalogApiIntegrationTest {

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

    @Test
    void ca01_lasSedesSonExactamenteHicEIcvConSuDireccion() throws Exception {
        mvc.perform(get("/api/v1/catalogs/sites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].code").value("HIC"))
                .andExpect(jsonPath("$.items[0].name").value("Hospital Internacional de Colombia"))
                .andExpect(jsonPath("$.items[0].address").value(
                        "Km 7 Autopista Bucaramanga–Piedecuesta, Valle de Menzulí, Santander"))
                .andExpect(jsonPath("$.items[1].code").value("ICV"))
                .andExpect(jsonPath("$.items[1].address").value(
                        "Calle 155A No. 23-58, Urbanización El Bosque, Floridablanca, Santander"));
    }

    @Test
    void ca02_regimenesDevuelveLosValoresSembrados() throws Exception {
        mvc.perform(get("/api/v1/catalogs/regimes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[?(@.code == 'CONTRIBUTIVO')].name").value("Contributivo"))
                .andExpect(jsonPath("$.items[?(@.code == 'SUBSIDIADO')].name").value("Subsidiado"));
    }

    @Test
    void ca02_estadosDeCitaIncluyenSuMarcaDeTerminal() throws Exception {
        mvc.perform(get("/api/v1/catalogs/appointment-statuses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(6))
                .andExpect(jsonPath("$.items[?(@.code == 'REQUESTED')].terminal").value(false))
                .andExpect(jsonPath("$.items[?(@.code == 'APPROVED')].terminal").value(false))
                .andExpect(jsonPath("$.items[?(@.code == 'REJECTED')].terminal").value(true))
                .andExpect(jsonPath("$.items[?(@.code == 'CANCELLED')].terminal").value(true))
                .andExpect(jsonPath("$.items[?(@.code == 'COMPLETED')].terminal").value(true))
                .andExpect(jsonPath("$.items[?(@.code == 'NO_SHOW')].terminal").value(true));
    }

    @Test
    void ca02_estadosDeReprogramacionDevuelveLosCuatroValores() throws Exception {
        mvc.perform(get("/api/v1/catalogs/reschedule-statuses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(4))
                .andExpect(jsonPath("$.items[?(@.code == 'PENDING')].terminal").value(false))
                .andExpect(jsonPath("$.items[?(@.code == 'APPROVED')].terminal").value(true));
    }

    @Test
    void ca02_rolesYTiposDeDocumentoVienenDeLaMigracionV1() throws Exception {
        mvc.perform(get("/api/v1/catalogs/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[?(@.code == 'USER')].name").value("Usuario"))
                .andExpect(jsonPath("$.items[?(@.code == 'ADMIN')].name").value("Administrador"));

        mvc.perform(get("/api/v1/catalogs/document-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(6))
                .andExpect(jsonPath("$.items[?(@.code == 'CC')].name").value("Cédula de ciudadanía"));
    }

    @Test
    void ca03_unUsuarioAutenticadoTampocoPuedeEscribirEnLosCatalogos() throws Exception {
        String token = accessTokenDeUnUsuarioNuevo();

        // El endpoint existe para GET, pero no hay manejador de escritura: 405, no 403.
        mvc.perform(post("/api/v1/catalogs/sites").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(put("/api/v1/catalogs/sites").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(patch("/api/v1/catalogs/sites").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(delete("/api/v1/catalogs/regimes").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void ca03_sinSesionLaEscrituraNiSiquieraLlegaAlControlador() throws Exception {
        mvc.perform(post("/api/v1/catalogs/sites")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/catalogs/sites")).andExpect(status().isUnauthorized());
    }

    @Test
    void losCatalogosSonPublicosParaAlimentarElFormularioDeRegistro() throws Exception {
        // Sin cabecera Authorization: el registro necesita tipos de documento y sedes.
        mvc.perform(get("/api/v1/catalogs/document-types")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/catalogs/sites")).andExpect(status().isOk());
    }

    @Test
    void unCatalogoInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/v1/catalogs/no-existe")).andExpect(status().isNotFound());
    }

    /** Registra un usuario sintético e inicia sesión; devuelve su access token. */
    private String accessTokenDeUnUsuarioNuevo() throws Exception {
        int n = SEQUENCE.incrementAndGet();
        String email = "catalogos" + n + "@test.local";
        Map<String, Object> registro = new LinkedHashMap<>();
        registro.put("firstNames", "Ana");
        registro.put("lastNames", "Prueba");
        registro.put("documentType", "CC");
        registro.put("documentNumber", "800" + String.format("%05d", n));
        registro.put("email", email);
        registro.put("phone", "3001234567");
        registro.put("password", PASSWORD);

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(registro)))
                .andExpect(status().isCreated());

        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(body).get("accessToken").asText();
    }
}
