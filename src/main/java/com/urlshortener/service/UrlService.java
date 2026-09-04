package com.urlshortener.service;

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
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;
    private final Base62Encoder base62Encoder;
    private final UrlCacheService urlCacheService;

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

        return toResponse(saved);
    }

    @Transactional
    public UrlResponse getOriginalUrlAndTrack(String shortCode) {
        UrlResponse response = urlCacheService.getCachedUrl(shortCode);

        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new NoSuchElementException("Short URL not found: " + shortCode));
        urlRepository.incrementClickCount(url.getId());

        return response;
    }

    private UrlResponse toResponse(Url url) {
        return UrlResponse.builder()
                .shortCode(url.getShortCode())
                .shortUrl(baseUrl + "/" + url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .clickCount(url.getClickCount())
                .expiresAt(url.getExpiresAt())
                .createdAt(url.getCreatedAt())
                .build();
    }
}