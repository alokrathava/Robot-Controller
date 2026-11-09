# Testing and SDK Release — Actual Inventory

## Tracked test inventory

As inspected on `Robot-Controller/master` at commit `269888a` (October 22, 2026):

| Scope | File | Status |
|---|---|---|
| SDK | `robot/sdk/src/test/java/com/alokrathava/sdk/internal/serialization/ProtocolCodecTest.kt` | **1 class, 4 test methods** |
| Navigation UI feature | `feature/navigation/src/test/java/com/alokrathava/navigation/NavigationViewModelTest.kt` | Separate feature test file |
| Map UI feature | `feature/map/src/test/java/com/alokrathava/map/MapViewModelTest.kt` | Separate feature test file |

Earlier documentation claimed 18 SDK unit-test classes covering command registry, reconnection, navigation, discovery and more. Those classes are **not in the inspected master tree**. `FakeRobotClient` and `ProtocolSimulator` are shipped in the main SDK source set; their existence does not establish comprehensive test coverage.

## Inspected GitHub Actions evidence

The [last master run for commit 269888a](https://github.com/alokrathava/Robot-Controller/actions/runs/39702386478) on **October 22, 2026** reported:

- `:robot:sdk:testDebugUnitTest` — failed (**4 tests, 1 failure**).
- Failing method — `ProtocolCodecTest.decodeMapSnapshot_parsesGridDataCorrectly`.
- `:robot:sdk:assembleRelease` and artifact upload — skipped.

The failing test's old flattened `widthCells`, `originX` and `dataBase64` fields differ from the current decoder and [RobotNavigation canonical map snapshot](https://github.com/alokrathava/RobotNavigation/blob/main/protocol-fixtures/map_snapshot.json), which use `width`, `height`, nested `origin` and compressed `data`.

A later **October 26, 2026** run on an `e2e` branch passed the unit-test step but failed `:robot:sdk:verifyReleaseResources` due to missing theme attributes. It does not establish that the same issue applies to master.

**No tests or builds were run as part of this documentation update.**

## Protocol fixtures and release tooling

The controller repo tracks **9** JSON fixtures (hello, hello_ack, telemetry, navigate_to, command_error, save_map, create_virtual_wall, save_location and submit_mission). RobotNavigation maintains a larger canonical set, including `map_snapshot`, `map_update`, `map_state` and mapping recovery. These are not presently identical.

Configured local commands (run only when intentionally authorized):

~~~bash
./gradlew :robot:sdk:testDebugUnitTest
./gradlew :robot:sdk:assembleRelease
./gradlew :robot:sdk:publishToMavenLocal
./gradlew :app:assembleDebug
~~~

The Maven coordinate is `com.alokrathava:robot-sdk:0.2.0`. AAR outputs should be found in `robot/sdk/build/outputs/aar/`. `samples/basic-android` is present and configured to consume a Maven artifact with release R8 shrinking; its independent build success is **not verified**.

The [SDK release workflow](../.github/workflows/sdk-release.yml) runs tests before AAR assembly, uploads the resulting artifact, and creates GitHub Releases for tags. The workflow's presence does not mean a particular commit passed.

Maven POM declares Apache-2.0 license metadata but **no root LICENSE file** was tracked at the inspected commit. Do not claim the repository itself is licensed solely on the basis of POM metadata.

## Release verification checklist

- [ ] Current intended release commit has green SDK test results.
- [ ] SDK release AAR builds and uploads successfully.
- [ ] Independent `samples/basic-android` consumer resolves the actual artifact and passes R8 build.
- [ ] RobotNavigation canonical fixtures and typed/generic ACK semantics match the SDK.
- [ ] Reconnection, cancellation, timeout, authentication, map recovery and motion safety behavior are verified.
- [ ] Credential redaction and appropriate `wss://` deployment are confirmed.
- [ ] A repository license file is approved and added.
- [ ] Exact SDK/gateway pairing is entered in the [compatibility matrix](compatibility.md) with run evidence.
- [ ] Release tag and attached artifact are verified.

**These are required future checks, not accomplished validations.** This documentation edit does not repair runtime code or generate tests.
