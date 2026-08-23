# Introduce variable to keep track if the pack has been instantiated or not
scoreboard objectives add Instantiated dummy "Biometunes initialization variable"
scoreboard players set #check Instantiated 0
# If pack is already instantiated then change check to 1
execute if score #pack Instantiated matches 1.. run scoreboard players set #check Instantiated 1
