#!/usr/bin/env python3
"""Compare every Overworld block-state section against the clean source worlds."""
import hashlib
import json
from collections import Counter

from build_task_b_greyboxes import OUT
from fit_default_task_a import encode
from serialization.nbt import dumps,plain
from serialization.region import read_region
from vanilla_search.extract import _palette_value
from vanilla_search.task_b import state_string
from pathlib import Path


def sections(root):
    return {s.value['Y'].value:s.value.get('block_states') for s in root.value['sections'].value}


def section_hashes(root):
    return {y:hashlib.sha256(dumps('',v)).hexdigest() if v else None for y,v in sections(root).items()}


def audit(seed):
    directory=OUT/str(seed)
    v=json.loads((directory/'validation.json').read_text()); plan=json.loads((directory/'build_plan.json').read_text())
    expected={tuple(c['xyz']):c['after'] for c in plan['changes']}
    source=Path(v['source_world_path']); world=Path(v['world_path']); original={}; targets={}; actual_chunks=set()
    for file in sorted((source/'region').glob('*.mca')):
        for cx,cz,_,root in read_region(file): original[cx,cz]=section_hashes(root)
    for file in sorted((world/'region').glob('*.mca')):
        for cx,cz,_,root in read_region(file):
            actual_chunks.add((cx,cz))
            if original.get((cx,cz))!=section_hashes(root): targets[cx,cz]=sections(root)
    changed=0; unexpected=[]; classes=Counter(); changed_chunks=set()
    for file in sorted((source/'region').glob('*.mca')):
        for cx,cz,_,root in read_region(file):
            if (cx,cz) not in targets: continue
            before=sections(root); after=targets[cx,cz]
            for sy in sorted(set(before)|set(after)):
                a=before.get(sy); b=after.get(sy)
                if a==b: continue
                aa=plain(a) if a else {'palette':[{'Name':'minecraft:air'}]}
                bb=plain(b) if b else {'palette':[{'Name':'minecraft:air'}]}
                for i in range(4096):
                    old=state_string(_palette_value(aa,i,4)); new=state_string(_palette_value(bb,i,4))
                    if old==new: continue
                    point=(cx*16+(i&15),sy*16+i//256,cz*16+(i//16&15))
                    changed+=1; changed_chunks.add((cx,cz)); classes[old+' -> '+new]+=1
                    if expected.get(point)!=new: unexpected.append({'xyz':list(point),'before':old,'after':new,'planned':expected.get(point)})
    def transient(item):
        before=item['before']; after=item['after']
        return ('distance=' in before and 'distance=' in after) or 'bubble_column' in after or 'water[level=' in after or 'lava[level=' in after
    transient_changes=[x for x in unexpected if transient(x)]
    authoring_discrepancies=[x for x in unexpected if not transient(x)]
    result={'seed':seed,'scope':'all existing Overworld chunk block-state sections; entity/time/lighting metadata is not terrain',
            'source_chunks':len(original),'physical_chunks':len(actual_chunks),
            'new_chunks':sorted(actual_chunks-set(original)),'removed_chunks':sorted(set(original)-actual_chunks),
            'changed_block_states':changed,'planned_block_edits':len(expected),'changed_chunks':len(changed_chunks),
            'unexpected_block_changes':unexpected,'transient_server_simulation_changes':transient_changes,
            'authoring_discrepancies':authoring_discrepancies,'block_state_changes':dict(sorted(classes.items())),
            'pass':not authoring_discrepancies and actual_chunks==set(original),
            'pass_scope':'authored-state preservation; server fluid/tick/leaf metadata changes are reported separately'}
    (directory/'preservation_audit.json').write_text(encode(result))
    print(f'{seed}: all-chunk audit: {changed} changed blocks; {len(unexpected)} outside expected final states',flush=True)


if __name__=='__main__':
    for seed in (930010639,930012642): audit(seed)
