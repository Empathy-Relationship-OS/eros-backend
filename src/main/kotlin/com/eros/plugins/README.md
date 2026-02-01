# Plugins Module

## Overview

The Plugins Module configures and manages Ktor plugins that provide cross-cutting functionality across the entire EROS backend application.

## Core Responsibilities

- **Authentication Middleware**: JWT validation and user context injection
- **Error Handling**: Global exception handling and error response formatting
- **Request Validation**: Input validation and sanitization
- **Logging**: Structured logging and request/response tracking
- **Monitoring**: Health checks, metrics, and performance monitoring
- **CORS Configuration**: Cross-origin resource sharing policies
- **Rate Limiting**: Request throttling and abuse prevention
- **Content Negotiation**: JSON serialization/deserialization

## Key Plugins Configured

### Security
- **Authentication**: JWT bearer token validation
- **CORS**: Configured for mobile and web clients
- **Rate Limiting**: Protection against abuse (10 req/min on auth endpoints)
- **Request Validation**: Input sanitization and validation

### Monitoring & Logging
- **Call Logging**: Request/response logging with correlation IDs
- **Status Pages**: Custom error pages and exception handlers
- **Metrics**: Performance monitoring and health endpoints

### Content & Headers
- **Content Negotiation**: JSON request/response handling via kotlinx.serialization
- **Default Headers**: Security headers (X-Frame-Options, X-Content-Type-Options, etc.)
- **Compression**: Response compression for bandwidth optimization

## Plugin Configuration Files

```
plugins/
├── Authentication.kt    - JWT setup and user principal extraction
├── ErrorHandling.kt     - Global exception handlers
├── Validation.kt        - Request validation configuration
├── Monitoring.kt        - Logging and metrics setup
├── Security.kt          - CORS, rate limiting, security headers
└── Serialization.kt     - JSON configuration
```

## Installation Order

Plugin installation order matters in Ktor. The current order is:
1. Serialization (Content Negotiation)
2. Administration (Rate Limiting)
3. HTTP (CORS, Headers)
4. Monitoring (Call Logging)
5. Security (Authentication)
6. Routing (must be last)

## Integration with Feature Modules

Feature modules interact with plugins through:
- Route-level authentication requirements
- Custom validators for domain models
- Exception throwing (handled by global error handler)
- Logging context injection

---

**Status:** Core infrastructure - Configured during application bootstrap
