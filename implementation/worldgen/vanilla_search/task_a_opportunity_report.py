"""Spec 4 human-readable shadow interpretation, without recommendations to reselect."""
from collections import Counter
from .task_a_opportunities import supported, POLICY


def selected(f):return [r['opportunity_relationship'] for rows in f['selected_handoffs'].values() for r in rows]
def aff(r):return [a['type'] for a in r['affordances'] if a['evidence_state'] in ('strong','supported')]
def functions(r):return ', '.join(k.title()+': '+v['state'] for k,v in r['supported_functions'].items())
def burden(r):return '/'.join(r['commitment_profile'][k]['state'] for k in ('access_burden','exploitation_burden','return_burden','infrastructure_dependency','exposure'))
def payoff_text(items):
    counts=Counter(r['kind'] for r in items)
    return '; '.join(f'{kind} ({n})' for kind,n in counts.items()) or 'none established'


def report(fits):
    rows=[r for f in fits for r in selected(f)];counts=Counter(r['completion_state'] for r in rows)
    rejected=[r for f in fits for r in f['opportunity_shadow']['rejected_sample']]
    promising=[r for r in rejected if r['completion_state']!='incomplete']
    incomplete=[f'{f["seed"]}/{r["slot"]} {r["handoff"]["feature_kind"]}' for f in fits for r in selected(f) if r['completion_state']=='incomplete']
    redundant=[f'{f["seed"]}/{t}' for f in fits for t,s in f['opportunity_shadow']['opening_opportunity_sets']['homelands'].items() if s['diversity_state']=='redundant']
    lines=['# Default Opportunity Relationship shadow analysis','',
        'Authority: `specs/mapseedsearchspec4.md`. Diagnostic only. Current selections, eligibility, scoring, homelands, dimensions, terrain regions, local continuation and deep-network logic are unchanged. '
        'This is an explanation of frozen choices—not a new fitting decision or physical validation.',
        '', '## Required report questions','',
        '1. **Does this explain suspicious selections better?** It separates structural access from payoff: a local directed path can be an unexplained opportunity, while a terrestrial dead end may have water-network access. '
        'The improvement is diagnostic, not proof that the provisional semantic rules are ready for selection.',
        '2. **930010639 North coast:** see the eight explicit coastal questions below; surface connectivity and shore projection are separated from observed payoff and unverified embarkation/navigation.',
        f'3. **Locally valid but strategically unexplained:** {len(incomplete)} selected relationships are Incomplete in this shadow model: '+('; '.join(incomplete) or 'none')+'. '
        'Incomplete means required opportunity evidence was not established, not that the actual terrain is proven empty.',
        f'4. **Promising rejected candidates:** {len(promising)}/{len(rejected)} sampled unselected candidates are Complete or Structurally Supported. '
        'See the disagreement inventory. Sampling is stratified, not exhaustive; no eligibility exception or reselection is applied.',
        '5. **Completion counts:** '+', '.join(f'{k}: {counts[k]}' for k in ('complete','structurally_supported','incomplete'))+'. Complete is conditional sampled evidence, not a player-scale pass.',
        f'6. **Authored payoff dependence:** {counts["structurally_supported"]}/{len(rows)} selected relationships have coherent structure but lack established natural payoff; '
        'future authored payoff is one possible completion, not a mandate to place content. Incomplete relationships cannot be rescued by an arbitrary POI. Complete relationships may still need ordinary map implementation, but not invented payoff to explain them.',
        '7. **Strongly redundant whole sets:** '+(', '.join(redundant) or 'none established')+'. Individual substitute/overlapping pairs are reported below. Unknown payoff is not proof of complementarity or proof that an entire set is interchangeable.',
        '8. **Seed differences:** yes, measured inventories differ in observed village/geographic-system access, water projection potential, functional evidence and overlaps. '
        'These differences are not an aggregate quality ranking or a winning-seed decision.',
        '9. **Scale:** differentiation, networking and spatial/opportunity contestation are reported separately. No healthy scale target is established. '
        'Unresolved payoff and conditional water reach prevent a general assertion that every map realizes the intended phase order; map dimensions are unchanged.',
        '10. **Future selection signals:** evidence-linked payoff, same-system duplication, entered-system persistence, and separate water/spatial convergence merit manual evaluation. '
        'Port clustering, four-layer persistence, exact branch-context labels, inferred access privilege, commitment tiers, and geographic value remain too provisional to govern selection now.',
        '', '**Another design/validation pass is needed before this model influences selection.** Review the shadow disagreements manually, especially access privilege, payoff significance and physical water feasibility. '
        'Do not convert these diagnostics into weights, quotas, eligibility gates or automatic authored placements yet.',
        '', '## All-eight summary','',
        '| Seed | Selected N/S (frozen) | Complete / Structural / Incomplete | Unselected sample | Set N / S | Scale diagnostic |',
        '|---|---|---|---|---|---|']
    for f in fits:
        c=Counter(r['completion_state'] for r in selected(f));sets=f['opportunity_shadow']['opening_opportunity_sets']
        lines.append(f'| [{f["seed"]}](../default_local_continuation_2026-09-11/{f["seed"]}/greybox.svg) | '+ '/'.join(str(len(f['selected_handoffs'][t])) for t in ('north','south'))+' | '+
            '/'.join(str(c[k]) for k in ('complete','structurally_supported','incomplete'))+f' | {len(f["opportunity_shadow"]["rejected_sample"])} | '+
            ' / '.join(sets['homelands'][t]['diversity_state'] for t in ('north','south'))+f' | {sets["map_scale"]["state"]} |')
    lines+=['','## Every selected handoff','',
        'G/J/D/T = Gateway/Junction/Destination/Transition evidence states, not quality grades. Commitment = access/exploitation/return/infrastructure/exposure. '
        'Full IDs, envelopes, payoff evidence, all shore accesses and inherited reach are in each `fit.json`; the new object is `selected_handoffs[team][i].opportunity_relationship`.',
        '', '| Seed/slot | Current ID / kind | HD / Travel | Local | Supported affordances | G/J/D/T | Completion | Observed payoff | Potential payoff | Commitment | Relationship |',
        '|---|---|---|---|---|---|---|---|---|---|---|']
    for f in fits:
        for r in selected(f):
            h=r['handoff'];states='/'.join(r['supported_functions'][k]['state'] for k in ('gateway','junction','destination','transition'))
            lines.append(f'| {f["seed"]}/{r["slot"]} | `{r["source_candidate_id"]}` / {h["feature_kind"]} | {h["homeland_depth"]:.1f}/{h["travel_cost"]:.1f} | '
                f'{r["reach"]["local_continuation"]["continuation_class"]} | '+', '.join(aff(r))+f' | {states} | {r["completion_state"]} | '
                f'{payoff_text(r["payoff"]["observed"])} | {payoff_text(r["payoff"]["potential"])} | {burden(r)} | {r["evidence_summary"]["description"]} |')
    lines+=['','## Homeland Opportunity Sets','',
        'Connectivity uses the frozen conditional-modest spatial matrices. Opportunity convergence uses shared payoff/system identity and can exist without terrestrial overlap. '
        '“Contested” means prospective access by opposing teams, not observed combat. “Exclusive” is only within the sampled evidence, not a universal exclusion.',
        '', '| Seed/team | Diversity | Pair relationships | Projection contexts / payoff IDs | Eventual opportunity / spatial contestation | Payoff unresolved / incomplete |',
        '|---|---|---|---|---|---|']
    phase_lines=[]
    for f in fits:
        ss=f['opportunity_shadow']['opening_opportunity_sets']
        for team,s in ss['homelands'].items():
            pairtext='; '.join(str(n+1)+': '+(', '.join(p['labels']) or 'unresolved') for n,p in enumerate(s['same_team_pairs']))
            lines.append(f'| {f["seed"]}/{team} | {s["diversity_state"]} | {pairtext} | '
                f'{s["projection_coverage"]["context_count"]}/{len(s["projection_coverage"]["opportunity_ids"])} | '
                f'{s["credible_eventual_contestation"]}/{s["spatial_opponent_pathway"]} | '
                f'{len(s["authored_payoff_dependencies"])}/{len(s["incomplete_not_rescuable_by_authored_poi"])} |')
        phase_lines+=['',f'{f["seed"]} topology phases: differentiation '+str(ss['topology_phases']['differentiation'])+
            '; networking connections '+str({t:sum('connected' in p['labels'] for p in s['same_team_pairs']) for t,s in ss['homelands'].items()})+
            '; contestation '+str(ss['topology_phases']['contestation'])+'. '
            'Full same-team pair identities, counterfactual removal tests, spatial meetings and North/South opportunity pairs are in the JSON.','']
    lines+=phase_lines
    lines+=['## Focused interpretations','']
    for f in fits:
        if f['seed'] not in (930010639,930012642,930005557,930007222):continue
        lines+=[f'### {f["seed"]}','']
        chosen=selected(f)
        if f['seed']==930007222:chosen=[r for r in chosen if r['homeland']=='south' and r['handoff']['feature_kind']=='forest entrance']
        for r in chosen:
            lines+=[f'#### {r["slot"]}: {r["source_candidate_id"]}','',
                f'Anchor {r["handoff"]["anchor"]["world_xyz"]}; arrival envelope {r["handoff"]["arrival_envelope"]["sample_count"]} same-feature samples. '
                f'Local {r["reach"]["local_continuation"]["continuation_class"]}; {r["completion_state"]}.',
                '', 'Affordances: '+(', '.join(aff(r)) or 'none supported')+'. Functions: '+functions(r)+'.',
                '', 'Entered-system evidence: '+('; '.join(f'{c["system_id"]}: sampled threshold={c["threshold"]}, immediate modest entry={c["immediate_modest_entry"]}, '
                    f'whole-system samples/core={c["whole_system_sample_count"]}/{c["whole_system_core_samples"]}, whole-system inward layers={c["whole_system_max_inward_layers"]}, '
                    f'reached persistence={c["persistent"]}, local samples={c["local_entered_samples"]}, commitment={c["commitment"]}, '
                    f'already accessible at homeland={c["homeland_already_accesses_same_system"]}; entry limitation={c["entry_limitation"]}' for c in r['reach']['geographic_persistence']) or 'no sampled forest/highland entrance established; non-cover formations remain an analyzer limitation')+'.',
                '', 'Observed payoff: '+payoff_text(r['payoff']['observed'])+'. Potential payoff: '+payoff_text(r['payoff']['potential'])+'. '
                'Commitment (access/exploitation/return/infrastructure/exposure): '+burden(r)+'.',
                '', 'Weak evidence: '+('; '.join(r['evidence_summary']['weak_evidence']) or 'sampled interpretation only')+'.']
            if r['reach']['water_reach']:
                lines+=['','Water interpretation: '+'; '.join(f'{w["network_id"]}: interface={w["interface"]}, corridor={w["corridor"]}, '
                    f'network={w["network"]}, crossing={w["crossing"]}, barrier={w["barrier"]}, local feature={w["local_feature"]}; '
                    f'{len(w["shore_access"])} shore contacts, {sum(s["distinct_remote_access"] for s in w["shore_access"])} distinct remote contacts, '
                    f'path extent {w["maximum_water_path_blocks"]:.1f}; {w["navigation_state"]}' for w in r['reach']['water_reach'])+'.']
            if f['seed']==930010639 and r['slot']=='N2':
                ws=r['reach']['water_reach'];ports=[p for w in ws for p in w['shore_access'] if p['distinct_remote_access'] or p['downstream_villages']]
                lines+=['','**North coastal questions**','',
                    '1. Usable interface: '+str(any(w['interface'] for w in ws))+'. This means modest dry-bank contact with sampled open water; exact embarkation, water surface height and landing clearance remain unverified.',
                    '2. Corridor / maritime access: '+str(any(w['corridor'] for w in ws))+' / '+str(any(w['network'] for w in ws))+'. These are water-geometry diagnostics, not evidence that a coastline label is inherently valuable.',
                    '3. Distinct access: '+str(len(ports))+' shore contacts with differentiated regional context or a downstream village payoff; region types: '+(', '.join(sorted({p['region_type'] for p in ports})) or 'none established')+'. Full region IDs, land components, cover systems, water path lengths and nearby villages are emitted in water_reach.',
                    '4. Observed markedly differentiated payoff: '+payoff_text(r['payoff']['observed'])+'. Structure value/projection is distinguished from unmeasured resource concentration/scarcity.',
                    '5. Grounded placement potential: '+payoff_text(r['payoff']['potential'])+'. No authored location is placed; potential is not guaranteed gameplay value.',
                    '6. Completion: '+r['completion_state']+' (sampled/conditional).',
                    '7. Low terrestrial deep reach is still accurate for that terrestrial graph, but is not a universal Reach failure. Water Reach is a separate modality; its value depends on usable shores and payoff, not its distance.',
                    '8. Transition state: '+r['supported_functions']['transition']['state']+'. The previous selector evaluated local viability and feature strength, not a complete water-to-payoff chain. '
                    'This shadow explanation cannot retroactively claim that the old selector made a strategic-water decision.']
                lines+=['','Observed payoff identities and alternative access: '+str([{'id':p['id'],'via':p.get('via'),
                    'land_access':p.get('alternative_land_access'),'water_advantage':p.get('water_advantage')} for p in r['payoff']['observed']])+'. '
                    'Same water-network contacts already at homeland perimeter: '+str(sum(w['same_network_contacts_at_homeland_perimeter'] for w in ws))+'. '
                    'A coherent water-to-village chain does not prove that this road offers superior or exclusive access compared with direct land travel.']
        if f['seed'] in (930010639,930012642,930005557):
            for team,s in f['opportunity_shadow']['opening_opportunity_sets']['homelands'].items():
                lines+=['',f'{team.title()} set: {s["diversity_state"]}; geographic contexts {s["geographic_contexts"]}; payoff kinds {s["payoff_kinds"]}. '
                    'Removal test: '+'; '.join(x['relationship']+' → '+x['lost_meaningful_opportunity'] for x in s['removal_diagnostic'])+'.']
        if f['seed']==930012642:lines+=['','All current continuations remain directed. Directed has no penalty in these rules: persistence, appropriate Reach, payoff and access context—not branch maximization—determine interpretation.']
        if f['seed']==930005557:lines+=['','The settlement is tested as a direct gameplay-bearing arrival, not forced to be a Gateway. South highland-like candidates are compared by actual entered-system and payoff identities; different feature labels alone do not establish complementarity.']
        if f['seed']==930007222:lines+=['','The sampled forest has a substantial core; this is **not** evidence of a tiny forest strip. The current anchor lacks an immediate modest entry into that core, and the same system already contacts the homeland. '
            'Gateway evidence is therefore weak for this handoff. Its Structurally Supported interpretation comes from independently detected water-network access and remote shore potential, not a fabricated forest payoff. '
            'No observed payoff or authored location is assigned. This is why feature kind and actual Opportunity Relationship must remain separate.']
        lines+=['']
    lines+=['## Sampled unselected candidates and disagreements','',
        'Eligible-but-unselected and ineligible candidates are both included. “Promising” is a shadow interpretation, not a selection recommendation. '
        'All sampled records (including Incomplete) and strata are in each fit.json.','',
        '| Seed/team | Candidate | Active eligibility / local | Shadow | Observed / potential payoff | Why inspect |',
        '|---|---|---|---|---|---|']
    for f in fits:
        for r in f['opportunity_shadow']['rejected_sample']:
            if r['completion_state']=='incomplete':continue
            lines.append(f'| {f["seed"]}/{r["homeland"]} | `{r["source_candidate_id"]}` | {r["current_selection"]["eligible"]} / '
                f'{r["reach"]["local_continuation"]["continuation_class"]} | {r["completion_state"]} | '
                f'{payoff_text(r["payoff"]["observed"])} / {payoff_text(r["payoff"]["potential"])} | '+', '.join(r['sample_strata'])+' |')
    lines+=['','## Natural versus authored responsibility','',
        'Natural world: terrain affordances, connected water, thresholds, geographic components, vanilla villages/structures and traversal topology. '
        'Authored map: Worksites, recurring animals, wild crops, lapis silos, minor POIs and other strategic resource placement. '
        'The seed need not supply every finished gameplay opportunity. Authored systems may complete a supported geographic relationship, not manufacture one from arbitrary terrain.',
        '', '**Stopping principle:** Starter Route infrastructure should stop at the point where further pre-established infrastructure would begin solving the opportunity rather than merely exposing it. '
        'This pass diagnoses that principle and changes no Route length.',
        '', '## Provisional heuristics and limitations','']
    lines += [f'- {k}: {v}' for k,v in POLICY.items()]
    lines+=['','Water samples can miss one-block barriers, waterfalls, submerged obstacles and narrow channels. Dry height is not compared with seabed height to invent embarkation grades. '
        'Separate dry traversal components are not necessarily islands: cliffs or graph limitations can split one landmass. Port potentials explicitly remain conditional for that reason. '
        'No unknown water outside the current window, underground navigation, non-village structure entrance or resource inventory is invented. '
        'Persistence core thresholds, shoreline grouping, and the comparative significance of geographic-system projection require sensitivity/manual validation before selection use.',
        '', 'A candidate with a nearby mask but no immediate modest entry is reported separately from absent geography. Regional/slope-foot/pass relationships that do not coincide with supported forest/highland masks are not fully interpreted by this first shadow model. '
        'In particular, 930010639 remains the manually promising analyzer test: unrecognized South relationships or a blocked immediate N1 entrance do not negate its broader highland/lowland/river/coast hierarchy.',
        '', '## Scope verification and reproduction','',
        'The runner checks the exact eight-seed manifest, hashes all prior result/input files and existing active fitter modules, snapshots all available existing finalist/Task B worlds, '
        'and asserts that removing only the new shadow fields reproduces each baseline fit exactly. This verifies unchanged dimensions, handoffs, scoring/eligibility, local/deep results, segmentation and previous outputs. '
        'No search, world writer, active selector or Task C entry point is called. Historical-route deletion/scrambling is tested separately.',
        '', 'World and artifact counts plus hashes: `verification.json`. Test result: `TEST_RESULTS.md`.',
        '', '`python3 implementation/worldgen/fit_default_task_a_opportunities.py`',
        '', '`python3 implementation/worldgen/fit_default_task_a_opportunities.py --verify`',
        '', 'Stop: manual shadow review and another bounded design/validation decision before allowing this evidence to influence selection.','']
    return '\n'.join(lines)
