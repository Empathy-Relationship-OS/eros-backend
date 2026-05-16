# Cache Implementation Guide

**Date:** 2026-05-16
**Status:** ✅ Implemented
**Backend:** Valkey (Redis-compatible)

---

## Overview

The Eros Backend now has a comprehensive caching infrastructure that supports:

- ✅ **Multiple backends**: Valkey, Redis, In-Memory
- ✅ **Local development**: Docker Compose with Valkey
- ✅ **Production**: AWS ElastiCache with TLS/SSL
- ✅ **Graceful degradation**: Automatic fallback to in-memory cache
- ✅ **Zero code changes**: Switch backends via configuration

---

## Architecture

### Abstraction Layers

```
Application Layer
    ↓ uses
UrlCache (specialized interface for CloudFront signed URLs)
    ↓ implemented by
CacheBackedUrlCache (adapter)
    ↓ uses
Cache (generic interface for any data)
    ↓ implemented by
DistributedCache | InMemoryCache
    ↓ uses
Lettuce Client (Valkey/Redis protocol)
```

### Key Components

| Component | Purpose | Location |
|-----------|---------|----------|
| `Cache` | Generic cache interface | `common/cache/Cache.kt` |
| `CacheConfig` | Configuration with TLS support | `common/cache/CacheConfig.kt` |
| `CacheClientFactory` | Creates cache based on config | `common/cache/CacheClientFactory.kt` |
| `DistributedCache` | Valkey/Redis implementation | `common/cache/DistributedCache.kt` |
| `InMemoryCache` | Fallback implementation | `common/cache/InMemoryCache.kt` |
| `CacheBackedUrlCache` | Adapter for UrlCache | `common/cache/CacheBackedUrlCache.kt` |
| `configureCache()` | Ktor plugin | `app/Cache.kt` |

---

## Local Development Setup

### 1. Start Valkey with Docker Compose

```bash
# Start all services (PostgreSQL + Valkey)
docker-compose up -d

# Check Valkey health
docker exec valkey valkey-cli -a devpassword PING
# Expected: PONG

# View Valkey logs
docker logs valkey

# Connect to Valkey CLI
docker exec -it valkey valkey-cli -a devpassword
```

### 2. Configure Environment Variables

Copy `.env.example` to `.env` and configure:

```bash
# Cache settings (defaults work for Docker Compose)
CACHE_ENABLED=true
CACHE_BACKEND=valkey
CACHE_HOST=localhost
CACHE_PORT=6379
CACHE_PASSWORD=devpassword
CACHE_DATABASE=0
CACHE_TLS_ENABLED=false
```

### 3. Run the Application

```bash
./gradlew run
```

Check cache health:
```bash
curl http://localhost:8940/health/cache
```

Expected response:
```json
{
  "status": "healthy",
  "backend": "valkey",
  "connected": true,
  "keyCount": 0,
  "memoryUsedBytes": 1048576
}
```

---

## AWS ElastiCache Production Deployment

### Prerequisites

- AWS account with ElastiCache access
- VPC with private subnets
- Security groups configured
- Terraform or AWS CLI

### 1. Create ElastiCache Cluster

#### Option A: Terraform

```hcl
# terraform/elasticache.tf

resource "aws_elasticache_cluster" "eros_cache" {
  cluster_id           = "eros-cache-${var.environment}"
  engine               = "valkey"  # or "redis"
  engine_version       = "9.0.4"
  node_type            = "cache.t4g.micro"  # $15/month
  num_cache_nodes      = 1
  parameter_group_name = "default.valkey7"
  port                 = 6379

  # Networking
  subnet_group_name    = aws_elasticache_subnet_group.eros.name
  security_group_ids   = [aws_security_group.cache.id]

  # Security
  transit_encryption_enabled = true
  auth_token                 = var.cache_auth_token
  auth_token_update_strategy = "ROTATE"

  # Maintenance
  maintenance_window = "sun:05:00-sun:06:00"
  snapshot_window    = "03:00-04:00"
  snapshot_retention_limit = 5

  tags = {
    Name        = "eros-cache-${var.environment}"
    Environment = var.environment
  }
}

# Subnet group for private subnets
resource "aws_elasticache_subnet_group" "eros" {
  name       = "eros-cache-subnet-group"
  subnet_ids = var.private_subnet_ids
}

# Security group
resource "aws_security_group" "cache" {
  name        = "eros-cache-sg-${var.environment}"
  description = "Security group for ElastiCache"
  vpc_id      = var.vpc_id

  # Allow inbound from application servers only
  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [var.app_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "eros-cache-sg-${var.environment}"
  }
}

# Outputs
output "cache_endpoint" {
  value       = aws_elasticache_cluster.eros_cache.cache_nodes[0].address
  description = "ElastiCache primary endpoint"
}

output "cache_port" {
  value       = aws_elasticache_cluster.eros_cache.port
  description = "ElastiCache port"
}
```

#### Option B: AWS CLI

```bash
# Create subnet group
aws elasticache create-cache-subnet-group \
  --cache-subnet-group-name eros-cache-subnet-group \
  --cache-subnet-group-description "Subnet group for Eros cache" \
  --subnet-ids subnet-xxxxx subnet-yyyyy

# Create ElastiCache cluster
aws elasticache create-cache-cluster \
  --cache-cluster-id eros-cache-prod \
  --engine valkey \
  --engine-version 9.0.4 \
  --cache-node-type cache.t4g.micro \
  --num-cache-nodes 1 \
  --cache-subnet-group-name eros-cache-subnet-group \
  --security-group-ids sg-xxxxx \
  --auth-token "your-secure-auth-token" \
  --transit-encryption-enabled \
  --port 6379

# Get endpoint
aws elasticache describe-cache-clusters \
  --cache-cluster-id eros-cache-prod \
  --show-cache-node-info \
  --query 'CacheClusters[0].CacheNodes[0].Endpoint.Address' \
  --output text
```

### 2. Configure Application for ElastiCache

Update environment variables (via AWS Systems Manager Parameter Store, Secrets Manager, or ECS task definition):

```bash
# Cache settings for AWS ElastiCache
CACHE_ENABLED=true
CACHE_BACKEND=valkey
CACHE_HOST=master.eros-cache.xxxxx.cache.amazonaws.com
CACHE_PORT=6379
CACHE_PASSWORD=<stored-in-secrets-manager>
CACHE_DATABASE=0
CACHE_TLS_ENABLED=true
CACHE_TLS_VERIFY_PEER=true
```

### 3. Store Auth Token in AWS Secrets Manager

```bash
# Store auth token
aws secretsmanager create-secret \
  --name /eros/prod/cache-auth-token \
  --secret-string "your-secure-auth-token"

# Retrieve in application (via IAM role)
aws secretsmanager get-secret-value \
  --secret-id /eros/prod/cache-auth-token \
  --query SecretString \
  --output text
```

### 4. Verify Connection

From an EC2 instance or ECS task in the same VPC:

```bash
# Install redis-cli
sudo yum install -y redis6

# Test connection
redis-cli -h master.eros-cache.xxxxx.cache.amazonaws.com \
  -p 6379 \
  --tls \
  -a <auth-token> \
  PING
# Expected: PONG
```

---

## Configuration Reference

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `CACHE_ENABLED` | `true` | Enable/disable cache |
| `CACHE_BACKEND` | `valkey` | Backend: `valkey`, `redis`, `in-memory` |
| `CACHE_HOST` | `localhost` | Cache server hostname |
| `CACHE_PORT` | `6379` | Cache server port |
| `CACHE_PASSWORD` | `""` | Auth token (required for AWS) |
| `CACHE_DATABASE` | `0` | Database number (0-15) |
| `CACHE_TLS_ENABLED` | `false` | Enable TLS/SSL |
| `CACHE_TLS_VERIFY_PEER` | `true` | Verify server certificate |
| `CACHE_POOL_MAX_TOTAL` | `50` | Max connections |
| `CACHE_POOL_MAX_IDLE` | `10` | Max idle connections |
| `CACHE_POOL_MIN_IDLE` | `5` | Min idle connections |
| `CACHE_TIMEOUT_MS` | `2000` | Connection timeout (ms) |

### Backend Comparison

| Feature | Valkey | Redis | In-Memory |
|---------|--------|-------|-----------|
| **License** | BSD 3-Clause | RSAL/SSPL | N/A |
| **Protocol** | Redis-compatible | Native | N/A |
| **Distributed** | Yes | Yes | No |
| **AWS Support** | Yes (ElastiCache) | Yes (ElastiCache) | N/A |
| **Recommended** | ✅ Yes | For legacy only | Fallback only |

---

## Usage Examples

### Using the Generic Cache Interface

```kotlin
// Get cache from application
val cache = application.attributes[CacheKey]

// Store with TTL
cache.set("user:123:profile", profileJson, ttlSeconds = 3600)

// Retrieve
val profile = cache.get("user:123:profile")

// Check TTL
val remaining = cache.ttl("user:123:profile")

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

The CloudFrontSignerService already uses the UrlCache interface, which can now be backed by distributed cache:

```kotlin
// In route initialization
val cache = application.attributes[CacheKey]
val urlCache = CacheBackedUrlCache(cache)
val signerService = CloudFrontSignerService(s3Config, urlCache)

// Usage (no changes required)
val signedUrl = signerService.generateSignedUrl(objectKey, userId, expiryHours = 48)
```

---

## Monitoring & Observability

### Health Check Endpoint

```bash
GET /health/cache
```

Response (healthy):
```json
{
  "status": "healthy",
  "backend": "valkey",
  "connected": true,
  "keyCount": 1234,
  "memoryUsedBytes": 5242880
}
```

Response (unhealthy):
```json
{
  "status": "unhealthy",
  "backend": "valkey",
  "connected": false
}
```

### Recommended Metrics (Prometheus)

```kotlin
// Add to your metrics module
private val cacheHitCounter = Counter.build()
    .name("cache_hits_total")
    .help("Total cache hits")
    .register()

private val cacheMissCounter = Counter.build()
    .name("cache_misses_total")
    .help("Total cache misses")
    .register()

private val cacheOperationDuration = Histogram.build()
    .name("cache_operation_duration_seconds")
    .labelNames("operation")  // "get", "set", "delete"
    .help("Cache operation duration")
    .register()
```

### CloudWatch Monitoring (AWS)

Key metrics to monitor:
- `CPUUtilization` - Target: < 75%
- `NetworkBytesIn/Out` - Monitor traffic patterns
- `CurrConnections` - Current connections
- `Evictions` - Memory pressure indicator
- `CacheHits` / `CacheMisses` - Hit rate

Set up alarms:
```bash
# High CPU alarm
aws cloudwatch put-metric-alarm \
  --alarm-name eros-cache-high-cpu \
  --alarm-description "Cache CPU > 75%" \
  --metric-name CPUUtilization \
  --namespace AWS/ElastiCache \
  --statistic Average \
  --period 300 \
  --evaluation-periods 2 \
  --threshold 75.0 \
  --comparison-operator GreaterThanThreshold \
  --dimensions Name=CacheClusterId,Value=eros-cache-prod
```

---

## Cost Analysis

### AWS ElastiCache Pricing (eu-west-2)

| Instance Type | vCPUs | Memory | Network | Price/Month | Use Case |
|---------------|-------|--------|---------|-------------|----------|
| cache.t4g.micro | 2 | 512 MB | Low | ~$15 | Dev/Staging |
| cache.t4g.small | 2 | 1.5 GB | Low to Moderate | ~$30 | Small Production |
| cache.t4g.medium | 2 | 3 GB | Low to Moderate | ~$60 | Medium Production |
| cache.m6g.large | 2 | 6.4 GB | Up to 10 Gbps | ~$120 | High Traffic |

### Memory Usage Estimates

```
Per-user cached profile: ~50 KB
10K active users: 500 MB → cache.t4g.small
100K active users: 5 GB → cache.m6g.large
```

### Cost Optimization Tips

1. **Start small**: Begin with `cache.t4g.micro` and monitor
2. **TTL configuration**: Use appropriate TTLs to control memory
3. **Monitoring**: Set up CloudWatch alarms for evictions
4. **Reserved instances**: Save up to 55% with 1-3 year commitment
5. **Cluster mode**: Only if you need >100GB memory

---

## Troubleshooting

### Issue: Connection Refused

**Symptoms:**
```
Failed to connect to valkey at localhost:6379, falling back to in-memory cache
```

**Solutions:**
1. Check Valkey is running: `docker ps | grep valkey`
2. Check port mapping: `docker port valkey`
3. Verify password: `docker exec valkey valkey-cli -a devpassword PING`

### Issue: Authentication Failed

**Symptoms:**
```
NOAUTH Authentication required
```

**Solutions:**
1. Ensure `CACHE_PASSWORD` is set correctly
2. For Docker: Password is `devpassword` (from `.env.example`)
3. For AWS: Verify auth token in Secrets Manager

### Issue: TLS Connection Failed

**Symptoms:**
```
Connection error: SSL handshake failed
```

**Solutions:**
1. Verify TLS is enabled in ElastiCache cluster
2. Check `CACHE_TLS_ENABLED=true` in environment
3. Verify security group allows port 6379 from application

### Issue: Cache Not Being Used

**Check:**
1. Verify cache is enabled: `curl http://localhost:8940/health/cache`
2. Check logs for "Cache initialized successfully"
3. Monitor cache hit/miss rates
4. Verify UrlCache is using CacheBackedUrlCache adapter

---

## Testing

### Unit Tests

```kotlin
class InMemoryCacheTest {
    private lateinit var cache: Cache

    @BeforeEach
    fun setup() {
        cache = InMemoryCache()
    }

    @Test
    fun `should cache and retrieve value`() = runTest {
        cache.set("key", "value", ttlSeconds = 3600)
        val result = cache.get("key")
        assertEquals("value", result)
    }
}
```

### Integration Tests with Testcontainers

```kotlin
class DistributedCacheTest {
    companion object {
        @Container
        val valkeyContainer = GenericContainer<Nothing>("valkey/valkey:9.0.4-alpine")
            .apply {
                withExposedPorts(6379)
                withCommand("valkey-server", "--requirepass", "testpass")
            }
    }

    @Test
    fun `should connect to Valkey and cache value`() = runTest {
        val config = CacheConfig(
            enabled = true,
            backend = CacheBackend.VALKEY,
            host = valkeyContainer.host,
            port = valkeyContainer.firstMappedPort,
            password = "testpass",
            // ... other config
        )

        val cache = CacheClientFactory.create(config)
        cache.set("test-key", "test-value", 60)

        val result = cache.get("test-key")
        assertEquals("test-value", result)
    }
}
```

---

## Migration Guide

### From InMemoryUrlCache to Distributed Cache

**Before:**
```kotlin
val urlCache = InMemoryUrlCache()
val signerService = CloudFrontSignerService(s3Config, urlCache)
```

**After:**
```kotlin
val cache = application.attributes[CacheKey]
val urlCache = CacheBackedUrlCache(cache)
val signerService = CloudFrontSignerService(s3Config, urlCache)
```

**Benefits:**
- Cache shared across application instances
- Persists across application restarts
- Configurable backend (Valkey/Redis)
- Graceful fallback to in-memory

---

## Future Enhancements

### Planned Features

1. **Match Batch Caching** (from redis-cache-optimization-proposal.md)
   - Cache daily batch responses
   - Invalidation on user actions
   - Target: 94% faster response times

2. **Additional Cache Implementations**
   - Memcached backend
   - Hazelcast for multi-region
   - DynamoDB as cache backend

3. **Advanced Features**
   - Cache warming on startup
   - Automatic eviction policies
   - Cache versioning for schema changes
   - Multi-tier caching (L1: in-memory, L2: distributed)

4. **Metrics & Monitoring**
   - Prometheus metrics integration
   - Grafana dashboard templates
   - Cache hit/miss rate tracking

---

## References

- [Valkey Official Docs](https://valkey.io/)
- [AWS ElastiCache for Valkey](https://aws.amazon.com/elasticache/)
- [Lettuce Documentation](https://lettuce.io/)
- [Redis Protocol Specification](https://redis.io/docs/reference/protocol-spec/)
- [Redis Cache Optimization Proposal](redis-cache-optimization-proposal.md)

---

**Document Version:** 1.0
**Last Updated:** 2026-05-16
**Status:** ✅ Implemented and Production-Ready
