# Alpha template migration — required report before any re-freeze

Status: **report only.** Nothing in the frozen template has been touched, and no
re-freeze has been performed. §10 of the handoff requires this to exist first.

The runtime architecture (Region → current-terrain eligibility → locus → finite
manifestation → resolution → recovery → fresh query) is implemented and tested
independently of this. Legacy geography remains physically present in the world
and is now **inert**: nothing selects a manifestation site from a pen or a
field, so the old representation no longer functions as the model.

---

## 1. Fenced pens that would be removed or changed

All eight `renewable_range` opportunities. Each is `massing.pen(span=40)`: a
41×41 grass floor with a fence ring one block high and a one-block gap per side.

| id | kind | authored point | fence |
|---|---|---|---|
| `rabbit_0_3` | rabbit | −2448, 92, −48 | birch |
| `sheep_0_5` | sheep | −2448, 98, 208 | oak |
| `cow_0_6` | cow | −2448, 94, 336 | oak |
| `sheep_2_3` | sheep | −2192, 90, −112 | oak |
| `sheep_2_6` | sheep | −2160, 99, 304 | oak |
| `rabbit_3_6` | rabbit | −2032, 90, 304 | birch |
| `chicken_4_1` | chicken | −1904, 66, −336 | birch |
| `sheep_4_4` | sheep | −1936, 66, 16 | oak |

Removed per pen: **160 fence posts** (the ring, less four gaps), and the
**1,681-column grass floor** is the harder question — see §5.

## 2. `founder_crop` fields that would be removed or changed

All four. Each is `massing.crop_patch(span=24)`: a 25×25 footprint of dirt, a
9×9 core of `farmland[moisture=7]` with mature crops on top, and a **central
water column** for hydration.

| id | crop | authored point |
|---|---|---|
| `carrots_0_3` | carrots | −2384, 92, −112 |
| `potatoes_0_6` | potatoes | −2384, 94, 336 |
| `carrots_2_3` | carrots | −2192, 90, −48 |
| `carrots_4_8` | carrots | −1906, 66, 510 |

Per field: **81 farmland + 81 mature crops + 1 water source + 544 dirt**.

## 3. How the `founder_crop` opportunities would be re-authored

Not deleted. The opportunity selections stay; their **physical representation**
is what is rejected. A wild carrot Patch is a legitimate game-authored ecology
even though vanilla never generates one — "not vanilla-natural" is not the test.

The re-authoring is to **author nothing physical at all**. Patches are now a
runtime manifestation: `PatchShape` grows an irregular, terrain-conforming
cluster at a locus the eligibility query chose, and `Renewables.placeCrops`
realises it. Pre-building a patch into the template would reintroduce exactly
the fixed-site assumption the model rejects, and would be overwritten by the
first manifestation anyway.

So the template's contribution becomes the **Region**, not the blocks.

## 4. Minimum substrate changes a wild Patch requires

Per crop block, and nothing more:

- one `FARMLAND` (default moisture, **not** 7) directly beneath the crop;
- the crop block itself at max age.

Explicitly not produced: water sources, hydrated farmland, contiguous prepared
ground, flattening, fences, or any block not directly under a resource block.
`PatchShapeTest` asserts the shape fills under 75% of its own bounding box and
emits at most one column per crop, which is the numeric difference between an
occurrence and a prepared field.

## 5. Terrain restoration after removing farm infrastructure

### MEASURED, 21 September — this section's original claim was overstated

The original text asserted that leaving a pen's floor would "bias the
eligibility query" back onto the old site. That was an assertion. It has now
been measured by running the real predicate against the real template
(`implementation/worldgen/reports/regenerative_2026-09-21/`), and the claim is
**directionally right and much weaker than stated**:

| Opportunity | Eligible loci | On legacy pad | Pad share | If uniform | Bias |
|---|---|---|---|---|---|
| rabbit_0_3 | 256 | 53 | 20.7% | 17.0% | 1.22x |
| sheep_0_5 | 271 | 66 | 24.4% | 17.0% | 1.43x |
| cow_0_6 | 412 | 92 | 22.3% | 17.0% | 1.31x |
| sheep_2_3 | 503 | 91 | 18.1% | 17.0% | 1.06x |
| sheep_2_6 | 490 | 89 | 18.2% | 17.0% | 1.07x |
| rabbit_3_6 | 507 | 94 | 18.5% | 17.0% | 1.09x |
| chicken_4_1 | 329 | 87 | 26.4% | 17.0% | 1.56x |
| sheep_4_4 | 282 | 76 | 27.0% | 17.0% | 1.59x |
| carrots_0_3 | 288 | 18 | 6.3% | 6.1% | 1.02x |
| potatoes_0_6 | 379 | 25 | 6.6% | 6.1% | 1.08x |
| carrots_2_3 | 498 | 39 | 7.8% | 6.1% | 1.28x |
| **carrots_4_8** | 206 | 40 | **19.4%** | 6.1% | **3.17x** |

Three findings, none of which was available by argument:

1. **The pads are over-represented but nowhere near dominant.** Most regions sit
   between 1.0x and 1.3x of what uniform placement would give. A manifestation
   landing on the old pen floor is somewhat more likely than chance, not the
   default.
2. **`carrots_4_8` is a genuine outlier at 3.17x**, and it is also the region
   with the fewest eligible loci (206). Its ecology is the one where the
   authored pad really is a large share of the usable ground.
3. **Every region has hundreds of eligible loci** -- 206 at the lowest. §9's
   worry that a region might have NO eligible locus once its pad is gone is
   refuted for all twelve: removing the pads removes at most 27% of the
   candidates.

The consequence for migration is that pad removal is **less urgent than stated**
and remains worth doing: a 1.2x-1.6x pull toward a flattened rectangle is still
a visible artefact in a system whose whole claim is that the manifestation moves.

The original cost argument below stands unchanged.

**The restoration cost is real, and it is not symmetrical.**

- **Fields.** Removing farmland/water/crops leaves 625 columns of authored
  `dirt` — the pad the field was laid on. Reverting it needs the pre-authoring
  surface, which the template does not retain. Either the authoring tool
  re-derives it from the base world, or the pads remain as visible bare patches.
- **Pens.** The same, worse: a 1,681-column flattened grass floor. Removing the
  fence and leaving the floor produces exactly the "decorative spawn pad" the
  model rejects — an obvious artificial clearing at the old site, which would
  also bias the eligibility query toward it, since flat natural grass is
  precisely what the predicate likes.

**Recommendation:** re-author from the base world rather than subtract from the
frozen template. Subtraction cannot restore what the pads replaced.

## 6. What replaces physical Herd containment

Nothing. That is the point. Members are spawned by the runtime at a locus chosen
from current terrain, clustered by `renewables.herd.cluster`, and held in place
by nothing at all. Membership is a PDC mark, so the system knows its herd
wherever it wanders — which is what made containment unnecessary rather than
merely unfashionable.

## 7. Optimizer evidence that survives

All of it. The selected cells are the product of real analysis and nothing in
this model invalidates them:

- `implementation/worldgen/reports/expedition_2026-09-20/portfolios/portfolio-consolidative.json`
  — `founders` and `renewables` arrays, the authoritative selection record;
- `docs/analysis/authored-strategic-opportunity-optimizer.md` — selection rationale;
- `docs/analysis/alpha-0.1-map-freeze.json` — the freeze record;
- `rescan-consolidative.json` — measured reach, which counts sites not pens.

These become **Opportunity Region seeds**. Until they are expressed as explicit
cells, each source falls back to a square of half-span
`renewables.migratedRegionHalfSpan` (48) around its authored point — a coarse
search area, clearly labelled as migrated origin+radius data rather than as
authored region geometry.

## 8. Generated artifacts that would change

| Artifact | Change |
|---|---|
| `artifacts/worldgen/alpha-0.1/consolidative-alpha` | region files rewritten; **28 MB, not in git** |
| `docs/analysis/alpha-0.1-map-freeze.json` | new provenance sha, new freeze date |
| `implementation/worldgen/terrain_harvest/massing.py` | `pen()` and `crop_patch()` retired |
| `author_portfolio.py` | `SPECIES_FENCE` and both `template_for` branches removed |
| `publish_world.py` | both kinds lose their authored footprint colours |
| `verify_portfolio.py` | readback checks for fences/farmland removed |

## 9. Validation passes to rerun

1. `verify_portfolio.py` — readback of every authored site;
2. `rescan.py` — measured reach, since removing 8 pens and 4 fields changes
   traversable terrain and therefore corridor costs;
3. `publish_world.py` — PNG/RLE exports;
4. the Alpha lifecycle check — open, start, reset, replay on the new template;
5. a renewables smoke test — that every Region still offers at least one
   eligible locus after the pads are gone. **Already measured and passing**: the
   thinnest region has 206 eligible loci against 40 on its pad, so no region is
   at risk of being emptied. Rerun it after re-authoring to confirm the number
   rather than to discover it.

## 10. Downstream snapshots and tests to regenerate

- `rescan-consolidative.json` and the reach tables derived from it;
- `docs/analysis/rescan-measured-reach.md`;
- `alpha-0.1-map-freeze.json` provenance;
- `implementation/worldgen/tests/test_author_portfolio.py` — asserts the current
  templates exist;
- any Route/spillover artifact quoting corridor costs across the affected cells.

---

## Blocking decision

Re-authoring the template is a **re-freeze**, with its own validation pass and a
new freeze record. The handoff forbids doing it silently, and §5 shows it cannot
be done by subtraction — the pads replaced terrain the template no longer knows.

Until that decision: legacy pens and fields stay in the world as inert geography.
They no longer participate in the model, and nothing in the runtime treats them
as the intended representation.
