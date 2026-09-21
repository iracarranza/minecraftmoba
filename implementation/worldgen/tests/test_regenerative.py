"""The Python predicate must agree with the plugin's, against a shared fixture.

The predicate exists twice on purpose -- see terrain_harvest/regenerative.py --
and the only thing that makes that safe is that both copies run the same fixture
and must produce the same answer. The Java side runs it in
EligibilityCrossCheckTest.
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from terrain_harvest.regenerative import Region, Rules, Terrain, eligible, generations

FIXTURE = Path(__file__).resolve().parents[1] / 'fixtures' / 'eligibility-crosscheck.json'


class FixtureTerrain(Terrain):
    def __init__(self, data):
        self.surfaces = {tuple(int(v) for v in k.split(',')): (self._ns(m), y)
                         for k, (m, y) in data['surface'].items()}
        self.obstructed = {tuple(int(v) for v in k.split(',')) for k in data['obstructed']}
        self.built = {tuple(int(v) for v in k.split(',')) for k in data['playerPlaced']}

    @staticmethod
    def _ns(name):
        return name if name.startswith('minecraft:') else 'minecraft:' + name

    def surface(self, x, z):
        return self.surfaces.get((x, z))

    def block(self, x, y, z):
        top = self.surfaces.get((x, z))
        if top is None:
            return 'minecraft:air'
        if (x, z) in self.obstructed and y == top[1] + 1:
            return 'minecraft:stone'
        return 'minecraft:air' if y > top[1] else top[0] if y == top[1] else 'minecraft:stone'

    def player_placed(self, x, y, z):
        top = self.surfaces.get((x, z))
        return (x, z) in self.built and top is not None and y == top[1]


def _load():
    data = json.loads(FIXTURE.read_text())
    r = data['rules']
    rules = Rules(headroom=r['headroom'],
                  ground=frozenset('minecraft:' + g for g in r['naturalGround']),
                  sample_stride=r['sampleStride'],
                  min_displacement=r['minDisplacement'],
                  player_exclusion=r['playerExclusion'],
                  reject_player_placed=r['rejectPlayerPlaced'])
    region = Region([(c['minX'], c['minZ'], c['maxX'], c['maxZ']) for c in data['region']])
    return data, rules, region, FixtureTerrain(data['terrain'])


def test_matches_the_shared_fixture():
    data, rules, region, terrain = _load()
    got = sorted(tuple(l) for l in eligible(region, terrain, rules))
    want = sorted(tuple(l) for l in data['expected'])
    assert got == want, f"\nexpected {want}\n     got {got}"


def test_water_obstruction_and_player_work_are_all_excluded():
    # Restating the fixture's intent, so a wrong `expected` cannot make the
    # cross-check agree on the wrong answer.
    _, rules, region, terrain = _load()
    loci = {(x, z) for x, _, z in eligible(region, terrain, rules)}
    assert (2, 0) not in loci and (2, 1) not in loci, 'water is not ground'
    assert (1, 2) not in loci and (4, 3) not in loci, 'no headroom'
    assert (5, 1) not in loci and (5, 2) not in loci, 'player-placed ground'
    assert (0, 3) not in loci, 'bedrock is not in the ground set'


def test_a_region_is_not_a_point():
    _, rules, region, terrain = _load()
    assert region.area() == 24
    assert len(eligible(region, terrain, rules)) > 1


def test_successive_manifestations_move():
    _, rules, region, terrain = _load()
    rules = Rules(rules.headroom, rules.ground, rules.sample_stride,
                  2.0, rules.player_exclusion, rules.reject_player_placed)
    _, chosen = generations(region, terrain, rules, 4, seed=7)
    placed = [c for c in chosen if c is not None]
    assert len(placed) >= 2
    for a, b in zip(placed, placed[1:]):
        assert a != b, 'a new manifestation must not reuse the previous locus'


def test_no_eligible_locus_returns_none_rather_than_a_fallback():
    _, rules, region, _ = _load()

    class Barren(Terrain):
        def surface(self, x, z):
            return ('minecraft:oak_planks', 64)

        def block(self, x, y, z):
            return 'minecraft:air'

    loci, chosen = generations(region, Barren(), rules, 3, seed=1)
    assert loci == []
    assert chosen == [None, None, None], 'no origin fallback, no forced spawn'
