# Map Type doctrine, and the draft order

**24 September 2026.**

## What was decided

`maps.md` gains a **Map Type doctrine** section, replacing the three-line
"Working map archetypes" preamble. The candidate Type catalogue is recorded as
**Working**. Nothing is promoted to Established.

`docs/design/MATCH_LIFECYCLE_OBJECTIVES_AND_OPENING_DECISIONS.md` had the draft
order explicitly open — "Class-first, map-first, and interleaved drafts have
materially different strategic consequences and should be decided
deliberately." It is now **class-first, then map ban/counterpick**. Ban order,
option-set overlap and final-choice rights remain open.

## What this reconciles

`maps.md` described Map Types only as "future archetypes, not implemented
recognizers" and had no Scale axis. The lifecycle design already had Map Type,
Scale and Resource Density as the three advertised properties, and already
established that Resource Density is discovered and measured rather than
requested. The two documents were not in conflict, but `maps.md` was the
canonical source for maps and held the weaker account.

The doctrine section now states Scale as an existing independent axis and
points at the lifecycle document for the information boundary, rather than
inventing a second Scale model.

## What changed because of measurement, not preference

Three items in the doctrine came from this session's measurements and would
have been written differently without them.

**Symmetry and extremeness are separate axes.** Mirror deviation scales with
relief almost proportionally over 1,073 scoops, so ranking raw symmetry is
ranking flatness, and the flattest places are water. A blind search on a
31%-ocean seed returned 97-99% water in its top three. Symmetry is gated on
deviation/relief; relief is recorded and not ranked.

**Symmetry is measured over contested ground.** Water matches water exactly,
so a wet scoop gets half its pairs free. Whole-scoop ratio falls as water
rises (0.246 to 0.182); restricted to land it reverses and nearly closes
(0.269 to 0.296).

**Per-Type symmetry bounds are deferred, not adopted.** Most of the apparent
need for them was the denominator error above. The ~10% residual is recorded
as OPEN rather than turned into fifteen bound-sets that would encode an
artefact.

## Design-state labels

- Map Type doctrine, the two axes, contested-ground measurement: **Working**.
- Candidate Type catalogue: **Working**.
- Per-Type symmetry bounds; forest canopy as contested ground; topological
  legibility: **Open**.
- Class-first draft order: **Working**; it supersedes the open choice.
- Default's regional gradient remains its own search contract and is not
  generalised by this section.
