# BiomeTunes

BiomeTunes is a client-only Fabric mod that provides crossfading biome soundtracks for Minecraft.

## Project layout

- `apps/mod` — the Kotlin/Fabric client mod.
- `apps/datapack` — the legacy datapack, retained intact.
- `apps/soundpack` — the source resource pack used for standalone and embedded distribution.

## Build

```bash
./gradlew clean build packageSoundpack
```
