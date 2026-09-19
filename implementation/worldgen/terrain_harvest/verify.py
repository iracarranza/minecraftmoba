"""Independent sparse block-state readback and envelope audit of a built gallery.

This is not a client walkthrough or an exhaustive block comparison. All region
hashes are recorded; deterministic probes include deep lateral scoop edges.
"""
import json
from pathlib import Path
from serialization.region import read_region
from serialization.nbt import plain
from vanilla_search.extract import _palette_value
from .model import loads,Mask
from .materialize import sha,json_write

class Reader:
    def __init__(self,path):self.path=path;self.region=None;self.chunks={}
    def state(self,x,y,z):
        region=(x//512,z//512)
        if self.region!=region:
            p=self.path/'region'/f'r.{region[0]}.{region[1]}.mca'
            self.chunks={(cx,cz):{s['Y']:s for s in plain(root)['sections']} for cx,cz,_,root in read_region(p)} if p.exists() else {}
            self.region=region
        if (x//16,z//16) not in self.chunks:return None
        section=self.chunks[(x//16,z//16)].get(y//16)
        if section is None or 'block_states' not in section:return {'Name':'minecraft:air'}
        return _palette_value(section['block_states'],(y&15)*256+(z&15)*16+(x&15),4)

def audit(gallery,sources,report):
    info=json.loads((gallery/'gallery.json').read_text());results=[]
    for record in info['volumes']:
        dest=gallery/'dimensions/harvest'/record['volume_id'];v=loads((dest/'terrain_volume.json').read_text());m=Mask(v);b=m.bounds
        src=sources[v['provenance']['source_seed']];a=Reader(src);o=Reader(dest)
        unchanged=all(sha(src/f)==h for f,h in record['source_region_sha256'].items()) and sha(src/'level.dat')==record['source_level_sha256']
        probes=set()
        xs={b['x'][0],b['x'][0]+1,sum(b['x'])//2,b['x'][1]-1,b['x'][1]};zs={b['z'][0],sum(b['z'])//2,b['z'][1]}
        ys={b['y'][0]-1,b['y'][0],-48,-16,0,32,48,63,80,128,192,b['y'][1],b['y'][1]+1}
        for x in xs:
            for z in zs:
                for y in ys:
                    for dx,dz in [(0,0),(-1,0),(1,0),(0,-1),(0,1)]:probes.add((x+dx,y,z+dz))
        # All chunks get an interior geological/surface/sky probe, including ores/fluids by exact state where encountered.
        for x in range(b['x'][0],b['x'][1]+1,16):
            for z in range(b['z'][0],b['z'][1]+1,16):
                for y in (-32,0,63,96,200):probes.add((x,y,z))
        counts={'source_equal':0,'shell':0,'outside_air':0};failures=[]
        for x,y,z in sorted(probes,key=lambda p:(p[0]//512,p[2]//512,p)):
            actual=o.state(x,y,z)
            if m.include_block(x,y,z):expected=a.state(x,y,z);key='source_equal'
            elif m.envelope(x,y,z):expected={'Name':'minecraft:barrier' if y>b['y'][1] else 'minecraft:bedrock'};key='shell'
            else:expected={'Name':'minecraft:air'};key='outside_air'
            # Outside written chunks is a void generator, separately checked below.
            if actual is None and key=='outside_air':actual=expected
            counts[key]+=1
            if actual!=expected:failures.append({'pos':[x,y,z],'expected':expected,'actual':actual})
        definition=json.loads((gallery/f'datapacks/terrain_gallery/data/harvest/dimension/{v["id"]}.json').read_text())
        void=definition['generator']['settings']['layers']==[]
        results.append({'id':v['id'],'probes':counts,'source_unchanged':unchanged,'void_generator':void,'failures':failures[:20],
                        'pass':unchanged and void and not failures})
    result={'schema':'terrain_gallery_audit/1','volumes':results,'pass':all(r['pass'] for r in results),
            'client_inspection':'NOT PERFORMED','world_file_sha256':{str(p.relative_to(gallery)):sha(p) for p in sorted(gallery.rglob('*')) if p.is_file()}}
    json_write(report,result)
    if not result['pass']:raise RuntimeError('gallery audit failed')
    return result
