package com.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UrlAccessDeniedException extends RuntimeException {
    public UrlAccessDeniedException(String shortCode) {
        super("You do not have access to: " + shortCode);
    }
}