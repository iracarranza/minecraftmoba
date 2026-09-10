# First-ever initialization only. moba_init persists across /reload, so a
# reload never resets an already-initialized player's progression.
scoreboard players set @s moba_init 1
function moba:prog/reset
