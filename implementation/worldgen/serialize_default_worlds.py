#!/usr/bin/env python3
"""Build, validate, render, and reproducibly package the two review worlds."""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import zipfile
from pathlib import Path

from serialization.bootstrap import bootstrap_level_template
from serialization.inspect import render_suite, validate_world
from serialization.server_check import validate_with_server
from serialization.world import write_world

ROOT=Path(__file__).resolve().parents[2]
CANDIDATES=ROOT/"implementation/worldgen/results/default_candidates_2026-09-09"
WORLDS=((920261010,"Horseshoe"),(920261022,"Offset_Basin"))
SERVER_JAR_SHA1="64bb6d763bed0a9f1d632ec347938594144943ed"


def verify_server_jar(path: Path):
    actual=hashlib.sha1(path.read_bytes()).hexdigest()
    if actual!=SERVER_JAR_SHA1:
        raise ValueError(f"expected Minecraft Java 1.21.11 server SHA-1 {SERVER_JAR_SHA1}, got {actual}")


def package_world(world: Path,destination: Path):
    destination.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(destination,"w",zipfile.ZIP_DEFLATED,compresslevel=9) as archive:
        for path in sorted(world.rglob("*")):
            if path.is_dir(): continue
            info=zipfile.ZipInfo(str(Path(world.name)/path.relative_to(world)),(2026,9,9,0,0,0)); info.compress_type=zipfile.ZIP_DEFLATED; info.external_attr=0o644<<16
            archive.writestr(info,path.read_bytes(),compresslevel=9)


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--server-jar",type=Path,required=True)
    parser.add_argument("--java",type=Path,required=True)
    parser.add_argument("--out",type=Path,default=ROOT/"artifacts/worldgen/default_serialized_2026-09-09")
    parser.add_argument("--accept-eula",action="store_true")
    parser.add_argument("--template-level-dat",type=Path)
    parser.add_argument("--reuse-existing",action="store_true",help="resume validation/render/package from an already serialized matching world")
    parser.add_argument("--server-check",action="store_true",help="boot a disposable copy with the matching Mojang server")
    args=parser.parse_args(); build=args.out/"build"; packages=args.out/"packages"
    verify_server_jar(args.server_jar)
    template=args.template_level_dat or bootstrap_level_template(args.server_jar,args.java,build/"vanilla_template",args.accept_eula)
    summary={"minecraft_java_version":"1.21.11","worlds":[]}
    for seed,label in WORLDS:
        name=f"Minecraft_MOBA_Default_{seed}_{label}"; world=args.out/"worlds"/name
        candidate=CANDIDATES/f"candidate_{seed}.json"
        existing=world/"serialization_manifest.json"
        if args.reuse_existing and existing.exists() and json.loads(existing.read_text()).get("seed")==seed:
            manifest=json.loads(existing.read_text()); print(name,"reusing serialized chunks",flush=True)
        else:
            _,manifest=write_world(candidate,template,world,name)
        reader,validation=validate_world(world,candidate)
        renders=render_suite(reader,world,json.loads(candidate.read_text()))
        server_validation=validate_with_server(world,args.server_jar,args.java,args.accept_eula) if args.server_check else None
        package=packages/f"{name}.zip"; package_world(world,package)
        summary["worlds"].append({"seed":seed,"name":name,"world":str(world),"package":str(package),"renders":str(renders),"validation_pass":validation["pass"],"server_validation_pass":server_validation["pass"] if server_validation else None,"validation_report":str(world/"validation_report.json")})
        print(name,"validation",validation["pass"],"package",package,flush=True)
    (args.out/"build_summary.json").write_text(json.dumps(summary,indent=2)+"\n")


if __name__=="__main__": main()
