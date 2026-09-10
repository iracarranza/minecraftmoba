#!/usr/bin/env python3
"""Destination refit of the existing eight finalists; analytical outputs only."""
import json
from pathlib import Path
import time

from fit_default_task_a import ROOT, SOURCE, OUTPUT, REFINED_OUTPUT, digest, encode, world_snapshot
from vanilla_search.task_a_destinations import refit, PARAMETERS, LIMITS
from vanilla_search.task_a_render import svg

OUT=ROOT/'implementation/worldgen/results/default_task_a_destinations_2026-09-10'


def report(results,old):
    lines=['# Destination-anchored Starter refit','',
           'Analytical / non-authoritative. No winner, freeze, physical edits, new seeds or Task C. Human review must accept the refit before materialization.',
           '', 'Homelands, Fountains, bounds, orientation, departures and deeper Route targets are unchanged. Existing A.0/A.1/B artifacts remain historical evidence, not validation of these new paths.',
           '', '## Eight-candidate comparison','',
           '| Seed | Anchored N/S | Distinct formations N/S | Opportunity kinds N/S | New depth range | Outside preferred band | Diagnostics |',
           '|---|---|---|---|---|---|---|']
    for f in results:
        ss=f['destination_sets'];starters=[r['starter'] for r in f['routes'] if r.get('starter') and r['starter'].get('destination')]
        depths=[s['effective_blocks_from_edge'] for s in starters]
        pair=lambda key:'/'.join(str(ss[t][key]) for t in ('north','south'))
        lines.append(f'| {f["seed"]} | {pair("anchored_routes")} | {pair("distinct_features")} | {pair("distinct_opportunity_kinds")} | {min(depths,default=0):.1f}–{max(depths,default=0):.1f} | {sum(s["outside_preferred_depth_band"] for s in starters)} | {len(f["failures"])} |')
    lines+=['','## Focused before / after','', 'Coordinates are actual world XYZ; depth is effective blocks from the homeland edge. “Unresolved” means no replacement is asserted; the old spine remains diagnostic only. Feature IDs are connected sampled interfaces, not proof of distinct named water bodies or visually distinct landforms.','']
    for f in results:
        if f['seed'] not in (930010639,930012642): continue
        lines += ['',f'### {f["seed"]}','','| Route | Old terminus / depth | New terminus / depth | Anchor | Weak evidence |','|---|---|---|---|---|']
        prior={r['id']:r for r in old[f['seed']]['routes']}
        for r in f['routes']:
            s=r['starter'];a=prior[r['id']]['starter'];d=s.get('destination')
            new=f'{s["terminus"]["world_xyz"]} / {s["effective_blocks_from_edge"]:.1f}' if d else 'Unresolved'
            lines.append(f'| {r["id"]} | {a["terminus"]["world_xyz"]} / {a["effective_blocks_from_edge"]:.1f} | {new} | {d["id"] if d else "No supported destination in bounded search"} | {"; ".join(d["weak_evidence"]) if d else "See path/search exclusions in fit.json; not a seed rejection"} |')
        for team in ('north','south'):
            a=old[f['seed']]['route_differentiation'][team];b=f['route_differentiation'][team];d=f['destination_sets'][team]
            lines+=['',f'{team}: three-choice analytical support {a["three_meaningful_choices_supported"]} → {b["three_meaningful_choices_supported"]}; anchored {d["anchored_routes"]}/3, {d["distinct_features"]} interfaces, {d["distinct_opportunity_kinds"]} kinds; joint pair penalty {d["joint_pair_penalty"]}.',
                    f'Minimum terminus separation {min(q["terminus_separation_blocks"] for q in a["pairs"]):.1f} → {min(q["terminus_separation_blocks"] for q in b["pairs"]):.1f} blocks; maximum opening overlap {max(q["opening_overlap"] for q in a["pairs"]):.2f} → {max(q["opening_overlap"] for q in b["pairs"]):.2f}. These geometry metrics include unresolved historical spines and do not establish destination coverage.',
                    'Pair-by-pair old/new overlap, separation and continuation-collapse evidence is retained in comparison.json. Full-network geometry is recomputed; no B.1 physical pass is transferred to a revised route.']
    lines+=['','## All-candidate diagnostics','']
    for f in results: lines.append(f'- {f["seed"]}: '+('; '.join(f['failures']) or 'No current analytical failure diagnostics.'))
    lines+=['','## Method and unsupported assumptions','',
            'Observable candidates: actual village piece edges; inland dry banks; coast access; lowland-to-highland and forest interfaces; persistent biome interfaces; sampled flat-to-rising slope feet. Ordinary empty terrain is never a successful destination. Passes, valleys, saddles and true junctions are not asserted from insufficient extraction data.',
            '', 'Each Route retains its homeland approach and deeper target. Conservative dry/rise-limited paths search a 32-block neighborhood of its original spine and reconnect to its exact original deeper suffix. Require a loop-free path, sampled feasibility ≥0.5, at least 32 effective blocks of unsupported continuation and at most the existing 1.8 detour factor. An unavailable replacement retains old geometry explicitly marked unresolved, not a destination fit.',
            '', 'Soft distance cost = |depth − 65| / 65. Add construction cost 3×(1−feasibility), evidence cost (1−strength), relative corridor detour and a dry-neighbor handoff cost. Strengths 1 for direct feature interfaces, 0.75 for slope-foot profiles and 0.5 for biome-only interfaces are uncalibrated evidence proxies, not measured gameplay utility. Dry neighboring samples are not proof of distinct branches.',
            '', 'Jointly enumerate retained candidate combinations for each team. Penalize shared feature (3), same opportunity kind (1), fuzzy opening overlap (3×fraction), and separation below the existing 24-block scale (3×normalized deficit). Search top 3 per feature and 12 per Route, retaining each feature’s best option before its additional nearby anchors; this is bounded joint optimization, not a global optimum. Missing routes remain explicit. Different interface IDs can still represent similar geography.',
            '', 'The combined three-choice diagnostic also requires three anchored, distinct interfaces. Fewer than two opportunity kinds emits an explicit prototype narrow-coverage warning, not a failure or required biome portfolio. Different geographic opportunities can share a kind.',
            '', 'The 40–150 search envelope reuses fringe/secondary-transition boundaries as a conservative prototype bound, not a new termination target. It can exclude real destinations beyond 150 or outside the corridor neighborhood; unresolved routes are fitter limitations, not automatic seed rejections. No unsupported destination is manufactured to meet a quota.',
            '', 'Slope-foot detection reuses the 32-block approach scale, 2-block flatness and 8-block relief: flat behind, monotonically rising ahead. Four connected evidence samples are required. This is not proof of mountain scale or visual significance.',
            '', *[f'- {key}: {value}' for key,value in LIMITS.items()],
            '', 'Existing Key Locations are regenerated from the revised network by the shared framework. Unverified narrow-neck/open-region references are not promoted into strong destinations merely because they were previously labeled Key Locations.',
            '', 'Block walkability, construction volume, water depth/fords, true chokepoints, visibility, resources, ecology, Hunger and measured travel time remain unavailable. Prior B.1 discrepancy notes remain relevant but were not used for per-seed tuning.',
            '', '## Stop / next step','', 'Human review of these JSON/SVG fits and especially unresolved/weak handoffs. No physical materialization or Task C is authorized by this run.','']
    return '\n'.join(lines)


def run():
    seeds=sorted(json.loads((SOURCE/'search_summary.json').read_text())['finalist_seeds'])
    if len(seeds)!=8 or len(set(seeds))!=8: raise ValueError('Expected existing eight finalists')
    candidates=[json.loads((SOURCE/'finalists'/str(s)/'candidate.json').read_text()) for s in seeds]
    protected=[SOURCE/'finalists'/str(s)/'candidate.json' for s in seeds]
    for directory in (OUTPUT,REFINED_OUTPUT,ROOT/'implementation/worldgen/results/default_task_b_2026-09-10'):
        protected.extend(p for p in sorted(directory.rglob('*')) if p.is_file())
    before={str(p.relative_to(ROOT)):digest(p) for p in protected}
    worlds=candidates+[{'seed':f'task_b_{s}','local_world_directory':str(ROOT/f'artifacts/worldgen/default_task_b_2026-09-10/{s}/world')} for s in (930010639,930012642)]
    snapshot=world_snapshot(worlds)
    if OUT.is_symlink(): raise ValueError('No symlink output directory')
    OUT.mkdir(exist_ok=True);results=[];old={};timings={}
    for c in candidates:
        seed=c['seed'];old[seed]=json.loads((REFINED_OUTPUT/str(seed)/'fit.json').read_text())
        source_hash=digest(SOURCE/'finalists'/str(seed)/'candidate.json')
        if old[seed]['source_candidate_sha256']!=source_hash: raise ValueError('Prior fit provenance mismatch')
        start=time.monotonic();f=refit(c);timings[str(seed)]=round(time.monotonic()-start,3)
        for key in ('playable_bounds','logical_orientation','homelands'):
            if f[key]!=old[seed][key]: raise ValueError('Bounded revision changed '+key)
        for a,b in zip(old[seed]['routes'],f['routes']):
            for key in ('id','departure','target'):
                if a.get(key)!=b.get(key): raise ValueError('Bounded revision changed '+key)
        f['source_candidate_sha256']=source_hash
        f['source_a1_fit_sha256']=digest(REFINED_OUTPUT/str(seed)/'fit.json')
        directory=OUT/str(seed)
        if directory.is_symlink(): raise ValueError('No symlink candidate output')
        directory.mkdir(exist_ok=True)
        for name,data in (('fit.json',encode(f)),('greybox.svg',svg(f,c,(SOURCE/'finalists'/str(seed)/'05_actual_ground_canopy.png').read_bytes()))):
            path=directory/name
            if path.is_symlink(): raise ValueError('No symlink output file')
            path.write_text(data)
        results.append(f)
        print(f'{seed}: anchored '+str({t:d['anchored_routes'] for t,d in f['destination_sets'].items()})+f'; {timings[str(seed)]}s',flush=True)
    aggregate=[]
    for f in results:
        changes=[]
        for a,b in zip(old[f['seed']]['routes'],f['routes']):
            s=b['starter'];changes.append({'route':b['id'],'old_terminus':a['starter']['terminus'],
                'old_depth':a['starter']['effective_blocks_from_edge'],
                'new_terminus':s['terminus'] if s.get('destination') else None,
                'new_depth':s['effective_blocks_from_edge'] if s.get('destination') else None,
                'destination':s.get('destination')})
        aggregate.append({'seed':f['seed'],'destination_sets':f['destination_sets'],'before_after':changes,
                          'old_differentiation':old[f['seed']]['route_differentiation'],
                          'new_differentiation':f['route_differentiation'],'network_metrics':f['network_metrics'],
                          'failures':f['failures'],'fit':f'{f["seed"]}/fit.json','svg':f'{f["seed"]}/greybox.svg'})
    for name,data in (('comparison.json',encode({'schema':'destination_comparison_v1','status':'human analytical review required; no physical readiness claimed','parameters':PARAMETERS,'candidates':aggregate})),('REPORT.md',report(results,old))):
        path=OUT/name
        if path.is_symlink(): raise ValueError('No symlink report')
        path.write_text(data)
    if snapshot!=world_snapshot(worlds): raise RuntimeError('World immutability verification failed')
    if before!={str(p.relative_to(ROOT)):digest(p) for p in protected}: raise RuntimeError('Prior artifact preservation failed')
    outputs=sorted(p for p in OUT.rglob('*') if p.is_file() and p.name!='verification.json')
    verification={'prior_inputs_and_artifacts_unchanged':True,'protected_sha256':before,
                  'implementation_sha256':{str(p.relative_to(ROOT)):digest(p) for p in [
                      Path(__file__),ROOT/'implementation/worldgen/vanilla_search/task_a.py',
                      ROOT/'implementation/worldgen/vanilla_search/task_a_refine.py',
                      ROOT/'implementation/worldgen/vanilla_search/task_a_destinations.py',
                      ROOT/'implementation/worldgen/vanilla_search/task_a_render.py',
                      ROOT/'specs/mapseedsearchspec2.md',ROOT/'specs/mapseedsearchspec3.md']},
                  'available_clean_and_task_b_worlds_unchanged':True,
                  'world_files_verified':{s:len(v.get('files',{})) for s,v in snapshot.items()},
                  'unavailable_worlds':[s for s,v in snapshot.items() if not v['available']],
                  'fit_seconds':timings,'output_sha256':{str(p.relative_to(OUT)):digest(p) for p in outputs}}
    path=OUT/'verification.json'
    if path.is_symlink(): raise ValueError('No symlink verification output')
    path.write_text(encode(verification))
    return verification


if __name__=='__main__': run()
