# Handoff — map compiler vertical slice

**23 September 2026. Branch `codex/spatial-cadence-migration`,** over the
14cb46c cadence migration. Audit:
[`docs/audit/2026-09-23-map-compiler-slice.md`](docs/audit/2026-09-23-map-compiler-slice.md).
The previous pass's audit
([`2026-09-22-spatial-cadence-migration.md`](docs/audit/2026-09-22-spatial-cadence-migration.md))
still holds for the runtime cadence.

Not merged to main. Branch base 03c5bea over 9bb5093 over main 8199c13. Nothing
here touched `/Users/iracarranza/minecraftmoba` or the live
`/private/tmp/alpha-server`; all server work used disposable directories.

## Where the compiler stops

An unseen seed, **99887766**, passes every analytical stage — Default
recognition, both Homebase sockets, compact Hinterlands, an ordered objective
layout, and a Lair socket at **A_L 0.068** — and stops at one physical blocker:

> **There is no End Spike mesh.**

Siting uses the measured `end_spike` contract. The repository has
`aether_fountain`, `nether_bastion`, `pillager_outpost` and the historical
`end_tower`, and registering that last one under the Spike's name is the
masquerade the physical contract forbids, so the compiler fails closed.

No PlayableMap was produced. The compiler now reports where each seed died:

| stage | unseen (6) | screened finalists (8) |
|---|---|---|
| recognize | 3 | 1 |
| homebase | 2 | 3 |
| objectives | 1 | 0 |
| lair | 0 | 3 |
| author | **1** | **1** |

## What is now measured that was not

- **All three objective forms**, from Minecraft itself. Outpost 15×21×15 and
  Bastion 32-span/506-contact out of the client jar's structure NBT; End Spike
  from a generated vanilla End — radius 2–5, height 76–103, two of ten caged.
  Contracts are per-objective, not one bounding box: the Spike's binding
  constraint is 103 blocks of sky over an 11-block footprint.
- **Lair access parity.** Best A_L per seed across eight finalists: 0.001,
  0.050, 0.073, then 0.226, 0.294, 0.415, 0.678, 0.787. Bound 0.15 sits in the
  widest gap; any value in [0.08, 0.22] selects the same three seeds.
- **Homeland socket quality**, 28 sockets, 0.110–0.950 with a break at
  0.468→0.559. Bound 0.52.
- **Opportunity reach**, from which the optimizer's two gates are set.

## What this pass changed about balance

`map_authoring_optimizer` no longer optimizes `balance_asymmetry`. Measurements
are all kept; the shape is now hard gates then optimization among viable
candidates, and the gate is **reachability, not equality**.

Worth carrying forward: **930010639 — the near-miss an earlier pass authored to
a 0.0161 balance scalar — has the worst Lair access parity of all eight seeds at
0.787**, and the compiler rejects it. The metric doctrine endorses and the one
it replaced disagree about that seed completely.

## Defects found by running things

1. **Lair lost its own occupant.** A Ghast 300 blocks off-socket in an unloaded
   chunk read as ALIVE-but-unreachable, and the sweep scanned loaded worlds
   only. Fixed: last-known-location tracking plus a `ChunkLoadEvent` queue.
   Verified end to end.
2. **The recognizer rejected every candidate ever screened**, on the
   straight-line corridor proxy already known to be a false positive.
3. **Objective siting chose the best site per layer independently**, and they
   did not coexist. Now an exhaustive joint search; the Fountain is no longer a
   free slot.
4. **The socket threshold was justified from two numbers that were one seed's
   two homelands**, not a distribution. Corrected against 28 measured sockets.

## Next step

1. **Author an End Spike mesh** to the measured contract — obsidian pillar,
   radius 2–5, height 76–103, bedrock cap, End Crystal, iron-bar cage on a
   minority. This is now a specification, not a design question, and it is the
   single blocker on the deepest candidate.
2. **Column-scan verification** for the Spike's vertical clearance and the
   Bastion's interior fit — both named in
   `structures.UNVERIFIABLE_FROM_SAMPLES`, neither provable from an 8-block
   surface grid.
3. **Exercise author and verify** on 99887766 in a disposable world. Feed any
   false positive back into the recognizer rather than normalizing terrain.
4. **Widen the A_L sample.** Eight seeds is enough to see a gap, not to fix a
   threshold.
5. Then the foundry. The compiler is what makes a ready-map pool possible, and
   per-stage rejection codes are what a foundry needs to know why it discarded
   a seed.

Two caveats to carry: locally harvested candidates have
`worldgen_truth.verified = false` — vanilla terrain, but not the official-jar
SHA-1 provenance the screened finalists have. And the compact-Hinterland gate
has never fired (observed 1.4%–2.5% against a 50% bound), so it is a guard, not
evidence.

Still open and untouched: siege advantage, boss and toppling XP, Worksite tier
packages, Giant/Ghast/Dragon encounter design, Lair art direction, and every
competitive threshold after Alpha testing. The vanilla Giant still has no
designed AI; that is recorded, not designed around.
