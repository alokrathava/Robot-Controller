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

## Prepare RobotNavigation on the LAN

Start RobotNavigation with an authentication token before binding the gateway to the network:

```bash
export ROBOT_GATEWAY_TOKEN="your-development-token"

./run.sh \
  --backend sim \
  --mode navigation \
  --rviz \
  --gateway-host 0.0.0.0 \
  --gateway-port 8080
```

Find the Ubuntu computer's LAN address with `ip -4 address show up`. Configure the phone or SDK with
that address rather than `127.0.0.1`:

```text
Host: 192.168.1.50
Port: 8080
Token: your-development-token
TLS: false
```

The phone and Ubuntu computer must be reachable on the same LAN. Disable AP/client isolation
on the Wi-Fi network. If Ubuntu's firewall is enabled, allow the gateway port:

```bash
sudo ufw allow 8080/tcp
```

Keep the token configured whenever the gateway listens on a non-loopback interface.

## Quick Start Example

```kotlin
import com.alokrathava.sdk.RobotAuthentication
import com.alokrathava.sdk.RobotClient
import com.alokrathava.sdk.RobotEndpoint
import com.alokrathava.sdk.RobotSdk
import com.alokrathava.sdk.RobotSdkConfig
import com.alokrathava.sdk.model.Pose2D

val client: RobotClient = RobotSdk.create(
    RobotSdkConfig(
        endpoint = RobotEndpoint(host = "192.168.1.50", port = 8080),
        authentication = RobotAuthentication.Token("your-development-token")
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
