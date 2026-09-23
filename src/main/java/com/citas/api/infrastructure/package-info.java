/**
 * Infraestructura: detalles técnicos que conectan el núcleo con el exterior.
 * <ul>
 *   <li>{@code adapters.in.web}: controladores REST, DTOs y manejo global de errores.</li>
 *   <li>{@code adapters.out.persistence}: entidades JPA, repositorios Spring Data e implementaciones de puertos.</li>
 *   <li>{@code adapters.out.security}: JWT, hash de contraseñas y filtros de Spring Security.</li>
 *   <li>{@code config}: configuración de Spring (beans, seguridad, CORS).</li>
 * </ul>
 * Regla: puede depender de {@code application} y {@code domain}; nadie del núcleo depende de esta capa.
 */
package com.citas.api.infrastructure;
