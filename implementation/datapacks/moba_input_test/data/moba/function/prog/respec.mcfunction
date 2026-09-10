scoreboard players set @s moba_shp 0
scoreboard players set @s moba_sfood 0
scoreboard players set @s moba_sinv 0
scoreboard players set @s moba_specc 0
# recalc lowers the caps; the attribute clamps current HP, the hunger clamp
# walks food back down, and the inventory sweep evicts newly-locked slots.
function moba:prog/recalc
