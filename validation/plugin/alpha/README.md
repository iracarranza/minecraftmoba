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
