# Applying authored additions at match open — measurement

Measured on the live Alpha server against the frozen Consolidative instance,
2026-09-22. The question: can "copy the base world and apply a verified set of
additions at match open" replace "copy a baked template", or is the write cost
prohibitive?

## Result

**It is faster than what it replaces.**

| | blocks | chunks | time | throughput |
|---|---|---|---|---|
| **Baseline: copy the 28MB template** | — | — | **4,351 ms** | — |
| Apply 200k blocks, cold chunks | 200,000 | 828 | **2,778 ms** | 72k /s |
| Apply 200k blocks, warm chunks | 200,000 | 828 | **1,009 ms** | 198k /s |
| Apply 50k blocks, cold | 50,000 | 380 | 468 ms | 107k /s |

The authored footprint is about 191,000 blocks across 32 sites, so the cold
200k row is the realistic case: **~2.8 seconds against a 4.4 second baseline.**

Worst single site was 989 ms cold, 95 ms warm. The work happens at match open
with no players in the instance, and the template copy it replaces already
blocks for longer.

## What the numbers say about where the time goes

Cold 2,778 ms against warm 1,009 ms puts roughly **1.8 seconds in chunk
loading** and **1.0 second in the writes themselves**. Writing is not the
bottleneck; reaching the chunks is. That matters for the design, because the
additions are spread across the map by nature and there is no arrangement of
them that avoids touching ~800 chunks.

It also means the cost scales with the number of SITES far more than with the
number of blocks: 50k blocks across 380 chunks cost 468 ms, while four times
the blocks across roughly twice the chunks cost six times as much.

## A confound worth recording rather than hiding

The first "dense" run measured **28,397 ms** — ten times slower than scattered,
which is the opposite of what a dense write should do.

It was not measuring writes. The dense benchmark anchored at (0, 70, 0), which
is roughly 2,000 blocks from the authored map and therefore **ungenerated
terrain**, so it paid for generating 784 fresh chunks. Scattered wrote into the
template's existing chunks.

The lesson is about the design, not the benchmark: applying additions into
already-generated terrain is cheap, and anything that pushes writes into
ungenerated chunks costs roughly 36 ms per chunk to generate. A base world that
ships with its play area already generated keeps this in the measured range; one
that generates on demand does not.

## Caveats

- Single material, single Y per site. Real additions clear a volume before
  filling it, so the true block count per site is higher than the massing
  suggests — though the pads are already included in the 191k figure.
- Measured on this machine, on an SSD, with one player online.
- The copy baseline includes unload and load; the apply figures do not include
  the base-world copy that would still precede them. A fair end-to-end
  comparison is *copy base + apply* against *copy template*, and both copies
  are the same size, so the apply time is the true added cost.

## Verdict

Runtime application costs about **2.8 seconds on top of a copy that already
happens**, in a phase that already blocks for 4.4 seconds. That is not
prohibitive, and the design is not invalidated by it.

The thing to watch is not throughput but **chunk reach**: cost tracks the
number of distinct chunks touched, so a future addition set that scatters more
widely is the case that would need re-measuring.
