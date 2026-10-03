# Robot Controller Documentation

This directory contains technical documentation for the Robot Controller application and reusable Robot SDK.

The project has two distinct layers:

- **Robot SDK (`:robot:sdk`)** — framework-neutral Android/Kotlin client library for the RobotNavigation gateway.
- **Controller application** — modular Jetpack Compose operator application that consumes the SDK.

## Recommended Reading Order

1. [Architecture Deep Dive](architecture.md) — system boundary, module topology, state ownership, and command lifecycle.
2. [SDK API Guide](sdk-api.md) — public API and domain capabilities.
3. [Getting Started](getting-started.md) — integrate the SDK.
4. [Connection Lifecycle](connection.md) — authentication, reconnect, heartbeat, and stale telemetry.
5. [Protocol Specification](protocol.md) — wire contract.
6. [Controller App Guide](controller-app.md) — how a real Compose app consumes the SDK.
7. [Testing & Release](testing-and-release.md) — verification and packaging.

## Focused Guides

| Topic | Guide |
|---|---|
| SDK installation | [Getting Started](getting-started.md) |
| Connection states & reconnection | [Connection Lifecycle](connection.md) |
| Pose navigation & manual motion | [Navigation](navigation.md) |
| Map switching | [Map Management](maps.md) |
| Protocol envelopes | [Protocol v1](protocol.md) |
| Version compatibility | [Compatibility Matrix](compatibility.md) |
| Full SDK surface | [SDK API Guide](sdk-api.md) |
| Android app architecture | [Controller App Guide](controller-app.md) |
| Test / package / release | [Testing & Release](testing-and-release.md) |
| Vulnerability reporting | [Security Policy](../SECURITY.md) |

## Visual Architecture

![System architecture](images/system-architecture.svg)

![SDK command lifecycle](images/sdk-command-lifecycle.svg)

![Android module map](images/module-map.svg)
