"""Translate saved analytical candidates into Minecraft Java 1.21.11 worlds."""

from __future__ import annotations

import json
import math
import random
import shutil
import uuid
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path

from successor.grid import hash01, unrle, value_noise

from .nbt import (
    BYTE, COMPOUND, DOUBLE, FLOAT, INT, LIST, SHORT, Tag, byte, compound,
    double, dump_gzip, float_tag, int_array, integer, list_tag, long,
    long_array, string,
)
from .region import write_region

DATA_VERSION=4671
VERSION_NAME="1.21.11"
MIN_Y=-64
MAX_Y=127
AIR=("minecraft:air",())
REPOSITORY_ROOT=Path(__file__).resolve().parents[3]


def block(name, **properties): return (f"minecraft:{name}",tuple(sorted((k,str(v)) for k,v in properties.items())))


@dataclass
class Entity:
    identifier: str
    kind: str
    x: float
    y: float
    z: float
    extra: dict


class WorldModel:
    def __init__(self,candidate):
        self.candidate=candidate; self.seed=candidate["seed"]
        grid=candidate["terrain"]["grid_encoding"]
        self.anx,self.anz=grid["nx"],grid["nz"]
        self.analytic_h=unrle(grid["height_rle"]); self.analytic_kind=unrle(grid["terrain_rle"])
        self.min_cx,self.max_cx=-27,26; self.min_cz,self.max_cz=-33,32
        self.x0,self.x1=self.min_cx*16,(self.max_cx+1)*16-1
        self.z0,self.z1=self.min_cz*16,(self.max_cz+1)*16-1
        self.width=self.x1-self.x0+1; self.depth=self.z1-self.z0+1
        self.heights=[0]*(self.width*self.depth); self.kinds=[""]*(self.width*self.depth)
        self.surface_override={}; self.mods={}; self.mod_sections=set(); self.entities=[]; self.route_columns=set(); self.bridge_water_y={}
        self.protected=set(); self.corrections=[]; self.route_observations=[]
        self._expand_analytical_surface()

    def idx(self,x,z): return (z-self.z0)*self.width+(x-self.x0)
    def in_bounds(self,x,z): return self.x0<=x<=self.x1 and self.z0<=z<=self.z1
    def height(self,x,z): return self.heights[self.idx(x,z)]
    def kind(self,x,z): return self.kinds[self.idx(x,z)]
    def set_height(self,x,z,h): self.heights[self.idx(x,z)]=max(MIN_Y+2,min(MAX_Y-10,round(h)))
    def set_kind(self,x,z,k): self.kinds[self.idx(x,z)]=k
    def set_block(self,x,y,z,state):
        if self.in_bounds(x,z) and MIN_Y<=y<=MAX_Y:
            self.mods[(x,y,z)]=state; self.mod_sections.add((x//16,z//16,y//16))

    def _expand_analytical_surface(self):
        for z in range(self.z0,self.z1+1):
            az=max(-520,min(520,z)); gz=(az+520)/5; zq=min(self.anz-2,max(0,math.floor(gz))); tz=gz-zq
            for x in range(self.x0,self.x1+1):
                ax=max(-420,min(420,x)); gx=(ax+420)/5; xq=min(self.anx-2,max(0,math.floor(gx))); tx=gx-xq
                q=zq*self.anx+xq
                h00=self.analytic_h[q]; h10=self.analytic_h[q+1]; h01=self.analytic_h[q+self.anx]; h11=self.analytic_h[q+self.anx+1]
                h=(h00*(1-tx)+h10*tx)*(1-tz)+(h01*(1-tx)+h11*tx)*tz
                # Coherent one-block breakup avoids visible 5-block interpolation facets.
                h+=round(value_noise(self.seed^0xB10C,x,z,13)*.8)
                i=self.idx(x,z); self.heights[i]=round(h)
                nx=min(self.anx-1,max(0,round(gx))); nz=min(self.anz-1,max(0,round(gz)))
                self.kinds[i]=self.analytic_kind[nz*self.anx+nx]

    def apply_homelands(self):
        for team,(cx,cz) in self.candidate["homelands"].items():
            target=self.height(cx,cz); changed=0; max_delta=0
            for z in range(cz-55,cz+56):
                for x in range(cx-55,cx+56):
                    d=math.hypot(x-cx,z-cz)
                    if d>55 or not self.in_bounds(x,z): continue
                    old=self.height(x,z); blend=1 if d<=34 else (55-d)/21
                    new=round(old*(1-blend)+target*blend); self.set_height(x,z,new)
                    if new!=old: changed+=1; max_delta=max(max_delta,abs(new-old))
                    if d<=38: self.protected.add((x,z))
            self.corrections.append({"stage":"homeland_safety","team":team,"columns_adjusted":changed,"maximum_y_adjustment":max_delta,"reason":"safe usable inspection/development pad"})

    def apply_settlement_pads(self):
        for item in self.candidate["ecology"]["instances"]:
            if item["category"]!="settlement": continue
            cx,cz=item["pos"]; target=self.height(cx,cz); changed=0; max_delta=0
            for z in range(cz-15,cz+16):
                for x in range(cx-15,cx+16):
                    d=math.hypot(x-cx,z-cz)
                    if d>15: continue
                    old=self.height(x,z); blend=1 if d<=10 else (15-d)/5
                    new=round(old*(1-blend)+target*blend); self.set_height(x,z,new)
                    if new!=old: changed+=1; max_delta=max(max_delta,abs(new-old))
                    if d<=14: self.protected.add((x,z))
            self.corrections.append({"stage":"settlement_pad","id":item["id"],"columns_adjusted":changed,"maximum_y_adjustment":max_delta,"reason":"small developable Minecraft-like settlement footprint"})

    def apply_routes(self):
        desired={}; branch_stats=[]
        for branch in self.candidate["routes"]["branches"]:
            centers=[]; water=forest=steep=0
            for a,b in zip(branch["centerline"],branch["centerline"][1:]):
                x0,z0=a; x1,z1=b; n=max(abs(x1-x0),abs(z1-z0),1)
                y0,y1=self.height(x0,z0),self.height(x1,z1)
                if abs(y1-y0)>1: steep+=1
                for step in range(n+1):
                    x=round(x0+(x1-x0)*step/n); z=round(z0+(z1-z0)*step/n); y=round(y0+(y1-y0)*step/n)
                    desired[(x,z)]=y; centers.append((x,z,y))
                    water+=int(self.kind(x,z) in ("water","ocean")); forest+=int(self.kind(x,z)=="forest")
            branch_stats.append({"branch":branch["id"],"center_columns":len(set((x,z) for x,z,_ in centers)),"stream_or_ocean_columns":water,"forest_columns":forest,"analytical_segments_rising_over_one_y":steep})
        old_heights=list(self.heights); adjusted=0; max_adjust=0
        for (cx,cz),y in desired.items():
            for dz in range(-4,5):
                for dx in range(-4,5):
                    d=math.hypot(dx,dz)
                    if d>4 or not self.in_bounds(cx+dx,cz+dz): continue
                    x,z=cx+dx,cz+dz; old=self.height(x,z)
                    blend=1 if d<=2 else (4-d)/2
                    new=round(old*(1-blend)+y*blend); self.set_height(x,z,new)
                    if new!=old: adjusted+=1; max_adjust=max(max_adjust,abs(new-old))
                    if d<=2:
                        self.route_columns.add((x,z)); self.protected.add((x,z))
                        if self.kind(x,z) in ("water","ocean"):
                            self.bridge_water_y[(x,z)]=old_heights[self.idx(x,z)]
                            self.set_height(x,z,max(self.height(x,z),self.bridge_water_y[(x,z)]+2))
                            self.surface_override[(x,z)]=block("oak_planks")
                        else:
                            self.surface_override[(x,z)]=block("dirt_path") if d<=1 else block("gravel")
        self.corrections.append({"stage":"route_physicalization","columns_adjusted":adjusted,"maximum_y_adjustment":max_adjust,"reason":"convert 5-block analytical centerlines into step-traversable temporary paths with blended aprons"})
        for stat in branch_stats:
            stat["assessment"]="crossing/grade review required" if stat["stream_or_ocean_columns"] or stat["analytical_segments_rising_over_one_y"] else "ordinary terrain fit"
        self.route_observations=branch_stats

    def material(self,x,z):
        if (x,z) in self.surface_override: return self.surface_override[(x,z)]
        kind=self.kind(x,z); h=self.height(x,z)
        east=self.height(min(self.x1,x+1),z); south=self.height(x,min(self.z1,z+1)); slope=max(abs(east-h),abs(south-h))
        r=hash01(self.seed^0x51A9,x,z)
        if kind=="coast": return block("sand") if r>.18 else block("gravel")
        if kind=="forest": return block("podzol") if r>.55 else block("grass_block",snowy="false")
        if kind=="foothill": return block("stone") if slope>=2 and r>.35 else block("coarse_dirt") if r>.72 else block("grass_block",snowy="false")
        if kind=="rock": return block("andesite") if r>.72 else block("stone")
        if kind=="scree": return block("gravel") if r>.28 else block("andesite")
        if kind=="snow": return block("snow_block") if slope<2 else block("stone")
        if kind=="water": return block("water",level="0")
        return block("grass_block",snowy="false")

    def base_block(self,x,y,z):
        changed=self.mods.get((x,y,z))
        if changed is not None: return changed
        h=self.height(x,z); kind=self.kind(x,z)
        if (x,z) in self.bridge_water_y:
            water_y=self.bridge_water_y[(x,z)]
            if y>h: return AIR
            if y==h: return self.surface_override[(x,z)]
            if y>water_y: return AIR
            if y==water_y: return block("water",level="0")
            if y==MIN_Y: return block("bedrock")
            if y>=water_y-3: return block("gravel")
            return block("deepslate",axis="y") if y<0 else block("stone")
        if kind=="ocean":
            if y>64: return AIR
            if y>h: return block("water",level="0")
        elif kind=="water":
            if y>h: return AIR
            if y==h: return block("water",level="0")
            h-=1
        elif y>h: return AIR
        if y==MIN_Y: return block("bedrock")
        top=self.material(x,z)
        if y==h: return top
        if y>=h-3:
            if top[0] in ("minecraft:sand","minecraft:gravel"): return block("sandstone") if top[0].endswith("sand") else block("gravel")
            if top[0] in ("minecraft:stone","minecraft:andesite","minecraft:snow_block"): return block("stone")
            return block("dirt")
        if y<0: return block("deepslate",axis="y")
        return block("stone")

    def top_y(self,x,z):
        h=self.height(x,z)
        if self.kind(x,z)=="ocean": h=max(h,64)
        return h

    def add_caves_and_resources(self):
        ore_blocks={"coal":"coal_ore","iron":"iron_ore","copper":"copper_ore","gold":"gold_ore","redstone":"redstone_ore","lapis":"lapis_ore","diamond":"diamond_ore","emerald":"emerald_ore"}
        for item in self.candidate["ecology"]["instances"]:
            x,z=item["pos"]
            if item["category"] in ("ordinary_geology","exceptional_geology"):
                name=ore_blocks[item["type"]]; exposure=item.get("exposure","buried")
                depth={"exposed":2,"cave_linked":5,"buried":10}[exposure]; cy=max(-48,self.height(x,z)-depth)
                count=max(3,round(item.get("relative_concentration",.7)*8))
                rng=random.Random(self.seed^sum(ord(c) for c in item["id"]))
                for _ in range(count): self.set_block(x+rng.randint(-2,2),cy+rng.randint(-1,1),z+rng.randint(-2,2),block(name))
            if item["category"]=="geological_opportunity" or (item["category"]=="homeland_baseline" and item["type"]=="cave"):
                length=min(55,max(18,item.get("internal_extent",25)//2)); direction=-1 if x<0 else 1; surface=self.height(x,z); y=surface-3
                for step in range(length):
                    tx=x+direction*step; tz=z+round(math.sin(step/6)*2)
                    for dy in range(0,3):
                        for dz in range(-1,2): self.set_block(tx,y+dy,tz+dz,AIR)
                self.corrections.append({"stage":"cave_physicalization","id":item["id"],"tunnel_length_blocks":length,"reason":"2D cave opportunity translated to a bounded inspectable tunnel; full cave topology remains unresolved"})

    def add_formations_and_crops(self):
        for item in self.candidate["ecology"]["instances"]:
            x,z=item["pos"]
            if item["category"]=="formation":
                radius=max(3,round(math.sqrt(item.get("area",100)/math.pi)))
                state=block({"sand":"sand","gravel":"gravel","snow":"snow_block"}[item["type"]])
                for dz in range(-radius,radius+1):
                    for dx in range(-radius,radius+1):
                        if dx*dx+dz*dz<=radius*radius*(.7+.3*hash01(self.seed,x+dx,z+dz)) and self.in_bounds(x+dx,z+dz):
                            self.surface_override[(x+dx,z+dz)]=state
            if item["category"]=="crop_starter":
                crop={"wheat":"wheat","carrot":"carrots","potato":"potatoes"}[item["type"]]; cy=self.height(x,z)
                for dz in range(-2,3):
                    for dx in range(-3,4):
                        tx,tz=x+dx,z+dz
                        if not self.in_bounds(tx,tz): continue
                        self.set_height(tx,tz,cy); self.surface_override[(tx,tz)]=block("farmland",moisture="7")
                        self.set_block(tx,cy+1,tz,block(crop,age="7"))
                        self.protected.add((tx,tz))
                self.set_block(x,cy,z,block("water",level="0"))

    def add_structures(self):
        for item in self.candidate["ecology"]["instances"]:
            x,z=item["pos"]; y=self.height(x,z)
            if item["category"]=="settlement": self._village(item,x,y,z)
            elif item["category"] in ("major_poi","minor_poi"): self._poi(item,x,y,z)

    def _village(self,item,x,y,z):
        for dx,dz in ((-8,-6),(7,-5),(-1,8)):
            bx,bz=x+dx,z+dz; by=self.height(bx,bz)
            for zz in range(bz-2,bz+3):
                for xx in range(bx-2,bx+3):
                    self.set_block(xx,by,zz,block("cobblestone"))
                    for yy in range(by+1,by+4): self.set_block(xx,yy,zz,block("oak_planks") if xx in (bx-2,bx+2) or zz in (bz-2,bz+2) else AIR)
                    self.set_block(xx,by+4,zz,block("oak_slab",type="bottom",waterlogged="false"))
            self.set_block(bx,by+1,bz-2,AIR); self.set_block(bx,by+2,bz-2,AIR)
            self.set_block(bx-1,by+1,bz+1,block("white_bed",part="foot",facing="south",occupied="false"))
            self.set_block(bx-1,by+1,bz+2,block("white_bed",part="head",facing="south",occupied="false"))
        for dx in range(-4,5): self.surface_override[(x+dx,z)]=block("dirt_path")
        for j in range(2): self.entities.append(Entity(f"{item['id']}-villager-{j}","minecraft:villager",x+j*2+.5,y+1,z+.5,{"villager":True}))

    def _poi(self,item,x,y,z):
        kind=item["type"]
        if kind=="ruined_portal":
            for dy in range(1,6):
                self.set_block(x-2,y+dy,z,block("obsidian")); self.set_block(x+2,y+dy,z,block("crying_obsidian") if dy==3 else block("obsidian"))
            for dx in range(-2,3): self.set_block(x+dx,y+5,z,block("obsidian"))
            for dx in range(-4,5):
                for dz in range(-3,4):
                    if hash01(self.seed,x+dx,z+dz)>.7: self.surface_override[(x+dx,z+dz)]=block("netherrack")
        elif kind in ("mineshaft_access","monster_room"):
            for dx in range(-2,3):
                for dy in range(1,5): self.set_block(x+dx,y+dy,z,block("oak_log",axis="y") if abs(dx)==2 else AIR)
            for dx in range(-2,3): self.set_block(x+dx,y+4,z,block("oak_log",axis="x"))
        elif kind=="geode":
            for dx,dz,dy in ((0,0,1),(1,0,1),(-1,0,1),(0,1,1),(0,-1,1),(0,0,2)):
                self.set_block(x+dx,y+dy,z+dz,block("amethyst_block") if dy==1 else block("budding_amethyst"))
        elif kind=="trail_ruin":
            for dx in range(-3,4):
                for dz in range(-3,4):
                    if hash01(self.seed,x+dx,z+dz)>.42: self.set_block(x+dx,y+1-(abs(dx+dz)%2),z+dz,block("mossy_cobblestone"))
        elif kind=="desert_well":
            for dx in range(-2,3):
                for dz in range(-2,3): self.set_block(x+dx,y+1,z+dz,block("sandstone") if abs(dx)==2 or abs(dz)==2 else block("water",level="0"))
        elif kind=="fossil":
            for dx in range(-4,5): self.set_block(x+dx,y+1+(abs(dx)%3==0),z,block("bone_block",axis="x"))
        elif kind=="spring":
            for dx,dz in ((0,0),(1,0),(-1,0),(0,1),(0,-1),(1,1)):
                self.set_block(x+dx,y,z+dz,block("water",level="0"))
        else:
            for dx in range(-3,4): self.set_block(x+dx,y+1,z,block("stone") if abs(dx)>1 else AIR)

    def add_trees(self):
        rng=random.Random(self.seed^0xF07E57); candidates=[]
        for z in range(self.z0+3,self.z1-3,2):
            for x in range(self.x0+3,self.x1-3,2):
                if self.kind(x,z)=="forest" and (x,z) not in self.protected and hash01(self.seed^77,x,z)>.76: candidates.append((x,z))
        rng.shuffle(candidates); chosen=[]; occupied=set()
        for x,z in candidates:
            if any((x+dx,z+dz) in occupied for dx in range(-4,5) for dz in range(-4,5)): continue
            if any((x+dx,z+dz) in self.protected for dx in range(-2,3) for dz in range(-2,3)): continue
            chosen.append((x,z)); occupied.add((x,z))
        for index,(x,z) in enumerate(chosen):
            y=self.height(x,z); trunk=4+(hash01(self.seed,x,z)>.62)+(hash01(self.seed^2,x,z)>.82)
            for dy in range(1,trunk+1): self.set_block(x,y+dy,z,block("oak_log",axis="y"))
            for dy in range(trunk-2,trunk+2):
                radius=2 if dy<trunk+1 else 1
                for dx in range(-radius,radius+1):
                    for dz in range(-radius,radius+1):
                        if abs(dx)+abs(dz)<=radius+1 and not (dx==dz==0 and dy<=trunk): self.set_block(x+dx,y+dy,z+dz,block("oak_leaves",distance="1",persistent="true",waterlogged="false"))
            if index%17==0:
                self.set_block(x+2,y+1,z,block("brown_mushroom"))
        return len(chosen)

    def add_animals(self):
        for item in self.candidate["ecology"]["instances"]:
            if item["category"] not in ("livestock","transport_ecology"): continue
            x,z=item["pos"]; population=min(5,item.get("population",3)); kind=f"minecraft:{item['type']}"
            for j in range(population):
                dx=(j%3)*1.5-1.5; dz=(j//3)*1.5
                tx,tz=x+dx,z+dz; self.entities.append(Entity(f"{item['id']}-{j}",kind,tx,self.top_y(round(tx),round(tz))+1,tz,{"persistent":True}))

    def build(self):
        self.apply_homelands(); self.apply_settlement_pads(); self.add_caves_and_resources(); self.add_formations_and_crops(); self.apply_routes(); self.add_structures(); trees=self.add_trees(); self.add_animals()
        return trees


def _palette_tag(states):
    entries=[]
    for name,props in states:
        values={"Name":string(name)}
        if props: values["Properties"]=Tag(COMPOUND,{k:string(v) for k,v in props})
        entries.append(Tag(COMPOUND,values))
    return list_tag(COMPOUND,entries)


def _pack(values,bits):
    per=64//bits; out=[]; mask=(1<<bits)-1
    for start in range(0,len(values),per):
        word=0
        for offset,value in enumerate(values[start:start+per]): word|=(value&mask)<<(offset*bits)
        out.append(word)
    return out


def _block_states_tag(states):
    palette=[]; indices=[]; lookup={}
    for state in states:
        if state not in lookup: lookup[state]=len(palette); palette.append(state)
        indices.append(lookup[state])
    values={"palette":_palette_tag(palette)}
    if len(palette)>1: values["data"]=long_array(_pack(indices,max(4,(len(palette)-1).bit_length())))
    return Tag(COMPOUND,values)


_UNIFORM_BLOCK_STATES={}
def _uniform_block_states(state):
    if state not in _UNIFORM_BLOCK_STATES: _UNIFORM_BLOCK_STATES[state]=Tag(COMPOUND,{"palette":_palette_tag([state])})
    return _UNIFORM_BLOCK_STATES[state]


def _biome_for(model,cx,cz):
    counts=Counter(model.kind(x,z) for z in range(cz*16,cz*16+16) for x in range(cx*16,cx*16+16))
    kind=counts.most_common(1)[0][0]
    return {"ocean":"minecraft:ocean","coast":"minecraft:beach","snow":"minecraft:snowy_slopes","forest":"minecraft:forest"}.get(kind,"minecraft:plains")


def _heightmap(model,cx,cz):
    values=[]
    for z in range(cz*16,cz*16+16):
        for x in range(cx*16,cx*16+16): values.append(model.top_y(x,z)+1-MIN_Y)
    return long_array(_pack(values,9))


def build_chunk(model,cx,cz):
    sections=[]; biome=_biome_for(model,cx,cz)
    tops=[model.top_y(x,z) for z in range(cz*16,cz*16+16) for x in range(cx*16,cx*16+16)]
    for sy in range(MIN_Y//16,MAX_Y//16+1):
        has_mod=(cx,cz,sy) in model.mod_sections
        if sy in (-3,-2,-1) and not has_mod: block_states=_uniform_block_states(block("deepslate",axis="y"))
        elif sy in (0,1,2) and not has_mod: block_states=_uniform_block_states(block("stone"))
        elif sy*16>max(tops)+8 and not has_mod: block_states=_uniform_block_states(AIR)
        else: block_states=_block_states_tag([model.base_block(cx*16+x,sy*16+y,cz*16+z) for y in range(16) for z in range(16) for x in range(16)])
        section={"Y":byte(sy),"block_states":block_states,"biomes":compound(palette=list_tag(8,[string(biome)]))}
        sections.append(Tag(COMPOUND,section))
    hm=_heightmap(model,cx,cz)
    return compound(DataVersion=integer(DATA_VERSION),xPos=integer(cx),zPos=integer(cz),yPos=integer(MIN_Y//16),Status=string("minecraft:full"),LastUpdate=long(0),InhabitedTime=long(0),isLightOn=byte(0),sections=list_tag(COMPOUND,sections),Heightmaps=Tag(COMPOUND,{"WORLD_SURFACE":hm,"MOTION_BLOCKING":hm,"MOTION_BLOCKING_NO_LEAVES":hm,"OCEAN_FLOOR":hm}),block_entities=list_tag(COMPOUND,[]),block_ticks=list_tag(COMPOUND,[]),fluid_ticks=list_tag(COMPOUND,[]),PostProcessing=list_tag(LIST,[list_tag(SHORT,[]) for _ in range(24)]),structures=Tag(COMPOUND,{"starts":compound(),"References":compound()}))


def _uuid_ints(identifier):
    value=uuid.uuid5(uuid.NAMESPACE_DNS,"minecraftmoba:"+identifier).int
    parts=[(value>>shift)&0xFFFFFFFF for shift in (96,64,32,0)]
    return [p-(1<<32) if p>=1<<31 else p for p in parts]


def entity_tag(entity):
    values={"id":string(entity.kind),"Pos":list_tag(DOUBLE,[double(entity.x),double(entity.y),double(entity.z)]),"Motion":list_tag(DOUBLE,[double(0),double(0),double(0)]),"Rotation":list_tag(FLOAT,[float_tag(0),float_tag(0)]),"UUID":int_array(_uuid_ints(entity.identifier)),"OnGround":byte(1),"Invulnerable":byte(1),"Silent":byte(0),"NoAI":byte(0)}
    if entity.extra.get("persistent"): values["PersistenceRequired"]=byte(1)
    if entity.extra.get("villager"): values["VillagerData"]=compound(type=string("minecraft:plains"),profession=string("minecraft:none"),level=integer(1))
    return Tag(COMPOUND,values)


def write_level_dat(template: Path,out: Path,model: WorldModel,name: str):
    from .nbt import load_gzip
    root_name,root=load_gzip(template); data=root.value["Data"].value
    data["LevelName"]=string(name); data["DataVersion"]=integer(DATA_VERSION); data["GameType"]=integer(1); data["allowCommands"]=byte(1); data["hardcore"]=byte(0); data["initialized"]=byte(1); data["LastPlayed"]=long(0); data["Time"]=long(6000); data["DayTime"]=long(6000); data["raining"]=byte(0); data["thundering"]=byte(0)
    sx,sz=model.candidate["homelands"]["north"]; sy=model.top_y(sx,sz)+1
    data["spawn"]=compound(pos=int_array([sx,sy,sz]),pitch=float_tag(0),dimension=string("minecraft:overworld"),yaw=float_tag(0))
    data["WorldGenSettings"].value["seed"]=long(model.seed)
    dump_gzip(out,root_name,root)


def write_world(candidate_path: Path,template_level_dat: Path,outdir: Path,name: str):
    candidate=json.loads(candidate_path.read_text()); model=WorldModel(candidate); trees=model.build()
    if outdir.exists(): shutil.rmtree(outdir)
    (outdir/"region").mkdir(parents=True); (outdir/"entities").mkdir()
    write_level_dat(template_level_dat,outdir/"level.dat",model,name)
    for rz in range(model.min_cz//32,model.max_cz//32+1):
        for rx in range(model.min_cx//32,model.max_cx//32+1):
            chunks={}
            for cz in range(max(model.min_cz,rz*32),min(model.max_cz,(rz+1)*32-1)+1):
                for cx in range(max(model.min_cx,rx*32),min(model.max_cx,(rx+1)*32-1)+1): chunks[(cx,cz)]=( "",build_chunk(model,cx,cz))
            write_region(outdir/"region"/f"r.{rx}.{rz}.mca",chunks)
    entity_regions=defaultdict(dict); by_chunk=defaultdict(list)
    for entity in model.entities: by_chunk[(math.floor(entity.x/16),math.floor(entity.z/16))].append(entity)
    for (cx,cz),entities in by_chunk.items():
        root=compound(DataVersion=integer(DATA_VERSION),Position=int_array([cx,cz]),Entities=list_tag(COMPOUND,[entity_tag(e) for e in entities]))
        entity_regions[(cx//32,cz//32)][(cx,cz)]=( "",root)
    for (rx,rz),chunks in entity_regions.items(): write_region(outdir/"entities"/f"r.{rx}.{rz}.mca",chunks)
    candidate_source=str(candidate_path.resolve().relative_to(REPOSITORY_ROOT))
    manifest={"schema_version":1,"minecraft_java_version":VERSION_NAME,"data_version":DATA_VERSION,"serialization_method":"direct modern Anvil/NBT with Mojang-server-derived level.dat template","candidate_source":candidate_source,"seed":model.seed,"world_name":name,"bounds":{"x":[model.x0,model.x1],"z":[model.z0,model.z1],"chunk_x":[model.min_cx,model.max_cx],"chunk_z":[model.min_cz,model.max_cz],"y":[MIN_Y,MAX_Y]},"expected_chunks":(model.max_cx-model.min_cx+1)*(model.max_cz-model.min_cz+1),"spawn":{"x":0,"y":model.top_y(0,-365)+1,"z":-365,"mode":"creative; commands enabled"},"trees":trees,"entities":len(model.entities),"deterministic_block_corrections":model.corrections,"route_terrain_observations":model.route_observations}
    (outdir/"serialization_manifest.json").write_text(json.dumps(manifest,indent=2)+"\n")
    landmarks={"spawn":manifest["spawn"],"homelands":candidate["homelands"],"mountain_feature_markers":candidate["terrain"]["mountain_feature_markers"],"hydrology":[{"source":x["source"],"mouth":x["mouth"]} for x in candidate["terrain"]["hydrology"]],"villages_and_pois":[{"id":x["id"],"type":x["type"],"pos":x["pos"]} for x in candidate["ecology"]["instances"] if x["category"] in ("settlement","major_poi","minor_poi")],"routes":[{"id":x["id"],"target":x["target"]} for x in candidate["routes"]["branches"]]}
    (outdir/"INSPECTION.json").write_text(json.dumps(landmarks,indent=2)+"\n")
    return model,manifest
