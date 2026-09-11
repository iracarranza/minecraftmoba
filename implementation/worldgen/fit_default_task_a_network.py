#!/usr/bin/env python3
"""Run destination-first analysis on existing finalist terrain; never author worlds."""
import base64
from html import escape
import json
from pathlib import Path
import time

from successor.grid import unrle
from fit_default_task_a import ROOT,SOURCE,OUTPUT,REFINED_OUTPUT,digest,encode,world_snapshot
from fit_default_task_a_destinations import OUT as PREVIOUS
from vanilla_search.task_a_network import analyze,MODES,PARAMETERS,LIMITS

OUT=ROOT/'implementation/worldgen/results/default_destination_first_2026-09-10'


def svg(fit,old,background):
    w=fit['sampling']['width'];h=fit['sampling']['height'];s=5;ox=30;oy=75;right=ox+w*s+25
    width=right+420;height=max(h*s+150,950)
    parts=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">',
           '<rect width="100%" height="100%" fill="#15211e"/><g fill="#f2f1e6" font-family="sans-serif">',
           f'<text x="30" y="28" font-size="22">Destination-first analysis · {fit["seed"]}</text>',
           '<text x="30" y="50" font-size="12">No imposed center, old departure or deeper Route target · analytical only</text>',
           f'<image x="{ox}" y="{oy}" width="{w*s}" height="{h*s}" href="data:image/png;base64,{base64.b64encode(background).decode()}"/>']
    def pt(sample):return ox+(sample[0]+.5)*s,oy+(sample[1]+.5)*s
    def line(path,color,width,dash=''):
        points=' '.join(f'{x},{y}' for x,y in map(pt,path))
        parts.append(f'<polyline points="{points}" fill="none" stroke="{color}" stroke-width="{width}" stroke-dasharray="{dash}"/>')
    def text(x,y,message,size=12):parts.append(f'<text x="{x}" y="{y}" font-size="{size}">{escape(message)}</text>')
    labels=unrle(fit['region_grid_rle']);boundaries=[]
    for z in range(h):
        for x in range(w):
            i=z*w+x
            if not labels[i]:continue
            if x+1<w and labels[i]!=labels[i+1]:boundaries.append(f'M{ox+(x+1)*s},{oy+z*s}v{s}')
            if z+1<h and labels[i]!=labels[i+w]:boundaries.append(f'M{ox+x*s},{oy+(z+1)*s}h{s}')
    parts.append(f'<path d="{" ".join(boundaries)}" stroke="#fff" stroke-opacity=".25" stroke-width=".7" fill="none"/>')
    for r in old['routes']:
        if r.get('sample_path'):line(r['sample_path'],'#555',1,'3 5')
    row=105
    for team,color in (('north','#48baff'),('south','#ff8974')):
        home=fit['homelands'][team];x0,x1,z0,z1=home['logical_footprint_samples']
        parts.append(f'<rect x="{ox+x0*s}" y="{oy+z0*s}" width="{(x1-x0+1)*s}" height="{(z1-z0+1)*s}" fill="none" stroke="{color}" stroke-width="3"/>')
        text(right,row,team.upper()+' HANDOFFS',16);row+=25
        for n,r in enumerate(fit['selected_handoffs'][team],1):
            c=next(c for c in fit['starter_corridors'][team] if c['destination']==r['id'])
            if c.get('sample_path'):line(c['sample_path'],color,4)
            x,y=pt(r['anchor']['sample']);name=team[0].upper()+str(n)
            parts.append(f'<circle cx="{x}" cy="{y}" r="5" fill="white" stroke="{color}"><title>{escape(r["id"])}</title></circle>')
            text(x+7,y-6,name);cont=r['continuation']['modest']
            text(right,row,f'{name}: {r["kind"]}');row+=18
            text(right,row,f'HD {r["homeland_depth"]:.1f} / travel {r["travel_cost"]:.1f} / {cont["continuation_class"]}');row+=18
            text(right,row,f'Deeper regions {len(cont["deeper_regions_reached"])}; {c["status"]}');row+=24
        text(right,row,f'Unresolved choices: {fit["unresolved_choices"][team]}');row+=40
    for message in ('HD: geometric Homeland Depth','Travel: independent terrain-weighted cost','Thin boundaries: coarse terrain components','Grey dashed: historical comparison only','No new physical infrastructure or Task C',f'Regions: {len(fit["regions"])} (descriptive)',
                    'Continuation and convergence: companion JSON','Major / conditional water links are not validated'):
        text(right,row,message);row+=22
    text(30,height-35,'Terrain/network diagnostics are sampled evidence, not a seed verdict or desired topology.')
    parts.append('</g></svg>');return '\n'.join(parts)+'\n'


def matrix(lines,title,rows,columns,values):
    lines+=['',title,'','| | '+' | '.join(columns)+' |','|---|'+'---|'*len(columns)]
    for name,row in zip(rows,values):
        cells=['—' if not m else f'{m["connection_homeland_depth"]:.1f}' if 'connection_homeland_depth' in m else f'{m["depth_a"]:.1f}/{m["depth_b"]:.1f}' for m in row]
        lines.append('| '+name+' | '+' | '.join(cells)+' |')


def report(fits,old):
    lines=['# Destination-first Default analysis','',
           'Descriptive analytical revision. No new seeds, worlds, physical authoring, Task C, topology targets or winner. Historical Routes are comparison/debug data only.',
           '', '## All eight finalists','',
           '| Seed | Coherent regions | Pool N/S | Selected N/S | Unresolved N/S | Prior anchored N/S | Natural / modest / major pair coverage |',
           '|---|---|---|---|---|---|---|']
    for f in fits:
        prior=old[f['seed']];pair=lambda obj:'/'.join(str(obj[t]) for t in ('north','south'))
        coverage='/'.join(f'{f["topology"]["by_dependency"][m]["convergence_pair_coverage"]:.2f}' for m in MODES)
        lines.append(f'| {f["seed"]} | {len(f["regions"])} | {pair({t:len(v) for t,v in f["destination_pools"].items()})} | {pair({t:len(v) for t,v in f["selected_handoffs"].items()})} | {pair(f["unresolved_choices"])} | {pair({t:v["anchored_routes"] for t,v in prior["destination_sets"].items()})} | {coverage} |')
    lines+=['','Counts and coverage are observations, not acceptance thresholds. “Modest” includes conditional sampled water crossings. Selected destinations with unresolved corridors are not counted as supported handoffs.','',
            '## Handoffs across all eight','',
            '| Seed/team | Handoff | Evidence | Homeland Depth | Travel Cost (field / finalized) | Continuation (modest) | Deeper regions/types | Dependency |',
            '|---|---|---|---|---|---|---|---|']
    for f in fits:
        for team,selected in f['selected_handoffs'].items():
            for n,r in enumerate(selected,1):
                c=next(c for c in f['starter_corridors'][team] if c['destination']==r['id']);cont=r['continuation']['modest']
                lines.append(f'| {f["seed"]}/{team} | {n}: {r["anchor"]["world_xyz"]} | {r["kind"]} | {r["homeland_depth"]:.1f} | {r["travel_cost"]:.1f} / {c.get("travel_cost_from_edge","unresolved")} | {cont["continuation_class"]}, gain {cont["depth_gain"]:.1f}, branches {cont["meaningful_branch_count"]} | {len(cont["deeper_regions_reached"])}/{len(cont["distinct_deeper_region_types"])} | {c.get("travel_dependency","unresolved")}{"; conditional water" if c.get("conditional_water_dependency") else ""} |')
            if f['unresolved_choices'][team]:lines.append(f'| {f["seed"]}/{team} | {f["unresolved_choices"][team]} unresolved | See solver/corridor diagnostics; not manufactured | — | — | — | — | — |')
    lines+=['','## Focused comparisons','',
            'Matrices report forward same-sample meeting, not merely entry into one large component. Same-team entries are the minimum shared depth ceiling (maximum of both approach depths); opposing entries report A/B at the minimum-max-depth meeting, not arrival time. No ideal depth is implied. Dependency classes describe continuation; Starter dependency is separate. Full natural/modest/major matrices, shared regions, isolation and exclusivity are in each fit.json.','']
    for f in fits:
        if f['seed'] not in (930010639,930012642,930005557):continue
        lines += [f'### {f["seed"]}','']
        topology=f['topology']['by_dependency']['modest']
        for team in ('north','south'):
            names=[team[0].upper()+str(n+1) for n in range(len(f['selected_handoffs'][team]))]
            matrix(lines,f'{team} lateral-connection depths (conditional modest)',names,names,topology['same_team_lateral_matrices'][team])
            lines+=['',f'Opening exclusivity: {json.dumps(topology["opening_exclusivity"][team],separators=(",",":"))}',
                    '',f'Previous historical-spine fit: '+', '.join(f'{r["id"]}: '+(r['starter']['destination']['kind'] if r['starter'].get('destination') else 'unresolved') for r in old[f['seed']]['routes'] if r['team']==team),
                    '', 'New weak evidence: '+'; '.join(r['kind']+': '+', '.join(r['weak_evidence']) for r in f['selected_handoffs'][team])]
        matrix(lines,'North/South convergence depths (conditional modest)',[f'N{i+1}' for i in range(len(f['selected_handoffs']['north']))],[f'S{i+1}' for i in range(len(f['selected_handoffs']['south']))],topology['north_south_convergence_matrix'])
        earliest=topology['earliest_opponent_convergence']
        growth=topology['connective_geography_growth']['observed_depth_profile']
        lines+=['',f'Earliest opposing meeting: '+(f'{earliest["depth_a"]:.1f}/{earliest["depth_b"]:.1f} at {earliest["first_shared_point"]["world_xyz"]}' if earliest else 'unresolved')+
                f'; broader shared regions {len(topology["broader_shared_regions"])}; isolated branches {json.dumps(topology["isolated_branches"])}; major-only opposing pairs {f["topology"]["major_dependency_pairs"]}.','',
                'Diagnostics: '+('; '.join(f['diagnostics']) or 'No unresolved current corridor choices; not a physical pass.'),'']
        if growth:
            lines+=['Observed overlap growth (not target bands): '
                    f'{growth[0]["regions_with_opposing_overlap"]} region(s) at depth ceiling {growth[0]["homeland_depth_ceiling"]:.1f}, '
                    f'rising to {growth[-1]["regions_with_opposing_overlap"]} regions and {growth[-1]["opposing_pairs_connected"]} opposing pairs '
                    f'by {growth[-1]["homeland_depth_ceiling"]:.1f}. Full event profile and region onsets are in fit.json.','']
        if f['seed']==930010639:
            highlands=[r for r in f['regions'] if r['dominant_signature'][3] or r['dominant_signature'][4]=='alpine']
            lines+=['Manual review already establishes meaningful mountain/highland, lowland, river, village, coast and regional/landmark hierarchy. '
                    f'This analyzer represents {len(highlands)} highland/alpine components, but does not validate named passes, valleys or landmark prominence. '
                    'Missing handoffs, coarse components, duplicate kinds or disconnected forward reach are analyzer limitations pending inspection—not automatic evidence against this seed.','']
    lines+=['## Interpretation and limits','',
            'Homeland Depth uses geometric edge length on the dry geographic graph plus explicit potential water links, without slope/canopy penalties. Travel Cost uses existing terrain burdens and is reported separately by natural/modest/major dependency. Nearby rough terrain increases cost, not geographic depth solely through its cost. Outward perimeter sources cover every valid dry face of the fixed homeland footprint inside the candidate window; no historical departure, opponent-facing half-plane or center constraint enters the field.',
            '', 'Terrain compression uses 32-block elevation resolution, existing 2/4-block sampled grade distinctions, broad biome family, woodland/open, highland and snow/coastal state. Components smaller than the existing 24-sample formation resolution merge by signature similarity and shared boundaries. These are analysis resolutions, not target region numbers. Water identities are connected systems; same-system accesses cannot become separate selected opportunities.',
            '', 'Every global destination is evaluated from each homeland. Candidate anchors are selected across full feature extents, without the previous 40–150 envelope, 32-block old-spine tube or old suffix join. All reachable candidates receive natural/modest/major forward continuation and backtracking diagnostics. Unreachable cases explicitly carry unavailable diagnostics.',
            '', 'Joint selection maximizes positive evidence/constructability utility using inherited prototype weights and soft travel calibration, plus map-relative Homeland Depth cost and binary continuation/network evidence. Pair costs evaluate new candidate corridors, estimated exits and shared early regions. No branch, convergence, region-count or map-size target is optimized. At most 36 eligible opportunity groups enter joint enumeration; retained group anchors and the score are analyzer heuristics, not new game design.',
            '', 'After selection, destinations remain fixed while all valid exits and then reuse-penalized early paths are tried. A single bounded destination-reconsideration pass retains successful destinations and excludes failed ones before retrying. Finalized cost may exceed the preliminary minimum-cost field. Remaining unsuccessful corridors are marked unresolved, not silently claimed as three supported roads. Continuation and whole-map topology derive only from terrain reach; they do not trace or reconnect to any historical Route.',
            '', 'Candidate-pool *_ref fields intern repeated region/type lists in continuation_region_sets and continuation_type_sets. Every candidate retains its own depth, branch, dependency and backtracking diagnostics; selected handoffs are expanded for direct inspection.',
            '', *[f'- {k}: {v}' for k,v in LIMITS.items()],
            '', '## Reproduction','', '`python3 implementation/worldgen/fit_default_task_a_network.py`',
            '', '`python3 -m unittest discover -s implementation/worldgen/tests -v`',
            '', 'Stop: analytical outputs and tests only. No physical greybox or Task C is authorized by these results.','']
    return '\n'.join(lines)


def run():
    seeds=sorted(json.loads((SOURCE/'search_summary.json').read_text())['finalist_seeds'])
    if len(seeds)!=8 or len(set(seeds))!=8:raise ValueError('Requires the existing eight-finalist manifest')
    candidates=[json.loads((SOURCE/'finalists'/str(s)/'candidate.json').read_text()) for s in seeds]
    protected=[SOURCE/'finalists'/str(s)/'candidate.json' for s in seeds]
    for directory in (OUTPUT,REFINED_OUTPUT,PREVIOUS,ROOT/'implementation/worldgen/results/default_task_b_2026-09-10'):
        protected.extend(p for p in sorted(directory.rglob('*')) if p.is_file())
    hashes={str(p.relative_to(ROOT)):digest(p) for p in protected}
    worlds=candidates+[{'seed':f'task_b_{s}','local_world_directory':str(ROOT/f'artifacts/worldgen/default_task_b_2026-09-10/{s}/world')} for s in (930010639,930012642)]
    before=world_snapshot(worlds);old={};fits=[];timings={}
    if OUT.is_symlink():raise ValueError('No symlink output')
    OUT.mkdir(exist_ok=True)
    def write(path,data):
        if path.is_symlink():raise ValueError('No symlink output')
        path.write_text(data)
    for c in candidates:
        seed=c['seed'];old[seed]=json.loads((PREVIOUS/str(seed)/'fit.json').read_text())
        if old[seed]['source_candidate_sha256']!=digest(SOURCE/'finalists'/str(seed)/'candidate.json'):raise ValueError('Source provenance mismatch')
        start=time.monotonic();f=analyze(c,old[seed]['homelands']);timings[str(seed)]=round(time.monotonic()-start,3)
        f['provenance']={'candidate_sha256':digest(SOURCE/'finalists'/str(seed)/'candidate.json'),'homeland_source_sha256':digest(PREVIOUS/str(seed)/'fit.json'),
                         'historical_route_use':'comparison/debug only; not passed to analyze'}
        f['historical_comparison']={'previous_destination_sets':old[seed]['destination_sets'],'routes':old[seed]['routes']}
        directory=OUT/str(seed)
        if directory.is_symlink():raise ValueError('No symlink candidate output')
        directory.mkdir(exist_ok=True);write(directory/'fit.json',encode(f))
        write(directory/'greybox.svg',svg(f,old[seed],(SOURCE/'finalists'/str(seed)/'05_actual_ground_canopy.png').read_bytes()))
        fits.append(f);print(f'{seed}: selected '+str({t:len(v) for t,v in f['selected_handoffs'].items()})+f'; unresolved {f["unresolved_choices"]}; {timings[str(seed)]}s',flush=True)
    write(OUT/'comparison.json',encode({'schema':'destination_first_comparison_v1','status':'descriptive, not a seed verdict','candidates':[
        {'seed':f['seed'],'regions':len(f['regions']),'selected_handoffs':f['selected_handoffs'],'unresolved_choices':f['unresolved_choices'],
         'topology':f['topology'],'previous_destination_sets':old[f['seed']]['destination_sets'],'diagnostics':f['diagnostics'],
         'fit':f'{f["seed"]}/fit.json','svg':f'{f["seed"]}/greybox.svg'} for f in fits]}))
    write(OUT/'REPORT.md',report(fits,old))
    if before!=world_snapshot(worlds):raise RuntimeError('World preservation failed')
    if hashes!={str(p.relative_to(ROOT)):digest(p) for p in protected}:raise RuntimeError('Prior artifacts changed')
    outputs=sorted(p for p in OUT.rglob('*') if p.is_file() and p.name!='verification.json')
    write(OUT/'verification.json',encode({'prior_inputs_and_artifacts_unchanged':True,'worlds_unchanged':True,'protected_sha256':hashes,
        'implementation_sha256':{str(p.relative_to(ROOT)):digest(p) for p in (Path(__file__),ROOT/'implementation/worldgen/vanilla_search/task_a_network.py')},
        'world_files_verified':{s:len(v.get('files',{})) for s,v in before.items()},'unavailable_worlds':[s for s,v in before.items() if not v['available']],
        'fit_seconds':timings,'output_sha256':{str(p.relative_to(OUT)):digest(p) for p in outputs}}))


if __name__=='__main__':run()
