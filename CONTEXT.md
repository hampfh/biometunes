# BiomeTunes

BiomeTunes selects and presents music according to the player's current Minecraft surroundings and active encounters.

## Language

**Environmental Classification**:
The interpretation of an eligible player's surroundings as one of the three environment modes. It does not apply during boss encounters or in native underground biomes.
_Avoid_: Environment system, underground system, inside detection

**Environment Mode**:
The result of environmental classification: Outside, Sheltered, or Subterranean.
_Avoid_: Level, state

**Outside**:
An environment mode for a player sufficiently exposed to the surface or open sky.

**Sheltered**:
An environment mode for a player enclosed or covered while still close to the surface or open sky.
_Avoid_: Inside, muffled mode

**Subterranean**:
An environment mode for a player sufficiently enclosed and far from the surface or open sky.
_Avoid_: Underground mode, deep mode

**Native Underground Biome**:
A biome explicitly designated as inherently underground by its environmental profile. Its configured biome soundtrack plays unchanged, without environmental classification.
_Avoid_: Cave biome

**Environmental Profile**:
Resource-pack configuration that opts a dimension into environmental classification and defines its native underground biomes, subterranean soundtrack, and classification tuning.
_Avoid_: Environment config, dimension rules

**Environmental Debug HUD**:
An optional in-game diagnostic display of the current Environmental Classification inputs, decision, and audio treatment.
_Avoid_: Debug overlay, environment HUD
