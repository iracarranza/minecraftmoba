#!/usr/bin/env python3
"""Deterministic batch entry point for Default geography greyboxes."""

from __future__ import annotations

import argparse
from pathlib import Path

from greybox.pipeline import search_candidates, write_results


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed", type=int, default=920261000)
    parser.add_argument("--attempts", type=int, default=48)
    parser.add_argument("--shortlist", type=int, default=6)
    parser.add_argument("--out", type=Path, default=Path(__file__).parent / "results" / "default_greyboxes_2026-09-09")
    args = parser.parse_args()
    if not args.seed <= 920261010 < args.seed + args.attempts or not args.seed <= 920261022 < args.seed + args.attempts:
        raise ValueError("the batch range must include regression seeds 920261010 and 920261022")
    selected, evaluated, report = search_candidates(args.seed, args.attempts, args.shortlist)
    write_results(args.out, selected, evaluated, report)
    print(f"evaluated={report['attempts']} hard_passes={report['hard_contract_passes']} quality_passes={report['experimental_quality_passes']} shortlisted={len(selected)} runtime_seconds={report['runtime_seconds']} out={args.out}")


if __name__ == "__main__":
    main()
