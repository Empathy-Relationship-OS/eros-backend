# Dates Module - Complete Documentation

## Overview

The Dates Module is a critical component handling the complete lifecycle of user dates, from mutual commitment through post-date feedback. According to the technical specification, it manages "date commitments (both users commit within 48h)" and orchestrates token spending, venue selection, and a 7-stage timeline system.

## Key Responsibilities

The module oversees:
- Date commitment windows (48-hour decision period)
- Token deduction coordination with the Wallet service
- Automatic venue assignment 2-3 days before dates
- Seven-stage timeline progression from match to completion
- Cancellation handling with refund logic
- Pre-date chat access (4 hours before)
- Post-date feedback collection and processing
- Optional phone number exchange between users

## Architecture Overview

**File Structure:**
```
dates/
├── DateRoutes.kt (HTTP endpoints)
├── DateService.kt (business orchestration)
├── DateRepository.kt (database access)
├── CommitmentService.kt (commitment logic)
├── CancellationService.kt (cancellation handling)
├── VenueService.kt (venue operations)
├── TimelineService.kt (stage management)
├── FeedbackService.kt (post-date processing)
├── models/ (domain objects)
├── venues/ (venue-specific logic)
├── timeline/ (stage progression)
└── validation/ (business rules)
```

## Seven-Stage Timeline

The system progresses through distinct stages:

1. **MATCHED** - Mutual attraction detected; instant trigger
2. **DEPOSIT_PENDING** - 48-hour commitment window; both users must decide
3. **DEPOSIT_PAID** - Both committed; tokens deducted immediately
4. **DATE_SCHEDULING** - System selects venue and time (1-3 days)
5. **LOCATION_CONFIRMED** - Venue assigned; cancellable until 12 hours prior
6. **FINAL_CONFIRMATION** - Final attendance confirmation (12 hours before)
7. **DATE_DAY** - Active date period with full chat access
8. **POST_DATE_FEEDBACK** - 24-hour feedback window after date concludes

## API Endpoints

**GET /dates/{dateId}** - Retrieve complete date details with timeline, venue information, and commitment status

**POST /dates/{matchId}/commit** - Record user commitment with activity preferences and availability; triggers token spending when both users commit

**GET /dates** - List user's dates with filtering by status (upcoming, pending commitment, past, cancelled)

**POST /dates/{dateId}/cancel** - Cancel date with reason and calculate refunds based on timing: partner receives full refund; self-cancellation >24h before = 50% refund; <24h = no refund

**POST /dates/{dateId}/feedback** - Submit attendance and rating after date concludes; enables phone number exchange if both attended and indicated interest

**POST /dates/{dateId}/confirm** - Confirm final attendance during the 12-hour final confirmation window

## Core Data Models

**DateCommitment** - Central entity tracking the relationship between two users, including timeline stage, token cost (1.0 per activity), scheduled time, venue assignment, and commitment timestamps

**Venue** - Location details with activity compatibility, operating hours, partner status, reservation requirements, and historical success metrics

**DateFeedback** - User ratings (1-5 stars), attendance confirmation, willingness to meet again, and optional phone number sharing preference

**DateTimeline** - Progression visualization showing completed, current, and pending stages with descriptions and timestamps

## Business Logic Flows

**Commitment Process:**
1. User submits commitment with activity type and preferred cities/times
2. System validates 48-hour deadline hasn't expired
3. System checks sufficient token balance
4. When both users commit: wallet service deducts tokens from both accounts simultaneously
5. Date stage advances to DEPOSIT_PAID
6. Venue scheduling job is triggered
7. Both users receive notifications

**Cancellation Process:**
1. User initiates cancellation with reason
2. System validates date is in cancellable stage (not completed or no-show)
3. Hours until scheduled date calculated
4. Refunds processed: canceller receives partial refund based on timing; partner always receives full refund
5. 60-day cooldown applied to cancelling user
6. Partner receives notification with refund amount

**Post-Date Feedback:**
1. After scheduled date time passes, feedback window opens for 24 hours
2. Each user submits attendance status and optional rating
3. When both submit: system checks if both attended
4. If no-show detected: penalties applied via reputation service
5. If both attended and marked "would date again": phone numbers exchanged
6. Date marked as COMPLETED or NO_SHOW

## Venue Management

The system includes a venue selection algorithm that:
- Identifies mutual city preferences between two users
- Filters venues by activity type and operational status
- Scores venues based on partner status (official partnerships prioritized), customer ratings, and historical success rates
- Selects highest-scoring option
- Falls back to user preference if no mutual city agreement

A background job runs every 6 hours to automatically assign venues to dates in the scheduling stage, generating reservation codes for non-partner venues in the MVP phase.

## Development Approach

The module requires 14 focused development tickets totaling approximately 39 story points (5-6 engineer-days):

1. Database schema creation with proper indexing
2. Domain models and state machine enums
3. DateRepository for data persistence
4. VenueRepository with test data
5. CommitmentService with token integration
6. Venue selection algorithm implementation
7. Timeline progression logic
8. CancellationService with refund calculations
9. FeedbackService with ELO updates
10. DateService orchestration layer
11. Venue scheduling background job
12. Commitment deadline checker job
13. HTTP route handlers
14. Request/response DTOs

## Testing Strategy

Comprehensive testing includes:

- Unit tests for each service validating commitment deadlines, refund calculations, and stage transitions
- Repository tests using in-memory databases
- Integration tests simulating complete date lifecycle from match through feedback
- Edge case coverage for deadline expiration, insufficient tokens, and concurrent operations
- Scheduled job testing with time advancement utilities

## Dependencies

The module integrates with:
- **Wallet Service** - Token spending and refund processing
- **Matching Module** - Match creation triggers
- **Notification Service** - Timeline and reminder communications
- **Chat Service** - Pre-date messaging (4 hours before)
- **Reputation Service** - ELO calculations post-date
- **Users Module** - Profile information and phone number storage

## Success Criteria

✓ Users commit to dates within 48-hour windows
✓ Tokens deducted only after mutual commitment
✓ Automatic venue assignment 2-3 days prior
✓ Timeline visibility for both participants
✓ Appropriate refunds for cancellations
✓ Pre-date chat access opens 4 hours before
✓ Post-date feedback captured successfully
✓ Phone number exchange when both agree
