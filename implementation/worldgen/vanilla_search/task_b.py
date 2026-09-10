"""Minimal vanilla-command greybox and actual-block validation. No seed search.

Reads existing full Anvil chunks with the established reader. Never interpolates
terrain or constructs anything beyond the supported Starter portions/markers.
"""
import math
import heapq
from collections import Counter
from pathlib import Path

from serialization.nbt import plain
from serialization.region import read_region
from .extract import VanillaChunk, _palette_value

AIR={'minecraft:air','minecraft:cave_air','minecraft:void_air'}
VEGETATION={'snow','vine','short_grass','tall_grass','fern','large_fern','dead_bush',
            'moss_carpet','pale_moss_carpet','hanging_roots','seagrass','tall_seagrass'}
PARAMETERS={'path_half_width':1,'head_clearance':3,'bridge_max_depth':3,
            'bridge_max_run':12,'boundary_marker_spacing':12,'maximum_edits':12000}


def vegetation(name):
    name=name.removeprefix('minecraft:')
    return name in VEGETATION or any(v in name for v in ('leaves','_log','_wood','sapling','flower','bamboo','mushroom','azalea'))


def state_string(state):
    props=state.get('Properties',{})
    return state['Name']+('['+','.join(f'{k}={v}' for k,v in sorted(props.items()))+']' if props else '')


def dense_path(path):
    """Cardinally connected reference spine through existing X/Z anchors."""
    result=[]
    for a,b in zip(path,path[1:]):
        x0,z0=a[0],a[2]; x1,z1=b[0],b[2]
        n=max(abs(x1-x0),abs(z1-z0),1)
        for k in range(n+1):
            point=(round(x0+(x1-x0)*k/n),round(z0+(z1-z0)*k/n))
            if result and point==result[-1]: continue
            if result and point[0]!=result[-1][0] and point[1]!=result[-1][1]: result.append((point[0],result[-1][1]))
            result.append(point)
    if not result and path: result=[(path[0][0],path[0][2])]
    return result


class BlockWorld:
    def __init__(self,world,columns):
        self.world=Path(world); self.chunks={}; self.cache={}
        wanted={(x//16,z//16) for x,z in columns}
        regions={(cx//32,cz//32) for cx,cz in wanted}
        for rx,rz in sorted(regions):
            path=self.world/'region'/f'r.{rx}.{rz}.mca'
            if not path.exists(): raise ValueError(f'missing existing region {path}')
            for cx,cz,_,root in read_region(path):
                if (cx,cz) not in wanted: continue
                data=plain(root)
                if data['Status']!='minecraft:full': raise ValueError(f'non-full chunk {cx},{cz}')
                if data['DataVersion']!=4671: raise ValueError('Expected established Java 1.21.11 world')
                self.chunks[cx,cz]=VanillaChunk(data)
        if wanted-set(self.chunks): raise ValueError('Missing existing chunks; generation is prohibited')

    def state(self,x,y,z):
        if (x//16,z//16) not in self.chunks: raise ValueError('Unloaded column is not air')
        section=self.chunks[x//16,z//16].sections.get(y//16)
        if not section or 'block_states' not in section: return {'Name':'minecraft:air'}
        return _palette_value(section['block_states'],(y&15)*256+(z&15)*16+(x&15),4)

    def column(self,x,z):
        if (x,z) in self.cache: return self.cache[x,z]
        chunk=self.chunks[x//16,z//16]
        top=chunk.height('WORLD_SURFACE',x,z)
        y=top; overhead=[]
        while y>-64:
            name=self.state(x,y,z)['Name']
            if name in AIR or vegetation(name):
                if name not in AIR: overhead.append({'y':y,'block':name})
                y-=1
            else: break
        water_y=y if self.state(x,y,z)['Name']=='minecraft:water' else None
        if water_y is not None:
            while y>-64 and (self.state(x,y,z)['Name']=='minecraft:water' or self.state(x,y,z)['Name'].endswith(('seagrass','kelp','kelp_plant'))): y-=1
        record={'x':x,'z':z,'ground_y':y,'surface_y':top,'water_y':water_y,
                'water_depth':water_y-y if water_y is not None else 0,
                'substrate':self.state(x,y,z)['Name'],'overhead':overhead}
        self.cache[x,z]=record
        return record


def required_columns(fit):
    points=set()
    for r in fit['routes']:
        for x,z in dense_path(r['world_path']):
            points.update((x+dx,z+dz) for dx in range(-3,4) for dz in range(-3,4))
    for c in fit['cross_connections']:
        points.update(dense_path(c['world_path']))
    for home in fit['homelands'].values():
        x0,x1,z0,z1=home['world_bounds']
        points.update((x,z) for x in range(x0-3,x1+4) for z in range(z0-3,z1+4))
    return points


def validation_columns(fit,plan):
    points=required_columns(fit)
    for route in plan['routes']:
        if route['source_block_profile']['maximum_adjacent_rise']>1:
            points.update((x+dx,z+dz) for x,z in route['center_xz'] for dx in range(-20,21) for dz in range(-20,21))
    return points


def profile(reader,path):
    cols=[reader.column(x,z) for x,z in path]
    cliffs=[]; water_runs=[]; run=[]
    for index,c in enumerate(cols):
        if index and abs(c['ground_y']-cols[index-1]['ground_y'])>1:
            cliffs.append({'from':[cols[index-1]['x'],cols[index-1]['ground_y'],cols[index-1]['z']],
                           'to':[c['x'],c['ground_y'],c['z']],'rise':c['ground_y']-cols[index-1]['ground_y']})
        if c['water_y'] is not None: run.append(c)
        elif run:
            water_runs.append({'columns':len(run),'max_depth':max(v['water_depth'] for v in run),'start':[run[0]['x'],run[0]['z']]}); run=[]
    if run: water_runs.append({'columns':len(run),'max_depth':max(v['water_depth'] for v in run),'start':[run[0]['x'],run[0]['z']]})
    return {'columns':len(cols),'height_range':[min(c['ground_y'] for c in cols),max(c['ground_y'] for c in cols)],
            'adjacent_rises_over_one':cliffs,'maximum_adjacent_rise':max((abs(a['ground_y']-b['ground_y']) for a,b in zip(cols,cols[1:])),default=0),
            'water_runs':water_runs,'vegetated_columns':sum(bool(c['overhead']) for c in cols)}


def plan_greybox(reader,fit):
    mods={}; skipped=[]; route_records=[]; protected=set()
    def place(x,y,z,name,reason):
        if not -64<=y<320: raise ValueError('Vertical write outside Overworld')
        old=reader.state(x,y,z)
        if state_string(old)==name: return
        key=(x,y,z)
        if key in mods: mods[key]['reasons']=sorted(set(mods[key]['reasons']+[reason])); return
        mods[key]={'xyz':list(key),'before':old,'after':name,'reasons':[reason]}
    def marker(x,z,color,reason):
        if (x,z) in protected:
            choices=[(x+2,z),(x-2,z),(x,z+2),(x,z-2),(x+3,z),(x-3,z),(x,z+3),(x,z-3)]
            point=next((q for q in choices if q not in protected),None)
            if point is None: skipped.append({'reason':'marker has no free shoulder','marker':reason}); return
            x,z=point
        c=reader.column(x,z)
        if c['water_y'] is not None or (x,z) in protected: return
        for dy in (1,2):
            old=reader.state(x,c['ground_y']+dy,z)['Name']
            if old not in AIR and not vegetation(old): return
        for dy in (1,2): place(x,c['ground_y']+dy,z,color,reason)
    for r in fit['routes']:
        path=dense_path(r['starter']['supported_world_path'])
        observations=profile(reader,path); floor=[]; unsafe_water=set(); run=[]
        for point in path+[None]:
            if point is not None and reader.column(*point)['water_y'] is not None: run.append(point)
            elif run:
                if len(run)>PARAMETERS['bridge_max_run'] or max(reader.column(*q)['water_depth'] for q in run)>PARAMETERS['bridge_max_depth']: unsafe_water.update(run)
                run=[]
        for k,(x,z) in enumerate(path):
            c=reader.column(x,z); y=c['ground_y']
            if (x,z) in unsafe_water:
                skipped.append({'route':r['id'],'xyz':[x,y,z],'reason':'bridge exceeds modest depth/span limits'}); floor.append(None); continue
            if c['water_y'] is not None: y=c['water_y']+1
            neighbor=path[min(k+1,len(path)-1)] if k<len(path)-1 else path[k-1]
            dx,dz=neighbor[0]-x,neighbor[1]-z
            for offset in (-1,0,1):
                xx,zz=x-dz*offset,z+dx*offset
                side=reader.column(xx,zz); sy=side['ground_y']
                if abs(sy-y)>1 and c['water_y'] is None and offset: continue
                if side['water_y'] is not None:
                    if side['water_depth']>PARAMETERS['bridge_max_depth']: continue
                    sy=side['water_y']+1
                name='minecraft:oak_planks' if side['water_y'] is not None else 'minecraft:coarse_dirt'
                if side['substrate'] in {'minecraft:lava','minecraft:powder_snow','minecraft:cactus','minecraft:magma_block'}:
                    skipped.append({'route':r['id'],'xyz':[xx,sy,zz],'reason':'hazardous substrate'}); continue
                obstructed=[yy for yy in range(sy+1,sy+4) if reader.state(xx,yy,zz)['Name'] not in AIR and not vegetation(reader.state(xx,yy,zz)['Name'])]
                if obstructed:
                    skipped.append({'route':r['id'],'xyz':[xx,sy,zz],'reason':'solid headroom obstruction; no excavation'}); continue
                place(xx,sy,zz,name,r['id']+':path'); protected.add((xx,zz))
                for yy in range(sy+1,sy+4):
                    if reader.state(xx,yy,zz)['Name'] not in AIR: place(xx,yy,zz,'minecraft:air',r['id']+':vegetation-clearance')
            floor.append(y)
        # Minimal stair on the lower column of a one-block rise. No earthwork.
        for k,((x,z),y) in enumerate(zip(path,floor)):
            if y is None: continue
            higher=next((j for j in (k+1,k-1) if 0<=j<len(path) and floor[j] is not None and floor[j]==y+1),None)
            if higher is None: continue
            xx,zz=path[higher]; facing='east' if xx>x else 'west' if xx<x else 'south' if zz>z else 'north'
            if reader.state(x,y+1,z)['Name'] in AIR or vegetation(reader.state(x,y+1,z)['Name']):
                # Clearance edits have priority only until the stair is installed.
                mods.pop((x,y+1,z),None)
                place(x,y+1,z,f'minecraft:stone_brick_stairs[facing={facing},half=bottom,shape=straight,waterlogged=false]',r['id']+':stair')
        route_records.append({'id':r['id'],'team':r['team'],'center_xz':[list(q) for q in path],
                              'planned_floor_y':floor,'source_block_profile':observations,
                              'analytical_starter_score':r['starter']['refined_feasibility']['score'],
                              'analytical_max_sample_rise':r['starter']['refined_feasibility']['full_supported_max_rise'],
                              'terminus':r['starter']['terminus'],'departure':r['departure']})
    for r in route_records:
        color='minecraft:light_blue_wool' if r['team']=='north' else 'minecraft:orange_wool'
        for index,label in ((0,'fountain-facing'),(-1,'terminus')):
            x,z=r['center_xz'][index]
            marker(x,z,'minecraft:yellow_concrete' if label=='terminus' else color,r['id']+':'+label)
        x,_,z=r['departure']['world_xyz']; marker(x,z,color,r['id']+':departure')
    for team,home in fit['homelands'].items():
        x0,x1,z0,z1=home['world_bounds']; color='minecraft:light_blue_wool' if team=='north' else 'minecraft:orange_wool'
        for x in range(x0,x1+1,PARAMETERS['boundary_marker_spacing']):
            for z in (z0,z1): marker(x,z,color,team+':boundary')
        for z in range(z0,z1+1,PARAMETERS['boundary_marker_spacing']):
            for x in (x0,x1): marker(x,z,color,team+':boundary')
        fx,_,fz=home['fountain']['world_xyz']
        for dz in (-1,0,1):
            for dx in (-1,0,1):
                x,z=fx+dx,fz+dz; c=reader.column(x,z)
                if c['water_y'] is None:
                    mods.pop((x,c['ground_y'],z),None)
                    place(x,c['ground_y'],z,'minecraft:sea_lantern' if dx==dz==0 else 'minecraft:polished_andesite',team+':fountain-placeholder')
    changes=[mods[k] for k in sorted(mods)]
    if len(changes)>PARAMETERS['maximum_edits']: raise ValueError('Minimal-authoring edit budget exceeded')
    return {'schema':'default_task_b_0_v1','seed':fit['seed'],'minecraft_version':'1.21.11','data_version':4671,
            'authoring':'vanilla setblock commands on independent copies; no terrain generation or synthetic reconstruction',
            'parameters':PARAMETERS,'bounds':fit['playable_bounds'],'orientation':fit['logical_orientation'],
            'homelands':fit['homelands'],'routes':route_records,'changes':changes,'skipped':skipped,
            'edit_counts':dict(sorted(Counter(c['after'] for c in changes).items())),
            'b2_corrections':[],'macro_terrain_edits':False}


def local_bypass(reader,path,radius=3):
    """Read-only test of a nearby alternative; never applies a B.2 correction."""
    allowed={(x+dx,z+dz) for x,z in path for dx in range(-radius,radius+1) for dz in range(-radius,radius+1)}
    def usable(q):
        c=reader.column(*q)
        return c['water_y'] is None and c['substrate'] not in {'minecraft:lava','minecraft:powder_snow','minecraft:magma_block'} and all(
            reader.state(q[0],c['ground_y']+dy,q[1])['Name'] in AIR or vegetation(reader.state(q[0],c['ground_y']+dy,q[1])['Name']) for dy in (1,2))
    start=tuple(path[0]); end=tuple(path[-1]); queue=[(0,start)]; distances={start:0}; previous={}
    while queue:
        d,q=heapq.heappop(queue)
        if d!=distances[q]: continue
        if q==end: break
        x,z=q
        for v in ((x-1,z),(x,z-1),(x,z+1),(x+1,z)):
            if v not in allowed or not usable(v): continue
            rise=abs(reader.column(*q)['ground_y']-reader.column(*v)['ground_y'])
            if rise>1: continue
            nd=d+1+.25*rise
            if nd<distances.get(v,math.inf): distances[v]=nd; previous[v]=q; heapq.heappush(queue,(nd,v))
    if end not in distances: return {'found':False,'search_radius_blocks':radius,'applied':False}
    result=[end]
    while result[-1]!=start: result.append(previous[result[-1]])
    result.reverse()
    return {'found':True,'search_radius_blocks':radius,'path_xz':[list(q) for q in result],
            'path_steps':len(result)-1,'reference_steps':len(path)-1,'applied':False,
            'maximum_horizontal_deviation_blocks':round(max(min(math.dist(q,r) for r in path) for q in result),3),
            'scope':'nearby dry one-block-step column route; clearing, exact collision shapes and experiential fit not validated'}


def validate_blocks(reader,fit,plan):
    mismatches=[c['xyz'] for c in plan['changes'] if state_string(reader.state(*c['xyz']))!=c['after']]
    routes=[]
    for record,r in zip(plan['routes'],fit['routes']):
        failures=[]; supports=[]
        for point,y in zip(record['center_xz'],record['planned_floor_y']):
            x,z=point
            if y is None: failures.append({'xz':point,'reason':'unsupported water crossing'}); supports.append(None); continue
            base=reader.state(x,y,z)['Name']; above=reader.state(x,y+1,z)['Name']
            stair=above.endswith('_stairs')
            foot=y+2 if stair else y+1
            if base in AIR or base in {'minecraft:water','minecraft:lava'}:
                failures.append({'xz':point,'reason':'no dry support'})
            for yy in (foot,foot+1):
                name=reader.state(x,yy,z)['Name']
                if name not in AIR and not vegetation(name): failures.append({'xz':point,'reason':'solid headroom obstacle','y':yy,'block':name})
            supports.append(y+1+(1 if stair else 0))
        for k,(a,b) in enumerate(zip(supports,supports[1:])):
            if a is not None and b is not None and abs(a-b)>1: failures.append({'xz':record['center_xz'][k+1],'reason':'adjacent step exceeds one block','rise':b-a})
        full=dense_path(r['world_path']); end=(r['starter']['terminus']['world_xyz'][0],r['starter']['terminus']['world_xyz'][2])
        tail=full[full.index(end):]
        bypass=local_bypass(reader,record['center_xz'],20) if failures else None
        routes.append({'id':r['id'],'starter_block_check_pass':not failures,'starter_failures':failures,
                       'source_geometry':record['source_block_profile'],'unsupported_continuation':profile(reader,tail),
                       'assessment':'sampled-spine block checks only, not normal player traversal or natural-route legibility',
                       'discrepancy_class':('local discrepancy: nearby dry bypass identified' if bypass['found'] else 'unresolved discrepancy; structural failure not established') if failures else 'no detected Starter spine obstacle',
                       'local_bypass_evidence':bypass,
                       'structural_fit_failure_proven':False})
    links=[{'id':c['id'],'profile':profile(reader,dense_path(c['world_path'])),
            'physical_shortcut_validated':None} for c in fit['cross_connections']]
    fountains={}
    for team,home in fit['homelands'].items():
        x,y,z=home['fountain']['world_xyz']
        fountains[team]={'xyz':[x,y,z],'support':reader.state(x,y-1,z)['Name'],
                         'two_air_blocks':all(reader.state(x,yy,z)['Name'] in AIR for yy in (y,y+1))}
    return {'seed':fit['seed'],'expected_edit_count':len(plan['changes']),'readback_mismatches':mismatches,
            'fountains':fountains,
            'routes':routes,'cross_connections':links,'server_loaded':True,
            'b1_status':'block-level pass performed; required player-scale walk incomplete',
            'candidate_pass':None,'candidate_fail':None,'structural_failure_proven':False,
            'b2_status':'not started: complete B.1 walk required before bounded corrections',
            'skeleton_frozen':False,'task_c_started':False,
            'unavailable':['player-scale three-choice legibility','normal movement traversal and perceived construction burden',
                           'unmarked Wilderness continuation','competitive interaction/ambush/avoidance','qualitative geographic depth',
                           'true mandatory crossings','features and Key Locations revealed by walking'],
            'construction_burden':{'block_edits':len(plan['changes']),'skipped_obstacles':len(plan['skipped']),
                                   'earth_cut_volume':0,'terrain_flattening':0,'cleared_blocks':sum(c['after']=='minecraft:air' for c in plan['changes'])}}
