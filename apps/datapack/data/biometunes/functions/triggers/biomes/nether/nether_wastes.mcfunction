function biometunes:triggers/prebiome

# Enable intro track
execute unless score @s biome matches 8 run tellraw @s[tag=receiveBiomeMsg] ["",{"text":"You entered the nether wastes","color":"gray","italic":true}]

# Set biome variables to forest
scoreboard players set @s biome 8
scoreboard players set @s biomeAmbient 8

# Set soundtrack time
scoreboard players set @s musicLength 2440
scoreboard players operation @s[tag=!playing] musicTimer = @s[tag=!playing] musicLength

# Set ambient time
scoreboard players set @s ambientLength 10000
scoreboard players operation @s[tag=!playingAmbient] ambientTimer = @s[tag=!playingAmbient] ambientLength

# Reset all advancements and music
function biometunes:triggers/reset
