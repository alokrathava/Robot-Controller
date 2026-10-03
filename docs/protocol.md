# Protocol Specification Version 1

## Message Frame

All WebSocket messages exchanged between the SDK and `robot_gateway` use JSON envelopes:

```json
{
  "type": "hello",
  "id": "cmd-1001",
  "protocolVersion": 1,
  "payload": {
    "sdkVersion": "0.2.0",
    "token": "secret"
  }
}
```

## Standard Messages

| Type | Direction | Description |
| :--- | :--- | :--- |
| `hello` | Client -> Gateway | Initial handshake & auth token |
| `hello_ack` | Gateway -> Client | Handshake response with gateway capabilities & session ID |
| `ping` | Client -> Gateway | Heartbeat ping |
| `pong` | Gateway -> Client | Heartbeat pong |
| `telemetry` | Gateway -> Client | Broadcast robot state (20Hz) |
| `navigate_to` | Client -> Gateway | Dispatch Nav2 pose goal |
| `list_maps` | Client -> Gateway | Request list of available maps |
| `list_maps_ack` | Gateway -> Client | Array of available maps |
| `switch_map` | Client -> Gateway | Request map switch |
| `command_ack` | Gateway -> Client | Command success acknowledgement |
| `command_error` | Gateway -> Client | Structured command error |
