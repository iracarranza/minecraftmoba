"""Vanilla Minecraft constants and formulas used by the expedition calculator.

Nothing here is a balance decision. Break time and movement speed are vanilla
mechanics, and the hardness table was read back off the running Paper 1.21.11
fixture (`moba curve`, 2026-09-20) rather than from memory. Every value in this
module is therefore tagged CANON (vanilla) or MAP MEASUREMENT in the sense of
docs/analysis/extraction-expedition-calculator.md section 20 -- none of it is a
SENSITIVITY input the reader has to accept on trust.
"""
from __future__ import annotations

import math

# --- Hardness, read off the fixture on 2026-09-20 -------------------------
# Deepslate variants are uniformly 1.5x the stone variant, which is why the
# deepslate share of a deposit matters to extraction time at all.
HARDNESS = {
    'stone': 1.5,
    'deepslate': 3.0,
    'iron_ore': 3.0, 'deepslate_iron_ore': 4.5,
    'copper_ore': 3.0, 'deepslate_copper_ore': 4.5,
    'coal_ore': 3.0, 'deepslate_coal_ore': 4.5,
    'diamond_ore': 3.0, 'deepslate_diamond_ore': 4.5,
    'redstone_ore': 3.0, 'deepslate_redstone_ore': 4.5,
    'gold_ore': 3.0, 'deepslate_gold_ore': 4.5,
    'lapis_ore': 3.0, 'deepslate_lapis_ore': 4.5,
    'emerald_ore': 3.0, 'deepslate_emerald_ore': 4.5,
}

# --- Tool speeds, vanilla ------------------------------------------------
TOOL_SPEED = {'wood': 2.0, 'stone': 4.0, 'iron': 6.0,
              'diamond': 8.0, 'netherite': 9.0, 'gold': 12.0}

# Which pickaxe tier is required to drop each resource at all.
TIER = {'wood': 0, 'stone': 1, 'iron': 2, 'diamond': 3, 'netherite': 4, 'gold': 0}
REQUIRED_TIER = {
    'coal': 0, 'copper': 1, 'iron': 1, 'lapis': 1,
    'gold': 2, 'redstone': 2, 'diamond': 2, 'emerald': 2,
}

# --- Movement, vanilla ---------------------------------------------------
WALK_BPS = 4.317
SPRINT_BPS = 5.612

# --- Yield ---------------------------------------------------------------
# Fortune expected multipliers as the project's existing analysis states them
# (calculator spec section 8). CANON / WORKING DESIGN.
FORTUNE_MULTIPLIER = {0: 1.00, 1: 1.33, 2: 1.75, 3: 2.20}

# Expected native drop per physical block, vanilla.
NATIVE_DROP_MEAN = {
    'coal': 1.0, 'iron': 1.0, 'gold': 1.0, 'diamond': 1.0, 'emerald': 1.0,
    'copper': 3.5,      # 2-5 raw copper
    'redstone': 4.5,    # 4-5 dust
    'lapis': 6.0,       # 4-9 lapis
}
# Fortune applies to the ore-style drops but not to the smelt-style ones
# (iron/copper/gold raw drops are affected in modern versions; coal/diamond etc.
# are too). The one real exception is that Fortune never multiplies a block that
# drops itself. All resources modelled here drop items, so all take Fortune.


def break_seconds(block: str, tool: str, efficiency: int = 0,
                  haste: int = 0, on_ground: bool = True,
                  in_water: bool = False) -> float:
    """Vanilla block-breaking time in seconds.

    speed = tool speed + (efficiency^2 + 1); damage = speed / hardness / 30
    when the tool can harvest the block. ticks = ceil(1 / damage).
    """
    hardness = HARDNESS[block]
    speed = TOOL_SPEED[tool]
    if efficiency > 0:
        speed += efficiency ** 2 + 1
    if haste > 0:
        speed *= 1.0 + 0.2 * haste
    if in_water:
        speed /= 5.0
    if not on_ground:
        speed /= 5.0
    damage = speed / hardness / 30.0
    if damage >= 1.0:
        return 0.0
    return math.ceil(1.0 / damage) / 20.0


def can_harvest(resource: str, tool: str) -> bool:
    return TIER[tool] >= REQUIRED_TIER[resource]


def ore_block(resource: str, deepslate: bool) -> str:
    return f"{'deepslate_' if deepslate else ''}{resource}_ore"


def expected_harvest(blocks: float, resource: str, fortune: int = 0) -> float:
    """Expected items from `blocks` physical ore blocks."""
    return blocks * NATIVE_DROP_MEAN[resource] * FORTUNE_MULTIPLIER[fortune]


def blocks_required(quantity: float, resource: str, fortune: int = 0) -> int:
    """Expected physical blocks needed to reach `quantity` items."""
    per = NATIVE_DROP_MEAN[resource] * FORTUNE_MULTIPLIER[fortune]
    return math.ceil(quantity / per)
