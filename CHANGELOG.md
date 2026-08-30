# Changelog

All notable changes to BiomeTunes are documented in this file.

## 0.6 - 2026-08-30

- Added weighted song pools for biomes, bosses, biome tags, dimensions, and fallback music.
- Added weighted intentional silence with global defaults, per-pool overrides, and random duration ranges.
- Added zero-weight pool entries, used by the bundled boss pools to opt out of global silence.
- Reroll the active pool whenever a track ends or a silence interval expires.
- Added Frozen Ocean, Mountains 2, Nether Wastes, Swamp, Taiga, and Underground recordings, bringing the bundled soundtrack to 21 tracks.
- Added Environmental Classification with Outside, Sheltered, and Subterranean Environment Modes in resource-pack-configured dimensions.
- Added a bounded Overworld classifier that never loads chunks, preserves Native Underground Biome and boss pools, and routes generic deep caverns to the Underground pool.
- Added strong, smooth Sheltered gain, OpenAL EFX low-pass treatment, and a shared short dark-room reverb with a gain-only fallback.
- Reapply changing low-pass values to active OpenAL voices so the audible filter follows its smoothing envelope.
- Preserve each outgoing voice's Sheltered treatment during crossfades and intentional-silence fades, preventing it from becoming louder as Subterranean begins.
- Added an opt-in Environmental Debug HUD showing cached evidence, scores, decisions, treatment, low-pass status, and reverb status without additional world sampling.

## 0.5 - 2026-08-23

- Rewrote BiomeTunes as a client-only Kotlin mod for Fabric and Minecraft 26.2 while retaining the legacy datapack.
- Added explicit mappings for all 66 vanilla Minecraft 26.2 biomes, with tag, dimension, and Plains fallbacks.
- Added Ender Dragon encounter music and a temporary Wither fallback to the Ender Dragon track.
- Added interruptible equal-power crossfades with a configurable duration from 0 to 15 seconds.
- Crossfades hold their curve through interruptions, lost audio handles, pauses, and duration changes instead of cutting to the new track.
- Added persistent client settings for enable/disable, volume, crossfade duration, boss music, and biome/boss notifications.
- Added the `/biometunes` configuration screen and optional Mod Menu integration.
- Added client-only multiplayer support so no mod is required on the server; connection to an unmodified vanilla 26.2 server remains a pre-release human verification gate.
- Embedded the 15-track soundpack in the Fabric JAR and added a standalone soundpack distribution.
