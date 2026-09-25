# A match on a pool map, with animals the compiler placed

**24 September 2026.** The end of the four-step sequence, verified in play
rather than in tests.

## What ran

```
pool=/private/tmp/mobapool entries=7 READY=7 IN_USE=0 USED=0
[pool] claimed 3141592-0ef7ec96c5 (seed 3141592)
[renewables] bound 16 derived source(s) from this map's manifest
[match] bound 16 renewable source(s)
Selected pool map 3141592-0ef7ec96c5; world 'alpha_match' bound.
  bindings: fountains=2 objectives=2 lair=anchored renewables=16 worksites=12
[match] Match started. DAY 0:00
```

Force-loading three derived source coordinates and asking what stands there:

| source | kind | manifested |
|---|---|---|
| `-722, 64, -2642` | chicken | **4** |
| `-722, 69, -2770` | cow | **2** |
| `-722, 69, -2514` | pig | **4** |

Those coordinates were not authored by anyone. The compiler derived them from
that map's own cell grid, using the recovered near/farther/deeper ordering,
and the runtime placed animals on them.

## Why this is the end of the sequence and not another green test

The goal's step 4 said the loop closes when "a generated map carries renewable
sources derived from its own geography, `renewables` enters
`readiness.REQUIRED`, and **a match starts on it**." Each half had been true
separately for hours: the compiler derived portfolios and refused to certify
without one, and the runtime's reader passed tests against a real derived
portfolio. Neither established that a match would run.

It nearly did not. Three environment faults stood between them, none of which
any test would have caught:

- the test server lacked the Alpha map configurations the plugin loads at
  enable time, and failed to enable at all;
- `templatePath` is a relative path that `deploy.sh` rewrites for a real
  server, so a hand-built one fingerprints a directory that is not there --
  and selection aborted **after** claiming a pool map and **before** binding
  renewables, which is why the first attempt logged a claim and no portfolio;
- the server was launched without a writable stdin, so no console command
  could reach it.

## A shadowing bug, the third this session

`foundry_run.run` took a `pool: Path` parameter, and
`with ProcessPoolExecutor(...) as pool:` shadowed it. Publishing then received
an executor: seven playable maps, zero published, with a `TypeError` about
`__fspath__`. The executors are named for what they are now.

The worlds survived, because the same run kept playable worlds and deleted
only rejected ones, so the pool was published from disk without regenerating
anything.

## And a gap the cleanup created

The goal batch's 33 certified maps have no worlds: the world cleanup added in
step 1 deleted every world once compilation finished. That is right for a
measurement batch and wrong for one meant to stock a pool -- **a map that
cannot be claimed is not in a pool, however thoroughly it was certified.**
`foundry_run` now publishes before deleting, and deletes only what was not
published.

## Status

Steps 1 through 4 are complete, with step 1's throughput target met by step 2
rather than by parallelism. The pool holds 7 claimable maps; the goal batch
demonstrated the refill rate at 16.28 READY/hour.
