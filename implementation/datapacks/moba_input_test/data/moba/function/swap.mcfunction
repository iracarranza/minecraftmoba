item replace entity @s weapon.mainhand from entity @s weapon.offhand
item replace entity @s weapon.offhand with minecraft:stick[minecraft:custom_data={moba_sentinel:1b}]
scoreboard players set @s moba_deb 3
execute if score @s moba_ult matches 1.. run return 0
execute if score @s moba_sprint matches 1.. run return run function moba:ult_start
execute if score @s moba_sneak matches 1.. run return run tellraw @a {"text":"A2"}
tellraw @a {"text":"A1"}
