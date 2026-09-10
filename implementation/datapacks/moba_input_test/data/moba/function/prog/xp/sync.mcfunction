# Drive Minecraft's native XP bar + level number FROM authoritative state.
# Also the mechanism that discards vanilla orb XP: any drift is overwritten.

# vanilla points-per-level requirement for the DISPLAYED level,
# needed only to convert our fraction into a bar fill
execute if score @s moba_lvl matches ..15 run function moba:prog/xp/req_low
execute if score @s moba_lvl matches 16.. run function moba:prog/xp/req_high

# points = moba_xp / #need * req(level)
scoreboard players operation #pts moba_calc = @s moba_xp
scoreboard players operation #pts moba_calc *= #req moba_calc
scoreboard players operation #pts moba_calc /= #need moba_calc
# clamp to req-1 so vanilla never auto-levels the display out from under us
scoreboard players operation #reqm moba_calc = #req moba_calc
scoreboard players operation #reqm moba_calc -= #c1 moba_calc
scoreboard players operation #pts moba_calc < #reqm moba_calc
execute if score #pts moba_calc matches ..-1 run scoreboard players set #pts moba_calc 0

# only write when the display has drifted (orb pickup, death, /xp, first join)
execute store result score #qlvl moba_calc run xp query @s levels
execute store result score #qpts moba_calc run xp query @s points
execute unless score #qlvl moba_calc = @s moba_lvl run function moba:prog/xp/write
execute if score #qlvl moba_calc = @s moba_lvl unless score #qpts moba_calc = #pts moba_calc run function moba:prog/xp/write
