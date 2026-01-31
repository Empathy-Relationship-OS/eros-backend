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
   - `configureSerialization()` - Content negotiation with JSON
   - `configureAdministration()` - Rate limiting
   - `configureHTTP()` - CORS and default headers
   - `configureMonitoring()` - Call logging
   - `configureSecurity()` - Authentication (OAuth + JWT)
   - `configureRouting()` - Routes and request validation

**Important**: Plugin installation order matters in Ktor. Security and serialization must be configured before routing.

### Configuration Files

- `src/main/resources/application.yaml` - Server and JWT configuration
  - Module loading: `com.eros.ApplicationKt.module`
  - Port configuration
  - JWT settings (domain, audience, realm)
- `src/main/resources/logback.xml` - Logging configuration

### Modular Plugin Structure

Each configuration function lives in its own file:

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

### Request Validation

Global validation is configured in `Routing.kt`:
- String body validation requires text to start with "Hello"
- Custom validators should be added to the `RequestValidation` plugin

### Error Handling

Status pages are configured to catch all `Throwable` and return 500 with error details. Customize in `Routing.kt:configureRouting()`.

## Project Structure

```
src/main/kotlin/
  ├── Application.kt      - Entry point and module orchestration
  ├── Serialization.kt    - JSON content negotiation
  ├── Administration.kt   - Rate limiting
  ├── HTTP.kt            - CORS and headers
  ├── Monitoring.kt      - Call logging
  ├── Security.kt        - OAuth + JWT authentication
  └── Routing.kt         - Routes and validation

src/main/resources/
  ├── application.yaml   - Ktor and JWT config
  └── logback.xml       - Logging config

src/test/kotlin/
  └── ApplicationTest.kt - Test suite
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
