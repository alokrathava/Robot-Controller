# Changelog

All notable changes to the Robot SDK and Gateway will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.0] - 2026-10-01

### Added
- **Controlled Automatic Reconnection:** Bounded exponential backoff with configurable initial/max delays and jitter (`ReconnectPolicy`).
- **Heartbeat & Latency Measurement:** Protocol-level `ping`/`pong` heartbeats and round-trip latency tracking in `ConnectionMetrics`.
- **Stale Telemetry Detection:** Dedicated `TelemetryFreshness` StateFlow tracking last telemetry reception age without polluting raw state.
- **Capability Negotiation:** `RobotCapability` enum and local capability checks before dispatching unsupported commands.
- **Map Support:** Added public map APIs `listMaps()`, `switchMap(mapId)`, and `activeMap: StateFlow<RobotMap?>`.
- **Gateway Session Control:** Single active control session policy with structured `CONTROL_SESSION_IN_USE` error codes.
- **Multi-Client Broadcast:** Multi-client read-only telemetry broadcasting.
- **Event Stream:** Shared flow `events: SharedFlow<RobotEvent>` emitting `ErrorRaised`, `ErrorCleared`, `ConnectionLost`, `ConnectionRestored`.
- **Local Maven Publication:** `maven-publish` Gradle plugin generating `com.alokrathava:robot-sdk:0.2.0` with sources artifact.
- **Standalone Consumer Sample:** External sample application (`samples/basic-android`) consuming published Maven artifact with R8 minification.
- **Cross-Language Protocol Fixtures:** Shared JSON fixture suite validated by both Kotlin and Python unit tests.

### Changed
- Refactored `ConnectionState.Connected` to include `gatewayVersion`, `protocolVersion`, `capabilities`, and `sessionId`.
- Upgraded gateway protocol handshake to advertise `0.2.0` capabilities.
- Preserved unknown/null semantics for telemetry standard deviations and obstacle distances.

### Security
- Added server-side map ID validation preventing path traversal (`..`, `/`, `\`).
- Redacted sensitive authentication tokens from gateway and SDK log output.
- Enforced 5MB maximum message frame size limits on the gateway.
