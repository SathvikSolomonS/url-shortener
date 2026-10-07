package com.urlshortener.service;

import com.urlshortener.entity.ClickEvent;
import com.urlshortener.entity.Url;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClickEventService {

    private final ClickEventRepository clickEventRepository;
    private final UrlRepository urlRepository;

    // Async: the redirect has already happened by the time this runs, and a
    // failure here never affects the user being redirected.
    @Async
    public void recordClick(Long urlId, String ipAddress, String userAgent, String referrer) {
        try {
            Url urlRef = urlRepository.getReferenceById(urlId);

            ClickEvent event = ClickEvent.builder()
                    .url(urlRef)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .build();

            clickEventRepository.save(event);
        } catch (Exception e) {
            log.warn("Failed to record click event for URL {}: {}", urlId, e.getMessage());
        }
    }
}