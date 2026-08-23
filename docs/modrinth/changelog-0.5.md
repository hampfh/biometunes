First public release of the rewritten BiomeTunes — a client-side Fabric mod for Minecraft 26.2, replacing the original datapack + resource pack pair.

**What's in it**

- Biome-aware music covering all **66 vanilla 26.2 biomes**, with closest-match, biome-tag, dimension, and Plains fallbacks so unknown or modded biomes still get a track.
- **Equal-power crossfades** between tracks, configurable from 0 to 15 seconds (default 5). Transitions are interruptible — rapid biome changes retarget the current fade instead of stacking tracks.
- **Ender Dragon** boss music, with a toggle.
- Settings screen via `/biometunes`, plus optional **Mod Menu** integration. Saved to `config/biometunes.json`.
- **Client-side only** — no server installation, works on unmodified vanilla servers.
- The 15-track soundtrack is **bundled in the JAR**; no separate resource pack needed.
- Vanilla scheduled music is suppressed only while BiomeTunes is active. Music discs, menus, and credits are untouched.

**Known limitations**

- The Wither temporarily shares the Ender Dragon track.
- Beach biomes map to the Ocean track.
- The composer of the Forest track is unrecorded; see the project description.

Requires [Fabric API](https://modrinth.com/mod/fabric-api) and [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin) on Minecraft 26.2 with Java 25.
