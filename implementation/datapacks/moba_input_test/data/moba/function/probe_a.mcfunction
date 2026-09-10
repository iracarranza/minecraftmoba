tellraw @s {"text":"[probe_a] PARSED - this shape is VALID","color":"green"}
execute if predicate moba:in_a run tellraw @s {"text":"[probe_a] sneak = TRUE","color":"green"}
execute unless predicate moba:in_a run tellraw @s {"text":"[probe_a] sneak = false","color":"gray"}
