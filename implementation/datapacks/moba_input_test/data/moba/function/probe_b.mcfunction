tellraw @s {"text":"[probe_b] PARSED - this shape is VALID","color":"green"}
execute if predicate moba:in_b run tellraw @s {"text":"[probe_b] sneak = TRUE","color":"green"}
execute unless predicate moba:in_b run tellraw @s {"text":"[probe_b] sneak = false","color":"gray"}
