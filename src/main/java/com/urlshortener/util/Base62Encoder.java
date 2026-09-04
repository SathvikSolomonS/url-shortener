package com.urlshortener.util;

import org.springframework.stereotype.Component;

@Component
public class Base62Encoder {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length();

    /**
     * Converts a numeric ID (e.g. a database auto-increment ID) into a short,
     * URL-safe string using Base62 (0-9, A-Z, a-z).
     *
     * Example: 125 -> "21" (much shorter than the decimal representation
     * once IDs get large, since we have 62 symbols instead of 10).
     */
    public String encode(long id) {
        if (id == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }

        StringBuilder sb = new StringBuilder();
        long value = id;

        while (value > 0) {
            int remainder = (int) (value % BASE);
            sb.append(ALPHABET.charAt(remainder));
            value /= BASE;
        }

        return sb.reverse().toString();
    }

    /**
     * Reverses the encoding — converts a short code back into its numeric ID.
     * Useful if you ever need to look up by ID after decoding a short code,
     * though in this project we primarily look up short_code directly via the DB index.
     */
    public long decode(String shortCode) {
        long value = 0;

        for (char c : shortCode.toCharArray()) {
            int digit = ALPHABET.indexOf(c);
            if (digit == -1) {
                throw new IllegalArgumentException("Invalid character in short code: " + c);
            }
            value = value * BASE + digit;
        }

        return value;
    }
}