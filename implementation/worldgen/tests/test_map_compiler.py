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


class AuthoringContract(unittest.TestCase):

    def test_every_current_form_now_has_a_mesh(self):
        # This used to fail closed: siting used the measured end_spike contract
        # and no End Spike mesh existed, so the compiler stopped rather than
        # building an End Tower under the Spike's name. The mesh exists now.
        out = mc.Compilation(seed=1)
        mc.author(out)
        self.assertEqual([], out.rejections)

    def test_the_historical_mesh_is_still_not_the_current_one(self):
        from terrain_harvest.build_structures import TEMPLATES
        self.assertIn('end_spike', TEMPLATES)
        self.assertIn('end_tower', TEMPLATES)
        self.assertIsNot(TEMPLATES['end_spike'], TEMPLATES['end_tower'])
        self.assertTrue(of.is_historical('end_tower'))
        self.assertFalse(of.is_historical('end_spike'))

    def test_the_spike_reproduces_the_measured_vanilla_geometry(self):
        from terrain_harvest.build_structures import (SPIKE_HEIGHTS, SPIKE_RADII,
                                                      end_spike, spike_disc)
        # Column counts measured from a real generated End: 21 / 37 / 57 / 89.
        self.assertEqual([21, 37, 57, 89], [len(spike_disc(r)) for r in (2, 3, 4, 5)])
        self.assertEqual((2, 2, 2, 3, 3, 3, 4, 4, 4, 5), SPIKE_RADII)
        self.assertEqual(76, SPIKE_HEIGHTS[0])
        self.assertEqual(103, SPIKE_HEIGHTS[-1])
        # A single bedrock block caps the centre -- not a bedrock layer.
        t = end_spike(3, 88)
        bedrock = [k for k, v in t.items() if 'bedrock' in str(v)]
        self.assertEqual(1, len(bedrock))
        self.assertEqual((0, 88, 0), bedrock[0])
        for bad in ((1, 88), (6, 88), (3, 40), (3, 200)):
            with self.assertRaises(ValueError):
                end_spike(*bad)

    def test_the_crystal_is_a_seam_not_a_block(self):
        from terrain_harvest.build_structures import end_spike, end_spike_entities
        t = end_spike(3, 88)
        self.assertNotIn('end_crystal', str(set(map(str, t.values()))))
        self.assertEqual('minecraft:end_crystal', end_spike_entities()[0]['type'])

    def test_the_contract_is_measured_from_what_actually_gets_built(self):
        # The contract and the builder used to be two measurement paths and
        # they drifted: the Bastion's recorded height was its tallest single
        # piece while the assembled body is two stacked.
        from terrain_harvest.build_structures import TEMPLATES
        for name in of.DEFENSIVE:
            req = of.site_requirements(name)
            cells = TEMPLATES[name]()
            ys = [d[1] for d in cells]
            self.assertEqual(max(ys) - min(ys) + 1, req['vertical_clearance'], name)
            self.assertEqual(len(cells), of.FORMS[name]['solid_blocks'], name)

    def test_overlapping_footprints_are_rejected_at_authored_span(self):
        # Siting reasons in 8-block samples; the Bastion is 32 blocks across.
        # A Bastion and an Outpost 11 blocks apart both passed every analytic
        # check, and the one built second overwrote the first.
        def site(advance, x, z):
            return {'advance': advance, 'quality': 1.0, 'world_xyz': [x, 64, z]}
        far = {'pillager_outpost': [site(0.9, 0, 0)],
               'nether_bastion': [site(0.6, 200, 0)],
               'end_spike': [site(0.3, 400, 0)]}
        self.assertIsNotNone(mc.order_constrained(far))
        near = {'pillager_outpost': [site(0.9, 0, 0)],
                'nether_bastion': [site(0.6, 8, 8)],
                'end_spike': [site(0.3, 400, 0)]}
        self.assertIsNone(mc.order_constrained(near),
                          'an Outpost inside the Bastion footprint is not a layout')


if __name__ == '__main__':
    unittest.main()
