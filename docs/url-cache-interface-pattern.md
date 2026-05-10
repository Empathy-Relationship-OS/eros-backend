# URL Cache Interface Pattern

## Overview

The URL caching system uses the **Strategy Pattern** with dependency injection to allow swapping cache implementations without changing service code.

## Architecture

```
┌─────────────────────────────────┐
│    CloudFrontSignerService      │
│                                 │
│  - Depends on UrlCache          │ ← Interface (abstraction)
│  - Doesn't care about           │
│    implementation details       │
└────────────┬────────────────────┘
             │ depends on
             ↓
      ┌──────────────┐
      │   UrlCache   │ ← Interface
      │  (interface) │
      └──────┬───────┘
             │ implemented by
      ┌──────┴───────┬─────────────┬──────────────┐
      ↓              ↓             ↓              ↓
┌─────────────┐ ┌───────────┐ ┌─────────┐ ┌──────────────┐
│ InMemory    │ │   Redis   │ │Memcached│ │   Custom     │
│ UrlCache    │ │ UrlCache  │ │UrlCache │ │Implementation│
└─────────────┘ └───────────┘ └─────────┘ └──────────────┘
```

## Benefits

### 1. Dependency Inversion Principle (SOLID)

**Before (Concrete Dependency):**
```kotlin
class CloudFrontSignerService(
    private val cache: InMemoryUrlCache  // ❌ Depends on concrete class
)
```

**After (Interface Dependency):**
```kotlin
class CloudFrontSignerService(
    private val cache: UrlCache  // ✅ Depends on abstraction
)
```

### 2. Easy to Swap Implementations

Change cache implementation **without touching CloudFrontSignerService**:

```kotlin
// Development: Use in-memory cache (fast, single-instance)
val devCache = InMemoryUrlCache()
val cloudFrontSigner = CloudFrontSignerService(s3Config, devCache)

// Production: Use Redis cache (shared, multi-instance)
val prodCache = RedisUrlCache(redisClient)
val cloudFrontSigner = CloudFrontSignerService(s3Config, prodCache)

// Testing: Use no-op cache (always generate fresh URLs)
val testCache = NoOpUrlCache()
val cloudFrontSigner = CloudFrontSignerService(s3Config, testCache)
```

### 3. Testability

Easy to mock or stub for unit tests:

```kotlin
@Test
fun `should cache URLs correctly`() {
    // Use a test double
    val mockCache = mockk<UrlCache>()
    val signer = CloudFrontSignerService(s3Config, mockCache)

    every { mockCache.getOrGenerate(any(), any(), any()) } returns "cached-url"

    val url = signer.generateSignedUrl("photos/test.jpg", 48)

    assertEquals("cached-url", url)
    verify { mockCache.getOrGenerate("photos/test.jpg:48", 48, any()) }
}
```

### 4. Open/Closed Principle (SOLID)

Open for extension (add new implementations), closed for modification (don't change existing code).

## Available Implementations

### InMemoryUrlCache (Default)

**Use when:**
- Single-instance deployment
- Development/testing
- Low latency requirements
- Acceptable to lose cache on restart

**Characteristics:**
- ✅ Fast (in-memory HashMap lookup)
- ✅ Simple (no external dependencies)
- ✅ Thread-safe (ConcurrentHashMap)
- ⚠️ Not shared across instances
- ⚠️ Lost on restart

**Example:**
```kotlin
val cache = InMemoryUrlCache()
val signer = CloudFrontSignerService(s3Config, cache)
```

---

### RedisUrlCache (Stub - Future Implementation)

**Use when:**
- Multi-instance deployment
- Production environments
- Horizontal scaling
- Cache persistence required

**Characteristics:**
- ✅ Shared across all instances
- ✅ Survives restarts
- ✅ Distributed cache
- ⚠️ Network latency (~1-2ms)
- ⚠️ External dependency (Redis server)

**Example (when implemented):**
```kotlin
val redisClient = RedisClient.create("redis://localhost:6379")
val cache = RedisUrlCache(redisClient)
val signer = CloudFrontSignerService(s3Config, cache)
```

---

### Custom Implementation

Implement `UrlCache` interface for your own caching strategy:

```kotlin
class MultiTierUrlCache(
    private val l1Cache: InMemoryUrlCache,
    private val l2Cache: RedisUrlCache
) : UrlCache {
    override fun getOrGenerate(
        key: String,
        expiryHours: Long,
        generator: () -> String
    ): String {
        // Try L1 (in-memory) first
        val l1Result = l1Cache.getOrGenerate(key, expiryHours) { "" }
        if (l1Result.isNotBlank()) return l1Result

        // Fall back to L2 (Redis)
        return l2Cache.getOrGenerate(key, expiryHours, generator)
    }

    // ... implement other methods
}
```

## Interface Definition

```kotlin
interface UrlCache {
    /**
     * Gets a cached URL or generates a new one.
     */
    fun getOrGenerate(
        key: String,
        expiryHours: Long,
        generator: () -> String
    ): String

    /**
     * Invalidates a specific cache entry.
     */
    fun invalidate(key: String)

    /**
     * Invalidates all entries for a user.
     */
    fun invalidateUser(userId: String)

    /**
     * Clears the entire cache.
     */
    fun clear()

    /**
     * Returns cache statistics.
     */
    fun getStats(): CacheStats

    data class CacheStats(
        val size: Int,
        val entries: List<String>,
        val implementation: String
    )
}
```

## Configuration via Dependency Injection

### Option 1: Constructor Injection (Current)

```kotlin
class CloudFrontSignerService(
    private val s3Config: S3Config,
    private val urlCache: UrlCache = InMemoryUrlCache()  // Default
)
```

**Usage:**
```kotlin
// Use default (InMemoryUrlCache)
val signer = CloudFrontSignerService(s3Config)

// Provide custom implementation
val redisCache = RedisUrlCache()
val signer = CloudFrontSignerService(s3Config, redisCache)
```

### Option 2: Factory Pattern (Future)

```kotlin
object UrlCacheFactory {
    fun create(config: CacheConfig): UrlCache {
        return when (config.type) {
            CacheType.IN_MEMORY -> InMemoryUrlCache()
            CacheType.REDIS -> RedisUrlCache(config.redisConfig)
            CacheType.MEMCACHED -> MemcachedUrlCache(config.memcachedConfig)
        }
    }
}

// Usage
val cache = UrlCacheFactory.create(appConfig.cacheConfig)
val signer = CloudFrontSignerService(s3Config, cache)
```

### Option 3: Dependency Injection Framework (Future)

Using Koin, Kodein, or similar:

```kotlin
// Module definition
val appModule = module {
    single<UrlCache> {
        when (getProperty("cache.type")) {
            "redis" -> RedisUrlCache(get())
            else -> InMemoryUrlCache()
        }
    }
    single { CloudFrontSignerService(get(), get()) }
}

// Usage
val signer: CloudFrontSignerService by inject()
```

## Migration Path: In-Memory → Redis

### Phase 1: Current State (In-Memory)

```kotlin
// PhotoService instantiation
class PhotoService(
    // ...
    private val cloudFrontSigner: CloudFrontSignerService =
        CloudFrontSignerService(s3Config)  // Uses InMemoryUrlCache by default
)
```

### Phase 2: Add Redis Implementation

1. Add Redis dependency:
   ```kotlin
   // build.gradle.kts
   implementation("io.lettuce:lettuce-core:6.3.0.RELEASE")
   ```

2. Implement RedisUrlCache methods:
   ```kotlin
   class RedisUrlCache(private val redisClient: RedisClient) : UrlCache {
       // Implement actual Redis operations
   }
   ```

3. Configure Redis connection:
   ```yaml
   # application.yaml
   redis:
     host: ${REDIS_HOST:localhost}
     port: ${REDIS_PORT:6379}
   ```

### Phase 3: Switch to Redis

**Zero code changes in services** - only change initialization:

```kotlin
// Before
val cache = InMemoryUrlCache()

// After
val redisClient = buildRedisClient(redisConfig)
val cache = RedisUrlCache(redisClient)

// PhotoService, UserService, MatchService remain unchanged!
```

## Testing Strategy

### Unit Tests: Mock the Interface

```kotlin
@Test
fun `should use cached URL when available`() {
    val mockCache = mockk<UrlCache>()
    val signer = CloudFrontSignerService(s3Config, mockCache)

    every { mockCache.getOrGenerate(any(), any(), any()) } returns "cached-url"

    val result = signer.generateSignedUrl("test.jpg", 48)

    assertEquals("cached-url", result)
}
```

### Integration Tests: Use Real Implementations

```kotlin
@Test
fun `InMemoryUrlCache should cache URLs correctly`() {
    val cache = InMemoryUrlCache()
    var generationCount = 0

    // First call - cache miss
    val url1 = cache.getOrGenerate("test:48", 48) {
        generationCount++
        "url-$generationCount"
    }

    // Second call - cache hit
    val url2 = cache.getOrGenerate("test:48", 48) {
        generationCount++
        "url-$generationCount"
    }

    assertEquals(1, generationCount)  // Generator called only once
    assertEquals(url1, url2)  // Same URL returned
}
```

## Comparison: Before vs After

### Before (No Interface)

```kotlin
// CloudFrontSignerService.kt
class CloudFrontSignerService(
    private val s3Config: S3Config,
    private val cache: InMemoryUrlCache = InMemoryUrlCache()
) {
    // ...
}

// To switch to Redis:
// ❌ Must change CloudFrontSignerService signature
// ❌ Must update all usages
// ❌ Breaks existing code
```

### After (With Interface)

```kotlin
// CloudFrontSignerService.kt
class CloudFrontSignerService(
    private val s3Config: S3Config,
    private val cache: UrlCache = InMemoryUrlCache()
) {
    // ...
}

// To switch to Redis:
// ✅ Change only initialization code
// ✅ Services remain unchanged
// ✅ No breaking changes
```

## Summary

| Aspect | Benefit |
|--------|---------|
| **Flexibility** | Easy to swap implementations |
| **Testability** | Can mock/stub for unit tests |
| **Maintainability** | Changes isolated to implementations |
| **Scalability** | Can switch to distributed cache without code changes |
| **SOLID Principles** | Follows Dependency Inversion & Open/Closed |

---

**Key Takeaway:** By depending on `UrlCache` interface instead of concrete `InMemoryUrlCache`, we can swap caching strategies (in-memory → Redis → Memcached → custom) **without changing a single line of service code**. 🎯

---

**Status:** ✅ Implemented
**Current Default:** `InMemoryUrlCache`
**Future Options:** `RedisUrlCache`, `MemcachedUrlCache`, custom implementations
**Migration Impact:** **Zero** - change only initialization code
