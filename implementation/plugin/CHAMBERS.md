# Prototype/test: chamber workspace

A chamber is a bounded bay of the lab world that one tester owns, so a
placement experiment has somewhere it cannot escape from. That is what makes
"regenerate the terrain here" and "undo everything here" answerable.

```
/moba lab chamber            allot a bay and go to it
/moba lab chamber leave      hand it back
/moba lab chamber status     bay, allotment count, what is pending
```

Placement follows the ability gesture rather than a second grammar: the first
press previews, the same input confirms, another input cancels. That is
`CastMode.DOUBLE`, and the chamber uses the same `AimState` an ability does.

## What the preview draws

Not every affected block. `TargetPreview` sends one particle per block per
cadence to one player, so a 200-column corridor three wide would be 600
particles a tick for as long as the tester is deciding.

So a **route** is drawn at its ends and wherever the path changes character,
and a **footprint** is drawn as edges. A budget then thins what remains,
degrading detail and never extent — the first and last points always survive,
because where a placement *stops* is the thing a tester is certain to be
reading.

## What a verdict means

A pending placement carries its own verdict, and leads with the worst thing:

| | |
|---|---|
| `NOTHING TO PLACE` | it produced no blocks; not a fit, a failure |
| `LEAVES THE CHAMBER` | some of it falls outside the bay |
| `N FAULTS` | assembly, terrain or entrance faults |
| `N BLOCKS` | acceptable |

Confirming an unacceptable placement is **refused and kept**, not discarded.
The tester pressed confirm because the preview was in front of them; making
them rebuild it to read why it was refused would withhold the thing they now
want.

## Allotment

Bays are laid out on a grid with a gutter, so non-overlap is a property of the
layout rather than a rule being enforced. A released bay is reused before the
next ring, so a lab that has been used does not sprawl.

**One chamber per tester.** A second allotment replaces the first: two chambers
would mean two undo histories and no way to say which `/moba lab undo` meant.

## [OPEN] Scoop source

Regenerating a chamber's terrain has two meanings and they must not be
confused:

- **A pre-staged certified scoop**, which `LabMaps` already copies and verifies
  by fingerprint.
- **Rough vanilla terrain** for severity testing, which no certified pipeline
  produces and which the offline compiler cannot be asked for at runtime.

Both are legitimate and they answer different questions. They want different
names, and a measurement taken in one must never be reported as the other.

Also open: a chamber of 33 x 21 x 33 fits `LabUndo`'s 120,000-block journal and
a larger one does not, so whether a regeneration is undoable is a decision
about chamber size rather than an accident.
