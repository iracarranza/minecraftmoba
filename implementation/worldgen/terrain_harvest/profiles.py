"""Read-only cardinal edge transects; observations, not inferred connection ports."""
import argparse
import json
from pathlib import Path
from .model import Mask,loads,digest
from .verify import Reader
from .materialize import sha,json_write


def edge_points(volume, step):
    if type(step) is not int or step<1:raise ValueError('positive integer vertical sampling step required')
    mask=Mask(volume);b=mask.bounds;cx=sum(b['x'])//2;cz=sum(b['z'])//2
    ys=sorted(set(range(b['y'][0],b['y'][1]+1,step))|{b['y'][1]})
    for y in ys:
        for name,dx,dz,limit in [('west',-1,0,cx-b['x'][0]),('east',1,0,b['x'][1]-cx),('north',0,-1,cz-b['z'][0]),('south',0,1,b['z'][1]-cz)]:
            points=[(cx+dx*i,y,cz+dz*i) for i in range(limit+1) if mask.include_block(cx+dx*i,y,cz+dz*i)]
            if not points:raise ValueError('no included cell on cardinal transect')
            yield name,points[-1]


def observe(volume, step, read_state):
    samples=[]
    for edge,pos in sorted(edge_points(volume,step), key=lambda item:(item[1][0]//512,item[1][2]//512,item[0],item[1])):
        state=read_state(*pos)
        if state is None:raise ValueError(f'missing source observation at {pos}')
        samples.append({'edge':edge,'source_position':list(pos),'block_state':state})
    return {'schema':'terrain_boundary_profile/1','volume_id':volume['id'],
        'evidence_state':'RAW WORLD OBSERVATION','boundary':volume['boundary'],
        'source_bounds':volume['provenance']['source_bounds'],
        'sampling':{'kind':'four cardinal vertical transects through integer X/Z midpoint',
                    'vertical_step_blocks':step,'includes_top_and_bottom':True,
                    'status':'ANALYTICAL FIXTURE; sparse sampling, not full boundary coverage'},
        'samples':samples,'connection_interfaces':[],
        'unresolved':['solid collision shape','cave connectivity','fluid depth and navigability',
                      'port width','usable boundary segment','seam compatibility','Practical Reach']}


def profile_gallery(gallery,sources,step,output):
    if output.exists():raise FileExistsError(output)
    manifest_path=gallery/'gallery.json';manifest=json.loads(manifest_path.read_text())
    profiles=[]
    for record in manifest['volumes']:
        path=gallery/'dimensions/harvest'/record['volume_id']/'terrain_volume.json'
        volume=loads(path.read_text());source=sources[volume['provenance']['source_seed']]
        pinned=dict(record['source_region_sha256']);pinned['level.dat']=record['source_level_sha256']
        if any(sha(source/f)!=h for f,h in pinned.items()):raise ValueError('source differs from pinned materialization snapshot')
        # The exporter already checked all chunks intersecting these bounds were
        # full and version-matched. Require its exact snapshot, never read shell
        # blocks or silently regenerate unknown source state.
        profile=observe(volume,step,Reader(source).state)
        if any(sha(source/f)!=h for f,h in pinned.items()):raise ValueError('source changed during observation')
        profile['provenance']={'volume_manifest_sha256':sha(path),'gallery_manifest_sha256':sha(manifest_path),
                               'source_seed':volume['provenance']['source_seed'],
                               'source_dimension':volume['provenance']['source_dimension'],
                               'minecraft_version':volume['provenance']['minecraft_version'],
                               'source_files_sha256':pinned}
        profile['id']='profile_'+digest(profile)[:24];profiles.append(profile)
    result={'schema':'terrain_boundary_profiles/1','profiles':profiles,
            'selection_note':'existing proof volumes are technical fixtures, not chosen composition partners',
            'source_preservation':'all pinned source hashes checked before and after each profile'}
    json_write(output,result)
    return result

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--gallery',type=Path,required=True)
    p.add_argument('--source',action='append',required=True,metavar='SEED=WORLD');p.add_argument('--vertical-step',type=int,default=16);p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();sources={int(s.split('=',1)[0]):Path(s.split('=',1)[1]) for s in a.source}
    result=profile_gallery(a.gallery,sources,a.vertical_step,a.output)
    print(f"Observed {sum(len(p['samples']) for p in result['profiles'])} natural-source boundary cells")
