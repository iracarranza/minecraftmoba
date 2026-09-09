#!/usr/bin/env python3
"""CLI for the successor metadata candidate search."""

from __future__ import annotations

import argparse
from pathlib import Path

from successor.pipeline import search_candidates, write_results


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--seed",type=int,default=920261000)
    parser.add_argument("--attempts",type=int,default=24)
    parser.add_argument("--finalists",type=int,default=4)
    parser.add_argument("--out",type=Path,default=Path(__file__).parent/"results"/"default_candidates_2026-09-09")
    args=parser.parse_args()
    selected,report=search_candidates(args.seed,args.attempts,args.finalists)
    write_results(args.out,selected,report)
    print(f"evaluated={report['attempts']} hard_passes={report['hard_contract_passes']} finalists={len(selected)} out={args.out}")


if __name__ == "__main__": main()
