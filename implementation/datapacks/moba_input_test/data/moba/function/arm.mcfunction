item replace entity @s weapon.offhand with minecraft:stick[minecraft:custom_data={moba_sentinel:1b}]
scoreboard players set @s moba_armed 1
scoreboard players set @s moba_deb 3
tellraw @s {"text":"[MOBA] armed","color":"green"}
