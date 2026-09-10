scoreboard objectives add moba_armed dummy
scoreboard objectives add moba_sneak dummy
scoreboard objectives add moba_sprint dummy
scoreboard objectives add moba_ult dummy
scoreboard objectives add moba_deb dummy
scoreboard objectives add moba_test trigger
scoreboard objectives add moba_arm trigger
scoreboard players reset @a moba_armed
scoreboard players reset @a moba_ult
scoreboard players set #ticks moba_deb 0
function moba:prog/config
# /reload must not reset progression: only re-derive caps from stored state
execute as @a if score @s moba_init matches 1 run function moba:prog/recalc
tellraw @a {"text":"[MOBA] v7 loaded - inputs + progression. /function moba:probe_all, /trigger moba_prog","color":"yellow"}
