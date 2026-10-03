# Robot Navigation & Manual Control

## Dispatching Navigation Goals

Send a 2D pose target (`xMeters`, `yMeters`, `yawRadians`) in the map coordinate frame:

```kotlin
val result = client.navigateTo(Pose2D(xMeters = 3.5, yMeters = -1.2, yawRadians = 1.57))
when (result) {
    is RobotResult.Success -> println("Goal accepted: ${result.value.value}")
    is RobotResult.Failure -> println("Goal rejected: ${result.error.message}")
}
```

## Cancelling Navigation

```kotlin
client.cancelNavigation()
```

## Manual Velocity Control

Send linear velocity (m/s) and angular velocity (rad/s):

```kotlin
client.setManualVelocity(linearMps = 0.4, angularRadPerSec = -0.1)
```

## Emergency Stop

Emergency stop is safety-critical and bypasses control session ownership:

```kotlin
client.emergencyStop()
client.releaseEmergencyStop()
```
