package com.urlshortener.service;

import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.entity.Url;
import com.urlshortener.entity.User;
import com.urlshortener.exception.InvalidUrlException;
import com.urlshortener.exception.UrlExpiredException;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.util.ShortCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlService {

    private static final int MAX_CODE_ATTEMPTS = 5;

    private final UrlRepository urlRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final UrlCacheService urlCacheService;
    private final AiTaggingService aiTaggingService;

    @Value("${app.shortener.base-url}")
    private String baseUrl;

    // Deliberately NOT @Transactional: each save() commits on its own, so a
    // duplicate-code failure can be caught and retried with a fresh code.
    // It also means the row is committed before the async AI tagging starts.
    public UrlResponse createShortUrl(CreateUrlRequest request, User user) {
        validateUrl(request.getOriginalUrl());

        String originalUrl = request.getOriginalUrl().trim();
        LocalDateTime expiresAt = request.getExpiresInDays() != null
                ? LocalDateTime.now().plusDays(request.getExpiresInDays())
                : null;

        for (int attempt = 1; attempt <= MAX_CODE_ATTEMPTS; attempt++) {
            Url url = Url.builder()
                    .shortCode(shortCodeGenerator.generate())
                    .originalUrl(originalUrl)
                    .user(user)
                    .expiresAt(expiresAt)
                    .build();
            try {
                Url saved = urlRepository.save(url);
                aiTaggingService.categorizeUrl(saved.getId(), saved.getOriginalUrl());
                return toResponse(saved);
            } catch (DataIntegrityViolationException e) {
                log.warn("Short code collision on attempt {}/{}, retrying", attempt, MAX_CODE_ATTEMPTS);
            }
        }
        throw new IllegalStateException("Could not generate a unique short code");
    }

    public UrlResponse getOriginalUrlAndTrack(String shortCode) {
        UrlResponse url = urlCacheService.getCachedUrl(shortCode);

        if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UrlExpiredException(shortCode);
        }

        urlRepository.incrementClickCountByShortCode(shortCode);

        return url;
    }

    @Transactional(readOnly = true)
    public List<UrlResponse> getUserUrls(Long userId) {
        return urlRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateUrl(String raw) {
        try {
            URI uri = new URI(raw.trim());
            String scheme = uri.getScheme();
            boolean ok = uri.getHost() != null && scheme != null
                    && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
            if (!ok) {
                throw new InvalidUrlException("Only http(s) URLs with a valid host are allowed");
            }
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("Malformed URL");
        }
    }

    private UrlResponse toResponse(Url url) {
        return UrlResponse.builder()
                .shortCode(url.getShortCode())
                .shortUrl(baseUrl + "/" + url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .clickCount(url.getClickCount())
                .expiresAt(url.getExpiresAt())
                .category(url.getCategory())
                .createdAt(url.getCreatedAt())
                .build();
    }
}