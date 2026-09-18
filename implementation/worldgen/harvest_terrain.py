#!/usr/bin/env python3
"""Reference first, materialize second. Run --help for the repeatable interface."""
import argparse
import json
from pathlib import Path
from terrain_harvest.library import harvest_corpus,write_library
from terrain_harvest.model import loads
from terrain_harvest.gallery import build_gallery
from terrain_harvest.verify import audit

def main():
    p=argparse.ArgumentParser(description=__doc__);sub=p.add_subparsers(dest='command',required=True)
    ref=sub.add_parser('catalog');ref.add_argument('--repository',type=Path,default=Path(__file__).resolve().parents[2]);ref.add_argument('--output',type=Path,required=True)
    mat=sub.add_parser('gallery');mat.add_argument('--library',type=Path,required=True);mat.add_argument('--output',type=Path,required=True)
    mat.add_argument('--source',action='append',required=True,metavar='SEED=WORLD_DIRECTORY');mat.add_argument('--id',action='append',help='materialize exact ID, repeatable')
    mat.add_argument('--proof',action='store_true',help='two named whole-map finalists and one local scoop (three volumes)')
    check=sub.add_parser('verify');check.add_argument('--gallery',type=Path,required=True);check.add_argument('--source',action='append',required=True);check.add_argument('--report',type=Path,required=True)
    a=p.parse_args()
    if a.command=='verify':
        sources={int(s.split('=',1)[0]):Path(s.split('=',1)[1]) for s in a.source}
        result=audit(a.gallery,sources,a.report);print('PASS' if result['pass'] else 'FAIL');return
    if a.command=='catalog':
        index=write_library(harvest_corpus(a.repository.resolve()),a.output);print(json.dumps(index,indent=2));return
    volumes=[loads(f.read_text()) for f in sorted(a.library.glob('tv_*.json'))]
    if a.proof:
        if a.id:p.error('choose --proof or --id')
        volumes=[v for v in volumes if (v['classification']['kind']=='whole_map' and v['provenance']['source_seed'] in (930010639,930012642)) or (v['classification']['kind']=='local_section' and v['provenance']['source_seed']==930010639)]
        if len(volumes)!=3:p.error('proof expects exactly two named whole-map references and one local section')
    elif a.id:
        wanted=set(a.id);volumes=[v for v in volumes if v['id'] in wanted]
        if {v['id'] for v in volumes}!=wanted:p.error('unknown requested ID')
    else:p.error('explicit --id or --proof required; no implicit bulk export')
    sources={}
    for entry in a.source:
        seed,path=entry.split('=',1);sources[int(seed)]=Path(path).resolve()
    if any(v['provenance']['source_seed'] not in sources for v in volumes):p.error('missing source binding')
    build_gallery(volumes,sources,a.output.resolve())

if __name__=='__main__':main()
