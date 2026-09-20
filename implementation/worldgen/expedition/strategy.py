"""Extraction strategies, each expressed as a rate derived from measurement.

The point of separating these is the design question behind the whole tool:
does the map reward practised vanilla underground behaviour, or does it reward
blind digging? That is a comparison between strategies, so each one has to be a
behaviour model with its own derivation, not a single fudge factor.

Two of the three are fully derivable from vanilla formulas plus the cave scan.
Cave exploration is not: it needs one honest sensitivity, the rate at which a
moving player sweeps cave volume with their eyes. It is declared as a parameter
rather than buried, and `sweep_band` reports the answer across a range.
"""
from __future__ import annotations

from dataclasses import dataclass

from . import vanilla


@dataclass(frozen=True)
class Kit:
    """The player capability that bears on extraction rate."""
    tool: str = 'stone'
    efficiency: int = 0
    fortune: int = 0
    haste: int = 0

    def break_time(self, block: str) -> float:
        return vanilla.break_seconds(block, self.tool, self.efficiency, self.haste)


@dataclass(frozen=True)
class Deposit:
    """A measured resource population in one scanned cell."""
    resource: str
    blocks_exposed: int
    blocks_buried: int
    deepslate_fraction: float
    mean_y: float
    cave_volume: int
    # blocks of this resource per solid block, in the layer band around mean_y
    density_at_depth: float

    @property
    def blocks(self) -> int:
        return self.blocks_exposed + self.blocks_buried

    def mean_break_time(self, kit: Kit) -> float:
        """Break time averaged over the measured deepslate/stone split."""
        d = self.deepslate_fraction
        return (d * kit.break_time(vanilla.ore_block(self.resource, True))
                + (1 - d) * kit.break_time(vanilla.ore_block(self.resource, False)))

    def overburden_break_time(self, kit: Kit) -> float:
        """Break time for the host rock at this depth, same deepslate split."""
        d = self.deepslate_fraction
        return d * kit.break_time('deepslate') + (1 - d) * kit.break_time('stone')


# --- Cave exploration ----------------------------------------------------
# SENSITIVITY / NON-CANON. A player walking a cave sees roughly a corridor of
# cave volume per second. Render distance is large but line of sight in a cave
# is not, and the player is not inspecting every block they pass. The band below
# brackets a plausible range and results are reported across it.
SWEEP_BLOCKS_PER_SECOND = (150.0, 300.0, 600.0)


def cave_exploration_seconds(deposit: Deposit, kit: Kit, blocks_wanted: int,
                             sweep: float) -> dict:
    """Seconds to extract `blocks_wanted` by walking caves and mining what shows.

    Only exposed ore is reachable this way; buried ore is invisible without
    digging, which is what the other two strategies do.
    """
    if deposit.blocks_exposed <= 0 or deposit.cave_volume <= 0:
        return {'feasible': False, 'reason': 'no exposed ore or no cave volume'}
    reachable = min(blocks_wanted, deposit.blocks_exposed)
    # Volume that must be swept to encounter that many exposed blocks.
    per_block_volume = deposit.cave_volume / deposit.blocks_exposed
    travel = reachable * per_block_volume / sweep
    mining = reachable * deposit.mean_break_time(kit)
    return {
        'feasible': True,
        'blocks': reachable,
        'short_by': max(0, blocks_wanted - reachable),
        'travel_s': travel,
        'mining_s': mining,
        'seconds': travel + mining,
        'sweep_blocks_per_second': sweep,
    }


# --- Branch mining -------------------------------------------------------
# Fully derivable. A 1-wide, 2-tall branch advanced one block digs 2 blocks and
# newly reveals 6 (two left, two right, one up, one down), so 8 blocks are
# inspected per 2 dug: 4 inspected per block broken. Branches on 3-block
# spacing do not re-inspect each other's walls.
INSPECTED_PER_BLOCK_DUG = 4.0


def branch_mining_seconds(deposit: Deposit, kit: Kit, blocks_wanted: int) -> dict:
    if deposit.density_at_depth <= 0:
        return {'feasible': False, 'reason': 'no measured density at depth'}
    ore_per_dug = deposit.density_at_depth * INSPECTED_PER_BLOCK_DUG
    dug = blocks_wanted / ore_per_dug
    overburden = dug * deposit.overburden_break_time(kit)
    mining = blocks_wanted * deposit.mean_break_time(kit)
    return {
        'feasible': True,
        'blocks': blocks_wanted,
        'short_by': 0,
        'blocks_dug': dug,
        'travel_s': 0.0,
        'overburden_s': overburden,
        'mining_s': mining,
        'seconds': overburden + mining,
    }


# --- Quarry / strip ------------------------------------------------------
# Also fully derivable, and deliberately included as the dumb baseline: dig out
# the volume wholesale and every ore block in it is yours.
def quarry_seconds(deposit: Deposit, kit: Kit, blocks_wanted: int) -> dict:
    if deposit.density_at_depth <= 0:
        return {'feasible': False, 'reason': 'no measured density at depth'}
    dug = blocks_wanted / deposit.density_at_depth
    overburden = dug * deposit.overburden_break_time(kit)
    mining = blocks_wanted * deposit.mean_break_time(kit)
    return {
        'feasible': True,
        'blocks': blocks_wanted,
        'short_by': 0,
        'blocks_dug': dug,
        'travel_s': 0.0,
        'overburden_s': overburden,
        'mining_s': mining,
        'seconds': overburden + mining,
    }


STRATEGIES = {
    'cave_exploration': cave_exploration_seconds,
    'branch_mining': branch_mining_seconds,
    'quarry': quarry_seconds,
}
