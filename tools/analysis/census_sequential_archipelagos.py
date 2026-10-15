"""Expand the approximate census without replacing older search evidence."""
import argparse,concurrent.futures,json
from search_archipelago_windows import search,ROOT,save
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--start',type=int,required=True);p.add_argument('--count',type=int,default=1000);a=p.parse_args()
 out=ROOT/'implementation/worldgen/reports/archipelago_sequence_search_2026-10-13';out.mkdir(parents=True,exist_ok=True)
 target=out/'broad-screen.json';rows=json.loads(target.read_text()) if target.exists() else [];known={r['seed'] for r in rows}
 with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
  for k,r in enumerate(pool.map(search,range(a.start,a.start+a.count)),1):
   if r and r['seed'] not in known:rows.append(r);known.add(r['seed'])
   if k%100==0:print('census',k,'eligible total',len(rows),flush=True);save(target,rows)
 save(target,rows);save(out/f'census-{a.start}-{a.count}.json',{'start':a.start,'count':a.count,'windows_per_seed':9,'approximation_only':True})
