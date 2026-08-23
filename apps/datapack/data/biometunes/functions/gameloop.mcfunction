# Call core
execute if score #check Instantiated matches 1.. run function biometunes:core
execute if score #check Instantiated matches 0 run title @a actionbar ["",{"text":"Run ","color":"dark_green"},{"text":"/function #biometunes:setup","bold":true,"color":"green"},{"text":" to setup the pack","color":"dark_green"}]

# Manage options
function biometunes:options