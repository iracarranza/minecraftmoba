"""Search acceptable Core sites for lower Mansion approach disparity.

Changes siting choices only. Existing gameplay thresholds are unchanged.
The Mansion-specific acceptance threshold remains Open: disparity is ranked,
not assigned a new canonical bound. Natural resource observations are reused
only for the same unchanged generated world and exact block bounds.
"""
from contextlib import ExitStack
import copy
import json
import math
from pathlib import Path
import sys
from unittest.mock import patch

from examine_scoop_axes import Terrain, socket
from filter_pale_gameplay import mc, REPORT, ROOT, save
from report_pale_gameplay import mansion_access
from vanilla_search import task_a
from terrain_harvest import characterize as character_mod


def slots(t,team):
    target=round(t.h*(.18 if team=='north' else .82));sites=[]
    for z in range(max(5,target-t.h//8),min(t.h-5,target+t.h//8+1)):
        for x in range(t.w//5,t.w*4//5):
            i=z*t.w+x;s=socket(t,i)
            if s:sites.append({'index':i,'sample':[x,z],**s})
    sites.sort(key=lambda s:s['quality'],reverse=True)
    chosen=[]
    for site in sites:
        xyz=site['fountain']['world_xyz']
        if any(math.dist([xyz[0],xyz[2]],[q['fountain']['world_xyz'][0],q['fountain']['world_xyz'][2]])<48 for q in chosen):continue
        chosen.append(site)
        if len(chosen)>=16:break
    return chosen,len(sites)


def picker(pair):
    def choose(rows,north,buildable,terrain_fit=False):
        site=pair['north' if north else 'south'];x,z=site['sample']
        return (site['quality'],x,z,site['diagnostics']['usable_fraction'])
    return choose


def examine(folder,max_pairs=4):
    result=json.loads((folder/'result.json').read_text())
    passing=[r for r in result.get('runs',[]) if r['status']=='shared_checks_pass']
    if not passing:return
    root=folder/'core-rebalance';root.mkdir(exist_ok=True)
    all_runs=[]
    for baseline in passing:
        rotation=baseline['rotation'];candidate=json.loads((folder/f'candidate-r{rotation}.json').read_text())
        t=Terrain(candidate)
        sets={};counts={}
        for team in ['north','south']:sets[team],counts[team]=slots(t,team)
        print('Core slots',result['seed'],rotation,counts,flush=True)
        options=[]
        # Fit each legal site through the SAME fitter first, then use its
        # actual Fountain rather than assuming the site centre is the spawn.
        actual={}
        for team in ['north','south']:
            for site in sets[team]:
                dummy={'north':site,'south':site}
                with patch.object(task_a,'_choose_home',side_effect=picker(dummy)):
                    fit=task_a.fit_homeland(t,team)
                if fit is None or fit[0]['fountain'] is None or fit[0]['quality']<mc.PROVISIONAL['min_homeland_developable_fraction']:continue
                actual[team,site['index']]=fit[0]
        # Cache Dijkstra cost per actual Fountain so pairwise ranking does
        # not repeatedly solve the same graph.
        from vanilla_search.task_a import shortest
        mx,mz=result['job']['mansion_start']
        grounds=[i for i,(x,z) in enumerate(t.coords) if 80<=math.hypot(x-mx,z-mz)<=112 and
                 not t.v['water'][i] and t.grade[i]<=8]
        costs={}
        for key,home in actual.items():
            x,z=home['fountain']['sample'];i=z*t.w+x
            distances=shortest(t.adj,{i:0.})[0]
            costs[key]=min(distances.get(j,math.inf) for j in grounds)
        for n in sets['north']:
            for s in sets['south']:
                nk,sk=('north',n['index']),('south',s['index'])
                if nk not in costs or sk not in costs:continue
                a,b=costs[nk],costs[sk]
                options.append({'north':n,'south':s,'mansion_effective_block_cost':{'north':a,'south':b},
                    'mansion_disparity':abs(a-b)/((a+b)/2),
                    'actual_home_quality':{team:actual[team,q['index']]['quality'] for team,q in [('north',n),('south',s)]}})
        options.sort(key=lambda p:(p['mansion_disparity'],-min(p['actual_home_quality'].values())))
        selected=[]
        for pair in options:
            if any(pair['north']['index']==p['north']['index'] and pair['south']['index']==p['south']['index'] for p in selected):continue
            selected.append(pair)
            if len(selected)>=max_pairs:break
        save(root/f'pair-search-r{rotation}.json',{'socket_counts':counts,'candidate_pairs':len(options),
            'selected_pairs':selected,'status':'Prototype/test placement search; no Mansion disparity gate'})
        measured=baseline['evidence'].get('characterization')
        for index,pair in enumerate(selected,1):
            path=root/f'compilation-r{rotation}-p{index}.json'
            if path.exists():
                cached=json.loads(path.read_text());all_runs.append(cached)
                if cached['status']=='shared_checks_pass':break
                continue
            with ExitStack() as stack:
                stack.enter_context(patch.object(task_a,'_choose_home',side_effect=picker(pair)))
                if measured and measured.get('measured'):
                    def reuse(seed,world,bounds,**kwargs):
                        assert seed==result['seed'] and list(bounds)==candidate['region']['block_bounds']
                        assert Path(world).resolve()==Path(result['world']).resolve()
                        return copy.deepcopy(measured)
                    stack.enter_context(patch.object(character_mod,'characterize',side_effect=reuse))
                out=mc.compile_candidate(candidate,Path(result['world']))
            record=out.as_dict();record.update(rotation=rotation,core_pair_variant=index,
                status='shared_checks_pass' if not out.rejections else 'reject',
                ready=False,playable=False,qualification_only=True,
                actual_symmetry=baseline['actual_symmetry'],core_pair=pair,
                characterization_reused=bool(measured and measured.get('measured')))
            record['mansion_access']=mansion_access(candidate,record,result['job'])
            save(path,record);all_runs.append(record)
            print('Core variant',result['seed'],rotation,index,record['status'],
                  record['mansion_access'].get('normalized_disparity'),[r.code for r in out.rejections],flush=True)
            # A first pass with the lowest predicted disparity is enough.
            # Other pairs remain available if an actual check invalidates it.
            if record['status']=='shared_checks_pass':break
    passed=[r for r in all_runs if r['status']=='shared_checks_pass']
    chosen=min(passed,key=lambda r:r['mansion_access'].get('normalized_disparity',math.inf)) if passed else None
    save(root/'summary.json',{'seed':result['seed'],'original_mansion_access':[
        mansion_access(json.loads((folder/f'candidate-r{r["rotation"]}.json').read_text()),r,result['job']) for r in passing],
        'checked_variants':len(all_runs),'selected':chosen,
        'same_shared_thresholds':True,'Mansion_specific_bound':'Open; disparity ranked without a new acceptance cut',
        'notes':'Natural world unchanged; legal bands and original Core quality formula retained; all shared checks rerun for each tested pair.'})


def main():
    folders=[]
    for path in (REPORT/'attempts').glob('*/result.json'):
        result=json.loads(path.read_text())
        if result['shared_gameplay_pass']:folders.append((result['job']['forest']['area_proxy_blocks2'],path.parent))
    for _,folder in sorted(folders,reverse=True)[:3]:examine(folder)


if __name__=='__main__':main()
