# Hunger authority investigation — 22 September 2026

## Finding and scope

**The reported freeze has not been reproduced on main `8199c13`.** Production
code was left unchanged. On a disposable Paper 1.21.11 / Java 21 Alpha server,
real sprint/jump activity away from fountains and Routes depleted saturation,
then vanilla food; ordinary bread became edible and restored food. It remained
inedible while food was actually full. There is no repeated Hunger HUD
projection writing food or exhaustion in this revision.

This is independent of the 930010639 fixture. No experiment trials, balancing,
screening, authoring changes, or production telemetry were added. Existing dirty
checkouts and the live server were preserved; work is isolated on
`codex/hunger-depletion-regression` in `/tmp/minecraftmoba-hunger`.

## Complete authority path

Paths below are relative to `implementation/plugin/src/main/java/com/minecraftmoba/plugin/`.

| Stage | Authority / behavior on current main |
|---|---|
| Activity | Vanilla sprint/jump/mining etc. generate exhaustion. The acceptance listener sees real `EntityExhaustionEvent` events, not fabricated depletion. Walking alone is not equivalent to sprinting/jumping. |
| Scaling | `VitalsScaling.sampleExhaustion()` samples every two ticks by default, adds only the extra positive delta for `Vitals.exhaustionMultiplier(capacity) = 20 / capacity`, and records the result. It never sets food or saturation and never clears exhaustion. Falling samples are left alone. |
| Vanilla reserve | Vanilla spends exhaustion against saturation first, then food. Both the server samples and client food updates show this sequence. |
| Custom Hunger | `PlayerData` contains no separate current-Hunger reserve. `MobaPlugin.effectiveHunger()` is the derived Capacity rate parameter (currently clamped at 20); `foodCeiling()` is 20 with scaling enabled. Vanilla `Player#getFoodLevel()` is current Hunger. |
| Match initialization | `Match.spawn()` writes food=`foodCeiling()` and saturation to that same value: **20 / 20** at level 1 under scaling. This is initialization, not a per-tick display projection. Adding an enrolled participant during a running match also invokes spawn. |
| Capacity enforcement | `MobaPlugin.enforceHunger()` returns immediately with scaling enabled. In legacy mode it can only clamp food and saturation downward. Its `FoodLevelChangeEvent` handler similarly returns under scaling, otherwise clamps the target downward. |
| Fountain | `FountainRegen.tick()` restores food by 1 per configured interval to the ceiling only for a living participant of a running match, near their enabled own Fountain (radius 8). `reconstruct()` writes food; it does not restore saturation. Respawn writes configured food (default 1), capped by the ceiling, and saturation 0 on the scheduled respawn task. |
| Lobby protection | `LobbySafety.hunger()` cancels food decreases outside living participation in a running match. Being in Survival or physically inside the map does **not** prove the player is in a running match. This is an existing explicit protection, not a display side effect. |
| Route relief | `Routes.applyEffects()` subtracts configured exhaustion relief (default 0.1) for sprinting near a recognized Route. It does not write food or saturation. The regression platform is outside authored Routes. |
| Health regeneration | `HungerRegen.tick()` adds exhaustion for qualifying custom healing, not food. With scaling it currently qualifies below vanilla's food>=18 threshold and within the configured missing-Hunger threshold. `Match.start()` explicitly disables vanilla natural regeneration by default. No healing occurs in the full-health test. |
| Display | Native hunger icons show vanilla food. Optional `HungerDisplay.refresh()` reads food and the ceiling and writes only a bossbar title; it is disabled by default. General `Hud` is progression / Health / slots display and does not write Hunger. Resource-pack sprites are presentation only. |
| Input before food use | `AbilityInputs.interact()` can cancel right-clicks while Ability Mode is armed; normal inactive mode passes through. `OffhandMap` cancels interaction with the tagged offhand map, not main-hand bread. `Routes.onInteract()` reserves banner interaction in Infrastructure Mode. These are contextual input gates, not Hunger writers; the test uses ordinary main-hand bread outside these modes. |
| Food use | Vanilla ordinary-food use is gated below food 20. `HungerRegen.consume()` additionally cancels completed ordinary-food consumption at `foodCeiling()` for enrolled players, while preserving vanilla always-edible items and non-food items. At food<20 with scaling, bread consumption proceeds; vanilla's resulting `FoodLevelChangeEvent` is neither capped nor cancelled for an active participant. |

A repository-wide search of production Java for `setFoodLevel`, `setSaturation`,
`setExhaustion`, and `FoodLevelChangeEvent` finds only the writers/handlers above.
There is no custom current-Hunger store waiting to consume vanilla exhaustion.

## Recent changes and plausible explanation of the observation

`52f44b4` introduced the full-length vitals bar and exhaustion scaling and
explicitly held vanilla natural regeneration off. `456fdad` corrected the spawn,
Fountain and consumption ceiling to use `foodCeiling()`. At level 1, its single
spawn-variable change also changed **starting saturation from 9 to 20**, because
`Match.spawn()` uses that variable for both fields. Exhaustion must now consume
20 hidden saturation points before the food bar moves. Ordinary food is correctly
inedible during that period because actual food is still 20.

That explains the observed delay in this controlled run, but is **not proof of
the user's live-session cause**. Fountain replenishment, non-running/dead
participation protection, activity type, and session duration remain possible
context differences. The live persisted match world reported Easy difficulty;
its `server.properties` also says Easy. Relevant class bytes in the live jar
matched the main build for MobaPlugin, VitalsScaling, HungerRegen, HungerDisplay,
FountainRegen and Match. No live deployment or state mutation was performed.

The sampling algorithm can miss some scaling when an exhaustion rollover falls
between samples. Its comment that exhaustion has no event is also inaccurate for
this Paper API; the test listener uses that event. Neither finding establishes a
freeze, and replacing the rate model or tuning starting saturation would exceed
this regression fix's evidence. Both remain unchanged.

## Reproduction and validation

The first attempt's unloaded platform caused a fall and Fountain respawn; it was
explicitly discarded. The corrected exploratory run built/loaded a 3,721-block
stone platform at y=149, centered near (-2100, 0), in the running Alpha match.
Starting from unmodified food=20, saturation=20, full Health, 120 seconds of real
sprint/jump input produced food=15; bread then restored food=20. With a separate
controlled saturation=0 / exhaustion=3.9 setup, activity produced food=17 and
bread again restored food=20. Food decrease events reached MONITOR uncancelled.

The checked-in [acceptance runner](../../validation/plugin/hunger/README.md)
turns this into assertions rather than a synthetic formula test: full-food
rejection, natural depletion, one bread consumed after depletion, alive/on-platform
checks, and the isolated exhaustion rollover. All diagnostics live in the separate
acceptance plugin, never in the gameplay jar. The runner stops its own server.
It forces the existing **Alpha `resource_light_v2`**, not the near-miss fixture.

Exact local validation commands (from `/tmp/minecraftmoba-hunger`):

```sh
export JAVA_HOME='/Users/iracarranza/Library/Application Support/minecraft/runtime/java-runtime-delta/mac-os-arm64/java-runtime-delta/jre.bundle/Contents/Home'
implementation/plugin/gradlew -p implementation/plugin jar test :acceptance-fixture:build
/tmp/nearmiss-fixture-930010639/venv/bin/python validation/plugin/hunger/prepare.py \
  --server /tmp/hunger-acceptance/server \
  --runtime /private/tmp/alpha-server \
  --base /Users/iracarranza/minecraftmoba/artifacts/worldgen/alpha-0.1/base-terrain
NODE_PATH=/tmp/minecraftmoba-terrain-audit/validation/plugin/protocol/node_modules \
  node validation/plugin/hunger/reproduce.cjs \
  /tmp/hunger-acceptance/server /tmp/hunger-acceptance/report-final
```

The reused venv provides PyYAML 6.0.3 and the existing protocol dependency install
provides locked Mineflayer 4.39.0. Reusing those dependencies does not operate the
near-miss world or server. The runner README includes fresh dependency setup.

Final acceptance: **11/11 checks passed**, with first natural food depletion
observed after 100 seconds of this sprint/jump pattern. Bread reduced the item
count by exactly one and restored food 19→20; the controlled boundary also
permitted consumption after depletion. All **205 unit tests passed**. The
platform check now waits for landing rather than mistaking a mid-jump snapshot
for departure. No production changes preceded or followed these runs.
Both disposable servers are stopped.

[Machine-readable results](../../validation/plugin/hunger/results/2026-09-22/results.json),
[server evidence excerpt](../../validation/plugin/hunger/results/2026-09-22/events.txt),
and [input hashes / unit results](../../validation/plugin/hunger/results/2026-09-22/inputs.json)
are committed. Full local log: `/tmp/hunger-acceptance/report-final/server.log`.

The user subsequently reported that the issue appears solved and requested push
and closure. No further testing or live investigation is pending.

## If the symptom recurs

If the symptom persists, capture the **affected player in the affected session**
without changing their reserve: `/moba match status`, player-side `/moba regen`,
and console `data get entity <player> foodLevel`, `foodSaturationLevel`, and
`foodExhaustionLevel` before/after activity, together with location, active input mode, and duration.
These existing read-only commands distinguish hidden saturation, replenishment,
protection, and a genuinely stalled exhaustion path. No allow-food-at-full bypass
or speculative gameplay fix is justified by the present reproduction.
