
tag @a[tag=underground] add undergroundCycle
# Clear underground tag
tag @a remove underground

# Update player Y positions
execute as @a store result score @s posY run data get entity @s Pos[1]

# Give tag to all under y level 45
execute as @a[scores={posY=..45}] run tag @s add REQUIREMENT_LOW

# summon an armour stand to compare position with
execute as @a[tag=REQUIREMENT_LOW] at @s run summon minecraft:armor_stand ~ ~ ~ {CustomName:"{\"text\": \"checkIfCave\"}", CustomNameVisible:0b, Invisible:1b, Invulnerable:1b, Marker:1b}
# (spreadplayer command always places the target on the highest block)
execute at @a[tag=REQUIREMENT_LOW] run spreadplayers ~ ~ 0 1 false @e[type=minecraft:armor_stand, distance=..2, name=checkIfCave]

# Give all messure armour stand a y position
execute as @e[type=minecraft:armor_stand, name=checkIfCave] at @s store result score @s posY run data get entity @s Pos[1]

# Check if armor stand y positions matches players
execute as @a[tag=REQUIREMENT_LOW] unless score @s posY = @e[type=minecraft:armor_stand, sort=nearest, limit=1, name=checkIfCave] posY run tag @s add underground

# Clear armor stand and reset tags
execute as @e[type=minecraft:armor_stand, name=checkIfCave] run scoreboard players reset @s posY
kill @e[type=minecraft:armor_stand, name=checkIfCave]
tag @a remove REQUIREMENT_LOW

execute as @a[tag=underground, tag=!undergroundCycle] run function biometunes:triggers/biomes/undergrounds

# Remove loop check
tag @a remove undergroundCycle

# Reset back to ground level biomes
execute as @a[tag=!underground] run function biometunes:triggers/reset

# Reloop after 4 seconds
schedule function biometunes:triggers/updateunderground 4s