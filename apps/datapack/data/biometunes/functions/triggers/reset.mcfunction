
# Add playing tag to all
#execute as @a[tag=!playing] run tag @s add playing

# Remove advancement
execute unless score @s biome matches 0 run advancement revoke @s only biometunes:biomes/ocean
execute unless score @s biome matches 1 run advancement revoke @s only biometunes:biomes/plains
execute unless score @s biome matches 2 run advancement revoke @s only biometunes:biomes/desert
execute unless score @s biome matches 3 run advancement revoke @s only biometunes:biomes/mountains
execute unless score @s biome matches 4 run advancement revoke @s only biometunes:biomes/forest
execute unless score @s biome matches 5 run advancement revoke @s only biometunes:biomes/taiga
execute unless score @s biome matches 6 run advancement revoke @s only biometunes:biomes/swamp
execute unless score @s biome matches 8 run advancement revoke @s only biometunes:biomes/nether/nether_wastes
execute unless score @s biome matches 9 run advancement revoke @s only biometunes:biomes/the_end
execute unless score @s biome matches 10 run advancement revoke @s only biometunes:biomes/snowy_ocean
execute unless score @s biome matches 12 run advancement revoke @s only biometunes:biomes/snowy
execute unless score @s biome matches 14 run advancement revoke @s only biometunes:biomes/mushroom_fields
execute unless score @s biome matches 21 run advancement revoke @s only biometunes:biomes/jungle
execute unless score @s biome matches 27 run advancement revoke @s only biometunes:biomes/birch_forest
execute unless score @s biome matches 29 run advancement revoke @s only biometunes:biomes/dark_forest
execute unless score @s biome matches 35 run advancement revoke @s only biometunes:biomes/savanna
execute unless score @s biome matches 44 run advancement revoke @s only biometunes:biomes/warm_ocean
execute unless score @s biome matches 132 run advancement revoke @s only biometunes:biomes/flower_forest
execute unless score @s biome matches 171 run advancement revoke @s only biometunes:biomes/nether/crimson_forest
execute unless score @s biome matches 172 run advancement revoke @s only biometunes:biomes/nether/warped_forest
execute unless score @s biome matches 173 run advancement revoke @s only biometunes:biomes/nether/basalt_deltas
execute unless score @s biome matches 178 run advancement revoke @s only biometunes:biomes/nether/sould_sand_valley

execute unless score @s biomeAmbient matches 16 run advancement revoke @s only biometunes:biomes/beach
