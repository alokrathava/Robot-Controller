# Robot Controller WebSocket Protocol v1

The Android SDK exchanges JSON WebSocket envelopes with `robot_gateway` in RobotNavigation. For newer gateway-only behavior, the [RobotNavigation protocol guide](https://github.com/alokrathava/RobotNavigation/blob/main/docs/websocket-protocol-v1.md) and its [canonical JSON fixtures](https://github.com/alokrathava/RobotNavigation/tree/main/protocol-fixtures) are authoritative.

## Envelope and correlation

~~~json
{
  "type": "navigate_to",
  "id": "cmd-1002",
  "protocolVersion": 1,
  "payload": { "xMeters": 1.5, "yMeters": 2.0, "yawRadians": 1.57 }
}
~~~

Commands typically use an `id` that the gateway repeats on `command_ack` or `command_error`. The SDK registers pending commands before sending so a fast ACK cannot outrun registration.

~~~json
{"type":"command_ack","id":"cmd-1002","protocolVersion":1}
~~~

A `command_ack` is **admission/acceptance**, not proof that a long-running navigation, mapping or docking action has finished. Observe robot state and terminal results separately.

~~~json
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
~~~

Use the error code and subsystem for program logic, not string parsing of the human-readable message.

## Message families

| Type | Direction | Purpose |
|---|---|---|
| `hello` / `hello_ack` | Client → gateway / gateway → client | Token handshake, version and capabilities |
| `ping` / `pong` | Client → gateway / gateway → client | Heartbeat and latency |
| `request_state_snapshot` / `robot_state_snapshot` | Client → gateway / gateway → client | Restore state on connection |
| `telemetry`, `battery_state`, `safety_state`, `docking_state`, `robot_health` | Gateway → client | Physical robot state |
| `navigate_to` / `navigate_through` | Client → gateway | Goal submission |
| `navigation_result` | Gateway → client | Terminal result with `requestId`, status and goal ID |
| `manual_velocity`, `stop`, `emergency_stop` | Client → gateway | Motion and safety requests |
| `list_maps`, `switch_map`, `start_mapping`, `stop_mapping`, `save_map` | Client → gateway | Map lifecycle requests |
| `map_state` | Gateway → client | Map mode and readiness |
| `subscribe_map` / `map_snapshot` / `map_update` | Both directions | Occupancy-grid stream |
| `mission_progress` | Gateway → client | Mission state |
| `command_ack` / `command_error` | Gateway → client | Correlated outcome of admission or error |

The SDK also implements virtual-wall, saved-location, diagnostics and other commands through typed request/response handling. **Typed response parity needs verification:** current RobotNavigation mapping documentation specifies `command_ack` for admitted long-running operations, whereas SDK `saveCurrentMap()` expects a typed map response (`save_map_ack`). This is not proven compatible by a shared protocol number.

## Map streaming format

The [canonical gateway `map_snapshot`](https://github.com/alokrathava/RobotNavigation/blob/main/protocol-fixtures/map_snapshot.json) contains `streamVersion`, `sequence`, `mapId`, map metadata, `width`, `height`, `resolution`, a nested origin (position + quaternion), `encoding`, `cellCount` and `data`.

`encoding = "occupancy-int8-zlib-base64-v1"` means row-major signed int8 occupancy cells (`-1` unknown, `0..100` occupied probability) compressed with zlib then base64 encoded. `map_update` includes `baseSequence` and the changed rectangle. The SDK applies matched updates, and triggers a subscription resync when sequences do not align.

The existing SDK `ProtocolCodecTest` uses an **older flattened** `dataBase64`/`originX` payload and fails on master. Do not use that test fixture as the canonical wire format.

## Newer gateway capabilities

RobotNavigation's inspected `hello_ack` advertises `0.3.0` and optional `map_streaming`, `localization_recovery` and `navigation_path`. The SDK's `RobotCapability` does not model all these labels, nor does `RobotClient` expose all their request methods. It does contain map subscription handling. Record successful test evidence before marking a version pair compatible.

## Security limits

The current SDK handler parses `hello_ack` but does not strictly validate its protocol-version/request-ID matching. Local feature checks allow calls when the advertised capability list is empty. Treat gateway authentication/authorization, command ownership, input validation and safety controls as essential enforcement boundaries. `ws://` is suitable only for isolated development; secure other deployments with properly configured `wss://`. A software E-stop does not replace a physical safety circuit.
