#!/usr/bin/env python3
"""Audit candidate metadata against the current prototype resource vocabulary.

Input is deliberately schema-light: a JSON object with arrays/objects named
livestock_ranges, horses, crop_patches, formations, ores, and contract_gaps.
The audit checks existence only; it does not invent the lost ecology pipeline's
counts, densities, radii, or placement parameters.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

REQUIRED_LIVESTOCK = {"cow", "sheep", "pig", "chicken"}
REQUIRED_CROPS = {"wheat", "carrot", "potato"}
REQUIRED_FORMATIONS = {"sand", "gravel", "snow"}
REQUIRED_ORES = {"coal", "iron", "copper", "gold", "redstone", "lapis", "diamond", "emerald"}


def audit(candidate: dict) -> dict:
    livestock = {
        species
        for region in candidate.get("livestock_ranges", [])
        for species in region.get("species", [])
    }
    crops = {patch.get("crop") for patch in candidate.get("crop_patches", [])}
    formations = {item.get("type") for item in candidate.get("formations", [])}
    ores = {item.get("type") for item in candidate.get("ores", [])}
    checks = {
        "at_least_four_livestock_ranges": len(candidate.get("livestock_ranges", [])) >= 4,
        "livestock_vocabulary": REQUIRED_LIVESTOCK <= livestock,
        "separate_horse_presence": bool(candidate.get("horses")),
        "crop_vocabulary": REQUIRED_CROPS <= crops,
        "surface_and_snow_vocabulary": REQUIRED_FORMATIONS <= formations,
        "ore_vocabulary": REQUIRED_ORES <= ores,
        "no_reported_contract_gaps": not candidate.get("contract_gaps"),
    }
    return {
        "pass": all(checks.values()),
        "checks": checks,
        "missing": {
            "livestock": sorted(REQUIRED_LIVESTOCK - livestock),
            "crops": sorted(REQUIRED_CROPS - crops),
            "formations": sorted(REQUIRED_FORMATIONS - formations),
            "ores": sorted(REQUIRED_ORES - ores),
        },
        "warning": "Existence is necessary but not sufficient; accessibility, ecology, separation, abundance, and opportunity fairness still require measurement.",
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("candidate", type=Path)
    args = parser.parse_args()
    print(json.dumps(audit(json.loads(args.candidate.read_text())), indent=2))


if __name__ == "__main__":
    main()
