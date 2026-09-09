#!/usr/bin/env python3
"""Generate and evaluate a deliberately small sample of real vanilla regions."""

from __future__ import annotations

import argparse
from pathlib import Path

from vanilla_search.pipeline import run_search

ROOT=Path(__file__).resolve().parents[2]


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--server-jar",type=Path,required=True)
    parser.add_argument("--java",type=Path,required=True)
    parser.add_argument("--accept-eula",action="store_true")
    parser.add_argument("--seed",type=int,default=920270001)
    parser.add_argument("--attempts",type=int,default=10)
    parser.add_argument("--shortlist",type=int,default=3)
    parser.add_argument("--out",type=Path,default=ROOT/"implementation/worldgen/results/vanilla_default_poc_2026-09-09")
    parser.add_argument("--artifacts",type=Path,default=ROOT/"artifacts/worldgen/vanilla_default_poc_2026-09-09")
    args=parser.parse_args()
    if not 10<=args.attempts<=30: parser.error("proof-of-concept attempts must remain between 10 and 30")
    run_search(args.server_jar,args.java,args.out,args.artifacts,args.seed,args.attempts,args.shortlist,args.accept_eula)


if __name__=="__main__": main()
