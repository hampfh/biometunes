scoreboard objectives remove hasLeft
scoreboard objectives remove posY

scoreboard objectives remove biome 
scoreboard objectives remove biomeAmbient
scoreboard objectives remove biomeTunes
scoreboard objectives remove bossfight
scoreboard objectives remove opt1
scoreboard objectives remove opt2
scoreboard objectives remove opt3
scoreboard objectives remove opt4
scoreboard objectives remove ambientTimer
scoreboard objectives remove musicTimer 
scoreboard objectives remove musicLength
scoreboard objectives remove ambientLength
scoreboard objectives remove ticksSinceLogin

tag @a remove playing 
tag @a remove playingAmbient
tag @a remove receiveBiomeMsg
tag @a remove receiveBossMsg
tag @a remove smoothTransition 
tag @a remove biomeTunesModerator
tag @a remove underground
tag @a remove noBossBattleMusic

tellraw @s ["",{"text":"Biometunes v.","bold":true,"color":"dark_green"},{"score":{"name":"#bt_version_major","objective":"version"},"bold":true,"color":"dark_green"},{"text":".","bold":true,"color":"dark_green"},{"score":{"name":"#bt_version_minor","objective":"version"},"bold":true,"color":"dark_green"},{"text":".","bold":true,"color":"dark_green"},{"score":{"name":"#bt_version_patch","objective":"version"},"bold":true,"color":"dark_green"},{"text":"\n"},{"text":"Pack successfully detached from world. It is now possible to remove the datapack...","color":"green"}]
scoreboard objectives remove Instantiated

# {"jformat":6,"jobject":[{"bold":true,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_green","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"a46acf17-3fc7-4e68-9725-9ac2eb780555","text":"Biometunes v."},{"bold":true,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_green","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"8272934e-a818-42ee-b458-4eef72fcc8d4","score_name":"#bt_version_major","score_objective":"version","score_value":null},{"bold":true,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_green","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"5efa7a51-e4d3-4040-b742-60051afa6eff","text":"."},{"bold":true,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_green","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"d6f37fe4-2f5e-40a3-8b3c-6a1c3376a8cf","score_name":"#bt_version_minor","score_objective":"version","score_value":null},{"bold":true,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_green","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"468a421b-6b92-4342-b32b-4499bc1a1775","text":"."},{"bold":true,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_green","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"4b8a8ef3-908c-4c37-b2be-f8721551b5d1","score_name":"#bt_version_patch","score_objective":"version","score_value":null},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"dc6e0928-35e0-4458-93b6-aa7f714d0128","text":"\n"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"green","insertion":"","click_event_type":0,"click_event_value":"","hover_event_type":0,"hover_event_value":"","hover_event_object":{},"hover_event_children":[],"id":"c3c0e818-7323-4fce-88af-bdbd17350a65","text":"Pack successfully detached from world. It is now possible to remove the datapack..."}],"command":"/tellraw @s %s","jtemplate":"tellraw"}
