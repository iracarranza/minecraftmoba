# Prototype/test: chamber workspace

A chamber is a bounded bay of the lab world that one tester owns, so a
placement experiment has somewhere it cannot escape from. That is what makes
"regenerate the terrain here" and "undo everything here" answerable.

```
/moba lab chamber            allot a bay and go to it
/moba lab chamber leave      hand it back
/moba lab chamber confirm    commit the pending placement
/moba lab chamber cancel     discard it
/moba lab chamber status     bay, allotment count, what is pending
```

Chambers exist only inside a **launched scoop**, not in the lab room. A bay is
terrain a tester edits, and the room is Adventure-mode and block-protected —
offering a chamber there would be offering somewhere to build that refuses
every block.

The workspace is created per launch and dropped in `teardown()` **before** the
world unloads, so a bay never outlives the terrain it was cut from.

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

## Entering: the observation platform

The chamber is a **place**, not a command. A tester walks to a viewing
platform overlooking the bay, cycles what terrain is in it, and then walks in
through a door; the terrain they are about to work on is visible before they
commit to it, which is the thing a command line cannot show.

## Scoop source: two buttons, never one

[Resolved] The platform offers **two** separate controls, because regenerating
a bay has two meanings that must not be confused:

| Control | What it puts in the bay | What it answers |
|---|---|---|
| **Certified scoop** | a pre-staged scoop `LabMaps` copies and verifies by fingerprint | does this placement work on ground we ship? |
| **Random seed** | rough vanilla terrain, generated at runtime | does this placement survive ground nobody vetted? |

They are never the same button with a toggle, and a measurement taken under
one is never reported as the other. The offline compiler cannot be asked for a
certified scoop at runtime, so **certified** means copied from what already
exists, not generated on demand.

Filters over either source are out of scope.

## The hotbar is a menu, not a loadout

A chamber has more verbs than a hotbar has slots, so the hotbar is a **view
onto a menu** the tester descends: using an item opens its page, and the last
slot climbs back out. `ChamberMenu` holds all of it and is settled without a
server; the Bukkit half only renders a view and reads a slot back.

```
root       Place objective | Place renewable | Draw route | Regenerate | Undo | Redo
objective  Fountain | Outpost | Rampart | Spike                            | Go back
pending    Show again | Place | Discard                                    | Go back
```

Three rules are enforced rather than described:

- **A page may not exceed eight entries.** A ninth is a tool the tester cannot
  reach and has no way to learn is missing, so it is refused at construction
  rather than quietly dropped.
- **Go back is always the last slot**, so descending does not move the exit —
  and is absent at the root rather than present and inert.
- **Session verbs are refused a hotbar slot at all.** *Return to lab* and *test
  as player* end or hand over the session, and a hotbar is a thing you scroll
  past by accident. They live in the inventory, where acting on one costs an
  explicit open and click. That is the misclick argument made into a failure.

An item the tester cannot currently use is **shown in its place with a
reason**, not hidden. A hotbar whose items shuffle as history changes would
break the muscle memory the fixed layout exists to give; and a silent no-op
reads as a broken button, whose obvious response is to press it again.

## [OPEN] Redo has no journal

`LabUndo` is a one-way LIFO: undoing discards. The menu declares **Redo** and
gates it, so it stays visibly unavailable until `LabUndo` keeps a forward
journal — a dim button with a reason, rather than a verb promised nowhere.
Adding the journal is what lights it up.

## [OPEN] Footprint as hologram

The preview is currently a particle outline, which is budgeted and works. A
`BLOCK_DISPLAY` ghost of the actual structure would read far better, and the
budget argument does not transfer: displays are entities with a per-entity
cost, not a per-block-per-tick one, so the reduction in `ChamberPreview` is the
wrong economy for them. It is a different mechanism, not a tuning of this one.

## Size is enforced against the journal

The shipped bay is 33 x 21 x 33, deliberately inside `LabUndo`'s
120,000-block journal so that regenerating one stays undoable. That is
**enforced**, not described: a test reads the radius from `config.yml`, the
budget from `LabUndo`, and fails if a future radius puts regeneration out of
reach. Nothing else would notice.
