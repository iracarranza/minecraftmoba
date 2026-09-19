"""Reference harvesting without generating or copying source chunks."""
import hashlib
import json
from pathlib import Path
from .model import make_volume, dumps

RESULTS = Path('implementation/worldgen/results')

def file_ref(path, root):
    return {'path':str(path.resolve().relative_to(root.resolve())), 'sha256':hashlib.sha256(path.read_bytes()).hexdigest()}

def harvest_corpus(root):
    volumes=[]
    for path in sorted((root/RESULTS/'staged_default_2026-09-09/finalists').glob('*/candidate.json')):
        c=json.loads(path.read_text()); seed=c['seed']
        fitpath=root/RESULTS/f'default_opportunity_shadow_2026-09-11/{seed}/fit.json'
        fit=json.loads(fitpath.read_text()) if fitpath.exists() else None
        x0,x1,z0,z1=c['region']['block_bounds']
        provenance={'source_seed':seed,'source_dimension':'minecraft:overworld',
                    'source_bounds':{'x':[x0,x1],'y':[-64,319],'z':[z0,z1]},
                    'minecraft_version':c['minecraft_java_version'],'data_version':c['data_version'],
                    'worldgen_settings':{'status':'UNRESOLVED until source level.dat is read; original generator: '+c['generator']},
                    'source_analysis_record':file_ref(path,root)}
        failures=c.get('stage_c',{}).get('hard_failures',[])
        kind='near_miss_map' if failures else 'whole_map'
        measurements=[{'evidence_state':'DERIVED MEASUREMENT','scope':'original full candidate, not recomputed for clipped sections',
                       'stage_c':c['stage_c'],'metrics':c['metrics'],'logical_orientation':c['orientation'],
                       'note':'logical fitter orientation is not physical block rotation'},
                      {'evidence_state':'INTERPRETATION','finalist_assessment':c['finalist_assessment']},
                      {'evidence_state':'UNRESOLVED','criteria':['physical traversal','homeland resource equivalence','Practical Reach','final Default acceptance']}]
        if fit:
            measurements.append({'evidence_state':'DERIVED MEASUREMENT','source':file_ref(fitpath,root),'status':fit['status'],
                                 'note':'frozen Handoffs, Opportunity Relationships, local/deep networks retained at source; no refit'})
        volumes.append(make_volume(provenance,kind,measurements=measurements,criteria=failures))
        if seed==930010639:
            # Explicit inspection fixtures around documented alpine highland-1 centroid.
            # These are retained section proposals, not a new terrain quality ranking.
            for geometry,kind,r in [('scoop','local_section',32),('ellipse','large_section',128)]:
                p=dict(provenance);p['source_bounds']={'x':[1940-r,1940+r],'y':[-64,319],'z':[364-r,364+r]}
                params={'radius_scale_by_y':[[-64,'1/4'],[0,'3/4'],[48,'1'],[319,'1']]} if geometry=='scoop' else {}
                volumes.append(make_volume(p,kind,geometry,params,measurements=measurements,
                    tags=['western_highland','section_proposal','inspection_fixture']))
    return sorted(volumes,key=lambda v:v['id'])

def reference_candidate(root, candidate_path, bounds, kind, geometry, parameters=None, criteria=None, tags=None):
    """Retain an explicit source selection; no ranking, world access or generation."""
    path=candidate_path.resolve();c=json.loads(path.read_text())
    x0,x1,z0,z1=c['region']['block_bounds']
    if not (x0<=bounds['x'][0]<=bounds['x'][1]<=x1 and z0<=bounds['z'][0]<=bounds['z'][1]<=z1):
        raise ValueError('selection outside candidate source bounds')
    if not (-64<=bounds['y'][0]<=bounds['y'][1]<=319):raise ValueError('source vertical bounds')
    provenance={'source_seed':c['seed'],'source_dimension':'minecraft:overworld','source_bounds':bounds,
        'minecraft_version':c['minecraft_java_version'],'data_version':c['data_version'],
        'worldgen_settings':{'status':'UNRESOLVED until source level.dat is read; original generator: '+c['generator']},
        'source_analysis_record':file_ref(path,root)}
    stage=c.get('stage_c')
    if kind=='whole_map' and (stage is None or 'hard_failures' not in stage or stage['hard_failures']):
        raise ValueError('whole-map retention requires recorded Stage C with no hard failures; use near_miss_map with named criteria')
    return make_volume(provenance,kind,geometry,parameters,
        measurements=[{'evidence_state':'DERIVED MEASUREMENT' if stage else 'UNRESOLVED',
                       'scope':'source candidate only; not recalculated for this selection','stage_c':stage},
                      {'evidence_state':'UNRESOLVED','criteria':['physical traversal','Practical Reach','final Default acceptance']}],
        criteria=criteria,tags=tags)

def write_library(volumes, output):
    if len({v['id'] for v in volumes})!=len(volumes):raise ValueError('duplicate volume reference IDs')
    if output.exists(): raise FileExistsError(output)
    output.mkdir(parents=True)
    for v in volumes: (output/(v['id']+'.json')).write_text(dumps(v))
    index={'schema':'terrain_library/1','volumes':[{'id':v['id'],'seed':v['provenance']['source_seed'],
          'classification':v['classification'],'manifest':v['id']+'.json','status':'reference_only'} for v in sorted(volumes,key=lambda v:v['id'])]}
    (output/'index.json').write_text(json.dumps(index,sort_keys=True,indent=2)+'\n')
    return index
