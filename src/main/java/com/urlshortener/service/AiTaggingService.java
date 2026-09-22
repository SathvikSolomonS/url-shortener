package com.urlshortener.service;

import com.urlshortener.repository.UrlRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTaggingService {

    private final UrlRepository urlRepository;

    @Value("${app.ai.groq-api-key}")
    private String groqApiKey;

    @Value("${app.ai.enabled}")
    private boolean aiEnabled;

    private final RestClient restClient = RestClient.create();

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "openai/gpt-oss-20b";

    @PostConstruct
    public void logConfigStatus() {
        log.info("=== AI TAGGING CONFIG CHECK === enabled={}, keyPresent={}",
                aiEnabled, (groqApiKey != null && !groqApiKey.isBlank()));
    }

    @Async
    public void categorizeUrl(Long urlId, String originalUrl) {
        if (!aiEnabled || groqApiKey == null || groqApiKey.isBlank()) {
            return;
        }

        try {
            String prompt = "In exactly one word, categorize this URL (e.g. Shopping, News, "
                    + "Documentation, Social Media, Video, Education, Other): " + originalUrl;

            Map<String, Object> requestBody = Map.of(
                    "model", MODEL,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    ),
                    "max_tokens", 150,
                    "reasoning_effort", "low"
            );

            Map<String, Object> response = restClient.post()
                    .uri(GROQ_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + groqApiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            String category = extractCategory(response);

            if (category != null) {
                urlRepository.updateCategory(urlId, category.trim());
                log.info("AI-tagged URL {} as category: {}", urlId, category.trim());
            }
        } catch (Exception e) {
            log.warn("AI categorization failed for URL {}: {}", urlId, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String extractCategory(Map<String, Object> response) {
        try {
            var choices = (List<Map<String, Object>>) response.get("choices");
            var message = (Map<String, Object>) choices.get(0).get("message");
            String text = (String) message.get("content");
            return text.replaceAll("[^a-zA-Z ]", "").trim();
        } catch (Exception e) {
            return null;
        }
    }
}