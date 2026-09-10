# Debug grant of universal growth units. The established 1-17 breakpoint set
# is incomplete, so this is how the 36-slot / 20-HP ceilings get tested.
scoreboard players operation @s moba_ubonus += @s moba_growth
scoreboard players set @s moba_growth 0
function moba:prog/recalc
tellraw @s {"text":"UNIVERSAL GROWTH: +1 Health / +1 Hunger / +3 Inventory (per unit, debug)","color":"aqua"}
function moba:prog/report
