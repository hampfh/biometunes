function biometunes:triggers/prebiome

# Enable intro track
execute unless score @s biome matches 27 run tellraw @s[tag=receiveBiomeMsg] ["",{"text":"You entered a birch forest biome","color":"gray","italic":true}]

# Set biome variables to forest
scoreboard players set @s biome 27
scoreboard players set @s biomeAmbient 27

# Set soundtrack time
scoreboard players set @s musicLength 1640
scoreboard players operation @s[tag=!playing] musicTimer = @s[tag=!playing] musicLength

# Set ambient time
scoreboard players set @s ambientLength 580
scoreboard players operation @s[tag=!playingAmbient] ambientTimer = @s[tag=!playingAmbient] ambientLength

# Reset all advancements and music
function biometunes:triggers/reset
