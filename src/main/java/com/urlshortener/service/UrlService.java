package com.urlshortener.service;

import com.urlshortener.exception.UrlExpiredException;
import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.entity.Url;
import com.urlshortener.entity.User;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.util.Base62Encoder;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

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
        Url url = Url.builder()
                .shortCode("PENDING")
                .originalUrl(request.getOriginalUrl())
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

    @Transactional
    public UrlResponse getOriginalUrlAndTrack(String shortCode) {
      UrlResponse url = urlCacheService.getCachedUrl(shortCode);

        if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UrlExpiredException(shortCode);
        }

        urlRepository.incrementClickCountByShortCode(shortCode);

        return url;
    }

    public List<UrlResponse> getUserUrls(Long userId) {
        return urlRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UrlResponse> getUrlsForUser(User user) {
        return urlRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
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