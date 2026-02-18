# Users Module Structure - Complete Reference

## 1. CreateUserRequest

**Location:** `users/src/main/kotlin/com/eros/users/models/User.kt`

All "sensitive" display-controlled fields are wrapped in `DisplayableField<T>`:
- `dateIntentions: DisplayableField<DateIntentions>`
- `relationshipType: DisplayableField<RelationshipType>`
- `kidsPreference: DisplayableField<KidsPreference>`
- `sexualOrientation: DisplayableField<SexualOrientation>`
- `ethnicity: DisplayableField<List<Ethnicity>>`

Plus visibility-controlled fields:
- `spokenLanguages: DisplayableField<List<Language>>`
- `religion: DisplayableField<Religion?>`
- `politicalView: DisplayableField<PoliticalView?>`
- `alcoholConsumption: DisplayableField<AlcoholConsumption?>`
- `smokingStatus: DisplayableField<SmokingStatus?>`
- `diet: DisplayableField<Diet?>`
- `pronouns: DisplayableField<Pronouns?>`
- `starSign: DisplayableField<StarSign?>`
- `brainAttributes: DisplayableField<List<BrainAttribute>?>`
- `brainDescription: DisplayableField<String?>`
- `bodyAttributes: DisplayableField<List<BodyAttribute>?>`
- `bodyDescription: DisplayableField<String?>`

## 2. User (Domain Entity)

**Location:** `users/src/main/kotlin/com/eros/users/models/User.kt`

Uses same DisplayableField wrapping pattern as CreateUserRequest for consistency.

## 3. UpdateUserRequest

**Location:** `users/src/main/kotlin/com/eros/users/models/User.kt`

All displayable fields are nullable with default `null`. Pattern:
- `null` = do not update this field
- `non-null DisplayableField<T>` = replace both value and display flag

## 4. DisplayableField<T>

```kotlin
@Serializable
data class DisplayableField<T>(
    val field: T,           // The actual value
    val display: Boolean    // Whether visible on profile
)
```

## 5. CityRepository Methods

All methods inherited from `IBaseDAO<Long, City>`:
- `suspend fun create(entity: City): City` → returns created City
- `suspend fun findById(id: Long): City?` → returns City or null
- `suspend fun findAll(): List<City>` → returns all cities
- `suspend fun update(id: Long, entity: City): City?` → returns updated City or null if not found
- `suspend fun delete(id: Long): Int` → returns rows affected
- `suspend fun doesExist(id: Long): Boolean` → returns boolean

## 6. CityRepositoryImpl

- Uses `insertReturning` to avoid round-trip refetch
- Preserves `createdAt`, sets `updatedAt` to now on updates
- Maps via `toCityDTO()` function

## 7. UserService Methods

| Method | Signature | Returns |
|--------|-----------|---------|
| `createUser()` | `CreateUserRequest → User` | The created User |
| `updateUser()` | `(String, UpdateUserRequest) → User?` | Updated User or null |
| `findByUserId()` | `String → User?` | User or null |
| `findByEmail()` | `String → User?` | User or null |
| `deleteUser()` | `String → Int` | Rows affected (1 or 0) |
| `userExists()` | `String → Boolean` | Boolean |

## 8. UserRepository Interface & Implementation

**Interface:** extends `IBaseDAO<String, User>`
- Adds: `suspend fun findByEmail(email: String): User?`

**Implementation details:**
- Uses Firebase UID (String) as PK
- Soft-deletes: sets `deletedAt` timestamp
- All queries automatically exclude soft-deleted records
- Email search is case-insensitive

## Key Architecture Decisions

1. **DisplayableField Pattern**: Every user preference field can be set to private/hidden
2. **Soft Deletes**: Users are never deleted, just marked with `deletedAt` timestamp
3. **Service Layer**: Handles DTO → Domain mapping; repositories work only with domain entities
4. **Generic BaseDAO**: All repositories inherit standard CRUD from `IBaseDAO<ID, T>`
5. **Clock Injection**: All timestamp generation is injectable for testability
