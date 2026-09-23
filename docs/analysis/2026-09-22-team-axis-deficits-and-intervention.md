# Team-axis deficits, and what authoring can be expected to fix

**22 September doctrine correction:** `deficit`, `opportunity`, `clearable`
and `physical` below are historical classifier labels, not validated competitive
diagnoses. N/S Wilderness may differ. `accessible land` counts selected dry
sampled depth bands outside both homeland footprints, not proven workable or
productive area. Opportunity count is not cut/fill/clearing or Socket integration
cost. No terrain compensation or screening gate follows from these numbers.
Read the [current audit](../audit/2026-09-22-spatial-doctrine.md). Measurements and
old hypotheses below are retained for traceability, not current prescriptions.

## The axes, restated because they are easy to collapse

**W–E is the REGIONAL axis.** Contrast there is intentional: forest against
arid, peaks against valley. It is the map having distinct places.

**N–S is the TEAM axis.** Both teams must get a competitive start.

A pairing like `forest/arid` does **not** mean one team gets forest and the
other arid. It means the map runs forest-to-arid across the axis the teams do
not sit on, so both face the same gradient side-on. Nothing below reinterprets
regional contrast as teams receiving different biomes.

## The degenerate metric, replaced

`land_asymmetry` read **0.0000 for all eight seeds** because it summed every
depth band, and `deep_core_350_plus` is most of the map for both teams by
construction. Eight zeros that looked like eight balanced maps.

Replaced with **accessible land**: the bands a team can actually reach and work
— opening, fringe, secondary, tertiary, deep transition and deep core — with
`deep_core_350_plus` excluded. It now ranges 0.10 to 0.64 across the same eight
seeds, which is the signal the old statistic was averaging away.

Added alongside it: **connected workable land**, summing
`largest_connected_patch_samples` over bands the fit already marks
`meaningful_area_available`. Both come from Task A's own output; no new measure
was invented.

## Deficit diagnosis

Components are compared **between teams** and the largest gap names the deficit.
No absolute threshold is introduced, and none is taken from 930015734.

| seed | W–E pairing | N–S homeland gap | accessible land gap | connected gap | deficit |
|---|---|---|---|---|---|
| 930012642 | forest/open | 0.0645 | 0.2404 | 0.0167 | opportunity |
| 930019528 | forest/frozen | 0.0886 | 0.1003 | 0.0356 | opportunity |
| 930005557 | forest/open | 0.0983 | 0.1840 | 0.0029 | opportunity |
| 930010639 | frozen/open | 0.1370 | 0.3173 | 0.0278 | opportunity |
| 930016664 | frozen/oceanic | 0.1439 | 0.2268 | 0.0073 | opportunity |
| 930007222 | open/oceanic | 0.2798 | 0.2646 | 0.0612 | opportunity |
| **930015734** | **forest/arid** | 0.3397 | **0.6381** | 0.1367 | **opportunity** |
| 930006815 | open/oceanic | 0.3443 | 0.2420 | 0.0670 | **clearable** |

### 930015734 is an authoring target, not a rejection

Its south homeland diagnostics against north:

| | north | south |
|---|---|---|
| `severe_grade_fraction` | 0.0 | **0.0** |
| `water_fraction` | 0.0 | **0.0** |
| `canopy_fraction` | 0.0 | **0.5185** |
| `open_fraction` | 0.5185 | 0.2963 |
| `usable_fraction` | 1.0 | 0.7037 |

**No physical or topological defect at all.** The south homeland is under
forest canopy and short of near-depth workable land. Trees and opportunity
placement are both things authoring already does.

930006815 diagnoses `clearable` on open ground, so two of the three deficit
kinds are represented in this sample.

### The arm this sample cannot test

**No seed here diagnoses `physical`**, and that is a property of the sample
rather than a finding. These are finalists of the staged screen, which gated on
homeland buildability upstream, so severe-geographic cases were rejected before
they could be extracted. Testing that arm needs a candidate that *failed* those
gates, and none is extracted. Until one is, "severe physical defect justifies
rejection" remains untested rather than supported.

## Minimum-intervention frontier

Authoring cost is the number of opportunities a configuration places; balance is
`balance_asymmetry`. Measured over all 80 stored finalists, 10 from each of the
optimizer's 8 profiles:

| authoring cost | best balance achieved | configurations |
|---|---|---|
| **28 opportunities** | **0.0182** | 10 |
| 32 opportunities | 0.0158 | 60 |
| 38 opportunities | 0.0211 | 10 |

**Correlation between cost and balance: −0.21.** Spending 38 opportunities does
not beat spending 28; the cheapest profile achieves 0.0182 while the most
expensive achieves 0.0211.

So on this volume **balance is bought by placement, not by quantity**, which is
the result that matters for a minimum-intervention policy: rescuing a near-miss
should mean placing opportunities *where the deficit is*, not placing more of
them.

### What this measurement cannot claim

All 80 configurations already passed the optimizer's balance filter. This
therefore describes the cost-balance relationship **among configurations that
are already balanced**, on a volume that was well chosen to begin with. It does
not show that authoring can lift a deficient seed to parity — that requires a
near-miss volume, and is blocked below.

## Blocker, precisely located

The near-miss test wants seed **930015734**, whose world was deleted: only
server scaffolding remains and it is not in the terrain gallery.

**But a cheaper subject exists.** `tv_51a79e3b1bee05ea7559e799` is seed
**930010639** — `frozen/open`, homeland gap 0.1370, accessible land gap 0.3173,
deficit kind `opportunity` — already harvested into the gallery at full 3,808
chunks. A different regional character from the Alpha map, with the same deficit
kind, and no world generation required.

The optimizer needs exactly two files zipped: `data/opportunity-map.json` and
`data/travel-matrix.json`.

- `opportunity-map` is runnable today: `terrain_harvest.opportunity_map
  --gallery <TerrainGallery> --volume-id tv_51a79e3b1bee05ea7559e799 --source
  <that volume's directory>`.
- `travel-matrix` is the gap. `expedition.travel` requires `--structures` and
  `--manifest`, and the structure-siting and map-manifest records exist only for
  the Alpha volume. Both are produced by existing modules that have not been run
  for this one.

That is the whole remaining distance to answering whether minimal authoring can
rescue a genuine N–S near-miss.
