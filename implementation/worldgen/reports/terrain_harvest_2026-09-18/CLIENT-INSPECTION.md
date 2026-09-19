# Client inspection — 19 September 2026

**First human inspection of a harvested volume.** Every prior report in this
directory states that no client walkthrough had occurred and that a server load
never implies graphical inspection. That gap is now closed for one volume.

Inspected: `tv_ef56852eda10acc88342e5ee`, seed 930012642, whole_map,
source x [-2480, -1617], y [-64, 319], z [-528, 527], rotation 0.
Java 1.21.11 client, adventure mode, via `/function harvest:hub` then
`harvest:next` and `harvest:visit/`.

## Observer verdict

Terrain is **good** and the map reads well overall. Legible as a place in some
areas, though not enough to pass as authored terrain. The characteristic
impression is **"strangely well-delineated biomes"** — boundaries read as
sharper than authored landscape would be.

The bedrock envelope is acceptable and not an issue at this stage. Water is
fine. No broken or diorama-like presentation was reported.

## What this establishes

Harvesting produces a usable map object. A clipped whole-map candidate reads as
a contained world rather than a sliced fragment, which was the open question
that all the byte-level and protocol evidence could not answer.

## What it does not establish

This is one observer, one volume, one session, in adventure mode. No selection
decision follows: Practical Reach and final acceptance remain UNRESOLVED, and
the volume's own recorded Stage C hard failures list stays empty rather than
verified. The scoop and the second whole-map volume were not inspected.

## Defect found: the index is unreadable in practice

`harvest:index` prints one long line per volume containing id, seed,
classification, full source bounds, rotation, evidence path, Stage C failures
and the unresolved-acceptance note. On screen this fills the chat with wrapped
text, and repeated `visit` calls reprint the same block. The observer did not
realise the entries were clickable.

The information is correct and deliberately complete; the presentation defeats
it. A compact clickable label with the detail in a hover tooltip would keep the
provenance without burying it. Recorded rather than fixed here.
