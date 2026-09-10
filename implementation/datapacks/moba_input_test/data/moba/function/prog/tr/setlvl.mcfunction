scoreboard players operation @s moba_lvl = @s moba_setlvl
scoreboard players set @s moba_setlvl 0
scoreboard players operation @s moba_lvl < #cap_lvl moba_calc
scoreboard players operation @s moba_lvl > #c1 moba_calc
scoreboard players set @s moba_xp 0
# claimed specializations may now exceed what the new level allows
function moba:prog/spec_avail
execute unless score @s moba_specc <= #avail moba_calc run function moba:prog/respec
function moba:prog/recalc
function moba:prog/report
