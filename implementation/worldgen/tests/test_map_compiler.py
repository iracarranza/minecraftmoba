"""Compiler stages, the ordinal constraint, and the Lair's one gated parity."""
import unittest

from terrain_harvest import map_compiler as mc
from terrain_harvest import objective_forms as of
from terrain_harvest import lair_socket


class Stages(unittest.TestCase):

    def test_a_failure_names_the_stage_and_a_code(self):
        out = mc.Compilation(seed=1)
        out.fail('lair', 'NO_LAIR_VOLUME', 'nothing fits')
        d = out.as_dict()
        self.assertFalse(d['playable'])
        self.assertEqual('lair', d['rejections'][0]['stage'])
        self.assertEqual('NO_LAIR_VOLUME', d['rejections'][0]['code'])

    def test_stages_run_outermost_first(self):
        self.assertEqual(('recognize', 'homebase', 'hinterland', 'objectives',
                          'lair', 'select', 'author', 'verify'), mc.STAGES)

    def test_the_corridor_warning_does_not_reject_regional_shape(self):
        # It fires on all eight screened finalists and is the straight-line
        # proxy an earlier pass showed to be a false positive. Treating it as a
        # shape failure rejected every candidate the screen ever produced.
        self.assertIn('six_corridors_connect', mc.NOT_REGIONAL_SHAPE)
        out = mc.Compilation(seed=1)
        mc.recognize({'metrics': {'western_mean_elevation_advantage_y': 5,
                                  'east_actual_ocean_fraction': .2,
                                  'open_buildable_fraction_of_land': .5},
                      'experimental_screen': {'warnings': ['six_corridors_connect']}}, out)
        self.assertEqual([], out.rejections)
        self.assertEqual(['six_corridors_connect'], out.evidence['deferred_screen_warnings'])

    def test_a_real_shape_warning_still_rejects(self):
        out = mc.Compilation(seed=1)
        mc.recognize({'metrics': {'western_mean_elevation_advantage_y': 0,
                                  'east_actual_ocean_fraction': 0,
                                  'open_buildable_fraction_of_land': 0},
                      'experimental_screen': {'warnings': ['western_relief_advantage']}}, out)
        self.assertEqual(['NO_DEFAULT_REGIONAL_SHAPE'], [r.code for r in out.rejections])


class OrdinalConstraint(unittest.TestCase):

    def site(self, advance, quality=1.0):
        return {'advance': advance, 'quality': quality}

    def test_it_picks_an_ordered_combination_over_the_best_per_slot(self):
        # The best Outpost alone starves the chain: nothing sits inside 0.10.
        pools = {
            'pillager_outpost': [self.site(0.10, 9.0), self.site(0.80, 1.0)],
            'nether_bastion': [self.site(0.50, 1.0)],
            'end_spike': [self.site(0.30, 1.0)],
        }
        got = mc.order_constrained(pools)
        self.assertIsNotNone(got)
        self.assertEqual(0.80, got['pillager_outpost']['advance'])

    def test_lateral_displacement_and_uneven_gaps_are_free(self):
        pools = {
            'pillager_outpost': [self.site(0.99)],
            'nether_bastion': [self.site(0.98)],
            'end_spike': [self.site(0.02)],
        }
        self.assertIsNotNone(mc.order_constrained(pools),
                             'only the order is invariant; gap size is not')

    def test_no_orderable_combination_is_reported_not_forced(self):
        pools = {
            'pillager_outpost': [self.site(0.10)],
            'nether_bastion': [self.site(0.50)],
            'end_spike': [self.site(0.90)],
        }
        self.assertIsNone(mc.order_constrained(pools))

    def test_the_fountain_is_not_one_of_the_choices(self):
        # It is Core geometry fixed by the homeland fit, not a fourth slot.
        pools = {k: [self.site(a)] for k, a in
                 (('pillager_outpost', 0.9), ('nether_bastion', 0.6), ('end_spike', 0.3))}
        got = mc.order_constrained(pools)
        self.assertNotIn('aether_fountain', got)

    def test_objectives_must_sit_outside_the_fountain(self):
        pools = {k: [self.site(a)] for k, a in
                 (('pillager_outpost', 0.9), ('nether_bastion', 0.6), ('end_spike', 0.0))}
        self.assertIsNone(mc.order_constrained(pools))


class LairParity(unittest.TestCase):

    def test_the_bound_comes_from_the_measured_distribution(self):
        # Best achievable A_L over eight screened finalists:
        # 0.001 0.050 0.073 | 0.226 0.294 0.415 0.678 0.787
        # The widest gap is 0.073 -> 0.226, and any bound inside it selects the
        # same three seeds, so the choice is insensitive across a band three
        # times its own width.
        bound = mc.PROVISIONAL['max_lair_access_asymmetry']
        measured = [0.001, 0.050, 0.073, 0.226, 0.294, 0.415, 0.678, 0.787]
        self.assertGreater(bound, 0.073)
        self.assertLess(bound, 0.226)
        for alternative in (0.08, 0.15, 0.22):
            self.assertEqual([m for m in measured if m <= bound],
                             [m for m in measured if m <= alternative])

    def test_access_parity_is_gated_but_terrain_contrast_is_not(self):
        # The one place parity is a real constraint. Everything else about
        # Wilderness may differ freely.
        self.assertIn('max_lair_access_asymmetry', mc.PROVISIONAL)
        self.assertNotIn('max_land_asymmetry', mc.PROVISIONAL)
        self.assertNotIn('max_accessible_land_gap', mc.PROVISIONAL)

    def test_the_search_reports_what_a_sample_grid_cannot_prove(self):
        # The Dragon's air volume is the limiting case and cannot be verified
        # from an 8-block surface grid. It is approximated and labelled, not
        # asserted.
        import inspect
        source = inspect.getsource(lair_socket)
        self.assertIn('dragon_air_volume', source)
        self.assertIn('UNVERIFIED', source)


class AuthorFailsClosed(unittest.TestCase):

    def test_no_historical_mesh_may_stand_in_for_a_current_form(self):
        out = mc.Compilation(seed=1)
        mc.author(out)
        self.assertEqual(['OBJECTIVE_MESH_MISSING'], [r.code for r in out.rejections])
        self.assertIn('end_spike', out.rejections[0].data['missing'])

    def test_siting_uses_end_spike_while_building_still_has_only_end_tower(self):
        from vanilla_search.structures import LAYERS
        from terrain_harvest.build_structures import TEMPLATES
        self.assertIn('end_spike', [l['id'] for l in LAYERS])
        self.assertNotIn('end_spike', TEMPLATES)
        self.assertIn('end_tower', TEMPLATES)
        self.assertTrue(of.is_historical('end_tower'))


if __name__ == '__main__':
    unittest.main()
