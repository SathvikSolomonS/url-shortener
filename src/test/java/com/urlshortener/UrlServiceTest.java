package com.urlshortener;

import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.entity.Url;
import com.urlshortener.entity.User;
import com.urlshortener.exception.UrlExpiredException;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.service.AiTaggingService;
import com.urlshortener.service.UrlCacheService;
import com.urlshortener.service.UrlService;
import com.urlshortener.util.Base62Encoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private Base62Encoder base62Encoder;

    @Mock
    private UrlCacheService urlCacheService;

    @Mock
    private AiTaggingService aiTaggingService;

    @InjectMocks
    private UrlService urlService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(urlService, "baseUrl", "http://localhost:8080");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
    }

    @Test
    void createShortUrl_savesUrlAndReturnsResponseWithShortCode() {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        Url savedUrl = Url.builder()
                .id(1L)
                .shortCode("PENDING")
                .originalUrl("https://www.example.com")
                .user(testUser)
                .clickCount(0L)
                .createdAt(LocalDateTime.now())
                .build();

        when(urlRepository.save(any(Url.class))).thenReturn(savedUrl);
        when(base62Encoder.encode(1L)).thenReturn("1");

        UrlResponse response = urlService.createShortUrl(request, testUser);

        assertNotNull(response);
        assertEquals("https://www.example.com", response.getOriginalUrl());
        verify(urlRepository, times(2)).save(any(Url.class)); // once for insert, once to set the real short code
    }

    @Test
    void getOriginalUrlAndTrack_throwsNotFoundWhenShortCodeMissing() {
        when(urlCacheService.getCachedUrl("missing"))
                .thenThrow(new UrlNotFoundException("missing"));

        assertThrows(UrlNotFoundException.class,
                () -> urlService.getOriginalUrlAndTrack("missing"));

        verify(urlRepository, never()).incrementClickCountByShortCode(any());
    }

    @Test
    void getOriginalUrlAndTrack_incrementsClickCountOnSuccess() {
        UrlResponse cachedResponse = UrlResponse.builder()
                .shortCode("abc123")
                .originalUrl("https://www.example.com")
                .build();

        when(urlCacheService.getCachedUrl("abc123")).thenReturn(cachedResponse);

        UrlResponse result = urlService.getOriginalUrlAndTrack("abc123");

        assertEquals("https://www.example.com", result.getOriginalUrl());
        verify(urlRepository, times(1)).incrementClickCountByShortCode("abc123");
    }

    @Test
    void getOriginalUrlAndTrack_throwsWhenLinkExpired_evenIfCached() {
        UrlResponse expiredResponse = UrlResponse.builder()
                .shortCode("old123")
                .originalUrl("https://www.example.com")
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(urlCacheService.getCachedUrl("old123")).thenReturn(expiredResponse);

        assertThrows(UrlExpiredException.class,
                () -> urlService.getOriginalUrlAndTrack("old123"));

        verify(urlRepository, never()).incrementClickCountByShortCode(any());
    }
}