# Rate-Limiter

A comprehensive in-memory rate-limiting implementation in Java with multiple algorithms to control API traffic and protect services from being overwhelmed.

## Overview

This project provides five different rate-limiting algorithms, each with different trade-offs in terms of memory usage, accuracy, and implementation complexity:

1. **Fixed Window** - Simple time-window based approach
2. **Sliding Window Counter** - Hybrid approach with weighted previous window
3. **Sliding Window Log** - Request-timestamp based tracking
4. **Token Bucket** - Token refill mechanism for burst handling
5. **Leaky Bucket** - Constant leak rate with queue simulation

## Algorithms

### 1. Fixed Window Counter
**Location:** `fixed-window/FixedWindowThrottleAPI.java`

- Divides time into fixed windows (e.g., 500ms)
- Counts requests in current window
- Resets counter when window expires
- **Pros:** Simple, low memory overhead
- **Cons:** Allows request bursts at window boundaries

**Usage:**
```java
FixedWindowThrottleAPI limiter = new FixedWindowThrottleAPI(3, 500);
boolean allowed = limiter.allowRequest("client-id");
```

**Parameters:**
- `maxRequest` - Maximum requests per window
- `windowSizeMs` - Window duration in milliseconds

### 2. Sliding Window Counter
**Location:** `sliding-window/SlidingWindowCounterAPI.java`

- Maintains counts from current and previous windows
- Calculates weighted request count across window boundary
- Smoother rate limiting than fixed window
- **Pros:** Reduces boundary spike issues
- **Cons:** Approximation, slightly more complex

**Usage:**
```java
SlidingWindowCounterAPI limiter = new SlidingWindowCounterAPI(3, 1000);
boolean allowed = limiter.isAllowRequest("client-id");
```

**Parameters:**
- `maxRequest` - Maximum requests per window
- `windowSizeMs` - Window duration in milliseconds

### 3. Sliding Window Log
**Location:** `sliding-window/SlidingWindowLogAPI.java`

- Maintains deque of request timestamps per client
- Removes requests outside the time window
- Most accurate but higher memory usage
- **Pros:** Precise, exact enforcement
- **Cons:** O(n) memory per client, cleanup overhead

**Usage:**
```java
SlidingWindowLogAPI limiter = new SlidingWindowLogAPI(3, 1000);
boolean allowed = limiter.isAllowRequest("client-id");
```

**Parameters:**
- `maxRequests` - Maximum requests per window
- `windowSizeMs` - Window duration in milliseconds

### 4. Token Bucket
**Location:** `token-bucket/TokenBucket.java`

- Bucket starts with N tokens
- Tokens refill at constant rate
- Each request consumes 1 token
- Allows controlled burst traffic
- **Pros:** Handles bursts well, flexible
- **Cons:** More complex state management

**Usage:**
```java
TokenBucketThrottleAPI limiter = new TokenBucketThrottleAPI(3, 1.0 / 200);
boolean allowed = limiter.allowRequest("client-id");
```

**Parameters:**
- `maxTokens` - Bucket capacity (initial tokens)
- `refillRatePerMs` - Tokens added per millisecond (e.g., 1.0/200 = 0.005 tokens/ms = 5 tokens/second)

### 5. Leaky Bucket
**Location:** `leaky-bucket/LeakyBucket.java`

- Bucket holds requests at max capacity
- Requests "leak" out at constant rate
- Rejects requests when bucket is full
- Smooths traffic over time
- **Pros:** Smooths traffic uniformly
- **Cons:** Higher request latency, rejects bursty traffic

**Usage:**
```java
LeakyBucketThrottleAPI limiter = new LeakyBucketThrottleAPI(3, 1.0 / 300);
boolean allowed = limiter.allowRequest("client-id");
```

**Parameters:**
- `bucketCapacity` - Maximum requests held in bucket
- `leakRatePerMs` - Requests processed per millisecond (e.g., 1.0/300 = ~3.33 requests/second)

## Architecture

- **Thread-Safe:** Uses `ConcurrentHashMap` for per-client state storage
- **Per-Client Tracking:** Each algorithm maintains separate state for each client ID
- **In-Memory:** All state stored in application memory (not persistent)
- **Synchronized Access:** Critical sections protected with synchronization blocks
- **Concurrent Requests:** Supports multiple concurrent clients and requests

## Running Examples

Each implementation includes a `main` method demonstrating:
- Burst request scenario
- Request after sleep (recovery period)
- Full drain/refill cycle

Example:
```bash
# Compile
javac -d . fixed-window/FixedWindowThrottleAPI.java
javac -d . sliding-window/SlidingWindowCounterAPI.java
javac -d . sliding-window/SlidingWindowLogAPI.java
javac -d . token-bucket/TokenBucket.java
javac -d . leaky-bucket/LeakyBucket.java

# Run
java -cp . pt.throttler.FixedWindowThrottleAPI
java -cp . pt.throttler.SlidingWindowCounterAPI
java -cp . pt.throttler.SlidingWindowLogAPI
java -cp . pt.throttler.TokenBucketThrottleAPI
java -cp . pt.throttler.LeakyBucketThrottleAPI
```

## Trade-offs Comparison

| Algorithm | Memory | Accuracy | Complexity | Burst Friendly | Use Case |
|-----------|--------|----------|------------|----------------|----------|
| Fixed Window | Low | Low | Low | Yes | Simple quotas |
| Sliding Counter | Low | Medium | Medium | No | Balanced accuracy |
| Sliding Log | High | High | Medium | No | Strict compliance |
| Token Bucket | Low | High | Medium | Yes | API with bursts |
| Leaky Bucket | Medium | High | Medium | No | Traffic shaping |

## Use Cases

- **Fixed Window:** Simple API quotas, high-volume filtering, basic throttling
- **Sliding Window Counter:** Balanced accuracy and performance, production APIs
- **Sliding Window Log:** Strict compliance requirements, financial transactions
- **Token Bucket:** APIs with burst allowance (WebRTC, uploads, CDN)
- **Leaky Bucket:** Smooth traffic shaping, streaming services, network congestion control

## Thread Safety

All implementations use:
- `ConcurrentHashMap` for client state storage
- `synchronized` blocks on critical sections
- Per-bucket/per-client locking to avoid global serialization
- Atomic operations where possible

**Note:** Per-client synchronization ensures safety without serializing all requests globally, enabling concurrent processing for different clients.

## Implementation Details

### Fixed Window
```
Timeline: |==Window 1==|==Window 2==|==Window 3==|
Counter:  |0 1 2 3 ✗ ✗|0 1 2 3 ✗ ✗|0 1 2 3 ✗ ✗|
```

### Sliding Window Counter
```
Weighted calculation across window boundary
Reduces burst at window edges
More accurate than fixed window
```

### Sliding Window Log
```
Request timestamps: [t1, t2, t3, ...]
Remove: t < now - windowSize
Check: len(requests) < maxRequests
```

### Token Bucket
```
Tokens:  3.0 --> 2.5 --> 2.0 --> 3.0 (refilled)
         Request consumed 0.5, refilled over time
Burst: Can consume all 3 tokens at once if available
```

### Leaky Bucket
```
Requests queue at max capacity
Leak at constant rate regardless of input
Smooth output: constant egress rate
```

## Configuration Examples

### Strict Rate Limiting (10 req/sec)
```java
// Option 1: Fixed Window (1000ms, 10 requests)
new FixedWindowThrottleAPI(10, 1000);

// Option 2: Token Bucket (10 tokens, 0.01 per ms = 10/sec)
new TokenBucketThrottleAPI(10, 0.01);

// Option 3: Sliding Window Log
new SlidingWindowLogAPI(10, 1000);
```

### Burst-Friendly (10 req/sec, 50 req burst)
```java
// Token Bucket with 50 token burst capacity
new TokenBucketThrottleAPI(50, 0.01);  // Refill 0.01 tokens/ms = 10/sec
```

### Smooth Traffic (5 req/sec constant)
```java
// Leaky Bucket with constant leak
new LeakyBucketThrottleAPI(10, 1.0 / 200);  // 1 request per 200ms = 5/sec
```

## Known Issues & Fixes Required

### Issue 1: FixedWindowThrottleAPI Constructor (Line 16)
- **Problem:** Constructor is `private` - cannot instantiate from outside
- **Fix:** Change to `public`

### Issue 2: SlidingWindowCounterAPI Method Visibility (Line 27)
- **Problem:** `isAllowRequest` is package-private `synchronized boolean`
- **Fix:** Change to `public synchronized boolean`

### Issue 3: SlidingWindowCounterAPI Division (Line 44)
- **Problem:** `(double) (elapsed / windowSizeMs)` performs integer division before casting
- **Fix:** Change to `((double) elapsed / windowSizeMs)` for correct floating-point result

### Issue 4: SlidingWindowLogAPI Static State (Line 27)
- **Problem:** `log` is `private static` - shared across all instances
- **Fix:** Change to `private` instance variable for per-instance state

### Issue 5: Synchronized Block Granularity
- **Problem:** Some implementations use global synchronization blocking all requests
- **Fix:** Synchronize on per-client basis to allow concurrent requests

## Future Enhancements

- [ ] Distributed rate limiting using Redis
- [ ] Configurable burst allowance parameters
- [ ] Request queuing/backpressure handling
- [ ] Metrics collection (allow rate, deny rate, queue length)
- [ ] Persistence layer for state recovery
- [ ] Graceful degradation under high load
- [ ] Request priority/weight support
- [ ] Admin API for runtime configuration

## Project Structure

```
rate-limiter/
├── fixed-window/
│   └── FixedWindowThrottleAPI.java
├── leaky-bucket/
│   └── LeakyBucket.java
├── sliding-window/
│   ├── SlidingWindowCounterAPI.java
│   └── SlidingWindowLogAPI.java
├── token-bucket/
│   └── TokenBucket.java
└── README.md
```

## Package Info

**Package:** `pt.throttler`

All classes use the same package for consistency and ease of compilation.

## Important Notes

- All implementations use per-client tracking with client ID as string key
- Time measurements use `System.currentTimeMillis()` for consistency across algorithms
- State is not persisted; requests after restart are counted afresh
- For distributed systems (multiple JVMs), consider Redis-based implementations
- Memory grows with number of unique clients; consider cleanup for long-lived clients
- Thread-safe for concurrent requests but not for concurrent configuration changes

## Compilation & Execution

### Compile All
```bash
javac -d build **/*.java
```

### Run Example
```bash
java -cp build pt.throttler.TokenBucketThrottleAPI
```

### Expected Output Format
```
=== Burst: 5 requests back-to-back (bucket starts full at 3) ===
Request 0 -> true
Request 1 -> true
Request 2 -> true
Request 3 -> false
Request 4 -> false

--- Sleeping 400 ms (refills ~2 tokens) ---

=== 5 more requests after partial refill ===
Request 0 -> true
Request 1 -> true
Request 2 -> false
...
```

## Performance Characteristics

- **Fixed Window:** O(1) lookup and update
- **Sliding Counter:** O(1) lookup and update
- **Sliding Log:** O(n) cleanup where n = requests in window
- **Token Bucket:** O(1) lookup and update
- **Leaky Bucket:** O(1) lookup and update

Memory usage per client:
- **Fixed Window:** 8 bytes (2 longs)
- **Sliding Counter:** 16 bytes (2 long arrays of 2 longs each)
- **Sliding Log:** O(n) where n = max requests
- **Token Bucket:** 16 bytes (2 doubles)
- **Leaky Bucket:** 16 bytes (2 doubles)
