tellraw @s {"text":"[probe_d] PARSED - this shape is VALID","color":"green"}
execute if predicate moba:in_d run tellraw @s {"text":"[probe_d] sneak = TRUE","color":"green"}
execute unless predicate moba:in_d run tellraw @s {"text":"[probe_d] sneak = false","color":"gray"}
