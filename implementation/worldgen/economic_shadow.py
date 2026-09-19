"""Read-only SHADOW revalidation; no fitter, selection or world-writing imports."""
from __future__ import annotations
import argparse
from collections import Counter, defaultdict
import hashlib
import json
from pathlib import Path
import re
import subprocess
from serialization.nbt import load_gzip, plain
from serialization.region import read_region
from vanilla_search.extract import VanillaChunk

SEEDS = (930010639, 930012642)
MATERIALS = ('coal', 'copper', 'iron', 'gold', 'redstone', 'lapis', 'diamond', 'emerald')
BASE = 'implementation/worldgen/results/'

def git(root, *args):
    return subprocess.check_output(['git', '-C', str(root), *args], text=True).strip()

def identity(root):
    remote = git(root, 'remote', 'get-url', 'origin')
    if not re.fullmatch(r'(https://github.com/|git@github.com:)iracarranza/minecraftmoba(?:\.git)?/?', remote):
        raise ValueError('STOP: repository identity is not iracarranza/minecraftmoba')
    if Path(git(root, 'rev-parse', '--show-toplevel')).resolve() != root.resolve():
        raise ValueError('STOP: root is not repository top level')
    return {'remote': remote, 'branch': git(root, 'branch', '--show-current'),
            'starting_commit': git(root, 'rev-parse', 'HEAD'),
            'reference_commit': git(root, 'rev-parse', 'origin/main')}

def digest(data):
    return hashlib.sha256(data).hexdigest()

def inventory(root):
    ident = identity(root)  # MUST precede corpus discovery.
    paths = set(git(root, 'ls-files', '--cached', '--others', '--exclude-standard').splitlines())
    refs = set(git(root, 'ls-tree', '-r', '--name-only', ident['reference_commit']).splitlines())
    evidence = []
    for p in sorted(paths | refs):
        if p.startswith('implementation/worldgen/reports/economic_shadow_'): continue
        if not (p.endswith(('.md', '.py', '.json', '.jsonl', '.txt')) and
                (p in ('maps.md','objectives.md','infrastructure.md','classes.md','AGENTS.md') or
                 p.startswith(('docs/', 'specs/', 'implementation/worldgen/')))):
            continue
        local = root / p
        entry = {'path': p, 'worktree_sha256': digest(local.read_bytes()) if local.is_file() else None}
        if p in refs:
            data = subprocess.check_output(['git','-C',str(root),'show',ident['reference_commit']+':'+p])
            entry['reference_sha256'] = digest(data)
        evidence.append(entry)
    return {'identity': ident, 'files': evidence,
            'scope': 'Tracked working tree and pinned local origin/main; no network freshness claim. Local edits retained.'}

def ore(name):
    name = name.removeprefix('minecraft:').removeprefix('deepslate_')
    return name[:-4] if name.endswith('_ore') and name[:-4] in MATERIALS else None

def summarize(points, bounds):
    """Six-face same-material components, clipped at inclusive cuboid boundary."""
    x0,x1,y0,y1,z0,z1 = bounds
    groups = defaultdict(set)
    for x,y,z,name in points:
        groups[ore(name)].add((x,y,z))
    result = {}
    for material in MATERIALS:
        pending = set(groups[material]); sizes=[]; clipped=0
        while pending:
            stack=[pending.pop()]; size=0; boundary=False
            while stack:
                x,y,z=stack.pop(); size+=1
                boundary |= x in (x0,x1) or y in (y0,y1) or z in (z0,z1)
                for q in ((x-1,y,z),(x+1,y,z),(x,y-1,z),(x,y+1,z),(x,y,z-1),(x,y,z+1)):
                    if q in pending: pending.remove(q); stack.append(q)
            sizes.append(size); clipped+=boundary
        result[material]={'blocks':len(groups[material]), 'six_face_components':len(sizes),
                          'largest_component_blocks':max(sizes,default=0), 'boundary_components':clipped,
                          'largest_component_fraction_of_observed_stock':max(sizes,default=0)/len(groups[material]) if groups[material] else None,
                          'blocks_by_32_y_band':dict(sorted(Counter((q[1]//32)*32 for q in groups[material]).items())),
                          'y_range':[min(q[1] for q in groups[material]),max(q[1] for q in groups[material])] if groups[material] else None}
    return result

def load_chunks(world, required):
    chunks={}; hashes={}
    for rx,rz in sorted({(cx//32,cz//32) for cx,cz in required}):
        path=world/'region'/f'r.{rx}.{rz}.mca'
        if not path.exists(): continue
        before=digest(path.read_bytes())
        for cx,cz,_,root in read_region(path):
            if (cx,cz) not in required: continue
            data=plain(root)
            if data.get('Status') == 'minecraft:full': chunks[cx,cz]=VanillaChunk(data)
        if digest(path.read_bytes()) != before: raise ValueError('world changed during read')
        hashes[str(path)]=before
    if set(chunks) != required: raise ValueError('UNRESOLVED: missing or non-full bounded chunks')
    return chunks,hashes

def bounded_box(anchor, region):
    x,y,z=anchor; x0,x1,z0,z1=region
    return [max(x0,x-32),min(x1,x+32),-64,319,max(z0,z-32),min(z1,z+32)]

def observe(chunks, bounds):
    x0,x1,y0,y1,z0,z1=bounds; points=[]
    for (cx,cz),chunk in sorted(chunks.items()):
        if cx*16>x1 or cx*16+15<x0 or cz*16>z1 or cz*16+15<z0: continue
        for sy,section in sorted(chunk.sections.items()):
            if sy*16>y1 or sy*16+15<y0: continue
            if not any(ore(v['Name']) for v in section.get('block_states',{}).get('palette',[])): continue
            for y in range(max(y0,sy*16), min(y1,sy*16+15)+1):
                for z in range(max(z0,cz*16),min(z1,cz*16+15)+1):
                    for x in range(max(x0,cx*16),min(x1,cx*16+15)+1):
                        name=chunk.block(x,y,z)
                        if ore(name): points.append([x,y,z,name])
    return sorted(points)

def seed_analysis(root, seed, output):
    cp=f'{BASE}staged_default_2026-09-09/finalists/{seed}/candidate.json'
    fp=f'{BASE}default_opportunity_shadow_2026-09-11/{seed}/fit.json'
    missing=[p for p in (cp,fp) if not (root/p).is_file()]
    if missing: return {'seed':seed,'measurement_status':'UNRESOLVED','missing_inputs':missing,'handoffs':[]}
    c=json.loads((root/cp).read_text()); f=json.loads((root/fp).read_text())
    if c['seed'] != seed or f['seed'] != seed: raise ValueError('seed mismatch')
    result={'seed':seed,'candidate_source':cp,'fit_source':fp,'sampled_geography':c['metrics'],
            'section_palette_presence_not_resource_counts':c['actual_block_volume_presence'],
            'opening_opportunity_relationships':f['opportunity_shadow'], 'handoffs':[]}
    world=Path(c['local_world_directory'])
    if not world.is_relative_to(root/'artifacts'): raise ValueError('world outside project artifacts')
    boxes=[]; required=set()
    for team in ('north','south'):
        for i,h in enumerate(f['selected_handoffs'][team],1):
            x,y,z=h['anchor']['world_xyz']; bounds=bounded_box((x,y,z),c['region']['block_bounds'])
            boxes.append((team[0].upper()+str(i),h,bounds))
            required.update((cx,cz) for cx in range(bounds[0]//16,bounds[1]//16+1) for cz in range(bounds[4]//16,bounds[5]//16+1))
    try:
        levelpath=world/'level.dat'; levelhash=digest(levelpath.read_bytes())
        level=plain(load_gzip(levelpath)[1])['Data']
        if level['WorldGenSettings']['seed'] != seed: raise ValueError('world seed mismatch')
        result['world']={'path':str(world),'level_sha256':levelhash,'seed_verified':seed,
                         'version':level.get('Version'), 'provenance':'Existing inspection world; current snapshot, pristine generation not independently proven'}
        chunks,hashes=load_chunks(world,required); result['world']['region_sha256']=hashes
        result['world']['full_chunks_read']=len(chunks)
        for slot,h,bounds in boxes:
            points=observe(chunks,bounds)
            raw=output/f'{seed}-{slot}-ore-observations.json'
            raw.write_text(json.dumps({'bounds_inclusive':bounds,'columns':['x','y','z','block'],'observations':points},separators=(',',':'))+'\n')
            stats=summarize(points,bounds)
            result['handoffs'].append({'slot':slot,'id':h['id'],'anchor':h['anchor'],
                'homeland_depth':h['homeland_depth'],'travel_cost':h.get('travel_cost'),
                'opportunity_relationship':h['opportunity_relationship'],
                'raw_observations':raw.name,'raw_sha256':digest(raw.read_bytes()),
                'bounds_inclusive':bounds,'resource_counts':stats,
                'ore_material_breadth':sum(v['blocks']>0 for v in stats.values()),
                'classification':'Derived geometry of finite ore stock in sampled cuboid; no access or mining yield assertion'})
        for path,expected in hashes.items():
            if digest(Path(path).read_bytes()) != expected: raise ValueError('world changed during analysis')
        if digest(levelpath.read_bytes()) != levelhash: raise ValueError('level changed during analysis')
    except (FileNotFoundError, ValueError, KeyError) as error:
        result['measurement_status']='UNRESOLVED'; result['reason']=str(error)
        result['handoffs']=[]
    else: result['measurement_status']='OBSERVED_CURRENT_SNAPSHOT'
    result['unresolved']={k:'UNRESOLVED' for k in ('accessible_resource_field','mining_time','depletion_over_time','food_and_fuel_portfolio','crop_and_animal_stock','inventory_and_equipment','processing_and_return','threat_and_coordination','UAU_flow','Practical_Reach','recognition_effect','integrated_system_effect')}
    return result

def main():
    parser=argparse.ArgumentParser(); parser.add_argument('--root',type=Path,default=Path(__file__).resolve().parents[2]);parser.add_argument('--output',type=Path,required=True);args=parser.parse_args()
    inv=inventory(args.root)
    args.output.mkdir(parents=True,exist_ok=True)
    (args.output/'inventory.json').write_text(json.dumps(inv,indent=2,sort_keys=True)+'\n')
    result={'status':'SHADOW / Prototype-test','states':{state:'UNRESOLVED: no measured undertaking/character/stock state' for state in ('opening','informed','organized','recognized','mature')},'seeds':[seed_analysis(args.root,s,args.output) for s in SEEDS]}
    (args.output/'measurements.json').write_text(json.dumps(result,indent=2,sort_keys=True)+'\n')

if __name__=='__main__': main()
