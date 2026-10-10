# Night and objective bench

**Prototype/test. Built 10 October 2026.** A lab bench for the part of a match a single person
cannot otherwise see: the six-night cadence (which Worksite tier opens, when the Lair installs
which boss) and the siege model (four routes reducing one defensive capacity). Seeing the
cadence normally means sitting through 110 minutes, and the siege arithmetic had only ever been
exercised by unit tests.

```
/moba lab night                 start (a launched scoop)
  night <1-6> | target | siege combat|structural|signature|lair | report | off
```

Reached from the hub's Night and objectives pedestal (crying obsidian, needs a launched scoop),
the Benches page, or the command. It is a view onto a real scoop, so like the map overlay it has
a hotbar menu and no deck. Leave is in the inventory. While ability mode is on, right-click is the
ability's and the menu stands back.

## It drives the real match

A night jump is `Match.skipTicks`, the same path the ticker takes, so a skipped night is a real
night: `onSunset` fires, Worksites activate, the Lair is told the ordinal. A siege act is
`Match.siege`, the entry point the player-facing paths route to. Nothing is simulated beside
them. The clock only runs forward (`LabTime`): a night that has begun is refused with the reason.

## The plan beside the runtime

The report prints `NightTimeline`, derived from `OpportunityCadence` rather than restated, with
the current night marked, and then what the runtime actually holds: the Worksites by state, the
Lair's report, and each objective's capacity with its contributions by route. A disagreement
between the plan and the state is visible. Tests assert the timeline matches the cadence stage
for stage and that every night opens exactly one thing.

| Night | Plan |
|---|---|
| 1 | Worksite I: Iron + Coal / Blast Furnace + Smoker |
| 2 | Lair: Giant |
| 3 | Worksite II: Diamond + Lapis / Enchanting Table |
| 4 | Lair: Ghast |
| 5 | Worksite III: Ancient Debris + Diamond / Smithing Table |
| 6 | Lair: Ender Dragon |

## The siege routes

Six targets (each team's Outpost, Bastion and Spike), cycled with one press. The four routes all
reduce the SAME capacity, which is the design ("one siege, not three minigames"). Bench amounts,
not tuning: a wave of five defenders, twenty-five blocks of structure, the signature, and the
Lair assault. Confirmed live on the north Nether Bastion (capacity 1818): five defenders took
181.77 (10%), the signature 817.96 (45%), the Lair assault 817.96 (45% of the original, the
remainder), toppling it; a toppled objective then refuses further siege.

It does not disable a Fountain: that is irreversible for the match and can end it.

## What running it showed

Nothing broke, which is itself a result: the cadence, Worksite activation, the Lair schedule and
the siege arithmetic all behaved as their code and docs say on a real launched scoop. Specific
observations, none a defect: night 1 activated **2 of the scoop's 12 Worksites** (the Worksite I
tier); night 2 left the Lair ALIVE with the Giant scheduled; six objectives were registered
from the map's bindings.

## Verification

Timeline against the cadence, the six targets and cycling, the routes, the menu and its refusals
(past nights, unbound and toppled objectives): unit-tested. Live, on a launched real scoop (13
checks): the bench starts with the hotbar and a report showing the plan beside the runtime's
state; night 1 lands on sunset 1 and activates Worksites (0 to 2); night 2 lands on 36000 with the
Lair ALIVE and the Giant scheduled; night 1 is then refused and the clock does not move; Next
objective cycles; combat, signature (from the hotbar) and Lair assault each take exactly their
share and topple the objective; a further siege is refused and the report says TOPPLED; off
restores the hotbar. **Not verified:** the player-facing siege paths (killing a defender,
breaking structure, stealing the Allay) that route to the same calls, and anything on a client.
