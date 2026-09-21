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

---

# Second pass — corrections, then Production / Development / Exploration

## 1. Corrections made first

### Construction category vs Construction work

`MaterialCategories` was correct and is untouched. The handler was not.

`WorkPoints.place` paid **2 WP for a Construction Block and 1 WP for every other
placed block**, which read the UAU supplement's "ordinary useful placement = 1
WP" as "every `BlockPlaceEvent` = 1 WP". The supplement does not say that. The
word doing the work in that sentence is *useful*, and nothing available here
distinguishes a wall from a dirt block dropped underfoot to climb one step.

Two separate defects followed:

- **Logs, torches, dirt and crafting tables generated Construction
  progression.** That is not a category error in `MaterialCategories` — those
  materials are correctly *not* Construction Blocks — it is the handler treating
  the non-Construction-Block branch as automatically useful.
- **`place → break → place` was an unbounded loop.** No material was consumed,
  no time passed, and nothing was built, yet WP accrued on every cycle.

Fixed as two independent changes, because they are two independent questions:

1. **Ordinary placement is flagged UNRESOLVED and ships at `0`.** The 1 WP
   fixture is preserved in `config.yml` behind a comment saying what would have
   to exist to turn it back on. Understating Construction is recoverable;
   asserting that all placement is useful is not.
2. **Credit attaches to a position, not to an event.** A player is paid for a
   block position they have not built at before. Rebuilding the same position
   pays nothing, forever.

The position rule is deliberately *not* a cooldown or a diminishing return.
Both of those would also punish someone legitimately laying a fifty-block wall,
which is exactly the legitimate repetition the common rule protects. Building
somewhere new pays in full every time and as fast as the player likes; the only
thing that pays nothing is rebuilding ground already built.

Construction Block membership is unchanged. No log or torch was reclassified.

### Config path mismatch

`WorkPoints` read `progression.work.extraction.*`; `config.yml` defined
`progression.extraction.*` and defined neither Construction key at all. Because
`getInt(path, default)` cannot distinguish a missing key from a key that equals
the default, **every live Extraction value came from the Java fallback** —
silently, with no warning at load and no crash. The documented Alpha fixture was
not the thing being played.

Everything now lives under `progression.work.*`, and `ProgressionConfigTest`
scans `WorkPoints.java` for config reads and fails if any key it reads is
undefined. A second test asserts values that exist only because the file says so
(iron opportunity 2 against a Java fallback of 0), so the scan cannot pass
against a file that merely repeats the defaults.

### Level curve

Unchanged and now asserted. The sawtooth instantiates the established relative
requirement indices exactly:

| Band | Index | 40 × index | Config |
|---|---|---|---|
| Lv1–6 | 1.000 | 40 | 40 |
| Lv7–12 | 1.350 | 54 | 54 |
| Lv13–19 | 1.875 | 75 | 75 |
| Lv20–24 | 2.575 | 103 | 103 |
| Lv25–30 | 3.250 | 130 | 130 |

**40 WP is the Bootstrap per-level cost, not a universal per-level cost.** Kept
labelled ALPHA CALIBRATION.

---

## 2. Sources implemented from existing authority

### PRODUCTION — completed transformations into strategically useful outputs

Classification is enumerated per established category in `ProductionRecipes`,
by **result material**, split by how the result was obtained.

| Category | Examples | WP/item |
|---|---|---|
| Tool | pickaxe, axe, shovel, hoe | 3 |
| Equipment/weapon | sword, bow, shield, armour | 4 |
| Utility item | furnace, chest, torch, bucket, rail, minecart | 1 |
| Food/consumable | bread, cooked meats, golden apple | 1 |
| Strategic input | smelted ingots, scrap, charcoal, brick | 2 |
| Construction Block | bricks, terracotta, concrete, glass | 2 |

Shift-click crafting completes many transformations in one event, so the real
count is derived from the scarcest ingredient rather than assumed to be one.

### DEVELOPMENT — improvement of productive renewable state

| Event | WP | Why it cannot be spammed |
|---|---|---|
| Successful animal breeding (`EntityBreedEvent`) | 8 | Fires only on an actual pairing; a failed or cooling-down attempt never reaches it |
| Harvesting a **mature** crop | 2 | Maturity costs growth time that no amount of clicking shortens |

Credit deliberately attaches to harvesting a mature crop rather than to planting
a seed. Planting is the interaction a player can repeat as fast as they can
click; the mature crop, not the seed in the ground, is the improved productive
state. Player-placed provenance is deliberately *not* consulted here — for ore
it marks an opportunity already counted, but for a crop the player having
planted it is the entire point.

### EXPLORATION — novel Reach

Credited once per player per **registered Worksite** resolved, 6 WP, within 24
blocks. The registry is the frozen Alpha map's own, not an invented POI list.

Explicitly paying nothing: distance walked, chunks entered, re-crossing known
geography, and using a resolved Route. Resolution is remembered per player for
the match, so leaving and re-entering the same site pays nothing on return.
Per player rather than per team, because Reach is something a player has.

---

## 3. NON-CANON ALPHA FIXTURES introduced

Every number in §2 is a fixture. Those worth naming because they are the ones
most likely to be wrong:

- **Production per-item rates** (3 / 4 / 1 / 1 / 2 / 2). The category *set* is
  established; the magnitudes are not.
- **Utility at 1 WP/item interacts badly with stacked outputs.** One coal plus
  one stick makes four torches, so a coal ore (1 opportunity + 1 harvest = 2 WP)
  becomes 4 WP of torches. That is a calibration question, flagged rather than
  tuned away by special-casing torches.
- **Breeding 8 WP** against **mature crop 2 WP**.
- **Exploration 6 WP per site, 24-block resolution radius, 1s survey interval.**
- **Construction `ordinaryPlacement: 0`** — a fixture standing in for an
  unresolved question, not a balance choice.

---

## 4. Actions intentionally excluded as ambiguous

- **All unenumerated recipes**, including planks, sticks, wool, slabs and stone
  bricks. Understating Production is recoverable; guessing a classification for
  the long tail is not.
- **Ordinary block placement** — see §1.
- **Sugar cane, bamboo, kelp and cactus harvesting.** Age-based like crops, but
  they regrow from a stalk that is never replanted, so "mature" does not mean
  the same thing and the anti-spam argument does not transfer.
- **Silk Touch → H**, still unresolved from the first pass and still defaulting
  to 0.
- **Smelted results that are also craftable**, where the crafting direction is
  reversible: paid on smelting only.

## 5. Exploit loops tested

| Loop | Result |
|---|---|
| `place → break → place` at one position | 0 WP after the first placement, verified for 100 cycles |
| Ingot → storage block → ingot | 0 WP in both directions; the direction that could loop is exactly the direction that pays |
| Every storage block and recoverable unit (iron, gold, copper, diamond, emerald, coal, redstone, lapis, wheat, bone meal, raw ores, slime, hay, dried kelp) | 0 WP crafted |
| Dried kelp both directions | pays smelted, not crafted |
| Plant seed → break seed | 0 WP; only maturity pays |
| Leave and re-enter a resolved site | 0 WP after the first resolution |
| Construction Block derivatives (panes, slabs) | still not Construction Blocks |

All by unit test. **Not yet verified on a live server** — see §6.

## 6. Normal-play WP composition — MODELLED, not measured

A live fresh-Survival opening has **not** been run for this pass. The following
is arithmetic over the fixtures for a conventional opening (wood → tools →
stone → coal → iron → a small farm), and should be read as a prediction to
check against a real session, not as a measurement:

| Domain | Opening activity | Approx. WP |
|---|---|---|
| Production | table, 2 stone tools, furnace, torches, 3 iron tools/armour pieces | ~25–35 |
| Extraction | ~10 coal (2 each), ~8 iron (2+1) | ~45 |
| Development | one wheat harvest cycle, one breeding pair | ~15 |
| Exploration | 1–2 Worksites resolved in passing | ~6–12 |
| Construction | 0 unless the player makes and places Construction Blocks | 0 |

Roughly 90–110 WP, or Lv1 → ~Lv3 in a first session, with Extraction still the
largest single contributor and Construction now contributing nothing to an
opening that never produces bricks, terracotta, concrete or glass. Whether that
last point is correct or merely conservative is the main thing a live session
should judge.

---

## Reported candidates, not implemented

**LOGISTICS.** Two existing recognized events are genuine candidates and neither
was wired: Route recognition in `Routes` (an actual established connectivity
relationship rather than item-distance), and Worksite **capitalization** in
`Contributions.capitalize`, which is already a real availability relationship
between a team and a site. Generic item-distance WP was not implemented and
should not be. Capitalization in particular may belong to Logistics, to
Construction, or to its own recognition premium; that is a design question, not
an implementation one.

**COMBAT.** Currently detectable candidates: a player kill (`PlayerDeathEvent`
with a player killer), damage dealt while inside a contested Worksite radius,
and a monster killed within a Worksite or Development Zone. Each is detectable
today. None was implemented, because all three reduce to damage or kills unless
"value secured or contested under threat" is given a definition, and inventing
that definition here would fix the cheapest reading of it in code.

Still unattached from the first pass: the **+64 WP Construct recognition
premium**, which remains blocked on what qualifies as a Construct.

---

# Third pass — higher-resolution curve and source recalibration

## The level requirement function

    cost(b, i) = 300 * bandMultiplier[b] * 1.15^i

`i` counts advancements **within** an economic band from 0. The band
multipliers are the established relative requirement indices, unchanged:
Bootstrap 1.000, Established 1.350, Developed 1.875, Advanced 2.575,
Endgame 3.250. Values round to 5-WP steps.

The **300 baseline and the 1.15 in-band factor are WORKING ALPHA CALIBRATION,
not canon.**

| Band | Levels | First | Last |
|---|---|---|---|
| Bootstrap | 1→2 … 6→7 | 300 | 605 |
| Established | 7→8 … 12→13 | 405 | 815 |
| Developed | 13→14 … 19→20 | 560 | 1300 |
| Advanced | 20→21 … 24→25 | 770 | 1350 |
| Endgame | 25→26 … 29→30 | 975 | 1705 |

The requirement **drops** at each boundary. That is the design: a new economic
phase restarts from its own multiplier and then compounds within itself, so
entering a phase is a step down in per-level cost and a step up in the cost that
phase will eventually reach. Three tests pin this, because `605` followed by
`405` reads like a transcription bug and would be "fixed" by the next person to
look at it: one checks every entry against the generating formula, one asserts
the drop at all four boundaries, one asserts each band peaks above the last.

The table is stored per level (`progression.levelCosts`) rather than as ranges,
so the curve is inspectable and a recalibration stays a config edit.

## Source recalibration

Fixtures were calibrated against a 40-WP level. The scaling is **not** a flat
10×, because the point of the new resolution is that 1 WP can remain the
minimum accounting unit for a genuinely small action while substantive work
occupies the tens.

| Source | Old | New | Note |
|---|---|---|---|
| Ordinary placement | 0 | 0 | UNRESOLVED, see below |
| Construction Block placement | 2 | 20 | |
| Extraction A(O), coal/copper | 1 | 10 | |
| Extraction A(O), iron/redstone/lapis | 2 | 20 | |
| Extraction A(O), gold | 3 | 30 | |
| Extraction A(O), diamond/emerald | 4 | 40 | |
| Extraction A(O), ancient debris | 6 | 60 | |
| Extraction q (harvest coefficient) | 1 | 10 | |
| Production tool | 3 | 30 | |
| Production equipment | 4 | 40 | |
| Production utility | 1 | **1** | held at the floor |
| Production consumable | 1 | **6** | below 10×, arrives in stacks |
| Production strategic input | 2 | 20 | |
| Production Construction Block | 2 | 20 | |
| Mature crop harvest | 2 | 20 | |
| Successful breeding | 8 | 80 | |
| First Worksite resolution | 6 | 60 | |

Three deliberate departures from 10×:

- **Utility stays at 1.** It is the category that arrives four at a time from
  one piece of coal. It is the clearest case of an action that is real but
  small, which is exactly what the 1-WP floor exists for.
- **Consumable is 6, not 10.** Food is smelted and crafted in stacks; at 10, one
  stack of cooked meat would outweigh a bred animal several times over.
- **Ordinary placement stays 0.** See below.

**Extraction was rescaled, not replaced.** `WP = A(O) + qH` is intact, with both
A and q raised together so Fortune still moves H and never A(O), and so Yield
remains the same fraction of a break as it was rather than being quietly
demoted by scaling A alone. The known q-asymmetry survives too: multi-drop ores
still accrue more harvest WP than single-drop ones. Per-ore q would be a new
design decision, so it stays flagged rather than tuned away.

### Ordinary placement: 0 is a blocked question, not a conclusion

The blocker is **qualification, not magnitude**, and the new resolution changes
the magnitude argument entirely. At 40 WP/level, a 1-WP placement was 1/40th of
a level and any blanket placement rule was economically absurd on its face. At
300 WP/level it is 1/300th, which makes a future *"ordinary **useful**
placement = 1 WP"* rule economically plausible — a player placing 300 blocks of
genuine structure earning one level is a defensible claim in a way it was not
before. What is still missing is a test for *useful*. When one exists, the
fixture is a single config edit.

## Modelled opening composition under the new curve

Same conventional opening as the second pass (wood → stone tools → coal → iron
→ cooked food → a small wheat farm and one breeding pair), recomputed against
the recalibrated fixtures. **Modelled, not measured** — no live session has been
run.

| Domain | Detail | WP |
|---|---|---|
| Extraction | 10 coal (20 each), 8 iron (30 each) | 440 |
| Production | table, 4 tools, 2 weapons, furnace, 16 torches, 8 ingots, 8 steak | 426 |
| Development | 9 mature wheat, one breeding | 260 |
| Exploration | 1–2 Worksites resolved in passing | 60–120 |
| Construction | no Construction Blocks made or placed | 0 |

**Total ≈ 1,186–1,246 WP → Lv4, roughly a third to a half of the way to Lv5.**

Cumulative requirements for reference: Lv2 at 300, Lv3 at 645, Lv4 at 1,040,
Lv5 at 1,495, Lv7 at 2,625.

The curve was **not** tuned to reproduce the old Lv1→Lv3 result; the recalibrated
sources happen to land one level further, and that is reported rather than
corrected. Two observations worth carrying into a live session:

- Extraction and Production are now near-equal contributors (440 vs 426), where
  Extraction previously dominated. That is mostly the 8 smelted ingots at 20
  each — refinement is now a large share of an opening, which may be right or
  may mean `strategic_input` is too high.
- Construction still contributes **nothing** to an opening that never produces
  bricks, terracotta, concrete or glass. That remains the single most likely
  place the model is wrong, and it is the same question as the unresolved
  ordinary-placement rule above.

## Correction — ordinary placement is 1 WP, and 1 WP is atomic

The previous two passes shipped `ordinaryPlacement: 0` and called it an
unresolved fixture awaiting a usefulness test. That framing was wrong, and the
value is now **1**.

**1 WP is the atomic Work Point**: the smallest legible unit of progression, not
a provisional or exceptional value. Placing a block is mundane legitimate
Minecraft activity, and mundane legitimate activity earns exactly one of those.

The reasoning the 0 replaced: mundane work is supposed to become
**insufficient**, not **worthless**, and the thing that makes it insufficient is
the level-cost curve. The same 1 WP placement is 1/300th of a Bootstrap level
and 1/1705th of an Endgame one, so primitive activity prices itself out of
competitiveness on its own, across two and a half orders of magnitude, without
ever being declared not to be work. Awarding 0 made that same point by deleting
the activity from the economy instead of pricing it — a different and worse
claim, and one that left an early player's most common action reading as
literally valueless.

This does **not** reopen the farming question, because the magnitude was never
what closed it. The anti-recycling rule is what closes it: credit attaches to a
block **position** the player has not built at before, so `place → break →
place` pays once and building somewhere new always pays in full
(`ConstructionWorkTest`). That rule is unchanged and was not relaxed here.

Construction Block placement remains 20. Membership remains classes.md's.

### Modelled opening, revised

Construction now contributes. A conventional opening places torches, a table, a
furnace, a chest, some covering and scaffolding, and a small farm's worth of
farmland and fencing — call it 60–100 first-time positions.

| Domain | WP |
|---|---|
| Extraction | 440 |
| Production | 426 |
| Development | 260 |
| Exploration | 60–120 |
| Construction | 60–100 |

**Total ≈ 1,246–1,346 WP → Lv4, roughly half to two-thirds of the way to Lv5.**

Still Lv4; the level position did not move, which is the point. What changed is
that Construction stopped reading as zero for a player who spent the session
building, and the domain breakdown a tester sees in `/moba` now reflects what
they actually did.
