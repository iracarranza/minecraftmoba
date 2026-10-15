"""Analysis-local acceleration of exact vanilla section counts; same 4096 entries."""
from collections import Counter
import math,json,time,sys
from pathlib import Path
import numpy as np

def exact_counts(container):
 palette=container['palette']
 if len(palette)==1:return {palette[0]['Name']:4096}
 bits=max(4,(len(palette)-1).bit_length());per=64//bits;needed=math.ceil(4096/per)
 if len(container['data'])<needed:raise ValueError('truncated block-state section')
 words=np.asarray(container['data'][:needed],dtype=np.int64).view(np.uint64)
 shifts=np.arange(per,dtype=np.uint64)*np.uint64(bits)
 indices=((words[:,None]>>shifts[None,:])&np.uint64((1<<bits)-1)).ravel()[:4096]
 counts=np.bincount(indices.astype(np.int64),minlength=len(palette));result=Counter()
 for i in np.flatnonzero(counts):result[palette[int(i)]['Name']]+=int(counts[i])
 return result

def validate(root):
 sys.path.insert(0,str(root/'implementation/worldgen'))
 from terrain_harvest.opportunity_map import _section_counts as original
 from serialization.region import read_region
 from serialization.nbt import plain
 rng=np.random.default_rng(1211);synthetic=0;real=0;bits_seen=set()
 for length in [1,2,15,16,17,31,32,33,63,64,65,127,128,129,255,256,257,511]:
  palette=[{'Name':f'block:{i%max(1,length-2)}'} for i in range(length)]
  for repetition in range(4):
   if length==1:c={'palette':palette}
   else:
    bits=max(4,(length-1).bit_length());per=64//bits;indices=rng.integers(0,length,4096);data=[]
    for k in range(0,4096,per):
     values=indices[k:k+per];word=sum(int(v)<<(j*bits) for j,v in enumerate(values))
     # Deliberately nonzero padding in the final word must not be counted.
     if len(values)<per:word|=((1<<64)-1)^((1<<(len(values)*bits))-1)
     if word>=1<<63:word-=1<<64
     data.append(word)
    c={'palette':palette,'data':data}
   assert exact_counts(c)==original(c);assert sum(exact_counts(c).values())==4096;synthetic+=1
 world=root/'artifacts/worldgen/archipelago_gameplay_2026-10-12/890000020/server/world'
 for path in sorted((world/'region').glob('*.mca'))[:3]:
  for cx,cz,_,r in read_region(path):
   for section in plain(r)['sections']:
    c=section.get('block_states')
    if not c:continue
    assert exact_counts(c)==original(c);bits_seen.add(max(4,(len(c['palette'])-1).bit_length()));real+=1
   if real>=150:break
  if real>=150:break
 report={'status':'Exact equivalence confirmed','synthetic_sections':synthetic,'actual_vanilla_sections':real,'actual_palette_bits':sorted(bits_seen),'preserves':['All 4096 entries, signed longs, padded packing, final-word padding, repeated block names, singleton palettes','Exact resource/vegetation counts and existing shared gameplay thresholds'],'scope':'Analysis process only; shared compiler and source worlds unchanged'}
 out=root/'implementation/worldgen/reports/archipelago_qualified_search_2026-10-12/exact-count-validation.json';out.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
 return report
if __name__=='__main__':validate(Path(__file__).resolve().parents[2])
