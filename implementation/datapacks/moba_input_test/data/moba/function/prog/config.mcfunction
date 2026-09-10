# Progression state (authoritative, datapack-owned). Scoreboards persist /reload.
scoreboard objectives add moba_init dummy
scoreboard objectives add moba_xp dummy
scoreboard objectives add moba_lvl dummy
scoreboard objectives add moba_uu dummy
scoreboard objectives add moba_ubonus dummy
scoreboard objectives add moba_uhp dummy
scoreboard objectives add moba_ufood dummy
scoreboard objectives add moba_uinv dummy
scoreboard objectives add moba_shp dummy
scoreboard objectives add moba_sfood dummy
scoreboard objectives add moba_sinv dummy
scoreboard objectives add moba_specc dummy
scoreboard objectives add moba_caphp dummy
scoreboard objectives add moba_capfood dummy
scoreboard objectives add moba_capinv dummy
scoreboard objectives add moba_fenf dummy
scoreboard objectives add moba_hclamp dummy
scoreboard objectives add moba_calc dummy

# Debug triggers (extends the moba_test / moba_arm trigger convention)
scoreboard objectives add moba_xpadd trigger
scoreboard objectives add moba_lvlup trigger
scoreboard objectives add moba_setlvl trigger
scoreboard objectives add moba_reset trigger
scoreboard objectives add moba_spec trigger
scoreboard objectives add moba_respec trigger
scoreboard objectives add moba_growth trigger
scoreboard objectives add moba_prog trigger

# --- TEMPORARY XP CURVE (edit this one line to retune) ---
# Flat cost per MOBA level. Deliberately NOT the vanilla escalating formula.
scoreboard players set #need moba_calc 100

# --- LEVEL 1 CAPACITY FLOORS ---
scoreboard players set #base_hp moba_calc 9
scoreboard players set #base_food moba_calc 9
scoreboard players set #base_inv moba_calc 6

# --- UNIVERSAL CAPACITY GROWTH UNIT ---
scoreboard players set #g_hp moba_calc 1
scoreboard players set #g_food moba_calc 1
scoreboard players set #g_inv moba_calc 3

# --- CAPACITY-SPECIALIZATION BONUS PER SELECTION ---
scoreboard players set #s_hp moba_calc 2
scoreboard players set #s_food moba_calc 2
scoreboard players set #s_inv moba_calc 6

# --- UNIVERSAL GROWTH CEILINGS (floors that specialization builds on) ---
# Universal growth stops here. Specialization is additive ABOVE these.
scoreboard players set #uni_hp moba_calc 20
scoreboard players set #uni_food moba_calc 20
scoreboard players set #uni_inv moba_calc 36

# --- HARD CAPS ON EFFECTIVE CAPACITY ---
# Inventory alone is a true hard cap: specialization cannot exceed it.
# Health and Hunger have NO effective cap - specialization may exceed 20.
scoreboard players set #cap_inv moba_calc 36
scoreboard players set #cap_lvl moba_calc 30
# Largest hunger value vanilla's foodLevel can hold. This is a REPRESENTATION
# limit used only to derive the enforcement target - never a design cap.
scoreboard players set #food_max moba_calc 20

# arithmetic constants
scoreboard players set #c1 moba_calc 1
scoreboard players set #c2 moba_calc 2
scoreboard players set #c5 moba_calc 5
scoreboard players set #c7 moba_calc 7
scoreboard players set #c38 moba_calc 38
