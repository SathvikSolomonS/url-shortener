# URL Shortener — Spring Boot Backend

A production-style URL shortening service built with Spring Boot, featuring Redis caching, JWT authentication, rate limiting, atomic concurrency handling, and full Docker containerization with CI/CD.

![CI](https://github.com/SathvikSolomonS/url-shortener/actions/workflows/ci.yml/badge.svg)

## Features

- **URL shortening** using Base62 encoding derived from database auto-increment IDs
- **Redis caching** (cache-aside pattern) on the redirect lookup path, with TTL-based expiry
- **Rate limiting** via atomic Redis counters (token-bucket style, per-IP)
- **JWT authentication** — registration, login, BCrypt password hashing, stateless token validation
- **Atomic click tracking** — race-condition-safe click counting using a single atomic SQL update instead of read-then-write
- **Flyway-managed database migrations** — versioned schema, no manual DDL
- **Dockerized** — full stack (app + MySQL + Redis) runs with one command via Docker Compose
- **CI/CD pipeline** — GitHub Actions runs the full test suite and builds the Docker image on every push
- **Unit tested** — service-layer logic tested in isolation with Mockito

## Tech Stack

- **Backend:** Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA
- **Database:** MySQL 8, Flyway migrations
- **Caching / Rate Limiting:** Redis
- **Auth:** JWT (JJWT library), BCrypt
- **Testing:** JUnit 5, Mockito
- **DevOps:** Docker, Docker Compose, GitHub Actions

## Architecture
Client
│
▼
┌─────────────────┐ ┌──────────┐
│ Rate Limit │────▶│ Redis │ (rate limit counters, cache)
│ Filter │ └──────────┘
└─────────────────┘
│
▼
┌─────────────────┐
│ JWT Auth Filter │
└─────────────────┘
│
▼
┌─────────────────┐ ┌──────────┐
│ Controllers │────▶│ MySQL │ (users, urls, click_events)
│ → Services │ └──────────┘
│ → Repositories │
└─────────────────┘

## Getting Started

### Run with Docker (recommended — one command, no setup)

```bash
git clone https://github.com/SathvikSolomonS/url-shortener.git
cd url-shortener
docker compose up --build
```

The app will be available at `http://localhost:8080`.

### API Examples

**Register:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "demo", "email": "demo@example.com", "password": "password123"}'
```

**Create a short URL** (use the token from registration):
```bash
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{"originalUrl": "https://www.example.com"}'
```

**Use the short URL:**
```bash
curl -I http://localhost:8080/<shortCode>
```

## Key Design Decisions

- **Atomic click increment at the DB level** rather than read-modify-write in application code — avoids lost updates under concurrent clicks.
- **Redis cache is separated from the click-tracking write path** — reads hit the cache, but click counts are always updated against the source of truth (MySQL), keeping analytics accurate.
- **Stateless JWT auth** (`SessionCreationPolicy.STATELESS`) — no server-side session storage, so the app can scale horizontally without sticky sessions.
- **Multi-stage Docker build** — final image ships only the compiled JAR on a minimal JRE base, not the full Maven/JDK build toolchain.

## Running Tests

```bash
mvn test
```

## What's Next

- Split into microservices (URL service, Analytics service, Auth service) behind an API gateway with Kafka-based async analytics — see [url-shortener-microservices](#) *(planned second project)*
- Integration tests with Testcontainers
- API documentation via Swagger/OpenAPI

## License

MIT