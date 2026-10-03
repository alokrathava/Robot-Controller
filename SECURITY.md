# Security Policy

## Reporting a Vulnerability

Security issues that affect robot safety, motor control, or authentication must be reported responsibly.

**Please DO NOT create public GitHub issues for security vulnerabilities.**

To report a vulnerability:
1. Contact the maintainers directly via repository security reporting.
2. Include a detailed description of the issue, steps to reproduce, and affected version(s).
3. Allow up to 48 hours for an initial response.

## Safety & Physical Security Expectations

- **Software Emergency Stop**: The SDK `emergencyStop()` function calls software safety services on the robot gateway. It is NOT a substitute for physical e-stop buttons or hardware power cutoffs.
- **Authentication**: Do not commit production API tokens, robot secrets, or gateway passwords into public version control.
- **Network Boundaries**: Ensure WebSocket connectivity to `robot_gateway` is secured over trusted networks or WSS in production environments.
