scoreboard players set @s moba_lvl 1
scoreboard players set @s moba_xp 0
scoreboard players set @s moba_ubonus 0
function moba:prog/respec
tellraw @s {"text":"[PROG] reset to LEVEL 1","color":"yellow"}
function moba:prog/report
