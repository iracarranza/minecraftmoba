#!/usr/bin/env python3
"""B.0 + bounded B.1 block readback on the two human-accepted vanilla fits."""
import argparse
import json
import shutil
import struct
import time
from pathlib import Path

from fit_default_task_a import ROOT,SOURCE,REFINED_OUTPUT,digest,encode,world_snapshot
from serialization.nbt import load_gzip,dump_gzip,string
from serialization.inspect import _png,COLORS
from vanilla_search.acquire import ServerConsole,verify_server_jar
from vanilla_search.task_b import BlockWorld,required_columns,validation_columns,plan_greybox,validate_blocks,state_string

OUT=ROOT/'implementation/worldgen/results/default_task_b_2026-09-10'
ART=ROOT/'artifacts/worldgen/default_task_b_2026-09-10'
RUNTIME=ROOT/'artifacts/worldgen/staged_default_2026-09-09/build/runtime'
JAVA=Path('/Users/iracarranza/Library/Application Support/minecraft/runtime/java-runtime-delta/mac-os-arm64/java-runtime-delta/jre.bundle/Contents/Home/bin/java')
JAR=Path('/private/tmp/moba-server-1.21.11.jar')
ACCEPTANCE=ROOT/'implementation/worldgen/decisions/task_b_gate_acceptance_2026-09-10.json'


def chunk_inventory(world):
    chunks=set()
    for f in (Path(world)/'region').glob('*.mca'):
        _,rx,rz=f.stem.split('.')
        with f.open('rb') as stream: locations=struct.unpack('>1024I',stream.read(4096))
        chunks.update((int(rx)*32+i%32,int(rz)*32+i//32) for i,v in enumerate(locations) if v)
    return chunks


def preservation(source,world):
    original=chunk_inventory(source); physical=chunk_inventory(world)
    return {'source_overworld_chunks':len(original),'physical_overworld_chunks':len(physical),
            'new_chunks':sorted(physical-original),'removed_chunks':sorted(original-physical)}


def build_server(source,fit,plan,server_root,java,jar):
    if server_root.exists(): raise ValueError(f'Refuse overwrite of existing build: {server_root}')
    server_root.mkdir(parents=True)
    world=server_root/'world'; shutil.copytree(source,world)
    name,root=load_gzip(world/'level.dat')
    root.value['Data'].value['LevelName']=string(f'Task B 1.21.11 — {fit["seed"]}')
    dump_gzip(world/'level.dat',name,root)
    for n in ('libraries','versions'): (server_root/n).symlink_to(RUNTIME/n,target_is_directory=True)
    # Reuse the affirmative EULA record from this exact pinned-server workflow.
    eula=ROOT/'artifacts/worldgen/staged_default_2026-09-09/build/930010639_2048_0/eula.txt'
    if 'eula=true' not in eula.read_text(): raise ValueError('Existing affirmative EULA record missing')
    shutil.copyfile(eula,server_root/'eula.txt')
    (server_root/'server.properties').write_text('level-name=world\nserver-ip=127.0.0.1\nserver-port=25575\nonline-mode=false\ngamemode=creative\nallow-flight=true\nspawn-protection=0\nview-distance=2\nsimulation-distance=2\npause-when-empty-seconds=-1\nmax-tick-time=60000\nsync-chunk-writes=true\n')
    commands=[f'setblock {c["xyz"][0]} {c["xyz"][1]} {c["xyz"][2]} {c["after"]} replace' for c in plan['changes']]
    server=ServerConsole([str(java),'-Xms512M','-Xmx2G','-jar',str(jar),'--nogui'],server_root)
    started=time.monotonic()
    try:
        server.wait_for('Done (',120)
        chunks=sorted({(c['xyz'][0]//16,c['xyz'][2]//16) for c in plan['changes']})
        if len(chunks)>256: raise ValueError('Too many chunks for minimal greybox')
        for cx,cz in chunks:
            server.send(f'forceload add {cx*16} {cz*16}')
            server.wait_for('to be force loaded',30)
        server.send('tick freeze'); server.wait_for('frozen',30)
        for cx,cz in chunks:
            marker=f'TASK_B_LOADED_{cx}_{cz}'
            server.send(f'execute if loaded {cx*16} 64 {cz*16} run say {marker}')
            server.wait_for(marker,30)
        for command in commands: server.send(command)
        server.send('say TASK_B_BLOCKS_APPLIED'); server.wait_for('TASK_B_BLOCKS_APPLIED',120)
        x,y,z=fit['homelands']['north']['fountain']['world_xyz']
        server.send(f'setworldspawn {x} {y} {z}')
        server.send('forceload remove all'); server.wait_for('Unmarked all force loaded chunks',30)
        server.send('save-all flush'); server.wait_for('Saved the game',120)
        server.send('stop'); server.process.wait(timeout=120)
    finally:
        if server.process.poll() is None:
            server.send('stop')
            try: server.process.wait(timeout=30)
            except Exception: server.process.kill(); server.process.wait(timeout=10)
        (server_root/'build_server.log').write_text('\n'.join(server.log)+'\n')
    errors=[line for line in server.log if any(v in line for v in ('ERROR','Exception','Unknown or incomplete command','That position is not loaded','Incorrect argument'))]
    return world,{'ready':True,'exit_code':server.process.returncode,'errors':errors,
                  'server_jar_sha256':digest(jar),'seconds':round(time.monotonic()-started,3),
                  'loaded_edit_chunks':len(chunks),'vanilla_commands':len(commands),'tick_freeze_during_placement':True}


def render_home(reader,home,path):
    x0,x1,z0,z1=home['world_bounds']; pixels=[]; scale=4
    colors={**COLORS,'minecraft:coarse_dirt':(148,100,58),'minecraft:light_blue_wool':(65,170,245),
            'minecraft:orange_wool':(239,136,44),'minecraft:yellow_concrete':(245,220,45),
            'minecraft:sea_lantern':(215,255,243),'minecraft:polished_andesite':(160,165,166),
            'minecraft:stone_brick_stairs':(140,140,140)}
    for z in range(z0,z1+1):
        row=[]
        for x in range(x0,x1+1):
            c=reader.column(x,z); name=reader.state(x,c['surface_y'],z)['Name']
            row.extend(colors.get(name,(105,125,85))*scale)
        for _ in range(scale): pixels.extend(row)
    _png(path,(x1-x0+1)*scale,(z1-z0+1)*scale,pixels)


def run(java=JAVA,jar=JAR):
    acceptance=json.loads(ACCEPTANCE.read_text())
    if not acceptance['remaining_readiness_criteria_accepted'] or not acceptance['task_b_authorized']: raise ValueError('Human gate acceptance required')
    verify_server_jar(jar)
    if not java.is_file(): raise ValueError('Java 21 runtime missing')
    candidates=[json.loads((SOURCE/'finalists'/str(s)/'candidate.json').read_text()) for s in acceptance['selected_candidates']]
    clean_before=world_snapshot(candidates)
    if any(not v['available'] for v in clean_before.values()): raise ValueError('A selected clean world is unavailable')
    OUT.mkdir(parents=True,exist_ok=True); summaries=[]
    for c in candidates:
        seed=c['seed']; fit_path=REFINED_OUTPUT/str(seed)/'fit.json'; fit=json.loads(fit_path.read_text())
        directory=OUT/str(seed); directory.mkdir(exist_ok=True)
        if (directory/'validation.json').exists():
            plan=json.loads((directory/'build_plan.json').read_text())
            if plan['source_fit_sha256']!=digest(fit_path): raise ValueError('Refuse reuse after fit change')
            old=json.loads((directory/'validation.json').read_text())
            actual=BlockWorld(Path(old['world_path']),validation_columns(fit,plan))
            validation=validate_blocks(actual,fit,plan)
            for key in ('world_path','source_world_path','b0_status'): validation[key]=old[key]
            validation['chunk_preservation']=preservation(old['source_world_path'],old['world_path'])
            (directory/'validation.json').write_text(encode(validation)); summaries.append(validation)
            print(f'{seed}: existing physical build revalidated without world writes',flush=True)
            continue
        print(f'{seed}: reading actual blocks and planning minimal Starter infrastructure',flush=True)
        columns=required_columns(fit); source=Path(c['local_world_directory']); reader=BlockWorld(source,columns)
        plan=plan_greybox(reader,fit); plan['source_fit_sha256']=digest(fit_path)
        (directory/'build_plan.json').write_text(encode(plan))
        (directory/'apply.mcfunction').write_text('\n'.join(f'setblock {p["xyz"][0]} {p["xyz"][1]} {p["xyz"][2]} {p["after"]} replace' for p in plan['changes'])+'\n')
        (directory/'undo.mcfunction').write_text('\n'.join(f'setblock {p["xyz"][0]} {p["xyz"][1]} {p["xyz"][2]} {state_string(p["before"])} replace' for p in reversed(plan['changes']))+'\n')
        print(f'{seed}: placing {len(plan["changes"])} blocks via matching vanilla server',flush=True)
        world,server=build_server(source,fit,plan,ART/str(seed),java,jar)
        (directory/'server_validation.json').write_text(encode(server))
        (directory/'server.log').write_text((ART/str(seed)/'build_server.log').read_text())
        if server['errors'] or server['exit_code']!=0: raise RuntimeError('Vanilla server reported an authoring/load error')
        actual=BlockWorld(world,validation_columns(fit,plan)); validation=validate_blocks(actual,fit,plan)
        validation['world_path']=str(world); validation['source_world_path']=str(source)
        validation['chunk_preservation']=preservation(source,world)
        validation['b0_status']='physical greybox materialized; block discrepancies retained for validation'
        for team,home in fit['homelands'].items(): render_home(actual,home,directory/f'{team}_block_readback.png')
        (world/'TASK_B_INSPECTION.json').write_text(encode({'seed':seed,'fit_source':str(fit_path),
            'fountains':{team:h['fountain']['world_xyz'] for team,h in fit['homelands'].items()},
            'routes':[{'id':r['id'],'departure':r['departure'],'terminus':r['starter']['terminus']} for r in fit['routes']],
            'legend':'Blue/orange posts: team boundary/departure; yellow posts: handoff; sea lantern: Fountain placeholder. Coarse dirt/stairs are supported Starter infrastructure only.',
            'status':'B.1 full walk pending; no freeze','version':'Minecraft Java 1.21.11'}))
        (directory/'validation.json').write_text(encode(validation)); summaries.append(validation)
        print(f'{seed}: readback mismatches={len(validation["readback_mismatches"])}; Starter spine passes={sum(r["starter_block_check_pass"] for r in validation["routes"])}/6',flush=True)
    unchanged=clean_before==world_snapshot(candidates)
    if not unchanged: raise RuntimeError('Clean source world changed')
    (OUT/'summary.json').write_text(encode({'gate':'human accepted; B.0 authorized','candidates':summaries,
        'clean_worlds_unchanged':unchanged,'source_world_files_verified':{s:len(v['files']) for s,v in clean_before.items()},
        'source_world_sha256':{s:v['files'] for s,v in clean_before.items()},
        'b2_performed':False,'frozen_candidate':None,'task_c_performed':False,
        'stop_condition':'B.1 player-scale walk incomplete; section 27 requires completing that pass before B.2. Block checks do not establish a competitive pass.'}))


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java',type=Path,default=JAVA); parser.add_argument('--server-jar',type=Path,default=JAR)
    args=parser.parse_args(); run(args.java,args.server_jar)
