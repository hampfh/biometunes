function biometunes:triggers/prebiome

# Only executes when entering biome
execute unless score @s biome matches 29 run tellraw @s[tag=receiveBiomeMsg] ["",{"text":"You entered a dark forest biome","color":"gray","italic":true}]

# Set biome variables to plains
scoreboard players set @s biome 29
scoreboard players set @s biomeAmbient 29

# Set soundtrack time
scoreboard players set @s musicLength 1780
scoreboard players operation @s[tag=!playing] musicTimer = @s[tag=!playing] musicLength

# Set ambient time
scoreboard players set @s ambientLength 0
scoreboard players operation @s[tag=!playingAmbient] ambientTimer = @s[tag=!playingAmbient] ambientLength

# Reset all advancements and music
function biometunes:triggers/reset