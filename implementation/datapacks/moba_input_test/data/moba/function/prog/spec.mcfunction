# expects selection in #sel moba_calc: 1=Health 2=Hunger 3=Inventory
execute unless score #sel moba_calc matches 1..3 run return run tellraw @s {"text":"[PROG] use /trigger moba_spec set 1|2|3 (1=Health 2=Hunger 3=Inventory)","color":"red"}
function moba:prog/spec_avail
execute unless score @s moba_specc < #avail moba_calc run return run tellraw @s {"text":"[PROG] no unclaimed specialization milestone (levels 3 / 18 / 24)","color":"red"}
execute if score #sel moba_calc matches 1 run scoreboard players add @s moba_shp 1
execute if score #sel moba_calc matches 2 run scoreboard players add @s moba_sfood 1
execute if score #sel moba_calc matches 3 run scoreboard players add @s moba_sinv 1
scoreboard players add @s moba_specc 1
function moba:prog/recalc
execute if score #sel moba_calc matches 1 run tellraw @s {"text":"SPECIALIZATION: HEALTH +2","color":"light_purple"}
execute if score #sel moba_calc matches 2 run tellraw @s {"text":"SPECIALIZATION: HUNGER +2","color":"light_purple"}
execute if score #sel moba_calc matches 3 run tellraw @s {"text":"SPECIALIZATION: INVENTORY +6","color":"light_purple"}
function moba:prog/report
