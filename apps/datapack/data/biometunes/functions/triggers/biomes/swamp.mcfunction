function biometunes:triggers/prebiome

# Enable intro track
execute unless score @s biome matches 6 run tellraw @s[tag=receiveBiomeMsg] ["",{"text":"You entered a swamp biome","color":"gray","italic":true}]

# Set biome variable to plains
scoreboard players set @s biome 6
scoreboard players set @s biomeAmbient 6

# Set soundtrack time
scoreboard players set @s musicLength 2740
scoreboard players operation @s[tag=!playing] musicTimer = @s[tag=!playing] musicLength

# Set ambient time
scoreboard players set @s ambientLength 0
scoreboard players operation @s[tag=!playingAmbient] ambientTimer = @s[tag=!playingAmbient] ambientLength

# Reset all advancements and music
function biometunes:triggers/reset