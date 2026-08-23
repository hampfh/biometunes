function biometunes:triggers/prebiome

# Enable intro track
execute unless score @s biome matches 172 run tellraw @s[tag=receiveBiomeMsg] ["",{"text":"You entered a warped forest biome","color":"gray","italic":true}]

# Set biome variables to forest
scoreboard players set @s biome 172
scoreboard players set @s biomeAmbient 172

# Set soundtrack time
scoreboard players set @s musicLength 10000
scoreboard players operation @s[tag=!playing] musicTimer = @s[tag=!playing] musicLength

# Set ambient time
scoreboard players set @s ambientLength 10000
scoreboard players operation @s[tag=!playingAmbient] ambientTimer = @s[tag=!playingAmbient] ambientLength

# Reset all advancements and music
function biometunes:triggers/reset
