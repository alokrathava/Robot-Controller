# Getting Started with Robot SDK

## Installation

Add the local Maven repository (or internal Maven server) to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
```

Add the SDK dependency to your module `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.alokrathava:robot-sdk:0.2.0")
}
```

## Quick Start Example

```kotlin
import com.alokrathava.sdk.RobotClient
import com.alokrathava.sdk.RobotEndpoint
import com.alokrathava.sdk.RobotSdk
import com.alokrathava.sdk.RobotSdkConfig
import com.alokrathava.sdk.model.Pose2D

val client: RobotClient = RobotSdk.create(
    RobotSdkConfig(
        endpoint = RobotEndpoint(host = "192.168.1.50", port = 8080)
    )
)

// Connect to robot
when (val result = client.connect()) {
    is RobotResult.Success -> println("Connected successfully!")
    is RobotResult.Failure -> println("Connection failed: ${result.error.message}")
}

// Observe telemetry
lifecycleScope.launch {
    client.telemetry.collect { telemetry ->
        telemetry?.let {
            println("Pose: (${it.xMeters}, ${it.yMeters}) Yaw: ${it.yawRadians}")
        }
    }
}

// Dispatch navigation goal
client.navigateTo(Pose2D(xMeters = 5.0, yMeters = 2.0, yawRadians = 0.0))
```
