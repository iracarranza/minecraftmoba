import unittest

from terrain_harvest import map_types, prospect


def row(**kw):
    base = {'water_fraction': 0.2, 'coast_density': 0.02, 'relief': 25.0}
    base.update(kw)
    return base


class TierAgreement(unittest.TestCase):
    """Predicates must survive both row shapes, or a Type is never tutored."""

    def setUp(self):
        self.p = map_types.predicates()

    def test_relief_reads_from_either_tier(self):
        """Cheap rows carry a float; exact rows carry a per-half dict."""
        cheap = map_types._get(row(relief=40.0), 'relief')
        exact = map_types._get({'relief_blocks': {'a': 10.0, 'b': 40.0}}, 'relief')
        self.assertEqual(cheap, 40.0)
        self.assertEqual(exact, 40.0)

    def test_archipelago_can_match_without_the_body_count(self):
        """`land_bodies` is connectivity, so no summed-area table carries it.
        Requiring it made archipelago match nothing across 20 seeds and
        receive no budget -- a missing input that looked like a seed property.
        """
        wet = map_types.at('water', 'p75') + 0.1
        self.assertTrue(self.p['archipelago'](
            row(water_fraction=wet, coast_density=0.2)))
        self.assertFalse(self.p['archipelago'](
            row(water_fraction=wet, coast_density=0.0)))

    def test_the_body_count_decides_when_it_is_present(self):
        wet = map_types.at('water', 'p75') + 0.1
        self.assertFalse(self.p['archipelago'](
            row(water_fraction=wet, coast_density=0.2, land_bodies=1)))
        self.assertTrue(self.p['archipelago'](
            row(water_fraction=wet, coast_density=0.0, land_bodies=5)))

    def test_shattered_coast_uses_the_exact_value_when_given(self):
        mid = (map_types.at('water', 'p25') + map_types.at('water', 'p75')) / 2
        busy = map_types.at('coast_1k', 'p75') + 1.0
        self.assertTrue(self.p['shattered_coast'](
            row(water_fraction=mid, coastline_per_1k_blocks2=busy)))
        self.assertFalse(self.p['shattered_coast'](
            row(water_fraction=mid, coastline_per_1k_blocks2=0.0)))


class Targets(unittest.TestCase):
    def test_open_water_is_never_a_generation_target(self):
        """It is defined as not a Map Type. On seed 31337 two open-water
        scoops ranked ABOVE the only dry one, so naming the sink and then
        emitting it anyway would spend generation on open sea."""
        self.assertIn('not a Map Type',
                      map_types.DEFINITIONS['open_water']['why'])

    def test_the_module_states_what_it_does_not_decide(self):
        import inspect
        doc = inspect.getdoc(prospect)
        self.assertIn('never a map', doc)
        for term in ('ore', 'caves', 'buildability'):
            self.assertIn(term, doc)


if __name__ == '__main__':
    unittest.main()
