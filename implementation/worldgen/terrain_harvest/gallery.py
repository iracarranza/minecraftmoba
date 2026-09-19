"""Void dimension gallery and deterministic navigation functions (Java 1.21.11)."""
import copy
import json
from pathlib import Path
from serialization.nbt import (byte,compound,double,float_tag,int_array,integer,list_tag,long,string,load_gzip,dump_gzip,COMPOUND)
from serialization.region import read_region,write_region
from serialization.world import _block_states_tag,AIR,block
from vanilla_search.extract import VanillaChunk
from serialization.nbt import plain
from .materialize import empty_chunk, export_volume, json_write, sha
from .model import Mask,dumps,validate
from .lifecycle import write_receipt

VOID={'type':'harvest:inspection','generator':{'type':'minecraft:flat','settings':{'biome':'minecraft:the_void','layers':[], 'features':False,'lakes':False,'structure_overrides':[]}}}
DIMENSION_TYPE={'ambient_light':0.0,'coordinate_scale':1.0,'has_ceiling':False,'has_skylight':True,
    'height':416,'min_y':-80,'logical_height':416,'infiniburn':'#minecraft:infiniburn_overworld',
    'monster_spawn_block_light_limit':0,'monster_spawn_light_level':0,'timelines':'#minecraft:in_overworld',
    'attributes':{'minecraft:visual/sky_color':'#78a7ff','minecraft:visual/fog_color':'#c0d8ff'}}

def nbt_json(value):
    if isinstance(value,dict):return compound(**{k:nbt_json(v) for k,v in value.items()})
    if isinstance(value,list):return list_tag(COMPOUND,[nbt_json(v) for v in value])
    if isinstance(value,bool):return byte(value)
    if isinstance(value,int):return integer(value)
    return string(value)

def navigation(volumes, targets):
    """Compact clickable labels; full provenance moves into hover text.

    The first client inspection filled the chat with wrapped provenance lines
    and the observer did not notice the entries were clickable. The detail is
    still complete and one hover away, rather than deleted.
    """
    functions={'load':['scoreboard objectives add harvest dummy'],
               'hub':['gamemode adventure @s','execute in minecraft:overworld run tp @s 0.5 65 0.5','scoreboard players set @s harvest -1',
                      'tellraw @s '+json.dumps({'text':'Terrain gallery | Run /tick freeze before snapshot inspection | /function harvest:next | /function harvest:index'})],
               'index':['tellraw @s '+json.dumps({'text':'Terrain gallery — click an entry to visit, hover for provenance','color':'gray','italic':True})],
               'next':['scoreboard players add @s harvest 1'], 'previous':['scoreboard players remove @s harvest 1']}
    n=len(volumes)
    functions['next']+= [f'execute if score @s harvest matches {n}.. run scoreboard players set @s harvest 0','function harvest:dispatch']
    functions['previous']+= [f'execute if score @s harvest matches ..-1 run scoreboard players set @s harvest {n-1}','function harvest:dispatch']
    functions['dispatch']=[]
    for i,v in enumerate(volumes):
        ident=v['id'];p=v['provenance'];t=targets[ident];dim='harvest:'+ident
        stage_c=next((m['stage_c'] for m in v['measurements'] if 'stage_c' in m),None)
        screen='Stage C unavailable' if stage_c is None else 'Recorded Stage C hard failures: '+json.dumps(stage_c.get('hard_failures','UNRESOLVED'))
        b=p['source_bounds']
        detail=(f"{ident}\nseed {p['source_seed']} | {v['classification']['kind']} | rotation 0\n"
                f"source x {b['x']} y {b['y']} z {b['z']}\n"
                f"evidence: {p['source_analysis_record']['path']}\n{screen}\n"
                "Practical Reach / final acceptance UNRESOLVED")
        span=(f"[{i+1}] {v['classification']['kind']} | seed {p['source_seed']} | "
              f"{b['x'][1]-b['x'][0]+1}x{b['z'][1]-b['z'][0]+1}")
        hover={'action':'show_text','value':{'text':detail}}
        functions['index'].append('tellraw @s '+json.dumps(
            {'text':span,'color':'aqua','underlined':True,
             'click_event':{'action':'run_command','command':'/function harvest:visit/'+ident},
             'hover_event':hover}))
        # Arriving somewhere should say where, briefly; detail stays on hover.
        arrival=json.dumps({'text':f"{span} — {ident[:12]}...",'color':'yellow','hover_event':hover})
        functions['visit/'+ident]=['gamemode adventure @s',f'scoreboard players set @s harvest {i}',f'execute in {dim} run tp @s {t[0]+.5} {t[1]} {t[2]+.5}',
            'tellraw @s '+arrival]
        functions['overview/'+ident]=['gamemode spectator @s',f'execute in {dim} run tp @s {b["x"][0]-12} {min(310,b["y"][1]+8)} {b["z"][0]-12}']
        functions['dispatch'].append(f'execute if score @s harvest matches {i} run function harvest:visit/{ident}')
    return functions

def find_target(world,v):
    mask=Mask(v);b=mask.bounds;center=(sum(b['x'])//2,sum(b['z'])//2)
    # Read actual exported blocks. Two air cells above a solid, non-fluid floor.
    candidates=sorted(((cx*16+8-center[0])**2+(cz*16+8-center[1])**2,cx,cz)
        for cz in range(b['z'][0]//16,b['z'][1]//16+1) for cx in range(b['x'][0]//16,b['x'][1]//16+1))
    cached_region=None;roots={}
    safe={'minecraft:'+name for name in ('stone','deepslate','grass_block','dirt','coarse_dirt','podzol','mycelium','sand','red_sand','gravel','snow_block','packed_ice','ice','blue_ice','moss_block','calcite','tuff','andesite','diorite','granite','terracotta','clay')}
    for _,cx,cz in candidates:
        region=(cx//32,cz//32)
        if region!=cached_region:
            roots={(x,z):r for x,z,_,r in read_region(world/'region'/f'r.{region[0]}.{region[1]}.mca')}
            cached_region=region
        c=VanillaChunk(plain(roots[(cx,cz)]))
        for z in range(cz*16,cz*16+16):
            for x in range(cx*16,cx*16+16):
                for y in range(min(316,b['y'][1]-2),max(-63,b['y'][0]),-1):
                    if not mask.include_block(x,y+2,z):continue
                    floor=c.block(x,y,z)
                    if floor in safe and c.block(x,y+1,z)=='minecraft:air' and c.block(x,y+2,z)=='minecraft:air':
                        return [x,y+1,z]
    raise ValueError('no conservative standing target; supply different selection')

def build_gallery(volumes, sources, output):
    volumes=sorted(volumes,key=lambda v:v['id'])
    if not volumes or len({v['id'] for v in volumes})!=len(volumes):raise ValueError('nonempty unique volume list required')
    for v in volumes:validate(v)
    if output.exists():raise FileExistsError(output)
    # A failed run stays visibly incomplete, never acquires a success manifest.
    output.mkdir(parents=True);(output/'INCOMPLETE').write_text('Do not inspect until gallery.json exists.\n')
    pack=output/'datapacks/terrain_gallery';base=pack/'data/harvest'
    json_write(pack/'pack.mcmeta',{'pack':{'min_format':[94,1],'max_format':[94,1],'description':'Terrain harvest inspection; Java 1.21.11'}})
    json_write(base/'dimension_type/inspection.json',DIMENSION_TYPE)
    for dim in ('overworld','the_nether','the_end'):json_write(pack/f'data/minecraft/dimension/{dim}.json',VOID)
    records=[];targets={}
    for v in volumes:
        ident=v['id'];dest=output/'dimensions/harvest'/ident
        json_write(base/f'dimension/{ident}.json',VOID)
        print('Materializing '+ident+' '+v['classification']['kind'],flush=True)
        source=sources[v['provenance']['source_seed']]
        record=export_volume(v,source,dest);records.append(record)
        targets[ident]=find_target(dest,v)
        (dest/'terrain_volume.json').write_text(dumps(v))
        # Receipt last: an interrupted volume is left without one, never with a false one.
        write_receipt(dest,v,record,source)
    source=next(iter(sources.values()));name,root=load_gzip(source/'level.dat');data=root.value['Data'].value
    for k in ('Player','BossEvents','WanderingTraderId'):data.pop(k,None)
    data['ScheduledEvents']=list_tag(COMPOUND,[]);data['CustomBossEvents']=compound()
    for key,value in [('SpawnX',0),('SpawnY',65),('SpawnZ',0)]:data[key]=integer(value)
    data['LevelName']=string('Terrain Harvest Gallery');data['GameType']=integer(2);data['allowCommands']=byte(1)
    data['spawn']=compound(pos=int_array([0,65,0]),dimension=string('minecraft:overworld'),yaw=float_tag(0),pitch=float_tag(0))
    data['DataPacks']=compound(Enabled=list_tag(8,[string('vanilla'),string('file/terrain_gallery')]),Disabled=list_tag(8,[]))
    data['WorldGenSettings']=compound(seed=long(0),generate_features=byte(0),bonus_chest=byte(0),dimensions=compound(**{'minecraft:'+d:nbt_json(VOID) for d in ('overworld','the_nether','the_end')}))
    data['Time']=long(6000);data['DayTime']=long(6000);data['LastPlayed']=long(0)
    rules=data['game_rules'].value
    for key in ('spawn_mobs','advance_time','advance_weather','allow_entering_nether_using_portals'):rules['minecraft:'+key]=byte(0)
    rules['minecraft:respawn_radius']=integer(0)
    rules['minecraft:random_tick_speed']=integer(0);rules['minecraft:keep_inventory']=byte(1)
    dump_gzip(output/'level.dat',name,root)
    hub=empty_chunk(0,0);states=[block('bedrock') if y==0 else AIR for y in range(16) for z in range(16) for x in range(16)]
    hub.value['sections']=list_tag(COMPOUND,[compound(Y=byte(4),block_states=_block_states_tag(states),biomes=compound(palette=list_tag(8,[string('minecraft:the_void')])))])
    write_region(output/'region/r.0.0.mca',{(0,0):('',hub)})
    for name,lines in navigation(volumes,targets).items():
        p=base/'function'/f'{name}.mcfunction';p.parent.mkdir(parents=True,exist_ok=True);p.write_text('\n'.join(lines)+'\n')
    json_write(pack/'data/minecraft/tags/function/load.json',{'values':['harvest:load']})
    json_write(output/'gallery.json',{'schema':'terrain_gallery/1','minecraft_version':'1.21.11','volumes':records,'targets':targets,
        'normal_mode':'adventure; run /tick freeze before visiting for static inspection; use /function harvest:hub then next/previous/index',
        'debug':'/function harvest:overview/<id> gives spectator; visit/<id> restores adventure',
        'containment':'26-neighbor exterior bedrock envelope, barrier roof; commands/spectator/teleport exploits excluded',
        'inspection_status':'generated and decoded; no Minecraft client inspection claimed'})
    (output/'INCOMPLETE').unlink()
    return records
