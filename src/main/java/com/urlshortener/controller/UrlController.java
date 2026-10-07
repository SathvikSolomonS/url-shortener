package com.urlshortener.controller;

import com.urlshortener.dto.ClickAnalyticsResponse;
import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.security.CustomUserDetails;
import com.urlshortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    @PostMapping("/api/urls")
    public ResponseEntity<UrlResponse> createShortUrl(
            @Valid @RequestBody CreateUrlRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        UrlResponse response = urlService.createShortUrl(request, currentUser.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/urls")
    public ResponseEntity<List<UrlResponse>> getMyUrls(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        List<UrlResponse> urls = urlService.getUserUrls(currentUser.getUser().getId());
        return ResponseEntity.ok(urls);
    }

    @GetMapping("/api/urls/{shortCode}/analytics")
    public ResponseEntity<List<ClickAnalyticsResponse>> getAnalytics(@PathVariable String shortCode) {
        return ResponseEntity.ok(urlService.getClickAnalytics(shortCode));
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        String referrer = request.getHeader("Referer");

        UrlResponse url = urlService.getOriginalUrlAndTrack(shortCode, ipAddress, userAgent, referrer);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url.getOriginalUrl()))
                .build();
    }
}