# Generated map to real match — 23 September 2026

Branch `codex/spatial-cadence-migration`, over the compiler slice
([`2026-09-23-map-compiler-slice.md`](2026-09-23-map-compiler-slice.md)).
Doctrine applied: [`Match Lifecycle and Objective-System Decisions`](../design/MATCH_LIFECYCLE_OBJECTIVES_AND_OPENING_DECISIONS.md).

The previous pass proved the compiler can make maps. This one closes the loop:
**a generated map now goes through the real match runtime, start to finish.**

A live disposable-server run on generated seed **2718281** entered PRE-MATCH,
resolved a map selection, atomically claimed the realization, bound its world,
played all six cadence nights with nothing reporting UNCONFIGURED, ran Lair
assaults onto the paired objectives, toppled a Bastion with all four siege
routes contributing to one shared state, and retired the map to USED.

---

## 1. Current pipeline

```
seed -> generate -> harvest -> recognize -> homebase -> hinterland
     -> objectives -> lair -> author (objectives + Lair) -> verify (columns + readback)
     -> READY certification -> READY inventory
                                    |
/moba match start -> PRE_MATCH -> map selection -> claim (READY -> IN_USE)
     -> bind generated world -> ACTIVE PLAY -> cadence + objectives + Lair
     -> match end -> USED
```

The stages the compiler gained are `ready`, and the runtime gained `PRE_MATCH`.

## 2. The six decisions, reconciled

| Decision | Implementation state | Remains open |
|---|---|---|
| **1. Map lifecycle** | Foundry publishes hidden realizations with provenance; `/moba match start` creates a match and enters PRE_MATCH rather than teleporting; a claim happens only after a selection. Entries expose Map Type / Scale / Resource Density as broad classification. | Draft order, rotation size, class-vs-map ordering, option counts. Resource Density is recorded `unmeasured` rather than guessed. |
| **2. Certification** | `VerifiedMap` and `READY` are separate: `Compilation.verified` is the compiler result, `Compilation.ready` adds runtime bindings. No `BalancedMap` status exists. | Empirical validation — nothing has been played. |
| **3. Objective toppling** | `DefensiveCapacity`: one finite state per objective, reduced by combat, structural siege, signature and Lair assault. Capacity derives from the blocks actually authored. | Defender counts, wave cadence, structural-integrity algorithm, signature magnitudes. |
| **4. Lair reward** | A defeated monster performs a siege on the paired enemy objective, reducing the same capacity. No percentage buff, no separate health model. Fires on the kill. | Siege magnitude, targeting, presentation, and the physical block/defender effects. |
| **5. Lair encounters** | Vanilla Giant, Ghast and Dragon spawn and persist at the generated Lair; no bespoke AI was added. | Giant AI, Ghast/Dragon tuning. Recorded, not designed around. |
| **6. Opening ceiling** | Compiler retains the compact-Hinterland check and the village/carrot/iron exclusions. | The strategic-consequence classifier; the exclusion list is a seam, not the whole ceiling. |

Two decisions were deliberately **not** implemented beyond a seam: the monster
siege currently moves capacity but does not yet destroy real blocks or strike
defenders, and the defender identities (Pillagers, Piglins, Endermen) are not
spawned. Both are named in §12.

## 3. The Lair

**Manifestation.** Nine irregular standing stones, each on its own terrain
height, plus a plinth at the socket centre. Deliberately minimal: doctrine says
the encounter space is the natural 3D terrain, so nothing is flattened,
excavated, enclosed or made circular. The markers are jittered in both angle and
distance specifically so the treatment cannot be mistaken for a decision that
the Lair *is* a stone circle. All of it is marked PROVISIONAL.

The one load-bearing part is the anchor: the plinth top plus one, which is what
the runtime binds to and spawns from.

**Verification.** Read back from the world: the anchor is open air, it sits on
solid ground, and all nine markers stand. This proves physical and runtime
feasibility. It says nothing about encounter quality, which is empirical.

**Runtime binding.** `MapBindings.lairAnchor` reads the manifest. On seed
2718281 the Lair bound at 380.5, 64, 468.5 and hosted Giant, Ghast and Dragon
successively at that same site.

## 4. Objectives

Forms unchanged from the previous pass: vanilla watchtower NBT, Bridge Bastion
body without the projecting bridge, measured arena spike. Contracts still derive
from the authored template, so verifier and builder cannot drift.

**Defensive Capacity.** One state per objective. Capacity comes from the
authored block count — watchtower 1156, Bastion 22,721, spike 3257 — so a
Bastion is harder to exhaust than a watchtower because it is twenty times the
building, and no second table has to be kept in step with the meshes.

Structural damage is a **share of the structure**, not a flat per-block rate. The
flat version multiplied by a capacity already derived from the block count, and
2000 blocks removed from a 1156-block watchtower exhausted it twice over — a
number that cannot happen.

The live run toppled the south Bastion with combat 181.8 + structural 320.0 +
signature 316.2 + Lair assault 999.7 = 1817.7, exactly its capacity. Four
routes, one siege.

## 5. Lair assault

Giant → enemy Outpost, Ghast → enemy Bastion, Dragon → enemy End Spike, all
reducing the same Defensive Capacity rather than a parallel model. Observed
live: the Giant kill took the south Outpost 92.5 → 41.6, the Ghast kill took the
south Bastion 1817.7 → 818.

It fires **on the kill**. An earlier version compared `lastVictory` across nights
and never fired at all, because replacing an occupant does not change the
recorded victory — the reward was recorded and silently dropped.

Magnitude is PROVISIONAL_ALPHA (55% of capacity). It is major progress rather
than a scripted topple, and because it acts on the objective's real current
state it can finish one players have already weakened, which is the setup
doctrine explicitly permits.

## 6. READY contract

What now prevents an invalid map entering READY: `readiness.certify` requires a
world identity, both Homebases, both Fountains, all six objective bindings, a
**singular anchored Lair**, and a Worksite portfolio covering three nights. The
foundry refuses anything that does not certify.

The specific case the spec named — physically verified plus unconfigured Lair —
is now `LAIR_UNCONFIGURED` and is tested.

## 7. Pre-match

`/moba match start` creates the match and enters `PRE_MATCH`. It claims nothing
and loads nothing; the previous version chose the map before anyone could be
asked, leaving nowhere for a draft to happen.

`/moba match options` lists READY realizations by id. `/moba match select [id]`
is the **TEST/DEBUG** resolution path, announced as such in its own output, and
`/moba match start-test` bypasses the participant requirement and nothing else.
Both are named so they cannot quietly become production rules.

Unresolved and left as seams: ban order, option counts, whether class or map
drafting comes first, and what a team is shown.

## 8. Live match test

Generated seed **2718281**, realization `2718281-93d1fb5c9f`, provenance
`worldgen_truth.verified = false` (vanilla terrain, not the official-jar SHA-1).

| step | evidence |
|---|---|
| PRE_MATCH entered | `state=PRE_MATCH preMatch=true`, world not yet bound |
| options offered | two READY realizations listed |
| selection claimed | `READY=1 IN_USE=1 USED=0` |
| world bound | `alpha_match` from the claimed realization |
| homelands | north −180,68,292 / south 236,65,44 — from the manifest |
| objectives | 3/3 sited per team, six distinct positions |
| Lair | anchored 380.5,64,468.5 |
| Worksites | 12 bound |
| ACTIVE PLAY | `state=RUNNING` |
| nights 1–6 | Worksite I (2) · Giant · Worksite II (2) · Ghast · Worksite III (3) · Dragon |
| UNCONFIGURED | **none at any point** |
| Lair assaults | Giant→Outpost, Ghast→Bastion observed |
| toppling | Bastion by four routes summing to capacity |
| retirement | `READY=1 IN_USE=0 USED=1`; used map no longer offered |

Full log: `implementation/worldgen/reports/lifecycle_2026-09-23/`.

## 9. Foundry

Job mode publishes until a target READY depth, an attempt bound, or the
candidates run out, and says which. Over 30 unseen seeds it published 2 in
**88.1s of compile time (44.1s per READY map)** — excluding world generation,
which remains the dominant cost at ~70s per seed.

Rejections: `NO_DEFAULT_REGIONAL_SHAPE` 23, `SOCKET_NOT_INDEPENDENTLY_ACCEPTABLE`
3, `LAIR_ACCESS_DISPARITY` 1.

**Cheap pre-screen: measured, not adopted.** A stride-4 lattice (~286 of 4320
chunks) reproduces the regional-shape decision with **zero false negatives** over
all 30 seeds and correctly discards 30%. The first version had a false negative
on 99887766 — one of the two maps that compiled — because it assumed the highland
end was low-x while the real screen tries all eight orientations; hence
`best_gradient` over four axis directions.

It is **not wired into the foundry**. The 30% discard rate is measured; the
saving is not, because scattered chunk generation is much less efficient per
chunk than a contiguous window and could plausibly eat the benefit. Claiming a
speedup without measuring the screen's own cost is the kind of number this
project keeps having to retract.

## 10. Empirical instrumentation

`MatchRecord` writes a TSV beside the realization that produced it: map id and
seed, the compiler's predictions (Lair anchor, Fountains, Worksite count), each
night with its stage and activations, every capacity change with its route,
Lair contests, any UNCONFIGURED, and the winner and duration.

Enough to put a compiler prediction and a match outcome side by side, and
nothing more. One match is evidence, not a mandate; nothing retunes itself.

## 11. A_L revisited — the justification has expired

Held at **0.15**, but its basis is gone and that had to be recorded.

It was chosen from eight finalists where best-achievable A_L ran
`0.001 0.050 0.073 | 0.226 0.294 0.415 0.678 0.787`; the widest gap by far was
0.073–0.226 and the recorded claim was that any bound inside it selected the
same three seeds.

With **31 seeds** the distribution is continuous — `0.000 ×3, 0.001 ×4, 0.022,
0.038, 0.050, 0.068, 0.073, 0.119, 0.173, 0.226, …, 1.890` — and there are now
values at 0.119 and 0.173 *inside* the gap that justified the number. Sensitivity
is smooth: 0.10 admits 39%, 0.15 admits 42%, 0.20 admits 45%.

The value is unchanged, because more data is not by itself a reason to move a
number. What changed is its status: **0.15 is now a judgement about tolerable
access disparity, not a break the evidence hands us.** It stays
PROVISIONAL_ALPHA and the first playtests should settle it.

## 12. Open design

Genuinely unresolved, not deferred implementation:

- Draft order, rotation size, option counts, class-vs-map ordering.
- Defender counts, wave cadence, replenishment, the structural-integrity
  algorithm, signature magnitudes, monster siege magnitude and presentation.
- Whether the monster siege should destroy real blocks and kill real defenders
  rather than only moving capacity. Doctrine says it should; the magnitude and
  targeting are open, so the physical half is a seam.
- Defender identities (Pillagers, Piglins, Endermen) are specified but not
  spawned; wave replenishment is not implemented.
- Giant AI, Ghast and Dragon encounter tuning.
- Resource Density bounds and measurement.
- Every competitive threshold.

## 13. Next step

Between here and "every real match receives a unique generated verified map
through the intended draft/foundry lifecycle":

1. **Play one.** Everything downstream is now blocked on evidence rather than on
   engineering. The instrumentation exists precisely to make the first match
   worth running.
2. **Spawn defenders.** The combat route currently moves capacity without
   anything to fight; Pillagers, Piglins and Endermen with replenishment are the
   next real system.
3. **Make the monster siege physical.** It should destroy structure and strike
   defenders, not only reduce a scalar.
4. **Decide the draft.** The state machine and seams are in place; the rules are
   not, and the test path must not be allowed to become them.
5. **Measure the cheap screen's cost** and adopt it only if it actually saves
   time.
6. **Keep the pool stocked.** Yield is 6.7% and world generation dominates;
   a foundry that runs unattended needs that to be cheaper or slower-and-fine.
