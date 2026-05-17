# HomeWatt — Home Energy Tracker

A microservices-based home energy monitoring platform built with **Java 25**, **Spring Boot 4**, **Spring Cloud Gateway**, **Kafka**, **InfluxDB**, **MySQL**, and **Keycloak**.

## Architecture

```
                    ┌───────────────────────┐
                    │     api-gateway        │
                    │  (port 9000)           │
                    │  JWT Auth (Keycloak)   │
                    │  Circuit Breakers      │
                    └──┬──┬──┬──┬──┬──┬─────┘
                       │  │  │  │  │  │
        ┌──────────────┘  │  │  │  │  └──────────────┐
        v                 v  │  v  v                 v
  user-service    device-svc │  insight-svc    ┌──────────┐
  (8080)           (8081)   │  (8085)          │ ingestion│
  /api/v1/user    /api/v1/  │  /api/v1/insight │ (8082)   │
        │          device   │                  └────┬─────┘
        └──────────┬────────┘                       │
                   v                                v
           ┌───────┴────────┐             ┌──────────┴──┐
           │  usage-service  │◄──Kafka────│ energy-usage │
           │  (8083)         │   topic     │  topic       │
           │  /api/v1/usage  │             └─────────────┘
           └──┬───────┬─────┘
              │       │
         (writes)  (reads)
              v       v
         ┌────────┐ ┌────────┐
         │InfluxDB│ │ MySQL  │
         │:8072   │ │:3306   │
         └────────┘ └────────┘
              │
              │ (every 10s aggregation)
              v
         ┌──────────┐
         │energy-   │◄──Kafka topic
         │alerts    │
         └────┬─────┘
              v
        ┌─────┴──────┐
        │alert-svc   │
        │(8084)      │
        ├────────────┤
        │ MySQL      │
        │ Mailpit    │ (SMTP)
        └────────────┘
```

### Services

| Service | Port | Description |
|---------|------|-------------|
| **user-service** | 8080 | User CRUD (name, email, address, alert thresholds) |
| **device-service** | 8081 | Device CRUD (name, type, location, user association) |
| **ingestion-service** | 8082 | REST endpoint for energy readings → Kafka producer (topic: `energy-usage`) |
| **usage-service** | 8083 | Kafka consumer (`energy-usage`), InfluxDB writes/reads, scheduled aggregation, threshold check → Kafka producer (topic: `energy-alerts`) |
| **alert-service** | 8084 | Kafka consumer (`energy-alerts`), sends email via Mailpit, persists alerts in MySQL |
| **insight-service** | 8085 | REST calls to usage-service + Google Gemini AI for energy-saving tips & overviews |
| **api-gateway** | 9000 | Spring Cloud Gateway with OAuth2 JWT (Keycloak) + Resilience4j circuit breakers |

### Data Flow

1. **Ingestion**: `POST /api/v1/ingestion` (direct on 8082 or via gateway on 9000) → Kafka topic `energy-usage`
2. **Processing**: `usage-service` consumes `energy-usage`, stores raw data in **InfluxDB**, aggregates every 10s, checks user thresholds from `user-service`/`device-service` via REST
3. **Alerting**: If threshold exceeded → Kafka topic `energy-alerts` → `alert-service` sends email via **Mailpit**
4. **Insights**: `insight-service` fetches usage history from `usage-service`, then calls **Google Gemini** for natural-language energy-saving tips

## Prerequisites

- Docker & Docker Compose
- Java 25 (for local service runs)
- Maven wrapper (bundled per service)

## Quick Start

```bash
# 1. Start infrastructure (MySQL, Kafka, InfluxDB, Keycloak, etc.)
docker compose up -d

# 2. Build a service (e.g., user-service)
cd user-service && ./mvnw package -DskipTests

# 3. Run the service
java -jar target/user-service-0.0.1-SNAPSHOT.jar

# Or run with Maven
./mvnw spring-boot:run
```

### Set up Keycloak

1. Open `http://localhost:8091` (admin/admin)
2. Create a realm named `homewatt`
3. Create a client (e.g., `homewatt-client`) with OIDC flow
4. Create test users and assign roles

### Set up Gemini API Key (insight-service)

```bash
export GEMINI_API_KEY=your-key-here
```

Or configure it in `insight-service/src/main/resources/application.properties`.

## Testing the Pipeline

```bash
# Direct ingestion (no JWT) — bypasses gateway
curl -X POST http://localhost:8082/api/v1/ingestion \
  -H "Content-Type: application/json" \
  -d '{"deviceId":"dev-1","timestamp":"2026-01-01T12:00:00Z","watts":1200}'

# Check Kafka UI: http://localhost:8070
# Check Mailpit (alerts): http://localhost:8025
# Check Grafana: http://localhost:3000 (admin/admin)
```

## Monitoring

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin) — pre-provisioned dashboards
- **Kafka UI**: http://localhost:8070
- **InfluxDB UI**: http://localhost:8072 (admin/admin123)

Each service exposes metrics at `/actuator/prometheus`.

## Infrastructure

| Component | Image | Port(s) |
|-----------|-------|---------|
| MySQL | mysql:8.3.0 | 3306 |
| Kafka | apache/kafka:latest | 9092, 9094 |
| Kafka UI | kafbat/kafka-ui:latest | 8070 |
| InfluxDB | influxdb:2.8 | 8072 |
| Keycloak | keycloak:26.6.1 | 8091 |
| Prometheus | prom/prometheus:v3.11.0 | 9090 |
| Grafana | grafana/grafana:13.0.1 | 3000 |
| Mailpit | axllent/mailpit:latest | 1025 (SMTP), 8025 (UI) |

## Tech Stack

- **Java 25**, Spring Boot 4.0.6, Spring Cloud 2025.1.1
- **Kafka** for async event streaming
- **InfluxDB 2.x** for time-series energy data
- **MySQL 8.3** for relational data
- **Keycloak 26** for OAuth2/OIDC authentication
- **Spring AI** with Google Gemini for AI insights
- **Micrometer/Prometheus + Grafana** for monitoring
- **Mailpit** for dev email capture
- **Resilience4j** circuit breakers on gateway
