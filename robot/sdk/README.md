# Robot SDK (`0.2.0`)

The **Robot SDK** (`:robot:sdk`) is a framework-neutral Kotlin Android client library for the `RobotNavigation` ROS 2 robot control stack. It connects to the `robot_gateway` WebSocket bridge and exposes reactive `StateFlow`/`SharedFlow` state plus structured suspend APIs for navigation, manual control, docking, safety, maps, virtual walls, saved locations, missions, configuration, diagnostics, discovery, and multi-robot client management. For the complete public API, see [`docs/sdk-api.md`](../../docs/sdk-api.md).

---

## 1. System Architecture

```text
Android Integrator App
        |
        v
Robot SDK (RobotClient)
        |
        | Authenticated + Versioned WebSocket (Protocol v1)
        v
robot_gateway (ROS2 Python Node)
        |
        | ROS2 Topics / Services / Actions
        v
RobotNavigation / Nav2 / Safety / Docking / Hardware
```

The Android SDK is a lightweight, framework-neutral client. It contains **zero Python dependencies** and zero DI framework requirements (no Hilt, Dagger, KSP, or Room inside `:robot:sdk`).

All physical robot states (telemetry, velocity, position, battery, docking, safety, health) originate from `RobotNavigation`. The SDK does not simulate physical robot success locally.

---

## 2. Quick Start & SDK Creation

Initialize the client using `RobotSdk.create()`:

```kotlin
import com.alokrathava.sdk.RobotSdk
import com.alokrathava.sdk.RobotSdkConfig
import com.alokrathava.sdk.RobotEndpoint
import com.alokrathava.sdk.RobotAuthentication
import com.alokrathava.sdk.RobotLogger

val robotClient = RobotSdk.create(
    RobotSdkConfig(
        endpoint = RobotEndpoint(
            host = "192.168.1.100",
            port = 8080
        ),
        authentication = RobotAuthentication.Token(
            token = "your-robot-token"
        ),
        commandTimeoutMs = 10_000,
        logger = RobotLogger { event ->
            println("[${event.category}] ${event.level}: ${event.message}")
        }
    )
)
```

---

## 3. Basic Usage

### 3.1 Connecting & Disconnecting

```kotlin
// Connect & authenticate handshake
val result = robotClient.connect()
if (result is RobotResult.Success) {
    println("Connected successfully to robot gateway!")
} else if (result is RobotResult.Failure) {
    println("Connection failed: ${result.error.message}")
}

// Disconnect
robotClient.disconnect()

// Release resources when finished
robotClient.close()
```

---

### 3.2 Collecting Telemetry & Robot State

```kotlin
// Connection state
lifecycleScope.launch {
    robotClient.connectionState.collect { state ->
        when (state) {
            is ConnectionState.Connected -> println("Connected (Gateway v${state.gatewayVersion})")
            is ConnectionState.Connecting -> println("Connecting...")
            is ConnectionState.Authenticating -> println("Authenticating...")
            is ConnectionState.Disconnected -> println("Disconnected")
            is ConnectionState.Failed -> println("Failed: ${state.error.message}")
        }
    }
}

// Telemetry (SI units: meters, radians, m/s, rad/s)
lifecycleScope.launch {
    robotClient.telemetry.collect { telem ->
        telem?.let {
            println("Position: (${it.xMeters}m, ${it.yMeters}m), Yaw: ${it.yawRadians}rad")
            println("Linear Vel: ${it.linearVelocityMps}m/s, Angular Vel: ${it.angularVelocityRadPerSec}rad/s")
        }
    }
}

// Battery State
lifecycleScope.launch {
    robotClient.batteryState.collect { batt ->
        batt?.let {
            println("Battery: ${it.percentage}% | Voltage: ${it.voltageVolts}V | Charging: ${it.isCharging}")
        }
    }
}

// Safety State
lifecycleScope.launch {
    robotClient.safetyState.collect { safety ->
        println("Safety State: $safety")
    }
}
```

---

### 3.3 Controlling Motion, Navigation & Docking

#### Manual Velocity Control
```kotlin
// Set velocity (linear m/s, angular rad/s)
robotClient.setManualVelocity(linearMps = 0.5, angularRadPerSec = 0.2)

// Emergency stop motion
robotClient.stop()
```

#### Navigation
```kotlin
val targetPose = Pose2D(xMeters = 2.5, yMeters = 1.0, yawRadians = 1.57)
val result = robotClient.navigateTo(targetPose)

if (result is RobotResult.Success) {
    println("Goal submitted with ID: ${result.value.value}")
}

// Cancel active navigation
robotClient.cancelNavigation()
```

#### Docking & Safety
```kotlin
// Request autonomous docking
robotClient.dock()

// Cancel docking
robotClient.cancelDocking()

// Undock
robotClient.undock()

// Emergency Stop
robotClient.emergencyStop()

// Release Emergency Stop
robotClient.releaseEmergencyStop()
```

---

## 4. Safety Disclaimer

> [!CAUTION]
> The SDK software emergency-stop API (`emergencyStop()`) requests the `RobotNavigation` safety supervisor (`/robot/safety/emergency_stop`) to inhibit motion. It does **not** replace a hardware emergency-stop button or certified physical safety mechanism.

---

## 5. Protocol Specification (`v1`)

The SDK uses WebSocket JSON Protocol Version `1`.

### 5.1 Handshake Example
Client Request:
```json
{
  "type": "hello",
  "id": "cmd-1001",
  "protocolVersion": 1,
  "payload": {
    "sdkVersion": "0.2.0",
    "clientId": "android-client",
    "token": "your-robot-token"
  }
}
```

Gateway Response:
```json
{
  "type": "hello_ack",
  "id": "cmd-1001",
  "protocolVersion": 1,
  "payload": {
    "gatewayVersion": "0.2.0",
    "capabilities": ["navigation", "manual_control", "docking", "safety", "telemetry", "battery", "health"]
  }
}
```

### 5.2 Command Acknowledgements
Command Request:
```json
{
  "type": "navigate_to",
  "id": "cmd-1002",
  "protocolVersion": 1,
  "payload": {
    "xMeters": 1.5,
    "yMeters": 2.0,
    "yawRadians": 1.57
  }
}
```

Success Response:
```json
{
  "type": "command_ack",
  "id": "cmd-1002",
  "protocolVersion": 1
}
```

Error Response:
```json
{
  "type": "command_error",
  "id": "cmd-1002",
  "protocolVersion": 1,
  "error": {
    "code": "LOCALIZATION_LOST",
    "subsystem": "navigation",
    "severity": "ERROR",
    "message": "Robot localization is unavailable",
    "recoverable": true
  }
}
```

---

## 6. Build Instructions

To build the standalone release `.aar` binary:

```bash
./gradlew :robot:sdk:assembleRelease
```

Output binary: `robot/sdk/build/outputs/aar/sdk-release.aar`

To run SDK unit tests:

```bash
./gradlew :robot:sdk:testDebugUnitTest
```
