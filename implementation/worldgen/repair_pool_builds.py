"""Recover mispublished stockrun worlds, retaining original worlds and manifests."""
import argparse
import json
import shutil
from pathlib import Path
from terrain_harvest.column_scan import World
from terrain_harvest.foundry import _fingerprint

def verifies(path, manifest):
    world = World(path)
    fountains = manifest['runtime_bindings']['fountains']
    for x,y,z in fountains.values():
        for dy in range(4):
            if world.block(x,y+dy,z) != 'minecraft:chiseled_quartz_block': return False
        if world.block(x,y+4,z) != 'minecraft:water': return False
    for sites in manifest['runtime_bindings']['objectives'].values():
        for x,z in sites.values():
            if world.chunk(x//16,z//16) is None: return False
    return True

def run(pool, builds, apply=False):
    results=[]
    for file in sorted(pool.glob('*/map.json')):
        manifest=json.loads(file.read_text()); seed=manifest['provenance']['seed']
        if (file.parent/'world.before-authored-repair').exists():
            results.append({'map':file.parent.name,'status':'already backed up'}); continue
        candidates=list(builds.glob(f'*/build-{seed}'))
        valid=[p for p in candidates if verifies(p,manifest)]
        if len(valid)!=1:
            results.append({'map':file.parent.name,'status':'BLOCKED','matching_builds':[str(p) for p in valid]}); continue
        target=file.parent/'world'; backup=file.parent/'world.before-authored-repair'
        if backup.exists():
            results.append({'map':file.parent.name,'status':'already backed up'}); continue
        if apply:
            staged=file.parent/'world.authored-staging'
            shutil.copytree(valid[0],staged)
            if not verifies(staged,manifest): raise RuntimeError('staged verification failed')
            shutil.copy2(file,file.parent/'map.before-authored-repair.json')
            target.rename(backup); staged.rename(target)
            manifest['world_fingerprint']=_fingerprint(target)
            manifest['repair']={'reason':'published generated source instead of authored build','source':str(valid[0]),'verification':'both fountain plinths and source water; objective chunks present'}
            file.write_text(json.dumps(manifest,indent=1))
        results.append({'map':file.parent.name,'status':'REPAIRED' if apply else 'MATCHED','source':str(valid[0])})
        print(json.dumps(results[-1]),flush=True)
    return results

if __name__=='__main__':
    p=argparse.ArgumentParser(); p.add_argument('--pool',type=Path,required=True); p.add_argument('--builds',type=Path,required=True); p.add_argument('--apply',action='store_true')
    a=p.parse_args(); print(json.dumps(run(a.pool,a.builds,a.apply),indent=2))
