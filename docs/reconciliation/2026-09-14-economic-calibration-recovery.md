# Economic Calibration Recovery — 14 September 2026

Status: reconciliation checkpoint. This file records design decisions and quantitative calibration developed after the last broad Work-mode recovery. It distinguishes canon from calibration fixtures and prevents superseded assumptions from re-entering the manuscript.

## 1. Canon corrections

### Copper is a Primary Material
Copper is now a **Primary Material**. Current set: Copper, Iron, Gold, Diamond, Netherite, Leather. Any current-facing statement excluding Copper is superseded. Copper is therefore eligible for material-specific systems such as Kitfighter Salvage when the recipe otherwise qualifies.

### No functional offhand
The game has **no functional offhand slot**. Shields, buckets, food, light sources, maps, utility and class items compete with weapons, tools, blocks, resources, provisioning and cargo in ordinary inventory/active-item handling. Any class ability requiring vanilla offhand functionality requires redesign rather than an exception silently being assumed.

### Match clock
Day is **6 minutes** and night is **6 minutes**; a full cycle is 12 minutes. In a 48-minute analytical match: 0–6 Day 1; 6–12 Night 1; 12–18 Day 2; 18–24 Night 2; 24–30 Day 3; 30–36 Night 3; 36–42 Day 4; 42–48 Night 4. Worksites open at sunset, so natural pulses are **6, 18, 30, 42 minutes**. This supersedes older vanilla-length, 10-minute-cycle, and first-sunset-near-10m assumptions.

## 2. Inventory economy
There is one inventory economy:

`inventory = weapons + tools + food + light + blocks + buckets + utility + class items + resources + mission cargo`.

No-offhand is therefore an economic rule as well as a control/equipment rule. At low capacity, utility directly displaces cargo or provisioning, increasing the strategic value of specialization, caches, remote provisioning, Logistics, and inventory progression.

**Economic Breadth Capacity** is a useful analysis metric: the number of distinct economically relevant stacks a player can carry without sacrificing mission equipment or expedition provisioning. It is analytical, not yet a player-facing stat. Exact capacity-curve values used in sensitivity work remain non-canon unless separately adopted.

## 3. Equipment-material thresholds
For seven players, seven full armor sets cost 168 tier material and seven swords cost 14. Therefore **182 material = universal armor+sword saturation for a tier**.

This is a gross recipe threshold, not an instantaneous purchase rule. Rational teams allocate persistent stock asymmetrically among occupational tools, high-exposure players, weapons, armor, utility, infrastructure/class uses, and reserves.

The older ~247-Iron generic mature-utility benchmark is retired. It bundled speculative utility into a universal kit and understated inventory opportunity cost. Approximately 200–219 remains useful only as an upper operational-maturity reference when same-tier occupational tools are included; **182 is the clean universal combat-tier threshold**.

## 4. Durability and equipment persistence
Do not globally compress durability because match time is compressed. Compress only where vanilla durability would outlive the meaningful match-scale economic cycle for that item's actual use pattern.

Current interpretation: armor acquisition capital greatly exceeds ordinary armor replacement; ordinary sword replacement is secondary under expected use and universal Unbreaking I; pickaxes are the strongest candidate for deliberate durability compression because Extraction is unusually tool-use-heavy; Diamond/Netherite equipment should primarily behave as capital.

Working pickaxe sensitivity Wood 16 / Stone 36 / Copper 60 / Iron 96 remains **non-canon**.

## 5. Copper correction and calibration
Copper ore drops 2–5 raw Copper, average ~3.5 before Fortune/Yield. Earlier near-1:1 Copper calculations are superseded. Useful expected-material sensitivity: F0 ~3.5 raw/ore; Yield I ~4.67; Yield II ~6.125; Yield III ~7.7.

Current **non-canon calibration fixture**: ordinary economically relevant Copper opportunity per team ~45–55 physical ore; early Copper Mining Site ~8–12 ore. These are opportunity-calibration values, not worldgen quotas.

Because Copper is Primary, Kitfighter Salvage can lower net Copper expenditure. Universal armor+sword saturation remains 182 gross. Seven armor sets create 28 qualifying armor crafts under the current >=3-Primary-Material trigger; at the Lv8 five-stack threshold, centralizing those crafts through Kitfighter can return about 10 Copper. Production reduces net expenditure without redefining the physical equipment threshold.

## 6. Iron calibration
Current **non-canon calibration fixture**: ordinary economically relevant Iron opportunity per team ~130–140 physical ore; Iron Mining Site ~40–50 ore, central ~45.

Intended relationship: ordinary balanced development reaches broad/majority Iron; universal Iron normally indicates exceptional opportunity, economic specialization, or sufficiently mature late play; a Mining Site can push a balanced team across a finite ordinary-world material ceiling; strong Yield can partially substitute for Worksite inflation by amplifying the same geology.

Do not interpret material equal to several full kits as literal complete-kit allocation. Mixed equipment states are rational.

## 7. Distinct economic functions
Preserve this separation:

- **Efficiency** = exploitation tempo, tool persistence, exposure reduction.
- **Yield** = material amplification of finite physical opportunity.
- **Production** = efficient conversion of acquired inputs into useful outputs.
- **Worksite** = additional/concentrated exceptional opportunity.

Efficiency does not multiply physical ore. It can exhaust geology faster but cannot by itself overcome a finite material ceiling. Yield changes realized material from the same geology. Production acts after acquisition. Worksites add/concentrate opportunity.

## 8. Diamond and Netherite calibration
Current **non-canon fixtures**: ordinary late/deep Diamond ~14–18 ore; late Diamond Worksite ~8–12 ore; ordinary late Ancient Debris ~4–8 blocks; apex/very-late Ancient Debris opportunity ~8–12 blocks.

A full Diamond armor+sword investment costs 26 Diamond. Target result is roughly one major Diamond investment for a balanced team and possibly two under strong economic specialization/opportunity, not universal Diamond.

Ancient Debris is not Fortune-amplified. Four Debris are required per Netherite ingot/item upgrade before Gold, template, and Smithing-capability constraints. Combined fixture supports roughly 3–5 individual Netherite upgrades in sufficiently long/developed matches, not teamwide saturation.

## 9. Sunset Worksite economy
Corrected sunset pulses: 6, 18, 30, 42 minutes. A specific Worksite is not guaranteed each sunset; current direction selects a phase-dependent subset from an eligible pool. Availability and intensity are separate tuning axes.

Useful **test vocabulary, not mandatory contents**: 6m Copper/Coal/opening industry; 18m Iron/system-resource opportunity; 30m Gold/Lapis/Diamond/advanced capability; 42m apex resources/Ancient Debris/Smithing relationships.

Worksites are accelerator/inflation sources rather than tier permission. Rational responses include immediate exploitation, delayed exploitation for later Yield, partial exploitation, ignoring the site, and losing it to the enemy.

## 10. Wider resource-coverage hypothesis
Analytical coverage bands for future worldgen calibration, **not canon generation targets**:

- very high ordinary coverage (~90%+ competitive demand): wood, basic food ecology, coal, ordinary construction stone;
- high ordinary / strategically amplifiable (~75–100%): Copper, sand, gravel, clay, common livestock, wheat;
- controlled strategic (~55–80%): Iron, Redstone, Lapis, specialty crops/livestock and similar resources;
- power resources (~20–50% of relevant saturation demand): Gold, Diamond, high-value enchanting inputs;
- apex resources: ordinary coverage may be very low, with exceptional opportunity and contest determining access.

## 11. Current macro equipment thesis
> Universal Copper should emerge through ordinary team development. Majority Iron should emerge through ordinary team development. Universal Iron should normally indicate exceptional opportunity, economic specialization, or sufficiently mature late play. Diamond should remain selective capital. Netherite should remain apex item-level investment.

## 12. XP frontier and lost UAU discussion
The next unresolved calibration dependency is XP valuation. A later chat discussion introduced **Useful Action Units (UAU)** as a basis for calculating XP values, but that exchange is not recoverable in current context. Do not invent its exact formula or silently canonize a reconstruction.

Recoverable state immediately before the lost discussion:

- Level cap 30.
- Relative XP requirement bands used in progression work: 1.000x Lv1–6; 1.350x Lv7–12; 1.875x Lv13–19; 2.575x Lv20–24; 3.250x Lv25–30.
- A normalized 48-minute level clock has been used only as a sensitivity fixture; it is not an imposed progression schedule.
- Exact XP yields for resource attainment, exploration, regenerative opportunities, Combat/Mob Swarms, Worksite contributions, objectives, and other legitimate activities remain to be calibrated.
- Infrastructure integration modifies legitimate XP-generating activity rather than creating passive/AFK XP.

The next XP pass should reconstruct UAU explicitly from first principles, mark every assumption, and test whether plausible XP values produce the desired match progression rather than tuning rewards merely to force predetermined timestamps.

## 13. Superseded assumptions to reject
Do not reintroduce: Copper excluded from Primary Materials; a functional/free offhand; vanilla-length day/night timing; a 6-minute entire day/night cycle; first sunset near 10m or second near 30m; Copper opportunity based on near-1:1 drops; Efficiency as a physical-resource multiplier; universal 247-Iron mature-kit demand; large routine sword/armor replacement as the main Copper/Iron sink; Worksites as mandatory tier permission; normalized minute-to-level sensitivity as canonical progression timing; or an invented UAU formula attributed to the lost conversation.


**15 September clarification:** These resource sensitivity examples describe activation-phase possibilities, not fixed site tiers or canonical quantities. The established Factory progression is 6m Blast Furnace + Smoker; 18m **TBD / OPEN**; 30m Enchanting Table; 42m Smithing Table. Surplus dormant locations receive packages only on selection. Capitalization and exploitation work, downstream Work Stock and the persistent +1 team Infrastructure Slot from first capture all contribute to acceleration; no flat XP value is assigned to the slot. See [objectives.md](../../objectives.md#11-worksites).
