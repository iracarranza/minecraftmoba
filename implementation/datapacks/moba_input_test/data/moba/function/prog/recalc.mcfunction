# Effective capacity is always RECALCULATED from authoritative state
# (level + debug bonus + specialization selections). Never mutated in place.
function moba:prog/uni_units

# universal floors
scoreboard players operation #t moba_calc = @s moba_uu
scoreboard players operation #t moba_calc *= #g_hp moba_calc
scoreboard players operation @s moba_uhp = #base_hp moba_calc
scoreboard players operation @s moba_uhp += #t moba_calc

scoreboard players operation #t moba_calc = @s moba_uu
scoreboard players operation #t moba_calc *= #g_food moba_calc
scoreboard players operation @s moba_ufood = #base_food moba_calc
scoreboard players operation @s moba_ufood += #t moba_calc

scoreboard players operation #t moba_calc = @s moba_uu
scoreboard players operation #t moba_calc *= #g_inv moba_calc
scoreboard players operation @s moba_uinv = #base_inv moba_calc
scoreboard players operation @s moba_uinv += #t moba_calc

# universal growth ceilings ('<' assigns the minimum of the two).
# These bound the UNIVERSAL floor only; specialization stacks on top.
scoreboard players operation @s moba_uhp < #uni_hp moba_calc
scoreboard players operation @s moba_ufood < #uni_food moba_calc
scoreboard players operation @s moba_uinv < #uni_inv moba_calc

# + specialization (additive ABOVE the universal floor)
scoreboard players operation #t moba_calc = @s moba_shp
scoreboard players operation #t moba_calc *= #s_hp moba_calc
scoreboard players operation @s moba_caphp = @s moba_uhp
scoreboard players operation @s moba_caphp += #t moba_calc

scoreboard players operation #t moba_calc = @s moba_sfood
scoreboard players operation #t moba_calc *= #s_food moba_calc
scoreboard players operation @s moba_capfood = @s moba_ufood
scoreboard players operation @s moba_capfood += #t moba_calc

scoreboard players operation #t moba_calc = @s moba_sinv
scoreboard players operation #t moba_calc *= #s_inv moba_calc
scoreboard players operation @s moba_capinv = @s moba_uinv
scoreboard players operation @s moba_capinv += #t moba_calc

# Inventory is the ONLY hard cap on effective capacity.
scoreboard players operation @s moba_capinv < #cap_inv moba_calc
# Health and Hunger are deliberately NOT capped: Health/Hunger specialization
# may raise maximum capacity above 20 (up to 26 with all three choices).

# Enforcement target for Hunger = the portion vanilla's foodLevel can hold.
# Effective capacity above 20 is real state that the vanilla 10-icon HUD
# cannot represent; how to surface it is an OPEN presentation question.
# Clamping only the enforcement target keeps the design value intact.
scoreboard players operation @s moba_fenf = @s moba_capfood
scoreboard players operation @s moba_fenf < #food_max moba_calc

# apply Health through the vanilla attribute (lowering auto-clamps current HP;
# raising does NOT heal, which is the behaviour we want)
execute store result storage moba:prog hp int 1 run scoreboard players get @s moba_caphp
function moba:prog/apply_hp with storage moba:prog
