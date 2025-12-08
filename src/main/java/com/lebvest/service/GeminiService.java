package com.lebvest.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class GeminiService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Cacheable(value = "searchSuggestions", key = "#query")
    public List<String> getSearchSuggestions(String query) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Gemini API key not configured, returning empty suggestions");
            return List.of();
        }

        try {
            String prompt = String.format(
                "Given the search query '%s' for an investment platform, suggest 5 relevant search terms or investment-related keywords. " +
                "Return only a JSON array of strings, no other text. Example: [\"real estate\", \"technology startups\", \"low risk bonds\"]",
                query
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("x-goog-api-key", apiKey);

            String requestBody = String.format(
                "{\"contents\":[{\"parts\":[{\"text\":\"%s\"}]}]}",
                prompt.replace("\"", "\\\"")
            );

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                String text = jsonNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

                // Parse JSON array from response
                text = text.trim();
                if (text.startsWith("[")) {
                    JsonNode suggestionsNode = objectMapper.readTree(text);
                    List<String> suggestions = new ArrayList<>();
                    for (JsonNode node : suggestionsNode) {
                        suggestions.add(node.asText());
                    }
                    return suggestions;
                }
            }

            log.warn("Failed to get suggestions from Gemini API");
            return List.of();
        } catch (Exception e) {
            log.error("Error calling Gemini API for search suggestions: {}", e.getMessage(), e);
            return List.of();
        }
    }
}

