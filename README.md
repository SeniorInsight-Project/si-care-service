# backend-core

Initial backend setup for SeniorInsight healthcare platform using Java, Spring Boot, PostgreSQL, Maven, and Docker.

## Tech Stack
- Java 21
- Spring Boot 3.5.6
- PostgreSQL
- Maven
- Docker / Docker Compose

## Running locally
1. Copy environment template:
   ```bash
   cp .env.example .env
   ```
2. Start services:
   ```bash
   docker compose up --build
   ```
3. Check health endpoint:
   - `http://localhost:8080/api/v1/health`

## Running with Maven
```bash
mvn spring-boot:run
```
