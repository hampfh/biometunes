# Changelog

All notable changes to BiomeTunes are documented in this file.

## 1.0.0 - 2026-08-22

- Rewrote BiomeTunes as a client-only Kotlin mod for Fabric and Minecraft 26.2 while retaining the legacy datapack.
- Added explicit mappings for all 66 vanilla Minecraft 26.2 biomes, with tag, dimension, and Plains fallbacks.
- Added Ender Dragon encounter music and a temporary Wither fallback to the Ender Dragon track.
- Added interruptible equal-power crossfades with a configurable duration from 0 to 15 seconds.
- Added persistent client settings for enable/disable, volume, crossfade duration, boss music, and biome/boss notifications.
- Added the `/biometunes` configuration screen and optional Mod Menu integration.
- Added client-only multiplayer support so no mod is required on the server; connection to an unmodified vanilla 26.2 server remains a pre-release human verification gate.
- Embedded the 15-track soundpack in the Fabric JAR and added a standalone soundpack distribution.
