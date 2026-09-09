"""Read back serialized Anvil worlds, validate them, and render compact PNGs."""

from __future__ import annotations

import json
import math
import struct
import zlib
from collections import Counter
from pathlib import Path

from successor.grid import unrle

from .nbt import plain
from .region import read_region

AIRLIKE={"minecraft:air","minecraft:cave_air","minecraft:void_air"}
ROUTE={"minecraft:dirt_path","minecraft:gravel","minecraft:oak_planks"}
COLORS={
    "minecraft:grass_block":(93,143,57),"minecraft:podzol":(91,67,40),"minecraft:coarse_dirt":(119,85,59),
    "minecraft:dirt_path":(192,148,91),"minecraft:dirt":(121,85,58),"minecraft:stone":(126,126,126),
    "minecraft:andesite":(137,137,137),"minecraft:gravel":(133,126,124),"minecraft:sand":(219,207,157),
    "minecraft:sandstone":(216,202,153),"minecraft:snow_block":(238,244,244),"minecraft:water":(48,100,185),
    "minecraft:oak_planks":(183,145,83),"minecraft:oak_log":(106,83,50),"minecraft:oak_leaves":(53,112,47),
    "minecraft:oak_slab":(190,153,91),"minecraft:white_bed":(235,235,235),"minecraft:bone_block":(220,216,188),
    "minecraft:cobblestone":(105,105,105),"minecraft:mossy_cobblestone":(91,111,77),"minecraft:netherrack":(113,54,52),
    "minecraft:obsidian":(42,30,57),"minecraft:crying_obsidian":(75,33,99),"minecraft:amethyst_block":(133,96,181),
    "minecraft:farmland":(112,74,43),"minecraft:wheat":(197,166,49),"minecraft:carrots":(74,143,43),"minecraft:potatoes":(91,141,48),
}


class Chunk:
    def __init__(self,root):
        p=plain(root); self.x=p["xPos"]; self.z=p["zPos"]; self.sections={}
        self.heightmaps=p.get("Heightmaps",{})
        for section in p["sections"]:
            bs=section["block_states"]; palette=[]
            for entry in bs["palette"]:
                palette.append((entry["Name"],tuple(sorted(entry.get("Properties",{}).items()))))
            self.sections[section["Y"]]=(palette,bs.get("data"))

    def state(self,x,y,z):
        section=self.sections.get(y//16)
        if not section: return ("minecraft:air",())
        palette,data=section
        if data is None: return palette[0]
        bits=max(4,(len(palette)-1).bit_length()); per=64//bits; i=(y&15)*256+(z&15)*16+(x&15)
        word=data[i//per] & ((1<<64)-1); index=(word>>((i%per)*bits))&((1<<bits)-1)
        return palette[index]

    def encoded_height(self,x,z):
        data=self.heightmaps["WORLD_SURFACE"]; i=(z&15)*16+(x&15); per=64//9
        word=data[i//per]&((1<<64)-1); return ((word>>((i%per)*9))&511)-64-1

    def visual_top(self,x,z):
        for sy in sorted(self.sections,reverse=True):
            palette,data=self.sections[sy]
            if len(palette)==1 and palette[0][0] in AIRLIKE: continue
            for ly in range(15,-1,-1):
                state=self.state(x,sy*16+ly,z)
                if state[0] not in AIRLIKE: return sy*16+ly,state
        return -64,("minecraft:bedrock",())


class WorldReader:
    def __init__(self,world):
        self.world=Path(world); self.chunks={}; self.entities=[]; self.block_counts=Counter(); self._visual_cache={}
        for path in sorted((self.world/"region").glob("*.mca")):
            for cx,cz,_,root in read_region(path):
                chunk=Chunk(root); self.chunks[(cx,cz)]=chunk
                for palette,data in chunk.sections.values():
                    if data is None: self.block_counts[palette[0][0]]+=4096
                    else:
                        bits=max(4,(len(palette)-1).bit_length()); per=64//bits; mask=(1<<bits)-1
                        counts=[0]*len(palette); remaining=4096
                        for signed in data:
                            word=signed&((1<<64)-1); take=min(per,remaining)
                            for offset in range(take): counts[(word>>(offset*bits))&mask]+=1
                            remaining-=take
                            if not remaining: break
                        for index,count in enumerate(counts): self.block_counts[palette[index][0]]+=count
        for path in sorted((self.world/"entities").glob("*.mca")):
            if path.stat().st_size<8192: continue
            for _,_,_,root in read_region(path): self.entities.extend(plain(root).get("Entities",[]))

    def chunk(self,x,z): return self.chunks.get((math.floor(x/16),math.floor(z/16)))
    def state(self,x,y,z):
        chunk=self.chunk(x,z); return chunk.state(x,y,z) if chunk else ("minecraft:air",())
    def height(self,x,z): return self.chunk(x,z).encoded_height(x,z)
    def visual_top(self,x,z):
        key=(x,z)
        if key not in self._visual_cache: self._visual_cache[key]=self.chunk(x,z).visual_top(x,z)
        return self._visual_cache[key]
    def water_near(self,x,z,radius=2):
        found=[]
        for dz in range(-radius,radius+1):
            for dx in range(-radius,radius+1):
                for y in range(125,-63,-1):
                    if self.state(x+dx,y,z+dz)[0]=="minecraft:water": found.append(y); break
        return max(found) if found else None


def _line_points(a,b):
    x0,z0=a; x1,z1=b; n=max(abs(x1-x0),abs(z1-z0),1)
    return [(round(x0+(x1-x0)*i/n),round(z0+(z1-z0)*i/n)) for i in range(n+1)]


def validate_world(world,candidate_path):
    world=Path(world); candidate=json.loads(Path(candidate_path).read_text()); manifest=json.loads((world/"serialization_manifest.json").read_text()); reader=WorldReader(world)
    expected=manifest["expected_chunks"]; chunk_count=len(reader.chunks)
    sx,sy,sz=manifest["spawn"]["x"],manifest["spawn"]["y"],manifest["spawn"]["z"]
    spawn_ok=reader.state(sx,sy-1,sz)[0] not in AIRLIKE|{"minecraft:water"} and reader.state(sx,sy,sz)[0] in AIRLIKE and reader.state(sx,sy+1,sz)[0] in AIRLIKE
    heights=[reader.height(x,z) for z in range(manifest["bounds"]["z"][0],manifest["bounds"]["z"][1]+1) for x in range(manifest["bounds"]["x"][0],manifest["bounds"]["x"][1]+1)]
    wall8=wall12=0; width=manifest["bounds"]["x"][1]-manifest["bounds"]["x"][0]+1
    for i,h in enumerate(heights):
        if i%width and abs(h-heights[i-1])>8: wall8+=1
        if i>=width and abs(h-heights[i-width])>8: wall8+=1
        if i%width and abs(h-heights[i-1])>12: wall12+=1
        if i>=width and abs(h-heights[i-width])>12: wall12+=1
    route_results={}
    for branch in candidate["routes"]["branches"]:
        points=[]
        for a,b in zip(branch["centerline"],branch["centerline"][1:]): points.extend(_line_points(a,b))
        hits=0
        for x,z in points:
            hit=False
            for dz in range(-2,3):
                for dx in range(-2,3):
                    top=reader.visual_top(x+dx,z+dz)[1][0]
                    if top in ROUTE: hit=True; break
                if hit: break
            hits+=hit
        route_results[branch["id"]]={"sample_columns":len(points),"physical_route_coverage":round(hits/max(1,len(points)),4),"pass":hits/max(1,len(points))>=.97}
    hydro=[]
    for index,feature in enumerate(candidate["terrain"]["hydrology"],1):
        samples=[]
        for a,b in zip(feature["path"],feature["path"][1:]): samples.extend(_line_points(a,b))
        ys=[reader.water_near(x,z,2) for x,z in samples]; present=[y for y in ys if y is not None]
        rises=sum(b>a+1 for a,b in zip(present,present[1:]))
        hydro.append({"feature":index,"sample_columns":len(samples),"water_coverage":round(len(present)/max(1,len(samples)),4),"uphill_artifacts":rises,"pass":len(present)/max(1,len(samples))>=.96 and rises==0})
    grid=candidate["terrain"]["grid_encoding"]; analytic=unrle(grid["height_rle"]); analytic_kind=unrle(grid["terrain_rle"]); preservation=[]
    excluded=[]
    for z in range(-520,521,5):
        for x in range(-420,421,5):
            near_home=min(math.hypot(x, z+365),math.hypot(x,z-365))<60
            near_feature=any(math.hypot(x-i["pos"][0],z-i["pos"][1])<18 for i in candidate["ecology"]["instances"] if i["category"]=="settlement")
            near_route=any(any(abs(x-p[0])+abs(z-p[1])<=8 for p in r["centerline"]) for r in candidate["routes"]["branches"])
            if near_home or near_feature or near_route: excluded.append((x,z)); continue
            ai=((z+520)//5)*grid["nx"]+(x+420)//5; expected_h=analytic[ai]
            if analytic_kind[ai] in ("ocean","water"): excluded.append((x,z)); continue
            actual=reader.height(x,z)
            preservation.append(abs(actual-expected_h))
    required_blocks={"minecraft:coal_ore","minecraft:iron_ore","minecraft:copper_ore","minecraft:gold_ore","minecraft:redstone_ore","minecraft:lapis_ore","minecraft:diamond_ore","minecraft:emerald_ore","minecraft:sand","minecraft:gravel","minecraft:snow_block","minecraft:wheat","minecraft:carrots","minecraft:potatoes","minecraft:water"}
    entity_types=Counter(x.get("id") for x in reader.entities); required_entities={"minecraft:cow","minecraft:sheep","minecraft:pig","minecraft:chicken","minecraft:horse"}
    checks={"expected_chunks":chunk_count==expected,"spawn_valid":spawn_ok,"no_massive_empty_regions":chunk_count==expected and reader.block_counts["minecraft:stone"]>1_000_000,"sane_terrain_heights":min(heights)>=48 and max(heights)<=125,"ocean_and_coast":reader.block_counts["minecraft:water"]>10000 and reader.block_counts["minecraft:sand"]>1000,"western_mountain_depth":max(reader.height(x,0) for x in range(-420,-100))-reader.height(-100,0)>=8,"six_routes_connected":len(route_results)==6 and all(x["pass"] for x in route_results.values()),"resource_vocabulary":required_blocks<=set(reader.block_counts),"livestock_and_horses":required_entities<=set(entity_types),"drainage_connected":all(x["pass"] for x in hydro),"no_serialization_walls":wall12==0,"candidate_geometry_preserved":max(preservation,default=0)<=2}
    result={"schema_version":1,"world":manifest["world_name"],"minecraft_java_version":manifest["minecraft_java_version"],"pass":all(checks.values()),"checks":checks,"metrics":{"chunks":chunk_count,"expected_chunks":expected,"height_min":min(heights),"height_max":max(heights),"adjacent_edges_gt8":wall8,"adjacent_edges_gt12":wall12,"geometry_anchor_max_delta":max(preservation,default=0),"geometry_anchor_mean_delta":round(sum(preservation)/max(1,len(preservation)),3),"excluded_corrected_anchor_count":len(excluded),"block_counts":dict(reader.block_counts),"entity_counts":dict(entity_types)},"routes":route_results,"hydrology":hydro,"manifest_corrections":manifest["deterministic_block_corrections"]}
    (world/"validation_report.json").write_text(json.dumps(result,indent=2)+"\n")
    return reader,result


def _png(path,width,height,pixels):
    def chunk(kind,data): return struct.pack(">I",len(data))+kind+data+struct.pack(">I",zlib.crc32(kind+data)&0xffffffff)
    raw=b"".join(b"\x00"+bytes(pixels[y*width*3:(y+1)*width*3]) for y in range(height))
    path.write_bytes(b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",struct.pack(">IIBBBBB",width,height,8,2,0,0,0))+chunk(b"IDAT",zlib.compress(raw,9))+chunk(b"IEND",b""))


def _shade(color,height):
    f=.76+min(.30,max(0,(height-58)/180)); return tuple(max(0,min(255,round(c*f))) for c in color)


def render_overhead(reader,manifest,path):
    x0,x1=manifest["bounds"]["x"]; z0,z1=manifest["bounds"]["z"]; width=x1-x0+1; height=z1-z0+1; pixels=[]
    for z in range(z0,z1+1):
        for x in range(x0,x1+1):
            y,state=reader.visual_top(x,z); color=COLORS.get(state[0],(116,112,102)); pixels.extend(_shade(color,y))
    _png(path,width,height,pixels)


def render_oblique(reader,path,bounds,width=900,height=520,stride=2):
    x0,x1,z0,z1=bounds; points=[]
    for z in range(z0,z1+1,stride):
        for x in range(x0,x1+1,stride):
            y,state=reader.visual_top(x,z); u=x-z; v=(x+z)*.38-y*2.4; points.append((x+z,u,v,y,state[0]))
    u0=min(p[1] for p in points); u1=max(p[1] for p in points); v0=min(p[2] for p in points); v1=max(p[2] for p in points)
    scale=min((width-30)/max(1,u1-u0),(height-30)/max(1,v1-v0)); pixels=[(226 if (i//3//width)%2 else 230) for i in range(width*height*3)]
    def put(px,py,color):
        if 0<=px<width and 0<=py<height:
            i=(py*width+px)*3; pixels[i:i+3]=color
    for _,u,v,y,name in sorted(points):
        px=round(15+(u-u0)*scale); py=round(15+(v-v0)*scale); color=_shade(COLORS.get(name,(116,112,102)),y)
        footprint=max(3,math.ceil(scale*stride*1.5))
        for dy in range(-1,footprint):
            for dx in range(-1,footprint): put(px+dx,py+dy,color)
    _png(path,width,height,pixels)


def render_suite(reader,world,candidate):
    world=Path(world); out=world/"inspection_renders"; out.mkdir(exist_ok=True); manifest=json.loads((world/"serialization_manifest.json").read_text())
    render_overhead(reader,manifest,out/"01_overhead.png")
    forest=candidate["ecology"]["forest_regions"][0]["centroid"]
    route_targets={tuple(r["target"]) for r in candidate["routes"]["branches"]}
    villages=[x["pos"] for x in candidate["ecology"]["instances"] if x["category"]=="settlement"]
    village=next((p for p in villages if tuple(p) in route_targets),villages[0])
    views={"02_western_mountain.png":(-420,-80,-270,270),"03_eastern_coast.png":(100,420,-270,270),"04_north_route_fan.png":(-250,250,-500,-80),"05_forest_interior.png":(forest[0]-150,forest[0]+150,forest[1]-150,forest[1]+150),"06_village_geography.png":(village[0]-120,village[0]+120,village[1]-120,village[1]+120)}
    for name,bounds in views.items(): render_oblique(reader,out/name,bounds)
    return out
