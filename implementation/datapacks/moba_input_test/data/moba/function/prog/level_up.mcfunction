scoreboard players operation #prev_uu moba_calc = @s moba_uu
function moba:prog/recalc
tellraw @s [{"text":"MOBA LEVEL ","color":"aqua","bold":true},{"score":{"name":"@s","objective":"moba_lvl"},"color":"aqua","bold":true}]
execute if score @s moba_uu > #prev_uu moba_calc run tellraw @s {"text":"UNIVERSAL GROWTH: +1 Health / +1 Hunger / +3 Inventory","color":"aqua"}
function moba:prog/spec_avail
execute if score @s moba_specc < #avail moba_calc run tellraw @s {"text":"SPECIALIZATION AVAILABLE: /trigger moba_spec set 1|2|3","color":"light_purple"}
