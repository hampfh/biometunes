
# Initializing variables

scoreboard objectives add hasLeft minecraft.custom:minecraft.leave_game
#scoreboard objectives add Instantiated dummy "Biometunes datapack variables" Setup in minecrat:load
scoreboard objectives add posY dummy "Y Position of each player"
scoreboard objectives add ticksSinceLogin dummy "Ticks since player login"
scoreboard objectives add version dummy "Version segment"

# Set version number
scoreboard players set #bt_version_major version 0
scoreboard players set #bt_version_minor version 3
scoreboard players set #bt_version_patch version 3

# Sound track variables
scoreboard objectives add biome dummy "Current biome"
scoreboard objectives add musicTimer dummy "Music timer"
scoreboard objectives add bossfight dummy "Bosses close to players"
scoreboard objectives add musicLength dummy "Length of music track"

# Trigger variables
scoreboard objectives add biomeTunes trigger "Trigger Control panel"
scoreboard objectives add opt1 trigger "Trigger for biome transition mode"
scoreboard objectives add opt2 trigger "Trigger for biome shift message"
scoreboard objectives add opt3 trigger "Trigger for boss battle message"
scoreboard objectives add opt4 trigger "Trigger for boss battle"

# Ambient variables
scoreboard objectives add biomeAmbient dummy "Current biome ambient"
scoreboard objectives add ambientTimer dummy "Ambient timer"
scoreboard objectives add ambientLength dummy "Length of ambient sound"

# In order to not spam chat when players are triggering menu panel
# gamerule sendCommandFeedback false

# Reseting players
scoreboard players reset * biome 
scoreboard players reset * hasLeft 

# Reset all advancements and music
function biometunes:triggers/reset

# Pack is used for "long term storage"
scoreboard players set #pack Instantiated 1
# Check is reset every time the world reloads and is used in the code
scoreboard players set #check Instantiated 1

tellraw @a ["",{"text":"Biometunes","bold":true,"color":"dark_green"},{"text":"\n"},{"text":"Visit our github page: ","bold":true,"color":"dark_green"},{"text":"https://github.com/biometunes/data-pack","italic":true,"underlined":true,"color":"gold","clickEvent":{"action":"open_url","value":"https://github.com/biometunes/data-pack"},"hoverEvent":{"action":"show_text","contents":{"text":"Go to site","color":"green"}}},{"text":"\n"},{"text":"Datapack successfully installed!","color":"green"},{"text":"\n"},{"text":"Current version: ","color":"green"},{"text":"v.","bold":true,"color":"green"},{"score":{"name":"#bt_version_major","objective":"version"},"bold":true,"color":"green"},{"text":".","bold":true,"color":"green"},{"score":{"name":"#bt_version_minor","objective":"version"},"bold":true,"color":"green"},{"text":".","bold":true,"color":"green"},{"score":{"name":"#bt_version_patch","objective":"version"},"bold":true,"color":"green"},{"text":"\n"},{"text":"To open your own control panel type ","color":"green"},{"text":"/trigger biomeTunes","bold":true,"color":"green","clickEvent":{"action":"run_command","value":"/trigger biomeTunes"},"hoverEvent":{"action":"show_text","contents":{"text":"Open control panel","color":"green"}}},{"text":"\n"},{"text":"Important:","bold":true,"color":"red"},{"text":" You are now required to relog. The music will start playing when you join...","color":"red"}]

# {"jformat":6,"jobject":[{"bold":true,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_green","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"a46acf17-3fc7-4e68-9725-9ac2eb780555","text":"Biometunes"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"dc6e0928-35e0-4458-93b6-aa7f714d0128","text":"\n"},{"bold":true,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_green","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"c3c0e818-7323-4fce-88af-bdbd17350a65","text":"Visit our github page: "},{"bold":false,"italic":true,"underlined":true,"strikethrough":false,"obfuscated":false,"font":null,"color":"gold","insertion":"","click_event_type":"1","click_event_value":"https://github.com/biometunes/data-pack","hover_event_type":"1","hover_event_value":"","hover_event_object":{},"hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"green","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"8bc3db1c-a5f7-46b3-b7ae-3e53b9f7b72a","text":"Go to site"}],"id":"cbe3b491-aa26-4e2a-814a-77296f46ba2c","text":"https://github.com/biometunes/data-pack"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"0dbb8624-2e0f-4ec1-861e-72b4a6cd09a7","text":"\n"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"green","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"c49bf55f-b5b4-40be-938f-19ae049e143a","text":"Datapack successfully installed!"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"d151db32-78fc-4d79-a14e-f586154f541b","text":"\n"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"green","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"614692e4-754d-4122-a1ce-6855ed087e76","text":"Current version: "},{"bold":true,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"green","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"064e63fe-5863-4d23-bab2-61d81a6627e4","text":"v."},{"bold":true,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"green","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"8272934e-a818-42ee-b458-4eef72fcc8d4","score_name":"#bt_version_major","score_objective":"version","score_value":null},{"bold":true,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"green","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"5efa7a51-e4d3-4040-b742-60051afa6eff","text":"."},{"bold":true,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"green","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"d6f37fe4-2f5e-40a3-8b3c-6a1c3376a8cf","score_name":"#bt_version_minor","score_objective":"version","score_value":null},{"bold":true,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"green","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"468a421b-6b92-4342-b32b-4499bc1a1775","text":"."},{"bold":true,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"green","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"4b8a8ef3-908c-4c37-b2be-f8721551b5d1","score_name":"#bt_version_patch","score_objective":"version","score_value":null},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"1c5677d5-faba-48f6-b481-24d6e8cbf6ce","text":"\n"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"green","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"208288da-009e-487c-99f6-7a4e8a263e6a","text":"To open your own control panel type "},{"bold":true,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"green","insertion":"","click_event_type":"2","click_event_value":"/trigger biomeTunes","hover_event_type":"1","hover_event_value":"","hover_event_object":{},"hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"green","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"76e18e50-f7af-4128-af60-5f7db6737ff6","text":"Open control panel"}],"id":"cb468282-7ae1-4e46-aad5-739977c77f79","text":"/trigger biomeTunes"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"16dfcbb6-c2ec-4e30-be26-420cd68e732d","text":"\n"},{"bold":true,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"red","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"e12dd4f5-7aea-4fca-afbb-ba5835e67ccb","text":"Important:"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"red","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"69d09dae-d2e6-40e7-8bc5-8fccf592b036","text":" You are now required to relog. The music will start playing when you join..."}],"command":"/tellraw @s %s","jtemplate":"tellraw"}

# Boss fights
# 0 No boss
# 1 Ender dragon
# 2 Wither
# 3 Elder guardian

# Biome index https://minecraft.gamepedia.com/Biome/ID

# Active biomes
# 0 Ocean 
# 1 Plains 
# 2 Desert 
# 3 Mountains
# 4 Forest 
# 5 Taiga 
# 6 swamp
# 8 nether_wastes
# 9 the_end
# 10 frozen_ocean -> snowy_ocean (#10)
# 11 frozen_river -> snowy_ocean (#10)
# 12 snowy_tundra -> snowy (#12)
# 13 snowy_mountain -> snowy (#12)
# 14 mushroom_fields
# 15 mushroom_fields_shore -> mushroom_fields
# 16 beach
# 17 desert_hills -> desert
# 18 wooded_hills -> forest
# 19 taiga_hills -> taiga
# 20 mountain_edge -> mountains
# 22 jungle_hills -> jungle
# 23 jungle_edge -> jungle
# 24 deep_ocean -> ocean
# 27 birch_forest
# 28 birch_forest_hills -> birch_forest
# 29 dark_forest
# 30 snowy_taiga -> snowy (#12)
# 31 snowy_taiga_hills -> snowy (#12)
# 32 giant_tree_taiga -> taiga
# 33 giant_tree_taiga_hills -> taiga
# 34 wooded_mountains -> mountains
# 35 savanna
# 36 savanna_plateau -> savanna
# 40 small_end_islands -> the_end
# 41 end_midlands -> the_end
# 42 end_highlands -> the_end
# 43 end_barrens -> the_end
# 44 warm_ocean
# 45 lukewarm_ocean -> warm_ocean
# 46 cold_ocean -> ocean
# 47 deep_warm_ocean -> warm_ocean
# 48 deep_lukewarm_ocean -> warm_ocean
# 49 deep_cold_ocean -> ocean
# 50 deep_frozen_ocean -> snowy_ocean (#10)
# 129 sunflower_plains -> plains
# 130 desert_lakes -> desert
# 131 gravelly_mountains -> mountains
# 132 flower_forest -> forest
# 133 taiga_mountains -> taiga
# 134 swamp_hills -> swamp
# 140 ice_spikes -> snowy (#12)
# 149 modified_jungle -> jungle
# 151 modified_jungle_edge -> jungle
# 155 tall_birch_forest -> birch_forest
# 156 tall_birch_hills -> birch_forest
# 157 dark_forest_hills -> dark_forest
# 158 snowy_taiga_mountains -> snowy (#12)
# 160 giant_spruce_taiga -> taiga
# 161 giant_spruce_taiga_hills -> taiga
# 162 modified_gravelly_mountains -> mountains
# 163 shattered_savanna -> savanna
# 164 shattered_savanna_plateau -> savanna
# 165 eroded_badlands -> mountains
# 168 bamboo_jungle -> jungle
# 169 bamboo_jungle_hills -> jungle
# 171 crimson_forest
# 172 warped_forest
# 173 basalt_deltas
# 178 soul_sand_valley

# Custom biomes
# 500 underground