"""Identity-placement Java 1.21.11 exporter; source worlds must be offline snapshots.

Preserves in-mask block palette states, biomes, block entities, scheduled ticks,
and contained entity trees. Removes cross-boundary structure/POI metadata. No
terrain synthesis, resource placement, or fitter changes occur here.
"""
from __future__ import annotations
import copy
import hashlib
import json
import math
from collections import defaultdict
from pathlib import Path
from serialization.nbt import (Tag, COMPOUND, LIST, SHORT, byte, compound, double, float_tag,
    int_array, integer, list_tag, long, string, plain, load_gzip, dump_gzip)
from serialization.region import read_region, write_region
from serialization.world import _block_states_tag, AIR, block
from vanilla_search.extract import _palette_value
from .model import Mask, validate

DATA_VERSION=4671

def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def json_write(path, data):
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,sort_keys=True,indent=2)+'\n')

def empty_chunk(cx,cz):
    return compound(DataVersion=integer(DATA_VERSION),xPos=integer(cx),zPos=integer(cz),yPos=integer(-5),
        Status=string('minecraft:full'),LastUpdate=long(0),InhabitedTime=long(0),isLightOn=byte(0),
        sections=list_tag(COMPOUND,[]),block_entities=list_tag(COMPOUND,[]),block_ticks=list_tag(COMPOUND,[]),
        fluid_ticks=list_tag(COMPOUND,[]),PostProcessing=list_tag(LIST,[list_tag(SHORT,[]) for _ in range(26)]),
        structures=compound(starts=compound(),References=compound()))

def state_tuple(entry): return (entry['Name'],tuple(sorted(entry.get('Properties',{}).items())))

def clip_chunk(root, cx, cz, mask):
    """Never writes into the source root. Interior sections retain exact state NBT."""
    source=root.value if root else {}; out=empty_chunk(cx,cz); sections=[]
    by_y={s.value['Y'].value:s for s in source.get('sections',list_tag(COMPOUND,[])).value}
    b=mask.bounds
    for sy in range(-5,21):
        src=by_y.get(sy); origin=(cx*16,sy*16,cz*16)
        if src and mask.section_inside(*origin):
            section=copy.deepcopy(src)
        else:
            srcplain=plain(src) if src else {}; container=srcplain.get('block_states')
            states=[]
            for y in range(sy*16,sy*16+16):
                for z in range(cz*16,cz*16+16):
                    for x in range(cx*16,cx*16+16):
                        if mask.include_block(x,y,z):
                            state=state_tuple(_palette_value(container,(y&15)*256+(z&15)*16+(x&15),4)) if container else AIR
                        elif mask.envelope(x,y,z):
                            # Transparent unbreakable roof; visible bedrock sides and floor.
                            state=block('barrier') if y>b['y'][1] else block('bedrock')
                        else: state=AIR
                        states.append(state)
            if all(s==AIR for s in states) and src is None: continue
            section=compound(Y=byte(sy),block_states=_block_states_tag(states),
                             biomes=copy.deepcopy(src.value['biomes']) if src and 'biomes' in src.value else compound(palette=list_tag(8,[string('minecraft:the_void')])))
        # Relight against void rather than retaining source neighbor lighting.
        section.value.pop('SkyLight',None);section.value.pop('BlockLight',None)
        sections.append(section)
    out.value['sections']=list_tag(COMPOUND,sections)
    for key in ('block_entities','block_ticks','fluid_ticks'):
        out.value[key]=list_tag(COMPOUND,[copy.deepcopy(t) for t in source.get(key,list_tag(COMPOUND,[])).value
            if all(a in t.value for a in ('x','y','z')) and mask.include_block(*(t.value[a].value for a in ('x','y','z')))])
    return out

def entity_inside(t, mask):
    d=t.value
    if 'Pos' not in d: return False
    if not mask.include_block(*(math.floor(x.value) for x in d['Pos'].value)): return False
    return all(entity_inside(p,mask) for p in d.get('Passengers',list_tag(COMPOUND,[])).value)

def export_volume(v, source, out):
    validate(v)
    if v['transform']!={'rotation':0,'translation':[0,0,0]}: raise ValueError('physical state-aware transforms not implemented; reference transforms only')
    if v['provenance']['source_dimension']!='minecraft:overworld': raise ValueError('only overworld source adapter implemented')
    level_path=source/'level.dat'; level_hash=sha(level_path); _,level=load_gzip(level_path);data=plain(level)['Data']
    p=v['provenance']
    if data['DataVersion']!=DATA_VERSION or p['data_version']!=DATA_VERSION or p['minecraft_version']!='1.21.11': raise ValueError('requires Java 1.21.11 DataVersion 4671')
    if data['WorldGenSettings']['seed']!=p['source_seed']: raise ValueError('source seed mismatch')
    mask=Mask(v);b=mask.bounds
    if b['y'][0]<-64 or b['y'][1]>319: raise ValueError('source height outside overworld')
    xmin,xmax=(b['x'][0]-1)//16,(b['x'][1]+1)//16;zmin,zmax=(b['z'][0]-1)//16,(b['z'][1]+1)//16
    needed={(x,z) for z in range(b['z'][0]//16,b['z'][1]//16+1) for x in range(b['x'][0]//16,b['x'][1]//16+1)}
    # Preflight all intersecting full chunks; missing chunks never turn into invented air.
    inventory={};found=set()
    for rz in range(zmin//32,zmax//32+1):
        for rx in range(xmin//32,xmax//32+1):
            f=source/'region'/f'r.{rx}.{rz}.mca'
            if f.exists():
                inventory[str(f.relative_to(source))]=sha(f)
                for cx,cz,_,r in read_region(f):
                    if (cx,cz) in needed:
                        if r.value.get('Status',string('')).value!='minecraft:full': raise ValueError('non-full source chunk')
                        if r.value.get('DataVersion',integer(-1)).value!=DATA_VERSION: raise ValueError('source chunk version mismatch')
                        found.add((cx,cz))
    if found!=needed: raise ValueError(f'missing source chunks: {sorted(needed-found)[:8]}')
    chunks_written=0;block_entities=0;ticks=defaultdict(int)
    for rz in range(zmin//32,zmax//32+1):
        for rx in range(xmin//32,xmax//32+1):
            f=source/'region'/f'r.{rx}.{rz}.mca'
            roots={(cx,cz):r for cx,cz,_,r in read_region(f) if xmin<=cx<=xmax and zmin<=cz<=zmax} if f.exists() else {}
            dest={}
            for cz in range(max(zmin,rz*32),min(zmax,rz*32+31)+1):
                for cx in range(max(xmin,rx*32),min(xmax,rx*32+31)+1):
                    chunk=clip_chunk(roots.get((cx,cz)),cx,cz,mask);dest[(cx,cz)]=('',chunk)
                    block_entities+=len(chunk.value['block_entities'].value)
                    for key in ('block_ticks','fluid_ticks'):ticks[key]+=len(chunk.value[key].value)
            write_region(out/'region'/f'r.{rx}.{rz}.mca',dest);chunks_written+=len(dest)
    entities=0;excluded=0
    for f in sorted((source/'entities').glob('*.mca')):
        _,rx,rz=f.stem.split('.')
        if not (xmin//32<=int(rx)<=xmax//32 and zmin//32<=int(rz)<=zmax//32):continue
        inventory[str(f.relative_to(source))]=sha(f);dest={}
        for cx,cz,name,r in read_region(f):
            if not (xmin<=cx<=xmax and zmin<=cz<=zmax):continue
            kept=[copy.deepcopy(t) for t in r.value['Entities'].value if entity_inside(t,mask)]
            excluded+=len(r.value['Entities'].value)-len(kept);entities+=len(kept)
            if kept: dest[(cx,cz)]=(name,compound(DataVersion=integer(DATA_VERSION),Position=int_array([cx,cz]),Entities=list_tag(COMPOUND,kept)))
        if dest:write_region(out/'entities'/f.name,dest)
    if sha(level_path)!=level_hash or any(sha(source/f)!=h for f,h in inventory.items()): raise RuntimeError('source changed during export; discard output')
    return {'volume_id':v['id'],'status':'materialized_not_client_inspected','source_level_sha256':level_hash,
        'worldgen_settings':data['WorldGenSettings'],'source_region_sha256':inventory,
        'source_snapshot_note':'existing staged inspection snapshot; pristine worldgen equivalence unproven',
        'chunks':chunks_written,'block_entities':block_entities,'entity_roots':entities,'excluded_entity_roots':excluded,'scheduled_ticks':dict(ticks),
        'preserved':'in-mask blocks including properties, air/caves, ores, fluids, vegetation, structure blocks, biomes, block entities and contained entity trees',
        'limitations':['structure starts/references and POI registries omitted; structure-specific spawning and villager POI behavior not preserved',
          'entities with any passenger outside mask excluded; external UUID/leash/brain references are not repaired',
          'fluids and neighbor-dependent blocks may update after load; frozen ticks recommended for snapshot inspection',
          'partial structures and caves are clipped by mask; no seam repair', 'physical rotation/translation unsupported']}
