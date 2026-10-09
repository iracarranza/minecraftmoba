# Pool yield measurement: 16 targets, after the trial-chamber rule

**9 October 2026.** A single 16-target foundry batch, run to learn whether
generating a map on demand is viable now that publication rejects trial
chambers (`0a8e77f`, 25 September). The previous yield figure (35%, 24 September)
predates that rule.

## State found before the batch

| | |
|---|---|
| Live pool (`/private/tmp/mobapool`, `alpha.pool.enabled: true`) | **0 maps** |
| `/private/tmp/stockrun` | 33 empty directories, wiped 7 October |
| Certified lab scoops | 3 |

With the pool enabled and empty, a pool match cannot start unless
`alpha.pool.allowIncompleteBinding` is set.

## Setup

- 8 fresh seeds (910001 to 910008), `per_seed=2`, 16 targets.
- 3 generation workers, 6 compile workers, on a 16 GB / 8-core machine with the
  live alpha server stopped.
- Published into a scratch pool, so the trial-chamber rejection was exercised.
- The cubiomes scanner was built from an existing local checkout
  (`artifacts/worldgen/staged_default_2026-09-09/build/cubiomes`), not downloaded.

## Result

| | |
|---|---|
| Targets | 16 |
| Generated | **16** (0 failures) |
| Compiled PLAYABLE | **3** (19%) |
| **Published** | **0**: all 3 refused for containing trial chambers |
| Wall clock | 839 s |

Rejections among the other 13: `SOCKET_NOT_INDEPENDENTLY_ACCEPTABLE` (Homebase
siting, the dominant cause again), one `LAIR_ACCESS_DISPARITY`, one
`OPENING_CEILING_VIOLATED`. Eleven of 16 stopped at the homebase stage.

**Effective yield after the trial-chamber rule: 0 of 16 in this batch.** The 24
September figure is not comparable. A sample of 16 cannot give a rate, but it is
enough to say the rule is not a rare filter.

## Timing

| Stage | Per target | Notes |
|---|---|---|
| Discovery (cubiomes prospect) | about 2.9 s per seed | 4 to 15 usable windows per seed, median about 11 |
| Generation | 74 to 130 s, mean 106 | with 3 running at once |
| Harvest | 6 to 35 s, mean 18 | |
| Compile phase | 108.6 s for 16 | 6 workers |
| Overall | about 52 s of wall clock per target | |

Generation across the batch took 689 s for 16 targets (about 43 s each, three at
a time). One target alone takes roughly 90 s, from the 24 September audit.

## Could the off-seed scanner have avoided it?

`mobastruct` locates trial chambers off the seed. Over the same 16 windows:

| Margin around the window | Windows with a trial chamber | Flagged among the 3 refused maps |
|---|---|---|
| 0 | 9 of 16 | 2 of 3 |
| 48 or 80 blocks | 10 of 16 | 2 of 3 |

So **a scanner prefilter would remove most of the problem but not all of it**: it
missed one of the three (910005 target 1). A generation-time check must remain as
the backstop. Windows are large (about 54 x 66 chunks), which is why more than
half contain one.

## What this means for on-demand generation

- One attempt costs about 2 to 3 minutes of wall clock alone, and the measured
  published yield was 0 of 16. A draft under two minutes cannot wait on that.
- A warm pool remains the primary source. Its refill needs the scanner
  prefilter, and then a re-measurement.
- Discovery is cheap enough to run per match (about 3 s per seed).

## Process notes

- **The first run was invalid, and it was the driver's fault.** The driver script
  had no `if __name__ == '__main__'` guard. Under macOS's spawn start method,
  every compile worker re-imported it and re-ran the whole batch, producing seven
  nested batches and 24 stray generator JVMs. Those were killed and the batch
  restarted. Any script that calls `foundry_run.run` needs the guard.
- The live alpha server was stopped for the measurement and not restarted.
- The scanner build and the driver live in the session scratchpad, not the repo.
