package com.urlshortener.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateUrlRequest {

    @NotBlank(message = "Original URL must not be blank")
    @Size(max = 2048, message = "URL must be at most 2048 characters")
    private String originalUrl;

    // Optional: days until the link expires. Null = never expires.
    @Positive(message = "expiresInDays must be positive")
    @Max(value = 3650, message = "expiresInDays must be at most 3650")
    private Integer expiresInDays;
}