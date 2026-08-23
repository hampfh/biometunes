# BiomeTunes 1.0.0 manual audio checklist

## Test record

- Checklist date: 2026-08-22
- Minecraft version: 26.2
- Operating system: macOS 26.6.2 (build 25G83), arm64
- Development client: `./gradlew runClient`
- Automated prerequisite suite: `./gradlew clean build packageSoundpack verifyDistributionArchives`
- Automated startup result: PASS — Fabric loaded BiomeTunes 1.0.0, BiomeTunes initialized, resources reloaded, and the sound engine started; the client was then terminated deliberately with Ctrl-C
- Manual execution status: not yet performed

Automated tests and a successful development-client startup can establish prerequisites, but they cannot establish what a human hears, observes in interactive UI, or experiences in real single-player/multiplayer play. Every row therefore remains `PENDING HUMAN VERIFICATION` until a person performs it. Do not release with any row pending or failed.

| # | Manual check | Date | Result | Automated prerequisite/evidence |
|---:|---|---|---|---|
| 1 | Fresh single-player world starts the mapped track | 2026-08-22 | PENDING HUMAN VERIFICATION | Development client startup gate only; world creation and audible playback were not observed. |
| 2 | Plains -> forest performs a five-second overlap | 2026-08-22 | PENDING HUMAN VERIFICATION | `PlaybackControllerTest` covers a 100-tick transition mathematically; no audible biome traversal was performed. |
| 3 | Crossfade midpoint has no obvious silence or loud spike | 2026-08-22 | PENDING HUMAN VERIFICATION | Equal-power midpoint gains are unit-tested; perceived loudness still requires hearing. |
| 4 | Rapid travel across three mappings leaves no orphan track | 2026-08-22 | PENDING HUMAN VERIFICATION | Third-track interruption ownership is unit-tested; rapid in-world travel was not observed. |
| 5 | Two biomes mapped to one family do not restart or notify twice | 2026-08-22 | PENDING HUMAN VERIFICATION | `MusicDirectorTest` covers same-track de-duplication; UI/audio behavior was not observed. |
| 6 | 0-second fade switches immediately | 2026-08-22 | PENDING HUMAN VERIFICATION | Zero-duration transitions are unit-tested; audible immediacy was not assessed. |
| 7 | 15-second fade completes and stops outgoing audio | 2026-08-22 | PENDING HUMAN VERIFICATION | Fade completion and configured duration arithmetic are unit-tested; a 15-second audible fade was not timed. |
| 8 | Volume 0%, 50%, and 100% responds immediately | 2026-08-22 | PENDING HUMAN VERIFICATION | Gain updates and slider normalization are unit-tested; the three UI/audio levels were not heard. |
| 9 | Ender Dragon enters and leaves boss music | 2026-08-22 | PENDING HUMAN VERIFICATION | Boss entry/exit selection is unit-tested; an End fight was not played. |
| 10 | Wither uses the temporary Ender Dragon track | 2026-08-22 | PENDING HUMAN VERIFICATION | Catalog data maps Wither to `ender_dragon`; an in-world Wither encounter was not played. |
| 11 | Disable stops custom audio and restores vanilla scheduling | 2026-08-22 | PENDING HUMAN VERIFICATION | Director/controller ownership release is unit-tested; vanilla scheduling was not heard after using the UI. |
| 12 | F3+T reload disposes old instances and resumes cleanly | 2026-08-22 | PENDING HUMAN VERIFICATION | Reload listener and retained-catalog logic compile/test; no interactive resource reload was performed. |
| 13 | World exit and world switch leave no audio playing | 2026-08-22 | PENDING HUMAN VERIFICATION | Disconnect, level-identity, and client-stop paths call `stop`; no world exit/switch was observed. |
| 14 | Connection to an unmodified 26.2 server succeeds | 2026-08-22 | PENDING HUMAN VERIFICATION | `fabric.mod.json` declares a client environment; no real vanilla server connection was attempted. |
| 15 | Music discs remain audible and unaffected | 2026-08-22 | PENDING HUMAN VERIFICATION | BiomeTunes only gates scheduled vanilla music; music-disc playback still requires in-game listening. |
| 16 | Malformed config falls back without deleting the file | 2026-08-22 | PENDING HUMAN VERIFICATION | `ConfigStoreTest` verifies defaults and byte-for-byte preservation; the development client's real config was not corrupted manually. |
| 17 | Malformed override catalog retains the previous valid catalog | 2026-08-22 | PENDING HUMAN VERIFICATION | `ReloadableTrackCatalogTest` verifies last-known-good retention; no interactive override reload was performed. |

## Additional pre-release gates

- PENDING HUMAN VERIFICATION: confirm the artist and approved public wording for the legacy `forest.ogg` attribution.
- PENDING EXTERNAL CONFIGURATION: set the Modrinth project environment to client **required** and server **unsupported**.
- PENDING RELEASE CREDENTIALS: configure `MODRINTH_TOKEN` and `MODRINTH_PROJECT_ID` only when an authorized maintainer is ready to publish.
- PENDING RELEASE AUTHORIZATION: do not tag, push, create a GitHub release, or run `:apps:mod:modrinth` until every gate above and all 17 rows are complete.
- PENDING FAIL-CLOSED APPROVAL: only after all 17 rows pass, the Forest attribution is confirmed, Modrinth environments and credentials are ready, and explicit release authorization is given may an authorized maintainer set the repository Actions variable `RELEASE_APPROVED_TAG` to the exact intended tag (for example, `v1.0.0`). Missing, empty, or mismatched values fail before build and publication. Remove or unset the variable after the release completes.
