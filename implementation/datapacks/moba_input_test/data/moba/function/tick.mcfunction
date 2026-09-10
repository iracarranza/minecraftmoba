scoreboard players add #ticks moba_deb 1
scoreboard players add @a moba_armed 0
scoreboard players add @a moba_deb 0
scoreboard players add @a moba_ult 0
execute as @a[scores={moba_test=1..}] run function moba:dialog_test
execute as @a[scores={moba_arm=1..}] run function moba:rearm
scoreboard players enable @a moba_test
scoreboard players enable @a moba_arm
execute as @a[scores={moba_deb=1..}] run scoreboard players remove @s moba_deb 1
execute as @a[scores={moba_armed=0}] run function moba:arm
function #moba:modifiers
function #moba:detect
function #moba:ultchan
