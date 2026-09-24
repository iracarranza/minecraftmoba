import json
import unittest

from terrain_harvest import map_types as MT


def scoop(**kw):
    base = {'water': 0.2, 'coast_1k': 2.0, 'land_bodies': 1, 'sep_sign': 0.5,
            'relief': 25.0, 'dev_rel': 0.28, 'central_mansion': 0}
    base.update(kw)
    return base


class Reference(unittest.TestCase):
    def test_the_reference_records_that_no_natural_cut_was_found(self):
        """The negative result is the point and must not be lost."""
        self.assertIn('none found', MT.REFERENCE['natural_cuts'])
        self.assertGreater(MT.REFERENCE['scoops'], 1000)
        self.assertGreaterEqual(len(MT.REFERENCE['seeds']), 20)

    def test_it_records_the_conditions_it_was_measured_under(self):
        """A quantile is only meaningful against a stated scan."""
        scan = MT.REFERENCE['scan']
        for key in ('half', 'step', 'sea_level', 'scoop_blocks'):
            self.assertIn(key, scan)

    def test_an_unknown_quantile_says_what_is_available(self):
        with self.assertRaises(KeyError) as e:
            MT.at('water', 'p99')
        self.assertIn('water', str(e.exception))


class Predicates(unittest.TestCase):
    def setUp(self):
        self.p = MT.predicates()

    def test_a_drowned_single_island_is_not_an_archipelago(self):
        """Fragmentation is the premise, so a fraction alone was never enough."""
        wet = MT.at('water', 'p75') + 0.1
        self.assertFalse(self.p['archipelago'](scoop(water=wet, land_bodies=1)))
        self.assertTrue(self.p['archipelago'](scoop(water=wet, land_bodies=4)))

    def test_open_water_is_named_rather_than_left_to_win_silently(self):
        """It is not a Map Type; it is the sink a blind search falls into."""
        self.assertTrue(self.p['open_water'](
            scoop(water=MT.at('water', 'p90') + 0.05)))
        self.assertIn('not a Map Type',
                      MT.DEFINITIONS['open_water']['why'])

    def test_shattered_coast_is_edge_density_at_moderate_water(self):
        mid = (MT.at('water', 'p25') + MT.at('water', 'p75')) / 2
        busy = MT.at('coast_1k', 'p75') + 1.0
        self.assertTrue(self.p['shattered_coast'](
            scoop(water=mid, coast_1k=busy)))
        self.assertFalse(self.p['shattered_coast'](
            scoop(water=mid, coast_1k=0.0)))

    def test_a_cut_and_a_ridge_are_opposite_signs(self):
        tall = MT.at('relief', 'p50') + 20
        cut = scoop(sep_sign=MT.at('sep_sign', 'p75') + 0.1, relief=tall)
        ridge = scoop(sep_sign=MT.at('sep_sign', 'p25') - 0.1, relief=tall)
        self.assertTrue(self.p['divided_by_a_cut'](cut))
        self.assertFalse(self.p['divided_by_a_ridge'](cut))
        self.assertTrue(self.p['divided_by_a_ridge'](ridge))
        self.assertFalse(self.p['divided_by_a_cut'](ridge))

    def test_flat_ground_is_neither_divided(self):
        flat = 1.0
        self.assertFalse(self.p['divided_by_a_cut'](
            scoop(sep_sign=0.99, relief=flat)))
        self.assertFalse(self.p['divided_by_a_ridge'](
            scoop(sep_sign=-0.99, relief=flat)))

    def test_a_mansion_predicate_is_presence_not_a_quantile(self):
        self.assertFalse(self.p['central_mansion'](scoop(central_mansion=0)))
        self.assertTrue(self.p['central_mansion'](scoop(central_mansion=1)))
        self.assertIn('Not a quantile',
                      MT.DEFINITIONS['central_mansion']['why'])

    def test_every_predicate_has_a_definition_and_vice_versa(self):
        self.assertEqual(set(self.p), set(MT.DEFINITIONS))
        for name, d in MT.DEFINITIONS.items():
            self.assertTrue(d['cut'] and d['why'], name)

    def test_predicates_track_a_re_measured_reference(self):
        """A quantile stays correct when the reference moves; a number does not."""
        moved = json.loads(json.dumps(MT.REFERENCE))
        moved['quantiles']['water']['p25'] = 0.9
        wet = MT.predicates(reference=moved)
        self.assertTrue(wet['landmass'](scoop(water=0.5)))
        self.assertFalse(self.p['landmass'](scoop(water=0.5)))


class Gate(unittest.TestCase):
    def test_the_symmetry_bound_is_on_the_ratio_over_contested_ground(self):
        b = MT.symmetry_bound()
        self.assertEqual(b['feature'], 'dev_rel')
        self.assertIn('contested', b['measured_over'])
        self.assertIn('not_per_type', b)

    def test_it_is_one_bound_for_every_type(self):
        """Per-Type bounds were deferred, not adopted."""
        self.assertIn('unexplained', MT.symmetry_bound()['not_per_type'])


if __name__ == '__main__':
    unittest.main()
