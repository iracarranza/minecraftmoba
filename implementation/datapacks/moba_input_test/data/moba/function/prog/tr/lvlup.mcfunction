scoreboard players set @s moba_lvlup 0
# exactly enough to cross the current temporary threshold
scoreboard players operation #amt moba_calc = #need moba_calc
scoreboard players operation #amt moba_calc -= @s moba_xp
function moba:prog/xp/add
