"""Column-level verification: what surface samples cannot establish."""
import unittest

from terrain_harvest import column_scan as cs
from terrain_harvest import objective_forms as of


class FakeWorld:
    """A world made of declared columns, so the contract is testable offline."""

    def __init__(self, surface=63, blocks=None, missing=()):
        self.surface_y = surface
        self.blocks = blocks or {}
        self.missing = set(missing)

    def surface(self, x, z):
        return None if (x, z) in self.missing else self.surface_y

    def block(self, x, y, z):
        if (x, z) in self.missing:
            return None
        if y <= self.surface_y:
            return 'minecraft:stone'
        return self.blocks.get((x, y, z), 'minecraft:air')


class Clearance(unittest.TestCase):

    def test_open_sky_is_clear_to_the_height_asked_for(self):
        got = cs.clearance(FakeWorld(), 0, 0, 63, 103, span=3)
        self.assertTrue(got['sufficient'])
        self.assertEqual(103, got['clear'])

    def test_a_cliff_blocks_and_names_the_column(self):
        w = FakeWorld(blocks={(1, 70, 0): 'minecraft:stone'})
        got = cs.clearance(w, 0, 0, 63, 103, span=3)
        self.assertFalse(got['sufficient'])
        self.assertEqual(6, got['clear'])
        self.assertEqual([1, 70, 0], got['blocked_by']['at'])

    def test_trees_are_clearing_cost_not_obstruction(self):
        # Authoring fells trees; doctrine treats clearing vegetation as bounded
        # work and reserves rejection for landscape surgery. Counting a canopy
        # as terrain rejected two of six real objectives on oak leaves.
        w = FakeWorld(blocks={(1, 70, 0): 'minecraft:oak_leaves',
                              (1, 71, 0): 'minecraft:oak_log'})
        got = cs.clearance(w, 0, 0, 63, 103, span=3)
        self.assertTrue(got['sufficient'])
        self.assertEqual(2, got['vegetation_blocks_to_clear'])

    def test_a_building_is_not_terrain_and_is_reported(self):
        w = FakeWorld(blocks={(1, 70, 0): 'minecraft:cobblestone'})
        got = cs.clearance(w, 0, 0, 63, 103, span=3)
        self.assertEqual(1, got['standing_structure_blocks'])

    def test_the_pad_datum_is_the_highest_column_not_the_centre(self):
        # Measuring from the centre reported ordinary undulation as an
        # obstruction: a neighbouring column one block higher "blocked" a
        # 103-block spike at dy=1. Authoring levels the pad before it builds.
        w = FakeWorld()
        w.surface = lambda x, z: 63 + (1 if (x, z) == (1, 0) else 0)
        self.assertEqual(64, cs.pad_level(w, 0, 0, 3))

    def test_ungenerated_terrain_is_not_treated_as_sky(self):
        w = FakeWorld(missing={(1, 0)})
        got = cs.clearance(w, 0, 0, 63, 20, span=3)
        self.assertFalse(got['sufficient'])
        self.assertEqual(0, got['clear'])


class IntegrationCost(unittest.TestCase):

    def test_flat_ground_costs_nothing(self):
        got = cs.integration_cost(FakeWorld(), 0, 0, 8)
        self.assertEqual(0, got['cut_blocks'] + got['fill_blocks'])
        self.assertEqual(0, got['relief_y'])

    def test_cost_is_measured_against_the_datum_that_minimises_it(self):
        w = FakeWorld()
        w.surface = lambda x, z: 63 + (10 if x > 0 else 0)
        got = cs.integration_cost(w, 0, 0, 8)
        self.assertGreater(got['moved_per_column'], 0)
        self.assertEqual(10, got['relief_y'])

    def test_it_does_not_compare_terrain_flatness_to_mesh_contact(self):
        # The first version did, and every site failed. One is a property of
        # the ground, the other of the structure.
        import inspect
        source = inspect.getsource(cs.integration_cost)
        self.assertIn('two different quantities', source)


class SiteVerification(unittest.TestCase):

    def test_a_submerged_site_is_named_as_such(self):
        w = FakeWorld(blocks={(0, 64, 0): 'minecraft:water'})
        got = cs.verify_site(w, 0, 0, of.site_requirements('end_spike'))
        self.assertFalse(got['ok'])
        self.assertIn('submerged', got['problems'][0])

    def test_terrain_surgery_is_rejected_rather_than_repaired(self):
        w = FakeWorld()
        w.surface = lambda x, z: 63 + (60 if x > 0 else 0)
        got = cs.verify_site(w, 0, 0, of.site_requirements('pillager_outpost'),
                             max_moved_per_column=10.0)
        self.assertFalse(got['ok'])
        self.assertIn('terrain surgery', ' '.join(got['problems']))

    def test_overlapping_footprints_are_caught_across_the_whole_set(self):
        # Overlap belongs to the SET, not to any one site: whichever objective
        # is authored second destroys the first, and a per-site check cannot
        # see it.
        import tempfile
        from pathlib import Path
        reqs = {k: of.site_requirements(k) for k in of.DEFENSIVE}
        with tempfile.TemporaryDirectory() as tmp:
            out = cs.verify_placements(
                Path(tmp),
                {'north': {'pillager_outpost': (0, 0), 'nether_bastion': (8, 8)}},
                reqs)
        self.assertTrue(any('overlap' in p for p in out['problems']))


if __name__ == '__main__':
    unittest.main()
