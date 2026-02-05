# Database Module

## Overview

The Database Module provides centralized database schema definitions, migrations, and data access infrastructure for the EROS backend using PostgreSQL and Exposed ORM.

## Core Responsibilities

- **Schema Definitions**: Centralized table definitions using Exposed DSL
- **Database Migrations**: Version-controlled schema changes via Flyway
- **Connection Management**: Database connection pooling and configuration
- **Transaction Management**: Database transaction utilities and helpers
- **Query Utilities**: Common query patterns and database helpers

## Technology Stack

- **PostgreSQL**: Primary relational database
- **Exposed**: Kotlin SQL framework for type-safe queries
- **Flyway**: Database migration management
- **HikariCP**: Connection pooling
- **Redis**: Caching layer (managed separately)

## Schema Organization

Tables are logically organized by feature module:

```
database/
├── schema/
│   ├── UserTables.kt           - users, user_profiles, photos, preferences
│   ├── AuthTables.kt           - auth_credentials, otp_codes
│   ├── MatchingTables.kt       - match_batches, matches, cooldowns
│   ├── DatesTables.kt          - date_commitments, venues, feedback
│   ├── WalletTables.kt         - wallets, transactions
│   └── NotificationTables.kt   - notification_logs, preferences
├── migrations/
│   ├── V1__initial_schema.sql
│   ├── V2__add_user_preferences.sql
│   └── ...
├── DatabaseConfig.kt
└── TransactionManager.kt
```

## Migration Strategy

- **Flyway-based**: All schema changes tracked in versioned SQL files
- **Environment-specific**: Separate migration paths for dev/staging/production
- **Rollback Support**: Down migrations for critical changes
- **Seed Data**: Development fixtures for local testing

## Database Design Principles

- **Logical Separation**: Tables organized by feature domain
- **Single PostgreSQL Instance**: Logical separation, not physical
- **Foreign Key Constraints**: Enforce referential integrity
- **Indexing Strategy**: Indexes on frequently queried columns
- **Soft Deletes**: Use status columns instead of hard deletes where appropriate

## Connection Configuration

Database connections are configured via environment variables:
- `DB_HOST`: Database host (default: localhost)
- `DB_PORT`: Database port (default: 5432)
- `DB_NAME`: Database name
- `DB_USER`: Database username
- `DB_PASSWORD`: Database password
- `DB_POOL_SIZE`: Connection pool size (default: 10)

## Development Workflow

1. **Create Migration**: Add new Flyway migration file with `V{version}__{description}.sql`
2. **Update Schema**: Add corresponding Exposed table definitions
3. **Run Migration**: Flyway automatically applies on application start
4. **Test**: Verify migration in local environment
5. **Review**: Peer review schema changes before merge

## Testing

- **Testcontainers**: Embedded PostgreSQL for integration tests
- **H2**: In-memory database for unit tests (where appropriate)
- **Test Fixtures**: Reusable test data builders

---

**Status:** Core infrastructure - Foundation for all data persistence
