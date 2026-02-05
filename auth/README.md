# Auth Module README

## Overview

The Authentication Module is a critical Phase 1 component handling user registration, login, phone verification, and JWT token management. Owned by Engineer 1, it requires 3-4 days to complete and depends on the common and database modules.

## Core Responsibilities

The module manages:
- User registration with email and phone verification
- Secure login with JWT token generation
- Phone number verification through SMS OTP
- Token refresh capabilities (Phase 2)
- Password reset workflows (Phase 2)

## Success Metrics

Implementation should achieve:
- Complete user registration with email and phone support
- 6-digit OTP verification via SMS
- Passwords secured using Bcrypt with cost factor 12
- JWT tokens expiring after 7 days
- Protected endpoints rejecting invalid or expired tokens
- Rate limiting of 10 requests per minute per IP on auth endpoints

## Key Dependencies

- PostgreSQL for credential storage
- Twilio for SMS OTP delivery (optional in MVP; mock service available)
- Redis for token blacklisting (Phase 2)

## API Endpoints

**POST /auth/register**: Creates new user accounts with email, phone, password, name, and birth date. Returns user ID and verification message upon success.

**POST /auth/verify-phone**: Confirms phone ownership using a 6-digit OTP code. Updates user verification status.

**POST /auth/login**: Authenticates users and returns JWT access tokens. Requires phone verification and active account status.

**Token Validation**: Protected routes use middleware to verify JWT signatures, check expiration, and extract user identity from claims.

## Security Architecture

Passwords are hashed using Bcrypt with cost 12, generating unique salts per password. JWTs use HS256 algorithm with environment-stored secret keys (minimum 256 bits). OTPs are hashed in the database with 10-minute expiration and rate-limited to 5 verification attempts. All auth endpoints enforce rate limiting at 10 requests per minute per IP address.

## Development Approach

The implementation follows layered architecture: routes handle HTTP concerns, services orchestrate business logic, repositories manage data access, and models define request/response contracts. Comprehensive testing includes unit tests with mocking and integration tests using Testcontainers.

## Implementation Timeline

Ten development tickets organize the work: database schema setup, password hashing utilities, JWT configuration, request/response models, repository layer, service logic, HTTP routes, authentication plugin configuration, validators, and OTP/SMS integration. Success requires all unit tests passing with 80%+ coverage, integration testing, peer review, and smoke testing before deployment.
