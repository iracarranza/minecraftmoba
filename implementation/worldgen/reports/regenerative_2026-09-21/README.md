# Regenerative opportunity simulation — frozen Alpha template, 21 September 2026

Regenerate with:

```
python3 -m terrain_harvest.simulate_regenerative \
  --world artifacts/worldgen/alpha-0.1/consolidative-alpha \
  --output implementation/worldgen/reports/regenerative_2026-09-21
```

## What this is

The eligibility layer of the regenerative system, run against the real frozen
template. For each of the twelve authored opportunities it reports the
Opportunity Region, every locus currently eligible inside it, and a simulated
sequence of four manifestations.

**There are no nodes here.** The model has no permanent manifestation points, so
there is no list of them to export. What is exported is where a manifestation
*could* occur and where a simulated sequence *did* land.

## Reading a PNG

One pixel per column, one image per opportunity, region-sized (97x97).

| Colour | Meaning |
|---|---|
| dark grey | inside the region, not currently eligible |
| green | currently eligible locus |
| brown | the legacy authored footprint (pen 40-span / field 24-span) |
| orange, fading | successive simulated manifestations, first brightest |

The green is the honest content: it is the answer to "if this opportunity
regenerated right now, where could Minecraft plausibly put it?"

## Caveats that matter

- **The predicates are NON-CANON ALPHA FIXTURES.** Ground set, headroom, sample
  stride and displacement are all unresolved. This shows what they currently
  imply, not what the design has settled.
- **The template is pristine**, so player-placed rejection is never exercised.
  The export cannot show Development displacing a manifestation, which is one of
  the architecture's load-bearing properties.
- **The predicate is implemented twice** -- authoritatively in the plugin, and
  here in Python so it can run against region files without a server. Both run
  `implementation/worldgen/fixtures/eligibility-crosscheck.json` and must agree;
  `EligibilityCrossCheckTest` and `tests/test_regenerative.py` enforce it.
- Sampling uses the configured stride (4), so the green pixels are a sample of
  eligible columns rather than all of them.

## The headline measurement

The legacy pens and fields **are** over-represented among eligible loci, by
1.0x-1.6x for eleven of twelve opportunities, and 3.2x for `carrots_4_8`. That is
a real artefact and much smaller than the migration report originally asserted.

Every region retains hundreds of eligible loci (206 at the thinnest), so no
opportunity is at risk of having nowhere to manifest once the pads are removed.
