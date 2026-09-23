# Forty unseen seeds against the full contract

**Date:** 2026-09-23
**Seeds:** 40, drawn from a fixed RNG seed (20260923) with every previously used
seed excluded. Generated, harvested and compiled with the Fountain
verification, Opening ceiling, structure refusal and entity eviction added the
same day.

## Yield: 0 of 40

```
recognize   reached 40   stopped here 33
homebase    reached  7   stopped here  5
hinterland  reached  2   stopped here  0
objectives  reached  2   stopped here  1
lair        reached  1   stopped here  0
select      reached  1   stopped here  0
author      reached  1   stopped here  0
verify      reached  1   stopped here  1
ready       reached  0
```

| code | n |
| --- | --- |
| `NO_DEFAULT_REGIONAL_SHAPE` | 33 |
| `SOCKET_NOT_INDEPENDENTLY_ACCEPTABLE` | 7 (across 5 maps) |
| `NO_ORDERED_OBJECTIVE_LAYOUT` | 1 |
| `OPENING_CEILING_VIOLATED` | 1 |

## The headline is not the yield

**Today's new checks are not why nothing passed.** Thirty-eight of forty were
rejected before any of them ran. Exactly one map was stopped by a check written
today — 1248940432, on the Opening ceiling — and by then it was the only
candidate still alive.

**The binding constraint is the regional-shape recogniser, at 82.5%.** Seven
maps in forty produce terrain the compiler will call a Default map at all, and
five of those seven then fail to seat a Homebase. Throughput is decided before
any question of objectives, Lair or opening quality is asked, and no amount of
tuning the later gates changes it.

## What that means for a foundry

At this rate a pool cannot be stocked from random seeds. The two previously
playable maps came from a batch of thirty on a looser contract, and across the
69 candidates compiled today under the current one, **none reached READY**.

The honest reading is not "the contract is too strict". It is that **seed search
is the throughput problem and nothing has ever been spent on it.** The
recogniser rejects 4 in 5 candidates on regional shape, and the cheap screen
measured in an earlier session — 0 false negatives, 30% correctly discarded —
was never wired in, so every one of those 33 rejections paid a full 65-second
world generation and a 10-second harvest first.

## Next, in order

1. **Wire in the cheap screen.** It is measured and idle. Discarding 30% before
   generation is the only lever that touches the 82.5%.
2. **Ask what `NO_DEFAULT_REGIONAL_SHAPE` is actually rejecting.** A gate that
   refuses 4 in 5 random seeds is either describing a genuinely rare geography
   or is mis-specified, and nobody has looked at the distribution of what it
   measures. This is the same discipline applied to the Socket threshold and to
   A_D: look at the distribution before defending or moving the bound.
3. **Preserve the pristine world** for any map that does reach READY. Nothing
   does, which is why 2718281 could not be re-verified this morning.
