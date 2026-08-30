BiomeTunes 0.6 makes soundtrack selection weighted and adds intentional silence between songs.

- Added six original recordings: Frozen Ocean, Mountains 2, Nether Wastes, Swamp, Taiga, and Underground.
- Added weighted song pools for biomes, bosses, biome tags, dimensions, and fallback music.
- Added global silence defaults with per-pool overrides and random duration ranges.
- Bundled single-song pools choose music 75% of the time and silence 25% of the time; two-song mountain pools choose music 6/7 of the time.
- Boss pools use zero-weight silence entries, so boss music never selects silence.
- Reroll the active pool whenever a track or silence interval ends.
