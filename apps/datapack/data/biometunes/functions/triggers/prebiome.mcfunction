# Reset music if player switches dimension
execute if score @s biome matches 8..9 run tag @s remove playing
execute if score @s biome matches 8..9 run stopsound @s music

# Hard reset music if not smooth transition
execute as @s[tag=!smoothTransition] run tag @s remove playing
execute as @s[tag=!smoothTransition] run stopsound @s music

# Hard reset ambient if not smooth transition
execute as @s[tag=!smoothTransition] run tag @s remove ambientTimer
execute as @s[tag=!smoothTransition] run stopsound @s ambient