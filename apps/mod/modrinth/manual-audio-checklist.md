# BiomeTunes 0.6 manual audio checklist

## Test record

- Checklist date: 2026-08-22
- Minecraft version: 26.2
- Operating system: macOS 26.6.2 (build 25G83), arm64
- Development client: `./gradlew runClient`
- Automated prerequisite suite: `./gradlew clean build packageSoundpack verifyDistributionArchives`
- Automated startup result: PASS — Fabric loaded BiomeTunes 0.5, BiomeTunes initialized, resources reloaded, and the sound engine started; the client was then terminated deliberately with Ctrl-C
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
| 18 | An opaque-roofed house becomes Sheltered and muffles the existing track without restarting it | 2026-08-30 | PENDING HUMAN VERIFICATION | Classifier, treatment-envelope, and no-reroll director behavior are unit-tested; the audible result was not assessed. |
| 19 | A glass-roofed and glass-walled house can be Sheltered but never Subterranean | 2026-08-30 | PENDING HUMAN VERIFICATION | Native skylight and enclosure semantics are unit-tested; the in-world glass structure was not observed. |
| 20 | A leaf canopy does not behave like an opaque surface roof | 2026-08-30 | PENDING HUMAN VERIFICATION | `MOTION_BLOCKING_NO_LEAVES` behavior is unit-tested through the observation boundary; an in-world canopy was not observed. |
| 21 | A shallow cave near an exposed entrance remains Outside or Sheltered | 2026-08-30 | PENDING HUMAN VERIFICATION | Nearby sky exposure forbids Subterranean in unit tests; cave-mouth traversal was not observed. |
| 22 | A deep generic cavern enters Subterranean and selects the profile pool | 2026-08-30 | PENDING HUMAN VERIFICATION | Deep evidence and resolver routing are unit-tested; a generic cavern was not played in-world. |
| 23 | A deep open ravine remains non-Subterranean while any sampled position sees sky | 2026-08-30 | PENDING HUMAN VERIFICATION | The immediate sky constraint is unit-tested; an open ravine was not traversed. |
| 24 | Deep Dark bypasses Environmental Classification and plays its exact biome pool unchanged | 2026-08-30 | PENDING HUMAN VERIFICATION | Native Underground Biome bypass and catalog membership are unit-tested. |
| 25 | Dripstone Caves bypasses Environmental Classification and plays its exact biome pool unchanged | 2026-08-30 | PENDING HUMAN VERIFICATION | Native Underground Biome bypass and catalog membership are unit-tested. |
| 26 | Lush Caves bypasses Environmental Classification and plays its exact biome pool unchanged | 2026-08-30 | PENDING HUMAN VERIFICATION | Native Underground Biome bypass and catalog membership are unit-tested. |
| 27 | Sulfur Caves bypasses Environmental Classification and plays its exact biome pool unchanged | 2026-08-30 | PENDING HUMAN VERIFICATION | Native Underground Biome bypass and catalog membership are unit-tested. |
| 28 | Entering a boss encounter removes Sheltered treatment and preserves boss-pool precedence | 2026-08-30 | PENDING HUMAN VERIFICATION | Boss bypass and resolver precedence over Subterranean are unit-tested; encounter audio was not heard. |
| 29 | The Nether has no Environmental Profile and retains its existing music behavior | 2026-08-30 | PENDING HUMAN VERIFICATION | The bundled profile-key contract asserts Overworld-only configuration. |
| 30 | The End has no Environmental Profile and retains its existing music behavior | 2026-08-30 | PENDING HUMAN VERIFICATION | The bundled profile-key contract asserts Overworld-only configuration. |
| 31 | Pausing holds both crossfade and Sheltered treatment progress, then resumes smoothly | 2026-08-30 | PENDING HUMAN VERIFICATION | Playback and treatment pause behavior are unit-tested; audible pause/resume was not assessed. |
| 32 | F3+T resets Environmental Classification and audio treatment, then samples cleanly | 2026-08-30 | PENDING HUMAN VERIFICATION | Accepted-reload reset boundaries compile and catalog retention is unit-tested. |
| 33 | Changing the audio device recreates the EFX filter without retaining an old-context ID | 2026-08-30 | PENDING HUMAN VERIFICATION | OpenAL context-token recreation is unit-tested with a fake backend. |
| 34 | Missing or failed EFX continues playback with gain-only Sheltered treatment and one warning | 2026-08-30 | PENDING HUMAN VERIFICATION | Unsupported/error fallback and failure deduplication are unit-tested. |
| 35 | Entering and leaving Subterranean uses the ordinary crossfade and leaves no orphan voice | 2026-08-30 | PENDING HUMAN VERIFICATION | Resolver pool keys and bounded crossfade voice ownership are unit-tested. |
| 36 | Environmental Debug HUD values update at the sampling cadence without visible frame or tick slowdown | 2026-08-30 | PENDING HUMAN VERIFICATION | HUD formatting consumes cached snapshots only; perceived performance requires interactive observation. |

## Additional pre-release gates

- PENDING HUMAN VERIFICATION: confirm the artist and approved public wording for the legacy `forest.ogg` attribution.
- PENDING POST-UPLOAD VERIFICATION: Modrinth derives project type and environment from the first uploaded
  version, not from project settings, so neither can be pre-set. After the first upload, confirm the listing
  shows type **Mod** and environment client **required** / server **unsupported**, derived from
  `"environment": "client"` in `fabric.mod.json`.
- PENDING RELEASE CREDENTIALS: configure `MODRINTH_TOKEN` and `MODRINTH_PROJECT_ID` only when an authorized maintainer is ready to publish.
- PENDING RELEASE AUTHORIZATION: do not tag, push, create a GitHub release, or run `:apps:mod:modrinth` until every gate above and all 36 rows are complete.
- PENDING FAIL-CLOSED APPROVAL: only after all 36 rows pass, the Forest attribution is confirmed, Modrinth environments and credentials are ready, and explicit release authorization is given may an authorized maintainer set the repository Actions variable `RELEASE_APPROVED_TAG` to the exact intended tag (`v` followed by `mod_version`). Missing, empty, or mismatched values fail before build and publication. Remove or unset the variable after the release completes.
