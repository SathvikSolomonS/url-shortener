package com.urlshortener.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfrastructureSmokeTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void flywayMigrationsRanAgainstRealMySql() {
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class);

        assertNotNull(applied);
        assertTrue(applied >= 3, "expected the V1, V2 and V3 migrations to be applied");
    }

    @Test
    void realRedisIsReachable() {
        redisTemplate.opsForValue().set("smoke-test", "ok");

        assertEquals("ok", redisTemplate.opsForValue().get("smoke-test"));
    }
}
