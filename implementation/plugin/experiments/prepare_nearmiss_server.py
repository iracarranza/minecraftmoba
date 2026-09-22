"""Prepare an isolated fixture server. Never touches an existing installation.
Requires PyYAML 6.0.3; gameplay values come from the pinned shipped config.
"""
import argparse
import hashlib
import json
import shutil
import zipfile
from pathlib import Path
import yaml

p = argparse.ArgumentParser(description=__doc__)
for k in ('server', 'runtime', 'jar', 'config', 'base', 'report'):
    p.add_argument('--'+k, type=Path, required=True)
a = p.parse_args()
for k,v in vars(a).items(): setattr(a,k,v.resolve())
if a.server.exists(): raise SystemExit('Refusing existing server directory')
a.server.mkdir(parents=True)
for n in ('paper.jar','eula.txt'):
    shutil.copyfile(a.runtime/n,a.server/n)
for n in ('libraries','versions','cache'):
    if (a.runtime/n).exists(): shutil.copytree(a.runtime/n,a.server/n)
plugins=a.server/'plugins'; plugins.mkdir()
cfg=yaml.safe_load(a.config.read_text()); original=yaml.safe_load(a.config.read_text())
build=json.loads((a.report/'build.json').read_text())
portfolio=json.loads((a.report/'portfolio.json').read_text())
name='nearmiss_match'
cfg['alpha']['templatePath']=str(a.base)
cfg['alpha']['instanceWorldName']=name
cfg['alpha']['configurations'].update(directory=str(a.server/'maps'),include=['resource_light'],force='resource_light')
cfg['alpha']['homelands']={t:h['spawn_xzy'] for t,h in build['homelands'].items()}
source_examples={s['type']:s for s in original['renewables']['sources'].values()}
for typ in ('CROP','ANIMAL'):
    values={tuple(s[k] for k in ('radius','capacity','recoverTicks')) for s in original['renewables']['sources'].values() if s['type']==typ}
    if len(values)!=1: raise SystemExit(f'Ambiguous baseline parameters for {typ}: {values}')
sources={}; worksites=[]
kinds={'potato':'potatoes','carrot':'carrots','beetroot':'beetroot','wheat':'wheat'}
for i,site in enumerate(portfolio['placements']):
    x,y,z=site['world_xyz']; kind=site['kind']
    if kind=='mining_worksite':
        worksites.append(dict(id=f'ws_{i}',xyz=[x,y,z],bias=site['bias'] or 'neutral'))
    if kind not in ('founder_crop','renewable_range'): continue
    typ='CROP' if kind=='founder_crop' else 'ANIMAL'
    detail=kinds[site['detail']] if typ=='CROP' else site['detail']
    example=source_examples[typ]
    sources[f'{detail}_{i}']=dict(world=name,type=typ,kind=detail,x=x,y=y,z=z,
        **{k:example[k] for k in ('radius','capacity','recoverTicks')})
cfg['renewables']['sources']=sources
cfg['alpha']['worksites']['sites']=worksites
# Resource pack remains enabled, but use the already published local pack bytes.
# This is a presentation dependency, not a gameplay change.
pack=a.runtime/'pack'
if pack.exists(): shutil.copytree(pack,a.server/'pack')
(plugins/'MinecraftMoba').mkdir()
config_text=yaml.safe_dump(cfg,sort_keys=False)
(plugins/'MinecraftMoba/config.yml').write_text(config_text)
# Bukkit copyDefaults(true) merges the jar's source registry into external
# config. Embed the SAME fixture config; preserve all class bytes and behavior.
with zipfile.ZipFile(a.jar) as src, zipfile.ZipFile(plugins/a.jar.name,'w') as dst:
    for entry in src.infolist():
        dst.writestr(entry, config_text.encode() if entry.filename=='config.yml' else src.read(entry.filename))
with zipfile.ZipFile(a.jar) as src, zipfile.ZipFile(plugins/a.jar.name) as dst:
    assert all(src.read(n)==dst.read(n) for n in src.namelist() if n!='config.yml')
(a.server/'maps').mkdir(); shutil.copyfile(a.report/'resource_light.json.gz',a.server/'maps/resource_light.json.gz')
props=dict(line.split('=',1) for line in (a.runtime/'server.properties').read_text().splitlines() if line and not line.startswith('#') and '=' in line)
props.update({'server-ip':'127.0.0.1','server-port':'25639','level-seed':'930010639','enable-rcon':'false','enable-query':'false','pause-when-empty-seconds':'-1'})
(a.server/'server.properties').write_text('\n'.join(k+'='+v for k,v in props.items())+'\n')
def flattened(d,prefix=''):
    out={}
    for k,v in d.items():
        key=f'{prefix}.{k}' if prefix else str(k)
        if isinstance(v,dict): out.update(flattened(v,key))
        else: out[key]=v
    return out
before,after=flattened(original),flattened(cfg)
changes={k:{'before':before.get(k),'after':after.get(k)} for k in before.keys()|after.keys() if before.get(k)!=after.get(k)}
(a.report/'server-config.yml').write_text(yaml.safe_dump(cfg,sort_keys=False))
(a.report/'server-preparation.json').write_text(json.dumps(dict(server=str(a.server),
    sources=sources,worksites=worksites,config_changes=changes,
    task_b_wheat_sources=[k for k,v in sources.items() if v['kind']=='wheat'],
    sha256={str(f):hashlib.sha256(f.read_bytes()).hexdigest() for f in [a.jar,plugins/a.jar.name,a.runtime/'paper.jar',a.config,a.base/'level.dat']}),indent=2,sort_keys=True)+'\n')
print('Prepared',a.server,'sources',len(sources),'worksites',len(worksites))
