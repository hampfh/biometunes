# BiomeTunes

BiomeTunes is a client-only Fabric mod that gives Minecraft 26.2 a weighted soundtrack selected from the player's current biome, dimension, or boss encounter. It covers all 66 vanilla 26.2 biomes, rerolls after every track or intentional silence interval, crossfades between its 21 included tracks, pauses scheduled vanilla music while it owns playback, and returns control to vanilla music when disabled or stopped. The Wither temporarily uses the Ender Dragon track because the imported soundpack has no separate Wither recording.

## Requirements and installation

- Minecraft Java Edition 26.2 and Java 25.
- [Fabric Loader](https://fabricmc.net/use/installer/) 0.19.3 or newer.
- [Fabric API](https://modrinth.com/mod/fabric-api) for Minecraft 26.2 (the development build uses `0.158.0+26.2`).
- [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin) `1.13.13+kotlin.2.4.10` or newer.
- Optional: [Mod Menu](https://modrinth.com/mod/modmenu) 20.0.1 or newer for a graphical settings entry.

Install the Fabric loader and required dependencies on the client, then place `biometunes-<version>.jar` in the client's `mods` directory. The soundtrack is embedded in the mod JAR, so mod users do not install the standalone soundpack ZIP.

BiomeTunes works in single-player and is client-only by design: it requires no server installation and is intended to connect to an unmodified vanilla Minecraft 26.2 server. A real vanilla-server connection remains a required human pre-release check; see the [manual audio checklist](apps/mod/modrinth/manual-audio-checklist.md).

## Settings

Run the client-side `/biometunes` command to open the settings screen. If Mod Menu is installed, open **Mods**, select **BiomeTunes**, and use its configuration button. The screen controls whether BiomeTunes is enabled, master volume, crossfade duration from 0 to 15 seconds, biome notifications, boss music, boss notifications, and the opt-in Environmental Debug HUD. Settings are stored in the client configuration directory as `biometunes.json`.

## Environmental Classification

In dimensions with an Environmental Profile, BiomeTunes classifies the player into one of three Environment Modes:

- **Outside** plays the ordinary biome, biome-tag, dimension, or fallback pool.
- **Sheltered** keeps that same selected track playing while smoothly lowering its gain and high frequencies. If OpenAL EFX is unavailable or fails, gain reduction remains active and playback continues.
- **Subterranean** crossfades to the profile's configured subterranean pool.

Boss encounters and Native Underground Biomes bypass Environmental Classification and play their existing pools unchanged. The bundled profile enables Environmental Classification only in the Overworld; Deep Dark, Dripstone Caves, Lush Caves, and Sulfur Caves are Native Underground Biomes and therefore play their exact `underground` biome pool.

Classification runs on the existing ten-tick context cadence and has a fixed maximum cost: nine sky-light and heightmap observations, 26 enclosure block-state observations, and one additional sky-light plus block-light observation at the player. Unloaded chunks are omitted and never requested. The Environmental Debug HUD displays only the last cached classification and audio-treatment diagnostics, so enabling it does not add sampling work.

## Repository layout

- `apps/mod` — the Kotlin/Fabric client mod, automated tests, and the Modrinth release copy in `apps/mod/modrinth`.
- `apps/soundpack` — the canonical audio/resource source copied into the mod JAR and packaged as a standalone ZIP.
- `apps/datapack` — the original datapack implementation, retained for legacy users.
- `.github/workflows` — Java 25 build CI and tag-driven release automation.

## Development

Use a Java 25 JDK. The Gradle wrapper provides the rest of the build toolchain.

```bash
./gradlew build
./gradlew test
./gradlew runClient
```

### Soundtrack catalog

Track pools and their weights live in the existing catalog at `apps/mod/src/client/resources/assets/biometunes/biometunes/tracks.json`. A resource pack can replace the catalog at `assets/biometunes/biometunes/tracks.json`; reload resources to apply it. The catalog is replaced as one resource rather than merged entry by entry.

Every biome, boss, biome-tag, dimension, and fallback mapping is an array of weighted entries. For example:

```json
{
  "silence": {
    "weight": 1,
    "duration_seconds": { "min": 30, "max": 120 }
  },
  "biomes": {
    "minecraft:plains": [
      { "track": "plains", "weight": 8 },
      { "track": "forest", "weight": 2 },
      {
        "track": "silence",
        "weight": 3,
        "duration_seconds": { "min": 15, "max": 60 }
      }
    ]
  }
}
```

Weights must be non-negative integers, and every pool must have at least one positive-weight entry. A pool without a `silence` entry automatically includes the global silence option. With the bundled song weight of 3 and global silence weight of 1, single-song pools have a 75% song chance and a 25% silence chance. Mountain pools contain two weight-3 songs, so they have a 6/7 music chance and a 1/7 silence chance. An inline silence entry replaces the global option for that pool, including its weight and duration range; the bundled boss pools use an inline silence weight of 0 to disable silence. Silence duration bounds are inclusive seconds; the mod samples uniformly between `min` and `max`. Song entries must reference an ID from `tracks` and cannot define a duration; `silence` is reserved and cannot be used as a song ID.

The top-level `environmental_profiles` object is required; use `{}` to opt every dimension out. Each profile is keyed by a namespaced dimension identifier and requires `native_underground_biomes`, a nonempty `subterranean_tracks` pool, `sampling`, `weights`, `thresholds`, `smoothing_seconds`, and `sheltered_audio`. Radii are limited to 1–16 blocks, `surface_depth_scale` to 1–256, score weights to finite values from 0–10 with positive exposure and depth totals, thresholds and audio values to 0–1, and smoothing to 0–10 seconds. Enter thresholds must be strictly beyond their matching exit thresholds. Native Underground Biomes must have exact biome mappings, and subterranean entries use the same validated weighted-pool format as every other soundtrack mapping. Resource packs may add an Environmental Profile for another dimension without changing mod code.

For a clean release-equivalent build of both distributions, run:

```bash
./gradlew clean build packageSoundpack verifyDistributionArchives
```

The Fabric artifact is `apps/mod/build/libs/biometunes-<version>.jar`, produced by `:apps:mod:jar`. The standalone soundpack is `build/distributions/biometunes-soundpack-<version>.zip`, produced by `packageSoundpack`. `<version>` is `mod_version` from `gradle.properties`, which is the single source of truth: it flows into the archive names, `fabric.mod.json`, and the Modrinth upload, so bumping the version is a one-line change.

The release workflow only runs for `v*` tags and rejects a tag whose version differs from `mod_version` in `gradle.properties`. It is also fail-closed on the repository Actions variable `RELEASE_APPROVED_TAG`: a missing, empty, or non-matching value stops the job before the build and all publication steps. A tag alone never authorizes publication.

Only after all 36 manual checklist rows pass, publication credentials exist, and explicit release authorization is given may an authorized maintainer set `RELEASE_APPROVED_TAG` to the exact tag name (`v` followed by `mod_version`). Remove or unset the variable after that release completes. Ordinary builds and tests do not need publishing credentials; an actual Modrinth upload requires repository secrets named exactly `MODRINTH_TOKEN` and `MODRINTH_PROJECT_ID`.

## Soundtrack provenance and credits

The soundpack was imported from [`BiomeTunes/soundpack`](https://github.com/BiomeTunes/soundpack) at exact commit `e45e65833b2808d53050eccf2bd3f47ad35a397c` and adapted to the `biometunes` namespace. Credits retained from that source are:

- Hampus Hallkvist (Hampfh) — pack developer.
- Abraham Frato (Reklawer) — Plains, Forest, The End, Mountains, Mountains 2, Dark Forest, Birch Forest, Snowy, Snowy Ocean, Frozen Ocean, and Underground.
- Martin Ryberg Laude (Mar01) — Desert and Ocean.
- Zacharias Frato (Taraneas) — Savanna, Ender Dragon boss fight, Beach (ambient), Jungle, Swamp, and Taiga. This release has no separate Beach OGG and maps beach biomes to Ocean; the Beach credit is retained from the legacy source README.
- Arman Aspromonti — Warm Ocean.
- Edit Lundström (Edito1) — Flower Forest and Nether Wastes.

Full source links and track-by-track credits are in the [soundpack README](apps/soundpack/README.md).

## License

BiomeTunes is licensed under the [Apache License 2.0](LICENSE).

## Publishing

BiomeTunes is published on Modrinth as [`biometunes`](https://modrinth.com/project/biometunes). The project description and per-version changelogs live in `apps/mod/modrinth` because they change with the code; the release build republishes the description on every upload, so web-editor edits are overwritten. Set-once settings such as categories, license, and links are managed in the Modrinth web UI.
