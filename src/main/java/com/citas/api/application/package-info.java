/**
 * Capa de aplicación: casos de uso y puertos.
 * <ul>
 *   <li>{@code port.in}: interfaces de casos de uso que consumen los adaptadores de entrada.</li>
 *   <li>{@code port.out}: interfaces que implementan los adaptadores de salida (persistencia, tokens, hash).</li>
 *   <li>{@code service}: implementaciones de los casos de uso.</li>
 * </ul>
 * Regla: depende solo de {@code domain}; nunca de {@code infrastructure}.
 */
package com.citas.api.application;
