scoreboard objectives add moba_armed dummy
scoreboard objectives add moba_sneak dummy
scoreboard objectives add moba_sprint dummy
scoreboard objectives add moba_ult dummy
scoreboard objectives add moba_deb dummy
scoreboard objectives add moba_test trigger
scoreboard objectives add moba_arm trigger
scoreboard players reset @a moba_armed
scoreboard players reset @a moba_ult
scoreboard players set #ticks moba_deb 0
tellraw @a {"text":"[MOBA] v6 loaded - run /function moba:probe_all","color":"yellow"}
