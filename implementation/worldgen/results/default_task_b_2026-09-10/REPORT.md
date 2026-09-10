# Task B physical greyboxes and validation checkpoint

Authority: `specs/mapseedsearchspec3.md`. The user selected **930010639 primary**
and **930012642 comparison/fallback**, then explicitly accepted the remaining
readiness questions. The A→B gate is satisfied for this physical experiment;
the earlier blocked-gate artifact remains historical evidence, not current state.

## Completed stages

**B.0 completed for both candidates.** Each is an independent copy of its existing
vanilla world, loaded with the pinned official Minecraft Java 1.21.11 server and
Java 21. Vanilla `setblock` commands provide approximate homeland-boundary posts,
Fountain placeholders, departure markers, three narrow Starter paths per team,
modest stair treatment and yellow handoff markers. Full Wilderness corridors are
not built. No objectives, Worksites, ecology, resources, final architecture or
macro terrain corrections were introduced.

**B.1 partially completed:** actual-block construction/readback, Fountain clearance,
Starter spine support/step/headroom checks, full unsupported corridor profiles,
sample-spine water depths, accepted-link profiles and preservation verification.
The matching dedicated server successfully loaded both physical worlds. Normal
player traversal in the graphical client was **not completed**. Native automation
reached the launcher but did not establish a matching 1.21.11 traversal session.
See `client_validation.json`. The block renders are explicitly not in-game
screenshots or evidence of a successful walk test.

## Candidate evidence

| Measurement | 930010639 primary | 930012642 comparison/fallback |
| --- | ---: | ---: |
| Planned physical block edits | 1,797 | 1,928 |
| Vegetation/overhead blocks cleared | 259 | 379 |
| Unexpected readback states at edited positions | 0 | 0 |
| Starter spines passing conservative block checks | 5/6 | 6/6 |
| Fountain anchors with dry support and two air blocks | 2/2 | 2/2 |
| New/removed Overworld chunks | 0/0 | 0/0 |
| Competitive candidate pass | Not established | Not established |
| Structural rejection | Not established | Not established |

### 930010639

North-2's fitted Starter crosses an **eight-block dip** near world X/Z
`1840, -244`, followed by three- and four-block rises. Its A.1 maximum sampled
rise was only two blocks. This exposes a real between-sample obstacle; the current
physical Starter is not reliably traversable along the reference spine.

A read-only search identified a nearby dry one-block-step alternative within a
20-block search buffer. The candidate alternative has 80 steps, matching the
80-step reference, and is recorded in `validation.json`. This supports a **local
discrepancy** classification rather than manufacturing a structural rejection.
The alternative is **not applied**: it remains subject to the complete B.1 walk
and permitted B.2 review. No pit filling or terrain flattening was performed.

The other five Starter spines passed the block checks. Unsupported continuations
still contain steep steps, including 16 on South-1 and eight on South-2. North-3's
continuation crosses nine sampled columns of one-block-deep water. The accepted
link-1 reference is dry with at most one-block adjacent rises, but its strategic
shortcut value and perceptual legibility remain unvalidated.

### 930012642

All six Starter spines passed the block checks. This is a construction/readback
result, **not** a full competitive pass or an automatic promotion over the primary.
The accepted 55.921-effective-block South-1 handoff remains unchanged.

Wilderness continuation is not pre-solved: South-3's reference encounters 32
greater-than-one-block adjacent rises and a 20-column water run reaching eight
blocks deep. No infrastructure was extended into that unsupported corridor.
Link-1 includes a two-block step near `-2148, 305`; its inferred shortcut role
has not been proven by player traversal. These findings require route-aware
inspection, not arbitrary repair or automatic rejection of the vanilla terrain.

## Corrections and stopping condition

B.0 path surfacing, stairs, limited vegetation clearance and debug markers are
initial materialization, not B.2 spatial corrections. **No B.2 corrections were
made.** Bounds, orientation, homeland/Fountain X/Z anchors, Route relationships
and Starter termini remain the accepted Task A geometry.

Section 27 places B.2 after the complete B.1 walk. That prerequisite is not met.
Neither candidate is declared a structural failure merely because it is not yet
fully validated. Neither has passed all physical validation criteria. Therefore
**no competitive skeleton is frozen**, and Task C has not started.

## World and artifact paths

Relative to the repository root:

- Primary: `artifacts/worldgen/default_task_b_2026-09-10/930010639/world/`
- Comparison: `artifacts/worldgen/default_task_b_2026-09-10/930012642/world/`
- Clean sources remain under `artifacts/worldgen/staged_default_2026-09-09/worlds/`.
- Per-candidate `build_plan.json`, `apply.mcfunction`, `undo.mcfunction`,
  `server.log`, `server_validation.json`, `validation.json`,
  `preservation_audit.json`, `north_block_readback.png`, `south_block_readback.png`.
- `summary.json`: candidate states, source-world hashes, chunk counts and stop gate.

Bulk worlds are local/gitignored; committed plans and command lists reproduce
the edits against the existing clean worlds. Undo commands cover authored blocks,
not ordinary server metadata changes; restoring the untouched source copy is the
cleanest full rollback. Never apply these functions to the clean inspection worlds.

## Verification commands

```sh
python3 implementation/worldgen/build_task_b_greyboxes.py
python3 implementation/worldgen/audit_task_b_blocks.py
python3 -m unittest discover -s implementation/worldgen/tests -v
```

The builder refuses to overwrite an existing physical build. On a completed run
it revalidates existing worlds without writing them. The all-chunk audit compares
actual block states across the complete existing Overworld, not just edited
positions. Source-world file hashes are checked before/after. Server sessions
listen on localhost and shut down cleanly after saving.

## Required next validation pass

Open **copies** of these worlds in Java **1.21.11**, not the launcher's currently
selected newer release. `TASK_B_INSPECTION.json` in each world records Fountain,
departure and terminus coordinates. Blue/orange posts identify team geography;
yellow posts identify provided-infrastructure handoffs; sea lanterns mark Fountains.

For each team, complete the spec's opening, construction-reality, unmarked
Wilderness-continuation, competitive-interaction and geographic-depth walk tests.
Record screenshots and route-specific notes before modifying anything. In
particular inspect the primary North-2 dip and nearby alternative, the comparison
South-3 deep-water continuation, and both candidates' central/lateral geography.
Only then decide local B.2 correction versus structural rejection and possible freeze.

The all-chunk preservation audits found expected transient server-side fluid
flow, bubble columns and leaf-distance metadata, plus a small unresolved set of
unplanned state differences (26 for 930010639 and 6 for 930012642). These are
listed separately in each `preservation_audit.json`; planned edit positions
read back exactly, but the all-chunk preservation pass is not clean. Clean
source worlds are untouched. This discrepancy is deferred for follow-up rather
than silently attributed to terrain or used to justify B.2.
