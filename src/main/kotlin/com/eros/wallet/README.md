# Wallet Module - Complete Documentation

## Overview

The Wallet Module is a critical financial system for managing user token balances, Stripe payment processing, and automated refunds. It's built in Kotlin for the eros-backend platform.

**Key Details:**
- Owner: Engineer 2
- Priority: 🔴 Critical (Phase 1)
- Estimated Effort: 5-6 days
- Dependencies: auth/, users/, matching/, database/

## Core Responsibilities

The module handles:
- User token balance management and tracking
- Secure token purchases via Stripe payment processing
- Token spending for date commitments
- Automated refunds when partners cancel dates
- Complete transaction history with audit capabilities
- Webhook event handling from Stripe
- Prevention of duplicate charges through idempotency
- Fraud detection integration (planned for Phase 2)

## Token Economy

**Pricing Tiers:**
- STARTER: 5 tokens for £25/$30
- POPULAR: 10 tokens for £45/$54
- PREMIUM: 20 tokens for £80/$96
- MEGA: 50 tokens for £180/$210

**Activity Costs:**
Activities like coffee (0.5 tokens), drinks (1.0), and dinner (1.5) determine spending amounts.

## Architecture Components

The module includes eight primary services:

1. **WalletService** - Orchestrates business logic and balance management
2. **WalletRepository** - Manages database operations with ACID guarantees
3. **TransactionService** - Creates and validates transaction records
4. **RefundService** - Processes automated refunds based on cancellation timing
5. **PaymentService** - Handles Stripe payment orchestration
6. **WebhookHandler** - Processes payment confirmation events
7. **StripeClient** - Wraps Stripe API calls
8. **IdempotencyService** - Prevents duplicate transactions

## Key API Endpoints

**GET /wallet/balance**
Returns current token count with lifetime statistics

**POST /wallet/purchase**
Initiates token purchase, returns Stripe client secret for payment confirmation

**GET /wallet/transactions**
Retrieves paginated transaction history with filtering options

**POST /wallet/spend**
Deducts tokens for date commitments with balance validation

**POST /webhooks/stripe**
Processes Stripe payment events (public endpoint, signature-verified)

## Transaction Safety

All financial operations use database transactions with row-level locking to prevent race conditions. Idempotency keys ensure that retried requests don't create duplicate charges. The system stores a unique key for each operation, allowing safe retry logic.

Database transactions utilize `FOR UPDATE` locks that prevent concurrent modifications to wallet records during critical operations.

## Refund Policy

- **Partner cancellation:** 100% refund to both participants
- **Early self-cancellation (>24h before):** 50% refund
- **Late self-cancellation (<24h before):** No refund

## Security Measures

- PCI compliance (no credit card data stored)
- Stripe webhook signature verification prevents spoofing
- HTTPS requirement for all endpoints
- Comprehensive audit logging of all transactions
- Encrypted sensitive data storage
- Database constraints prevent negative balances

## Testing Strategy

The implementation includes:
- Unit tests for all services with mocked dependencies
- Integration tests using Stripe test mode
- End-to-end purchase flow validation
- Webhook processing simulation
- Refund scenario coverage (full, partial, none)

## Development Tickets (13 Total)

**Critical Path:** Database schema → Models → Repository → Stripe config → Payment intents → Webhooks → Services → Routes

**Parallelizable:** Refund service, idempotency layer, validators

**Total: 33 story points (~5-6 engineer-days)**

## Before Production

- External security audit required
- Stripe test mode comprehensive validation
- Fraud detection integration (Stripe Radar)
- Financial reconciliation procedures
- Code review by both engineers
- Real Stripe test card testing

---

**Status:** Complete and ready for sprint planning
**Last Updated:** January 2025
