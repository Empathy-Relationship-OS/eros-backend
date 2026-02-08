# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is **Project Eros Backend**, a Ktor-based REST API server written in Kotlin. The project was bootstrapped from the Ktor Project Generator and uses a modular plugin architecture.

**Package**: `com.eros`
**Main Class**: `io.ktor.server.netty.EngineMain`
**Default Port**: 8080

## Common Commands

### Development
- `./gradlew run` - Run the development server
- `./gradlew test` - Run all tests
- `./gradlew build` - Build the project

### Docker
- `./gradlew buildFatJar` - Build executable JAR with all dependencies
- `./gradlew buildImage` - Build Docker image
- `./gradlew publishImageToLocalRegistry` - Publish Docker image locally
- `./gradlew runDocker` - Run using local Docker image

### Testing
- `./gradlew test --tests ApplicationTest` - Run specific test class
- `./gradlew test --tests "ApplicationTest.testRoot"` - Run specific test method

## Architecture

### Application Bootstrap

The application follows Ktor's module-based configuration pattern:

1. **Entry Point**: `Application.kt:main()` starts the Netty engine
2. **Module Loading**: `Application.kt:module()` orchestrates plugin configuration in this order:
   - `configureDatabase()` - Database connectivity, Exposed ORM, Flyway migrations
   - `configureSerialization()` - Content negotiation with JSON
   - `configureAdministration()` - Rate limiting
   - `configureHTTP()` - CORS and default headers
   - `configureMonitoring()` - Call logging
   - `configureSecurity()` - Authentication (OAuth + JWT)
   - `configureRouting()` - Routes and request validation

**Important**: Plugin installation order matters in Ktor. Database must be configured first, then security and serialization before routing.

### Configuration Files

- `app/src/main/resources/application.yaml` - Server, database, and JWT configuration
  - Module loading: `com.eros.ApplicationKt.module`
  - Port configuration
  - Database settings (host, port, name, credentials, connection pool)
  - JWT settings (domain, audience, realm)
- `app/src/main/resources/logback.xml` - Logging configuration

### Modular Plugin Structure

Each configuration function lives in its own file:

- **Database.kt** - Database connectivity with PostgreSQL, Exposed ORM, and Flyway migrations
  - HikariCP connection pooling
  - Automatic schema migrations on startup
  - Configured via `database` section in application.yaml
- **Serialization.kt** - kotlinx.serialization JSON support via ContentNegotiation
- **Administration.kt** - Rate limiting with TokenBucket (100 capacity, 10s refill)
- **HTTP.kt** - CORS (currently `anyHost()` - needs production restriction) and custom headers
- **Monitoring.kt** - Call logging for all requests
- **Security.kt** - Dual authentication:
  - OAuth with Google (requires `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` env vars)
  - JWT verification (HMAC256, configured via application.yaml)
  - Session management with `UserSession`
- **Routing.kt** - Request validation and route definitions

### Authentication Flow

The Security module sets up two authentication schemes:

1. **OAuth ("auth-oauth-google")**:
   - Login endpoint: `/login` redirects to Google OAuth
   - Callback: `/callback` receives token and creates session
   - Requires environment variables for Google client credentials

2. **JWT**:
   - Validates HMAC256 signed tokens
   - Checks audience and issuer claims
   - Configuration pulled from `application.yaml` (partially hardcoded)

### Database Module

The application uses PostgreSQL with Exposed ORM and Flyway migrations.

**Components**:
- **HikariCP**: High-performance connection pooling
- **Exposed ORM**: Type-safe database access for Kotlin
- **Flyway**: Versioned database migrations

**Configuration**: Database settings in `application.yaml` under `database:` section
- Supports environment variables (e.g., `${DB_HOST:localhost}`)
- Default configuration for local development (localhost:5432)

**Database Query Helper**:

```kotlin
import com.eros.database.dbQuery

suspend fun getUser(id: UUID): User? = dbQuery {
    Users.select { Users.id eq id }.singleOrNull()?.toUser()
}
```

**Migration Files**: Located in `database/src/main/resources/db/migration/`
- Follow naming convention: `V{version}__{description}.sql`
- Example: `V1__auth_tables.sql`, `V2__user_profiles.sql`
- Migrations run automatically on application startup
- Status tracked in `flyway_schema_history` table

**Environment Variables**:
- `DB_HOST` - Database hostname (default: localhost)
- `DB_PORT` - Database port (default: 5432)
- `DB_NAME` - Database name (default: eros)
- `DB_USER` - Database username (default: postgres)
- `DB_PASSWORD` - Database password (default: postgres)

### Request Validation

Global validation is configured in `Routing.kt`:
- String body validation requires text to start with "Hello"
- Custom validators should be added to the `RequestValidation` plugin

### Error Handling

Status pages are configured to catch all `Throwable` and return 500 with error details. Customize in `Routing.kt:configureRouting()`.

## Module Architecture

Eros Backend uses a **modular monolith** architecture where each module can potentially be extracted as an independent microservice. Modules follow a clear separation of concerns:

### Core Modules

**auth/** - Authentication & Identity
- **Purpose**: "Who you are" - identity, credentials, verification
- **Owns**: User authentication, OTP verification, session management
- **Tables**: `users`, `otp_verification`, `refresh_tokens`
- **Routes**: `/auth/login`, `/auth/signup`, `/auth/verify`
- **Reusable**: Can authenticate any entity type (users, admins, services)

**users/** - User Profiles & Management
- **Purpose**: "What you're like" - profiles, preferences, social features
- **Owns**: User profiles, preferences, activity tracking
- **Tables**: `user_profiles`, `user_preferences`, `user_activity`
- **Routes**: `/users/{id}`, `/users/me`, `/users/search`
- **Dependencies**: Auth module for identity

**database/** - Database Infrastructure
- **Purpose**: Shared database connectivity and migrations
- **Provides**: HikariCP connection pooling, Flyway migrations, Exposed setup
- **No business logic**: Pure infrastructure module

**common/** - Shared Utilities
- **Purpose**: Cross-cutting utilities used by all modules
- **Provides**: Security helpers (PasswordHasher), common DTOs, extensions

### Feature Modules (Future)

- **matching/** - Matching algorithm and compatibility
- **dates/** - Date scheduling and management
- **notifications/** - Push notifications and messaging
- **wallet/** - Payment and transaction management

### Design Principles

1. **Auth vs Users Separation**
   - Auth handles security-critical identity data (credentials, verification)
   - Users handles business-specific profile data (preferences, activity)
   - This allows auth to be reused across multiple entity types
   - Enables independent scaling and security auditing

2. **Database Module Pattern**
   - Migrations live in `database/src/main/resources/db/migration/`
   - Table definitions (Exposed objects) live in their owning module (e.g., `auth/tables/`)
   - Database module provides only connectivity infrastructure

3. **Microservice Extraction Path**
   - Each module has minimal dependencies
   - Business logic stays within module boundaries
   - Modules communicate through well-defined interfaces
   - Can extract to standalone service by adding HTTP client layer

## Project Structure

```
app/
  ├── src/main/kotlin/
  │   ├── Application.kt      - Entry point and module orchestration
  │   ├── Database.kt         - Database plugin configuration
  │   ├── Serialization.kt    - JSON content negotiation
  │   ├── Administration.kt   - Rate limiting
  │   ├── HTTP.kt            - CORS and headers
  │   ├── Monitoring.kt      - Call logging
  │   ├── Security.kt        - OAuth + JWT authentication
  │   └── Routing.kt         - Routes and validation
  ├── src/main/resources/
  │   ├── application.yaml   - Ktor, database, and JWT config
  │   └── logback.xml       - Logging config
  └── src/test/kotlin/
      └── ApplicationTest.kt - Test suite

auth/
  └── src/main/kotlin/com/eros/auth/
      ├── tables/           - Exposed table definitions (Users, OtpVerification)
      ├── services/         - Auth business logic (AuthService, OtpService)
      ├── routes/           - Auth endpoints
      └── models/           - DTOs and data classes

users/
  └── src/main/kotlin/com/eros/users/
      ├── tables/           - User profile tables
      ├── services/         - User business logic
      ├── routes/           - User endpoints
      └── models/           - User DTOs

database/
  ├── src/main/kotlin/com/eros/database/
  │   ├── DatabasePlugin.kt  - Ktor plugin for database lifecycle
  │   ├── DatabaseConfig.kt  - Database configuration data class
  │   ├── DatabaseFactory.kt - HikariCP DataSource factory
  │   └── FlywayConfig.kt   - Flyway migration management
  └── src/main/resources/db/migration/
      ├── V0__init.sql      - Baseline migration
      ├── V1__auth_tables.sql - Auth module tables
      └── V2__user_profiles.sql - Users module tables

common/
  └── src/main/kotlin/com/eros/common/
      └── security/
          └── PasswordHasher.kt - BCrypt password hashing utility
```

## Important Notes

- **Security Warning**: `HTTP.kt` currently uses `anyHost()` for CORS, which allows all origins. This should be restricted in production.
- **JWT Configuration**: JWT settings are partially hardcoded in `Security.kt` (secret, audience, domain). Consider moving entirely to `application.yaml`.
- **Environment Variables**: Google OAuth requires `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` to be set.
- **Rate Limiting**: Currently applies TokenBucket (100 capacity, 10s rate) to all routes under `/`. Adjust in `Administration.kt` as needed.

## Adding New Features

1. **New Routes**: Add to `Routing.kt:configureRouting()` function
2. **New Plugin**: Create new configuration file (e.g., `MyFeature.kt`) with `fun Application.configureMyFeature()`, then call it from `Application.kt:module()`
3. **New Validators**: Add to `RequestValidation` block in `Routing.kt`
4. **Environment Config**: Add to `application.yaml` and reference via `environment.config`

## Testing

Tests use `ktor-server-test-host` for in-memory server testing. See `ApplicationTest.kt` for examples.

When successful, the server logs:
```
Application started in X.XXX seconds.
Responding at http://0.0.0.0:8080
```
