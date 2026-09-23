# Worldgen and Initial System Balancing

This is the active quantitative design specification for converting generated geography into realized human economic activity. Established spatial principles live in [maps.md](../maps.md#spatial-opportunities-and-initial-balancing); this document owns the proposed experiment and its assumptions, not final game balance. Read it with [infrastructure.md](../infrastructure.md), the [manuscript](manuscript/Minecraft-MOBA-Design.md), and the [reconciliation](reconciliation/2026-09-12-worldgen-balancing.md).

## Spatial terminology reconciliation — 22 September 2026

Use **Strategic Depth** for outward Homebase/opening relationship and **Regional
Character** for Map Type geographic composition. Earlier "regional depth" here
means the former, not the Ocean-to-Alpine regional gradient. Keep both separate
from current travel cost. The [Core/Socket contract](../maps.md#competitive-spatial-contract--established-clarification-22-september-2026)
controls symmetry only inside the Core/interface. Natural hinterlands supply a
distributed opening floor and concentration ceiling; Wilderness asymmetry is not
a deficit without gameplay evidence. Depth allows richness in quantity/
concentration as well as novelty; actual candidate density need not rise
monotonically. Existing X/T observations and no-quota rules survive. The model
below remains a proposal, not a generic gradient already implemented.

## CANON / ESTABLISHED principles

Worldgen creates spatial opportunities, not income rates. Depth's existing access costs and resource rewards remain, with regenerative eligibility and vocabulary additive. Candidate Density, Regenerative Eligibility, and Regenerative Vocabulary are distinct. X/T at shallow A and Y/T at deeper Z, Y/T > X/T, express an emergent eligible-candidate share; neither is a node-placement quota. Ecology/geology determine actual candidates, which need not become denser with depth. T may differ between regions.

Finite and regenerative opportunities both require human search, access, exploitation, inventory management, and delivery. Regeneration restores opportunity rather than income. Recurring availability creates reasons to revisit geography; it does not promise constant throughput. MNOP/Q separates logistical remoteness from Strategic Exposure. Q traffic follows connections and terrain, may pass through a resource region without Q occupying it, and need not make every remote settlement contested. Exposure, density, and defensibility are not monotonic depth functions.

Preserve these layers in both attribution and experiments:

| Layer | What it contributes |
|---|---|
| Worldgen | Geography, candidate resources, eligible vocabulary, access topology and Q locations |
| Vanilla-intrinsic mechanics | Ordinary movement, mining, growth, storage, item stacking, transport and other Minecraft interactions |
| Emergent organization | Intelligent selection of paths, forward caches, farming, task division, deposit policies and coordinated hauling |
| Character progression | Personal Health, Hunger, Inventory, Efficiency, Yield and applicable progression constraints |
| Recognized MOBA infrastructure | The additional benefits and requirements of Constructs, Development Zones, Routes and Supply Lines |
| Classes / integration | Authored exceptional capabilities, temporary abilities and legitimate integration effects |

These are analytical layers, not an unlock order. A physical road is not automatically a recognized Route; caches and manual hauling are not Supply Lines; a crop patch or ordinary farm is not a Development Zone; a useful shelter is not automatically a Construct. Keep map-authored Starter Routes distinct as well. Recognition requires the relevant current contract. Ordinary organization remains useful before recognition and must not have its benefits credited to recognition or a class.

## CURRENT WORKING DIRECTIONS

### Scope and model boundary

Build a small stochastic opportunity-field model before attempting a whole-match simulation. Generate a reproducible synthetic world with configurable depth bands and several ecology/geology profiles, then simulate human expeditions through it. Bands summarize regional conditions; they do not prescribe rings, fixed counts, names, resource tiers, or equal area. This is an experimental plan; no simulation has been run by this documentation revision.

Start with ordinary intelligent behavior under a declared character state. Compare progression and organization separately, then add recognized infrastructure, classes, and integration. Reuse candidate worlds and random seeds across comparisons where feasible. Do not fit worldgen backward to a desired income curve or level schedule. Existing progression proposals are inputs with provenance, not independently validated constants.

### Region structure

A Region record should hold:

- Identity, spatial extent and sampling unit; regional depth descriptor or band and provenance of its calculation; team-relative access where relevant.
- Biome, ecology, geology, terrain, elevation, connectivity, traversal methods and baseline travel costs.
- Actual candidate opportunity IDs, counts/density, resource portfolio, finite and regeneration-eligible subsets, and eligible regenerative vocabulary. Derive aggregates from candidates instead of separately inventing totals.
- Q locations and references to external Q destinations whose approaches cross the region; activity state, plausible traffic paths, team-relative Strategic Exposure and local danger as separate fields.
- Storage and construction possibilities, current ordinary development, recognized infrastructure state and independently measured defensibility.
- Current world time, depletion/availability summary and optional phase state; flags distinguishing observed world data from synthetic assumptions.

Store baseline regional depth separately from current expedition travel cost. A bridge should reduce traversal cost without silently rerolling the world's resource population. Whether generation depth is fixed or dynamically updated is an Open design decision; use a declared fixed-generation assumption for a first experiment if needed.

### ResourceOpportunity structure

Each candidate should hold:

- Identity, region and location or spatial support; resource/encounter kind and ecological/geological conditions.
- Resource portfolio and prospective yield by item, with incidental material and mixed drops; no fixed regional income property.
- Eligibility result and reason, finite/regenerative behavior, renewal conditions, local remaining stock or population, current availability and next possible change.
- Current manifestation, including day/night alternatives where applicable. Keep potential vocabulary separate from the manifestation drawn now.
- Discoverability and search requirements, access requirements, exploitation work/time, hazards, and relevant tools or capabilities.
- Inventory consequences: item types, stack limits, partial stacks, mandatory operating requirements, and opportunities for local consumption or conversion.
- Player/team knowledge state separately from omniscient world truth; history of discovery, extraction, exhaustion and renewal for diagnostics.

An opportunity is not automatically a block, vein, patch, mob, or unit of value. Select and document a consistent candidate granularity before calculating X/T. A finite opportunity's renewal rule is none; regeneration eligibility does not erase exhaustion or imply its current manifestation is available.

### Generation and opportunity accounting

Generate or sample ecology/geology and connected terrain; identify supported candidate opportunities; evaluate depth-sensitive eligibility and vocabulary against those candidates; initialize finite stocks and regenerative availability; then evaluate strategic access and Q traffic. Worldgen and strategic evaluation may iterate, but strategic polygons must not substitute for actual resources.

Report candidate count, spatial density, eligible count, eligible share and available count independently. At T = 0, share is undefined, not zero eligibility. If a separate value-weighted share is useful, declare item valuation, time horizon and demand context. Never hold regional total opportunity equal merely to simplify the depth comparison. Inspect distributions across seeds and comparable regions before considering any rejection criteria; do not repair a failed gradient by filling missing node quotas.

### Human expedition cycle

Use search → discovery → exploitation burst → local exhaustion → search, with choices to revisit, deposit, return, switch task, or consume locally. This cycle supports both finite deposits and intermittently available recurring opportunities. Search includes failed searches and revisits to unavailable sites; known location and known readiness are different information states.

An expedition carries primary-task cargo, opportunistic cargo, mandatory loadout, food, tools, traversal materials, and reserved headroom for future discoveries. Model physical slot occupancy, bulk quantities, and functional capacity separately. Mixed partially filled stacks can bind capacity well before a theoretical homogeneous inventory fills. Blocks broken, useful yield, and strategic value acquired are separate observables.

Inventory Policy includes pickup, ignore, discard, reserve, deposit, destination and return decisions. Deposit Policy is only one component: capacity-triggered, passing-storage, timed, demand-triggered and hybrid policies are experimental behaviors. Local exhaustion, risk, provisioning or urgent team demand may cause return before capacity binds. A human may discard common material to retain an active valuable opportunity. Policies should be bounded heuristics, not omniscient perfect optimizers; compare them rather than asserting a single rational policy.

Include outbound travel, food consumption, exploitation, incidental combat, interruptions, local storage and onward delivery. Remote workers do not have permanent access to full homeland stores. Temporary Development abilities should act over their actual duration, eligible area, cooldown and player presence; do not silently replace them with a permanent multiplier.

### Strategic Exposure from Q

Represent terrain/connectivity as a small graph or equivalent spatial model. Q creates destination demand whose plausible approach paths produce regional visitation. Vary Q activity and alternate paths; distinguish ordinary paths from recognized Routes. Baseline exposure can be a provisional traffic estimate before full enemy simulation, provided its assumptions are visible. Do not equate exposure with deterministic loss or a circular hazard radius.

Compare a recurring region near a strategically attractive Q with a low-exposure remote region, plus a sparse but heavily traversed connection. These are sensitivity scenarios, not required map archetypes. Test whether investment lowers travel/provisioning costs while some traffic remains; allow investments to redirect traffic and improve defense too. Cooldown-driven circulation and Q-driven circulation are separate causes.

### Outcome stages and ledgers

Track Potential Value → Available Value → Discovered Value → Extracted Value → Carried Value → Delivered Value → Consumed/Applied Value.

| Stage | Meaning for a declared opportunity cohort and horizon |
|---|---|
| Potential Value | Opportunity that the world could support under its rules |
| Available Value | Current exploitable manifestation or stock |
| Discovered Value | Available opportunity found or recognized by the relevant players |
| Extracted Value | Successfully harvested, mined, acquired or otherwise released by human action |
| Carried Value | Acquired resources retained in personal or carrier inventory |
| Delivered Value | Resources reaching an explicitly named demand location or recipient |
| Consumed/Applied Value | Resources used as food, production inputs, equipment, construction or other relevant activity |

This is an analytical conversion chain, not a requirement that every item visit homeland storage. Local consumption or construction may apply value directly; record its destination and use. Potential/available stock snapshots and cumulative extraction/delivery flows are not interchangeable quantities. Renewals create new availability episodes; label cohorts or events to avoid counting repeated access as newly generated finite value. Item counts remain primary; a scalar “value” requires an explicit valuation rule. Transformations require input/output accounting rather than a false universal numerical funnel.

Distinguish personal stock, local stored stock, in-transit stock, central stock and available-at-demand stock. Production rate, deposit rate, transport rate, delivery rate and consumption rate are different. Installed network capacity can remain idle when upstream storage is empty; larger Inventory may improve individual uptime while delaying deposits and team availability. Delivery time and ending stock matter as much as apparent worker efficiency. Do not infer automatic cargo loss on death: honor the current KeepInventory direction and keep special-cargo behavior explicitly configurable pending its contract.

### Implementation sequence and diagnostics

1. Declare candidate granularity, spatial units, generation-depth assumption, simulation horizon, random seed, item catalog and policy assumptions. Keep every chosen trial number in a parameter manifest labeled Prototype/test.
2. Generate connected regions and candidate populations; derive eligibility and inspect the depth gradient, composition and sparse-region cases.
3. Add finite depletion, renewal state and current manifestations. Verify that an untouched world never credits a player inventory and exhausted finite opportunity does not restore.
4. Add imperfect player knowledge and expeditions, mixed inventories, provisioning, exploitation bursts and deposits. Record time spent traveling, searching, working, waiting and returning.
5. Add Q traffic and operational interruption as separate variables from terrain and density. Exercise both contested and low-exposure settlements.
6. Compare organization, progression, infrastructure and class effects in controlled runs. Report delivery distributions, delayed stocks, network utilization and unmet demand alongside acquired items.
7. Only then evaluate proposed Hunger/Inventory curves, Supply Line capacity, Development timing, XP pacing and Temporal Windows. Do not claim final balance from one seed, one horizon or one policy.

Use deterministic event traces for debugging and multiple seeds for sensitivity. Check item conservation across inventories and containers, finite depletion, legal renewal, slot occupancy, no transfer from empty stock, and no benefit from unrecognized infrastructure. Distinguish unchanged mechanisms from the new hypotheses being varied. Extend to Construction's reverse flow—storage to inventory to remote placement—and input-consuming Production after the acquisition/delivery baseline works. No whole-match implementation is authorized by this specification alone.

## NON-CANON SENSITIVITY EXAMPLES

The earlier 48 ore/min gathering fixture is only a vacuum benchmark: a homogeneous, perfectly stackable output without search, incidental pickups or depletion. It isolated carrying-capacity and batching effects. Its bottleneck sequence—Character capacity to organization to transport—remains a hypothesis to retest with realistic opportunity fields, not a demonstrated full-game result.

Illustrative percentages, four named depth bands, exact regeneration periods, exploit-time examples, species/composition examples, trial travel distances and speeds, output rates, inventory reservations, supply rates and numerical value funnels are not selected balance. Preserve their provenance if reused; do not copy them into defaults disguised as canon. Example level samples are sampling choices, not new progression milestones. Match lengths, Temporal Window durations and existing class curves retain their own status and unresolved conflicts.

## OPEN QUESTIONS

- What is one candidate opportunity, and at what spatial resolution should counts and density be compared? Is a separate weighted-value metric useful, and under what demand/horizon assumptions?
- How is regional depth measured for both teams, and is its generation meaning fixed after infrastructure changes? At which aggregation scale must the intended eligible-share gradient hold, with what variation?
- Which ecological/geological conditions qualify each regenerative kind? How are renewable farms, managed populations, world regeneration and Worksite-generated resources distinguished in implementation?
- What renews, where, under what triggers, with what limits? What happens after terrain destruction, relocation, prolonged absence or management? Does phase gate opportunities, and how do day/night manifestations transition?
- What Q states generate traffic, how do players choose paths, and how does learned information change risk? What exposure distribution supports both contested operations and viable remote settlements?
- Which search, discard, provision, revisit and deposit heuristics approximate human behavior well enough? What demand destinations and local uses should the first model include?
- How should recognition, intrinsic benefits, temporary class abilities, nighttime effects and integration be represented while their existing contracts remain unsettled?
- Which outcome distributions and player observations count as satisfactory balance? The model should expose tradeoffs before targets are selected.

Resume by implementing the small Region/ResourceOpportunity prototype and its event ledger, declaring trial assumptions above. The immediate task is to measure how humans realize world opportunity; it is not to re-derive these principles or choose final income rates.
