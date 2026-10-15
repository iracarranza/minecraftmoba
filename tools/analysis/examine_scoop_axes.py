"""Prototype/test centered-axis configurations on preserved generated scoop grids.

No terrain writing and no changes to the installed lab catalog. Equal-radius
Fountain samples force the bisector through the rectangular map center. The
Lair is restricted to one sample spacing from that axis. Defensive sites are
chosen jointly by reflection error while preserving existing ordinal/footprint
constraints. These are analytical proposals, not READY certifications.
"""
import argparse
import itertools
import json
import math
from pathlib import Path
import sys
from unittest.mock import patch

REPO=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(REPO/'implementation/worldgen'))
from vanilla_search.task_a import Terrain, shortest
from vanilla_search import structures
from terrain_harvest import map_compiler as mc, lair_socket, objective_forms


def save(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,indent=2)+'\n')


def socket(t,i):
    x,z=t.xy(i)
    if not(4<=x<t.w-4 and 4<=z<t.h-4):return None
    if t.v['water'][i] or not t.v['buildable'][i] or t.v['canopy'][i] or t.grade[i]>2:return None
    cells=t.cells(i); heights=[t.v['height'][j] for j in cells]
    usable=sum(t.v['buildable'][j] for j in cells)/len(cells)
    canopy=sum(t.v['canopy'][j] for j in cells)/len(cells)
    water=sum(t.v['water'][j] for j in cells)/len(cells)
    quality=usable-.01*(max(heights)-min(heights))-.3*canopy-water
    if quality<mc.PROVISIONAL['min_homeland_developable_fraction']:return None
    coords=[t.coords[j] for j in cells];half=t.s//2
    return {'quality':round(quality,4),'fountain':t.point(i),'world_bounds':[min(p[0] for p in coords)-half,max(p[0] for p in coords)+half-1,min(p[1] for p in coords)-half,max(p[1] for p in coords)+half-1],'diagnostics':{'usable_fraction':usable,'water_fraction':water,'canopy_fraction':canopy,'relief_y':max(heights)-min(heights)}}


def axis_geometry(a,b):
    midpoint=[(a[k]+b[k])/2 for k in range(2)]
    normal=[b[k]-a[k] for k in range(2)];length=math.hypot(*normal)
    normal=[v/length for v in normal]
    def distance(p):return sum((p[k]-midpoint[k])*normal[k] for k in range(2))
    def reflect(p):
        d=distance(p);return [p[k]-2*d*normal[k] for k in range(2)]
    return midpoint,normal,length,distance,reflect


def ordered(combo):
    advance=[c['advance'] for c in combo]
    return all(v>0 for v in advance) and all(a>b for a,b in zip(advance,advance[1:])) and not mc._overlaps(list(objective_forms.DEFENSIVE),list(combo))


def examine(candidate,old,budget):
    t=Terrain(candidate);sockets={i:s for i in range(t.n) if (s:=socket(t,i))}
    pairs=[];dry=[i for i in range(t.n) if not t.v['water'][i]]
    # A bisector through the map center requires equal distances to that
    # center, not antipodal points. Include every socket pair on a shared
    # sampled radius; this also includes horizontal-axis reflection.
    radii={}
    for j,s in sockets.items():
        x,z=t.xy(j)
        if z<.6*t.h:continue
        radius=(2*x-(t.w-1))**2+(2*z-(t.h-1))**2
        radii.setdefault(radius,[]).append(j)
    for i,n in sockets.items():
        x,z=t.xy(i)
        if z>.4*t.h:continue
        radius=(2*x-(t.w-1))**2+(2*z-(t.h-1))**2
        for j in radii.get(radius,[]):
            mid,normal,length,dist,reflect=axis_geometry(t.coords[i],t.coords[j])
            counts=[sum(dist(t.coords[k])<0 for k in dry),sum(dist(t.coords[k])>0 for k in dry)]
            pairs.append({'north':n,'south':sockets[j],'indices':[i,j],'dry_side_counts':counts,'dry_side_disparity':abs(counts[0]-counts[1])/max(1,sum(counts)),'fountain_separation_blocks':length})
    pairs.sort(key=lambda p:(p['dry_side_disparity'],-min(p['north']['quality'],p['south']['quality'])))
    selected=[]
    for p in pairs:
        if any(math.dist(t.coords[p['indices'][0]],t.coords[q['indices'][0]])<48 for q in selected):continue
        selected.append(p)
        if len(selected)>=budget:break
    results=[]
    for number,pair in enumerate(selected):
        a,b=[t.coords[i] for i in pair['indices']];mid,normal,length,dist,reflect=axis_geometry(a,b)
        costs={team:shortest(t.adj,{i:0.})[0] for team,i in zip(['north','south'],pair['indices'])}
        lairs=[]
        for i in range(t.n):
            if abs(dist(t.coords[i]))>t.s:continue
            if any(max(abs(t.xy(i)[k]-t.xy(h)[k]) for k in range(2))<18 for h in pair['indices']):continue
            if lair_socket.viability(t,i,6,3,.55,.75):continue
            n,s=[costs[team].get(i,math.inf) for team in ['north','south']]
            if not math.isfinite(n+s):continue
            disparity=abs(n-s)/((n+s)/2)
            if disparity>mc.PROVISIONAL['max_lair_access_asymmetry']:continue
            lairs.append({'sample':list(t.xy(i)),'world_xz':list(t.coords[i]),'reach':{'north':round(n,2),'south':round(s,2)},'access_asymmetry':round(disparity,4),'axis_distance_blocks':round(abs(dist(t.coords[i])),3)})
        lairs.sort(key=lambda s:(s['access_asymmetry'],s['axis_distance_blocks']))
        item={'pair_rank':number,'pair':pair,'axis':{'midpoint_world_xz':mid,'normal':normal,'rectangle_side_area_fraction':[.5,.5]},'lair':lairs[0] if lairs else None,'status':'analytical-rejection'}
        if not lairs:
            item['rejection']='no sampled viable Lair within one sample of axis and current access bound';results.append(item);continue
        homes={team:pair[team]['fountain']['sample'] for team in ['north','south']}
        with patch.object(structures,'Terrain',return_value=t):
            sited=structures.evaluate(candidate,homes,per_layer=24)
        pools={}
        for kind in objective_forms.DEFENSIVE:
            north=sited['teams']['north'][kind]['candidates'];south=sited['teams']['south'][kind]['candidates']
            options=[]
            for n,s in itertools.product(north,south):
                np=[n['world_xyz'][0],n['world_xyz'][2]];sp=[s['world_xyz'][0],s['world_xyz'][2]]
                if dist(np)>=0 or dist(sp)<=0:continue
                # The Lair and defensive structures need independent loci.
                if any(math.dist(q,lairs[0]['world_xz'])<mc._footprint_radius(kind)+48 for q in [np,sp]):continue
                error=math.dist(reflect(np),sp)
                options.append((error,n,s))
            pools[kind]=sorted(options,key=lambda v:v[0])[:12]
        best=None
        if all(pools.values()):
            for combo in itertools.product(*(pools[k] for k in objective_forms.DEFENSIVE)):
                nc=[v[1] for v in combo];sc=[v[2] for v in combo]
                if not ordered(nc) or not ordered(sc):continue
                if any(math.dist([c['world_xyz'][0],c['world_xyz'][2]],f)<mc._footprint_radius(k)+8 for k,c,f in [(k,n,a) for k,n in zip(objective_forms.DEFENSIVE,nc)]+[(k,s,b) for k,s in zip(objective_forms.DEFENSIVE,sc)]):continue
                score=sum(v[0] for v in combo)
                if best is None or score<best[0]:best=(score,combo)
        if best is None:
            item['rejection']='no paired ordered non-overlapping defensive layout in sampled pools';results.append(item);continue
        item['objectives']={team:{kind:option[index] for kind,option in zip(objective_forms.DEFENSIVE,best[1])} for team,index in [('north',1),('south',2)]}
        item['objective_reflection_error_blocks']={kind:round(option[0],2) for kind,option in zip(objective_forms.DEFENSIVE,best[1])}
        cheaper={'north':0,'south':0,'tie':0}
        for i in dry:
            n,s=(costs[team].get(i,math.inf) for team in ['north','south'])
            cheaper['north' if n<s else 'south' if s<n else 'tie']+=1
        item['dry_samples_cheaper_by_team']=cheaper
        item['status']='analytical-proposal'
        results.append(item)
        print(candidate['seed'],number,item['status'],item['objective_reflection_error_blocks'],flush=True)
    proposals=[r for r in results if r['status']=='analytical-proposal']
    proposals.sort(key=lambda r:(max(r['objective_reflection_error_blocks'].values()),r['pair']['dry_side_disparity'],r['lair']['access_asymmetry']))
    return {'schema':'scoop_axis_examination/1','design_status':'Prototype/test','bounds':candidate['region']['block_bounds'],'eligible_sockets':len(sockets),'centered_pairs':len(pairs),'examined_pairs':len(selected),'experiments':results,'selected':proposals[0] if proposals else None,'original_bindings':old,'limitations':['sampled terrain; no READY certification','one-sample Lair axis tolerance and 48-block loci separation are experiment parameters','equal rectangle halves do not imply equal valuable or traversable geography','defensive reflection errors are ranked, not asserted acceptable']}


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--reports',type=Path,required=True);p.add_argument('--out',type=Path,required=True);p.add_argument('--budget',type=int,default=12);args=p.parse_args()
    catalog=json.loads((args.reports/'catalog.json').read_text());index=[]
    for row in catalog:
        stem=f"{row['seed']}_{row['window_index']}";candidate=json.loads((args.reports/'candidates'/f'{stem}.json').read_text());old=json.loads((args.reports/'compilations'/f'{stem}.json').read_text())['runs'][0]['evidence']['runtime_bindings']
        result=examine(candidate,old,args.budget);result['map_id']=row['map_id'];result['candidate_path']=str((args.reports/'candidates'/f'{stem}.json').resolve());save(args.out/f"{row['map_id']}.json",result);index.append({'map_id':row['map_id'],'selected':result['selected'] is not None,'centered_pairs':result['centered_pairs']})
        print('finished',row['map_id'],index[-1],flush=True)
    save(args.out/'summary.json',index)


if __name__=='__main__':main()
