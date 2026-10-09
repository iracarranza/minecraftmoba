# Legibility bench

**Prototype/test. Built 9 October 2026.** A lab bench for how a state READS to someone
looking at it from a distance. It exists because the settled doctrine ("Global Effect
Legibility", `classes.md`, settled 1 October 2026) makes glow the universal signal and
class presentation the specific one, and neither had been looked at. It tests the
mechanisms the Werewolf presentation investigation found reachable without art:
scale (`Attribute.SCALE`), glow, worn armor and particles.

```
/moba lab legibility            enter
  scale | glow | armor | particles | clutter | mark | report | clear | leave
```

Everything inside is on the hotbar, on a deck of nine world buttons west of the track, or
typed; Leave is in the inventory. It uses `BenchUi`, the generic form of the combat
chamber's UI, so the next bench supplies a menu and a deck instead of rewriting them.

## What it is

A 107-block track of stone with a stripe every 8 blocks, a subject (a body) at one end, and
the tester 16 blocks out looking at it. Each press cycles one axis: scale 1.0, 1.25, 1.5,
2.0, 3.0 (1.0 is the control); glow on or off; armor none, leather, iron, diamond,
netherite; particles none, flame, soul fire, end rod; clutter on or off (a seeded scatter
of pillars between observer and subject, so two tries see the same obstruction).

## What is measured, and what is judged

- **Computed:** distance, the angle the subject subtends, and its height in pixels for a
  stated FOV and screen height (default 70 and 1080p, to be set to the tester's own).
- **Judged:** "it still reads." The tester walks away until it stops reading and presses
  Mark; the distance is recorded against the variant. The report gives each variant's
  marks as min, median and max, and the pixel height at the median, so a scale step is
  judged by what it buys. The median is used so a stray mark does not move the answer.

## What the glow is, and is not

The doctrine's Tier 2 glow is OBSERVER-RELATIVE: an ally always sees it; an enemy sees it
only in Combat, within the Empowerment Legibility Radius, and when the subject is not
Invisible. The platform's glow flag is the same for every viewer, and per-viewer delivery
is an open technical risk. So the glow on this bench is the universal flag, which is
Tier 1's behavior. `Legibility.glowVisible` is the RULE as a pure, tested function and
the report prints what it would show an ally, an enemy in combat at the tester's distance,
and an enemy out of combat, beside what is actually displayed. It does not claim they are
the same. The radius is not set in canon; 24 blocks is a PROTOTYPE value.

## Not a class

The subject is a plain body wearing a variant, not a Werewolf. No Werewolf kit exists in
the plugin, and forms are not yet the question. When one does, it becomes a variant.

## Verification

Arithmetic, variants, marks, the track, the menu and the deck: unit-tested. Live (real
server, real plugin): entry and set-up, hotbar scale, the GLOW button, armor and particles
and clutter, marks and the report, the doctrine lines, and leaving: 71 of 71 with the
combat acceptance run. Running it found a real defect, since fixed: non-persistent
entities (the deck's buttons) are discarded when their chunk unloads, and nothing held the
bench's chunks, so the controls vanished within ticks; the bench now keeps its chunks
loaded while occupied. **Not verified:** that particles render and that a client SEES the
glow and the scale. Those need a person at a client. The bench is for that.
