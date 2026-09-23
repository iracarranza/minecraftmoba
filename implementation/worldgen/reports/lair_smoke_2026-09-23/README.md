# Lair lifecycle smoke test — 23 September 2026

Disposable Paper 1.21.11 server, real `Lair` / `LairLifecycle` classes driven by
a throwaway probe plugin. Not a reimplementation: the probe calls the shipped
runtime, so a defect here is a defect in the runtime.

## What it exercised

The six opportunity nights in order, with the Giant deliberately left alive
through Worksite II, the Ghast deliberately moved 300 blocks off its socket
before Worksite III, and the Dragon killed at the end.

## Result before the fix

`before-fix` behaviour, reproduced in the first run:

    night=5 ghastSurvivesWorksiteNight=false distanceFromSocket=-1

The lifecycle reported ALIVE while the runtime could not find the occupant.
`Bukkit.getEntity` returns null for an entity in an unloaded chunk, and the only
fallback was to load the SOCKET chunk -- the one place a flying boss is least
likely to be. `sweepTagged` then scanned loaded worlds only, so it missed it and
reported success. A replaced Ghast would have stayed in the world alongside the
Dragon that replaced it, and nothing would ever have looked for it again,
because match state is memory-only.

Classification: **LIFECYCLE DEFECT.** It blocks the compiler pass, so it was
fixed rather than recorded.

## The fix

`Lair` now tracks the occupant's last known location, refreshes it before any
replacement decision, and on removal tries the entity, then that location's
chunk, then the tagged sweep -- and if all three fail, queues the id and removes
it on `ChunkLoadEvent`. Unreachable is not gone. `report()` discloses
`(occupant unloaded)` rather than reporting a bare ALIVE.

## Result after the fix

See `after-fix.txt` and `lair-log.txt`:

    [lair] occupant 39946175-... is in an unloaded chunk; queued for removal on chunk load
    [lair] removed stale occupant on chunk load

and, with every chunk a boss had occupied force-loaded first so the count cannot
hide an unloaded entity, `leftover bosses after reset=0`.

## Other findings

- **Giant, Ghast and Dragon all spawn and persist** in an Overworld socket. The
  Dragon spawns and is killable outside the End; no arena assumption prevented
  it. VANILLA-ENGINE observation, not a defect.
- **Encounter quality was not tested** and is not claimed. No players were
  present, so the vanilla Giant's lack of designed AI is still an
  ENCOUNTER-DESIGN DEFICIENCY, recorded rather than hidden behind an invented
  combat redesign. It does not block the compiler.
- **The plugin refused to start in a world it was not shipped for.**
  `MapConfigurations.reload` throws when an Alpha map diff named in
  `alpha.configurations.include` is absent, which took the whole plugin down on
  a clean server. Recorded as a hard-coded Alpha assumption (spec §20); the run
  proceeded with the catalogue disabled.
