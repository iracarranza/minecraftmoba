tellraw @s {"text":"[probe_flags] PARSED","color":"green"}
execute if predicate moba:fl_sneak run tellraw @s {"text":"is_sneaking = TRUE","color":"green"}
execute unless predicate moba:fl_sneak run tellraw @s {"text":"is_sneaking = false","color":"gray"}
execute if predicate moba:fl_sprint run tellraw @s {"text":"is_sprinting = TRUE","color":"green"}
execute unless predicate moba:fl_sprint run tellraw @s {"text":"is_sprinting = false","color":"gray"}
