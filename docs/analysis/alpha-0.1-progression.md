# Alpha 0.1 progression integration

Status: **Integrated for the domains that have authority. Not a rebalance.**
Date: 20 September 2026

## What existed before

`PlayerData` held level, xp and choices. `Rewards`/`RewardCatalog` ran the
level-up choice menu, `TaskEffects` applied automatic grants, `Contributions`
held the Level 6 fork, and the HUD showed a level line.

**No Work Point concept existed** — zero occurrences in the codebase — and
**no gameplay source of progression existed at all.** The only way to gain a
level was `/moba xp`, the admin command, against a flat `xpPerLevel: 100`.

## What is now integrated

`WorkPoints` declares the seven domains and owns awarding, the band curve and
the diagnostics. The admin grant was rerouted through the same curve, so
gameplay and `/moba xp` can no longer disagree about what a level costs.

### The band curve

ALPHA CALIBRATION, supplied for this pass and held in config, not derived here:

| Levels | WP per level |
|---|---:|
| 1–6 | 40 |
| 7–12 | 54 |
| 13–19 | 75 |
| 20–24 | 103 |
| 25–30 | 130 |

Each level is charged its own band, so crossing a band boundary pays the old
price for the level being left and the new price for the next.

### Live WP sources

| Domain | Rule | Rate | Authority |
|---|---|---:|---|
| Construction | ordinary useful placement | 1 WP | UAU supplement §11 sensitivity fixture |
| Construction | Construction Block placement | 2 WP | same |
| Extraction | opportunity A(O), once per ore block | 1–6 WP by resource | rule from §10; magnitudes are NON-CANON ALPHA FIXTURES |
| Extraction | harvest qH, from the real drops | q = 1 per item | same |

**Extraction is WP = A(O) + qH.** A(O) credits exploiting one physical
opportunity and is paid once per ore block. H is the qualifying harvest
actually obtained, read from the drops the break produced, and q converts it to
points.

**Fortune raises H and never A(O).** One ore block does not become 2.2
opportunities because the pick is enchanted, but the extra material really was
extracted and is additional Extraction work. This is what makes Yield a
progression specialization rather than only an item enchantment, alongside
Efficiency buying opportunities per unit time and Unbreaking buying sustained
exploitation.

> An earlier version of this pass had the model backwards — "per block broken,
> never per item dropped" — which would have made Yield progression-inert.
> Corrected before the live test.

**Recovered material is not new acquisition.** `Provenance` already tracked
player-placed blocks, so breaking a block you placed earns nothing.

> The provenance read was also wrong, and worse: the handler listened at
> `MONITOR`, but SPEC section 7.1 requires consumers to read `isPlayerPlaced` at
> `HIGHEST` because Provenance clears its mark at `MONITOR` and is registered
> first. Player-placed **ore** was therefore payable. The original anti-farm
> live test passed vacuously because it broke dirt, where `oreKind` returns null
> before provenance is consulted. Fixed to `HIGHEST`.

## Domains left unimplemented, and why

| Domain | Gap |
|---|---|
| Exploration | credits "resolving access"; no trigger or coefficient specified |
| Production | credits transformation; no coefficient, and no rule for which transformations qualify |
| Development | no coefficient, and its relationship to Construction recognition is unresolved |
| Logistics | credits "making resources available"; Supply Line recognition rules are unresolved |
| Combat | credits "securing value under threat", and canon forbids generic proximity sharing; no rule distinguishes a qualifying kill from any other |

None of these was given a coefficient. Inventing one to make every domain
produce something would have made the curve look calibrated while measuring
nothing.

Also unimplemented: the **recognition premium** distinct from base work. The
manuscript gives one number — baseline Construct recognition at +64 WP — but
Construct recognition itself is unresolved, so there is nothing to attach it
to. Worksite capitalization is an existing hook with no published premium.

## Edge cases checked

| # | Case | Result |
|---|---|---|
| 1 | player-placed ore | A(O) and H both refused; provenance now read at `HIGHEST` |
| 2 | Fortune | **not verifiable with this harness** — see below |
| 3 | Silk Touch | **UNRESOLVED IN AUTHORITY**; credits no harvest by default, configurable, labelled at the award site |
| 4 | non-ore natural blocks | pay nothing; verified live with stone, oak log, terracotta |
| 5 | natural Construction Blocks | terracotta is a Construction Block and is not Extraction; asserted by test and verified live |
| 6 | event validity | cancelled breaks ignored; Creative and Spectator produce no work, verified live |
| 7 | band boundaries | 6→7 at 40, 7→8 at 54, 12→13 at 54, 13→14 at 75, 19→20 at 75, 20→21 at 103, 24→25 at 103, 25→26 at 130 |
| 8 | level 30 | no level 31; WP stops accumulating rather than growing behind a stale denominator |
| 9 | reward idempotency | `TaskEffects.grant` returns early when the tier is already held, so refresh paths cannot re-apply |
| 10 | match scope | progression persists across death and rejoin within a match; reset clears it; the next match opens from baseline |
| 11 | diagnostics | each award names its domain and source, e.g. `iron opportunity` and `copper harvest x3` |
| 12 | test quality | the live test drives real `BlockBreakEvent` and `BlockDropItemEvent` from a Survival player |

Also fixed while checking these: the vanilla XP bar was filling against the flat
fallback rather than the band cost, so it read wrongly for every level past 6.

## Live verification

Real gameplay only. No admin XP grant was used as evidence.

| Demonstration | Result |
|---|---|
| Baseline | `0/40 to Lv2` |
| Ordinary placement × 1 | `1/40`, construction 1 |
| Non-qualifying repetition | broke own placed block → still `1/40`, **extraction 0** |
| Construction Block × 1 (bricks) | `2/40`, construction 2 |
| Extraction: 8 iron + 4 diamond | `32/40`, **extraction 32** — exactly 8×2 + 4×4 |
| Extraction: 5 copper, three separate runs | 15, 20 and 23 WP |
| Non-ore: stone, oak log, terracotta | **+0** |
| Creative mining, 3 diamond ore | **+0** |
| Band | 40 WP required for Lv2, and 32 did not level |
| Reset | `0/40`, construction 0, extraction 0 |
| Second match | opened from baseline |

Mining twelve ore took about 60 seconds with a netherite pickaxe, so roughly
15 ore reaches level 2 on Extraction alone. **This is not a calibration
result.** Five of seven domains award nothing, so any observed rate
understates the intended curve and must not be read as evidence the bands need
changing.

**The harvest term is demonstrably live.** Five copper blocks produced 15, 20
and 23 WP on three separate runs. A(O) is fixed at 5 for those blocks, so the
harvest term supplied 10, 15 and 18 — it varies with what the break actually
dropped, which is the whole mechanism Fortune acts through.

**Fortune itself is not verified by me, and cannot be with this harness.**
mineflayer fails with `enchantments is not iterable` on any 1.21.11 enchanted
tool, so the bot cannot dig with a Fortune pickaxe at all. The plugin reads
whatever `BlockDropItemEvent` reports and never inspects the tool, and that same
path is what produced the varying harvest above — but reading more drops from an
enchanted pick is a vanilla behaviour I did not observe. Mining copper with and
without Fortune and comparing `/moba work` settles it in under a minute.

The self-placed-**ore** case is likewise unverified live: the bot failed to
place ore. The code path is the one the `HIGHEST` fix repaired and is asserted
by test, but I did not watch it refuse.

One apparent failure during testing was not one: `polished_deepslate` paid
1 WP rather than 2, which is correct — canon excludes stone derivatives from
Construction Blocks, and `MaterialCategories` already knew.

## Breakpoint rewards

Not re-verified in this pass. `TaskEffects` applies the automatic grants at
Lv4 (Efficiency I + Unbreaking I) and Lv7 (Yield I), and `Rewards` opens the
choice menu, both on the same path gameplay levelling now uses. Whether those
land correctly when reached through WP rather than through `/moba setlevel` is
untested, because the live run reached 32 WP and not 40.

## Diagnostics

`/moba work [player]` reports level, WP against the band cost, total earned
this match in WP and UAU, and a per-domain breakdown. An action bar names the
domain and source of each award. The HUD line shows `WP n/cost`. This is
Alpha/debug presentation, not permanent UX.
