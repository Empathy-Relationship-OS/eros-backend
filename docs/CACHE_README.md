# Cache System - Quick Reference

**Status:** ✅ Production Ready
**Latest Versions:** Valkey 9.0.4, Lettuce 6.7.1.RELEASE

---

## TL;DR

```bash
# Start Valkey
docker-compose up -d valkey

# Run application
./gradlew run

# Check health
curl http://localhost:8940/health/cache
```

---

## Key Features

| Feature | Status | Notes |
|---------|--------|-------|
| **Valkey 9.0.4 Support** | ✅ | Latest stable version |
| **Redis Compatible** | ✅ | Same protocol, can use either |
| **AWS ElastiCache Ready** | ✅ | TLS/SSL support built-in |
| **Fail-Fast Behavior** | ✅ | Prevents silent degradation |
| **Health Checks** | ✅ | `/health/cache` endpoint |
| **In-Memory Fallback** | ✅ | For testing only (explicit) |

---

## Quick Start

### 1. Local Development

```bash
# Ensure .env is configured
CACHE_ENABLED=true
CACHE_BACKEND=valkey
CACHE_HOST=localhost
CACHE_PORT=6379
CACHE_PASSWORD=devpassword

# Start services
docker-compose up -d

# Verify Valkey
docker exec valkey valkey-cli -a devpassword PING
# Expected: PONG
```

### 2. Production (AWS ElastiCache)

```env
CACHE_ENABLED=true
CACHE_BACKEND=valkey
CACHE_HOST=master.eros-cache.xxxxx.cache.amazonaws.com
CACHE_PORT=6379
CACHE_PASSWORD=<stored-in-aws-secrets-manager>
CACHE_TLS_ENABLED=true
```

---

## Architecture

```txt
Application
    ↓ uses
UrlCache (specialized for CloudFront URLs)
    ↓ implemented by
CacheBackedUrlCache (adapter)
    ↓ uses
Cache (generic interface)
    ↓ implemented by
DistributedCache → Lettuce → Valkey/Redis
    OR
InMemoryCache (thread-safe, no network)
```

---

## Fail-Fast Behavior

### What It Means

When you configure Valkey/Redis and it's **not available**, the application **will not start**.

```txt
❌ Application startup failed: Unable to connect to valkey at localhost:6379
```

### Why This Is Good

1. **Catches problems early** - Wrong password? Wrong hostname? You'll know immediately
2. **No silent degradation** - Production won't secretly fall back to in-memory cache
3. **Consistent behavior** - All instances use the same cache backend
4. **Clear feedback** - Failed deployment = cache issue

### How to Handle It

**Development (Don't want to run Valkey):**
```env
CACHE_ENABLED=false
# OR
CACHE_BACKEND=in-memory
```

**Production (Want high availability):**
- Use AWS ElastiCache Multi-AZ
- Configure connection timeout (default: 2000ms)
- Set up CloudWatch alarms
- Monitor `/health/cache` endpoint

---

## Configuration Reference

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `CACHE_ENABLED` | `true` | No | Enable/disable cache |
| `CACHE_BACKEND` | `valkey` | No | `valkey`, `redis`, `in-memory` |
| `CACHE_HOST` | `localhost` | Yes* | Cache server hostname |
| `CACHE_PORT` | `6379` | Yes* | Cache server port |
| `CACHE_PASSWORD` | `""` | Yes** | Auth password/token |
| `CACHE_DATABASE` | `0` | No | Database number (0-15) |
| `CACHE_TLS_ENABLED` | `false` | No | Use TLS (AWS requires true) |
| `CACHE_POOL_MAX_TOTAL` | `50` | No | Max connections |
| `CACHE_TIMEOUT_MS` | `2000` | No | Connection timeout |

\* Required if backend is `valkey` or `redis`
\** Required for AWS ElastiCache, optional for local

---

## Common Scenarios

### Scenario 1: First Time Setup

```bash
# 1. Start Valkey
docker-compose up -d valkey

# 2. Verify it's running
docker ps | grep valkey

# 3. Test connection
docker exec valkey valkey-cli -a devpassword PING

# 4. Run application
./gradlew run

# 5. Check cache works
curl http://localhost:8940/health/cache
```

### Scenario 2: Application Won't Start

**Error:**

```txt
Failed to connect to valkey at localhost:6379
io.lettuce.core.RedisConnectionException
```

**Fix:**
```bash
# Option 1: Start Valkey
docker-compose up -d valkey

# Option 2: Disable cache (development only)
export CACHE_ENABLED=false
./gradlew run
```

### Scenario 3: Testing Without Cache

```env
# In .env file:
CACHE_ENABLED=false
# OR
CACHE_BACKEND=in-memory
```

### Scenario 4: Deploying to Production

1. **Provision ElastiCache:**
   ```hcl
   resource "aws_elasticache_cluster" "eros_cache" {
     engine = "valkey"
     engine_version = "9.0.4"
     node_type = "cache.t4g.micro"
     transit_encryption_enabled = true
     auth_token = var.cache_auth_token
   }
   ```

2. **Configure application:**
   ```env
   CACHE_ENABLED=true
   CACHE_BACKEND=valkey
   CACHE_HOST=master.eros-cache.xxxxx.cache.amazonaws.com
   CACHE_PASSWORD=<from-secrets-manager>
   CACHE_TLS_ENABLED=true
   ```

3. **Deploy and verify:**
   ```bash
   # From EC2/ECS instance:
   curl http://localhost:8940/health/cache
   ```

---

## API Usage

### Using Generic Cache

```kotlin
// Get cache instance
val cache = application.attributes[CacheKey]

// Store with TTL
cache.set("user:123:profile", profileJson, ttlSeconds = 3600)

// Retrieve
val profile = cache.get("user:123:profile")

// Check TTL
val remainingSeconds = cache.ttl("user:123:profile")

// Delete
cache.delete("user:123:profile")

// Delete by pattern
cache.deleteByPattern("user:123:*")

// Health check
val healthy = cache.ping()

// Statistics
val stats = cache.getStats()
println("Backend: ${stats.backend}, Keys: ${stats.keyCount}")
```

### Using with CloudFrontSignerService

```kotlin
// The service already uses UrlCache interface
// To use distributed cache:

val cache = application.attributes[CacheKey]
val urlCache = CacheBackedUrlCache(cache)
val signerService = CloudFrontSignerService(s3Config, urlCache)

// Usage (no changes needed):
val signedUrl = signerService.generateSignedUrl(objectKey, userId, expiryHours = 48)
```

---

## Monitoring

### Health Check Endpoint

```bash
GET /health/cache

# Healthy response:
{
  "status": "healthy",
  "backend": "valkey",
  "connected": true,
  "keyCount": 1234,
  "memoryUsedBytes": 5242880
}

# Unhealthy response:
{
  "status": "unhealthy",
  "backend": "valkey",
  "connected": false
}
```

### Recommended Metrics (CloudWatch/Prometheus)

- `cache_hits_total` - Total cache hits
- `cache_misses_total` - Total cache misses
- `cache_operation_duration_seconds` - Operation latency
- `CPUUtilization` (AWS) - ElastiCache CPU usage
- `NetworkBytesIn/Out` (AWS) - Traffic patterns

---

## Troubleshooting

### Q: Application won't start, says "Connection refused"

**A:** This is expected! Start Valkey first:
```bash
docker-compose up -d valkey
```

Or temporarily disable cache:
```bash
export CACHE_ENABLED=false
./gradlew run
```

### Q: How do I test without running Valkey?

**A:** Use in-memory cache:
```env
CACHE_ENABLED=false
# OR
CACHE_BACKEND=in-memory
```

### Q: Why not silently fall back to in-memory cache?

**A:** Fail-fast prevents production incidents:
- Catches configuration errors immediately
- Ensures all instances use the same backend
- No silent degradation that goes unnoticed

### Q: What's the difference between Valkey and Redis?

**A:**
- **Valkey**: Open-source fork, BSD license, Linux Foundation maintained
- **Redis**: Original, restrictive license (RSAL/SSPL), commercial company
- **Protocol**: 100% compatible - same commands, same API
- **Recommendation**: Use Valkey (fully open-source)

### Q: How much does ElastiCache cost?

**A:**
- **Dev/Staging**: cache.t4g.micro (~$15/month)
- **Small Prod**: cache.t4g.small (~$30/month)
- **Medium Prod**: cache.m6g.large (~$120/month)

### Q: Can I use Redis instead of Valkey?

**A:** Yes! Change backend:
```env
CACHE_BACKEND=redis
```
Everything else stays the same (same protocol).

---

## Files & Locations

### Core Implementation
- `common/src/main/kotlin/com/eros/common/cache/Cache.kt` - Generic cache interface
- `common/src/main/kotlin/com/eros/common/cache/DistributedCache.kt` - Valkey/Redis implementation
- `common/src/main/kotlin/com/eros/common/cache/InMemoryCache.kt` - In-memory implementation
- `common/src/main/kotlin/com/eros/common/cache/CacheConfig.kt` - Configuration
- `common/src/main/kotlin/com/eros/common/cache/CacheClientFactory.kt` - Factory

### Configuration
- `app/src/main/resources/application.yaml` - Cache config section
- `.env.example` - Environment variable templates
- `docker-compose.yml` - Valkey service definition

### Documentation
- `docs/cache-implementation.md` - Comprehensive guide
- `docs/cache-implementation-summary.md` - Executive summary
- `docs/CACHE_README.md` - This file

---

## Version Information

| Component | Version | Notes |
|-----------|---------|-------|
| **Valkey** | 9.0.4-alpine | Latest stable |
| **Lettuce** | 6.7.1.RELEASE | Latest stable |
| **kotlin-logging** | 7.0.0 | Latest stable |
| **Docker Image** | valkey/valkey:9.0.4-alpine | Official image |

---

## Next Steps

1. **Start local development:**
   ```bash
   docker-compose up -d valkey
   ./gradlew run
   ```

2. **Write tests** (see `InMemoryUrlCacheTest` as template):
   - Use Testcontainers with `valkey/valkey:9.0.4-alpine`
   - Test all Cache interface methods
   - Test fail-fast behavior

3. **Deploy to production:**
   - Provision AWS ElastiCache (Terraform example in docs)
   - Configure TLS and auth token
   - Monitor `/health/cache` endpoint

---

## Support

**Documentation:**
- Full guide: `docs/cache-implementation.md`
- Summary: `docs/cache-implementation-summary.md`

**Code Examples:**
- See `InMemoryUrlCacheTest` for testing patterns
- See `CloudFrontSignerService` for usage examples

**AWS Setup:**
- See Terraform example in `cache-implementation.md`
- See AWS CLI commands in documentation

---

**Last Updated:** 2026-05-16
**Status:** ✅ Production Ready
