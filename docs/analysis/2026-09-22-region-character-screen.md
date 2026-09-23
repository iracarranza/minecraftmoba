# Generalizing the seed screen from one composition to region character

**22 September scope clarification:** cross-composition measurements remain
useful, but composition-agnostic scoring is not a replacement for a future
Map Type macro-geography contract. Default seeks its intended Overworld regional
gradient; other types may seek different geography. No new recognizer or screen
is implemented here. N/S natural terrain similarity is not a fairness gate.
See [spatial doctrine audit](../audit/2026-09-22-spatial-doctrine.md).

## What the staged screen actually tested

`vanilla_search/coarse.c` stage A rejects a window unless it contains ocean and
highland. Stage B then scores the west third for ocean/highland/alpine and the
east third for ocean/highland, with three hard gates:

```c
if(eo<.04 || eo<=wo || wh<.025 || hn<.30 || hs<.30) continue;
```

Two of those are **general**: `hn` and `hs` require both ends of the TEAM axis to
be workable land. The other three name biomes.

The axes are worth stating plainly, because they are easy to get backwards.
West–east carries the regional character; **north–south is the team axis**, and
it is checked only for fairness. So the map is deliberately asymmetric in
geography and symmetric between teams — both teams face the same gradient
side-on. An objection about "jungle side versus mesa side" does not apply to
what this design does.

## The change

A new `--regions` mode, leaving the staged screen untouched and reproducible.
Each block is categorized into one of seven characters — oceanic, alpine,
frozen, arid, forest, open, wet — and the two ends are required to **differ**
rather than to be anything in particular. The team-axis gate is unchanged.

## A mistake worth recording

The first version generalized `eo > wo` as **dominance**: each end classified by
its most common character, requiring both to be strongly dominated. That looked
equivalent and is not. `eo > wo` is a **gradient** — ocean increases eastward —
and a gradient can be strong without either end being made of that character.

Highland almost never dominates a third, so under the dominance version the
current Alpha seed came back classified `open/forest` at a window its own screen
never chose. The second version finds, for each end, the character whose
fraction most increases toward it, and uses the weaker of the two gradients as
the contrast.

## Results, 20,000 seeds

| | staged screen | `--regions` |
|---|---|---|
| survivors | 19,288 windows | 26,693 windows / **8,425 seeds** |
| compositions found | one, by construction | **12 pairings** |

```
forest/oceanic 6137   forest/open 5614   oceanic/open 3530
arid/forest    2296   arid/oceanic 2126  forest/frozen 1375
frozen/oceanic 1360   arid/open   1334   frozen/open    750
forest/wet      456   oceanic/wet  443   open/wet       294
```

`arid/forest` is the mesa-and-jungle case, `forest/frozen` and `frozen/open` are
the snowy-peaks-and-valley case. Both were unreachable before, not because they
failed a balance test but because they failed a composition test.

## What this does NOT establish

**The current Alpha seed still classifies as `forest/open`, not
`oceanic/alpine`.** That is not a bug in the classifier; it is the difference
between a specification-driven search and a descriptive one. The staged screen
was told to find ocean and highland and found the window where that contrast is
strongest. Asked instead "what is this seed's strongest contrast", the honest
answer for that window is forest against open ground.

So "categorize rather than filter" delivers a menu, and choosing a map still
means naming which pairing you want. That is a profile selection, not a
rediscovery of the old screen.

**Nothing here measures balance.** These are candidates for the structural
pipeline, not finished maps. `balance_asymmetry`, Practical Reach and the
opportunity measures are all downstream and all composition-agnostic — they are
what would decide whether an `arid/forest` seed is playable.

**Cross-character economic equivalence is still unresolved.** The team axis is
kept fair by land fraction, which is why this does not need an exchange rate
between biomes. If a future design puts different characters on the two TEAM
ends rather than the two map ends, it would.

## Next step

Take two or three accepted seeds from different pairings through
`terrain_harvest` and the optimizer, and compare their `balance_asymmetry`
against the current map's 0.0685. That is the experiment that would show whether
the balance criteria really are composition-agnostic, and it is the one thing
this screen cannot answer on its own.
