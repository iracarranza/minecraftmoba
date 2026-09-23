# Does the balance machinery work on seeds that are not ocean-and-highland?

**22 September doctrine correction:** the table measures sampled homeland-pad
scores and gaps, not competitive map quality. Alpha's smaller gap does not prove
it was better chosen; 930015734 is not proven unbalanced. The old proposal to
compensate or screen on that gap alone is superseded. Natural N/S Wilderness
asymmetry is permitted. Read the [current audit](../audit/2026-09-22-spatial-doctrine.md)
for the Core/Socket boundary. Numeric results below remain historical evidence.

Yes. Task A's existing fit ran unmodified over eight finalist candidates
spanning six region pairings, and produced comparable numbers for all of them.
Nothing in it needed to know what the biomes were.

## Results

`terrain_harvest.compare_seeds`, homeland quality per team and the normalised
asymmetry between them:

| seed | pairing | north | south | asymmetry | fit failures |
|---|---|---|---|---|---|
| **930012642** | forest/open | 0.942 | 0.828 | **0.0645** | 2 |
| 930019528 | forest/frozen | 0.804 | 0.673 | 0.0886 | 2 |
| 930005557 | forest/open | 0.762 | 0.625 | 0.0983 | 1 |
| 930010639 | frozen/open | 0.903 | 0.685 | 0.1370 | 2 |
| 930016664 | frozen/oceanic | 0.693 | 0.925 | 0.1439 | 4 |
| 930007222 | open/oceanic | 0.435 | 0.773 | 0.2798 | 1 |
| **930015734** | **forest/arid** | 0.950 | 0.468 | **0.3397** | 3 |
| 930006815 | open/oceanic | 0.370 | 0.759 | 0.3443 | 2 |

930012642 is the current Alpha map, and it has the lowest asymmetry of the
eight. It was well chosen.

## What this establishes

**The structural measures are composition-agnostic in practice, not just in
principle.** `forest/frozen` at 0.0886 and `forest/open` at 0.0983 sit close
behind the current map. A snowy-peaks-and-valley seed is not disqualified by
being one; it is simply another candidate, measured the same way.

**The mesa-and-jungle case is the interesting failure.** 930015734 is
`forest/arid` with a north homeland at 0.950 and a south at 0.468 — the largest
gap in the set. That is exactly the near-miss shape this work is looking for: a
strong regional contrast producing unequal starts.

## A metric that is broken, reported rather than buried

`land_asymmetry` came out **0.0000 for all eight seeds**. That is not eight
perfectly balanced maps; it is a degenerate statistic. Land band counts are
summed per team over halves that contain the same number of samples by
construction, so the totals match whatever the terrain does.

It is left in the output labelled, because a metric that always reads zero is
worth seeing rather than silently dropping — but nothing should be concluded
from it. Per-team land quality needs a measure that can actually differ:
buildable fraction, or land weighted by depth band.

## The next experiment, and the point of authoring

Authoring exists to take a near-miss and make it competitive. So the question
930015734 poses is not "is this seed balanced" — it plainly is not — but
**"can authored opportunities close a 0.34 homeland gap?"**

That is answerable with machinery that already exists: run the optimizer over
930015734's export to get a scenario frontier, then `author_portfolio` and read
`balance_asymmetry` out of its metrics, exactly as the current map reports
0.0685. If a deliberately lopsided seed can be authored to parity, the screen
can afford to accept far more geography than it does. If it cannot, homeland
asymmetry becomes a screening gate rather than an authoring problem, and that
is worth knowing before broadening the search.

Blocked on one thing only: the optimizer takes an `export.zip` per seed, and
only the Alpha volume has one.

## Caveat carried from the fit itself

Task A's own `uncertainties` list names `resource_equivalence` first. The fit
measures reach and ground, not what the ground is worth. A `forest/arid` map
could be reach-fair and economically lopsided, and nothing measured here would
show it.
