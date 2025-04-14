package com.parking.app.gateway_service.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class JwtValidationFilter extends AbstractGatewayFilterFactory<JwtValidationFilter.Config> {

    private final WebClient webClient;

    @Value("${jwt.secret}")
    private String jwtSecret;

    public JwtValidationFilter(WebClient.Builder webClientBuilder) {
       super(Config.class);
       this.webClient = webClientBuilder.baseUrl("http://auth-service:8081").build();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
           System.out.println("🔍 [Gateway] Petición interceptada -> " + exchange.getRequest().getURI());

           String token = exchange.getRequest().getHeaders().getFirst("Authorization");
           System.out.println("🔍 [Gateway] Token recibido: " + token);

           if (token == null || !token.startsWith("Bearer ")) {
               System.err.println("⛔ [Gateway] Token no presente o mal formado");
               exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
               return exchange.getResponse().setComplete();
           }

           token = token.substring(7);

           String finalToken = token;
           return webClient.post()
                   .uri("/auth/validate")
                   .header("Authorization", "Bearer " + token)
                   .retrieve()
                   .bodyToMono(Boolean.class)
                   .doOnSuccess(isValid -> System.out.println("✅ [Gateway] Validación del token: " + isValid))
                   .doOnError(error -> System.err.println("❌ [Gateway] Error en validación de token: " + error.getMessage()))
                   .flatMap(isValid -> {
                       if (Boolean.TRUE.equals(isValid)) {
                           System.out.println("🚀 [Gateway] Token válido, enviando petición a Customer Service");
                           // 2. Decodificar el token localmente
                           Claims claims = Jwts.parser()
                                   .setSigningKey(jwtSecret)
                                   .parseClaimsJws(finalToken)
                                   .getBody();

                           String username = claims.getSubject();
                           List<String> roles = claims.get("roles", List.class);
                           // 🔁 Transformar roles a mayúsculas para evitar problemas de formato
                           List<String> rolesUpper = roles.stream()
                                   .map(r -> r.toString().toUpperCase(Locale.ROOT))
                                   .collect(Collectors.toList());

                           String path = exchange.getRequest().getURI().getPath();

                           System.out.println("🔍 Ruta solicitada: " + path);
                           System.out.println("🔍 Roles en el token: " + roles);
                           System.out.println("🔍 Roles normalizados: " + rolesUpper);

                           // 3. Verificar acceso por ruta y roles
                           if (path.startsWith("/customer") &&
                                   !rolesUpper.contains("ADMIN") &&
                                   !rolesUpper.contains("DUENIO")) {
                               System.err.println("⛔ Acceso denegado a /customer para roles: " + rolesUpper);
                               exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                               return exchange.getResponse().setComplete();
                           }

                           if (path.startsWith("/booking/") && !roles.contains("EMPLEADO") && !roles.contains("ADMIN")) {
                               exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                               return exchange.getResponse().setComplete();
                           }
                           return chain.filter(exchange);
                       } else {
                           System.err.println("⛔ [Gateway] Token inválido, bloqueando solicitud");
                           exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                           return exchange.getResponse().setComplete();
                       }
                   });
        };
    }

    public static class Config {
    }
}
