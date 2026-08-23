
# Turn of default minecraft music
stopsound @a music minecraft:music.creative
stopsound @a music minecraft:music.credits
stopsound @a music minecraft:music.dragon
stopsound @a music minecraft:music.end
stopsound @a music minecraft:music.game
stopsound @a music minecraft:music.nether
stopsound @a music minecraft:music.under_water

# Restart music for reconnected clients
execute as @a[scores={hasLeft=1..}] run scoreboard players set @s musicTimer 0
execute as @a[scores={hasLeft=1..}] run scoreboard players set @s ambientTimer 0
execute as @a[scores={hasLeft=1..}] run scoreboard players set @s ticksSinceLogin 0
execute as @a[scores={hasLeft=1..}] run scoreboard players set @s bossfight 0
execute as @a[scores={hasLeft=1..}] run scoreboard players set @s hasLeft 0

# Check for bosses for players that have enabled it
execute as @a[tag=!noBossBattleMusic] run function biometunes:triggers/updatebosses

# Play biome specific music
execute at @a as @a[scores={bossfight=0, musicTimer=10.., biome=0},tag=!playing, tag=!underground] run playsound biometunes:music.ocean music @s ~ ~ ~ 1
execute at @a as @a[scores={bossfight=0, musicTimer=10.., biome=1},tag=!playing, tag=!underground] run playsound biometunes:music.plains music @s ~ ~ ~ 1
execute at @a as @a[scores={bossfight=0, musicTimer=10.., biome=2},tag=!playing, tag=!underground] run playsound biometunes:music.desert music @s ~ ~ ~ 1
execute at @a as @a[scores={bossfight=0, musicTimer=10.., biome=3},tag=!playing, tag=!underground] run playsound biometunes:music.mountains music @s ~ ~ ~ 1
execute at @a as @a[scores={bossfight=0, musicTimer=10.., biome=4},tag=!playing, tag=!underground] run playsound biometunes:music.forest music @s ~ ~ ~ 1
execute at @a as @a[scores={bossfight=0, musicTimer=10.., biome=5},tag=!playing, tag=!underground] run playsound biometunes:music.taiga music @s ~ ~ ~ 1
execute at @a as @a[scores={bossfight=0, musicTimer=10.., biome=6},tag=!playing, tag=!underground] run playsound biometunes:music.swamp music @s ~ ~ ~ 1
execute at @a as @a[scores={bossfight=0, musicTimer=10.., biome=9},tag=!playing, tag=!underground] run playsound biometunes:music.the_end music @s ~ ~ ~ 1
execute at @a as @a[scores={bossfight=0, musicTimer=10.., biome=10},tag=!playing, tag=!underground] run playsound biometunes:music.snowy_ocean music @s ~ ~ ~ 1
execute at @a as @a[scores={bossfight=0, musicTimer=10.., biome=12},tag=!playing, tag=!underground] run playsound biometunes:music.snowy music @s ~ ~ ~ 1
execute at @a as @a[scores={bossfight=0, musicTimer=10.., biome=14},tag=!playing, tag=!underground] run playsound biometunes:music.mushroom_fields music @s ~ ~ ~ 1
execute at @a as @a[scores={bossfight=0, musicTimer=10.., biome=21},tag=!playing, tag=!underground] run playsound biometunes:music.jungle music @s ~ ~ ~ 1
execute at @a as @a[scores={bossfight=0, musicTimer=10.., biome=27},tag=!playing, tag=!underground] run playsound biometunes:music.birch_forest music @s ~ ~ ~ 1
execute at @a as @a[scores={bossfight=0, musicTimer=10.., biome=29},tag=!playing, tag=!underground] run playsound biometunes:music.dark_forest music @s ~ ~ ~ 1
execute at @a as @a[scores={bossfight=0, musicTimer=10.., biome=35},tag=!playing, tag=!underground] run playsound biometunes:music.savanna music @s ~ ~ ~ 1
execute at @a as @a[scores={bossfight=0, musicTimer=10.., biome=44},tag=!playing, tag=!underground] run playsound biometunes:music.warm_ocean music @s ~ ~ ~ 1
execute at @a as @a[scores={bossfight=0, musicTimer=10.., biome=132},tag=!playing, tag=!underground] run playsound biometunes:music.flower_forest music @s ~ ~ ~ 1

# Nether biomes
execute at @a as @a[scores={bossfight=0, musicTimer=10.., biome=8},tag=!playing, tag=!underground] run playsound biometunes:music.nether_wastes music @s ~ ~ ~ 1
execute at @a as @a[scores={bossfight=0, musicTimer=10.., biome=171},tag=!playing, tag=!underground] run playsound biometunes:music.crimson_forest music @s ~ ~ ~ 1
execute at @a as @a[scores={bossfight=0, musicTimer=10.., biome=172},tag=!playing, tag=!underground] run playsound biometunes:music.warped_forest music @s ~ ~ ~ 1
execute at @a as @a[scores={bossfight=0, musicTimer=10.., biome=173},tag=!playing, tag=!underground] run playsound biometunes:music.basalt_deltas music @s ~ ~ ~ 1
execute at @a as @a[scores={bossfight=0, musicTimer=10.., biome=178},tag=!playing, tag=!underground] run playsound biometunes:music.soul_sand_valley music @s ~ ~ ~ 1

# Play custom biome music
execute at @a as @a[scores={bossfight=0, musicTimer=10.., biome=500},tag=!playing] run playsound biometunes:music.undergrounds music @s ~ ~ ~ 1

# Play boss music
execute at @a as @a[scores={bossfight=1, musicTimer=10..},tag=!playing] run playsound biometunes:music.ender_dragon music @s ~ ~ ~ 1
execute at @a as @a[scores={bossfight=2, musicTimer=10..},tag=!playing] run playsound biometunes:music.wither music @s ~ ~ ~ 1

# Ambient tracks
execute at @a as @a[scores={ambientTimer=10.., biomeAmbient=4},tag=!playingAmbient] run playsound biometunes:ambient.forest ambient @s ~ ~ ~ 1000 1 1
execute at @a as @a[scores={ambientTimer=10.., biomeAmbient=16},tag=!playingAmbient] run playsound biometunes:ambient.ambient_beach ambient @s ~ ~ ~ 1

# Add playing tag to all
execute as @a[tag=!playing] run tag @s add playing
execute as @a[tag=!playingAmbient] run tag @s add playingAmbient

# Decrease soundtrack timer
execute as @a[tag=playing, scores={musicTimer=1..}] run scoreboard players remove @s musicTimer 1
# Decrease ambient timer
execute as @a[tag=playingAmbient, scores={ambientTimer=1..}] run scoreboard players remove @s ambientTimer 1

# Increase login timer
execute as @a run scoreboard players add @s ticksSinceLogin 1

# Resetting music
execute as @a[tag=playing, scores={musicTimer=0}] run stopsound @s music
execute as @a[tag=playing, scores={musicTimer=0}] run tag @s remove playing
# Reloop biome music
execute as @a[tag=!playing, scores={musicTimer=0}] run scoreboard players operation @s musicTimer = @s musicLength

# Resetting ambient
execute as @a[tag=playingAmbient, scores={ambientTimer=0}] run stopsound @s ambient
execute as @a[tag=playingAmbient, scores={ambientTimer=0}] run tag @s remove playingAmbient
# Reloop ambient sounds
execute as @a[tag=!playingAmbient, scores={ambientTimer=0}] run scoreboard players operation @s ambientTimer = @s ambientLength