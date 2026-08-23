<div align="center">

# 🛡️ AI Incident Commander

### Autonomous AI-Powered Incident Management Platform

[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 4.1](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![React 19](https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![Apache Kafka](https://img.shields.io/badge/Kafka-7.6-231F20?style=for-the-badge&logo=apachekafka&logoColor=white)](https://kafka.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![Gemini AI](https://img.shields.io/badge/Gemini-2.5_Flash-4285F4?style=for-the-badge&logo=google&logoColor=white)](https://ai.google.dev/)
[![Grafana](https://img.shields.io/badge/Grafana-Tempo_%7C_Loki-F46800?style=for-the-badge&logo=grafana&logoColor=white)](https://grafana.com/)
[![OpenTelemetry](https://img.shields.io/badge/OpenTelemetry-Collector-425CC7?style=for-the-badge&logo=opentelemetry&logoColor=white)](https://opentelemetry.io/)
[![Swagger](https://img.shields.io/badge/OpenAPI-Swagger_UI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)](https://swagger.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](https://opensource.org/licenses/MIT)

> **An event-driven, CQRS-based platform where AI agents autonomously investigate production incidents, diagnose root causes via live telemetry, and propose remediations — all with human-in-the-loop approval before any action executes.**

[Features](#-key-features) · [Architecture](#-high-level-design-hld) · [Low-Level Design](#-low-level-design-lld) · [Security](#-security) · [Getting Started](#-getting-started) · [API Reference](#-api-reference) · [API Docs (Swagger)](#-api-documentation-swagger--openapi) · [Tech Stack](#%EF%B8%8F-tech-stack)

</div>

---

## 📋 Table of Contents

- [Key Features](#-key-features)
- [High-Level Design (HLD)](#-high-level-design-hld)
  - [System Architecture Diagram](#system-architecture-overview)
  - [Event-Driven Flow](#event-driven-flow)
  - [CQRS Pattern](#cqrs-pattern)
- [Low-Level Design (LLD)](#-low-level-design-lld)
  - [Domain Models & ER Diagram](#domain-models--er-diagram)
  - [Incident Lifecycle State Machine](#incident-lifecycle-state-machine)
  - [Action Lifecycle State Machine](#action-lifecycle-state-machine)
  - [Kafka Topic Architecture](#kafka-topic-architecture)
  - [AI Agent Reasoning Pipeline](#ai-agent-reasoning-pipeline)
  - [Circuit Breaker & Resilience](#circuit-breaker--resilience-pattern)
  - [Dead Letter Queue (DLQ) Pipeline](#dead-letter-queue-dlq-pipeline)
- [Service Breakdown](#-service-breakdown)
- [🔐 Security](#-security)
  - [Authentication Flow](#authentication-flow)
  - [What's Protected](#whats-protected)
  - [Security Configuration](#security-configuration)
- [Getting Started](#-getting-started)
- [API Reference](#-api-reference)
- [API Documentation (Swagger / OpenAPI)](#-api-documentation-swagger--openapi)
- [Frontend Pages](#-frontend-pages)
- [Tech Stack](#%EF%B8%8F-tech-stack)

---

## ✨ Key Features

| Feature | Description |
|:---|:---|
| 🤖 **Autonomous AI Investigation** | Gemini 2.5 Flash + Groq LLaMA 3.3 agents automatically investigate incidents using diagnostic tools |
| 🔄 **CQRS Architecture** | Strict command/query separation with Kafka event sourcing for scalability and auditability |
| ✅ **Human-in-the-Loop** | AI proposes actions; humans approve/reject before execution. No autonomous execution. |
| 🐳 **Docker Execution Engine** | Approved actions (restart, scale) execute against real Docker containers via Docker socket |
| 📊 **Live Prometheus Telemetry** | Real-time error rate & latency metrics with Resilience4j circuit breaker fallback to Redis cache |
| 📧 **Email Notifications** | Automated alerts for proposed actions, rollbacks, and escalations via SMTP |
| 🔌 **Real-Time WebSocket** | STOMP over WebSocket pushes live incident updates to the React dashboard |
| 💀 **Dead Letter Queue Admin** | Failed Kafka events are persisted to MySQL and can be inspected/replayed from the UI |
| 🛡️ **Idempotency Protection** | Redis-backed idempotency keys prevent duplicate approve/execute operations |
| 🚦 **Rate Limiting** | Per-client token bucket (Bucket4j) at the Gateway — 20 requests/min by default, in-memory, no Redis dependency |
| 🔁 **Rollback & Escalation** | Failed remediations auto-generate compensating rollback actions; unresolvable incidents escalate |
| 📡 **Full Observability Stack** | OpenTelemetry Collector ships traces/logs from every service to Tempo + Loki; Grafana dashboards unify metrics, traces, and logs |
| 🔗 **Cross-Project Scaling** | `SCALE_WORKER_PODS` reads Docker Compose labels off the target container to scale services in *other* Compose projects (e.g. [Distributed Job Forge](../distributed-job-forge)) via a host-path mapping config |
| 🔐 **JWT Authentication** | Stateless, BCrypt + JWT-secured login at the API Gateway; every downstream call from the SPA carries a signed `Bearer` token |
| 🧾 **Centralized Swagger / OpenAPI** | Each service exposes its own `springdoc-openapi` spec; the Gateway's Swagger UI aggregates all four into a single interactive API console |



---

## 🏗️ High-Level Design (HLD)

### System Architecture Overview

```mermaid
graph TB
    subgraph Client["🖥️ Client Layer"]
        UI["React 19 SPA<br/>(Vite + STOMP WebSocket)"]
    end

    subgraph Gateway["🌐 API Gateway — :8080"]
        GW["Spring Cloud Gateway WebMVC<br/>Route-based Proxy"]
        SEC["Spring Security<br/>JWT Auth Filter"]
        SWG["springdoc Swagger UI<br/>Aggregated API Docs"]
    end

    subgraph Services["⚙️ Backend Microservices"]
        subgraph CS["Command Service — :8081"]
            CSC["IncidentController"]
            CSS["IncidentService"]
            CSE["KafkaEventPublisher"]
            DOCK["DockerExecutionService"]
            KADM["KafkaAdminService"]
        end

        subgraph QS["Query Service — :8082"]
            QSC["IncidentQueryController<br/>ActionQueryController<br/>DlqAdminController"]
            QSK["IncidentEventConsumer<br/>ActionEventConsumer"]
            QSWS["WebSocketEventRelay"]
            DLQ["PersistingDeadLetterRecoverer"]
        end

        subgraph AS["Agent Service — :8083"]
            ASK["IncidentEventListener"]
            ASAI["AiService<br/>(Gemini + Groq)"]
            ASTL["AgentTools<br/>(6 diagnostic tools)"]
            ASMC["MetricsClient"]
        end

        subgraph NS["Notification Service — :8084"]
            NSK["NotificationEventListener"]
            NSE["EmailNotificationService"]
        end
    end

    subgraph Infra["🏭 Infrastructure"]
        KAFKA["Apache Kafka<br/>(9 topics + 9 DLQ topics)"]
        MYSQL["MySQL 8.0<br/>(Incidents, Actions, DLQ, Notifications)"]
        REDIS["Redis 7<br/>(Cache, Idempotency, Metrics)"]
        PROM["Prometheus<br/>(Metrics Endpoint)"]
        DOCKER["Docker Engine<br/>(Container Mgmt)"]
    end

    subgraph AI["🧠 AI Providers"]
        GEMINI["Google Gemini 2.5 Flash"]
        GROQ["Groq LLaMA 3.3 70B"]
    end

    subgraph Obs["📡 Observability Stack"]
        OTEL["OTel Collector<br/>:4317 / :4318"]
        TEMPO["Grafana Tempo<br/>:3200 (Traces)"]
        LOKI["Grafana Loki<br/>:3100 (Logs)"]
        AICPROM["aic-prometheus<br/>:9091 (Metrics)"]
        GRAF["Grafana<br/>:3001 (Dashboards)"]
    end

    UI <-->|REST + STOMP| GW
    GW --> SEC
    SEC -->|validates Bearer JWT| GW
    GW -.-> SWG
    GW -->|/api/incidents/**| CS
    GW -->|/api/query/**| QS
    GW -->|/api/agent/**| AS

    CSS -->|Publish Events| KAFKA
    KAFKA -->|Consume Events| QSK
    KAFKA -->|incident.created| ASK
    KAFKA -->|action.proposed<br/>action.rolled_back<br/>incident.escalated| NSK
    QSK -->|Push Updates| QSWS
    QSWS -->|STOMP Broadcast| UI

    QSK --> MYSQL
    CSS --> MYSQL
    NSE --> MYSQL
    DLQ --> MYSQL

    CSS --> REDIS
    ASMC --> REDIS

    ASMC -->|PromQL HTTP API| PROM
    DOCK -->|Docker Socket| DOCKER
    KADM -->|Admin API| KAFKA

    ASAI -->|Primary| GEMINI
    ASAI -->|Fallback| GROQ

    ASK --> ASAI
    ASAI --> ASTL
    ASTL -->|proposeAction| CS

    NSE -->|SMTP| EMAIL["📧 Email Server"]

    CS & QS & AS & NS -.->|OTLP traces + logs| OTEL
    OTEL --> TEMPO
    OTEL --> LOKI
    GRAF -->|PromQL| AICPROM
    GRAF -->|TraceQL| TEMPO
    GRAF -->|LogQL| LOKI

    style Client fill:#1a1a2e,stroke:#e94560,color:#fff
    style Gateway fill:#16213e,stroke:#0f3460,color:#fff
    style Services fill:#0f3460,stroke:#533483,color:#fff
    style Infra fill:#1a1a2e,stroke:#e94560,color:#fff
    style AI fill:#1a1a2e,stroke:#00d2ff,color:#fff
    style Obs fill:#16213e,stroke:#F46800,color:#fff
```

---

### Event-Driven Flow

> How an incident flows through the entire system — from creation to resolution.

```mermaid
sequenceDiagram
    actor Ops as 👤 Ops Engineer
    participant UI as React Dashboard
    participant GW as API Gateway
    participant CMD as Command Service
    participant K as Apache Kafka
    participant QRY as Query Service
    participant AGT as Agent Service
    participant AI as Gemini / Groq
    participant NTF as Notification Service
    participant WS as WebSocket

    Ops->>UI: Create Incident (service, severity)
    UI->>GW: POST /api/incidents
    GW->>CMD: Forward Request
    CMD->>CMD: Save to MySQL
    CMD->>K: Publish "incident.created"
    CMD-->>GW: 201 Created
    GW-->>UI: IncidentResponse

    par Parallel Consumers
        K->>QRY: Consume "incident.created"
        QRY->>QRY: Materialize read model (MySQL)
        QRY->>WS: Push to /topic/incidents/active
        WS-->>UI: Real-time update

        K->>AGT: Consume "incident.created"
        AGT->>AI: Send investigation prompt + tools
        AI->>AGT: Tool calls (getServiceHealth, getKafkaConsumerLag, ...)
        AGT->>AGT: Execute diagnostic tools
        AI->>AGT: proposeAction(actionType, rationale)
        AGT->>CMD: POST /api/incidents/{id}/actions
        CMD->>K: Publish "action.proposed"
    end

    K->>NTF: Consume "action.proposed"
    NTF->>NTF: Send email notification
    K->>QRY: Consume "action.proposed"
    QRY->>WS: Push to /topic/incidents/{id}
    WS-->>UI: Show proposed action

    Ops->>UI: Approve Action
    UI->>GW: POST /api/incidents/{id}/actions/{actionId}/approve
    GW->>CMD: Forward (X-Idempotency-Key header)
    CMD->>CMD: Check idempotency → Save approval
    CMD->>K: Publish "action.approved"

    Ops->>UI: Execute Action
    UI->>GW: POST /api/incidents/{id}/actions/{actionId}/execute
    GW->>CMD: Forward
    CMD->>CMD: Docker restart / scale / clear DLQ
    CMD->>K: Publish "action.executed"

    Ops->>UI: Resolve Incident
    UI->>GW: PATCH /api/incidents/{id}/status
    GW->>CMD: Forward {targetStatus: "RESOLVED"}
    CMD->>K: Publish "incident.resolved"
```

---

### CQRS Pattern

```mermaid
graph LR
    subgraph Write["✏️ Write Side (Command Service :8081)"]
        W1["POST /api/incidents"]
        W2["POST .../approve"]
        W3["POST .../execute"]
        W4["POST .../rollback"]
        W5["PATCH .../status"]
    end

    subgraph Bus["📨 Event Bus (Kafka)"]
        E1["incident.created"]
        E2["action.proposed"]
        E3["action.approved"]
        E4["action.executed"]
        E5["action.rolled_back"]
        E6["incident.escalated"]
        E7["incident.resolved"]
    end

    subgraph Read["📖 Read Side (Query Service :8082)"]
        R1["GET /api/query/incidents"]
        R2["GET .../incidents/active"]
        R3["GET .../{id}/detail"]
        R4["GET .../incidents/{id}/actions"]
        R5["WebSocket /topic/incidents/*"]
    end

    Write -->|Publish Events| Bus
    Bus -->|Consume & Materialize| Read

    style Write fill:#0f3460,stroke:#e94560,color:#fff
    style Bus fill:#533483,stroke:#e94560,color:#fff
    style Read fill:#16213e,stroke:#00d2ff,color:#fff
```

---

## 📐 Low-Level Design (LLD)

### Domain Models & ER Diagram

```mermaid
erDiagram
    INCIDENT {
        Long id PK
        String serviceName "NOT NULL"
        String severity "NOT NULL (SEV1/SEV2/SEV3)"
        IncidentStatus status "NOT NULL (enum)"
        LocalDateTime createdAt "auto-set @PrePersist"
        LocalDateTime resolvedAt "nullable"
        String escalationReason "nullable"
    }

    REMEDIATION_ACTION {
        Long id PK
        Long incidentId FK
        String actionType "NOT NULL (RESTART_SERVICE, SCALE_WORKER_PODS, etc.)"
        String rationale "max 1000 chars"
        ActionStatus status "NOT NULL (enum)"
        String approvedBy "nullable"
        LocalDateTime executedAt "nullable"
        Long rollbackOf "nullable — FK to self"
        LocalDateTime createdAt "auto-set @PrePersist"
    }

    DLQ_RECORD {
        Long id PK
        String originalTopic "NOT NULL"
        String payload "TEXT — JSON serialized"
        String errorMessage "TEXT"
        boolean replayed "default false"
        LocalDateTime createdAt "auto-set"
        LocalDateTime replayedAt "nullable"
    }

    NOTIFICATION_LOG {
        Long id PK
        Long incidentId FK
        String channel "EMAIL"
        String eventType "e.g. ACTION_PROPOSED"
        String messageBody "TEXT"
        NotificationStatus status "PENDING / SENT / FAILED"
        String failureReason "nullable"
        LocalDateTime createdAt "auto-set"
    }

    INCIDENT ||--o{ REMEDIATION_ACTION : "has many"
    INCIDENT ||--o{ NOTIFICATION_LOG : "triggers"
    REMEDIATION_ACTION ||--o| REMEDIATION_ACTION : "rollbackOf (self-ref)"
```

---

### Incident Lifecycle State Machine

```mermaid
stateDiagram-v2
    [*] --> NEW: Incident Created

    NEW --> INVESTIGATING: AI begins investigation
    NEW --> ESCALATED: Direct escalation

    INVESTIGATING --> ACTION_PROPOSED: AI proposes remediation
    INVESTIGATING --> ESCALATED: Manual escalation

    ACTION_PROPOSED --> WAITING_APPROVAL: Human approves
    ACTION_PROPOSED --> ESCALATED: Manual escalation

    WAITING_APPROVAL --> EXECUTING: Human triggers execute
    WAITING_APPROVAL --> INVESTIGATING: Human rejects action
    WAITING_APPROVAL --> ESCALATED: Manual escalation

    EXECUTING --> MONITORING: Post-execution observation
    EXECUTING --> ROLLBACK: Execution failed
    EXECUTING --> ESCALATED: Manual escalation

    MONITORING --> RESOLVED: Metrics look healthy ✅
    MONITORING --> ROLLBACK: Metrics still degraded
    MONITORING --> ESCALATED: Manual escalation

    ROLLBACK --> INVESTIGATING: Rollback succeeded → re-investigate
    ROLLBACK --> ESCALATED: Rollback failed 🔥

    RESOLVED --> [*]
    ESCALATED --> [*]

    note right of NEW: @PrePersist sets status = NEW
    note right of ESCALATED: Terminal state — requires manual intervention
    note right of RESOLVED: Terminal state — resolvedAt timestamp set
```

---

### Action Lifecycle State Machine

```mermaid
stateDiagram-v2
    [*] --> PROPOSED: AI agent calls proposeAction()

    PROPOSED --> APPROVED: Human approves + approvedBy set
    PROPOSED --> REJECTED: Human rejects → incident back to INVESTIGATING

    APPROVED --> EXECUTED: Human triggers execute → Docker/Kafka action runs

    EXECUTED --> ROLLED_BACK: Rollback triggered → compensating action created

    REJECTED --> [*]
    ROLLED_BACK --> [*]

    note right of PROPOSED
        Created by Agent Service
        via Command Service API
    end note

    note right of EXECUTED
        performRealAction() dispatches to:
        • DockerExecutionService.restartService()
        • DockerExecutionService.scaleWorkerPods()
        • KafkaAdminService.clearDeadLetterQueue()
    end note

    note right of ROLLED_BACK
        Creates a new compensating
        REMEDIATION_ACTION with
        rollbackOf = original action ID
    end note
```

---

### Kafka Topic Architecture

```mermaid
graph TB
    subgraph Publishers["📤 Publishers"]
        CMD["Command Service"]
    end

    subgraph Topics["📨 Kafka Topics (3 partitions each)"]
        T1["incident.created"]
        T2["action.proposed"]
        T3["action.approved"]
        T4["action.executed"]
        T5["action.rolled_back"]
        T6["action.rejected"]
        T7["incident.escalated"]
        T8["incident.resolved"]
        T9["incident.status_updated"]
    end

    subgraph DLQTopics["💀 Dead Letter Queues"]
        D1["incident.created.dlq"]
        D2["action.proposed.dlq"]
        D3["action.approved.dlq"]
        D4["action.executed.dlq"]
        D5["action.rolled_back.dlq"]
        D6["action.rejected.dlq"]
        D7["incident.escalated.dlq"]
        D8["incident.resolved.dlq"]
        D9["incident.status_updated.dlq"]
    end

    subgraph Consumers["📥 Consumer Groups"]
        QS["query-service-group<br/>(Query Service)"]
        AGT["agent-service-group<br/>(Agent Service)"]
        NTF["notification-service-group<br/>(Notification Service)"]
    end

    CMD --> T1 & T2 & T3 & T4 & T5 & T6 & T7 & T8 & T9

    T1 --> QS & AGT
    T2 --> QS & NTF
    T3 --> QS
    T4 --> QS
    T5 --> QS & NTF
    T6 --> QS
    T7 --> QS & NTF
    T8 --> QS
    T9 --> QS

    T1 -.->|On failure| D1
    T2 -.->|On failure| D2
    T3 -.->|On failure| D3
    T4 -.->|On failure| D4
    T5 -.->|On failure| D5
    T6 -.->|On failure| D6
    T7 -.->|On failure| D7
    T8 -.->|On failure| D8
    T9 -.->|On failure| D9

    style Publishers fill:#0f3460,color:#fff
    style Topics fill:#533483,color:#fff
    style DLQTopics fill:#8B0000,color:#fff
    style Consumers fill:#16213e,color:#fff
```

---

### AI Agent Reasoning Pipeline

```mermaid
graph TB
    subgraph Trigger["🔔 Event Trigger"]
        KE["Kafka: incident.created"]
    end

    subgraph Agent["🤖 Agent Service"]
        EL["IncidentEventListener"]
        PROMPT["Build Investigation Prompt<br/>(service, severity, incidentId)"]

        subgraph AIService["AiService (Dual-LLM)"]
            G["Gemini 2.5 Flash<br/>(Primary)"]
            Q["Groq LLaMA 3.3 70B<br/>(Fallback)"]
        end

        subgraph Tools["AgentTools (Spring AI @Tool)"]
            T1["getServiceHealth(serviceName)"]
            T2["getKafkaConsumerLag(topic)"]
            T3["getRedisMemoryStats()"]
            T4["checkDatabaseDeadlocks()"]
            T5["getRecentDeployments(serviceName)"]
            T6["proposeAction(actionType, rationale)"]
        end

        subgraph Metrics["MetricsClient"]
            MC["fetchMetrics(serviceName)"]
            CB["Resilience4j Circuit Breaker"]
            RC["Redis Cache Fallback"]
        end
    end

    subgraph External["🌐 External"]
        PROM["Prometheus API<br/>/api/v1/query"]
        CMDSVC["Command Service<br/>POST /api/incidents/{id}/actions"]
    end

    KE --> EL
    EL --> PROMPT
    PROMPT --> G
    G -->|Tool calls| Tools
    G -.->|On failure| Q
    Q -->|Tool calls| Tools

    T6 -->|HTTP POST| CMDSVC
    MC --> CB
    CB -->|Healthy| PROM
    CB -->|Open/Half-Open| RC

    style Trigger fill:#e94560,color:#fff
    style Agent fill:#0f3460,color:#fff
    style External fill:#16213e,color:#fff
```

---

### Circuit Breaker & Resilience Pattern

```mermaid
stateDiagram-v2
    [*] --> CLOSED: Normal operation

    CLOSED --> OPEN: Failure rate ≥ 50%<br/>(sliding window = 10 calls)
    CLOSED --> CLOSED: Success → live Prometheus data

    OPEN --> HALF_OPEN: After 30s wait duration
    OPEN --> OPEN: All calls → Redis cache fallback<br/>(MetricSnapshot.stale = true)

    HALF_OPEN --> CLOSED: 3 permitted calls succeed
    HALF_OPEN --> OPEN: Any call fails

    note right of CLOSED
        MetricsClient.fetchMetrics()
        calls Prometheus /api/v1/query
        Queries: error_rate & avg_latency
    end note

    note right of OPEN
        fallbackToCache() returns
        cached MetricSnapshot from
        Redis with stale=true flag
    end note

    note right of HALF_OPEN
        3 trial calls permitted
        to test Prometheus recovery
    end note
```

---

### Dead Letter Queue (DLQ) Pipeline

```mermaid
sequenceDiagram
    participant K as Kafka Topic
    participant C as Consumer<br/>(Query/Notification Service)
    participant DLR as PersistingDeadLetterRecoverer
    participant DB as MySQL (dlq_records)
    participant DLQT as Kafka DLQ Topic
    participant UI as React DLQ Admin Page
    participant Admin as 👤 Ops Engineer

    K->>C: Deliver message
    C->>C: Processing fails (exception)
    C->>DLR: Error handler invoked

    DLR->>DB: Persist DlqRecord<br/>(topic, payload, error, replayed=false)
    DLR->>DLQT: Forward to {topic}.dlq

    Admin->>UI: Open DLQ Admin page
    UI->>UI: GET /api/admin/dlq (unreplayed)
    UI-->>Admin: Show failed events list

    Admin->>UI: Click "Replay"
    UI->>UI: POST /api/admin/dlq/{id}/replay
    UI->>K: Re-publish to original topic
    UI->>DB: Set replayed=true, replayedAt=now
```

---

## 🔧 Service Breakdown

### Command Service (`:8081`) — Write Side

The **single source of truth** for all state mutations. Every write operation publishes a Kafka event.

| Endpoint | Method | Description |
|:---|:---:|:---|
| `/api/incidents` | `POST` | Create a new incident |
| `/api/incidents/{id}` | `GET` | Get incident by ID |
| `/api/incidents/{id}/status` | `PATCH` | Update status (escalate / resolve) |
| `/api/incidents/{id}/actions` | `POST` | Propose a remediation action |
| `/api/incidents/{id}/actions/{actionId}/approve` | `POST` | Approve a proposed action (idempotent) |
| `/api/incidents/{id}/actions/{actionId}/reject` | `POST` | Reject a proposed action |
| `/api/incidents/{id}/actions/{actionId}/execute` | `POST` | Execute an approved action (idempotent) |
| `/api/incidents/{id}/actions/{actionId}/rollback` | `POST` | Rollback an executed action |

**Execution Engine** — When an action is executed, it dispatches to real infrastructure:

| Action Type | Handler | What It Does |
|:---|:---|:---|
| `RESTART_SERVICE` | `DockerExecutionService` | Restarts Docker container via Docker socket |
| `SCALE_WORKER_PODS` | `DockerExecutionService` | Reads the target container's `com.docker.compose.project/service/working_dir` labels, resolves the correct (possibly cross-project) Compose file via `scaling.path-mappings`, and runs `docker compose -p <project> --scale {service}=N` |
| `CLEAR_DEAD_LETTER_QUEUE` | `KafkaAdminService` | Deletes all records from a Kafka DLQ topic (checks topic existence first to avoid spurious errors) |

> `SCALE_WORKER_PODS` isn't limited to this project's own containers — it can scale services in a **different** Compose project (e.g. Distributed Job Forge) as long as that project's directory is mounted read-only and mapped via `scaling.path-mappings` in `command-service`'s `application.yml`.

---

### Query Service (`:8082`) — Read Side

Consumes Kafka events and materializes **read-optimized projections** in its own MySQL tables. Also hosts WebSocket + DLQ admin.

| Endpoint | Method | Description |
|:---|:---:|:---|
| `/api/query/incidents` | `GET` | List all incidents |
| `/api/query/incidents/active` | `GET` | List active (non-resolved/escalated) incidents |
| `/api/query/incidents/{id}` | `GET` | Get incident by ID |
| `/api/query/incidents/{id}/detail` | `GET` | Get full incident detail with actions |
| `/api/query/incidents/status/{status}` | `GET` | Filter by status |
| `/api/query/incidents/severity/{severity}` | `GET` | Filter by severity |
| `/api/query/incidents/service/{name}` | `GET` | Filter by service name |
| `/api/query/incidents/{id}/actions` | `GET` | List actions for incident |
| `/api/admin/dlq` | `GET` | List unreplayed DLQ records |
| `/api/admin/dlq/all` | `GET` | List all DLQ records |
| `/api/admin/dlq/{id}/replay` | `POST` | Replay a failed event |

**WebSocket Destinations:**

| STOMP Destination | Trigger |
|:---|:---|
| `/topic/incidents/{id}` | Any update to a specific incident |
| `/topic/incidents/active` | Active incident list changes |

---

### Agent Service (`:8083`) — AI Brain

Listens for `incident.created` events and autonomously investigates using LLM-powered tool calling.

**AI Tools (Spring AI `@Tool`):**

| Tool | Description |
|:---|:---|
| `getServiceHealth(serviceName)` | Returns `HEALTHY` / `DEGRADED` / `UNHEALTHY` |
| `getKafkaConsumerLag(topic)` | Returns current consumer lag count |
| `getRedisMemoryStats()` | Returns `usedMemoryMb` and `evictedKeys` |
| `checkDatabaseDeadlocks()` | Returns recent deadlock count from MySQL |
| `getRecentDeployments(serviceName)` | Returns latest deployment info for correlation |
| `proposeAction(actionType, rationale)` | **Terminal tool** — proposes remediation via Command Service |

**MetricsClient (Prometheus Integration):**

| PromQL Query | Metric |
|:---|:---|
| `sum(rate(http_requests_total{job="{svc}",status=~"5.."}[5m])) / sum(rate(http_requests_total{job="{svc}"}[5m])) * 100` | Error Rate (%) |
| `sum(rate(http_request_duration_seconds_sum{job="{svc}"}[5m])) / sum(rate(http_request_duration_seconds_count{job="{svc}"}[5m]))` | Avg Latency (seconds → ms) |

---

### Notification Service (`:8084`) — Alerting

Consumes specific Kafka topics and sends email alerts asynchronously.

| Kafka Topic | Email Subject Pattern |
|:---|:---|
| `action.proposed` | 🚨 Incident Created \| #ID |
| `action.rolled_back` | ↩ Action Rolled Back \| Incident #ID |
| `incident.escalated` | 🔥 Incident Escalated \| Incident #ID |

---

### API Gateway (`:8080`) — Entry Point

Spring Cloud Gateway WebMVC with route-based proxying, and the platform's **security perimeter** — JWT issuance/validation and the aggregated Swagger UI both live here.

| Route Pattern | Target |
|:---|:---|
| `/api/auth/**` | Handled locally (login → JWT issuance) |
| `/api/incidents/**` | Command Service `:8081` |
| `/api/query/**` | Query Service `:8082` |
| `/api/agent/**` | Agent Service `:8083` |
| `/v3/api-docs/**`, `/swagger-ui/**` | Handled locally (aggregated OpenAPI docs) |

---

## 🔐 Security

The platform is secured end-to-end with **stateless JWT authentication**, enforced at the API Gateway — the single public entry point for the SPA and any external client.

### Authentication Flow

```mermaid
sequenceDiagram
    actor User as 👤 User
    participant UI as React Dashboard
    participant GW as API Gateway
    participant SEC as Spring Security<br/>(JwtAuthFilter)
    participant SVC as Downstream Service<br/>(Command / Query / Agent)

    User->>UI: Enter username & password
    UI->>GW: POST /api/auth/login
    GW->>GW: AuthenticationManager verifies credentials<br/>(BCryptPasswordEncoder)
    GW-->>UI: 200 OK { token: "<JWT>" }
    UI->>UI: Store token in sessionStorage

    Note over UI,GW: Every subsequent request

    UI->>GW: GET/POST ... Authorization: Bearer <JWT>
    GW->>SEC: JwtAuthFilter intercepts request
    SEC->>SEC: Extract username, verify signature + expiry
    alt Token valid
        SEC->>GW: SecurityContext populated
        GW->>SVC: Proxy request downstream
        SVC-->>UI: 200 OK + data
    else Token missing / invalid / expired
        SEC-->>UI: 401 Unauthorized (JSON error body)
    end
```

**How it works:**

1. **Login** — `POST /api/auth/login` accepts `{ userName, password }`. Credentials are checked via a `DaoAuthenticationProvider` backed by an in-memory `AppUserDetailsService`, seeded at startup from `ADMIN_USERNAME` / `ADMIN_PASSWORD` and hashed with **BCrypt**.
2. **Token issuance** — On success, `JwtService` signs an **HS256 JWT** (subject = username, 1‑hour expiry by default) using a secret from `JWT_SECRET`.
3. **Token storage** — The React SPA stores the token in `sessionStorage` (cleared on tab close) and attaches it as `Authorization: Bearer <token>` to every Axios request via an interceptor.
4. **Token validation** — `JwtAuthFilter` (a `OncePerRequestFilter`) runs before Spring Security's built-in auth filter on every request, extracts and validates the token, and populates the `SecurityContext` so downstream `authorizeHttpRequests` rules apply.
5. **Session handling** — The gateway is fully **stateless** (`SessionCreationPolicy.STATELESS`); no server-side session state — every request is authenticated independently from its token.
6. **Auto-logout** — The frontend's Axios response interceptor watches for `401` responses, clears the stored token, and redirects to `/login`.

### What's Protected

| Path | Access |
|:---|:---|
| `/api/auth/**` | Public — login endpoint |
| `/actuator/**` | Public — health checks & Prometheus scraping |
| `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` | Public — API documentation |
| Everything else (`/api/incidents/**`, `/api/query/**`, `/api/agent/**`, ...) | **Requires a valid JWT** |

Unauthenticated or unauthorized calls receive a structured JSON error instead of Spring's default HTML error page:

```json
// 401 — missing/invalid token
{ "status": 401, "error": "Unauthorized", "message": "Missing or invalid token", "path": "/api/incidents" }
```

> A `403 Forbidden` handler (`SecurityErrorHandlers.forbiddenHandler()`) is also wired into the filter chain, ready for role-based rules — but today there's a single `ADMIN` user and no `hasRole(...)` checks anywhere, so in practice every authenticated request is authorized. Anyone building on this should treat that as a gap to close (e.g. a `VIEWER` role that can read incidents but not approve/execute actions) rather than existing role enforcement.

### Rate Limiting

Every request through the Gateway passes through `RateLimitFilter` before it reaches `JwtAuthFilter` — so rate limiting applies even to unauthenticated traffic hitting `/api/auth/login`.

- **Algorithm:** token bucket, via [Bucket4j](https://bucket4j.com/) (`Bandwidth.classic` + `Refill.greedy`). Default config is 20 tokens capacity, refilling 20 tokens every 60 seconds — see `rate-limit.*` in `api-gateway/application.yaml`.
- **Keying:** one bucket per client, keyed off `X-Forwarded-For` (falling back to `request.getRemoteAddr()`) — so it's per-IP, not per-user or per-JWT.
- **Storage:** a plain in-memory `ConcurrentHashMap<String, Bucket>` inside `RateLimitService`. **No Redis involved.** This is a deliberate simplification for a single-instance deployment — it means limits reset on restart and won't be shared if you ever run more than one `api-gateway` replica behind a load balancer. If you scale the gateway horizontally, this needs to move to a shared store (Bucket4j has a Redis/Hazelcast-backed `ProxyManager` for exactly that).
- **On limit exceeded:** returns `429 Too Many Requests` with a `Retry-After` header (seconds until refill) and a JSON body. On success, the response carries `X-RateLimit-Remaining` so clients can self-throttle.

```json
// 429 — rate limit exceeded
{ "status": 429, "error": "Too Many Requests", "message": "Rate limit exceeded. Try again in 42s.", "path": "/api/incidents" }
```

### Security Configuration

| Component | File | Responsibility |
|:---|:---|:---|
| `SecurityConfig` | `api-gateway/.../config/SecurityConfig.java` | Defines the `SecurityFilterChain`: stateless sessions, CSRF disabled (pure REST API), route-level authorization rules |
| `JwtAuthFilter` | `api-gateway/.../config/JwtAuthFilter.java` | Per-request filter that extracts, validates, and applies the JWT to the security context |
| `JwtService` | `api-gateway/.../security/JwtService.java` | Signs and parses HS256 JWTs via `jjwt` |
| `JwtProperties` | `api-gateway/.../security/JwtProperties.java` | Binds `jwt.secret` / `jwt.expiration-ms` from configuration |
| `AppUserDetailsService` | `api-gateway/.../security/AppUserDetailsService.java` | Seeds the admin user (from env vars) and implements Spring Security's `UserDetailsService` |
| `PasswordConfig` | `api-gateway/.../config/PasswordConfig.java` | Exposes the `BCryptPasswordEncoder` bean |
| `SecurityErrorHandlers` | `api-gateway/.../config/SecurityErrorHandlers.java` | JSON `401`/`403` response bodies instead of default Spring error pages |
| `AuthController` | `api-gateway/.../controller/AuthController.java` | `POST /api/auth/login` — the only unauthenticated write endpoint |
| `RateLimitFilter` | `api-gateway/.../ratelimit/RateLimitFilter.java` | Per-request filter enforcing the token bucket, ahead of JWT auth |
| `RateLimitService` | `api-gateway/.../ratelimit/RateLimitService.java` | Owns the in-memory bucket map; builds a Bucket4j `Bandwidth` per client key |
| `RateLimitProperties` | `api-gateway/.../ratelimit/RateLimitProperties.java` | Binds `rate-limit.capacity` / `rate-limit.refill-tokens` / `rate-limit.refill-seconds` |

> ⚠️ **Trust boundary note:** JWT validation currently happens **only at the API Gateway**. Command/Query/Agent/Notification services do not independently verify the token — they trust traffic arriving from the gateway. In production, run these services on a private network/VPC not reachable directly from outside the gateway (the bundled `docker-compose.yml` already keeps them off the host network except via their debug port mappings), or add a shared-secret / mTLS check between services for defense in depth.

> 🔑 **Credentials are configuration, not code.** Set `ADMIN_USERNAME`, `ADMIN_PASSWORD`, and `JWT_SECRET` via environment variables (see `.env.example`) — never commit real credentials. Rotate `JWT_SECRET` to invalidate all outstanding tokens.

---

## 🚀 Getting Started

### Prerequisites

- **Java 21** (JDK) — only needed if building services outside Docker
- **Docker & Docker Compose**
- **Node.js 18+** — only needed for frontend hot-reload dev mode
- **Prometheus** (optional external instance) — the platform monitors your *own* microservices' metrics; if you don't have one, the bundled `aic-prometheus` container works standalone

### 1. Clone & Configure

```bash
git clone https://github.com/your-username/AI-Incident-Commander.git
cd AI-Incident-Commander
```

Copy the environment template and fill in your values:

```bash
cp .env.example .env
```

Key environment variables:

```env
# Authentication (API Gateway)
ADMIN_USERNAME=admin
ADMIN_PASSWORD=change_this_password
JWT_SECRET=replace-with-a-long-random-string-min-256-bits

# Database (MySQL)
DB_USERNAME=root
DB_PASSWORD=change_this_password
MYSQL_ROOT_PASSWORD=change_this_password
MYSQL_DATABASE=incident_commander
MYSQL_PORT=3308

# Redis / Kafka
REDIS_PORT=6380
KAFKA_PORT=9094

# AI API Keys (required)
GEMINI_API_KEY=your_gemini_api_key
GROQ_API_KEY=your_groq_api_key

# Email Notifications (required for alerts)
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
ALERT_EMAIL=recipient@example.com

# Telemetry target — services this platform monitors/investigates
PROMETHEUS_URL=http://host.docker.internal:9090
MONITORED_SERVICES=order-service,payment-service
ANOMALY_LATENCY_THRESHOLD_MS=500

# Docker execution engine
DOCKER_HOST=tcp://localhost:2375
DOCKER_COMPOSE_DIR=C:/AI Incident Commander

# Optional — enables SCALE_WORKER_PODS against another Compose project
# (host path -> the read-only mount `command-service` exposes it under)
DJF_PROJECT_DIR=C:/DistributedJobForge
```

> The `docker-compose.yml` `scaling.path-mappings` config (in `command-service`'s `application.yml`) maps that host path to `/djf-workspace` inside the container, so `SCALE_WORKER_PODS` can target a sibling project's Compose file directly.

### 2. Start Everything

```bash
docker compose up --build -d
```

This single command builds and starts **all 5 backend services, the frontend, and the full observability stack** — 15 containers total: MySQL, Redis, Zookeeper, Kafka, the 5 Spring Boot services, the React frontend (served via Nginx), OTel Collector, Tempo, Loki, `aic-prometheus`, and Grafana.

**Application services:**

| Service | Host Port → Container | Health Check |
|:---|:---:|:---|
| Frontend (Nginx) | `5174` → `80` | `http://localhost:5174` |
| API Gateway | `18080` → `8080` | `http://localhost:18080/actuator/health` |
| Command Service | `18081` → `8081` | `http://localhost:18081/actuator/health` |
| Query Service | `18082` → `8082` | `http://localhost:18082/actuator/health` |
| Agent Service | `18083` → `8083` | `http://localhost:18083/actuator/health` |
| Notification Service | `18084` → `8084` | `http://localhost:18084/actuator/health` |
| MySQL | `${MYSQL_PORT}` → `3306` | — |
| Redis | `${REDIS_PORT}` → `6379` | — |
| Kafka | `${KAFKA_PORT}` → `9092` | — |

**Observability stack:**

| Service | Port | Purpose |
|:---|:---:|:---|
| Grafana | `3001` | Unified dashboards (metrics + traces + logs); anonymous admin access enabled for local dev |
| aic-prometheus | `9091` | Scrapes `/actuator/prometheus` from all 5 services; remote-write + exemplar storage enabled |
| Tempo | `3200` | Distributed trace storage (100% sampling from every service) |
| Loki | `3100` | Centralized log aggregation |
| OTel Collector | `4317` / `4318` | OTLP gRPC / HTTP ingestion from all services, forwarding to Tempo + Loki |

Open **http://localhost:5174** for the dashboard, or **http://localhost:3001** for Grafana.

### 3. (Optional) Frontend Dev Mode

For hot-reload during frontend development, run it outside Docker instead:

```bash
cd Frontend
npm install
npm run dev
```

Open **http://localhost:5173** in your browser (point `Frontend/.env`'s API base URL at `http://localhost:18080`).

---

## 📡 API Reference

> Examples below target the Docker Compose gateway port (`18080`). If you're running `api-gateway` standalone (outside Docker), use `8080` instead. Every endpoint except `/api/auth/login`, `/actuator/**`, and the Swagger paths **requires** an `Authorization: Bearer <token>` header — see [Security](#-security).

### 0. Log In (required first)

```bash
curl -X POST http://localhost:18080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"userName": "admin", "password": "change_this_password"}'

# → { "token": "eyJhbGciOiJIUzI1NiJ9..." }

export TOKEN="eyJhbGciOiJIUzI1NiJ9..."   # save it for the calls below
```

### Create an Incident

```bash
curl -X POST http://localhost:18080/api/incidents \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "order-service",
    "severity": "SEV1"
  }'
```

### Approve an Action (Idempotent)

```bash
curl -X POST http://localhost:18080/api/incidents/1/actions/1/approve \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: $(uuidgen)" \
  -d '{"approvedBy": "ops-engineer-1"}'
```

### Execute an Action

```bash
curl -X POST http://localhost:18080/api/incidents/1/actions/1/execute \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Idempotency-Key: $(uuidgen)"
```

### Escalate an Incident

```bash
curl -X PATCH http://localhost:18080/api/incidents/1/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "targetStatus": "ESCALATED",
    "reason": "Multiple rollback attempts failed"
  }'
```

### Replay a DLQ Record

```bash
curl -X POST http://localhost:18080/api/admin/dlq/1/replay \
  -H "Authorization: Bearer $TOKEN"
```

---

## 📘 API Documentation (Swagger / OpenAPI)

Every backend service ships its own **springdoc-openapi** spec, and the API Gateway aggregates all of them into a single Swagger UI.

| Service | Swagger UI | Raw OpenAPI JSON |
|:---|:---|:---|
| **API Gateway (aggregated)** | `http://localhost:18080/swagger-ui.html` | `http://localhost:18080/v3/api-docs` |
| Command Service | `http://localhost:18081/swagger-ui.html` | `http://localhost:18081/v3/api-docs` |
| Query Service | `http://localhost:18082/swagger-ui.html` | `http://localhost:18082/v3/api-docs` |
| Agent Service | `http://localhost:18083/swagger-ui.html` | `http://localhost:18083/v3/api-docs` |

Open the **Gateway's** Swagger UI and use the dropdown in the top-right corner to switch between **API Gateway (Auth)**, **Command Service**, **Query Service**, and **Agent Service** — no need to open four separate tabs.

**Trying protected endpoints from Swagger:**

1. Expand `POST /api/auth/login` under the Gateway spec, execute it with your admin credentials, and copy the `token` from the response.
2. Click the **Authorize 🔒** button at the top of the page and paste the token as `Bearer <token>`.
3. All subsequent "Try it out" calls automatically include the `Authorization` header.

`/v3/api-docs/**` and `/swagger-ui/**` are explicitly allow-listed in `SecurityConfig`, so the documentation itself is reachable without a token — only the operations you execute *from* it need one.

---

## 🖥️ Frontend Pages

| Page | Route | Description |
|:---|:---|:---|
| **Login** | `/login` | JWT-based sign-in; stores the token in `sessionStorage` and gates every other route |
| **Dashboard** | `/` | Live overview with active incident count, severity breakdown, and real-time WebSocket updates |
| **Incidents** | `/incidents` | Full incident list with status filtering and create-incident modal |
| **Incident Detail** | `/incidents/:id` | Complete incident timeline with action lifecycle controls (approve/reject/execute/rollback/escalate/resolve) |
| **Telemetry** | `/telemetry` | Live Prometheus metrics — error rate, latency, circuit breaker state indicator |
| **DLQ Admin** | `/dlq` | Dead letter queue inspector with replay functionality |

---

## 🛠️ Tech Stack

### Backend

| Technology | Purpose |
|:---|:---|
| **Java 21** | Language runtime |
| **Spring Boot 4.1** | Application framework |
| **Spring AI 2.0** | LLM integration (Gemini + OpenAI-compatible) |
| **Spring Cloud Gateway** | API gateway routing |
| **Spring Security** | Authentication & authorization at the gateway |
| **jjwt (JSON Web Token)** | JWT signing/parsing (HS256) |
| **BCrypt** | Password hashing |
| **springdoc-openapi (Swagger UI)** | Interactive, auto-generated API documentation per service |
| **Apache Kafka 7.6** | Event streaming / CQRS event bus |
| **MySQL 8.0** | Persistent storage (command + query + DLQ + notifications) |
| **Redis 7** | Caching (metrics, idempotency keys) |
| **Resilience4j** | Circuit breaker for Prometheus calls |
| **Bucket4j** | Token-bucket rate limiting at the Gateway (in-memory, per-client-IP) |
| **Docker Java API** | Container management execution |
| **Spring WebSocket (STOMP)** | Real-time push to frontend |
| **JavaMailSender** | SMTP email notifications |

### Frontend

| Technology | Purpose |
|:---|:---|
| **React 19** | UI library |
| **Vite 8** | Build tooling & dev server |
| **React Router 7** | Client-side routing |
| **Axios** | HTTP client with request/response interceptors (attaches JWT, handles 401 auto-logout) |
| **STOMP.js + SockJS** | WebSocket client |
| **Lucide React** | Icon library |
| **sessionStorage** | Client-side JWT storage (cleared when the tab closes) |

### Infrastructure

| Technology | Purpose |
|:---|:---|
| **Docker Compose** | Multi-container orchestration (15 containers) |
| **Zookeeper** | Kafka coordination |
| **Nginx** | Serves the built frontend in production/Docker mode |

### Observability

| Technology | Purpose |
|:---|:---|
| **OpenTelemetry Collector** | Central OTLP ingestion (gRPC `:4317` / HTTP `:4318`); fans out traces + logs |
| **Grafana Tempo** | Distributed trace backend (100% sampling from every Spring Boot service) |
| **Grafana Loki** | Log aggregation backend |
| **Prometheus** (`aic-prometheus`) | Scrapes `/actuator/prometheus` from all 5 services; remote-write + exemplar storage |
| **Grafana** | Unified dashboards correlating metrics, traces, and logs; provisioned via `grafana/provisioning/` |

---

## 📁 Project Structure

```
AI-Incident-Commander/
├── .env                              # Environment configuration
├── docker-compose.yml                # Full stack orchestration (15 containers)
├── otel-collector-config.yml         # OTLP receiver -> Tempo/Loki exporters
├── tempo.yml                         # Grafana Tempo trace backend config
├── loki-config.yml                   # Grafana Loki log backend config
├── aic-prometheus.yml                # Prometheus scrape config (all 5 services)
├── alerts.yml                        # Prometheus alerting rules
├── grafana/provisioning/             # Auto-provisioned datasources + dashboards
│
├── Backend/
│   ├── api-gateway/                  # Spring Cloud Gateway + Security perimeter (:8080)
│   │   ├── controller/               #   AuthController (POST /api/auth/login)
│   │   ├── security/                 #   AppUser, AppUserDetailsService, JwtService, JwtProperties
│   │   └── config/                   #   SecurityConfig, JwtAuthFilter, PasswordConfig,
│   │                                 #   SecurityErrorHandlers, OpenApiConfig (Swagger)
│   ├── command-service/              # CQRS Write Side (:8081)
│   │   ├── controller/               #   REST endpoints
│   │   ├── model/                    #   JPA entities (Incident, RemediationAction)
│   │   ├── service/                  #   Business logic + Docker + Kafka admin
│   │   ├── event/                    #   Kafka event publisher
│   │   └── config/                   #   Kafka topics, Docker, Redis, OpenApiConfig (Swagger)
│   ├── query-service/                # CQRS Read Side (:8082)
│   │   ├── controller/               #   Query endpoints + DLQ admin
│   │   ├── event/                    #   Kafka consumers (incident + action)
│   │   ├── websocket/                #   STOMP WebSocket relay
│   │   └── config/                   #   DLQ recoverer, error handling, OpenApiConfig (Swagger)
│   ├── agent-service/                # AI Brain (:8083)
│   │   ├── service/                  #   AiService (dual-LLM) + AgentTools
│   │   ├── client/                   #   MetricsClient + CommandServiceClient
│   │   ├── event/                    #   Kafka listener (incident.created)
│   │   └── config/                   #   Prometheus, Redis, AI configs, OpenApiConfig (Swagger)
│   └── notification-service/         # Email Alerts (:8084)
│       ├── event/                    #   Kafka listeners
│       └── service/                  #   EmailNotificationService
│
└── Frontend/
    ├── Dockerfile                    # Multi-stage build -> Nginx (served at :5174 in Docker)
    ├── nginx.conf                    # SPA routing + API proxy config
    └── src/
        ├── api/                      # auth.js (login/token) + client.js (Axios + JWT interceptors)
        ├── components/               # Sidebar, CreateIncidentModal, StatusBadge
        ├── hooks/                    # useWebSocket (STOMP)
        ├── pages/                    # Login, Dashboard, Incidents, Detail, Telemetry, DLQ
        └── index.css                 # Design system
```

---

<div align="center">

**Built with ❤️ using Spring AI, Apache Kafka, and React**

*AI Incident Commander — Because production incidents shouldn't wait for humans to wake up.*

</div>
