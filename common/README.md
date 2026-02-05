# Common Module

## Overview

The Common Module provides shared utilities, extensions, and cross-cutting concerns used across all feature modules in the EROS backend.

## Core Responsibilities

- **Extensions**: Kotlin extension functions for common operations
- **Error Hierarchy**: Standardized exception classes and error handling
- **Validators**: Reusable validation logic for business rules
- **Security Utilities**: Cryptography helpers, token generation, and security functions
- **Configuration**: Shared configuration models and utilities
- **Constants**: Application-wide constants and enumerations

## Key Components

### Error Handling
- Custom exception hierarchy (BusinessException, ValidationException, NotFoundException, etc.)
- Error response models for consistent API error formatting
- Error codes and message templates

### Validators
- Email and phone number validation
- Date range validation
- Geographic coordinate validation
- Input sanitization utilities

### Extensions
- String manipulation helpers
- Date/time utilities
- Collection extensions
- Null-safety helpers

### Security
- Password strength validation
- Token generation utilities
- Encryption/decryption helpers
- Sanitization functions

### Configuration
- Environment variable parsing
- Feature flags
- Application settings models

## Design Principles

- **Zero Dependencies on Feature Modules**: Common only depends on external libraries, never on feature modules
- **Pure Functions**: Most utilities are stateless and side-effect-free
- **Testability**: All utilities have comprehensive unit test coverage
- **Reusability**: Generic, composable functions that solve common problems

## Usage Guidelines

Feature modules should:
- Import only what they need from common
- Never modify common code for feature-specific needs
- Propose additions to common when utilities could benefit multiple modules
- Follow the established patterns and conventions

---

**Status:** Foundation module - Required by all feature modules
