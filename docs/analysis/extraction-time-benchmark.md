# Extraction Time Benchmark — first run

Status: **Measurement result**
Date: 20 September 2026

First implementation of the calculator contract in
[extraction-expedition-calculator.md](extraction-expedition-calculator.md). It
covers the sections that the map can currently answer — target extraction,
access/excavation, Fortune yield, Efficiency break time — and explicitly does
not cover the ones it cannot, listed at the end.

## What it is

Three modules under `implementation/worldgen/expedition/`:

- `vanilla.py` — break-time and movement formulas, plus the hardness table read
  back off the running Paper fixture (`moba curve`, 20 Sep 2026). Nothing here
  is a balance decision.
- `manifest.py` — exports the measured world into the calculator's map-manifest
  schema (spec §17). Fields the source does not measure, notably routes, are
  emitted null rather than filled in.
- `benchmark.py` — time-to-quantity per strategy, per kit, per resource.

`terrain_harvest/caves.py` was extended to record per-resource mean depth,
deepslate share, and a per-layer y-histogram, so branch-mining density is
measured at a depth a miner would actually choose instead of averaged over
layers nobody digs.

## The measurement

9 cells of 48×48, y −60..90, on the Task B default map. 182 units, the gross
combat-tier benchmark. Minutes of mining and in-cave travel only.

| Resource | Kit | Branch | Quarry | Cave (150/300/600 sweep) |
|---|---|---:|---:|---:|
| Copper | stone, Lv1 | 5.5 | 19.0 | 7.2 / 4.1 / 2.5 |
| Copper | iron + Eff I + Fort I | 2.1 | 7.4 | 5.1 / 2.8 / 1.6 |
| Iron | stone, Lv1 | 56.8 | 215.5 | 26.4 / 15.2 / 9.6 |
| Iron | iron + Eff I + Fort I | 21.8 | 82.6 | 18.4 / 10.0 / 5.8 |
| Diamond | iron + Eff I | 102.2 | 400.9 | **cannot finish** |
| Diamond | diamond + Eff I + Fort I | 57.8 | 226.6 | **cannot finish** |

## What it says about the design question

**Caves do reward practised behaviour, for copper and iron.** Walking caves
beats branch mining across the whole sweep band for copper, and beats it for
iron at all but the most pessimistic sweep rate. The 2.2% exposed fraction
sounded alarming, but exposed ore is cheap to collect precisely because the
cave already did the digging — the comparison that matters is time, not share.

**Diamond is the exception, and it is a real finding.** Across nine cells there
is not enough exposed diamond to reach 182 by exploration at all — the run
reports `SHORT BY 102` rather than a time. A diamond benchmark currently
*forces* branch mining, at 58–102 minutes, which is longer than the whole match.
Either the diamond benchmark is wrong, or diamond needs a different acquisition
route (Worksite capitalization is the obvious candidate), or diamond exposure
needs authoring. This is the first quantified case where the map does not
support a resource target.

**Progression moves the needle about as much as strategy does.** Iron at 182
goes 56.8 → 21.8 minutes on branch mining from Lv1 to Lv7. Efficiency I and
Fortune I are each doing real work, which is worth knowing given Efficiency was
silently inert until the duplicate-config-key fix.

## The one soft number

Cave sweep rate — how much cave volume a moving player's eyes actually cover per
second — is not derivable and is not measured. It is declared as a parameter and
every result is reported across a 4× band (150/300/600 blocks/s) instead of
being collapsed to one number. The qualitative conclusion holds across the whole
band for copper; for iron it flips only at the pessimistic end. Measuring this
properly means instrumenting a real player walking a real cave, which is a
worthwhile future fixture scenario.

## Not covered

Everything the manifest has no measurement for, and which the full calculator
adds once it does:

- travel from base to cave mouth, and search time to find a deposit — the
  manifest has no measured routes, so `route_from` is null
- Hunger, tool durability, inventory capacity, return trips and delivery, which
  separate acquired time from available time
- hostile interruption underground; the opportunity map reads hostiles as zero
- Worksite activation and capitalization
- multiple concurrent extractors; these figures are one player's minutes

The headline numbers are therefore a **lower bound**: real extraction is slower
than this by the travel, search and logistics the manifest cannot yet supply.
