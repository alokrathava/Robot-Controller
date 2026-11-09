# Robot SDK API Guide

Package:

```text
com.alokrathava.sdk
```

Current Maven metadata:

```text
com.alokrathava:robot-sdk:0.2.0
```

The primary public interface is `RobotClient`. These are source-level APIs, not proof that every operation is validated against a running gateway. The Maven coordinates currently support local publication; public Maven Central availability is not established.

---

## 1. Create a Client

```kotlin
val client = RobotSdk.create(
    RobotSdkConfig(
        endpoint = RobotEndpoint(
            host = "192.168.1.50",
            port = 8080
        ),
        authentication = RobotAuthentication.Token(
            token = "robot-access-token"
        )
    )
)
```

### RobotSdkConfig

| Property | Purpose | Default |
|---|---|---|
| `endpoint` | Robot gateway host/port/TLS | required |
| `authentication` | Handshake authentication | null |
| `commandTimeoutMs` | Default command/handshake timeout | 15,000 ms |
| `commandTimeouts` | Specific map query/switch/save/delete/mapping budgets | 15/60/120/30/60 seconds |
| `reconnectPolicy` | Automatic reconnect behavior | enabled policy |
| `heartbeatIntervalMs` | Protocol ping cadence | 5,000 ms |
| `telemetryStaleTimeoutMs` | When telemetry is considered stale | 3,000 ms |
| `sslSocketFactory` | Optional custom TLS socket factory | null |
| `trustManager` | Optional custom trust manager | null |
| `logger` | SDK log sink | no-op |

---

## 2. Public State Streams

| Property | Type | Meaning |
|---|---|---|
| `connectionState` | StateFlow<ConnectionState> | Connection state machine |
| `connectionMetrics` | StateFlow<ConnectionMetrics> | Latency, uptime, last-message age, reconnect count |
| `telemetryFreshness` | StateFlow<TelemetryFreshness> | Stale-data detection |
| `telemetry` | StateFlow<RobotTelemetry?> | Pose, velocity, navigation/localization/safety summary |
| `batteryState` | StateFlow<RobotBatteryState?> | Battery/charging data |
| `safetyState` | StateFlow<SafetyState?> | Current safety state |
| `dockingState` | StateFlow<DockingState?> | Docking state machine |
| `dockingDetails` | StateFlow<DockingStateDetails?> | Detailed docking state |
| `health` | StateFlow<RobotHealth?> | Overall/subsystem health |
| `activeMap` | StateFlow<RobotMap?> | Active map |
| `occupancyGrid` | StateFlow<RobotOccupancyGrid?> | Latest map snapshot / applied updates, if available |
| `mapOperationState` | StateFlow<MapOperationState> | Map operation progress |
| `navigation` | StateFlow<NavigationState> | Active navigation summary |
| `missionProgress` | StateFlow<MissionProgress?> | Mission execution |
| `configuration` | StateFlow<RobotConfiguration?> | Runtime configuration |
| `robotInfo` | StateFlow<RobotInfo?> | Gateway/capability identity |
| `diagnostics` | StateFlow<RobotDiagnostics> | Aggregated client diagnostics |
| `events` | SharedFlow<RobotEvent> | Transient error/connection events |

---

## 3. Connection Lifecycle

### Connect

```kotlin
val result: RobotResult<Unit> = client.connect()
```

Connection performs:
1. WebSocket open
2. `hello` handshake
3. authentication token submission, when configured
4. protocol-version exchange
5. capability negotiation
6. session establishment
7. asynchronous initial state snapshot and map subscription request

**Current limitations:** `hello_ack` is not strictly checked against the request ID or expected protocol version in `RobotClientImpl`. An empty advertised capability set currently bypasses the local unsupported-capability check. These require hardening; do not rely on the handshake as a complete security gate.

### Disconnect

```kotlin
client.disconnect()
```

Explicit disconnect:
- cancels heartbeat/stale checks
- suppresses auto-reconnect for that disconnect
- closes transport
- clears pending commands/state

### Close

```kotlin
client.close()
```

Use when the client will no longer be used.

---

## 4. Navigation

### Single pose

```kotlin
client.navigateTo(
    Pose2D(
        xMeters = 2.0,
        yMeters = 4.0,
        yawRadians = 0.0
    )
)
```

Returns `RobotResult<CommandId>` after admission. **Success means accepted, not physically completed.** Observe the correlated terminal `navigation_result` via `client.navigation`, plus fresh `client.telemetry`.

Required capability: `NAVIGATION`.

### Waypoints

```kotlin
client.navigateThrough(
    listOf(
        Pose2D(1.0, 1.0, 0.0),
        Pose2D(2.0, 1.0, 0.0),
        Pose2D(2.0, 2.0, 1.57)
    )
)
```

Required capability: `WAYPOINT_NAVIGATION`.

### Route

```kotlin
client.navigateRoute(
    NavigationRoute(
        waypoints = waypoints,
        stopOnFailure = true
    )
)
```

### Cancel

```kotlin
client.cancelNavigation()
```

Observe progress through `client.navigation` and/or `client.telemetry`.

---

## 5. Manual Motion

```kotlin
client.setManualVelocity(
    linearMps = 0.35,
    angularRadPerSec = -0.20
)
```

Required capability: `MANUAL_CONTROL`.

Stop:

```kotlin
client.stop()
```

SI units are used throughout:
- distance: meters
- angle: radians
- linear velocity: meters/second
- angular velocity: radians/second

---

## 6. Docking

```kotlin
client.dock()
client.cancelDocking()
client.undock()
```

Required capability: `DOCKING`.

Observe:
- `client.dockingState`
- `client.batteryState`
- `client.telemetry`

---

## 7. Safety

```kotlin
client.emergencyStop()
client.releaseEmergencyStop()
```

Required capability: `SAFETY`.

> These calls are software safety requests. They do not replace a physical emergency-stop circuit or certified machinery safety system.

---

## 8. Maps

Operations:
- `listMaps()`
- `switchMap(mapId)`
- `saveCurrentMap(name)`
- `renameMap(mapId, newName)`
- `deleteMap(mapId)`

Example:

```kotlin
when (val result = client.listMaps()) {
    is RobotResult.Success -> result.value.forEach(::println)
    is RobotResult.Failure -> println(result.error)
}
```

Map-management commands use local capability/input checks, but the gateway is authoritative for execution. A command acknowledgement need not mean a map transition has finished. Observe `activeMap` and `mapOperationState`.

### Mapping lifecycle

```kotlin
val start = client.startMapping(resumeUnsaved = false, discardUnsaved = false)
val stop = client.stopMapping(discardUnsaved = false)
```

The recovery options are mutually exclusive. `RECOVERABLE_SESSION_EXISTS` requires an operator decision; after a timeout, inspect authoritative map state before retrying.

### Occupancy-grid streaming

The client subscribes following authentication and exposes `occupancyGrid: StateFlow<RobotOccupancyGrid?>`. It decodes `map_snapshot` and `map_update` with nested `origin`, signed int8 occupancy cells (-1 unknown, 0–100 known) and zlib/base64 `data`, and resubscribes when sequences diverge. The old flattened `originX` / `dataBase64` fixture used by one tracked test is not the current gateway contract. The newer gateway's localization-recovery and navigation-path subscriptions are not yet first-class `RobotClient` APIs.

---

## 9. Virtual Walls

Domain types:

```kotlin
enum class VirtualWallType {
    LINE,
    POLYGON
}
```

Create:

```kotlin
client.createVirtualWall(
    VirtualWallDraft(
        mapId = "main_floor",
        name = "No Entry Zone",
        type = VirtualWallType.LINE,
        points = listOf(
            MapPoint(1.0, 2.0),
            MapPoint(4.0, 2.0)
        )
    )
)
```

Operations:
- `listVirtualWalls(mapId)`
- `createVirtualWall(draft)`
- `updateVirtualWall(wall)`
- `deleteVirtualWall(wallId)`
- `setVirtualWallEnabled(wallId, enabled)`
- `clearVirtualWalls(mapId)`

Client-side geometry validation enforces:
- line: at least 2 points
- polygon: at least 3 points
- finite coordinates

---

## 10. Saved Locations

```kotlin
client.saveLocation(
    name = "Reception",
    pose = Pose2D(2.5, 1.0, 3.14),
    mapId = "main_floor"
)
```

Operations:
- `listSavedLocations(mapId)`
- `saveLocation(name, pose, mapId)`
- `renameLocation(id, name)`
- `deleteLocation(id)`
- `navigateToLocation(id)`

Map-point validation:

```kotlin
client.checkMapPoint(
    MapPoint(
        xMeters = 3.2,
        yMeters = 1.4
    )
)
```

returns `MapPointValidity` with a boolean and optional reason.

---

## 11. Missions

Supported mission steps:
- Navigate to pose
- Navigate to saved location
- Wait
- Dock
- Undock

Example:

```kotlin
val mission = RobotMission(
    name = "Morning Route",
    steps = listOf(
        MissionStep.Undock,
        MissionStep.NavigateToLocation("reception"),
        MissionStep.Wait(5_000),
        MissionStep.NavigateToLocation("lounge"),
        MissionStep.Dock
    )
)

client.submitMission(mission)
```

Cancel:

```kotlin
client.cancelMission()
```

Observe execution with `client.missionProgress`.

---

## 12. Robot Configuration

```kotlin
client.updateMotionLimits(
    MotionLimits(
        maxLinearVelocityMps = 0.6,
        maxAngularVelocityRadPerSec = 1.2
    )
)
```

The SDK rejects non-positive velocity limits before network dispatch.

---

## 13. Diagnostics

Create a client diagnostic report:

```kotlin
client.collectDiagnosticReport()
```

The report includes:
- SDK version
- gateway version
- protocol version
- connection metrics
- telemetry freshness
- active map
- availability summary
- recent active error codes

Retrieve robot-side logs when supported:

```kotlin
client.getRecentRobotLogs(limit = 100)
```

Required capability: `LOG_RETRIEVAL`.

---

## 14. Multi-Robot Management

```kotlin
val manager = RobotManager()

val robotA = manager.getOrCreateClient(
    robotId = "robot-a",
    config = configA
)

val robotB = manager.getOrCreateClient(
    robotId = "robot-b",
    config = configB
)

println(manager.activeRobotIds)

manager.removeClient("robot-a")
manager.close()
```

Each robot receives an independent client and lifecycle.

---

## 15. Discovery

```kotlin
val discovery: RobotDiscoveryManager = RobotDiscoveryManagerImpl()

discovery.discoverRobots(
    port = 8088,
    timeoutMs = 5_000
).collect { robot ->
    println(robot.id + " @ " + robot.host + ":" + robot.port)
}
```

Discovery listens for local UDP beacons and emits unique robots.

---

## 16. Error Handling

Do not parse human-readable messages as program logic.

```kotlin
when (val result = client.dock()) {
    is RobotResult.Success -> Unit

    is RobotResult.Failure -> {
        val error = result.error

        if (error.recoverable) {
            // present a retry option
        }

        println(error.code + " / " + error.subsystem)
    }
}
```

`RobotError` provides:
- code
- subsystem
- severity
- message
- recoverable flag
- optional source code

---

## 17. Events

`events` is intended for transient runtime signals:
- `ErrorRaised`
- `ErrorCleared`
- `ConnectionLost`
- `ConnectionRestored`

Durable flows such as `connectionState` and `health` remain the source of current state.

---

## 18. Logging

```kotlin
val logger = RobotLogger { event ->
    println(event.level.toString() + " " + event.category + ": " + event.message)
}
```

Log levels:
- DEBUG
- INFO
- WARN
- ERROR

Authentication tokens should never be written into application logs.

---

## 19. TLS / WSS

```kotlin
RobotEndpoint(
    host = "robot.local",
    port = 8443,
    useTls = true
)
```

For custom trust configuration, provide both:
- `SSLSocketFactory`
- `X509TrustManager`

Do not install an insecure trust-all manager in production.

---

## 20. Testkit

### FakeRobotClient

Use `FakeRobotClient` in ViewModel/UI tests when a real gateway is unavailable.

It exposes mutable backing flows for connection, telemetry, battery, map, mission progress, and diagnostics, and can force command success/failure.

### ProtocolSimulator

```kotlin
val simulator = ProtocolSimulator()

val hello = simulator.generateHelloAckEnvelope()
val telemetry = simulator.generateTelemetryEnvelope(
    pose = Pose2D(1.0, 2.0, 0.5)
)
```

---

## 21. Deprecated API

`RobotRepository` is retained for compatibility with the controller application's earlier UI architecture and is annotated:

```text
@Deprecated("Use RobotClient instead for direct SDK interactions")
```

New integrations should use `RobotClient`. Deprecated `RobotRepository` has placeholder default-success implementations that must not be interpreted as evidence of actual robot execution.
