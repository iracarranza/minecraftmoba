"""The eligibility predicate, offline.

This is a SECOND implementation of a rule that lives authoritatively in the
plugin (`Eligibility.java`). That is a real cost and it is taken deliberately:
the question worth answering before any re-freeze -- "given the frozen template
as it stands, where could each opportunity actually manifest?" -- needs the
predicate run against region files, and the plugin can only run against a loaded
Bukkit world.

Two things keep the copies honest.

  - Every constant is read from the plugin's own `config.yml`, including the
    ground list, so a fixture edit cannot move one side without the other.
  - Both sides run `fixtures/eligibility-crosscheck.json` and must produce the
    same loci. If they disagree, this module is lying about the map, and the
    disagreement is the finding rather than a nuisance.

Nothing here decides anything. The predicates are Alpha fixtures and are
expected to change; this reports what they currently imply.
"""
from __future__ import annotations

import json
import random
import re
from dataclasses import dataclass, field
from pathlib import Path

CONFIG = Path(__file__).resolve().parents[2] / 'plugin' / 'src' / 'main' / 'resources' / 'config.yml'
AIR = {'minecraft:air', 'minecraft:cave_air', 'minecraft:short_grass', 'minecraft:tall_grass'}


@dataclass(frozen=True)
class Rules:
    headroom: int
    ground: frozenset
    sample_stride: int
    min_displacement: float
    player_exclusion: float
    reject_player_placed: bool


@dataclass
class Region:
    """A set of authored cells. Deliberately not origin+radius."""
    cells: list[tuple[int, int, int, int]]

    @staticmethod
    def square(x: int, z: int, half: int) -> 'Region':
        return Region([(x - half, z - half, x + half, z + half)])

    def contains(self, x: int, z: int) -> bool:
        return any(a <= x <= c and b <= z <= d for a, b, c, d in self.cells)

    def columns(self, stride: int):
        for a, b, c, d in self.cells:
            for x in range(a, c + 1, stride):
                for z in range(b, d + 1, stride):
                    yield x, z

    def area(self) -> int:
        return sum((c - a + 1) * (d - b + 1) for a, b, c, d in self.cells)


def _scalar(text: str, dotted: str, cast, default=None):
    """Read one scalar out of config.yml by indentation path. No YAML dependency."""
    parts = dotted.split('.')
    depth, idx = 0, 0
    lines = text.splitlines()
    while depth < len(parts):
        want = parts[depth]
        found = False
        while idx < len(lines):
            line = lines[idx]
            idx += 1
            if not line.strip() or line.lstrip().startswith('#'):
                continue
            m = re.match(r'^(\s*)([A-Za-z0-9_"\-]+)\s*:(.*)$', line)
            if not m:
                continue
            key = m.group(2).strip('"')
            if key != want:
                continue
            found = True
            if depth == len(parts) - 1:
                value = m.group(3).split('#')[0].strip()
                return cast(value) if value else default
            break
        if not found:
            return default
        depth += 1
    return default


def _string_list(text: str, dotted: str) -> list[str]:
    parts = dotted.split('.')
    lines = text.splitlines()
    idx, depth = 0, 0
    while depth < len(parts):
        want = parts[depth]
        while idx < len(lines):
            line = lines[idx]
            idx += 1
            m = re.match(r'^(\s*)([A-Za-z0-9_"\-]+)\s*:(.*)$', line)
            if m and m.group(2).strip('"') == want:
                break
        else:
            return []
        depth += 1
    out = []
    while idx < len(lines):
        line = lines[idx]
        idx += 1
        if line.strip().startswith('-'):
            out.append(line.strip().lstrip('-').strip())
        elif line.strip() and not line.strip().startswith('#'):
            break
    return out


def rules_from_config(path: Path = CONFIG) -> Rules:
    """Alpha fixtures, read from the file the plugin reads."""
    text = path.read_text()
    base = 'renewables.eligibility.'
    ground = _string_list(text, base + 'naturalGround')
    return Rules(
        headroom=_scalar(text, base + 'headroom', int, 2),
        # lstrip() strips CHARACTERS, not a prefix: it turned coarse_dirt into
        # "oarse_dirt" and mycelium into "ycelium", silently shrinking the
        # ground set to the materials whose names happen not to start with one
        # of "minecraft:".
        ground=frozenset(g if g.startswith('minecraft:') else 'minecraft:' + g
                         for g in ground),
        sample_stride=_scalar(text, base + 'sampleStride', int, 4),
        min_displacement=_scalar(text, base + 'minDisplacement', float, 12.0),
        player_exclusion=_scalar(text, base + 'playerExclusion', float, 24.0),
        reject_player_placed=_scalar(text, base + 'rejectPlayerPlaced',
                                     lambda v: v.strip() == 'true', True))


class Terrain:
    """What the predicate is allowed to know. Mirrors the plugin's TerrainView."""

    def surface(self, x, z):          # -> (material, y) or None when unknown
        raise NotImplementedError

    def block(self, x, y, z):         # -> material name
        raise NotImplementedError

    def player_placed(self, x, y, z) -> bool:
        return False


def eligible(region: Region, terrain: Terrain, rules: Rules) -> list[tuple[int, int, int]]:
    """Every locus the region currently offers. The same test, in the same order."""
    out = []
    for x, z in region.columns(rules.sample_stride):
        top = terrain.surface(x, z)
        if top is None:
            continue
        material, ground_y = top
        if material not in rules.ground:
            continue
        if rules.reject_player_placed and terrain.player_placed(x, ground_y, z):
            continue
        if any(terrain.block(x, ground_y + dy, z) not in AIR
               for dy in range(1, rules.headroom + 1)):
            continue
        out.append((x, ground_y + 1, z))
    return out


def select(candidates, rules: Rules, previous, rng: random.Random):
    """Uniform among viable loci, or None. No fallback of any kind."""
    viable = [l for l in candidates
              if previous is None or _distance(l, previous) >= rules.min_displacement]
    return rng.choice(viable) if viable else None


def _distance(a, b) -> float:
    return sum((p - q) ** 2 for p, q in zip(a, b)) ** 0.5


def generations(region: Region, terrain: Terrain, rules: Rules, count: int, seed: int):
    """Where successive manifestations would land. Locus(t) need not equal Locus(t+1)."""
    rng = random.Random(seed)
    loci = eligible(region, terrain, rules)
    chosen, previous = [], None
    for _ in range(count):
        pick = select(loci, rules, previous, rng)
        chosen.append(pick)
        if pick is not None:
            previous = pick
    return loci, chosen
