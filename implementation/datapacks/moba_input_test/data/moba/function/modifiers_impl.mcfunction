execute as @a if predicate moba:fl_sneak run scoreboard players set @s moba_sneak 3
execute as @a[scores={moba_sneak=1..}] unless predicate moba:fl_sneak run scoreboard players remove @s moba_sneak 1
execute as @a if predicate moba:fl_sprint run scoreboard players set @s moba_sprint 3
execute as @a[scores={moba_sprint=1..}] unless predicate moba:fl_sprint run scoreboard players remove @s moba_sprint 1
