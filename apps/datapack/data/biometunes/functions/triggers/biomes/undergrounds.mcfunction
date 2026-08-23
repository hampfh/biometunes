# Always reset music when entering the end
tag @s remove playing
stopsound @s music

# Enable intro track
execute unless score @s biome matches 9 run tellraw @s[tag=receiveBiomeMsg] ["",{"text":"You entered the undergrounds","color":"gray","italic":true}]

# Set biome variable to plains
scoreboard players set @s biome 500
scoreboard players set @s biomeAmbient 500

# Set soundtrack time
scoreboard players set @s musicLength 960
scoreboard players operation @s[tag=!playing] musicTimer = @s[tag=!playing] musicLength

# Set ambient time
scoreboard players set @s ambientLength 0
scoreboard players operation @s[tag=!playingAmbient] ambientTimer = @s[tag=!playingAmbient] ambientLength