# Marketing Module README

## Overview

The Marketing Module is responsible for managing user marketing communication preferences and consent tracking. It provides a GDPR-compliant system for users to opt in/out of marketing communications while giving administrators tools to manage and query consent records.

## Core Responsibilities

- **Consent Management**: User opt-in/opt-out for marketing communications
- **GDPR Compliance**: Default opt-out with explicit consent requirement
- **User Authorization**: Users can only modify their own preferences
- **Admin Queries**: Tools for generating marketing email lists
- **Audit Trail**: Tracks creation and update timestamps for all consent records

## Architecture

### Key Components

The module follows the standard modular layered architecture:

**Routes Layer**: HTTP endpoints for preference management
**Service Layer**: Business logic and authorization checks
**Repository Layer**: Database operations via Exposed ORM
**Table Layer**: Database schema definitions

### File Structure

```
marketing/
├── routes/
│   └── MarketingRoutes.kt
├── service/
│   └── MarketingPreferenceService.kt
├── repository/
│   ├── MarketingRepository.kt (interface)
│   └── MarketingRepositoryImpl.kt
├── tables/
│   └── UserMarketingConsent.kt
└── models/
    └── UserMarketingConsent.kt (domain model + DTOs)
```

## API Endpoints

### User Endpoints (Authenticated)

#### GET /marketing/preference
Retrieves the authenticated user's marketing consent record. Returns default (marketingConsent = false) if no record exists.

**Response**: `MarketingPreferenceResponse`
```json
{
  "userId": "user123",
  "marketingConsent": false,
  "createdAt": "2024-01-15T10:00:00Z",
  "updatedAt": "2024-01-15T10:00:00Z"
}
```

#### POST /marketing/preference
Creates a new marketing consent record for the authenticated user.

**Request**: `CreateMarketingConsentRequest`
```json
{
  "marketingConsent": true
}
```

**Response**: `201 Created` with `MarketingPreferenceResponse`

#### PUT /marketing/preference
Updates existing marketing consent or creates new record if none exists (upsert behavior).

**Request**: `UpdateMarketingConsentRequest`
```json
{
  "marketingConsent": false
}
```

**Response**: `200 OK` with `MarketingPreferenceResponse`

### Admin Endpoints (ADMIN/EMPLOYEE Only)

#### GET /marketing/admin/preference/{userId}
Retrieves marketing consent for any user. Returns default if no record exists.

**Response**: `MarketingPreferenceResponse`

#### DELETE /marketing/admin/preference/{userId}
Deletes user's marketing consent record. Idempotent (no error if record doesn't exist).

**Response**: `204 No Content`

#### GET /marketing/admin/consented
Lists all users who have opted in to marketing communications.

**Response**: Array of `MarketingPreferenceResponse`
```json
[
  {
    "userId": "user1",
    "marketingConsent": true,
    "createdAt": "2024-01-15T10:00:00Z",
    "updatedAt": "2024-01-15T10:00:00Z"
  }
]
```

## Data Models

### Domain Model

**UserMarketingConsent**: Core domain model with userId as primary key
- `userId`: String - User's unique identifier (FK to users table)
- `marketingConsent`: Boolean - Consent status
- `createdAt`: Instant - Record creation timestamp
- `updatedAt`: Instant - Last modification timestamp

### Request DTOs

**CreateMarketingConsentRequest**: Used when creating new consent record
- `marketingConsent`: Boolean

**UpdateMarketingConsentRequest**: Used when updating existing consent
- `marketingConsent`: Boolean

### Response DTO

**MarketingPreferenceResponse**: Returned from all endpoints
- `userId`: String
- `marketingConsent`: Boolean
- `createdAt`: Instant (ISO-8601 format)
- `updatedAt`: Instant (ISO-8601 format)

## Database Schema

### Table: user_marketing_consent

```sql
CREATE TABLE user_marketing_consent (
    user_id VARCHAR(128) PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    marketing_consent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_marketing_consent_consent ON user_marketing_consent(marketing_consent);
```

**Key Design Decisions**:
- `user_id` as primary key (one consent record per user)
- Default `FALSE` for GDPR compliance
- Index on `marketing_consent` for efficient admin queries
- CASCADE delete maintains referential integrity

## Business Rules

### User Authorization
- Users can only **create**, **read**, and **update** their own preferences
- Users **cannot** delete their own records (admin-only operation)
- All user endpoints derive userId from authenticated Firebase token

### Admin Authorization
- Only users with `ADMIN` or `EMPLOYEE` roles can access admin endpoints
- Admins can view, delete, and query any user's consent records
- Role enforcement via `requireRoles()` plugin

### Default Behavior
- New users have **no record** until they explicitly set a preference
- GET requests for users without records return default: `marketingConsent = false`
- Default consent timestamp matches request time (not stored in DB)

### Upsert Pattern
- PUT operations create new record if none exists
- PUT operations update existing record if present
- Provides convenient single-endpoint preference management

### Idempotent Operations
- DELETE operations succeed even if record doesn't exist
- Prevents client-side error handling complexity
- Always returns `204 No Content` regardless of record existence

## GDPR Compliance

The module implements privacy-first principles:

1. **Explicit Opt-In**: No marketing consent assumed; defaults to `false`
2. **User Control**: Users can update preferences anytime
3. **Right to Erasure**: Admin delete endpoint supports data removal
4. **Audit Trail**: `createdAt` and `updatedAt` track consent history
5. **Cascade Deletion**: Consent records deleted when user account deleted

## Testing Strategy

### Unit Tests
**MarketingPreferenceServiceTest** (11 tests)
- Service layer business logic with mocked repository
- Authorization validation (ForbiddenException scenarios)
- Default consent behavior
- Upsert logic verification

### Integration Tests
**MarketingRepositoryImplTest** (14 tests)
- Database operations with PostgreSQL testcontainer
- CRUD operations validation
- Custom query methods (`findAllConsented`, `countConsented`)
- Foreign key constraint enforcement

### Route Tests
**MarketingRoutesTest** (21 tests)
- HTTP endpoint behavior with mock authentication
- Role-based authorization (user vs admin endpoints)
- Request/response serialization
- Error handling (401, 403, 404 scenarios)

**Coverage**: 46/46 tests passing (100% pass rate)

## Dependencies

- **Auth Module**: Firebase authentication and role verification
- **Users Module**: User table foreign key reference
- **Database Module**: Exposed ORM and Flyway migrations
- **Common Module**: Shared error classes and serializers

## Success Metrics

✅ Users can opt in/out of marketing at any time
✅ Default opt-out respects GDPR requirements
✅ Admins can generate accurate marketing lists
✅ Authorization prevents unauthorized access
✅ Audit trail maintains compliance documentation
✅ All operations complete in <100ms

## Future Enhancements

**Granular Consent** (Phase 2):
- Separate flags for email, SMS, and push notifications
- Channel-specific opt-in/opt-out management

**Consent History** (Phase 2):
- Track all consent changes over time
- Immutable audit log for compliance reporting

**Integration Points** (Phase 2):
- Webhook notifications to email service on consent changes
- Export API for marketing platform integrations
- Analytics dashboard for consent trends

## Development Notes

### Adding New Consent Types

To add granular consent (e.g., email vs SMS):

1. Add columns to migration: `email_consent`, `sms_consent`
2. Update `UserMarketingConsent` domain model
3. Modify DTOs to include new fields
4. Update service validation logic
5. Add corresponding tests

### Performance Considerations

- Index on `marketing_consent` enables fast admin queries
- Single-record-per-user design minimizes database bloat
- No caching required due to low query frequency
- Consider materialized view if consent analytics become complex
