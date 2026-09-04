-- Users table: for authentication
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- URLs table: the shortened links themselves
CREATE TABLE urls (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_code VARCHAR(10) NOT NULL UNIQUE,
    original_url VARCHAR(2048) NOT NULL,
    user_id BIGINT NOT NULL,
    click_count BIGINT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_urls_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Index for fast redirect lookups (this is the most frequent query in the whole app)
CREATE INDEX idx_urls_short_code ON urls(short_code);

-- Click events table: analytics data, one row per click
CREATE TABLE click_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    url_id BIGINT NOT NULL,
    clicked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(512),
    referrer VARCHAR(2048),
    CONSTRAINT fk_click_events_url FOREIGN KEY (url_id) REFERENCES urls(id) ON DELETE CASCADE
);

-- Index for fetching a URL's analytics fast
CREATE INDEX idx_click_events_url_id ON click_events(url_id);