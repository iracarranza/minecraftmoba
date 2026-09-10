# ============================================================
# UNIVERSAL GROWTH BREAKPOINT SCHEDULE  --  EDIT THIS FILE ONLY
# One line per level at which a universal growth unit is granted.
# Universal growth occurs on ordinary growth levels AND stacks with certain
# breakpoint rewards, so the full non-specialized 1-17 schedule is:
#   2 4 7 8 9 11 13 14 15 16 17  = 11 units
#   Health/Hunger  9 + 11      = 20 at Lv17 (vanilla baseline)
#   Inventory      6 + 10 x 3  = 36 at Lv16; Lv17's +3 is absorbed by the cap
# Add/remove lines to retune; nothing else needs to change.
# ============================================================
scoreboard players set @s moba_uu 0
execute if score @s moba_lvl matches 2.. run scoreboard players add @s moba_uu 1
execute if score @s moba_lvl matches 4.. run scoreboard players add @s moba_uu 1
execute if score @s moba_lvl matches 7.. run scoreboard players add @s moba_uu 1
execute if score @s moba_lvl matches 8.. run scoreboard players add @s moba_uu 1
execute if score @s moba_lvl matches 9.. run scoreboard players add @s moba_uu 1
execute if score @s moba_lvl matches 11.. run scoreboard players add @s moba_uu 1
execute if score @s moba_lvl matches 13.. run scoreboard players add @s moba_uu 1
execute if score @s moba_lvl matches 14.. run scoreboard players add @s moba_uu 1
execute if score @s moba_lvl matches 15.. run scoreboard players add @s moba_uu 1
execute if score @s moba_lvl matches 16.. run scoreboard players add @s moba_uu 1
execute if score @s moba_lvl matches 17.. run scoreboard players add @s moba_uu 1
# optional debug-granted extra units (/trigger moba_growth), for testing only
scoreboard players operation @s moba_uu += @s moba_ubonus
