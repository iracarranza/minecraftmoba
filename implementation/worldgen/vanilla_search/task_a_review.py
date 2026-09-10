"""Machine-supported A.2 comparison and explicit human-readiness gate.

This module cannot approve human criteria or authorize physical authoring.
"""


def readiness(fits):
    candidates=[]
    for f in fits:
        home=f['homeland_equivalence']; central=f['central_connective_area']
        routes=f['routes']; diff=f['route_differentiation']
        checks={
            'homeland_placement_believable':{'automated_evidence':all(home['independently_usable'].values()) and home['classification']!='Severe','human_validation':None},
            'three_meaningful_openings_each_team':{'automated_evidence':all(v['three_meaningful_choices_supported'] for v in diff.values()),'human_validation':None},
            'starter_handoffs_plausible':{'automated_evidence':all(r.get('starter') and 58<=r['starter']['effective_blocks_from_edge']<=76 and r['starter']['refined_feasibility']['score']>=.5 for r in routes),'human_validation':None},
            'obvious_route_collapse_absent':{'automated_evidence':not any(q['functional_branch_collapse'] for v in diff.values() for q in v['pairs']),'human_validation':None},
            'central_connectivity_understood':{'automated_evidence':central['north_south_interaction_supported'],'human_validation':None},
            'major_lateral_connections_plausible':{'automated_evidence':any(c['lateral'] for c in f['cross_connections']),'human_validation':None},
            'geographic_depth_represented':{'automated_evidence':all(v['geographic_depth_supported'] for d in f['operational_depth'].values() for v in d['bands'].values()),'human_validation':None},
            'no_fundamental_diagnostic_mismeasurement':{'automated_evidence':None,'human_validation':None},
            'human_review_selects_candidate':{'automated_evidence':None,'human_validation':None},
        }
        candidates.append({'seed':f['seed'],'checks':checks,'gate_passed':False,
                           'negative_analytical_checks':[k for k,v in checks.items() if v['automated_evidence'] is False],
                           'pending_human_checks':list(checks)})
    return {'authority':'specs/mapseedsearchspec3.md sections 13–16',
            'a1_status':'completed','a2_status':'automated comparison complete; human comparative validation and selection pending',
            'task_b_ready':False,'selected_candidates':[], 'physically_validated_candidates':[],
            'skeleton_frozen':False,'candidates':candidates,
            'stop_reason':'Task B requires a human review selecting one or two candidates after the readiness criteria are accepted. No such A.1 review/selection has been supplied.',
            'unexecuted_stages':['B.0 physical greybox','B.1 player-scale validation','B.2 revision/freeze','C gameplay placement','later physical authoring'],
            'prior_manual_inspection_agreement':'unavailable: no completed human comparison of these revised A.1 overlays with world inspection is supplied'}


def comparison_report(fits,gate):
    lines=['# Task A.2 comparative validation packet','',
           'Working analytical comparison, not a human selection or seed ranking. A.1 retains the A.0 bounds, homeland/Fountain geometry and six Route spines; it refines what the measurements mean. No seed-specific edits were made.','',
           '| Seed | Homeland disparity | Usable N/S | Choices N/S | Shared central clusters | Incidental / useful / shortcut | Geographic bands N/S |',
           '| --- | --- | --- | --- | ---: | --- | --- |']
    for f in fits:
        h=f['homeland_equivalence']; m=f['network_metrics']; c=m['connection_counts']
        usable='/'.join('yes' if h['independently_usable'].get(t) else 'no' for t in ('north','south'))
        choices='/'.join('supported' if f['route_differentiation'][t]['three_meaningful_choices_supported'] else 'unproven' for t in ('north','south'))
        bands=' ; '.join(t[0].upper()+': '+str(sum(v['geographic_depth_supported'] for v in f['operational_depth'][t]['bands'].values()))+'/3' for t in ('north','south'))
        lines.append(f'| {f["seed"]} | {h["classification"]} ({h.get("absolute_disparity")}) | {usable} | {choices} | {f["central_connective_area"]["shared_team_cluster_count"]} | {c["incidental_traversability"]} / {c["useful_lateral_connection"]} / {c["strategic_shortcut"]} | {bands} |')
    lines+=['','“Supported” means sampled evidence, not a physical or experiential pass. Geographic-band counts distinguish formation evidence from connected land; they are not biome counts. Link counts are filtered observed formation relationships, not a fixed quota.','']
    for f in fits:
        lines += [f'## {f["seed"]}','',f'[Fit JSON]({f["seed"]}/fit.json) · [Greybox SVG]({f["seed"]}/greybox.svg)','',
                  f'A.0 diagnostics: {len(f["a0_diagnostics"]["failures"])}. A.1 diagnostics: {len(f["failures"])}. These counts are not comparable scalar scores: their semantics changed.','']
        lines += ['- '+v for v in f['failures']] or ['- No analytical flags; this does not bypass human review.']
        lines+=['']
    lines+=['## Regression preservation','',
            'All eight remain regression substrates, including the pre-A.1 interest candidates 930010639, 930012642 and 930019528. None is promoted by that prior interest. Generic focused fixtures exercise Route collapse, homeland inequivalence, over-centralization, under-convergence, incidental shortcuts and forced Starter construction. A.0 artifacts remain unchanged for comparison.','',
            '## Human comparative review — pending','',
            'For each candidate under consideration, record evidence for:','',
            '- Natural plausibility of the three corridors and differentiated opening decisions.',
            '- Sensible Starter handoffs and modest construction feasibility.',
            '- Shared interior interaction without mandatory point convergence.',
            '- Recognizable landforms behind the accepted lateral links.',
            '- Genuine secondary, tertiary and deep geography from each homeland.',
            '- Competitive acceptability of homeland component differences.',
            '- Agreement between these SVGs and prior player-scale world inspection.',
            '- Whether any diagnostic fundamentally mismeasures the Default grammar.','',
            'No agreement with prior manual inspection, human preference, player-scale walk result or final selection is inferred from the analytical data.','',
            '## Readiness gate and sequence stop','',gate['stop_reason'],'',
            'See [readiness_gate.json](readiness_gate.json) for all nine criteria per candidate, including negative analytical evidence and pending human judgments. A.2 selection is not completed. Selected candidates: none. Physically validated candidates: none. Skeleton freeze: not reached.','',
            'B.0, B.1, B.2, C and later authoring were not executed because the Task A→B gate is not passed. No worlds were generated or edited. The sequence can resume at human A.2 review and selection; it must not jump to physical construction.','',
            '## Limitations','',
            'Eight-block sampling cannot prove block walkability, water depth/ford viability, construction volume, caves/ravines/overhangs, sightlines, true mandatory chokepoints, resource/ecology equivalence, Hunger cost or measured travel time. Conservative surface disconnection is not proof of physical impossibility. Landform masks and connected band overlap are geographic evidence, not proof of human-perceived significance.','']
    return '\n'.join(lines)
