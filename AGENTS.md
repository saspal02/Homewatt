# AGENTS.md — Guidance for automated coding/operational agents

Purpose: Give AI agents the minimal, actionable knowledge to build, run, and inspect the Home Energy Tracker microservices.

Quick plan for agents
- Bring up infra (MySQL, Kafka, InfluxDB, Mailpit, Kafka UI, Keycloak, Prometheus, Grafana)
- Build or run a single service locally
- Exercise the ingestion → usage → alert pipeline and verify side-effects

Quick start (infra)
- From repo root: `docker compose up -d` (uses `docker-compose.yml`)
- Stop: `docker compose down`
- If DB issues occur: remove/recreate volumes or re-run `docker/mysql/init.sql`

Build / run a service
- Build: `cd <service> && ./mvnw package` (each microservice has its own Maven wrapper)
- Run artifact: `java -jar target/<artifact>.jar` or `./mvnw spring-boot:run`

Architecture & key components
- Microservices (top-level dirs): `alert-service`, `api-gateway`, `device-service`, `ingestion-service`, `insight-service`, `usage-service`, `user-service`.
- Stack: **Spring Boot 4** + **Java 21** for most services; **insight-service** uses **Spring Boot 3.5** with Spring AI (Ollama). Spring Cloud 2025.1.0 on the gateway (Gateway Server WebMVC, Resilience4j circuit breakers). No Spring Cloud Config Server in this repo—config is per-service `application.properties`.
- Important infra files: `docker-compose.yml`, `docker/mysql/init.sql`, `docker/prometheus/prometheus.yml`, `docker/grafana/provisioning/`, `docker/kafka_data/`, `influxdb_data/`.
- Human-oriented architecture diagrams: `diagrams/*.png` (see top-level `README.md`).

Critical integration points & dataflows (explicit)
- Ingestion → Kafka → Usage → InfluxDB & Alerts → Alerting consumer
  - Topic `energy-usage`: produced by `ingestion-service` (`ingestion-service/src/main/java/com/homewatt/ingestion_service/service/IngestionService.java`) and consumed by `usage-service` (`usage-service/src/main/java/com/homewatt/usage_service/service/UsageService.java`).
  - Topic `energy-alerts`: produced by `usage-service` (aggregation/threshold logic) and consumed by `alert-service` (`alert-service/src/main/java/com/homewatt/alert_service/service/AlertService.java`).
- InfluxDB usage: `usage-service/src/main/java/com/homewatt/usage_service/config/InfluxDBConfig.java` and writes/queries in `UsageService.java`.
- MySQL: DB name `home_energy_tracker`, init in `docker/mysql/init.sql`; JDBC URLs appear in services' `src/main/resources/application.properties`. Keycloak uses a separate MySQL instance in Compose (`keycloak-db`).

API Gateway (routing & security)
- Gateway port **9000**; routes under `/api/v1/...` proxy to localhost backends with circuit breakers.
- OAuth2 Resource Server (JWT): most paths require a valid token; excluded URLs are defined in `application.properties`.
- `/api/v1/ingestion/**` is NOT excluded → requires Bearer token when going through gateway.
- For quick tests: call ingestion-service directly on port 8082 (no Spring Security).

Observability & useful endpoints
- Kafka UI: http://localhost:8070
- Mailpit: SMTP 1025, UI http://localhost:8025
- InfluxDB: http://localhost:8072 (bucket: usage-bucket)
- Keycloak: http://localhost:8091 (admin/admin)
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000

Service ports:
- user-service 8080
- device-service 8081
- ingestion-service 8082
- usage-service 8083
- alert-service 8084
- insight-service 8085
- api-gateway 9000

Example agent actions
- Direct ingestion test (no JWT):
```bash
curl -X POST http://localhost:8082/api/v1/ingestion \
  -H "Content-Type: application/json" \
  -d '{"deviceId":"dev-1","timestamp":"2026-01-01T12:00:00Z","watts":1200}'