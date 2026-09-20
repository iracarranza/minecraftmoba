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
| Extraction | per ore block **broken** | 1–6 WP by resource | rule from §10; magnitudes are NON-CANON ALPHA FIXTURES |

Two properties are load-bearing and are asserted by tests:

**Yield must not multiply Extraction.** §10 is explicit — *"WHAT YOU GOT →
material economy. WHAT IT TOOK TO GET IT → Extraction progression."* Credit is
per block broken and never per item dropped, so a Fortune pick changes the ore
in your inventory and not your level.

**Recovered material is not new acquisition.** `Provenance` already tracked
player-placed blocks, so breaking a block you placed earns nothing and the
place-and-break loop cannot farm levels.

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

## Live verification

Real gameplay only. No admin XP grant was used as evidence.

| Demonstration | Result |
|---|---|
| Baseline | `0/40 to Lv2` |
| Ordinary placement × 1 | `1/40`, construction 1 |
| Non-qualifying repetition | broke own placed block → still `1/40`, **extraction 0** |
| Construction Block × 1 (bricks) | `2/40`, construction 2 |
| Extraction: 8 iron + 4 diamond | `32/40`, **extraction 32** — exactly 8×2 + 4×4 |
| Band | 40 WP required for Lv2, and 32 did not level |
| Reset | `0/40`, construction 0, extraction 0 |
| Second match | opened from baseline |

Mining twelve ore took about 60 seconds with a netherite pickaxe, so roughly
15 ore reaches level 2 on Extraction alone. **This is not a calibration
result.** Five of seven domains award nothing, so any observed rate
understates the intended curve and must not be read as evidence the bands need
changing.

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
