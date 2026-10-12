# A real ability, measured through the combat chamber

**11 October 2026.** Paper `1.21.11-132`, the real `MobaPlugin`, the real
`/moba lab combat` commands, driven by an `NmsBodies` tester.
**6 of 6 pass** (`ACCEPT_ONLY="ability damage:"`).

## Why this run

Every earlier number in the chamber's damage log came from a bare
`Player.damage(...)` call. That proves the **instrument** — the raw/final pair,
the immunity window, the rescale — and not that the instrument reads an
**ability**. The slice-1 audit said so in its own "Not covered":

> the fixture used direct `damage` calls, not Miner's Strike or Brawling Slash

Drill Rush is the one built ability whose damage lands on a stationary target:
emerging deals `emergeDamage` to enemies within `emergeRadius` of a point
`emergeDistance` ahead of the caster.

## What was run

Enter as a Mole Operator at level 15 with the cooldown waiver; stand 2.00 blocks
from the dummy, facing it; press A2 to burrow; press A2 again to emerge.

```
PASS enter as a Mole Operator beside the dummy :: class=mole distance=2.00 rawBefore=0.00
PASS Drill Rush burrows the tester :: executions 0 -> 1 active=true
PASS emerging deals the REAL ability's damage and the chamber measures it
     :: dummy hits=1 raw total=4.00 final total=4.00 from this emergence=4.00 dummyHealth=11.11
PASS the report attributes it to the tester, not to a bare damage call
     :: Dealt to the dummy: 1 hits, raw 4.00 -> final 4.00 (0.0% mitigated) sources=[B_tester entity_attack]
```

## What it establishes

- **The measured number is the configured one.** `abilities.definitions.drill_rush.emergeDamage`
  is `4.0`; the log recorded raw `4.00`. The chamber reads the ability, not an
  approximation of it.
- **The rescale round-trips on a real hit.** The dummy is classless at level 0, whose
  effective capacity is 9 points. 4 effective points is 4/9 of capacity, and the
  dummy's Bukkit bar fell 8.89 of 20 — exactly 4/9 × 20. `VitalsScaling` and
  `Vitals.toEffective` agree across an ability's damage, not only a synthetic one.
- **No mitigation, correctly.** A classless level-0 dummy has no armor and no
  reduction, so raw equals final. The 0.0% is a measurement, not a failure to measure.

## [OPEN] The log records the damager, not the ability

`COMBAT_CHAMBER.md` says each hit should record "source ability and branch".
It records `source(e)` = damager name plus vanilla damage cause:

```
sources=[B_tester entity_attack]
```

So the report can say *who* hit the dummy and *how* vanilla classified it, but
**not which ability or which branch** — and `entity_attack` is what a melee swing
reports too. With one ability in flight that is unambiguous; with a kit being
exercised it is not, and the per-source breakdown in the report would merge a
Drill Rush emergence with a sword hit from the same caster.

This only shows when a real ability is run, which is why it did not surface in
slice 1. Closing it means carrying the ability and branch on the damage, not
inferring them at the listener.

## Method

A throwaway server (never the live alpha server), `minecraft-moba.jar` built from
the branch plus `CombatAcceptance.jar`, `maps/*.json.gz` from
`artifacts/worldgen/alpha-0.1/maps/`. `CombatChamber.dealtHits` was added so the
fixture can read individual hits and their damager: the report prints totals and a
per-source breakdown, which is what a tester wants and not enough to assert that an
ability — rather than a `damage()` call — produced the number.
