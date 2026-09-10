# Macro: {slot:"<item slot name>", i:"<Inventory Slot byte>"}
# Copies the stack out to a ground item BEFORE clearing the slot, and only
# clears if the copy verifiably succeeded. Items are never deleted.
summon minecraft:item ~ ~0.4 ~ {Tags:["moba_ev"],PickupDelay:60s,Item:{id:"minecraft:stone",count:1}}
$execute store success score #ok moba_calc run data modify entity @e[type=item,tag=moba_ev,limit=1] Item.id set from entity @s Inventory[{Slot:$(i)}].id
$execute if score #ok moba_calc matches 1 run data modify entity @e[type=item,tag=moba_ev,limit=1] Item.count set from entity @s Inventory[{Slot:$(i)}].count
$execute if score #ok moba_calc matches 1 run data modify entity @e[type=item,tag=moba_ev,limit=1] Item.components set from entity @s Inventory[{Slot:$(i)}].components
$execute if score #ok moba_calc matches 1 run item replace entity @s $(slot) with air
execute if score #ok moba_calc matches 1 run tellraw @s {"text":"[PROG] locked slot cleared - item dropped, not deleted","color":"gold"}
execute if score #ok moba_calc matches 0 run kill @e[type=item,tag=moba_ev]
tag @e[type=item,tag=moba_ev] remove moba_ev
