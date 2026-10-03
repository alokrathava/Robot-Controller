# Testing & Release

## 1. SDK Test Strategy

The SDK currently contains 18 unit-test classes under:

```text
robot/sdk/src/test/java/com/alokrathava/sdk/
```

Coverage is organized around behavior boundaries rather than UI implementation details.

| Test area | Representative tests |
|---|---|
| Client lifecycle | RobotClientTest, RobotClientReconnectionTest |
| Command correlation | PendingCommandRegistryTest |
| Protocol codec | ProtocolCodecTest |
| Cross-language compatibility | ProtocolFixtureTest |
| State recovery | StateResynchronizationTest |
| Navigation | NavigationTest |
| Maps | MapLifecycleTest |
| Virtual walls | VirtualWallTest |
| Saved locations | SavedLocationTest |
| Missions | MissionTest |
| Configuration | RobotConfigurationTest |
| Diagnostics | DiagnosticsTest |
| Discovery | DiscoveryTest |
| Security/transport | SecurityAndTransportTest |
| Multi-client | RobotManagerTest |
| Test tooling | TestkitTest |

The repository also includes a basic Android consumer sample to validate standalone SDK consumption.

---

## 2. Testkit

### FakeRobotClient

`FakeRobotClient` implements the same public interface as the real SDK client.

Use it to:
- test ViewModels without a gateway
- inject telemetry/battery/map state
- simulate connected/disconnected state
- force command success/failure
- validate UI reactions deterministically

### ProtocolSimulator

`ProtocolSimulator` creates deterministic JSON frames for:
- hello acknowledgement
- telemetry

This supports protocol-facing tests without a network socket.

---

## 3. Protocol Fixtures

Directory:

```text
protocol-fixtures/
```

Fixtures include:
- hello
- hello_ack
- telemetry
- navigate_to
- command_error
- save_map
- create_virtual_wall
- save_location
- submit_mission

The intent is to validate the same protocol examples from both Kotlin and robot-side tests.

This guards against a common distributed-system failure: both sides compile while disagreeing about JSON shape.

---

## 4. Local Verification Commands

SDK unit tests:

```bash
./gradlew :robot:sdk:testDebugUnitTest
```

SDK release AAR:

```bash
./gradlew :robot:sdk:assembleRelease
```

Publish to Maven Local:

```bash
./gradlew :robot:sdk:publishToMavenLocal
```

Controller debug build:

```bash
./gradlew :app:assembleDebug
```

Controller release build:

```bash
./gradlew :app:assembleRelease
```

---

## 5. Maven Publication

The SDK configures `maven-publish`.

Coordinates:

```text
groupId    = com.alokrathava
artifactId = robot-sdk
version    = 0.2.0
```

Release publication includes a sources JAR.

POM metadata includes:
- project name
- description
- project URL
- Apache-2.0 license metadata
- developer identity

> The repository currently has Apache-2.0 metadata in the POM but no root `LICENSE` file. Add the actual license file before broad public package distribution so repository licensing and published metadata agree.

---

## 6. AAR Output

Release AAR task:

```bash
./gradlew :robot:sdk:assembleRelease
```

Expected output directory:

```text
robot/sdk/build/outputs/aar/
```

The SDK ships consumer ProGuard/R8 rules to preserve:
- public SDK entry points
- data/error models
- protocol serialization types
- Kotlin serialization metadata

---

## 7. GitHub Actions

Workflow:

```text
.github/workflows/sdk-release.yml
```

### Build-and-test job

Configured for:
- pushes to master
- pull requests targeting master
- version tags
- manual dispatch

Steps:
1. checkout
2. install JDK 21
3. run SDK unit tests
4. assemble release AAR
5. upload AAR artifact

### Release job

Runs for tags after the build-and-test job.

It:
1. downloads the AAR artifact
2. determines whether the tag is a prerelease
3. creates a GitHub Release
4. attaches the AAR
5. generates release notes

Tags containing `alpha`, `beta`, or `rc` are marked as prereleases.

This document describes the workflow configuration; it does not claim a particular commit has passed CI unless a corresponding run exists.

---

## 8. Version Compatibility

Current documented combination:

| SDK | Protocol | Gateway | RobotNavigation |
|---|---:|---|---|
| 0.2.0 | 1 | 0.2.0 | 0.2.x |

Protocol version is the hard compatibility boundary.

Unknown compatible JSON fields are intended to be tolerated so minor schema evolution can occur without immediately breaking older clients.

See [Compatibility Matrix](compatibility.md).

---

## 9. Release Checklist

Before a public SDK release:

- [ ] Run SDK unit tests
- [ ] Build release AAR
- [ ] Build/minify a consumer application
- [ ] Validate sample app against the published/local Maven artifact
- [ ] Verify protocol fixtures against the robot-side implementation
- [ ] Confirm gateway/SDK compatibility matrix
- [ ] Confirm authentication tokens are absent from logs
- [ ] Test reconnect behavior against a real gateway disconnect
- [ ] Verify telemetry stale-state behavior
- [ ] Verify emergency-stop semantics against the robot-side safety supervisor
- [ ] Confirm R8/minification does not break serialization
- [ ] Review CHANGELOG
- [ ] Add/update repository LICENSE
- [ ] Tag the release
- [ ] Verify the GitHub Actions release artifact

---

## 10. Integration Quality Signals

The codebase intentionally includes:
- explicit protocol versioning
- capability negotiation
- structured errors
- typed command results
- command timeouts
- thread-safe pending-command correlation
- telemetry freshness
- automatic reconnect
- state resynchronization request after connection
- release minification rules
- standalone consumer sample
- cross-language fixtures
- fake client/testkit

These are the pieces that move a robot controller from a demo connection toward a reusable integration boundary.
