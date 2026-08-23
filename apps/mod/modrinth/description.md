# BiomeTunes

**Every biome gets its own song.**

Vanilla Minecraft plays a handful of tracks on a random timer that mostly ignores where you actually are. BiomeTunes replaces that with an original soundtrack chosen from your current biome, dimension, or boss fight — and **crossfades** between tracks instead of cutting one off to start the next.

Walk from plains into a dark forest and the music follows you across a smooth, equal-power fade. Turn around halfway through and it fades back, without stacking a third track on top.

## Features

- **Biome-aware soundtrack.** All 66 vanilla Minecraft 26.2 biomes are mapped. Biomes with no dedicated recording fall back to their closest thematic match, then to a biome tag, then to a dimension default, and finally to Plains — so you are never left in silence, including in modded or server-provided biomes.
- **Real crossfades.** Two tracks are mixed simultaneously along an equal-power curve, so there is no volume dip at the midpoint. Configurable from 0 seconds (instant switch) up to 15 seconds; the default is 5.
- **Interruptible transitions.** Sprinting through five biomes never stacks five songs. At most two tracks are ever audible, and a new transition retargets the one in progress.
- **Boss music.** The Ender Dragon fight gets its own track, and boss music can be turned off entirely.
- **Client-side only.** Nothing is installed on the server. BiomeTunes works in single-player and when you connect to ordinary, unmodified vanilla servers.
- **In-game settings.** The `/biometunes` command opens a settings screen. Mod Menu is supported but not required.
- **Stays out of the way.** BiomeTunes suppresses only vanilla's scheduled background music, and only while it is enabled and you are in a world. Music discs, menus, and the credits are untouched, and vanilla scheduling resumes the moment you disable it.

## Requirements

| | |
|---|---|
| Minecraft | Java Edition 26.2 |
| Java | 25 or newer |
| Mod loader | [Fabric Loader](https://fabricmc.net/use/installer/) 0.19.3 or newer |
| Required | [Fabric API](https://modrinth.com/mod/fabric-api), [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin) |
| Optional | [Mod Menu](https://modrinth.com/mod/modmenu) 20.0.1 or newer |
| Server | Nothing — this is a client-side mod |

## Installation

1. Install Fabric Loader for Minecraft 26.2.
2. Put **Fabric API** and **Fabric Language Kotlin** in your `mods` folder.
3. Put the BiomeTunes `.jar` in the same `mods` folder.
4. Launch the game and run `/biometunes` to open the settings screen.

The soundtrack is bundled inside the JAR. There is **no separate resource pack to install**.

## Settings

| Setting | Default | Range |
|---|---|---|
| Enabled | On | On / Off |
| Volume | 100% | 0–100% |
| Crossfade | 5 seconds | 0–15 seconds |
| Boss music | On | On / Off |
| Biome notifications | Off | On / Off |
| Boss notifications | Off | On / Off |

Settings are saved to `config/biometunes.json` and apply immediately.

## The soundtrack

Fifteen original tracks, written for this project:

| Track | Composer |
|---|---|
| Plains | Abraham Frato (Reklawer) |
| Forest | Abraham Frato (Reklawer) |
| Birch Forest | Abraham Frato (Reklawer) |
| Dark Forest | Abraham Frato (Reklawer) |
| Flower Forest | Edit Lundström (Edito1) |
| Jungle | Zacharias Frato (Taraneas) |
| Savanna | Zacharias Frato (Taraneas) |
| Desert | Martin Ryberg Laude (Mar01) |
| Snowy | Abraham Frato (Reklawer) |
| Mountains | Abraham Frato (Reklawer) |
| Ocean | Martin Ryberg Laude (Mar01) |
| Warm Ocean | Arman Aspromonti |
| Snowy Ocean | Abraham Frato (Reklawer) |
| The End | Abraham Frato (Reklawer) |
| Ender Dragon | Zacharias Frato (Taraneas) |

Pack development by Hampus Hallkvist ([Hampfh](https://www.hampushallkvist.com)).

## Beta status

This is the first public release of the rewritten mod. It is functionally complete but has not yet had a wide audience, so a few things are worth knowing:

- **The Wither temporarily uses the Ender Dragon track.** There is no separate Wither recording yet.
- **Beach biomes use the Ocean track.** The legacy pack's beach recording is not part of this release.
- **Several biome families share a track.** Fifteen recordings cover 66 biomes, so mappings are closest-match rather than one-per-biome. Taiga, swamp, mushroom fields, the Nether, and the deep underground all borrow their nearest neighbour.

Bug reports and mapping suggestions are welcome on the [issue tracker](https://github.com/BiomeTunes/datapack/issues).

## Legacy datapack

BiomeTunes started life as a vanilla datapack plus a separate resource pack. That version is preserved in the [repository](https://github.com/BiomeTunes/datapack) under `apps/datapack` for historical interest. It targets a much older Minecraft version and is **not** supported by this release; the mod is the supported way to use BiomeTunes.

## License

BiomeTunes is released under the [Apache License 2.0](https://github.com/BiomeTunes/datapack/blob/master/LICENSE).
