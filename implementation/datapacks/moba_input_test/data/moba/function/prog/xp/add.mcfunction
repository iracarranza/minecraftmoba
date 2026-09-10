# expects amount in #amt moba_calc
execute if score @s moba_lvl >= #cap_lvl moba_calc run return run tellraw @s {"text":"[PROG] LEVEL 30 - progression capped, XP discarded","color":"gray"}
scoreboard players operation @s moba_xp += #amt moba_calc
tellraw @s [{"text":"XP +","color":"green"},{"score":{"name":"#amt","objective":"moba_calc"},"color":"green"}]
function moba:prog/xp/level_check
