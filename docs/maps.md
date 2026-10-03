# Map Management

## Active Map

Observe the currently active map broadcast by the gateway:

```kotlin
lifecycleScope.launch {
    client.activeMap.collect { map ->
        map?.let {
            println("Active map: ${it.name} (id: ${it.id})")
        }
    }
}
```

## Listing Available Maps

```kotlin
when (val result = client.listMaps()) {
    is RobotResult.Success -> {
        result.value.forEach { map ->
            println("Map ${map.name} (${map.id}) - Active: ${map.isActive}")
        }
    }
    is RobotResult.Failure -> println("Failed to list maps: ${result.error.message}")
}
```

## Switching Maps

```kotlin
when (val result = client.switchMap("second_floor")) {
    is RobotResult.Success -> println("Switched map successfully")
    is RobotResult.Failure -> println("Switch map failed: ${result.error.message}")
}
```

> [!NOTE]
> Map IDs are validated both client-side and server-side to prevent path traversal attempts (`..`, `/`, `\`).
