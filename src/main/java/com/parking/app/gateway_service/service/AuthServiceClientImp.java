package com.parking.app.gateway_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class AuthServiceClientImp {

    private final WebClient webClient;

    public AuthServiceClientImp(@Value("${auth.service.url}") String authServiceUrl) {
        this.webClient = WebClient.builder().baseUrl(authServiceUrl).build();
    }

    public Mono<Boolean> validateToken(String token) {
        return webClient.post()
                .uri("/validate?token=" + token)
                .bodyValue(token)
                .retrieve()
                .bodyToMono(Boolean.class);
    }
}
