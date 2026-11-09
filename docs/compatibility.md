# SDK and RobotNavigation Compatibility

This is a **source and protocol evidence matrix**, not a complete interoperability certification. Sharing `protocolVersion: 1` is necessary but insufficient for full functionality.

| Component | Reference | Evidence / status |
|---|---|---|
| Robot SDK | `0.2.0`, `Robot-Controller/master`, commit `269888a` | Source and Maven metadata present; last inspected master CI failed |
| Earlier documented gateway baseline | `0.2.0` | Previously declared pairing, not reverified in this review |
| Current RobotNavigation gateway fixture | `0.3.0` | Canonical `hello_ack.json` advertises Protocol v1 and additional capabilities; compatibility **unverified** |
| Wire schema | Protocol `1` | Shared envelope format; individual message semantics still require verification |

The earlier `0.1.0-alpha.1` SDK / `0.1.0` gateway pair was historically documented but has not been requalified.

## Known differences and gaps

- Newer gateway fixture capabilities include `map_streaming`, `localization_recovery` and `navigation_path`. Not all are represented by the current SDK `RobotCapability` enum or exposed as first-class operations, although the client has an occupancy-grid subscription.
- `RobotClientImpl` parses `hello_ack` but does not enforce strict handshake request-ID/protocol-version matching. The local capability check also permits calls if the negotiated set is empty.
- RobotNavigation's canonical map fixtures use a nested origin and compressed `data`, while one tracked SDK test uses an old flattened payload.
- RobotNavigation documents generic `command_ack` for long-running map admission. The SDK's `saveCurrentMap()` expects a typed map result (such as `save_map_ack`). This requires live contract verification.

## Qualification checklist

1. Verify authentication, handshake version, request correlation and capability behavior.
2. Run the current SDK against the [RobotNavigation canonical fixtures](https://github.com/alokrathava/RobotNavigation/tree/main/protocol-fixtures).
3. Check typed versus generic acknowledgements and long-running map/navigation state transitions.
4. Run an end-to-end SDK/gateway simulation and record command acceptance, terminal results, timeouts and reconnect behavior.
5. Record the exact SDK and gateway commit hashes, fixture revision and green CI evidence before marking a pairing **verified**.

References: [RobotNavigation Protocol v1](https://github.com/alokrathava/RobotNavigation/blob/main/docs/websocket-protocol-v1.md) · [SDK testing and release](testing-and-release.md) · [October 22, 2026 master CI failure](https://github.com/alokrathava/Robot-Controller/actions/runs/39702386478).
