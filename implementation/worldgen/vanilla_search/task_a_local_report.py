"""Compact evidence report for the bounded local/deep continuation correction."""
import json
from collections import Counter
from .task_a_local import POLICY


def selection_changes(fit,old):
    out={}
    for team in ('north','south'):
        before={r['id']:r for r in old['selected_handoffs'][team]}
        after={r['id']:r for r in fit['selected_handoffs'][team]}
        out[team]={'removed':[{'id':i,'kind':before[i]['kind'],'anchor':before[i]['anchor'],
            'current_local_class':next((r['local_continuation']['continuation_class'] for r in fit['destination_pools'][team] if r['id']==i and r.get('local_continuation')),None)} for i in sorted(before.keys()-after.keys())],
            'added':[{'id':i,'kind':after[i]['kind'],'anchor':after[i]['anchor']} for i in sorted(after.keys()-before.keys())],
            'retained_anchor_changes':[i for i in sorted(before.keys()&after.keys()) if before[i]['anchor']!=after[i]['anchor']]}
    return out


def report(fits,baseline):
    lines=['# Local handoff continuation / deep-network reach','',
        'Bounded analytical correction from `4ec2dc5b10e317f6a0025907333c16d0a8512c73`. '
        'No candidate winner or physical readiness is inferred. Local shape is descriptive, not an ordinal quality score. '
        'An ordinary local dead_end is ineligible; directed receives exactly the same viability term as branching/junction.',
        '', '## Derived local horizon and guards','']
    lines += [f'- **{k}**: {v}' for k,v in POLICY.items()]
    lines += ['', 'The band markers are inherited from Task A (40/70/110/150/210/250/350). '
        'They are applied to the existing geometric Homeland Depth field, not to Travel Cost. '
        'For example, a handoff in 40–70 is inspected through 110; no arbitrary 64/100/150-block local radius is introduced. '
        'The factor-two physical path cap and one-diagonal fixed backward allowance are provisional locality guards, not gameplay distances. '
        'Effective extent is the maximum least-cost travel burden within the local induced graph; physical extent is maximum least-length graph distance. '
        'Neither is a promised Minecraft walking distance. Exact-boundary discontinuities and open-ended extension are explicit limitations.',
        '', '## All eight','',
        '| Seed | Selected N/S | Unresolved N/S | Local dead_end pool N/S | Changed destination IDs N/S |',
        '|---|---|---|---|---|']
    for f in fits:
        old=baseline[f['seed']]
        pairs=lambda xs:'/'.join(str(v) for v in xs)
        lines.append(f'| {f["seed"]} | '+pairs(len(f['selected_handoffs'][t]) for t in ('north','south'))+' | '+
            pairs(f['unresolved_choices'][t] for t in ('north','south'))+' | '+
            pairs(sum(r.get('local_rejection') is not None for r in f['destination_pools'][t]) for t in ('north','south'))+' | '+
            pairs(len({r['id'] for r in f['selected_handoffs'][t]}^{r['id'] for r in old['selected_handoffs'][t]}) for t in ('north','south'))+' |')
    lines += ['', 'Changed IDs counts additions plus removals, not a quality delta. Pool rejection applies to the retained best anchor for each feature: '
        'this bounded pass does not search second-best anchors to rescue a rejected feature. Missing choices therefore remain evidence/local-analyzer/joint-selection limitations, not demonstrated terrain failure.',
        '', '## Every selected handoff','',
        'N1…/S1… use the JSON selected order. Evidence is sampled feature evidence; all physical entrances, crossings and geographic legibility remain provisional. '
        'Travel shows the field / finalized corridor cost. Extents are physical-path / effective (terrain-cost) maxima from the terminus, not from home.',
        '', '| Seed/team | Destination and evidence | HD | Travel | Local class; gain; branches | Local depth limit; extents | Deep: max HD; regions/types |',
        '|---|---|---|---|---|---|---|']
    for f in fits:
        for team,selected in f['selected_handoffs'].items():
            for n,r in enumerate(selected,1):
                c=next(c for c in f['starter_corridors'][team] if c['destination']==r['id'])
                local=r['local_continuation'];deep=r['deep_network_reach']['modest']
                lines.append(f'| {f["seed"]}/{team[0].upper()}{n} | {r["kind"]}: `{r["id"]}` at {r["anchor"]["world_xyz"]}; {r["evidence"]} | '
                    f'{r["homeland_depth"]:.1f} | {r["travel_cost"]:.1f}/{c.get("travel_cost_from_edge","unresolved")} | '
                    f'{local["continuation_class"]}; {local["depth_gain"]:.1f}; {local["meaningful_branch_count"]} | '
                    f'{local["analysis_depth_limit"]:.1f}; {local["physical_extent"]:.1f}/{local["effective_extent"]:.1f} | '
                    f'{deep["maximum_homeland_depth"]:.1f}; {len(deep["deeper_regions_reached"])}/{len(deep["distinct_deeper_region_types"])} |')
    locals_=[r['local_continuation'] for f in fits for ss in f['selected_handoffs'].values() for r in ss]
    lines += ['', 'Observed local scale across selected handoffs (not desired values): '+
        '; '.join(f'{key} {min(r[key] for r in locals_):.1f}–{max(r[key] for r in locals_):.1f}' for key in
        ('physical_extent','effective_extent','depth_gain'))+'.' if locals_ else 'No selected handoffs.',
        '', 'Class distribution: '+json.dumps(dict(Counter(r['continuation_class'] for r in locals_)))+'. '
        'The derived band horizon bounds the analysis but does not establish scale stability under perturbations; terrain burden can produce a wide effective extent. '
        'Inspect JSON guard_contact and discarded_pockets rather than treating all reported extents as equally confident.']
    lines += ['', 'Guard contact among selected modest local analyses: '+
        ', '.join(f'{key} {sum(r["guard_contact"][key] for r in locals_)}/{len(locals_)}' for key in ('depth_ceiling','depth_floor','physical_budget'))+'. '
        'A contact means reachable terrain touches an excluded edge, not proof that a useful branch was truncated. '
        'Frequent physical-budget contacts mean the provisional lateral-flood guard materially defines locality; horizon stability is still unresolved.',
        '', 'Both remaining unresolved choices (930015734 South and 930016664 North) were already unresolved in the baseline. '
        'The selected IDs on those teams are unchanged. Classify these as unresolved destination/joint-fitter capability, '
        'not demonstrated terrain failure or a newly lost handoff caused by this local filter.']
    lines += ['', '## Material selection changes from the previous pass','']
    for f in fits:
        changes=selection_changes(f,baseline[f['seed']])
        for team,change in changes.items():
            if any(change.values()):lines += [f'- {f["seed"]}/{team}: '+json.dumps(change,separators=(',',':'))]
    lines += ['', 'Other destination IDs and anchors are unchanged; local classifications and extents are new measurements. '
        'The previous deep method is retained, so whole-map matrices change only when selected inputs change.',
        '', '## Focused before / after','']
    for f in fits:
        if f['seed'] not in (930010639,930012642,930005557):continue
        old=baseline[f['seed']]
        lines += [f'### {f["seed"]}','']
        for team in ('north','south'):
            lines += ['', '| Team | Prior destination / terminus | Prior broad class; gain; ports | Current state |','|---|---|---|---|']
            current={r['id']:r for r in f['selected_handoffs'][team]}
            for r in old['selected_handoffs'][team]:
                d=r['continuation']['modest'];new=current.get(r['id'])
                state=('retained: local '+new['local_continuation']['continuation_class']+f', gain {new["local_continuation"]["depth_gain"]:.1f}') if new else 'not selected'
                pool=next((v for v in f['destination_pools'][team] if v['id']==r['id']),{})
                if not new:state+='; '+str(pool.get('local_rejection') or 'joint set/corridor choice')
                lines.append(f'| {team} | {r["kind"]} `{r["id"]}` / {r["anchor"]["world_xyz"]} | {d["continuation_class"]}; {d["depth_gain"]:.1f}; {d["meaningful_branch_count"]} | {state} |')
            additions=[r for r in current.values() if r['id'] not in {v['id'] for v in old['selected_handoffs'][team]}]
            lines += ['', team+' additions: '+('; '.join(r['kind']+' '+r['id']+' at '+str(r['anchor']['world_xyz']) for r in additions) or 'none')+'. '
                f'Unresolved: {f["unresolved_choices"][team]}. Local classes: '+', '.join(r['local_continuation']['continuation_class'] for r in current.values())+'.',
                '', 'Weak evidence: '+'; '.join(r['kind']+': '+', '.join(r['weak_evidence']) for r in current.values())+'.','']
        if f['seed']==930010639:
            previous_coast=[r for r in old['selected_handoffs']['north'] if r['kind']=='coastal access' and r['continuation']['modest']['continuation_class']=='dead_end']
            retained=[r for r in f['selected_handoffs']['north'] if any(r['id']==p['id'] and r['anchor']==p['anchor'] for p in previous_coast)]
            types=sorted({typ for ss in f['selected_handoffs'].values() for r in ss for typ in r['deep_network_reach']['modest']['distinct_deeper_region_types']})
            water=sum(not w['ocean'] for w in f['water_systems'])
            lines += ['#### Primary analyzer questions','',
                '1. Previous North coastal broad dead_end still selected: '+('yes (local viability must be judged separately from its old broad class)' if retained else 'no')+'.',
                '2. Replacement/additions and North supported count are listed above: '+str(3-f['unresolved_choices']['north'])+'/3. No forced third.',
                '3. Team local classes are listed above in selected order.',
                '4. The local component rule separates directed (one persistent outward component) from branching/junction (two/three-plus); it does not inherit distant regional ports. '
                'Open interconnected terrain can conservatively collapse to directed, and turns/forks beyond the immediate removed neighborhood remain one initial choice. Synthetic tests verify the distinctions; player-perceived formation quality remains unverified.',
                f'5. Deep reach is preserved unchanged at identical anchors and still exposes {len(types)} broader region types in the modest proxy, with {water} inland water systems in the unchanged terrain graph. '
                'Represented types: '+', '.join(types)+'. '
                'This retains sampled highland/open-lowland/woodland/coastal reach, not proof of the manually observed river/pass/valley hierarchy or actual crossings. Water identities remain independent evidence. Manual geography is not invalidated by local filtering.',
                '6. Exact physical/effective local extents for all six (or fewer) primary handoffs are in the all-handoff table and JSON; no distances are canonized.','']
            for r in retained:
                c=r['local_continuation'];b=c['branches']
                lines += [f'Coastal retention evidence: start HD {c["start_depth"]:.3f}, fixed floor {c["depth_floor"]:.3f}, '
                    f'actual minimum HD {c["minimum_depth_reached"]:.3f}, maximum {c["max_depth_reached"]:.3f}; '
                    f'{c["meaningful_branch_count"]} persistent local choice ({sum(v["sample_count"] for v in b)} samples), '
                    f'locally reached regions {c["regions_reached"]}, new types {c["entered_region_types"]}. '
                    f'Small backward detour required: {c["small_backward_detour_dependency"]}; '
                    f'natural/modest/major branches {c["natural_branch_count"]}/{c["modest_branch_count"]}/{c["major_branch_count"]}. '
                    'This is the same generic fixed-floor rule used for every feature, not a coast exception. '
                    'Its old strict-forward deep reach remains limited; local viability does not imply rich eventual network participation.','']
    lines += ['## All-eight topology (unchanged deep-network method)','',
        'Matrices use selected-order N1…/S1…. Same-team cells are minimax shared Homeland Depth; opposing cells show North/South depths. '
        'These remain deep-network diagnostics and never determine local class. The report shows conditional modest graphs; JSON retains all three sensitivities.','']
    def matrix(title,rows,columns,values):
        lines.extend(['',title,'','| | '+' | '.join(columns)+' |','|---|'+'---|'*len(columns)])
        for name,row in zip(rows,values):
            cells=['—' if not m else f'{m["connection_homeland_depth"]:.1f}' if 'connection_homeland_depth' in m else f'{m["depth_a"]:.1f}/{m["depth_b"]:.1f}' for m in row]
            lines.append('| '+name+' | '+' | '.join(cells)+' |')
    for f in fits:
        topology=f['topology']['by_dependency']['modest'];names={t:[t[0].upper()+str(n+1) for n in range(len(f['selected_handoffs'][t]))] for t in ('north','south')}
        lines += [f'### {f["seed"]}','']
        for team in names:
            matrix(team+' lateral',names[team],names[team],topology['same_team_lateral_matrices'][team])
            lines+=['','Opening exclusivity: '+json.dumps(topology['opening_exclusivity'][team],separators=(',',':'))]
        matrix('North/South convergence',names['north'],names['south'],topology['north_south_convergence_matrix'])
        earliest=topology['earliest_opponent_convergence']
        lines+=['','Earliest opposing depth: '+(f'{earliest["depth_a"]:.1f}/{earliest["depth_b"]:.1f}' if earliest else 'unresolved')+
            f'; modest pair coverage {topology["convergence_pair_coverage"]:.2f}; broader shared regions {len(topology["broader_shared_regions"])}; '
            f'isolated {json.dumps(topology["isolated_branches"])}; major-only pairs {f["topology"]["major_dependency_pairs"]}.',
            '', 'Diagnostics: '+('; '.join(f['diagnostics']) or 'No unresolved analytical corridor choices; not a physical pass.')]
    lines+=['','## Limitations and scope verification','',
        '- Local natural/modest/major counts are cumulative sensitivity graphs. Adding edges can merge branches, so counts are not additive or necessarily monotonic. Major includes unverified water transport; it is not proof that large terrain edits are required.',
        '- Backtracking dependency distinguishes a necessary allowed small backward detour (strict-forward has no branch) from viability available only below the fixed local floor. The latter remains locally dead_end/ineligible. Additional reach can require backtracking even when a viable no-backtracking choice already exists; this is separately counted.',
        '- No second-best feature anchor rescue, segmentation redesign, destination-weight tuning, map resizing or desired topology target changes. Immediate core/persistence and derived-band sensitivity remain analyzer limitations, not seed failure evidence.',
        '- No new seeds generated: runner consumes exactly the existing eight-finalist manifest; no search/generation entry point is called.',
        '- No Minecraft worlds changed/materialized: before/after hashes for all available finalist and Task B world files are in verification.json. Unavailable worlds, if any, are explicitly listed there.',
        '- No Task C: only analyzer, tests, analytical JSON/SVG and documentation outputs.',
        '- No historical Route dependency: analyze accepts only terrain, fixed homelands and parameters. Deletion/scrambling and mocked-old-solver regression tests are retained.',
        '- Prior analytical outputs preserved: verification.json includes hashes of all previous Task A/refit/network/Task B result files and input candidates.',
        '- Test count/result: see TEST_RESULTS.md in this directory (written after executing the full suite).',
        '', 'Reproduce: `python3 implementation/worldgen/fit_default_task_a_network.py`; '
        '`python3 -m unittest discover -s implementation/worldgen/tests -q`.',
        '', 'Stop: analytical human review only. No new physical greybox, Task C, skeleton freeze, or seed rejection is authorized by this pass.','']
    return '\n'.join(lines)
