"""Dependency-free review renders of extracted vanilla data and overlays."""

from __future__ import annotations

import struct
import zlib
from pathlib import Path

SCALE=3


def _png(path,width,height,pixels):
    def chunk(kind,data): return struct.pack(">I",len(data))+kind+data+struct.pack(">I",zlib.crc32(kind+data)&0xffffffff)
    raw=b"".join(b"\x00"+bytes(pixels[y*width*3:(y+1)*width*3]) for y in range(height))
    path.write_bytes(b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",struct.pack(">IIBBBBB",width,height,8,2,0,0,0))+chunk(b"IDAT",zlib.compress(raw,9))+chunk(b"IEND",b""))


def _canvas(rows,color):
    h,w=len(rows),len(rows[0]); pixels=[0]*(w*SCALE*h*SCALE*3)
    for z,row in enumerate(rows):
        for x,cell in enumerate(row):
            rgb=color(cell,x,z)
            for dz in range(SCALE):
                for dx in range(SCALE):
                    i=(((z*SCALE+dz)*w*SCALE)+x*SCALE+dx)*3; pixels[i:i+3]=rgb
    return pixels


def _put(pixels,w,h,x,z,color,radius=2):
    cx,cz=x*SCALE+1,z*SCALE+1
    for dz in range(-radius,radius+1):
        for dx in range(-radius,radius+1):
            qx,qz=cx+dx,cz+dz
            if 0<=qx<w*SCALE and 0<=qz<h*SCALE:
                i=(qz*w*SCALE+qx)*3; pixels[i:i+3]=color


def render(candidate,outdir):
    outdir=Path(outdir); outdir.mkdir(parents=True,exist_ok=True); rows=candidate["_rows"]; h,w=len(rows),len(rows[0]); masks=candidate["_masks"]
    def elevation(cell,x,z):
        y=cell["terrain_y"]; east=rows[z][min(w-1,x+1)]["terrain_y"]; south=rows[min(h-1,z+1)][x]["terrain_y"]; light=max(.62,min(1.25,1-(east-y)*.045-(south-y)*.03)); base=(76+min(145,max(0,y-45)*2),105+min(120,max(0,y-45)*2),68+min(140,max(0,y-45)*2)); return tuple(max(0,min(255,round(c*light))) for c in base)
    pixels=_canvas(rows,elevation); _png(outdir/"01_actual_surface_elevation.png",w*SCALE,h*SCALE,pixels)
    def landcover(cell,x,z):
        biome=cell["biome"]
        if cell["actual_surface_water"]: return (36,92,175) if "ocean" in biome else (48,126,190)
        if any(q in biome for q in ("forest","taiga","jungle","grove","wooded","pale_garden","mangrove")): return (38,104,52)
        if any(q in biome for q in ("snow","frozen","ice_spikes","jagged_peaks")): return (224,235,237)
        if any(q in biome for q in ("desert","beach","badlands")): return (211,190,126)
        if any(q in biome for q in ("mountain","peak","slope","stony")): return (132,130,123)
        if "swamp" in biome: return (78,104,61)
        return (105,151,72)
    pixels=_canvas(rows,landcover); _png(outdir/"02_actual_biomes_water_landcover.png",w*SCALE,h*SCALE,pixels)
    def overlay(cell,x,z):
        i=z*w+x
        if cell["actual_surface_water"]: return (45,96,170)
        if masks["highland"][i]: return (139,132,118)
        if masks["forest"][i]: return (43,102,50)
        return (128,158,91) if masks["buildable"][i] else (113,132,86)
    pixels=_canvas(rows,overlay)
    for branch in candidate["routes"]["branches"]:
        for x,z in branch["sample_path"]: _put(pixels,w,h,x,z,(238,190,43),1)
    for team,color in (("north",(42,74,215)),("south",(204,48,50))):
        x,z=candidate["homelands"][team]["logical_sample"]; _put(pixels,w,h,x,z,color,8)
    raw_lookup={(cell["x"],cell["z"]):(x,z) for z,row in enumerate(rows) for x,cell in enumerate(row)}
    for item in candidate["actual_structures"]:
        nearest=min(raw_lookup,key=lambda p:(p[0]-item["pos"][0])**2+(p[1]-item["pos"][1])**2); x,z=raw_lookup[nearest]; _put(pixels,w,h,x,z,(177,65,201),4)
    _png(outdir/"03_analytical_compatibility_overlay.png",w*SCALE,h*SCALE,pixels)
    return [outdir/name for name in ("01_actual_surface_elevation.png","02_actual_biomes_water_landcover.png","03_analytical_compatibility_overlay.png")]
