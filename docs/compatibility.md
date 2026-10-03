# Compatibility Matrix

| SDK Version | Protocol Version | Gateway Version | RobotNavigation Version |
| :--- | :--- | :--- | :--- |
| `0.1.0-alpha.1` | 1 | `0.1.0` | 0.1.x |
| **`0.2.0`** | **1** | **`0.2.0`** | **0.2.x** |

## Compatibility Rules
- **Major Protocol Version:** Must match exactly (`protocolVersion = 1`). Mismatches result in immediate `PROTOCOL_VERSION_MISMATCH` connection failure.
- **Gateway Version:** Tested against `0.2.0`. Minor/patch differences are compatible if protocol version matches.
- **Unknown Fields:** `robot_gateway` and SDK ignore unknown JSON fields to preserve forward/backward compatibility.
