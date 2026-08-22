# BiomeTunes

BiomeTunes is a client-only Fabric mod that gives Minecraft 26.2 a soundtrack selected from the player's current biome, dimension, or boss encounter. Version 1.0.0 covers all 66 vanilla 26.2 biomes, crossfades between its 15 included tracks, pauses scheduled vanilla music while it owns playback, and returns control to vanilla music when disabled or stopped. The Wither temporarily uses the Ender Dragon track because the imported soundpack has no separate Wither recording.

## Requirements and installation

- Minecraft Java Edition 26.2 and Java 25.
- [Fabric Loader](https://fabricmc.net/use/installer/) 0.19.3 or newer.
- [Fabric API](https://modrinth.com/mod/fabric-api) for Minecraft 26.2 (the development build uses `0.158.0+26.2`).
- [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin) `1.13.13+kotlin.2.4.10` or newer.
- Optional: [Mod Menu](https://modrinth.com/mod/modmenu) 20.0.1 or newer for a graphical settings entry.

Install the Fabric loader and required dependencies on the client, then place `biometunes-1.0.0.jar` in the client's `mods` directory. The soundtrack is embedded in the mod JAR, so mod users do not install the standalone soundpack ZIP.

BiomeTunes works in single-player and is client-only by design: it requires no server installation and is intended to connect to an unmodified vanilla Minecraft 26.2 server. A real vanilla-server connection remains a required human pre-release check; see the [manual audio checklist](docs/testing/manual-audio-checklist.md).

## Settings

Run the client-side `/biometunes` command to open the settings screen. If Mod Menu is installed, open **Mods**, select **BiomeTunes**, and use its configuration button. The screen controls whether BiomeTunes is enabled, master volume, crossfade duration from 0 to 15 seconds, biome notifications, boss music, and boss notifications. Settings are stored in the client configuration directory as `biometunes.json`.

## Repository layout

- `apps/mod` — the Kotlin/Fabric client mod, automated tests, and Modrinth publication configuration.
- `apps/soundpack` — the canonical audio/resource source copied into the mod JAR and packaged as a standalone ZIP.
- `apps/datapack` — the original datapack implementation, retained for legacy users.
- `docs/testing` — the dated pre-release manual verification record.
- `.github/workflows` — Java 25 build CI and tag-driven release automation.

## Development

Use a Java 25 JDK. The Gradle wrapper provides the rest of the build toolchain.

```bash
./gradlew build
./gradlew test
./gradlew runClient
```

For a clean release-equivalent build of both distributions, run:

```bash
./gradlew clean build packageSoundpack
```

The Fabric artifact is `apps/mod/build/libs/biometunes-1.0.0.jar`, produced by `:apps:mod:jar`. The standalone soundpack is `build/distributions/biometunes-soundpack-1.0.0.zip`, produced by `packageSoundpack`.

The release workflow only runs for `v*` tags and rejects a tag whose version differs from `mod_version` in `gradle.properties`. It is also fail-closed on the repository Actions variable `RELEASE_APPROVED_TAG`: a missing, empty, or non-matching value stops the job before the build and all publication steps. A tag alone never authorizes publication.

Only after all 17 manual checklist rows pass, the Forest attribution wording is confirmed, Modrinth's client environment is **required** and server environment is **unsupported**, publication credentials exist, and explicit release authorization is given may an authorized maintainer set `RELEASE_APPROVED_TAG` to the exact tag name (for example, `v1.0.0`). Remove or unset the variable after that release completes. Ordinary builds and tests do not need publishing credentials; an actual Modrinth upload requires repository secrets named exactly `MODRINTH_TOKEN` and `MODRINTH_PROJECT_ID`.

## Soundtrack provenance and credits

The soundpack was imported from [`BiomeTunes/soundpack`](https://github.com/BiomeTunes/soundpack) at exact commit `e45e65833b2808d53050eccf2bd3f47ad35a397c` and adapted to the `biometunes` namespace. Credits retained from that source are:

- Hampus Hallkvist (Hampfh) — pack developer.
- Abraham Frato (Reklawer) — Plains, The End, Mountains, Dark Forest, Birch Forest, Snowy, and Snowy Ocean.
- Martin Ryberg Laude (Mar01) — Desert and Ocean.
- Zacharias Frato (Taraneas) — Savanna, Ender Dragon boss fight, Beach (ambient), and Jungle. Version 1.0.0 has no separate Beach OGG and maps beach biomes to Ocean; the Beach credit is retained from the legacy source README.
- Arman Aspromonti — Warm Ocean.
- Edit Lundström (Edito1) — Flower Forest.

The imported commit contains `forest.ogg`, but its README does not identify that track's artist. Its current label, **“Legacy soundpack — artist not recorded,” is not a confirmed attribution**. The artist and final wording must be confirmed by a human before release. Full source links and track-by-track credits are in the [soundpack README](apps/soundpack/README.md).

## License

BiomeTunes is licensed under the [Apache License 2.0](LICENSE).
