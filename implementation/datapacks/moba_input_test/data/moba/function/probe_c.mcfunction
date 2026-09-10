tellraw @s {"text":"[probe_c] PARSED - this shape is VALID","color":"green"}
execute if predicate moba:in_c run tellraw @s {"text":"[probe_c] sneak = TRUE","color":"green"}
execute unless predicate moba:in_c run tellraw @s {"text":"[probe_c] sneak = false","color":"gray"}
