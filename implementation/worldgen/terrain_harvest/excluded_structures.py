"""Fail closed on forbidden structures in saved worlds, including underground ones."""
from pathlib import Path
from serialization.nbt import plain
from serialization.region import read_region

def trial_chambers(world):
    findings=[]
    for file in sorted((Path(world)/'region').glob('*.mca')):
        for cx,cz,_,root in read_region(file):
            chunk=plain(root)
            starts=chunk.get('structures',{}).get('starts',{})
            if any('trial_chamber' in key and value.get('id')!='INVALID' for key,value in starts.items()):
                findings.append([cx,cz,'structure_start']); continue
            if any(p.get('Name')=='minecraft:trial_spawner' for section in chunk.get('sections',[]) for p in section.get('block_states',{}).get('palette',[])):
                findings.append([cx,cz,'trial_spawner'])
    return findings
