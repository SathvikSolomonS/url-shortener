package com.urlshortener;

import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.entity.Url;
import com.urlshortener.entity.User;
import com.urlshortener.exception.InvalidUrlException;
import com.urlshortener.exception.UrlExpiredException;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.service.AiTaggingService;
import com.urlshortener.service.UrlCacheService;
import com.urlshortener.service.UrlService;
import com.urlshortener.util.ShortCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
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
    private ShortCodeGenerator shortCodeGenerator;

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

    private Url savedUrlWithCode(String code) {
        return Url.builder()
                .id(1L)
                .shortCode(code)
                .originalUrl("https://www.example.com")
                .user(testUser)
                .clickCount(0L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createShortUrl_savesOnceAndReturnsGeneratedCode() {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        when(shortCodeGenerator.generate()).thenReturn("aZ3kP9x");
        when(urlRepository.save(any(Url.class))).thenReturn(savedUrlWithCode("aZ3kP9x"));

        UrlResponse response = urlService.createShortUrl(request, testUser);

        assertNotNull(response);
        assertEquals("aZ3kP9x", response.getShortCode());
        assertEquals("http://localhost:8080/aZ3kP9x", response.getShortUrl());
        assertEquals("https://www.example.com", response.getOriginalUrl());
        verify(urlRepository, times(1)).save(any(Url.class));
    }

    @Test
    void createShortUrl_retriesWithNewCodeOnCollision() {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        when(shortCodeGenerator.generate()).thenReturn("dup1111", "ok22222");
        when(urlRepository.save(any(Url.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate short_code"))
                .thenReturn(savedUrlWithCode("ok22222"));

        UrlResponse response = urlService.createShortUrl(request, testUser);

        assertEquals("ok22222", response.getShortCode());
        verify(shortCodeGenerator, times(2)).generate();
        verify(urlRepository, times(2)).save(any(Url.class));
    }

    @Test
    void createShortUrl_givesUpAfterFiveCollisions() {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        when(shortCodeGenerator.generate()).thenReturn("dup1111");
        when(urlRepository.save(any(Url.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate short_code"));

        assertThrows(IllegalStateException.class,
                () -> urlService.createShortUrl(request, testUser));

        verify(urlRepository, times(5)).save(any(Url.class));
        verify(aiTaggingService, never()).categorizeUrl(any(), any());
    }

    @Test
    void createShortUrl_rejectsNonHttpSchemes() {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("javascript:alert(1)");

        assertThrows(InvalidUrlException.class,
                () -> urlService.createShortUrl(request, testUser));

        verify(urlRepository, never()).save(any());
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