# CloudFront Signed URL Caching Strategy

## Problem Statement

When users refresh their daily batch, the backend was regenerating CloudFront signed URLs on every request:

```text
User refreshes daily batch 10 times in 5 minutes:
→ 10 calls to CloudFrontSigner.generateSignedUrl()
→ 10 RSA private key signing operations (CPU intensive)
→ All 10 URLs are functionally identical (same expiry time)
→ Wasteful and poor performance
```

## Solution: In-Memory Caching

We implemented an in-memory cache that stores signed URLs with TTL matching their CloudFront expiration time.

### Architecture

```text
┌─────────────────────┐
│  MatchService       │
│  fetchDailyBatch()  │
└──────────┬──────────┘
           │
           ↓
┌─────────────────────┐
│  UserService        │
│  getUserMatchData() │
└──────────┬──────────┘
           │
           ↓
┌─────────────────────┐
│  PhotoService       │
│  generateAccessUrl()│
└──────────┬──────────┘
           │
           ↓
┌─────────────────────────────┐
│  CloudFrontSignerService    │
│  generateSignedUrl()        │
└──────────┬──────────────────┘
           │
           ↓
┌─────────────────────────────┐
│  InMemoryUrlCache           │
│  getOrGenerate()            │
│                             │
│  Cache Key:                 │
│  "photos/user/123.jpg:48"   │
│                             │
│  If cached & not expired:   │
│    → Return cached URL      │
│  Else:                      │
│    → Generate new URL       │
│    → Store in cache         │
│    → Return new URL         │
└─────────────────────────────┘
```

## Implementation Details

### Cache Key Structure

```kotlin
cacheKey = "$objectKey:$expiryHours"

// Examples:
"photos/user123/abc123.jpg:48"  // Daily batch (48h expiry)
"photos/user456/def456.jpg:24"  // Reconsideration (24h expiry)
"photos/user789/ghi789.jpg:720" // Mutual match (30 days = 720h)
```

### Cache Entry

```kotlin
data class CacheEntry(
    val signedUrl: String,           // The CloudFront signed URL
    val expiresAt: Instant           // When the URL expires
)
```

### Cache Behavior

1. **First Request** (cache miss):
   ```text
   User A requests daily batch
   → Cache miss for "photos/userB/photo1.jpg:48"
   → Generate CloudFront signed URL (RSA signing operation)
   → Store in cache with 48h TTL
   → Return signed URL to user
   ```

2. **Subsequent Requests** (cache hit):
   ```text
   User A refreshes daily batch
   → Cache hit for "photos/userB/photo1.jpg:48"
   → URL still valid (expires in 47h)
   → Return cached URL immediately
   → No RSA signing operation needed
   ```

3. **After Expiry** (cache miss):
   ```text
   User A requests batch after 48 hours
   → Cache entry expired and removed
   → Generate new CloudFront signed URL
   → Store in cache with fresh 48h TTL
   → Return new signed URL
   ```

## Performance Impact

### Before Caching

```text
User refreshes daily batch 10 times:
- 10 RSA private key operations
- ~10-20ms per signature generation
- Total: ~100-200ms CPU time
- CloudFront URL generated 10 times (all identical)
```

### After Caching

```text
User refreshes daily batch 10 times:
- 1 RSA private key operation (first request)
- 9 cache lookups (subsequent requests)
- ~10-20ms for first request
- ~0.1ms per cache lookup
- Total: ~11-21ms CPU time
- 90% reduction in signature operations
```

### Metrics

| Scenario | Before Caching | After Caching | Improvement |
|----------|---------------|---------------|-------------|
| **1 user, 10 refreshes** | 10 signatures | 1 signature | 90% reduction |
| **100 users, 3 refreshes each** | 300 signatures | 100 signatures | 67% reduction |
| **1000 active users** | 3000 signatures | 1000 signatures | 67% reduction |

## Cache Invalidation

### When Photos Change

The cache is automatically invalidated when:

1. **User uploads new photo** (`PhotoService.confirmUpload()`):
   ```kotlin
   // After uploading new photo
   cloudFrontSigner.invalidateCache(objectKey)
   ```

2. **User deletes photo** (`PhotoService.deletePhoto()`):
   ```kotlin
   // After deleting photo
   cloudFrontSigner.invalidateCache(objectKey)
   cloudFrontSigner.invalidateCache(thumbnailObjectKey)
   ```

3. **User replaces photo** (`PhotoService.confirmUpload()` when replacing):
   ```kotlin
   // After replacing old photo
   cloudFrontSigner.invalidateCache(oldPhotoObjectKey)
   cloudFrontSigner.invalidateCache(newPhotoObjectKey)
   ```

### Manual Invalidation (if needed)

```kotlin
// Invalidate specific photo
cloudFrontSigner.invalidateCache("photos/user123/abc.jpg")

// Invalidate all photos for a user
cloudFrontSigner.invalidateUserCache("user123")

// Clear entire cache (e.g., for testing)
cloudFrontSigner.urlCache.clear()
```

## Memory Considerations

### Memory Usage Estimation

```text
Average CloudFront signed URL length: ~300 characters
Cache entry overhead (Instant + key): ~100 bytes
Total per entry: ~400 bytes

Example scenarios:
- 1,000 active users × 6 photos each = 6,000 entries = ~2.4 MB
- 10,000 active users × 6 photos each = 60,000 entries = ~24 MB
- 100,000 active users × 6 photos each = 600,000 entries = ~240 MB
```

### Automatic Cleanup

The cache automatically removes expired entries:

```kotlin
// Called on every cache access
private fun cleanExpiredEntries() {
    val now = Instant.now()
    cache.entries.removeIf { (_, entry) ->
        entry.expiresAt.isBefore(now)
    }
}
```

**Worst-case memory:** Bounded by number of active users and photos per user.

**Typical memory:** Much lower due to automatic expiry cleanup.

## Monitoring

### Cache Statistics

```kotlin
// Get cache stats
val stats = cloudFrontSigner.getCacheStats()

println("Cache size: ${stats.size}")
println("Cached keys: ${stats.entries}")

// Example output:
// Cache size: 1523
// Cached keys: [
//   "photos/user123/abc.jpg:48",
//   "photos/user456/def.jpg:24",
//   ...
// ]
```

### Recommended Monitoring

Add metrics to track:

1. **Cache hit rate**:
   ```kotlin
   // Increment on cache hit
   metrics.increment("cloudfront.cache.hit")

   // Increment on cache miss
   metrics.increment("cloudfront.cache.miss")

   // Calculate hit rate
   hitRate = hits / (hits + misses)
   ```

2. **Cache size**:
   ```kotlin
   metrics.gauge("cloudfront.cache.size", cache.size)
   ```

3. **Signature generation time**:
   ```kotlin
   metrics.timer("cloudfront.signature.generation") {
       CloudFrontUrlSigner.getSignedURLWithCannedPolicy()
   }
   ```

### Expected Metrics

For a healthy system with 10,000 active users:

- **Cache hit rate**: 60-80% (users refresh batches multiple times)
- **Cache size**: 30,000-60,000 entries (~12-24 MB memory)
- **Signature generation time**: 10-20ms per operation

## Edge Cases

### 1. Server Restart

**Behavior:** Cache is lost (in-memory only).

**Impact:** First request after restart generates URLs (slight performance hit).

**Mitigation:** Not needed - URLs regenerate automatically, no user-facing impact.

---

### 2. Clock Skew

**Behavior:** If server clock is incorrect, URLs may expire prematurely or stay cached too long.

**Mitigation:** Cache includes 5-minute buffer before expiry:

```kotlin
// Return cached URL if still valid (with 5-minute buffer)
if (existing.expiresAt.isAfter(Instant.now().plusSeconds(300))) {
    return existing.signedUrl
}
```

---

### 3. Photo Updated But Not Served Yet

**Scenario:** User uploads new photo but hasn't been matched yet.

**Behavior:** Old URL cached, but invalidated immediately on upload.

**Impact:** No issue - cache invalidation ensures fresh URLs.

---

### 4. Concurrent Requests

**Behavior:** Multiple threads may try to generate URLs for same photo simultaneously.

**Mitigation:** `ConcurrentHashMap` ensures thread safety. Worst case: multiple identical URLs generated, but last one wins.

---

## Alternative Approaches Considered

### ❌ Store URLs in Database

**Pros:** Persists across restarts.

**Cons:**
- Database schema changes
- Slower (DB query vs memory lookup)
- More complex (need to manage expiry, regeneration)
- Storage overhead

**Verdict:** Overkill for this use case.

---

### ❌ Client-Side Caching Only

**Pros:** Zero server changes.

**Cons:**
- Can't enforce (relies on client behavior)
- Doesn't solve server-side inefficiency
- Mobile apps might still refresh aggressively

**Verdict:** Doesn't solve the root problem.

---

### ✅ In-Memory Caching (Chosen)

**Pros:**
- Simple implementation
- Fast (memory lookup)
- Automatic expiry
- No database changes
- Thread-safe

**Cons:**
- Lost on server restart (acceptable)
- Memory usage grows with users (bounded and manageable)

**Verdict:** Best balance of simplicity and performance.

---

## Testing

### Unit Tests

```kotlin
@Test
fun `should return cached URL on subsequent requests`() {
    val cache = InMemoryUrlCache()
    val key = "photos/user123/test.jpg:48"

    var generationCount = 0
    val generator = {
        generationCount++
        "https://cloudfront.net/signed-url-$generationCount"
    }

    // First request - cache miss
    val url1 = cache.getOrGenerate(key, 48, generator)
    assertEquals(1, generationCount)

    // Second request - cache hit
    val url2 = cache.getOrGenerate(key, 48, generator)
    assertEquals(1, generationCount) // Generator not called again
    assertEquals(url1, url2) // Same URL returned
}

@Test
fun `should regenerate URL after expiry`() {
    val cache = InMemoryUrlCache()
    val key = "photos/user123/test.jpg:0" // 0-hour expiry (expires immediately)

    var generationCount = 0
    val generator = {
        generationCount++
        "https://cloudfront.net/signed-url-$generationCount"
    }

    // First request
    cache.getOrGenerate(key, 0, generator)
    assertEquals(1, generationCount)

    // Wait for expiry (simulate)
    Thread.sleep(1000)

    // Second request after expiry - cache miss
    cache.getOrGenerate(key, 0, generator)
    assertEquals(2, generationCount) // Generator called again
}
```

### Integration Tests

```kotlin
@Test
fun `daily batch should return cached URLs on refresh`() = runTest {
    // Fetch daily batch
    val batch1 = matchService.fetchDailyBatch(userId)
    val firstUrl = batch1.profiles.first().thumbnailUrl

    // Refresh (fetch again)
    val batch2 = matchService.fetchDailyBatch(userId)
    val secondUrl = batch2.profiles.first().thumbnailUrl

    // URLs should be identical (cached)
    assertEquals(firstUrl, secondUrl)
}
```

## Summary

### Key Benefits

✅ **90% reduction** in RSA signature operations for repeat requests
✅ **67% reduction** in CPU time for typical usage patterns
✅ **Automatic cleanup** - expired entries removed automatically
✅ **Thread-safe** - ConcurrentHashMap ensures correctness
✅ **Cache invalidation** - automatic on photo upload/delete/update
✅ **Low memory footprint** - ~24 MB for 10,000 active users
✅ **Simple implementation** - no database changes needed

### Trade-offs

⚠️ Cache lost on server restart (acceptable - URLs regenerate automatically)
⚠️ Memory usage grows with active users (bounded and manageable)

---

**Status:** ✅ Implemented and tested
**Last Updated:** 2025-05-09
**Performance Impact:** 90% reduction in signature generation
**Memory Impact:** ~24 MB for 10,000 active users
