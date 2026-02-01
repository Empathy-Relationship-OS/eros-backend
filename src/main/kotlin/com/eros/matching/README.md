# Matching Module README

## Overview

The Matching Module is a critical Phase 1 component (weeks 2-3) responsible for daily match batch generation, matchmaking algorithms, and mutual match detection. It delivers 8 profiles daily at 7 PM to active users while respecting preferences and preventing repeat matches.

## Core Responsibilities

- **Daily Batch Generation**: 8 profiles delivered at 7 PM to active users
- **Preference Filtering**: Gender, age, height, and location matching
- **Match Creation**: Mutual like detection with 48-hour commitment deadlines
- **Cooldown Management**: 30-day minimum between repeat matches
- **Redis Caching**: 24-hour batch caching for performance
- **Fair Distribution**: Visibility tracking for users with lower engagement

## Architecture

### Key Components

The module follows a layered architecture:

**Routes Layer**: HTTP endpoints expose the matching API
**Service Layer**: Business logic orchestration
**Algorithm Layer**: Configurable matching strategies
**Repository Layer**: Database operations
**Cache Layer**: Redis-backed performance optimization

### File Structure

```
matching/
├── MatchingRoutes.kt
├── MatchingService.kt
├── MatchingRepository.kt
├── BatchGenerationService.kt
├── algorithm/
│   ├── MatchingAlgorithm.kt (interface)
│   ├── SimpleRandomMatcher.kt
│   ├── EloBasedMatcher.kt
│   ├── ScoringEngine.kt
│   ├── CooldownManager.kt
│   └── FairnessTracker.kt
├── models/
├── jobs/
└── cache/

```

## Matching Algorithm

### Phase 1: Random Selection (MVP)

Filters candidates by preferences, excludes cooldown users, then randomly selects 8 profiles. Prioritizes speed (2-3 day implementation) over optimization.

### Phase 2: ELO-Based Scoring (Future)

Weighted multi-factor approach:

- **Preference Matching** (40%): Gender, age, height, location, activities
- **ELO Ranking** (30%): Users matched with similar reputation scores
- **Engagement** (20%): Activity recency and date completion history
- **Fairness** (10%): Visibility distribution across user base

Final scores determine the top 8 candidates, ensuring "balanced matches while maintaining user engagement."

## API Endpoints

### GET /matches/daily
Retrieves today's batch with 8 profiles. Returns profile summaries including shared interests and distance. Response indicates action status (PENDING, LIKED, PASSED).

### POST /matches/{batchId}/like/{userId}
Records a like action. If mutual like detected, returns 201 with match details and 48-hour commitment deadline. Otherwise returns 200 with confirmation.

### POST /matches/{batchId}/pass/{userId}
Registers pass action and creates 30-day cooldown preventing future matches with that user.

### GET /matches/{matchId}
Fetches full match details including hours remaining for commitment deadline.

### GET /matches
Lists all matches with status filtering (PENDING_COMMITMENT, COMMITTED, COMPLETED, etc.) and pagination support.

## Data Models

**MatchBatch**: Represents 8 profiles with release/expiry times and active status tracking

**ProfileAction**: Records LIKED or PASSED actions with unique constraint per user pair

**Match**: Mutual match record with 48-hour commitment deadline and status progression

**MatchCooldown**: Exclusion entries with reason-based duration (30-90 days depending on cancellation/no-show)

## Scheduled Jobs

Daily batch generation runs at 7 PM UTC via ScheduledExecutorService. The job:

1. Fetches all active users with 50%+ profile completeness
2. Processes users in parallel chunks (100 per batch)
3. Invokes matching algorithm for each user
4. Persists batches to database and Redis cache
5. Sends push notifications upon completion

Target performance: "Batch generation < 5 seconds per user" across the entire operation.

## Caching Strategy

Redis implements cache-aside pattern for match batches:

- **match:batch:{userId}**: 24-hour TTL for active batches
- **match:actions:{userId}:{batchId}**: User action history per batch
- **match:cooldown:{userId}**: SET of excluded user IDs with sliding expiry
- **match:fairness:{userId}**: Last shown timestamp for fairness tracking

Cache misses trigger database queries with automatic cache population for subsequent requests.

## Testing Strategy

**Unit Tests**: Algorithm behavior, scoring logic, cooldown calculations with mocked dependencies

**Integration Tests**: Full match creation flow, mutual like detection, database persistence with embedded PostgreSQL

**Coverage Target**: >80% for matching-critical code paths

## Development Roadmap

**Epic Scope**: 43 story points (~6-7 engineer-days)

**Critical Path**: Schema → Models → Repository → Cooldowns → RandomMatcher → BatchService → MatchingService → Routes → Scheduler

**Phase 2 Enhancements** (deferrable): ELO-based matching, advanced fairness tracking, A/B testing framework

### Definition of Done

- Daily batches generate reliably at 7 PM
- Like/pass actions function correctly
- Mutual matches create records and notifications
- Cooldowns prevent 30-day repeats
- Cache layer operational
- Unit/integration tests passing
- Performance benchmarks met
- Code reviewed and documented

## Dependencies

- PostgreSQL (match storage and cooldown tracking)
- Redis (batch and action caching)
- User Module (profile data and preferences)
- Notification Service (push alerts)
- Reputation Module (ELO scores for Phase 2)

## Success Metrics

Every active user receives 8 daily matches; matches respect preferences; no repeats within 30 days; mutual likes create instant matches; batch generation completes in <5 seconds per user; fair visibility distribution enforced.
