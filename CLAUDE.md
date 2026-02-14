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
   - `configureAuthentication()` - Firebase authentication (token verification)
   - `configureRouting()` - Routes and request validation

**Important**: Plugin installation order matters in Ktor. Database must be configured first, then security and serialization before routing.

### Configuration Files

- `app/src/main/resources/application.yaml` - Server, database, and Firebase configuration
  - Module loading: `com.eros.ApplicationKt.module`
  - Port configuration
  - Database settings (host, port, name, credentials, connection pool)
  - Firebase settings (service account path, project ID)
- `app/src/main/resources/logback.xml` - Logging configuration
- `.env.example` - Example environment variables file

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
- **Authentication.kt** - Firebase authentication:
  - Firebase Admin SDK initialization
  - Firebase ID token verification
  - Bearer token authentication scheme ("firebase-auth")
  - Configured via `firebase` section in application.yaml
- **Routing.kt** - Request validation and route definitions

### Authentication Flow (Firebase)

The application uses **Firebase Authentication** for user identity management:

**Firebase handles**:
- User registration (email/password, phone, social providers)
- Password hashing and storage
- OTP generation and verification
- Email/phone verification
- JWT (ID token) generation and signing
- Token refresh

**Backend handles**:
- Firebase ID token verification
- User profile data storage and management
- Business logic and authorization

**Flow**:
1. Client authenticates with Firebase (via Firebase SDK)
2. Firebase returns ID token to client
3. Client sends ID token in `Authorization: Bearer <token>` header
4. Backend verifies token with Firebase Admin SDK
5. Backend extracts user info (UID, email, phone) from verified token
6. Backend syncs/retrieves user profile from local database

**Authentication Scheme**: `"firebase-auth"` (Bearer token - Firebase ID tokens are JWTs)
**Principal**: `FirebaseUserPrincipal` containing UID, email, phone, emailVerified
**Token Format**: `Authorization: Bearer <firebase-id-token>`

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
- `FIREBASE_SERVICE_ACCOUNT_PATH` - Path to Firebase service account JSON (required)
- `FIREBASE_PROJECT_ID` - Firebase project ID (required)
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

**auth/** - Authentication & Identity (Firebase Integration)
- **Purpose**: Sync Firebase-authenticated users with backend database
- **Owns**: User profile data linked to Firebase UIDs
- **Tables**: `users` (Firebase UID as primary key)
- **Routes**:
  - `POST /auth/sync-profile` - Sync Firebase user to backend database
  - `GET /auth/me` - Get current user profile
  - `DELETE /auth/delete-account` - Delete user account (GDPR compliance)
- **Firebase Integration**: Uses Firebase Admin SDK for token verification
- **Note**: Firebase handles passwords, OTP, email/phone verification, JWT tokens

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
- **Provides**: Common DTOs, extensions, shared utilities

### Feature Modules (Future)

- **matching/** - Matching algorithm and compatibility
- **dates/** - Date scheduling and management
- **notifications/** - Push notifications and messaging
- **wallet/** - Payment and transaction management

### Design Principles

1. **Auth vs Users Separation**
   - Auth handles syncing Firebase users with backend database
   - Users handles business-specific profile data (preferences, activity)
   - Firebase handles all security-critical operations (auth, verification)
   - Enables independent scaling and clear separation of concerns

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
      ├── firebase/         - Firebase configuration and authentication
      ├── tables/           - Exposed table definitions (Users)
      ├── repository/       - Data access layer (AuthRepository)
      ├── routes/           - Auth endpoints
      ├── models/           - DTOs and data classes
      └── validation/       - Input validators (Email, Phone, Age)

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
      └── V1__auth_tables.sql - Auth module tables (Firebase UID as PK)

common/
  └── src/main/kotlin/com/eros/common/
      └── (shared utilities and DTOs)
```

## Important Notes

- **Security Warning**: `HTTP.kt` currently uses `anyHost()` for CORS, which allows all origins. This should be restricted in production.
- **Firebase Setup Required**:
  - Download Firebase service account JSON from Firebase Console
  - Set `FIREBASE_SERVICE_ACCOUNT_PATH` and `FIREBASE_PROJECT_ID` environment variables
  - See `.env.example` for configuration template
- **Authentication**: All authentication is handled by Firebase. Backend only verifies tokens and manages user profiles.
- **Rate Limiting**: Currently applies TokenBucket (100 capacity, 10s rate) to all routes under `/`. Adjust in `Administration.kt` as needed.
- **GDPR Compliance**: Firebase handles user data deletion. Backend implements `DELETE /auth/delete-account` for profile cleanup.

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
