import json
import unittest
from pathlib import Path

from terrain_harvest import portfolio, reach_fields, swarm_vocabulary


def cell(n, north, south, **kw):
    base = {'cell': [n, 0], 'centroid': [n * 64, 0], 'mean_surface_y': 70,
            'strategic_depth_cost': {'north': north, 'south': south},
            'fauna': {}, 'vegetation': {}, 'regenerative_vocabulary': [],
            'regional_character': {}}
    base.update(kw)
    return base


PEAKS = {'minecraft:jagged_peaks': 30, 'minecraft:plains': 10}
PLAINS = {'minecraft:plains': 40}


def ladder(biomes, n=9):
    return [cell(i, 50.0 + i * 80, 900.0 - i * 80, regional_character=biomes)
            for i in range(n)]


def swarms(p):
    return [s for s in p['sources'] if s['type'] == 'SWARM']


class SwarmDerivation(unittest.TestCase):
    def test_a_mountain_map_derives_ravager_sources(self):
        found = swarms(portfolio.derive(ladder(PEAKS)))
        self.assertTrue(found)
        for s in found:
            self.assertEqual(s['swarms'], ['mountain_ravager'])
            self.assertEqual(s['kind'], 'ravagers')
            self.assertEqual(s['band'], 'deeper')

    def test_a_map_without_the_ecology_derives_none(self):
        p = portfolio.derive(ladder(PLAINS))
        self.assertEqual(swarms(p), [], 'an absent opportunity is a real answer')
        self.assertTrue(any(k['kind'] == 'mountain_ravager' for k in p['skipped']))

    def test_a_fringe_cell_is_not_an_ecology(self):
        fringe = {'minecraft:jagged_peaks': 1, 'minecraft:plains': 39}
        self.assertEqual(swarms(portfolio.derive(ladder(fringe))), [])

    def test_the_cap_holds_per_team(self):
        found = swarms(portfolio.derive(ladder(PEAKS, n=30), per_band=2))
        for team in ('north', 'south'):
            self.assertLessEqual(sum(1 for s in found if s['near_team'] == team), 2)

    def test_swarms_never_satisfy_the_opening_floor(self):
        """A hostile swarm in a team's opening is not something Development
        can begin on, so it must not stand in for a developable opportunity."""
        cells = [cell(0, 50.0, 900.0), cell(1, 900.0, 50.0)]
        swarm_only = {'derived': True, 'sources': [{
            'type': 'SWARM', 'kind': 'ravagers', 'band': 'near',
            'strategic_depth_cost': {'north': 50.0, 'south': 900.0}}]}
        verdict = portfolio.certify(swarm_only, cells)
        teams = {p['team'] for p in verdict['problems']}
        self.assertIn('north', teams, 'a swarm in the opening must not count as developable')
        animal = {'derived': True, 'sources': [dict(swarm_only['sources'][0],
                                                    type='ANIMAL', kind='chicken')]}
        self.assertNotIn('north', {p['team'] for p in portfolio.certify(animal, cells)['problems']})

    def test_animal_and_crop_derivation_is_unchanged_by_the_swarm_layer(self):
        cells = [cell(i, 50.0 + i * 80, 900.0 - i * 80,
                      regenerative_vocabulary=['chicken', 'cow', 'carrot'])
                 for i in range(9)]
        p = portfolio.derive(cells)
        self.assertEqual(swarms(p), [])
        self.assertEqual(p['by_band']['near'], ['chicken'])


class BiomeGroupFixture(unittest.TestCase):
    def test_the_compiler_and_the_plugin_read_one_fixture(self):
        raw = json.loads(swarm_vocabulary.FIXTURE.read_text())['groups']
        self.assertEqual({k: frozenset(v) for k, v in raw.items()},
                         swarm_vocabulary.biome_groups())
        java = (Path(__file__).resolve().parents[2] / 'plugin/src/main/java/com/'
                'minecraftmoba/plugin/BiomeGroups.java').read_text()
        for ids in raw.values():
            for biome in ids:
                self.assertIn(f'"{biome}"', java, f'{biome} missing from BiomeGroups.java')

    def test_every_listed_swarm_names_a_real_group(self):
        groups = swarm_vocabulary.biome_groups()
        for spec in swarm_vocabulary.SWARMS.values():
            for g in spec['groups']:
                self.assertIn(g, groups)

    def test_the_swarm_vocabulary_matches_the_runtime_registry(self):
        java = (Path(__file__).resolve().parents[2] / 'plugin/src/main/java/com/'
                'minecraftmoba/plugin/SwarmDefinitions.java').read_text()
        for swarm_id in swarm_vocabulary.SWARMS:
            self.assertIn(f'"{swarm_id}"', java)


class ReachSpace(unittest.TestCase):
    def test_relation_distinguishes_the_cost_fields(self):
        r = reach_fields.relation
        self.assertEqual(r(cell(0, 50, 900)), 'cheap_north')
        self.assertEqual(r(cell(0, 900, 50)), 'cheap_south')
        self.assertEqual(r(cell(0, 500, 520)), 'equal')
        self.assertEqual(r(cell(0, 300, 900)), 'behind_north')
        self.assertEqual(r(cell(0, 50, 60)), 'contested_opening')
        self.assertEqual(r(cell(0, None, None)), 'unreachable')

    def test_bands_reproduce_the_portfolio_rule(self):
        cells = ladder(PEAKS)
        got = reach_fields.bands(cells)
        for s in portfolio.derive(cells)['sources']:
            if s.get('near_team'):
                self.assertEqual(got[tuple(s['cell'])][s['near_team']], s['band'])

    def test_profile_reports_where_a_biome_sits(self):
        cells = [cell(0, 50, 900, regional_character={'minecraft:jungle': 16}),
                 cell(1, 900, 50, regional_character={'minecraft:plains': 16}),
                 cell(2, 300, 900, regional_character={'minecraft:jungle': 8})]
        prof = reach_fields.profile(cells)
        self.assertEqual(prof['by_relation']['minecraft:jungle'],
                         {'cheap_north': 16, 'behind_north': 8})
        self.assertIn('minecraft:jungle', prof['by_team_band']['north']['near'])


if __name__ == '__main__':
    unittest.main()
