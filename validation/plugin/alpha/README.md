# Alpha 0.1 lifecycle verification

`lifecycle.cjs` drives the complete Alpha lifecycle against a live Paper server
with one real player, using only the admin commands a solo tester would use.
`lifecycle-2026-09-20.log` is the run this was verified with.

The run exercises, in order: instance load from the frozen template, team
assignment, match start with homeland spawn, all four sunsets and three
sunrises, Fountain disable, elimination, the real victory predicate firing,
match end, and world restore.

Two pre-existing Phase 1 defects surfaced during the run and are **not** fixed
here; they are reported in the implementation report rather than silently
rewritten. Both are filtered out of the saved log for legibility and are present
in the raw server output.

## Match-scoping verification, 20 September

`alpha-proof.cjs` drives mutate -> reset -> match 2 for Routes, InfraMode,
Contributions, Worksites and Renewables. `match-scoping-2026-09-20.log` is the
run.

Proven non-vacuously: Infrastructure Mode (`inInfraMode` 1 -> cleared -> 0) and
Contributions (`1 capitalization(s)` cleared, match 2 worksites all dormant).

**Not** proven non-vacuously, and deliberately not claimed:

- **Routes.** No Route was created, so `Routes.reset()` ran over empty
  collections. A Route needs two banner interactions with tracked travel
  between them, and the frozen map has no banners at known coordinates.
- **Renewable harvest.** `crop-proof.cjs` could not break a crop. The crops
  exist -- a direct region-file read finds `minecraft:carrots` at
  -2388,92,-116 and its neighbours, four blocks out from the `carrots_0_3`
  centre, which is the patch's water column. The failure is the bot harness
  not receiving those chunks, not the plugin.
