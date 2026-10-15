"""Scheduled continuation: retain live workers, resume stopped cursors, refresh index."""
from __future__ import annotations
import argparse,errno,fcntl,json,os,subprocess,sys,time
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
REPORTS=ROOT/'implementation/worldgen/reports'
OUT=REPORTS/'seed_catalogue_continuation_2026-10-14'
PYTHON=ROOT/'artifacts/worldgen/pale_mansion_search_2026-10-12/build/plot-env/bin/python'
NATIVE=REPORTS/'default_seed_catalogue_2026-10-14'
ACTUAL=REPORTS/'default_actual_catalogue_2026-10-14'
def read(path):
 return json.loads(path.read_text()) if path.exists() else {}
def save(path,data):
 tmp=path.with_suffix(path.suffix+'.tmp');tmp.write_text(json.dumps(data,indent=2)+'\n');tmp.replace(path)
def live(pid):
 if not isinstance(pid,int) or pid<1:return 'missing'
 try:os.kill(pid,0);return 'alive'
 except OSError as e:return 'protected' if e.errno==errno.EPERM else 'missing' if e.errno==errno.ESRCH else 'unknown'
def held(path):
 path.parent.mkdir(parents=True,exist_ok=True)
 with path.open('a') as lock:
  try:fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
  except BlockingIOError:return True
  fcntl.flock(lock,fcntl.LOCK_UN);return False
def native_status():
 cp=read(NATIVE/'checkpoint.json');meta=read(NATIVE/'worker-launch.json');pid=cp.get('worker_pid') or meta.get('pid');process=live(pid);locked=held(NATIVE/'worker.lock')
 active=locked or process in ['alive','protected','unknown'];stop=(NATIVE/'STOP').exists();budget=cp.get('stop_reason')=='storage_budget' or cp.get('retained_bytes_estimate',0)>=cp.get('max_storage_bytes',4*1024**3)
 return {'pid':pid,'process':process,'lock_held':locked,'active_or_protected':active,'stop_requested':stop,'storage_budget_reached':budget,'failure':cp.get('failure'),'safe_to_start':not active and not stop and not budget and not cp.get('failure'),'checkpoint':str(NATIVE/'checkpoint.json'),'next_seed':cp.get('next_seed',910000001),'counts':{k:cp.get(k) for k in ['unique_seeds_measured','distinct_regions_measured','orientations_measured']}}
def run(command):
 value=subprocess.run(command,cwd=ROOT,capture_output=True,text=True,timeout=120)
 if value.returncode:raise RuntimeError(f'{command[1:]} returned {value.returncode}: {value.stderr[-2000:]}')
 try:return json.loads(value.stdout.strip().splitlines()[-1])
 except (json.JSONDecodeError,IndexError):return {'output':value.stdout[-4000:]}
def coordinate(hours,ensure):
 OUT.mkdir(parents=True,exist_ok=True)
 with (OUT/'coordinator.lock').open('a') as guard:
  try:fcntl.flock(guard,fcntl.LOCK_EX|fcntl.LOCK_NB)
  except BlockingIOError:return {'status':'another_coordinator_is_running'}
  native=native_status();result={'status':'checked','time_unix':time.time(),'mode':'ensure' if ensure else 'status','native_before':native,'actual_before':run([str(PYTHON),'tools/analysis/catalogue_actual_scoops.py','--status'])}
  if ensure:
   if native['safe_to_start']:
    command=[str(PYTHON),'-u','tools/analysis/catalogue_default_seeds.py','--start','910000001','--max-seeds','100000','--hours',str(hours),'--workers','2','--max-storage-gib','4']
    with (NATIVE/'worker.log').open('a') as log:proc=subprocess.Popen(command,cwd=ROOT,stdin=subprocess.DEVNULL,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
    result['native_action']={'action':'resume_requested','pid':proc.pid,'command':command}
   else:result['native_action']={'action':'kept_existing_or_preserved_stop','reason':native}
   result['actual_action']=run([str(PYTHON),'tools/analysis/catalogue_actual_scoops.py','--ensure','--hours',str(hours)])
   result['index_refresh']=run([str(PYTHON),'tools/analysis/index_seed_catalogues.py'])
   result['index_watcher']=run([str(PYTHON),'tools/analysis/index_seed_catalogues.py','--ensure-watch','--watch-seconds',str(int(hours*3600)),'--interval','60'])
   result['status']='continuation_checked'
  result['native_after']=native_status();result['actual_after']=run([str(PYTHON),'tools/analysis/catalogue_actual_scoops.py','--status'])
  if ensure:save(OUT/'last-run.json',result);save(OUT/f'run-{int(time.time()*1000)}.json',result)
  return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--ensure',action='store_true');p.add_argument('--hours',type=float,default=4.5);a=p.parse_args();print(json.dumps(coordinate(a.hours,a.ensure),indent=2))
