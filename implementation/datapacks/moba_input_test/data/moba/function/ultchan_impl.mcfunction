execute as @a[scores={moba_ult=1..}] unless predicate moba:fl_sprint run function moba:ult_cancel
execute as @a[scores={moba_ult=1..}] run scoreboard players add @s moba_ult 1
execute as @a[scores={moba_ult=41..}] run function moba:ult_cast
