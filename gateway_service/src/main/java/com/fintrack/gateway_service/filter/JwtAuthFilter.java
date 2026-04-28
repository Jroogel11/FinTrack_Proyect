package com.fintrack.gateway_service.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * FILTRO JWT DEL GATEWAY
 *
 * A diferencia de los filtros de los otros servicios (que usaban OncePerRequestFilter
 * de Spring MVC), este filtro usa la API de Spring Cloud Gateway que es reactiva
 * (WebFlux). Por eso trabajamos con ServerWebExchange en vez de HttpServletRequest.
 *
 * AbstractGatewayFilterFactory<Config> es la clase base para crear filtros
 * configurables en el Gateway. El Config es la clase interna que contiene
 * los parámetros del filtro (en nuestro caso, excludedPaths).
 */
@Slf4j
@Component
public class JwtAuthFilter extends
        AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    @Value("${jwt.secret}")
    private String secretKey;

    public JwtAuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            // Si la ruta está excluida, dejamos pasar sin validar JWT
            if (isExcluded(path, config.getExcludedPaths())) {
                log.debug("Path excluded from JWT validation: {}", path);
                return chain.filter(exchange);
            }

            // Obtener el header Authorization
            String authHeader = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or invalid Authorization header for path: {}", path);
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = extractAllClaims(token);

                if (isTokenExpired(claims)) {
                    log.warn("Expired JWT token for path: {}", path);
                    return onError(exchange, HttpStatus.UNAUTHORIZED);
                }

                // Añadimos el userId en los headers para que los servicios
                // downstream no tengan que volver a parsear el JWT
                Long userId = claims.get("userId", Long.class);
                String role = claims.get("role", String.class);

                log.debug("JWT valid — userId: {}, role: {}, path: {}", userId, role, path);

                ServerWebExchange modifiedExchange = exchange.mutate()
                        .request(exchange.getRequest().mutate()
                                .header("X-User-Id", userId.toString())
                                .header("X-User-Role", role)
                                .build())
                        .build();

                return chain.filter(modifiedExchange);

            } catch (Exception e) {
                log.warn("JWT validation failed for path {}: {}", path, e.getMessage());
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }
        };
    }

    /**
     * Devuelve una respuesta de error sin pasar la petición al siguiente filtro.
     * reactor.core.publisher.Mono es el tipo reactivo equivalente a un Future/Promise.
     */
    private reactor.core.publisher.Mono<Void> onError(
            ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    private boolean isExcluded(String path, String excludedPaths) {
        if (excludedPaths == null || excludedPaths.isEmpty()) {
            return false;
        }
        List<String> excluded = Arrays.asList(excludedPaths.split(","));
        return excluded.stream().anyMatch(path::equals);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Clase de configuración del filtro.
     * Los campos se mapean desde el application.yml:
     *
     * filters:
     *   - name: JwtAuthFilter
     *     args:
     *       excludedPaths: /api/v1/auth/register,/api/v1/auth/login
     */
    public static class Config {
        private String excludedPaths;

        public String getExcludedPaths() { return excludedPaths; }
        public void setExcludedPaths(String excludedPaths) {
            this.excludedPaths = excludedPaths;
        }
    }
}