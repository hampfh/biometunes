function biometunes:triggers/prebiome

# Always reset music when entering the end
tag @s remove playing

# Enable intro track
execute unless score @s biome matches 9 run tellraw @s[tag=receiveBiomeMsg] ["",{"text":"You entered the end","color":"gray","italic":true}]

# Set biome variable to the end
scoreboard players set @s biome 9
scoreboard players set @s biomeAmbient 9

# Set soundtrack time
scoreboard players set @s musicLength 1920
scoreboard players operation @s[tag=!playing] musicTimer = @s[tag=!playing] musicLength

# Set ambient time
scoreboard players set @s ambientLength 0
scoreboard players operation @s[tag=!playingAmbient] ambientTimer = @s[tag=!playingAmbient] ambientLength

# Reset all advancements and music
function biometunes:triggers/reset