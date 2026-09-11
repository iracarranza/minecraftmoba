# Default Opportunity Relationship shadow analysis

Authority: `specs/mapseedsearchspec4.md`. Diagnostic only. Current selections, eligibility, scoring, homelands, dimensions, terrain regions, local continuation and deep-network logic are unchanged. This is an explanation of frozen choices—not a new fitting decision or physical validation.

## Required report questions

1. **Does this explain suspicious selections better?** It separates structural access from payoff: a local directed path can be an unexplained opportunity, while a terrestrial dead end may have water-network access. The improvement is diagnostic, not proof that the provisional semantic rules are ready for selection.
2. **930010639 North coast:** see the eight explicit coastal questions below; surface connectivity and shore projection are separated from observed payoff and unverified embarkation/navigation.
3. **Locally valid but strategically unexplained:** 29 selected relationships are Incomplete in this shadow model: 930005557/N1 highland approach; 930005557/N3 inland-water relationship; 930005557/S3 highland approach; 930006815/N1 forest entrance; 930006815/N2 highland approach; 930006815/N3 persistent regional threshold; 930006815/S1 forest entrance; 930006815/S2 persistent regional threshold; 930007222/N1 highland approach; 930007222/N2 slope-foot approach; 930007222/N3 persistent regional threshold; 930007222/S3 inland-water relationship; 930010639/S1 slope-foot approach; 930010639/S2 persistent regional threshold; 930010639/S3 inland-water relationship; 930012642/N1 slope-foot approach; 930012642/N3 inland-water relationship; 930012642/S2 persistent regional threshold; 930012642/S3 inland-water relationship; 930015734/S1 persistent regional threshold; 930016664/N1 slope-foot approach; 930016664/S1 forest entrance; 930016664/S2 highland approach; 930016664/S3 inland-water relationship; 930019528/N2 inland-water relationship; 930019528/N3 inland-water relationship; 930019528/S1 highland approach; 930019528/S2 slope-foot approach; 930019528/S3 persistent regional threshold. Incomplete means required opportunity evidence was not established, not that the actual terrain is proven empty.
4. **Promising rejected candidates:** 55/192 sampled unselected candidates are Complete or Structurally Supported. See the disagreement inventory. Sampling is stratified, not exhaustive; no eligibility exception or reselection is applied.
5. **Completion counts:** complete: 7, structurally_supported: 10, incomplete: 29. Complete is conditional sampled evidence, not a player-scale pass.
6. **Authored payoff dependence:** 10/46 selected relationships have coherent structure but lack established natural payoff; future authored payoff is one possible completion, not a mandate to place content. Incomplete relationships cannot be rescued by an arbitrary POI. Complete relationships may still need ordinary map implementation, but not invented payoff to explain them.
7. **Strongly redundant whole sets:** none established. Individual substitute/overlapping pairs are reported below. Unknown payoff is not proof of complementarity or proof that an entire set is interchangeable.
8. **Seed differences:** yes, measured inventories differ in observed village/geographic-system access, water projection potential, functional evidence and overlaps. These differences are not an aggregate quality ranking or a winning-seed decision.
9. **Scale:** differentiation, networking and spatial/opportunity contestation are reported separately. No healthy scale target is established. Unresolved payoff and conditional water reach prevent a general assertion that every map realizes the intended phase order; map dimensions are unchanged.
10. **Future selection signals:** evidence-linked payoff, same-system duplication, entered-system persistence, and separate water/spatial convergence merit manual evaluation. Port clustering, four-layer persistence, exact branch-context labels, inferred access privilege, commitment tiers, and geographic value remain too provisional to govern selection now.

**Another design/validation pass is needed before this model influences selection.** Review the shadow disagreements manually, especially access privilege, payoff significance and physical water feasibility. Do not convert these diagnostics into weights, quotas, eligibility gates or automatic authored placements yet.

## All-eight summary

| Seed | Selected N/S (frozen) | Complete / Structural / Incomplete | Unselected sample | Set N / S | Scale diagnostic |
|---|---|---|---|---|---|
| [930005557](../default_local_continuation_2026-09-11/930005557/greybox.svg) | 3/3 | 2/1/3 | 24 | unresolved / complementary | unresolved |
| [930006815](../default_local_continuation_2026-09-11/930006815/greybox.svg) | 3/3 | 0/1/5 | 24 | unresolved / unresolved | unresolved |
| [930007222](../default_local_continuation_2026-09-11/930007222/greybox.svg) | 3/3 | 0/2/4 | 24 | unresolved / partially overlapping | unresolved |
| [930010639](../default_local_continuation_2026-09-11/930010639/greybox.svg) | 3/3 | 2/1/3 | 24 | partially overlapping / unresolved | unresolved |
| [930012642](../default_local_continuation_2026-09-11/930012642/greybox.svg) | 3/3 | 1/1/4 | 24 | unresolved / unresolved | unresolved |
| [930015734](../default_local_continuation_2026-09-11/930015734/greybox.svg) | 3/2 | 2/2/1 | 24 | partially overlapping / unresolved | unresolved |
| [930016664](../default_local_continuation_2026-09-11/930016664/greybox.svg) | 2/3 | 0/1/4 | 24 | unresolved / unresolved | unresolved |
| [930019528](../default_local_continuation_2026-09-11/930019528/greybox.svg) | 3/3 | 0/1/5 | 24 | unresolved / unresolved | unresolved |

## Every selected handoff

G/J/D/T = Gateway/Junction/Destination/Transition evidence states, not quality grades. Commitment = access/exploitation/return/infrastructure/exposure. Full IDs, envelopes, payoff evidence, all shore accesses and inherited reach are in each `fit.json`; the new object is `selected_handoffs[team][i].opportunity_relationship`.

| Seed/slot | Current ID / kind | HD / Travel | Local | Supported affordances | G/J/D/T | Completion | Observed payoff | Potential payoff | Commitment | Relationship |
|---|---|---|---|---|---|---|---|---|---|---|
| 930005557/N1 | `highland approach:1381` / highland approach | 36.0/64.4 | directed | passage, threshold | weak/unsupported/unsupported/unsupported | incomplete | none established | none established | low/unresolved/low/low/unresolved | highland approach → function not established → incomplete |
| 930005557/N2 | `village:[-152, 15]` / settlement approach | 55.6/69.7 | branching | passage, network_access, direct_interaction | unsupported/supported/strong/unsupported | complete | village (1) | none established | low/low/low/low/unresolved | settlement approach → Junction/Destination → complete |
| 930005557/N3 | `water-3868` / inland-water relationship | 36.0/98.8 | directed | passage, threshold, barrier, crossing, corridor_access | unsupported/unsupported/unsupported/unsupported | incomplete | none established | none established | low/unresolved/unresolved/unresolved/unresolved | inland-water relationship → function not established → incomplete; water mobility has no demonstrated valuable payoff |
| 930005557/S1 | `forest entrance:6336` / forest entrance | 39.6/64.6 | directed | passage, threshold | supported/unsupported/unsupported/unsupported | structurally_supported | none established | persistent forest interior (1) | medium/unresolved/low/medium/unresolved | forest entrance → Gateway → structurally_supported |
| 930005557/S2 | `highland approach:6909` / highland approach | 36.0/63.9 | directed | passage, threshold | supported/unsupported/unsupported/unsupported | complete | differentiated highland system (1) | none established | low/medium/low/low/unresolved | highland approach → Gateway → complete |
| 930005557/S3 | `highland approach:8898` / highland approach | 49.0/64.8 | branching | passage, threshold | weak/weak/unsupported/unsupported | incomplete | none established | none established | low/unresolved/low/low/unresolved | highland approach → function not established → incomplete |
| 930006815/N1 | `forest entrance:192` / forest entrance | 33.0/89.9 | directed | passage, threshold | weak/unsupported/unsupported/unsupported | incomplete | none established | none established | medium/unresolved/low/low/unresolved | forest entrance → function not established → incomplete |
| 930006815/N2 | `highland approach:559` / highland approach | 44.0/66.1 | directed | passage, threshold | weak/unsupported/unsupported/unsupported | incomplete | none established | none established | low/unresolved/low/low/unresolved | highland approach → function not established → incomplete |
| 930006815/N3 | `transition:region-32:region-43` / persistent regional threshold | 28.0/65.3 | directed | passage | unsupported/unsupported/unsupported/unsupported | incomplete | none established | none established | medium/unresolved/low/medium/unresolved | persistent regional threshold → function not established → incomplete |
| 930006815/S1 | `forest entrance:10855` / forest entrance | 36.0/64.9 | directed | passage, threshold | weak/unsupported/unsupported/unsupported | incomplete | none established | none established | medium/unresolved/low/medium/unresolved | forest entrance → function not established → incomplete |
| 930006815/S2 | `transition:region-10596:region-10600` / persistent regional threshold | 41.0/70.3 | directed | passage | unsupported/unsupported/unsupported/unsupported | incomplete | none established | none established | medium/unresolved/low/medium/unresolved | persistent regional threshold → function not established → incomplete |
| 930006815/S3 | `water-5880` / inland-water relationship | 63.6/69.8 | directed | passage, threshold, barrier, crossing, corridor_access, network_access | unsupported/unsupported/unsupported/weak | structurally_supported | none established | remote shore projection (13) | low/unresolved/unresolved/unresolved/unresolved | inland-water relationship → function not established → structurally_supported; water mobility has no demonstrated valuable payoff |
| 930007222/N1 | `highland approach:27` / highland approach | 52.0/91.3 | directed | passage, threshold | weak/unsupported/unsupported/unsupported | incomplete | none established | none established | low/unresolved/low/low/unresolved | highland approach → function not established → incomplete |
| 930007222/N2 | `slope-foot approach:1235` / slope-foot approach | 36.0/56.8 | directed | passage | unsupported/unsupported/unsupported/unsupported | incomplete | none established | none established | low/unresolved/low/low/unresolved | slope-foot approach → function not established → incomplete |
| 930007222/N3 | `transition:region-49:region-56` / persistent regional threshold | 57.0/64.0 | directed | passage | unsupported/unsupported/unsupported/unsupported | incomplete | none established | none established | low/unresolved/low/low/unresolved | persistent regional threshold → function not established → incomplete |
| 930007222/S1 | `forest entrance:11829` / forest entrance | 28.0/64.7 | directed | passage, threshold, barrier, corridor_access, network_access | weak/unsupported/unsupported/weak | structurally_supported | none established | remote shore projection (23) | medium/unresolved/unresolved/unresolved/unresolved | forest entrance → function not established → structurally_supported; water mobility has no demonstrated valuable payoff |
| 930007222/S2 | `transition:region-10644:region-11964` / persistent regional threshold | 52.0/73.5 | branching | passage, threshold, barrier, corridor_access, network_access | unsupported/weak/unsupported/weak | structurally_supported | none established | remote shore projection (21) | low/unresolved/unresolved/unresolved/unresolved | persistent regional threshold → function not established → structurally_supported; water mobility has no demonstrated valuable payoff |
| 930007222/S3 | `water-12110` / inland-water relationship | 36.0/69.4 | directed | passage, threshold | unsupported/unsupported/unsupported/unsupported | incomplete | none established | none established | low/unresolved/unresolved/unresolved/unresolved | inland-water relationship → function not established → incomplete; water mobility has no demonstrated valuable payoff |
| 930010639/N1 | `highland approach:1662` / highland approach | 12.0/21.4 | branching | passage, threshold, network_access | weak/supported/unsupported/unsupported | structurally_supported | none established | none established | low/unresolved/low/low/unresolved | highland approach → Junction → structurally_supported |
| 930010639/N2 | `water-123` / coastal access | 37.7/45.4 | directed | passage, threshold, barrier, corridor_access, network_access | unsupported/unsupported/unsupported/supported | complete | village (1) | none established | low/unresolved/unresolved/unresolved/unresolved | coastal access → Transition → complete |
| 930010639/N3 | `water-745` / inland-water relationship | 44.0/64.7 | directed | passage, threshold, crossing, corridor_access, network_access | unsupported/unsupported/unsupported/supported | complete | village (1) | none established | low/unresolved/unresolved/unresolved/unresolved | inland-water relationship → Transition → complete |
| 930010639/S1 | `slope-foot approach:11409` / slope-foot approach | 44.0/63.1 | directed | passage | unsupported/unsupported/unsupported/unsupported | incomplete | none established | none established | low/unresolved/low/low/unresolved | slope-foot approach → function not established → incomplete |
| 930010639/S2 | `transition:region-5500:region-7712` / persistent regional threshold | 13.7/64.5 | directed | passage | unsupported/unsupported/unsupported/unsupported | incomplete | none established | none established | low/unresolved/low/low/unresolved | persistent regional threshold → function not established → incomplete |
| 930010639/S3 | `water-10829` / inland-water relationship | 96.2/127.2 | directed | passage, threshold, barrier, crossing, corridor_access | unsupported/unsupported/unsupported/unsupported | incomplete | none established | none established | low/unresolved/unresolved/unresolved/unresolved | inland-water relationship → function not established → incomplete; water mobility has no demonstrated valuable payoff |
| 930012642/N1 | `slope-foot approach:1559` / slope-foot approach | 49.0/67.7 | directed | passage | unsupported/unsupported/unsupported/unsupported | incomplete | none established | none established | low/unresolved/low/low/unresolved | slope-foot approach → function not established → incomplete |
| 930012642/N2 | `village:[-133, -24]` / settlement approach | 36.0/64.4 | directed | passage, direct_interaction | unsupported/unsupported/strong/unsupported | complete | village (1) | none established | low/low/low/low/unresolved | settlement approach → Destination → complete |
| 930012642/N3 | `water-891` / inland-water relationship | 44.0/94.6 | directed | passage, threshold, crossing | unsupported/unsupported/unsupported/unsupported | incomplete | none established | none established | low/unresolved/unresolved/unresolved/unresolved | inland-water relationship → function not established → incomplete; water mobility has no demonstrated valuable payoff |
| 930012642/S1 | `forest entrance:7412` / forest entrance | 60.3/105.7 | directed | passage, threshold | supported/unsupported/unsupported/unsupported | structurally_supported | none established | persistent forest interior (1) | medium/unresolved/low/medium/unresolved | forest entrance → Gateway → structurally_supported |
| 930012642/S2 | `transition:region-12466:region-9889` / persistent regional threshold | 36.3/67.5 | directed | passage | unsupported/unsupported/unsupported/unsupported | incomplete | none established | none established | medium/unresolved/low/medium/unresolved | persistent regional threshold → function not established → incomplete |
| 930012642/S3 | `water-11721` / inland-water relationship | 28.0/65.2 | directed | passage, threshold, barrier, corridor_access | unsupported/unsupported/unsupported/unsupported | incomplete | none established | none established | low/unresolved/unresolved/unresolved/unresolved | inland-water relationship → function not established → incomplete; water mobility has no demonstrated valuable payoff |
| 930015734/N1 | `forest entrance:51` / forest entrance | 49.0/64.2 | directed | passage, threshold | supported/unsupported/unsupported/unsupported | complete | village (1) | none established | low/medium/low/low/unresolved | forest entrance → Gateway → complete |
| 930015734/N2 | `highland approach:53` / highland approach | 41.0/63.7 | directed | passage, threshold | supported/unsupported/unsupported/unsupported | structurally_supported | none established | persistent highland interior (1) | low/unresolved/low/low/unresolved | highland approach → Gateway → structurally_supported |
| 930015734/N3 | `slope-foot approach:3619` / slope-foot approach | 52.0/66.7 | directed | passage, threshold | supported/unsupported/unsupported/unsupported | complete | village (1); differentiated highland system (1) | none established | low/medium/low/low/unresolved | slope-foot approach → Gateway → complete |
| 930015734/S1 | `transition:region-13529:region-6399` / persistent regional threshold | 17.0/60.5 | branching | passage | unsupported/weak/unsupported/unsupported | incomplete | none established | none established | medium/unresolved/low/low/unresolved | persistent regional threshold → function not established → incomplete |
| 930015734/S2 | `water-1128` / inland-water relationship | 33.0/65.3 | directed | passage, threshold, barrier, crossing, corridor_access, network_access | unsupported/unsupported/unsupported/weak | structurally_supported | none established | remote shore projection (26) | low/unresolved/unresolved/unresolved/unresolved | inland-water relationship → function not established → structurally_supported; water mobility has no demonstrated valuable payoff |
| 930016664/N1 | `slope-foot approach:1802` / slope-foot approach | 28.0/91.9 | directed | passage | unsupported/unsupported/unsupported/unsupported | incomplete | none established | none established | low/unresolved/low/low/unresolved | slope-foot approach → function not established → incomplete |
| 930016664/N2 | `water-1605` / inland-water relationship | 29.7/74.5 | branching | passage, threshold, barrier, crossing, corridor_access, network_access | unsupported/weak/unsupported/weak | structurally_supported | none established | remote shore projection (5) | low/unresolved/unresolved/unresolved/unresolved | inland-water relationship → function not established → structurally_supported; water mobility has no demonstrated valuable payoff |
| 930016664/S1 | `forest entrance:10324` / forest entrance | 36.3/65.2 | directed | passage, threshold | weak/unsupported/unsupported/unsupported | incomplete | none established | none established | low/unresolved/low/low/unresolved | forest entrance → function not established → incomplete |
| 930016664/S2 | `highland approach:901` / highland approach | 20.0/65.8 | directed | passage, threshold | weak/unsupported/unsupported/unsupported | incomplete | none established | none established | low/unresolved/low/low/unresolved | highland approach → function not established → incomplete |
| 930016664/S3 | `water-13872` / inland-water relationship | 37.7/58.5 | directed | passage | unsupported/unsupported/unsupported/unsupported | incomplete | none established | none established | low/unresolved/medium/low/unresolved | inland-water relationship → function not established → incomplete |
| 930019528/N1 | `forest entrance:1464` / forest entrance | 36.0/59.3 | directed | passage, threshold | supported/unsupported/unsupported/unsupported | structurally_supported | none established | persistent forest interior (1) | low/unresolved/low/low/unresolved | forest entrance → Gateway → structurally_supported |
| 930019528/N2 | `water-1465` / inland-water relationship | 36.0/64.0 | directed | passage | unsupported/unsupported/unsupported/unsupported | incomplete | none established | none established | low/unresolved/low/low/unresolved | inland-water relationship → function not established → incomplete |
| 930019528/N3 | `water-475` / inland-water relationship | 33.0/60.7 | branching | passage | unsupported/weak/unsupported/unsupported | incomplete | none established | none established | low/unresolved/low/low/unresolved | inland-water relationship → function not established → incomplete |
| 930019528/S1 | `highland approach:12560` / highland approach | 41.0/62.7 | directed | passage, threshold | weak/unsupported/unsupported/unsupported | incomplete | none established | none established | low/unresolved/low/low/unresolved | highland approach → function not established → incomplete |
| 930019528/S2 | `slope-foot approach:9965` / slope-foot approach | 92.0/141.3 | directed | passage | unsupported/unsupported/unsupported/unsupported | incomplete | none established | none established | low/unresolved/low/low/unresolved | slope-foot approach → function not established → incomplete |
| 930019528/S3 | `transition:region-11028:region-11888` / persistent regional threshold | 36.0/68.3 | directed | passage | unsupported/unsupported/unsupported/unsupported | incomplete | none established | none established | low/unresolved/low/low/unresolved | persistent regional threshold → function not established → incomplete |

## Homeland Opportunity Sets

Connectivity uses the frozen conditional-modest spatial matrices. Opportunity convergence uses shared payoff/system identity and can exist without terrestrial overlap. “Contested” means prospective access by opposing teams, not observed combat. “Exclusive” is only within the sampled evidence, not a universal exclusion.

| Seed/team | Diversity | Pair relationships | Projection contexts / payoff IDs | Eventual opportunity / spatial contestation | Payoff unresolved / incomplete |
|---|---|---|---|---|---|
| 930005557/north | unresolved | 1: connected; 2: connected; 3: connected | 2/1 | False/True | 0/2 |
| 930005557/south | complementary | 1: complementary, connected; 2: connected; 3: connected | 2/2 | False/True | 1/1 |
| 930006815/north | unresolved | 1: connected; 2: connected; 3: connected | 0/0 | False/True | 0/3 |
| 930006815/south | unresolved | 1: connected; 2: connected; 3: connected | 1/13 | False/True | 1/2 |
| 930007222/north | unresolved | 1: connected; 2: unresolved; 3: unresolved | 0/0 | False/False | 0/3 |
| 930007222/south | partially overlapping | 1: overlapping, complementary; 2: unresolved; 3: connected | 1/23 | False/False | 2/1 |
| 930010639/north | partially overlapping | 1: connected; 2: connected; 3: overlapping, substitutes | 1/1 | False/True | 1/0 |
| 930010639/south | unresolved | 1: connected; 2: connected; 3: connected | 1/0 | False/True | 0/3 |
| 930012642/north | unresolved | 1: connected; 2: connected; 3: connected | 1/1 | False/True | 0/2 |
| 930012642/south | unresolved | 1: connected; 2: connected; 3: connected | 2/1 | False/True | 1/2 |
| 930015734/north | partially overlapping | 1: complementary, connected; 2: overlapping, complementary, connected; 3: complementary, connected | 3/3 | False/False | 1/0 |
| 930015734/south | unresolved | 1: unresolved | 1/26 | False/False | 1/1 |
| 930016664/north | unresolved | 1: connected | 1/5 | False/False | 1/1 |
| 930016664/south | unresolved | 1: connected; 2: unresolved; 3: unresolved | 0/0 | False/False | 0/3 |
| 930019528/north | unresolved | 1: connected; 2: unresolved; 3: unresolved | 1/1 | False/True | 1/2 |
| 930019528/south | unresolved | 1: connected; 2: connected; 3: connected | 0/0 | False/True | 0/3 |

930005557 topology phases: differentiation {'north': 'unresolved', 'south': 'complementary'}; networking connections {'north': 3, 'south': 3}; contestation {'opportunity_pairs': 0, 'shared_system_only_pairs': 0, 'spatial_pair_coverage': 1.0}. Full same-team pair identities, counterfactual removal tests, spatial meetings and North/South opportunity pairs are in the JSON.


930006815 topology phases: differentiation {'north': 'unresolved', 'south': 'unresolved'}; networking connections {'north': 3, 'south': 3}; contestation {'opportunity_pairs': 0, 'shared_system_only_pairs': 0, 'spatial_pair_coverage': 1.0}. Full same-team pair identities, counterfactual removal tests, spatial meetings and North/South opportunity pairs are in the JSON.


930007222 topology phases: differentiation {'north': 'unresolved', 'south': 'partially overlapping'}; networking connections {'north': 1, 'south': 1}; contestation {'opportunity_pairs': 0, 'shared_system_only_pairs': 0, 'spatial_pair_coverage': 0.0}. Full same-team pair identities, counterfactual removal tests, spatial meetings and North/South opportunity pairs are in the JSON.


930010639 topology phases: differentiation {'north': 'partially overlapping', 'south': 'unresolved'}; networking connections {'north': 2, 'south': 3}; contestation {'opportunity_pairs': 0, 'shared_system_only_pairs': 0, 'spatial_pair_coverage': 1.0}. Full same-team pair identities, counterfactual removal tests, spatial meetings and North/South opportunity pairs are in the JSON.


930012642 topology phases: differentiation {'north': 'unresolved', 'south': 'unresolved'}; networking connections {'north': 3, 'south': 3}; contestation {'opportunity_pairs': 0, 'shared_system_only_pairs': 0, 'spatial_pair_coverage': 1.0}. Full same-team pair identities, counterfactual removal tests, spatial meetings and North/South opportunity pairs are in the JSON.


930015734 topology phases: differentiation {'north': 'partially overlapping', 'south': 'unresolved'}; networking connections {'north': 3, 'south': 0}; contestation {'opportunity_pairs': 0, 'shared_system_only_pairs': 0, 'spatial_pair_coverage': 0.0}. Full same-team pair identities, counterfactual removal tests, spatial meetings and North/South opportunity pairs are in the JSON.


930016664 topology phases: differentiation {'north': 'unresolved', 'south': 'unresolved'}; networking connections {'north': 1, 'south': 1}; contestation {'opportunity_pairs': 0, 'shared_system_only_pairs': 0, 'spatial_pair_coverage': 0.0}. Full same-team pair identities, counterfactual removal tests, spatial meetings and North/South opportunity pairs are in the JSON.


930019528 topology phases: differentiation {'north': 'unresolved', 'south': 'unresolved'}; networking connections {'north': 1, 'south': 3}; contestation {'opportunity_pairs': 0, 'shared_system_only_pairs': 0, 'spatial_pair_coverage': 1.0}. Full same-team pair identities, counterfactual removal tests, spatial meetings and North/South opportunity pairs are in the JSON.

## Focused interpretations

### 930005557

#### N1: highland approach:1381

Anchor [-2316, 94, 84]; arrival envelope 7 same-feature samples. Local directed; incomplete.

Affordances: passage, threshold. Functions: Gateway: weak, Junction: unsupported, Destination: unsupported, Transition: unsupported.

Entered-system evidence: highland-system-1383: sampled threshold=True, immediate modest entry=False, whole-system samples/core=281/82, whole-system inward layers=6, reached persistence=False, local samples=0, commitment=False, already accessible at homeland=False; entry limitation=mask present next to anchor, but no immediate modest dry connection within the existing local reach; steep/diagonal/sampling dependency unresolved.

Observed payoff: none established. Potential payoff: none established. Commitment (access/exploitation/return/infrastructure/exposure): low/unresolved/low/low/unresolved.

Weak evidence: sampled access edge; physical entrance and visual legibility unverified.
#### N2: village:[-152, 15]

Anchor [-2380, 97, 236]; arrival envelope 11 same-feature samples. Local branching; complete.

Affordances: passage, network_access, direct_interaction. Functions: Gateway: unsupported, Junction: supported, Destination: strong, Transition: unsupported.

Entered-system evidence: highland-system-40: sampled threshold=False, immediate modest entry=True, whole-system samples/core=274/22, whole-system inward layers=5, reached persistence=True, local samples=98, commitment=True, already accessible at homeland=True; entry limitation=None.

Observed payoff: village (1). Potential payoff: none established. Commitment (access/exploitation/return/infrastructure/exposure): low/low/low/low/unresolved.

Weak evidence: village entrance, occupancy and unobstructed sightline unverified.
#### N3: water-3868

Anchor [-2244, 78, 180]; arrival envelope 2 same-feature samples. Local directed; incomplete.

Affordances: passage, threshold, barrier, crossing, corridor_access. Functions: Gateway: unsupported, Junction: unsupported, Destination: unsupported, Transition: unsupported.

Entered-system evidence: no sampled forest/highland entrance established; non-cover formations remain an analyzer limitation.

Observed payoff: none established. Potential payoff: none established. Commitment (access/exploitation/return/infrastructure/exposure): low/unresolved/unresolved/unresolved/unresolved.

Weak evidence: water depth, ford/bridge viability and navigational significance unavailable.

Water interpretation: water-network-3868: interface=True, corridor=True, network=False, crossing=True, barrier=True, local feature=False; 3 shore contacts, 0 distinct remote contacts, path extent 264.0; conditional sampled open-water continuity; depth, channel width, embarkation and obstacles unverified.
#### S1: forest entrance:6336

Anchor [-1804, 104, 276]; arrival envelope 8 same-feature samples. Local directed; structurally_supported.

Affordances: passage, threshold. Functions: Gateway: supported, Junction: unsupported, Destination: unsupported, Transition: unsupported.

Entered-system evidence: forest-system-10587: sampled threshold=True, immediate modest entry=True, whole-system samples/core=122/29, whole-system inward layers=6, reached persistence=True, local samples=9, commitment=True, already accessible at homeland=False; entry limitation=None; highland-system-8856: sampled threshold=False, immediate modest entry=True, whole-system samples/core=1004/312, whole-system inward layers=11, reached persistence=True, local samples=44, commitment=True, already accessible at homeland=True; entry limitation=None.

Observed payoff: none established. Potential payoff: persistent forest interior (1). Commitment (access/exploitation/return/infrastructure/exposure): medium/unresolved/low/medium/unresolved.

Weak evidence: sampled access edge; physical entrance and visual legibility unverified.
#### S2: highland approach:6909

Anchor [-1940, 95, 188]; arrival envelope 9 same-feature samples. Local directed; complete.

Affordances: passage, threshold. Functions: Gateway: supported, Junction: unsupported, Destination: unsupported, Transition: unsupported.

Entered-system evidence: highland-system-7174: sampled threshold=True, immediate modest entry=True, whole-system samples/core=151/27, whole-system inward layers=5, reached persistence=True, local samples=53, commitment=True, already accessible at homeland=False; entry limitation=None.

Observed payoff: differentiated highland system (1). Potential payoff: none established. Commitment (access/exploitation/return/infrastructure/exposure): low/medium/low/low/unresolved.

Weak evidence: sampled access edge; physical entrance and visual legibility unverified.
#### S3: highland approach:8898

Anchor [-1916, 93, 132]; arrival envelope 8 same-feature samples. Local branching; incomplete.

Affordances: passage, threshold. Functions: Gateway: weak, Junction: weak, Destination: unsupported, Transition: unsupported.

Entered-system evidence: highland-system-9420: sampled threshold=True, immediate modest entry=True, whole-system samples/core=7/0, whole-system inward layers=1, reached persistence=False, local samples=7, commitment=False, already accessible at homeland=False; entry limitation=None; highland-system-9422: sampled threshold=True, immediate modest entry=False, whole-system samples/core=2/0, whole-system inward layers=1, reached persistence=False, local samples=0, commitment=False, already accessible at homeland=False; entry limitation=mask present next to anchor, but no immediate modest dry connection within the existing local reach; steep/diagonal/sampling dependency unresolved.

Observed payoff: none established. Potential payoff: none established. Commitment (access/exploitation/return/infrastructure/exposure): low/unresolved/low/low/unresolved.

Weak evidence: sampled access edge; physical entrance and visual legibility unverified.

North set: unresolved; geographic contexts ['village:[-152, 15]', 'water-network-3868']; payoff kinds ['village']. Removal test: north:highland approach:1381 → not established; others overlap or payoff unresolved; north:village:[-152, 15] → supported/potential; north:water-3868 → not established; others overlap or payoff unresolved.

South set: complementary; geographic contexts ['forest-system-10587', 'highland-system-7174']; payoff kinds ['differentiated highland system', 'persistent forest interior']. Removal test: south:forest entrance:6336 → supported/potential; south:highland approach:6909 → supported/potential; south:highland approach:8898 → not established; others overlap or payoff unresolved.

The settlement is tested as a direct gameplay-bearing arrival, not forced to be a Gateway. South highland-like candidates are compared by actual entered-system and payoff identities; different feature labels alone do not establish complementarity.

### 930007222

#### S1: forest entrance:11829

Anchor [-1764, 63, -124]; arrival envelope 1 same-feature samples. Local directed; structurally_supported.

Affordances: passage, threshold, barrier, corridor_access, network_access. Functions: Gateway: weak, Junction: unsupported, Destination: unsupported, Transition: weak.

Entered-system evidence: forest-system-11319: sampled threshold=True, immediate modest entry=False, whole-system samples/core=554/175, whole-system inward layers=9, reached persistence=False, local samples=0, commitment=False, already accessible at homeland=True; entry limitation=mask present next to anchor, but no immediate modest dry connection within the existing local reach; steep/diagonal/sampling dependency unresolved.

Observed payoff: none established. Potential payoff: remote shore projection (23). Commitment (access/exploitation/return/infrastructure/exposure): medium/unresolved/unresolved/unresolved/unresolved.

Weak evidence: sampled access edge; physical entrance and visual legibility unverified.

Water interpretation: water-network-99: interface=True, corridor=True, network=True, crossing=False, barrier=True, local feature=False; 37 shore contacts, 23 distinct remote contacts, path extent 1072.0; conditional sampled open-water continuity; depth, channel width, embarkation and obstacles unverified.

The sampled forest has a substantial core; this is **not** evidence of a tiny forest strip. The current anchor lacks an immediate modest entry into that core, and the same system already contacts the homeland. Gateway evidence is therefore weak for this handoff. Its Structurally Supported interpretation comes from independently detected water-network access and remote shore potential, not a fabricated forest payoff. No observed payoff or authored location is assigned. This is why feature kind and actual Opportunity Relationship must remain separate.

### 930010639

#### N1: highland approach:1662

Anchor [1796, 72, -196]; arrival envelope 1 same-feature samples. Local branching; structurally_supported.

Affordances: passage, threshold, network_access. Functions: Gateway: weak, Junction: supported, Destination: unsupported, Transition: unsupported.

Entered-system evidence: highland-system-2861: sampled threshold=True, immediate modest entry=False, whole-system samples/core=1/0, whole-system inward layers=1, reached persistence=False, local samples=0, commitment=False, already accessible at homeland=False; entry limitation=mask present next to anchor, but no immediate modest dry connection within the existing local reach; steep/diagonal/sampling dependency unresolved.

Observed payoff: none established. Potential payoff: none established. Commitment (access/exploitation/return/infrastructure/exposure): low/unresolved/low/low/unresolved.

Weak evidence: sampled access edge; physical entrance and visual legibility unverified.
#### N2: water-123

Anchor [1884, 67, -284]; arrival envelope 1 same-feature samples. Local directed; complete.

Affordances: passage, threshold, barrier, corridor_access, network_access. Functions: Gateway: unsupported, Junction: unsupported, Destination: unsupported, Transition: supported.

Entered-system evidence: no sampled forest/highland entrance established; non-cover formations remain an analyzer limitation.

Observed payoff: village (1). Potential payoff: none established. Commitment (access/exploitation/return/infrastructure/exposure): low/unresolved/unresolved/unresolved/unresolved.

Weak evidence: water depth, ford/bridge viability and navigational significance unavailable.

Water interpretation: water-network-122: interface=True, corridor=True, network=True, crossing=False, barrier=True, local feature=False; 18 shore contacts, 0 distinct remote contacts, path extent 824.0; conditional sampled open-water continuity; depth, channel width, embarkation and obstacles unverified.

**North coastal questions**

1. Usable interface: True. This means modest dry-bank contact with sampled open water; exact embarkation, water surface height and landing clearance remain unverified.
2. Corridor / maritime access: True / True. These are water-geometry diagnostics, not evidence that a coastline label is inherently valuable.
3. Distinct access: 3 shore contacts with differentiated regional context or a downstream village payoff; region types: open/lowland/open/gentle/inland, open/lowland/open/rough/inland. Full region IDs, land components, cover systems, water path lengths and nearby villages are emitted in water_reach.
4. Observed markedly differentiated payoff: village (1). Structure value/projection is distinguished from unmeasured resource concentration/scarcity.
5. Grounded placement potential: none established. No authored location is placed; potential is not guaranteed gameplay value.
6. Completion: complete (sampled/conditional).
7. Low terrestrial deep reach is still accurate for that terrestrial graph, but is not a universal Reach failure. Water Reach is a separate modality; its value depends on usable shores and payoff, not its distance.
8. Transition state: supported. The previous selector evaluated local viability and feature strength, not a complete water-to-payoff chain. This shadow explanation cannot retroactively claim that the old selector made a strategic-water decision.

Observed payoff identities and alternative access: [{'id': 'village:[103, -9]', 'via': 'water-network-122', 'land_access': {'homeland_depth': 76.284, 'homeland_modest_travel_cost': 124.069, 'water_path_to_village_shore_blocks': 480}, 'water_advantage': 'unresolved: sampled water-path length is not calibrated against land Travel Cost; coherence does not establish dominance'}]. Same water-network contacts already at homeland perimeter: 1. A coherent water-to-village chain does not prove that this road offers superior or exclusive access compared with direct land travel.
#### N3: water-745

Anchor [1732, 64, -212]; arrival envelope 10 same-feature samples. Local directed; complete.

Affordances: passage, threshold, crossing, corridor_access, network_access. Functions: Gateway: unsupported, Junction: unsupported, Destination: unsupported, Transition: supported.

Entered-system evidence: no sampled forest/highland entrance established; non-cover formations remain an analyzer limitation.

Observed payoff: village (1). Potential payoff: none established. Commitment (access/exploitation/return/infrastructure/exposure): low/unresolved/unresolved/unresolved/unresolved.

Weak evidence: water depth, ford/bridge viability and navigational significance unavailable.

Water interpretation: water-network-122: interface=True, corridor=True, network=True, crossing=True, barrier=False, local feature=False; 18 shore contacts, 0 distinct remote contacts, path extent 1216.0; conditional sampled open-water continuity; depth, channel width, embarkation and obstacles unverified.
#### S1: slope-foot approach:11409

Anchor [2316, 79, 60]; arrival envelope 4 same-feature samples. Local directed; incomplete.

Affordances: passage. Functions: Gateway: unsupported, Junction: unsupported, Destination: unsupported, Transition: unsupported.

Entered-system evidence: no sampled forest/highland entrance established; non-cover formations remain an analyzer limitation.

Observed payoff: none established. Potential payoff: none established. Commitment (access/exploitation/return/infrastructure/exposure): low/unresolved/low/low/unresolved.

Weak evidence: 32-block flat-to-rising profile; hill scale and human-perceived significance unverified.
#### S2: transition:region-5500:region-7712

Anchor [2308, 78, -68]; arrival envelope 36 same-feature samples. Local directed; incomplete.

Affordances: passage. Functions: Gateway: unsupported, Junction: unsupported, Destination: unsupported, Transition: unsupported.

Entered-system evidence: no sampled forest/highland entrance established; non-cover formations remain an analyzer limitation.

Observed payoff: none established. Potential payoff: none established. Commitment (access/exploitation/return/infrastructure/exposure): low/unresolved/low/low/unresolved.

Weak evidence: component boundary is not a verified landmark or natural pass.
#### S3: water-10829

Anchor [2452, 78, 84]; arrival envelope 3 same-feature samples. Local directed; incomplete.

Affordances: passage, threshold, barrier, crossing, corridor_access. Functions: Gateway: unsupported, Junction: unsupported, Destination: unsupported, Transition: unsupported.

Entered-system evidence: no sampled forest/highland entrance established; non-cover formations remain an analyzer limitation.

Observed payoff: none established. Potential payoff: none established. Commitment (access/exploitation/return/infrastructure/exposure): low/unresolved/unresolved/unresolved/unresolved.

Weak evidence: water depth, ford/bridge viability and navigational significance unavailable.

Water interpretation: water-network-13110: interface=True, corridor=True, network=False, crossing=True, barrier=True, local feature=False; 1 shore contacts, 0 distinct remote contacts, path extent 160.0; conditional sampled open-water continuity; depth, channel width, embarkation and obstacles unverified.

North set: partially overlapping; geographic contexts ['water-network-122']; payoff kinds ['village']. Removal test: north:highland approach:1662 → not established; others overlap or payoff unresolved; north:water-123 → not established; others overlap or payoff unresolved; north:water-745 → not established; others overlap or payoff unresolved.

South set: unresolved; geographic contexts ['water-network-13110']; payoff kinds []. Removal test: south:slope-foot approach:11409 → not established; others overlap or payoff unresolved; south:transition:region-5500:region-7712 → not established; others overlap or payoff unresolved; south:water-10829 → not established; others overlap or payoff unresolved.

### 930012642

#### N1: slope-foot approach:1559

Anchor [-2092, 68, -412]; arrival envelope 4 same-feature samples. Local directed; incomplete.

Affordances: passage. Functions: Gateway: unsupported, Junction: unsupported, Destination: unsupported, Transition: unsupported.

Entered-system evidence: no sampled forest/highland entrance established; non-cover formations remain an analyzer limitation.

Observed payoff: none established. Potential payoff: none established. Commitment (access/exploitation/return/infrastructure/exposure): low/unresolved/low/low/unresolved.

Weak evidence: 32-block flat-to-rising profile; hill scale and human-perceived significance unverified.
#### N2: village:[-133, -24]

Anchor [-2180, 78, -388]; arrival envelope 9 same-feature samples. Local directed; complete.

Affordances: passage, direct_interaction. Functions: Gateway: unsupported, Junction: unsupported, Destination: strong, Transition: unsupported.

Entered-system evidence: no sampled forest/highland entrance established; non-cover formations remain an analyzer limitation.

Observed payoff: village (1). Potential payoff: none established. Commitment (access/exploitation/return/infrastructure/exposure): low/low/low/low/unresolved.

Weak evidence: village entrance, occupancy and unobstructed sightline unverified.
#### N3: water-891

Anchor [-2252, 63, -452]; arrival envelope 8 same-feature samples. Local directed; incomplete.

Affordances: passage, threshold, crossing. Functions: Gateway: unsupported, Junction: unsupported, Destination: unsupported, Transition: unsupported.

Entered-system evidence: no sampled forest/highland entrance established; non-cover formations remain an analyzer limitation.

Observed payoff: none established. Potential payoff: none established. Commitment (access/exploitation/return/infrastructure/exposure): low/unresolved/unresolved/unresolved/unresolved.

Weak evidence: water depth, ford/bridge viability and navigational significance unavailable.

Water interpretation: water-network-891: interface=True, corridor=False, network=False, crossing=True, barrier=False, local feature=True; 0 shore contacts, 0 distinct remote contacts, path extent 0.0; conditional sampled open-water continuity; depth, channel width, embarkation and obstacles unverified.
#### S1: forest entrance:7412

Anchor [-1932, 72, 372]; arrival envelope 9 same-feature samples. Local directed; structurally_supported.

Affordances: passage, threshold. Functions: Gateway: supported, Junction: unsupported, Destination: unsupported, Transition: unsupported.

Entered-system evidence: forest-system-7202: sampled threshold=True, immediate modest entry=True, whole-system samples/core=1247/370, whole-system inward layers=8, reached persistence=True, local samples=61, commitment=True, already accessible at homeland=False; entry limitation=None.

Observed payoff: none established. Potential payoff: persistent forest interior (1). Commitment (access/exploitation/return/infrastructure/exposure): medium/unresolved/low/medium/unresolved.

Weak evidence: sampled access edge; physical entrance and visual legibility unverified.
#### S2: transition:region-12466:region-9889

Anchor [-2084, 68, 484]; arrival envelope 24 same-feature samples. Local directed; incomplete.

Affordances: passage. Functions: Gateway: unsupported, Junction: unsupported, Destination: unsupported, Transition: unsupported.

Entered-system evidence: no sampled forest/highland entrance established; non-cover formations remain an analyzer limitation.

Observed payoff: none established. Potential payoff: none established. Commitment (access/exploitation/return/infrastructure/exposure): medium/unresolved/low/medium/unresolved.

Weak evidence: component boundary is not a verified landmark or natural pass.
#### S3: water-11721

Anchor [-2004, 68, 364]; arrival envelope 7 same-feature samples. Local directed; incomplete.

Affordances: passage, threshold, barrier, corridor_access. Functions: Gateway: unsupported, Junction: unsupported, Destination: unsupported, Transition: unsupported.

Entered-system evidence: no sampled forest/highland entrance established; non-cover formations remain an analyzer limitation.

Observed payoff: none established. Potential payoff: none established. Commitment (access/exploitation/return/infrastructure/exposure): low/unresolved/unresolved/unresolved/unresolved.

Weak evidence: water depth, ford/bridge viability and navigational significance unavailable.

Water interpretation: water-network-11721: interface=True, corridor=True, network=False, crossing=False, barrier=True, local feature=False; 0 shore contacts, 0 distinct remote contacts, path extent 8.0; conditional sampled open-water continuity; depth, channel width, embarkation and obstacles unverified.

North set: unresolved; geographic contexts ['village:[-133, -24]']; payoff kinds ['village']. Removal test: north:slope-foot approach:1559 → not established; others overlap or payoff unresolved; north:village:[-133, -24] → supported/potential; north:water-891 → not established; others overlap or payoff unresolved.

South set: unresolved; geographic contexts ['forest-system-7202', 'water-network-11721']; payoff kinds ['persistent forest interior']. Removal test: south:forest entrance:7412 → supported/potential; south:transition:region-12466:region-9889 → not established; others overlap or payoff unresolved; south:water-11721 → not established; others overlap or payoff unresolved.

All current continuations remain directed. Directed has no penalty in these rules: persistence, appropriate Reach, payoff and access context—not branch maximization—determine interpretation.

## Sampled unselected candidates and disagreements

Eligible-but-unselected and ineligible candidates are both included. “Promising” is a shadow interpretation, not a selection recommendation. All sampled records (including Incomplete) and strata are in each fit.json.

| Seed/team | Candidate | Active eligibility / local | Shadow | Observed / potential payoff | Why inspect |
|---|---|---|---|---|---|
| 930005557/north | `water-80` | False / directed | structurally_supported | none established / remote shore projection (1) | coastal access |
| 930005557/north | `village:[-129, 5]` | False / directed | complete | village (1) / none established | settlement approach |
| 930005557/north | `slope-foot approach:1220` | True / branching | structurally_supported | none established / none established | slope-foot approach |
| 930005557/north | `highland approach:62` | True / branching | structurally_supported | none established / none established | highest remaining active utility |
| 930005557/south | `water-80` | False / branching | structurally_supported | none established / remote shore projection (3) | coastal access |
| 930005557/south | `transition:region-10460:region-10587` | True / directed | structurally_supported | none established / persistent forest interior (1) | persistent regional threshold |
| 930005557/south | `village:[-129, 5]` | True / directed | complete | village (1) / none established | settlement approach |
| 930005557/south | `transition:region-10460:region-9654` | True / directed | structurally_supported | none established / persistent forest interior (1) | highest remaining active utility |
| 930006815/north | `water-67` | False / dead_end | structurally_supported | none established / remote shore projection (12) | coastal access, local_dead_end |
| 930006815/north | `village:[4, -24]` | False / directed | complete | village (1) / none established | settlement approach |
| 930006815/south | `water-67` | False / directed | structurally_supported | none established / remote shore projection (18) | coastal access |
| 930006815/south | `village:[4, -24]` | False / directed | complete | village (1) / none established | settlement approach |
| 930006815/south | `water-11922` | True / directed | structurally_supported | none established / remote shore projection (1) | highest remaining active utility |
| 930006815/south | `transition:region-8621:region-9017` | True / directed | structurally_supported | none established / remote shore projection (13) | highest remaining active utility |
| 930007222/north | `water-99` | False / directed | structurally_supported | none established / remote shore projection (11) | coastal access |
| 930007222/north | `village:[-133, 9]` | False / directed | complete | village (1) / none established | settlement approach |
| 930007222/north | `water-1393` | True / directed | structurally_supported | none established / remote shore projection (11) | highest remaining active utility |
| 930007222/south | `water-9038` | False / dead_end | structurally_supported | none established / remote shore projection (23) | coastal access, local_dead_end |
| 930007222/south | `forest entrance:13928` | True / directed | structurally_supported | none established / remote shore projection (22) | forest entrance, same_opportunity_group |
| 930007222/south | `water-11037` | True / directed | structurally_supported | none established / remote shore projection (23) | inland-water relationship, locally_valid |
| 930007222/south | `water-11826` | True / directed | structurally_supported | none established / remote shore projection (21) | highest remaining active utility |
| 930007222/south | `water-12220` | True / directed | structurally_supported | none established / remote shore projection (21) | highest remaining active utility |
| 930007222/south | `water-11828` | True / directed | structurally_supported | none established / remote shore projection (23) | highest remaining active utility |
| 930010639/north | `transition:region-1675:region-735` | True / directed | complete | village (1) / none established | persistent regional threshold |
| 930010639/north | `village:[103, -9]` | True / branching | complete | village (1) / none established | settlement approach, locally_valid |
| 930010639/north | `transition:region-3127:region-735` | True / directed | complete | village (1) / none established | highest remaining active utility |
| 930010639/north | `transition:region-1821:region-2224` | True / directed | complete | village (1) / none established | highest remaining active utility |
| 930010639/south | `water-123` | False / dead_end | complete | village (1) / none established | coastal access |
| 930010639/south | `village:[136, 17]` | False / directed | complete | village (1) / none established | settlement approach |
| 930010639/south | `water-11700` | False / dead_end | complete | village (1) / none established | local_dead_end |
| 930010639/south | `water-11698` | True / directed | complete | village (1) / none established | highest remaining active utility |
| 930012642/north | `highland approach:5086` | False / directed | structurally_supported | none established / persistent highland interior (1) | highland approach |
| 930012642/north | `water-638` | False / dead_end | structurally_supported | none established / remote shore projection (1) | local_dead_end |
| 930012642/north | `water-15` | True / directed | structurally_supported | none established / remote shore projection (1) | highest remaining active utility |
| 930012642/south | `village:[-133, -24]` | False / directed | complete | village (1) / none established | settlement approach |
| 930012642/south | `forest entrance:10681` | False / directed | structurally_supported | none established / persistent forest interior (1) | same_opportunity_group |
| 930015734/north | `water-7858` | False / directed | structurally_supported | none established / remote shore projection (12) | coastal access |
| 930015734/north | `forest entrance:4390` | False / directed | complete | village (1) / none established | forest entrance, same_opportunity_group |
| 930015734/north | `highland approach:22` | True / branching | complete | village (1); differentiated highland system (1) / none established | highland approach, locally_valid |
| 930015734/north | `village:[137, 8]` | False / directed | complete | village (1) / none established | settlement approach |
| 930015734/north | `transition:region-30:region-48` | True / directed | complete | village (1) / none established | highest remaining active utility |
| 930015734/south | `water-1951` | False / dead_end | structurally_supported | none established / remote shore projection (23) | coastal access |
| 930015734/south | `forest entrance:13013` | True / branching | structurally_supported | none established / remote shore projection (26) | forest entrance |
| 930015734/south | `water-12485` | True / directed | structurally_supported | none established / remote shore projection (26) | inland-water relationship, locally_valid |
| 930015734/south | `transition:region-12481:region-13013` | True / branching | structurally_supported | none established / remote shore projection (26) | persistent regional threshold |
| 930015734/south | `transition:region-12481:region-6399` | True / directed | structurally_supported | none established / remote shore projection (26) | highest remaining active utility |
| 930016664/south | `water-1403` | False / directed | structurally_supported | none established / remote shore projection (3) | coastal access |
| 930016664/south | `forest entrance:9848` | False / directed | structurally_supported | none established / persistent forest interior (1) | forest entrance |
| 930016664/south | `village:[-25, 121]` | False / directed | complete | village (1) / none established | settlement approach |
| 930016664/south | `highland approach:4156` | False / directed | complete | differentiated highland system (1) / none established | same_opportunity_group |
| 930019528/north | `forest entrance:65` | True / directed | structurally_supported | none established / persistent forest interior (1) | forest entrance, same_opportunity_group |
| 930019528/north | `village:[-117, 21]` | True / branching | complete | village (1) / none established | settlement approach |
| 930019528/north | `water-2110` | True / directed | structurally_supported | none established / persistent forest interior (1) | highest remaining active utility |
| 930019528/south | `highland approach:11124` | True / directed | complete | differentiated highland system (1) / none established | highland approach |
| 930019528/south | `village:[-121, -9]` | False / branching | complete | village (1) / none established | settlement approach |

## Natural versus authored responsibility

Natural world: terrain affordances, connected water, thresholds, geographic components, vanilla villages/structures and traversal topology. Authored map: Worksites, recurring animals, wild crops, lapis silos, minor POIs and other strategic resource placement. The seed need not supply every finished gameplay opportunity. Authored systems may complete a supported geographic relationship, not manufacture one from arbitrary terrain.

**Stopping principle:** Starter Route infrastructure should stop at the point where further pre-established infrastructure would begin solving the opportunity rather than merely exposing it. This pass diagnoses that principle and changes no Route length.

## Provisional heuristics and limitations

- mode: Shadow only. Consume frozen local-continuation fits; never call selection, refit routes, or change eligibility.
- arrival: Connected same-feature dry samples within the existing 32-block analytical approach scale; diagnostic only. One anchor can remain if evidence is sparse.
- cover_persistence: Use existing forest/highland masks on the modest dry graph. Existing 24-sample formation support plus four inward sample layers distinguish a core from a thin strip. This diagnostic heuristic is not eligibility or resource proof.
- water: Cardinal connected samples whose recorded surface block is water; ice/other surfaces excluded. Adjacent former ocean/inland IDs can share a diagnostic network, but active water identities are unchanged. Sample continuity is conditional, not boat clearance or depth proof.
- water_ports: Connected shore contacts grouped by frozen region and diagnostic water network; at least four shore samples. Region ID alone is not distinct value. Different land components or persistent forest/highland systems provide projection context.
- water_shape: Double-sweep geodesic extent is a lower bound. At least the inherited 32-block continuation scale and length >= twice area/length indicate corridor geometry provisionally. Multiple distinct shore contexts support network geometry. Neither proves payoff.
- payoff: Only observed village footprints and demonstrated differentiated geographic-system projection count as observed. Remote persistent-system/landmass access can be potential. Ore, crops, animals, caves and other structure access/value are unmeasured. No authored locations.
- near_shore_payoff: Village edge within the existing 32-block approach scale of a reachable shore is conditional landfall access; payoff may be far from the initial handoff along the water. No unlimited shore-to-any-village flood.
- comparison: Compare claimed entered systems with outward homeland sources, and remote water contacts with origin shore/system context. Different biome alone is never payoff.
- commitment: Qualitative dependency/geometry proxies, not fixed distance bands: natural starter low, modest medium, major high; exploitation/return unresolved where payoff or water navigation is unverified; exposure always unresolved.
- rejected_sample: Deterministic best-utility examples per feature kind, local-dead-end, locally-valid, water, village and duplicate-opportunity strata; at most 12 per homeland, not exhaustive or a new ranking.
- completion: Complete/Structurally Supported refer only to sampled analytical evidence, never physical readiness. Missing evidence is conservatively Incomplete with an unresolved reason, not proof of terrain failure.

Water samples can miss one-block barriers, waterfalls, submerged obstacles and narrow channels. Dry height is not compared with seabed height to invent embarkation grades. Separate dry traversal components are not necessarily islands: cliffs or graph limitations can split one landmass. Port potentials explicitly remain conditional for that reason. No unknown water outside the current window, underground navigation, non-village structure entrance or resource inventory is invented. Persistence core thresholds, shoreline grouping, and the comparative significance of geographic-system projection require sensitivity/manual validation before selection use.

A candidate with a nearby mask but no immediate modest entry is reported separately from absent geography. Regional/slope-foot/pass relationships that do not coincide with supported forest/highland masks are not fully interpreted by this first shadow model. In particular, 930010639 remains the manually promising analyzer test: unrecognized South relationships or a blocked immediate N1 entrance do not negate its broader highland/lowland/river/coast hierarchy.

## Scope verification and reproduction

The runner checks the exact eight-seed manifest, hashes all prior result/input files and existing active fitter modules, snapshots all available existing finalist/Task B worlds, and asserts that removing only the new shadow fields reproduces each baseline fit exactly. This verifies unchanged dimensions, handoffs, scoring/eligibility, local/deep results, segmentation and previous outputs. No search, world writer, active selector or Task C entry point is called. Historical-route deletion/scrambling is tested separately.

World and artifact counts plus hashes: `verification.json`. Test result: **126 tests passed** in 51.777 seconds; see `TEST_RESULTS.md`.

`python3 implementation/worldgen/fit_default_task_a_opportunities.py`

`python3 implementation/worldgen/fit_default_task_a_opportunities.py --verify`

Stop: manual shadow review and another bounded design/validation decision before allowing this evidence to influence selection.
