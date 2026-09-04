package com.urlshortener;

import com.urlshortener.util.Base62Encoder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTest {

    private final Base62Encoder encoder = new Base62Encoder();

    @Test
    void encode_zeroReturnsFirstCharacter() {
        assertEquals("0", encoder.encode(0));
    }

    @Test
    void encode_smallNumberProducesShortCode() {
        String code = encoder.encode(125);
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    @Test
    void encodeThenDecode_returnsOriginalId() {
        long originalId = 123456789L;
        String encoded = encoder.encode(originalId);
        long decoded = encoder.decode(encoded);
        assertEquals(originalId, decoded);
    }

    @Test
    void encode_differentIdsProduceDifferentCodes() {
        String code1 = encoder.encode(1L);
        String code2 = encoder.encode(2L);
        assertNotEquals(code1, code2);
    }

    @Test
    void decode_invalidCharacterThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> encoder.decode("!!!invalid!!!"));
    }
}