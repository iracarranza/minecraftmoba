#!/usr/bin/env python3
"""Compact analysis helpers recovered from the latest Default-map discussion.

This intentionally does not reconstruct lost generator tuning. It preserves
the explicit traversal arithmetic and the straight-line accessibility proxy so
they can be replaced by terrain-aware pathfinding without changing interfaces.
"""

from __future__ import annotations

import argparse
import json
import math
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable, Sequence


@dataclass(frozen=True)
class TravelState:
    name: str
    food: float
    cutoff_food: float = 6.0
    exhaustion_per_food: float = 4.0

    @property
    def sprint_budget(self) -> float:
        return max(0.0, self.food - self.cutoff_food) * self.exhaustion_per_food


L1 = TravelState("L1", 10.0)
L3_HUNGER = TravelState("L3 Hunger", 12.0)


def locomotion_exhaustion(
    blocks: float,
    jumps_per_100_blocks: float,
    route_share: float = 0.0,
    route_efficiency: float = 0.10,
    sprint_cost_per_block: float = 0.10,
    sprint_jump_cost: float = 0.20,
) -> float:
    """Estimate sprint locomotion cost using the recovered prototype model."""
    if blocks < 0 or jumps_per_100_blocks < 0:
        raise ValueError("distance and jump rate must be non-negative")
    if not 0 <= route_share <= 1 or not 0 <= route_efficiency < 1:
        raise ValueError("route_share must be 0..1 and efficiency must be 0..<1")
    base = blocks * sprint_cost_per_block
    base += (blocks * jumps_per_100_blocks / 100.0) * sprint_jump_cost
    return base * (1.0 - route_share * route_efficiency)


def unsupported_round_trip_radius(
    state: TravelState,
    jumps_per_100_blocks: float,
    route_share: float,
    operating_reserve: float = 0.20,
    route_efficiency: float = 0.10,
) -> float:
    """One-way radius for sprinting out/back while preserving a budget share."""
    if not 0 <= operating_reserve < 1:
        raise ValueError("operating_reserve must be in [0, 1)")
    cost_per_block = locomotion_exhaustion(
        1.0, jumps_per_100_blocks, route_share, route_efficiency
    )
    return state.sprint_budget * (1.0 - operating_reserve) / (2.0 * cost_per_block)


def euclidean(a: Sequence[float], b: Sequence[float]) -> float:
    return math.hypot(a[0] - b[0], a[1] - b[1])


def access_bias(
    point: Sequence[float],
    north_home: Sequence[float] = (0.0, -292.0),
    south_home: Sequence[float] = (0.0, 292.0),
) -> dict[str, float]:
    """Historical geometric proxy; positive means north is farther away."""
    north = euclidean(point, north_home)
    south = euclidean(point, south_home)
    return {
        "north": round(north, 3),
        "south": round(south, 3),
        "bias": round((north - south) / (north + south + 1e-9), 6),
    }


def portfolio_summary(points: Iterable[Sequence[float]]) -> dict[str, float]:
    biases = [access_bias(point)["bias"] for point in points]
    if not biases:
        return {"count": 0, "mean_bias": 0.0, "mean_absolute_bias": 0.0}
    return {
        "count": len(biases),
        "mean_bias": round(sum(biases) / len(biases), 6),
        "mean_absolute_bias": round(sum(abs(x) for x in biases) / len(biases), 6),
    }


def traversal_report() -> dict[str, object]:
    scenarios = {
        "all_clean_route": (2.0, 1.0),
        "mostly_route": (3.5, 0.75),
        "half_route_half_wilderness": (5.0, 0.50),
        "mostly_wilderness": (6.5, 0.25),
        "ordinary_wilderness": (8.0, 0.0),
    }
    return {
        "assumptions": {
            "operating_reserve": 0.20,
            "route_efficiency": 0.10,
            "sprint_exhaustion_per_block": 0.10,
            "sprint_jump_exhaustion": 0.20,
            "warning": "jump rates are analysis assumptions, not generation rules",
        },
        "states": [asdict(L1), asdict(L3_HUNGER)],
        "unsupported_round_trip_one_way_blocks": {
            name: {
                state.name: round(unsupported_round_trip_radius(state, jumps, share), 1)
                for state in (L1, L3_HUNGER)
            }
            for name, (jumps, share) in scenarios.items()
        },
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--points", type=Path, help="JSON array of [x,z] POI/resource points")
    args = parser.parse_args()
    report = {"traversal": traversal_report()}
    if args.points:
        report["opportunity_access"] = portfolio_summary(json.loads(args.points.read_text()))
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
