# Users Module - Complete Documentation

## Overview

The Users Module manages comprehensive profile functionality for the EROS dating platform. According to the documentation, it "handles user profile management (CRUD operations), photo uploads and ordering, profile preferences, profile visibility controls, photo moderation workflow, and public profile views for matching."

## Key Responsibilities

- User profile creation and updates
- Photo management with cloud storage integration
- Dating preference configuration
- Profile visibility controls (Active/Sleep Mode)
- Photo moderation workflows
- Public profile exposure for matching

## Critical Success Metrics

The module succeeds when users can create comprehensive profiles, upload photos reliably with proper ordering, achieve profile completeness tracking (50%+ required for matching), ensure preferences impact matchmaking, implement sleep mode functionality, and maintain privacy in public-facing profiles.

## Core API Endpoints

**Profile Management:**
- `GET /users/me` - Retrieve complete private profile
- `PATCH /users/me/profile` - Update profile content
- `PATCH /users/me` - Modify basic information
- `PATCH /users/me/visibility` - Toggle profile status

**Photo Operations:**
- `POST /users/me/photos` - Upload new photo
- `PUT /users/me/photos/reorder` - Reorganize photo sequence
- `DELETE /users/me/photos/{photoId}` - Remove photo

**Preference Configuration:**
- `PATCH /users/me/preferences` - Set dating preferences
- `GET /users/{userId}/public` - Access sanitized profile for matching

## Data Architecture

The module uses six primary domain models:

**User Model:** Contains identity data including email, phone, name, birthdate, gender, height, location, education, occupation, profile status, verification status, ELO score, and badges.

**UserProfile Model:** Stores biographical content—photos, bio text, hobbies, personality traits, Q&A responses, lifestyle habits, and relationship goals. Includes a completeness percentage calculation method.

**Photo Model:** Manages individual images with metadata including CDN URL, thumbnail URL, position sequencing, moderation status, and upload timestamp.

**UserPreferences Model:** Defines matching criteria encompassing gender preferences, age range, height range, maximum distance, preferred cities, languages, activities, and reach level flexibility.

**Request/Response DTOs:** Separate classes handle data validation and serialization for API communication while maintaining security through field filtering.

## Profile Completeness Scoring

The system calculates profile completeness across eight dimensions:
- Photos: 30 points
- Biography: 10 points
- Hobbies: 15 points
- Personality traits: 15 points
- Q&A responses: 10 points
- Lifestyle habits: 10 points
- Relationship goals: 10 points

Users must achieve 50% completion before becoming eligible for the matching system.

## Photo Upload Strategy

Documentation recommends Cloudinary for MVP implementation due to "automatic thumbnail generation, built-in image transformations, CDN included, and AI-powered auto-tagging." File requirements include JPEG/PNG/HEIC formats, 500KB-10MB size range, and minimum 800x800px dimensions. Users can maintain up to 6 photos total.

## Security & Privacy Controls

The public profile endpoint excludes sensitive information: "No email or phone number, no exact coordinates (only city + distance), no ELO score (unless trusted user)." Public profile access requires either matched status or membership in the same matching batch.

## Development Roadmap

The implementation requires 12 interconnected tickets estimated at 38 story points:

1. Database schema creation
2. Domain model implementation
3. Request/Response DTOs
4. UserRepository data access layer
5. PhotoService cloud integration
6. Photo metadata management
7. Validation logic
8. UserService business orchestration
9. PreferenceService functionality
10. HTTP endpoint routing
11. Access control implementation
12. Completeness calculation

## Testing Coverage

The specification includes unit tests for domain logic, validation rules, and service operations, plus integration tests validating complete CRUD workflows and photo upload flows with database transactions.

**Status:** Complete and ready for sprint planning, estimated delivery within 5-6 engineer-days.
