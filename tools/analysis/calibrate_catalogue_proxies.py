"""Compare native proxies with cached vanilla columns at identical X/Z coordinates."""
import argparse,concurrent.futures,json,subprocess
from collections import Counter
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'implementation/worldgen/reports/catalogue_proxy_calibration_2026-10-14'
ART=ROOT/'artifacts/worldgen/catalogue_proxy_calibration_2026-10-14'
BIN=ROOT/'artifacts/worldgen/pale_gameplay_2026-10-12/build/pale_gameplay_probe'
OCEAN=[0,10,24,44,45,46,47,48,49,50]
def summarize(pred_height,actual_height,pred_water,actual_water):
 diff=pred_height.astype(float)-actual_height;dry=(~pred_water)&(~actual_water)
 return {'samples':int(diff.size),'height_mean_bias_y':float(diff.mean()),'height_median_absolute_error_y':float(np.median(np.abs(diff))),'height_p90_absolute_error_y':float(np.quantile(np.abs(diff),.9)),'joint_dry_samples':int(dry.sum()),'joint_dry_height_median_absolute_error_y':float(np.median(np.abs(diff[dry]))) if dry.any() else None,'water_confusion':{'native_ocean_actual_water':int((pred_water&actual_water).sum()),'native_ocean_actual_dry':int((pred_water&~actual_water).sum()),'native_land_actual_water':int((~pred_water&actual_water).sum()),'native_land_actual_dry':int((~pred_water&~actual_water).sum())}}
def process(path,step):
 ex=json.loads(path.read_text());rows=ex['rows'];h=len(rows);w=len(rows[0]);spacing=rows[0][1]['x']-rows[0][0]['x'];stride=step//spacing
 if h!=w or step%spacing or (w*spacing)%step:return None
 width=w*spacing;seed=int(path.parent.name);cx=rows[0][0]['x']+width//2;cz=rows[0][0]['z']+width//2;n=width//step
 key=f'{seed}-{rows[0][0]["x"]}-{rows[0][0]["z"]}-{width}';p=ART/f'{key}.bin'
 if not p.exists():subprocess.run([str(BIN),str(seed),str(cx),str(cz),str(width//2),str(step),str(p)],check=True,timeout=90)
 with p.open('rb') as f:header=f.readline().decode().strip();native=np.frombuffer(f.read(),np.int16).reshape(n,n,2)
 rr=[row[::stride] for row in rows[::stride]];x=np.array([[q['x'] for q in row] for row in rr]);z=np.array([[q['z'] for q in row] for row in rr]);assert np.array_equal(x[0],cx-width//2+np.arange(n)*step);assert np.array_equal(z[:,0],cz-width//2+np.arange(n)*step)
 actual_h=np.array([[q['terrain_y'] for q in row] for row in rr]);actual_water=np.array([[q['actual_surface_water'] for q in row] for row in rr],bool);pred_water=np.isin(native[:,:,1],OCEAN);canopy=np.array([[q.get('canopy_overhead',False) for q in row] for row in rr],bool);biome=np.array([[q['biome'] for q in row] for row in rr]);surf=np.array([[q['surface_y'] for q in row] for row in rr]);dest=OUT/'paired-samples'/f'{key}.npz';np.savez_compressed(dest,x=x,z=z,native_height=native[:,:,0],native_biome=native[:,:,1],native_ocean=pred_water,actual_ground_height=actual_h,actual_surface_water=actual_water,actual_surface_height=surf,actual_canopy=canopy,actual_biome=biome)
 d=summarize(native[:,:,0],actual_h,pred_water,actual_water);d['native_non_ocean_actual_water_by_actual_biome']=dict(Counter(biome[(~pred_water)&actual_water].ravel().tolist()));d['height_estimate_semantics']='Compared to extracted terrain_y, which can be the water surface on wet columns; joint-dry metrics are the direct ground comparison';d.update(seed=seed,source_surface=str(path),native_probe=str(p),probe_header=header,paired_samples=str(dest),coordinate_alignment='exact same X/Z; source grids generally start four blocks inside chunk boundary, so native query center includes that offset',native_biome_y=319,actual_biome_y='source surface column sampling height',sample_spacing=step)
 return d
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--step',type=int,default=64);a=p.parse_args();OUT.mkdir(parents=True,exist_ok=True);ART.mkdir(parents=True,exist_ok=True);(OUT/'paired-samples').mkdir(exist_ok=True)
 base=ROOT/'implementation/worldgen/reports';paths=sorted(list((base/'archipelago_qualified_search_2026-10-12/generated').glob('*/surface.json'))+list((base/'archipelago_gameplay_2026-10-12').glob('*/surface.json')))
 with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:results=[r for r in pool.map(lambda path:process(path,a.step),paths) if r]
 arrays=[]
 for r in results:
  with np.load(r['paired_samples'],allow_pickle=False) as d:arrays.append({k:d[k].ravel() for k in ['native_height','actual_ground_height','native_ocean','actual_surface_water']})
 agg=summarize(*[np.concatenate([q[k] for q in arrays]) for k in ['native_height','actual_ground_height','native_ocean','actual_surface_water']]) if arrays else {}
 agg['native_non_ocean_actual_water_by_actual_biome']=dict(sum((Counter(r['native_non_ocean_actual_water_by_actual_biome']) for r in results),Counter()))
 result={'status':'Proxy calibration evidence; no candidate acceptance thresholds changed','scope':'Existing actual source worlds, predominantly archipelago prospects; selection-biased sample, not a prevalence estimate','source_grids':len(results),'aggregate':agg,'per_source':results,'limitations':['Native height is approximate, not a generated column.','Ocean biome is a proxy for actual surface water; rivers and above-sea-level ocean-biome terrain can disagree.','Native and actual biome Y differ; the paired record retains that distinction.','No threshold corrections or gameplay claims are inferred from this sample.']};(OUT/'calibration.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps({'source_grids':len(results),'aggregate':agg},indent=2))
