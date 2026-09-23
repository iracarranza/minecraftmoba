# Handoff — spatial doctrine reconciliation and cadence migration COMPLETE

**22 September 2026. Branch `codex/spatial-cadence-migration`.** The pass
specified by [`specs/superspatialdoctrinespec.md`](specs/superspatialdoctrinespec.md)
is implemented, tested, documented and pushed. Full record:
[`docs/audit/2026-09-22-spatial-cadence-migration.md`](docs/audit/2026-09-22-spatial-cadence-migration.md),
which carries the supersession matrix.

This branch is **not merged to main**. It starts at 03c5bea (spatial doctrine
audit) over 9bb5093 (Hunger audit) over main 8199c13. The original dirty
checkout at `/Users/iracarranza/minecraftmoba` and the live server at
`/private/tmp/alpha-server` were not touched; nothing here ran a server,
regenerated a world or deployed.

## What changed

**The match no longer has a phase timeline.** It has one tick counter and an
alternating cadence derived from it:

> Worksite I → Giant → Worksite II → Ghast → Worksite III → Ender Dragon

The six nights fall at 10/30/50/70/90/110 minutes as a consequence of the
vanilla clock. That is **not** a new match length — the inferred 80-minute
horizon is gone, along with the tick-loop branch that announced a "48-minute
analytical horizon" while double-incrementing `elapsed` and silently losing any
boundary that landed on the skipped tick.

**All three defensive objectives now coexist**, with a spatial ordinal
(midline → Outpost → Bastion → Spike → Fountain) and no prerequisite gate.
`objectives.md §21` moves from "Unresolved" to resolved.

**One permanent Lair**, occupant succeeding Giant → Ghast → Dragon: survivors
persist through the Worksite night between, are replaced (not killed) at the
next Lair night, and a kill leaves the site dormant. Missing a monster does not
stall the clock.

**N/S terrain contrast is no longer read as a defect.** `compare_seeds` emits a
descriptive contrast with `competitive_consequence: unknown` under schema
`seed_team_axis_contrast/2`; historical `/1` JSON keeps its `diagnosis` block.

Tests: Java **231 pass**. Worldgen **346, 2 failures**, both reproduced
unchanged at 03c5bea in `test_author_portfolio`.

## What is deliberately absent

Read this before "finishing" anything below — each is an unresolved design
question, not an oversight, and inventing an answer is the failure mode the
spec names repeatedly.

- **Paired siege advantage.** Giant/Outpost, Ghast/Bastion, Dragon/Spike are
  canonical; the *effect* is OPEN. `LairLifecycle` records a `SiegeOpportunity`
  and applies nothing. Direct objective damage is **not** restored.
- **Boss XP, objective-toppling XP.** Not awarded. The old tiered
  XP-by-method hierarchy is obsolete and must not come back.
- **Worksite tier packages.** No Blast Furnace, Smoker, Enchanting Table or
  Anvil is placed. The tier is recorded and reported UNRESOLVED; the existing
  Mining Outpost / Industrial Enchanter work is the specific form.
- **`alpha.lair.site: []`.** Empty by design. No map here has a certified Lair
  socket, and the runtime reports UNCONFIGURED rather than picking a centre.
- **Objective form measurements.** `objective_forms.certify` fails every
  candidate today, on purpose. `end_tower` is marked HISTORICAL and was
  deliberately **not** renamed to `end_spike`.
- **Physical toppling validation.** `recordValidatedToppling` exists; nothing
  calls it.
- **Post-Dragon cadence.** `UNSCHEDULED`.

## Next step, and the blocker under it

The immediate technical step is **runtime verification of the boss encounters
in a disposable world**: bind a Lair socket, `skip` through the six nights, and
confirm spawn, persistence across a Worksite night, replacement, kill-to-dormant
and reset cleanup. A vanilla Giant has no designed encounter AI and an Overworld
Ender Dragon may carry arena/flight assumptions — the seams are honest about
that, and no AI or tuning was invented to hide it. Use `skip`, not six nights of
elapsed gameplay.

The highest-value **measurement** is Lair access parity, A_L, from both teams.
It has never been computed, because no candidate has a Lair socket. It matters
more than it looks: it is the one parity claim the new doctrine actually
endorses, so it gives the balance work a target that survives §1.

And the blocker is still the one the rescue test left:
`tools/analysis/map_authoring_optimizer.py` still optimizes `balance_asymmetry`,
which on 930010639 drove the scalar to 0.0161 while leaving the 0.3173
accessible-land gap exactly where it was — it measures travel-cost equality to
*placed* opportunities, not the terrain. `compare_seeds` has been reconciled to
§1; the optimizer has not, and its two pre-existing test failures sit in that
same path. Reconciling it is the natural next migration, and the question
underneath — **whether a large accessible-land gap is ever felt in play** —
remains a playtest question no analysis here can answer.
