# CAPACITY-SPECIALIZATION MILESTONE LEVELS -- edit to retune
scoreboard players set #avail moba_calc 0
execute if score @s moba_lvl matches 3.. run scoreboard players add #avail moba_calc 1
execute if score @s moba_lvl matches 18.. run scoreboard players add #avail moba_calc 1
execute if score @s moba_lvl matches 24.. run scoreboard players add #avail moba_calc 1
