
tag @s remove playing

# Always hard reset transition
execute as @s run stopsound @s music

# Executes ones (when player engages in battle)
execute as @s[tag=receiveBossMsg] unless score @s bossfight matches 2 run tellraw @s ["",{"text":"You entered a boss battle with a wither","color":"gray","italic":true}]

scoreboard players set @s bossfight 2

# Set soundtrack time
scoreboard players set @s musicLength 2920
scoreboard players operation @s musicTimer = @s[tag=!playingAmbient] musicLength