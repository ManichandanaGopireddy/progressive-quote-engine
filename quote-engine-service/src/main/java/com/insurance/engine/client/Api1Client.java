package com.insurance.engine.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class Api1Client {

    private final WebClient webClient;

    public Api1Client(
            @Value("${api1.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchSession(String quoteReferenceId) {
        log.info("Fetching session data from API 1 for: {}",
                quoteReferenceId);
        try {
            return webClient.get()
                    .uri("/api/v1/quote-session/{id}/review",
                            quoteReferenceId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            throw new RuntimeException(
                    "SESSION_NOT_FOUND: " + quoteReferenceId);
        } catch (Exception e) {
            throw new RuntimeException(
                    "API1_UNAVAILABLE: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getDrivers(
            Map<String, Object> session) {
        Object drivers = session.get("drivers");
        if (drivers instanceof List) {
            return (List<Map<String, Object>>) drivers;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getVehicles(
            Map<String, Object> session) {
        Object vehicles = session.get("vehicles");
        if (vehicles instanceof List) {
            return (List<Map<String, Object>>) vehicles;
        }
        return List.of();
    }
}