# WORKAROUND. Vanilla exposes no max-hunger attribute, and /data modify is
# refused on players, so foodLevel cannot be written directly. We READ
# foodLevel (reads are permitted) and, while it exceeds the progression cap,
# drive it down with a high-amplifier hunger effect. moba_hclamp records that
# WE applied it, so we only ever clear our own effect.
# NOTE: we clamp to moba_fenf (= min(effective capacity, 20)), NOT to the
# effective capacity itself. Above 20 there is nothing to enforce - vanilla's
# foodLevel simply cannot hold more, so the clamp correctly never fires.
execute store result score #food moba_calc run data get entity @s foodLevel
execute if score #food moba_calc > @s moba_fenf run scoreboard players set @s moba_hclamp 1
execute if score #food moba_calc > @s moba_fenf run effect give @s minecraft:hunger 1 255 true
execute if score @s moba_hclamp matches 1 if score #food moba_calc <= @s moba_fenf run effect clear @s minecraft:hunger
execute if score @s moba_hclamp matches 1 if score #food moba_calc <= @s moba_fenf run scoreboard players set @s moba_hclamp 0
