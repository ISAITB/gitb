# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

The **Interoperability Test Bed (ITB)** is a conformance testing platform. This repository contains two deployable components:

- **`gitb-srv`** — the test engine (Java 21, Spring Boot 4, Apache Pekko actors). Entry point: `Application` class in `gitb-testbed-service`. Runs on port 8080.
- **`gitb-ui`** — the frontend application (Scala 2.13, Play Framework 3, Slick ORM) with an embedded Angular 21 SPA. Runs on port 9000.

Both components need a **MySQL 8+** database and a **Redis 7+** session cache to operate.

## Build Commands

### Prerequisites

JDK 21+, Maven 3.9+, SBT 1.10+, Scala 2.13+, Node 22+.

### Test Engine (`gitb-srv`) — Maven

```bash
# Build all Java modules (from repo root)
mvn clean install -DskipTests=true

# Build for Docker packaging
mvn clean install -DskipTests=true -Denv=docker

# Run a single test class
mvn test -pl gitb-reports -Dtest=ReportGeneratorTest

# Run Spring Boot locally (from gitb-testbed-service module)
mvn spring-boot:run
# Or launch the Application class directly in your IDE
```

### Frontend (`gitb-ui`) — SBT + npm

```bash
# Compile the Play/Scala backend
cd gitb-ui && sbt compile

# Run the Play app in development mode
sbt run

# Production distribution build (also triggers Angular prod build)
sbt clean dist
# Output: gitb-ui/target/universal/gitb-1.0-SNAPSHOT.zip
```

### Angular UI (`gitb-ui/ui`) — npm

```bash
cd gitb-ui/ui

# Install dependencies (once)
npm install

# Option A: Build into Play assets (access via http://localhost:9000)
npm run build          # one-shot build
npm run build:dev      # watch mode (auto-rebuild on changes)

# Option B: Standalone dev server with hot reload (http://localhost:4200)
npm run build          # required once to place static resources (tinymce)
npm start              # serves and proxies API calls to Play on port 9000

# Production build
npm run build:prod
```

## Development Setup

### Required environment variables for `gitb-srv`

```
remote.testcase.repository.url=http://localhost:9000/repository/tests/:test_id/definition
remote.testresource.repository.url=http://localhost:9000/repository/resource/:test_id/:resource_id
```

### Required infrastructure (start via Docker)

```bash
docker run --name gitb-mysql -p 3306:3306 -d isaitb/gitb-mysql
docker run --name gitb-redis -p 6379:6379 -d isaitb/gitb-redis
```

### Full Docker Compose build from source

Define these env vars first, then:

```powershell
# PowerShell
$env:MYSQL_ROOT_PASSWORD = 'rootPasswordForLocalBuild'
$env:MYSQL_PASSWORD = 'gitbPasswordForLocalBuild'
$env:DB_DEFAULT_PASSWORD = $env:MYSQL_PASSWORD
$env:APPLICATION_SECRET = 'a-local-development-application-secret-at-least-32-chars'
$env:MASTER_PASSWORD = 'a-local-development-master-password'
$env:HMAC_KEY = 'a-local-development-hmac-key'
docker compose up -d --build
```

`HMAC_KEY` must match between `gitb-ui` and `gitb-srv` — it authenticates inter-service calls.

### First login

Go to http://localhost:9000, log in as `admin@itb`. The one-time password is printed in the `gitb-ui` container logs at startup.

## Architecture

### Component Interaction

```
Browser (Angular SPA)
    ↕ REST API + WebSocket
Play/Scala (gitb-ui, port 9000)
    ↕ SOAP/CXF (WSDL)
Spring Boot (gitb-srv, port 8080)
    ↕ callbacks for test resources
Play/Scala (gitb-ui /repository/*)
```

- `gitb-ui` is the authoritative backend: it owns the database (MySQL via Slick), manages users/communities/conformance, and drives the Angular frontend.
- `gitb-srv` is the stateless test executor: it fetches test definitions and resources from `gitb-ui` at runtime, runs test sessions using Pekko actors, and pushes results back via SOAP callbacks.
- The Angular app is served as static assets embedded in the Play distribution; it calls Play's REST API exclusively (under `/api/*`).
- WebSocket connections (`/api/ws`) push real-time test step progress from `gitb-srv` → `gitb-ui` → browser.

### `gitb-ui` Layer Structure

| Package | Role |
|---------|------|
| `controllers/` | Play HTTP controllers — thin layer, delegates to managers |
| `managers/` | Business logic layer; one manager per domain concept |
| `persistence/` | Slick database queries |
| `actors/` | Pekko actors for async operations (session updates, triggers, webhooks) |
| `authentication/` | pac4j integration for LDAP, OIDC (EU Login), CAS, native form auth |
| `filters/` | Auth guard, CORS, security headers |
| `db/migration/default/` | Flyway SQL (+ Scala) migrations; numbered `V<n>__Description` |

### `gitb-srv` Module Structure

| Module | Role |
|--------|------|
| `gitb-testbed-service` | Spring Boot entry point, SOAP endpoint exposure |
| `gitb-engine` | Pekko-based test session execution, step processors |
| `gitb-core` | GITB TDL model classes and shared utilities |
| `gitb-lib` | Processing helpers (XML, scripting, crypto) |
| `gitb-reports` | PDF/HTML report generation via OpenHTML-to-PDF |
| `gitb-remote` | Client stubs for calling external GITB test services |
| `gitb-validator-tdl` | Validates test suite XML against the TDL schema |
| `gitb-xml-resources` | XSD schemas and XML resources bundled as a JAR |

### Key Configuration

All `gitb-ui` configuration is in `gitb-ui/conf/application.conf`. Every property can be overridden by an environment variable (e.g., `TESTBED_SERVICE_URL` overrides `testbed.service.url`). Never commit secrets — use env vars or a `.env` file for local Docker Compose runs.

### Database Migrations

Flyway migrations live in `gitb-ui/conf/db/migration/default/`. Most are SQL (`V<n>__*.sql`); a few are Scala classes when logic is required (e.g., `V53__Encrypt_secrets_at_rest.scala`). Add new migrations with the next sequential version number.

### Authentication

Auth is handled by pac4j (`play-pac4j`). Supported providers: native form (JWT tokens stored in Redis), EU Login/CAS, OIDC, LDAP. The `AuthenticationFilter` in `filters/` gates all `/api/*` routes. The `BaseProfileResolver` / `ProfileResolver` classes in `authentication/` extract user identity from pac4j profiles.

### Angular Frontend

The Angular app in `gitb-ui/ui/` is a standard Angular CLI project. It communicates exclusively with the Play backend via REST. When served standalone (`npm start`), a proxy (`proxy.conf.json`) forwards API calls to `localhost:9000`. The Play controller `FrontendController` serves the Angular `index.html` for all `/app/**` routes (SPA routing).
