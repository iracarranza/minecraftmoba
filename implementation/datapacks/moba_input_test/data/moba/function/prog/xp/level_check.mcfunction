# At the cap, hold XP pinned at the threshold so the bar reads full.
execute if score @s moba_lvl >= #cap_lvl moba_calc run return run scoreboard players operation @s moba_xp = #need moba_calc
# not enough for a level: excess simply stays banked toward the next one
execute if score @s moba_xp < #need moba_calc run return 0
scoreboard players operation @s moba_xp -= #need moba_calc
scoreboard players add @s moba_lvl 1
function moba:prog/level_up
# recurse: one award may cross several levels
function moba:prog/xp/level_check
