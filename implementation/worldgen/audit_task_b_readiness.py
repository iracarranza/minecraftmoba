#!/usr/bin/env python3
"""Re-evaluate the Task B gate after the user's explicit A.2 selection."""
import json
from pathlib import Path

from fit_default_task_a import ROOT, SOURCE, OUTPUT, REFINED_OUTPUT, digest, encode, world_snapshot
from vanilla_search.task_a_review import selected_readiness

DECISION=ROOT/'implementation/worldgen/decisions/task_b_selection_2026-09-10.json'
OUT=ROOT/'implementation/worldgen/results/default_task_b_gate_2026-09-10'


def run():
    decision=json.loads(DECISION.read_text())
    seeds=[r['seed'] for r in decision['selected_candidates']]
    fits=[json.loads((REFINED_OUTPUT/str(s)/'fit.json').read_text()) for s in seeds]
    candidates=[json.loads((SOURCE/'finalists'/str(s)/'candidate.json').read_text()) for s in seeds]
    protected=[DECISION]+[p for root in (OUTPUT,REFINED_OUTPUT) for p in sorted(root.rglob('*')) if p.is_file()]
    protected += [SOURCE/'finalists'/str(s)/'candidate.json' for s in seeds]
    before={str(p.relative_to(ROOT)):digest(p) for p in protected}
    worlds=world_snapshot(candidates)
    gate=selected_readiness(fits,decision)
    gate['evidence']={str(f['seed']):{
        'fit':str((REFINED_OUTPUT/str(f['seed'])/'fit.json').relative_to(ROOT)),
        'a1_failures':f['failures'],
        'shared_central_team_clusters':f['central_connective_area']['shared_team_cluster_count'],
        'accepted_lateral_links':[c['id'] for c in f['cross_connections'] if c['lateral']],
        'starter_handoffs':{r['id']:r['starter']['effective_blocks_from_edge'] for r in f['routes']},
        'clean_world':c['local_world_directory'],
    } for f,c in zip(fits,candidates)}
    lines=['# Task B readiness recheck after human selection','',
           'Authority: `specs/mapseedsearchspec3.md`, section 16, and the user’s explicit selection. A.2 human selection is complete: 930010639 primary; 930012642 comparison/fallback. Selection satisfies that gate criterion, not an implicit waiver of the remaining criteria.','',
           '## Remaining evidence requiring resolution','',
           '- **930010639:** the conservative analysis has no shared N/S central cluster and no accepted lateral link. Central interaction and lateral plausibility are therefore not established by the available fit. This is not proof that the actual world lacks them.',
           '- **930012642:** central connectivity is supported, but no accepted lateral link is represented. South-1 hands off at 55.921 effective blocks, outside the prototype 58–76 soft band. This is a review question, not a rigid design rejection or permission to move the terminus.',
           '- **Both:** the gate’s diagnostic-validity acceptance is still unrecorded. Positive analytical evidence remains available for homeland usability, three openings, absence of detected Route collapse and geographic depth.','',
           '## Stage state','',
           'Human selection recorded; readiness gate re-evaluated. Task B.0 has not begun. No physical candidate passes or fails yet; neither is rejected as structurally unsound. No local corrections, skeleton freeze or Task C work occurred. No seed search, refit, world generation or world edits were performed.','',
           'Spec section 16 requires these readiness issues to be resolved before B.0. Do not use B.0 construction to bypass them or silently translate coarse negative evidence into structural failure. Confirm whether the human review accepts the flagged connectivity/handoff conditions as plausible and finds no fundamental diagnostic mismeasurement, or provide the missing review evidence.','',
           '## Artifacts and verification','',
           '`readiness_gate.json` preserves every criterion and records the satisfied selection criterion separately. `verification.json` hashes the selection, A.0/A.1 artifacts, candidate inputs and existing selected worlds before/after this read-only audit. Clean-world paths are recorded in the gate evidence; no new world path is claimed.','',
           'B.1 measurements—including player-scale opening/continuation/interaction/depth tests, construction burden, screenshots, collision clearance, actual water depth and physical crossings—were not performed because the preceding gate remains closed. They are not reported as tooling failures or fabricated results.','']
    if OUT.is_symlink(): raise ValueError('Refuse symlink output directory')
    OUT.mkdir(parents=True,exist_ok=True)
    for name,data in (('readiness_gate.json',encode(gate)),('REPORT.md','\n'.join(lines))):
        target=OUT/name
        if target.is_symlink(): raise ValueError('Refuse symlink output file')
        target.write_text(data)
    if before!={str(p.relative_to(ROOT)):digest(p) for p in protected} or worlds!=world_snapshot(candidates):
        raise RuntimeError('Read-only audit changed a protected input or world')
    verification={'protected_inputs_unchanged':True,'available_selected_worlds_unchanged':True,
                  'protected_input_sha256':before,
                  'world_files_verified':{s:len(v.get('files',{})) for s,v in worlds.items()},
                  'unavailable_worlds':[s for s,v in worlds.items() if not v['available']],
                  'output_sha256':{name:digest(OUT/name) for name in ('readiness_gate.json','REPORT.md')}}
    if (OUT/'verification.json').is_symlink(): raise ValueError('Refuse symlink verification file')
    (OUT/'verification.json').write_text(encode(verification))
    print('Selection recorded; remaining readiness gate unresolved; no Task B world writes.')


if __name__=='__main__': run()
