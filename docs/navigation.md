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

Any authenticated client can engage the emergency stop, even when another client owns the
control session. Releasing it requires the active control owner:

```kotlin
client.emergencyStop()
client.releaseEmergencyStop()
```

The gateway returns `CONTROL_SESSION_REQUIRED` when there is no active owner and
`CONTROL_SESSION_IN_USE` when another client owns control.
