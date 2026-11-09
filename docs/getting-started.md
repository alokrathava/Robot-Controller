# Getting Started with Robot SDK

This is a **local Maven integration guide** for the `Robot-Controller` Android SDK, version `0.2.0`. This version is configured for `mavenLocal()`; public Maven Central publication and successful release builds for the inspected master commit have **not been verified**.

## 1. Prerequisites and SDK installation

The SDK is an Android library (`minSdk 24`, `compileSdk 35`, Java 17 source/target). Prepare the Android build tools and a reachable RobotNavigation `robot_gateway` on a trusted LAN.

From the Robot-Controller root, publish to Maven Local (this is a command, **not a verified green result**):

~~~bash
./gradlew :robot:sdk:publishToMavenLocal
~~~

In the consumer's `settings.gradle.kts`:

~~~kotlin
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}
~~~

In its module `build.gradle.kts`:

~~~kotlin
dependencies {
    implementation("com.alokrathava:robot-sdk:0.2.0")
}
~~~

`samples/basic-android` is an independent consumer example. Its release variant enables minification; a passing build still needs verification.

## 2. Prepare RobotNavigation

An example RobotNavigation command for a development simulator is:

~~~bash
export ROBOT_GATEWAY_TOKEN="your-development-token"

./run.sh \
  --backend sim \
  --mode navigation \
  --rviz \
  --gateway-host 0.0.0.0 \
  --gateway-port 8080
~~~

Check the current RobotNavigation run script for supported CLI options. Use the robot host's actual LAN IP in Android, never `127.0.0.1` on the phone. Ensure Wi-Fi client isolation and firewall rules allow the connection. Bind to non-loopback only with appropriate authentication; use `wss://` outside a trusted development network.

## 3. Create, connect and observe

The following is an Activity/Fragment snippet where `lifecycleScope` is available:

~~~kotlin
import androidx.lifecycle.lifecycleScope
import com.alokrathava.sdk.RobotAuthentication
import com.alokrathava.sdk.RobotEndpoint
import com.alokrathava.sdk.RobotSdk
import com.alokrathava.sdk.RobotSdkConfig
import com.alokrathava.sdk.error.RobotResult
import com.alokrathava.sdk.model.Pose2D
import kotlinx.coroutines.launch

val client = RobotSdk.create(
    RobotSdkConfig(
        endpoint = RobotEndpoint(host = "192.168.1.50", port = 8080),
        authentication = RobotAuthentication.Token("your-development-token")
    )
)

lifecycleScope.launch {
    client.telemetry.collect { sample ->
        if (sample != null && sample.poseValid) {
            println("Robot: " + sample.xMeters + ", " + sample.yMeters)
        }
    }
}

lifecycleScope.launch {
    when (val result = client.connect()) {
        is RobotResult.Success -> println("Gateway connection accepted")
        is RobotResult.Failure -> println("Connection error: " + result.error.code)
    }
}

// Call client.close() when the owning component is destroyed.
~~~

Do **not** send physical motion solely because `connect()` succeeded. Verify fresh localization, safety state, active map and capability readiness.

## 4. Submit a navigation request

When the robot is independently confirmed ready:

~~~kotlin
lifecycleScope.launch {
    when (val result = client.navigateTo(Pose2D(2.0, 1.0, 0.0))) {
        is RobotResult.Success -> println("Goal accepted: " + result.value.value)
        is RobotResult.Failure -> println("Goal error: " + result.error.code)
    }
}
~~~

Acceptance is not arrival. Observe `client.navigation` and fresh `client.telemetry` for the terminal outcome, and retain independent motion-stop/safety controls.

## 5. Before integration or release claims

- Compare SDK behavior with [RobotNavigation canonical fixtures](https://github.com/alokrathava/RobotNavigation/tree/main/protocol-fixtures), especially mapping and occupancy grids.
- Build the actual SDK artifact and an independent R8-minified consumer.
- Verify the exact gateway/SDK version pair; the latest inspected RobotNavigation fixture advertises a newer gateway version than the historical 0.2.0 baseline.
- Resolve the current SDK unit-test failure before claiming a green release.

See [SDK API](sdk-api.md), [Protocol](protocol.md), [Compatibility Matrix](compatibility.md), and [Testing & Release](testing-and-release.md).
