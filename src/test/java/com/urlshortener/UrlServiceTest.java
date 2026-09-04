package com.urlshortener;

import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.entity.Url;
import com.urlshortener.entity.User;
import com.urlshortener.repository.UrlRepository;
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
import java.util.NoSuchElementException;
import java.util.Optional;

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
    void getOriginalUrlAndTrack_throwsExceptionWhenShortCodeNotFound() {
        when(urlCacheService.getCachedUrl("missing"))
                .thenThrow(new NoSuchElementException("Short URL not found: missing"));

        assertThrows(NoSuchElementException.class,
                () -> urlService.getOriginalUrlAndTrack("missing"));
    }

    @Test
    void getOriginalUrlAndTrack_incrementsClickCountOnSuccess() {
        UrlResponse cachedResponse = UrlResponse.builder()
                .shortCode("abc123")
                .originalUrl("https://www.example.com")
                .build();

        Url foundUrl = Url.builder().id(5L).shortCode("abc123").build();

        when(urlCacheService.getCachedUrl("abc123")).thenReturn(cachedResponse);
        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(foundUrl));

        UrlResponse result = urlService.getOriginalUrlAndTrack("abc123");

        assertEquals("https://www.example.com", result.getOriginalUrl());
        verify(urlRepository, times(1)).incrementClickCount(5L);
    }
}