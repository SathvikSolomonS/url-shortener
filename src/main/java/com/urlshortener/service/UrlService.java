package com.urlshortener.service;

import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.entity.Url;
import com.urlshortener.entity.User;
import com.urlshortener.exception.InvalidUrlException;
import com.urlshortener.exception.UrlExpiredException;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;
    private final Base62Encoder base62Encoder;
    private final UrlCacheService urlCacheService;
    private final AiTaggingService aiTaggingService;

    @Value("${app.shortener.base-url}")
    private String baseUrl;

    @Transactional
    public UrlResponse createShortUrl(CreateUrlRequest request, User user) {
        validateUrl(request.getOriginalUrl());

        Url url = Url.builder()
                .shortCode("PENDING")
                .originalUrl(request.getOriginalUrl().trim())
                .user(user)
                .expiresAt(request.getExpiresInDays() != null
                        ? LocalDateTime.now().plusDays(request.getExpiresInDays())
                        : null)
                .build();

        Url saved = urlRepository.save(url);

        String shortCode = base62Encoder.encode(saved.getId());
        saved.setShortCode(shortCode);
        urlRepository.save(saved);

        aiTaggingService.categorizeUrl(saved.getId(), saved.getOriginalUrl());

        return toResponse(saved);
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