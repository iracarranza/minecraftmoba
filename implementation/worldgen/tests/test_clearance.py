"""Authoring refuses what is built, and moves what is alive."""
import unittest
from pathlib import Path

from terrain_harvest import clearance


def reader_with(built):
    """A column reader where `built` maps (x, y, z) to a block name."""
    lookup = {tuple(k): v for k, v in built.items()}
    return lambda x, y, z: lookup.get((x, y, z), 'minecraft:air')


class RefuseBuiltSites(unittest.TestCase):

    EXTENTS = {'pillager_outpost': (8, 21)}

    def placements(self):
        return [{'structure': 'pillager_outpost', 'team': 'north', 'world_xyz': [0, 64, 0]}]

    def test_a_clean_site_is_not_refused(self):
        r = clearance.refuse_built_sites(reader_with({}), self.placements(), self.EXTENTS)
        self.assertEqual([], r)

    def test_a_village_house_in_the_footprint_refuses_the_site(self):
        # The exact failure: the builder used to step over this and build
        # around the house, while the verifier failed the same condition.
        built = {(3, 65, 2): 'minecraft:oak_planks'}
        r = clearance.refuse_built_sites(reader_with(built), self.placements(), self.EXTENTS)
        self.assertEqual(1, len(r))
        self.assertEqual('pillager_outpost', r[0]['structure'])
        self.assertIn('wrong site', r[0]['detail'])

    def test_natural_ground_does_not_refuse(self):
        # Levelling a hillside is terrain work. If stone refused a site, no map
        # would ever author anything.
        built = {(3, 65, 2): 'minecraft:stone', (4, 64, 1): 'minecraft:dirt',
                 (2, 66, 2): 'minecraft:oak_log'}
        self.assertEqual([], clearance.refuse_built_sites(
            reader_with(built), self.placements(), self.EXTENTS))

    def test_every_offending_site_is_reported_not_just_the_first(self):
        # A build that stops at the first refusal makes the next run discover
        # the second, and the run after that the third.
        places = [{'structure': 'pillager_outpost', 'team': 'north', 'world_xyz': [0, 64, 0]},
                  {'structure': 'pillager_outpost', 'team': 'south', 'world_xyz': [200, 64, 0]}]
        built = {(0, 64, 0): 'minecraft:bell', (200, 64, 0): 'minecraft:bell'}
        r = clearance.refuse_built_sites(reader_with(built), places, self.EXTENTS)
        self.assertEqual(2, len(r))
        self.assertEqual({'north', 'south'}, {x['team'] for x in r})


class EvictEntities(unittest.TestCase):

    def test_an_absent_entities_directory_is_reported_not_crashed(self):
        r = clearance.evict_entities(
            Path('/nonexistent-world'),
            [{'structure': 'pillager_outpost', 'world_xyz': [0, 64, 0]}],
            {'pillager_outpost': (8, 21)})
        self.assertEqual(0, r['removed'])
        self.assertIn('note', r)

    def test_nothing_to_place_evicts_nothing(self):
        r = clearance.evict_entities(Path('/nonexistent-world'), [], {})
        self.assertEqual(0, r['removed'])

    def test_the_footprint_test_covers_the_column_a_structure_occupies(self):
        inside = clearance._inside((2.0, 65.0, 2.0), [(0, 64, 0, 8, 21)])
        above = clearance._inside((2.0, 200.0, 2.0), [(0, 64, 0, 8, 21)])
        beside = clearance._inside((40.0, 65.0, 0.0), [(0, 64, 0, 8, 21)])
        self.assertTrue(inside, 'an animal standing where the structure goes')
        self.assertFalse(above, 'a bird far overhead is not entombed')
        self.assertFalse(beside, 'an animal outside the footprint is left alone')


if __name__ == '__main__':
    unittest.main()
