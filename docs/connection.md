# Connection Lifecycle & Resilience

## Connection States

The SDK transitions through the following connection states:

```
Disconnected
    ↓ (connect)
Connecting(attempt)
    ↓
Authenticating
    ↓
Connected(gatewayVersion, protocolVersion, capabilities, sessionId)
    ↓ (unexpected drop)
Reconnecting(attempt, retryInMs)
```

## Reconnection Policy

Automatic reconnection uses bounded exponential backoff with jitter:

```kotlin
data class ReconnectPolicy(
    val enabled: Boolean = true,
    val initialDelayMs: Long = 1_000,
    val maxDelayMs: Long = 30_000,
    val multiplier: Double = 2.0,
    val jitterRatio: Double = 0.1
)
```

Reconnection will NOT trigger when:
- `disconnect()` or `close()` is explicitly called by the application.
- Authentication token is rejected by gateway (`INVALID_AUTHENTICATION`).
- Protocol version mismatch (`PROTOCOL_VERSION_MISMATCH`).

## Heartbeat & Metrics

The SDK sends `ping` frames every 5 seconds (configurable via `heartbeatIntervalMs`). Latency and connection duration are tracked in:

```kotlin
val metrics: ConnectionMetrics = client.connectionMetrics.value
println("Latency: ${metrics.latencyMs}ms, Connected: ${metrics.connectedDurationMs}ms")
```
