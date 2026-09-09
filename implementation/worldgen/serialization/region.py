"""Anvil region container writer/reader with zlib-compressed NBT chunks."""

from __future__ import annotations

import math
import struct
import zlib
from pathlib import Path

from .nbt import dumps, loads

SECTOR=4096


def write_region(path: Path, chunks: dict[tuple[int,int],tuple[str,object]]):
    locations=[0]*1024; timestamps=[0]*1024; bodies=[]; sector=2
    for (cx,cz),(name,root) in sorted(chunks.items(),key=lambda item: ((item[0][1]&31)*32+(item[0][0]&31))):
        raw=dumps(name,root); compressed=zlib.compress(raw,6); body=struct.pack(">I",len(compressed)+1)+b"\x02"+compressed
        sectors=math.ceil(len(body)/SECTOR); index=(cx&31)+(cz&31)*32
        locations[index]=(sector<<8)|sectors; timestamps[index]=0
        bodies.append(body+b"\x00"*(sectors*SECTOR-len(body))); sector+=sectors
    header=struct.pack(">1024I",*locations)+struct.pack(">1024I",*timestamps)
    path.parent.mkdir(parents=True,exist_ok=True); path.write_bytes(header+b"".join(bodies))


def read_region(path: Path):
    data=path.read_bytes()
    if len(data)<8192: raise ValueError(f"short region {path}")
    locations=struct.unpack(">1024I",data[:4096])
    for index,location in enumerate(locations):
        if not location: continue
        sector=location>>8; offset=sector*SECTOR; length=struct.unpack(">I",data[offset:offset+4])[0]
        compression=data[offset+4]; payload=data[offset+5:offset+4+length]
        if compression!=2: raise ValueError(f"unsupported region compression {compression}")
        name,root=loads(zlib.decompress(payload)); lx=index%32; lz=index//32
        rx=int(path.stem.split(".")[1]); rz=int(path.stem.split(".")[2])
        yield rx*32+lx,rz*32+lz,name,root
