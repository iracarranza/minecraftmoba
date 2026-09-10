# first-join initialization (never re-runs for an initialized player)
execute as @a unless score @s moba_init matches 1 run function moba:prog/init

# debug trigger dispatch
execute as @a[scores={moba_xpadd=1..}] run function moba:prog/tr/xpadd
execute as @a[scores={moba_lvlup=1..}] run function moba:prog/tr/lvlup
execute as @a[scores={moba_setlvl=1..}] run function moba:prog/tr/setlvl
execute as @a[scores={moba_reset=1..}] run function moba:prog/tr/reset
execute as @a[scores={moba_spec=1..}] run function moba:prog/tr/spec
execute as @a[scores={moba_respec=1..}] run function moba:prog/tr/respec
execute as @a[scores={moba_growth=1..}] run function moba:prog/tr/growth
execute as @a[scores={moba_prog=1..}] run function moba:prog/tr/report
scoreboard players enable @a moba_xpadd
scoreboard players enable @a moba_lvlup
scoreboard players enable @a moba_setlvl
scoreboard players enable @a moba_reset
scoreboard players enable @a moba_spec
scoreboard players enable @a moba_respec
scoreboard players enable @a moba_growth
scoreboard players enable @a moba_prog

# continuous enforcement / visualization
execute as @a run function moba:prog/xp/sync
execute as @a run function moba:prog/food
execute as @a at @s run function moba:prog/inv/sweep
