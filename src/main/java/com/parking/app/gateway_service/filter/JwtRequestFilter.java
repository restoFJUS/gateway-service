package com.parking.app.gateway_service.filter;

import com.parking.app.gateway_service.service.AuthServiceClientImp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class JwtRequestFilter implements WebFilter {


    @Autowired
    private AuthServiceClientImp authServiceClientImp;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String jwt = authorizationHeader.substring(7);

            // Validar token usando Feign (esto es bloqueante, mejor cambiarlo por WebClient)
            Mono<Boolean> isValid = authServiceClientImp.validateToken("Bearer " + jwt);

            isValid.flatMap(valid -> {
                if (Boolean.TRUE.equals(valid)) {
                    return chain.filter(exchange);  // ✅ Si es válido, sigue la petición
                } else {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();  // ❌ Si es inválido, bloquea
                }
            });


        }

        return chain.filter(exchange);
    }

    private String extractUsernameFromToken(String token) {
        // Aquí puedes hacer una llamada al auth-service para extraer el username
        return "user"; // Esto es solo un ejemplo
    }
}
