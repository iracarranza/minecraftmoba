# Handoff — the game can use the maps it makes

**23 September 2026. Branch `codex/spatial-cadence-migration`.** Audit:
[`docs/audit/2026-09-23-generated-map-lifecycle.md`](docs/audit/2026-09-23-generated-map-lifecycle.md).
Doctrine: [`Match Lifecycle and Objective-System Decisions`](docs/design/MATCH_LIFECYCLE_OBJECTIVES_AND_OPENING_DECISIONS.md).
The compiler's own audit is
[`2026-09-23-map-compiler-slice.md`](docs/audit/2026-09-23-map-compiler-slice.md).

Not merged to main. Base 03c5bea over 9bb5093 over main 8199c13. Nothing here
touched `/Users/iracarranza/minecraftmoba` or the live `/private/tmp/alpha-server`;
all server work used disposable directories.

## Deepest level reached: **F — complete lifecycle**

Live disposable-server run on generated seed **2718281**:

```
READY -> /moba match start -> PRE_MATCH -> test selection -> claim (IN_USE)
      -> generated world bound -> ACTIVE PLAY -> six cadence nights
      -> Lair assaults -> objective toppled -> match end -> USED
```

No system reported UNCONFIGURED at any point. Evidence in
`implementation/worldgen/reports/lifecycle_2026-09-23/`.

## The defect this pass existed to fix

A map could pass every physical check and enter the pool with its Lair socket
chosen, measured, and never actually established. The compiler was not wrong
about the geography — **nothing was asking whether the runtime could bind to
it.**

So VERIFIED and READY are now different claims. `readiness.certify` requires a
world identity, both Homebases, both Fountains, all six objective bindings, a
singular anchored Lair, and a Worksite portfolio covering three nights. The
foundry refuses anything that fails. `LAIR_UNCONFIGURED` is a tested rejection.

## What is new

- **The Lair is manifested** — nine irregular standing stones on their own
  terrain heights and a plinth anchor. Minimal and marked PROVISIONAL, because
  the terrain is the encounter. Verified by readback.
- **Runtime bindings come from the manifest.** `MapBindings` reads the claimed
  realization; config is the Alpha template's fallback, not the default.
- **`PRE_MATCH` is a real state.** Match start claims nothing and loads nothing.
  `/moba match select` and `start-test` are TEST/DEBUG paths, named so.
- **Defensive Capacity** makes combat, structural siege, signature and Lair
  assault one system. Capacity derives from the blocks actually authored.
- **The Lair reward is a siege**, not a buff, and fires on the kill.
- **`MatchRecord`** writes a playtest record beside the realization.
- **Foundry job mode** publishes to a target depth with a bound.

## Two findings worth carrying

**The A_L justification expired.** Held at 0.15, but with 31 seeds the
distribution is continuous and there are values at 0.119 and 0.173 inside the
gap that justified it. The value is unchanged; its status is not — it is now a
judgement about tolerable disparity rather than a break in the data.

**The cheap pre-screen is measured but not adopted.** Zero false negatives over
30 seeds, 30% correctly discarded. Not wired in: the discard rate is measured
and the *saving* is not, because scattered chunk generation may cost more than
it saves.

## Next step

1. **Play one.** Everything downstream is blocked on evidence, not engineering.
2. **Spawn defenders** — Pillagers, Piglins, Endermen with replenishment. The
   combat route currently moves capacity with nothing to fight.
3. **Make the monster siege physical** — destroy structure, strike defenders,
   not only reduce a scalar.
4. **Decide the draft.** Seams are in place; the test path must not become the
   rule.
5. **Measure the cheap screen's cost** before adopting it.

Still open: draft rules, defender counts and wave cadence, structural-integrity
algorithm, signature and siege magnitudes, Giant AI, Ghast/Dragon tuning,
Resource Density bounds, and every competitive threshold. All PROVISIONAL_ALPHA.

Tests: Java 256, worldgen 401, all passing.
