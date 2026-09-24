"""Prominence: how much a feature stands out, with no threshold anywhere."""
import unittest

from terrain_harvest import prominence as pr


def grid(rows):
    flat = [v for row in rows for v in row]
    return {'width': len(rows[0]), 'height': len(rows),
            'sample_spacing_blocks': 8,
            'height_rle': [[v, 1] for v in flat]}


class Prominence(unittest.TestCase):

    def test_one_massif_and_scattered_bumps_are_told_apart(self):
        # The case the old vocabulary cannot express: highland_fraction is the
        # same for both, and they are completely different maps.
        massif = grid([[64, 64, 64, 64, 64],
                       [64, 80, 84, 80, 64],
                       [64, 84, 88, 84, 64],
                       [64, 80, 84, 80, 64],
                       [64, 64, 64, 64, 64]])
        scattered = grid([[64, 88, 64, 88, 64],
                          [64, 64, 64, 64, 64],
                          [88, 64, 64, 64, 88],
                          [64, 64, 64, 64, 64],
                          [64, 88, 64, 88, 64]])
        m = pr.describe(massif)
        s = pr.describe(scattered)
        self.assertEqual(1, len([f for f in m['peaks'] if f['prominence'] > 0]),
                         'a single massif is one feature')
        self.assertGreater(len([f for f in s['peaks'] if f['prominence'] > 0]), 3,
                           'scattered bumps are many features')

    def test_a_bump_on_a_shoulder_is_not_a_feature_at_all(self):
        # Absolute height is not the point. A bump that descends continuously
        # into higher ground never forms its own component, so it is absorbed
        # rather than recorded with a small number. That is the stronger and
        # more honest result: it is not a feature, not a minor one.
        ridge = grid([[64, 70, 100, 70, 64],
                      [64, 70, 99, 98, 64],
                      [64, 64, 64, 64, 64]])
        peaks = pr.describe(ridge)['peaks']
        self.assertNotIn(98, [f['height'] for f in peaks],
                         'a shoulder never separates from the summit it hangs off')
        self.assertIn(100, [f['height'] for f in peaks], 'the summit itself is a feature')

    def test_pits_are_measured_by_the_same_sweep(self):
        # A chasm must be describable by the measurement that describes a
        # highland, or the vocabulary is not type-neutral.
        basin = grid([[80, 80, 80, 80, 80],
                      [80, 40, 30, 40, 80],
                      [80, 80, 80, 80, 80]])
        r = pr.describe(basin)
        self.assertTrue(any(f['prominence'] >= 40 for f in r['pits']))

    def test_no_threshold_constants_exist(self):
        import inspect
        src = inspect.getsource(pr)
        self.assertIn('NO THRESHOLDS', src)
        for forbidden in ('MIN_PROMINENCE', 'PEAK_THRESHOLD', 'MIN_HEIGHT'):
            self.assertNotIn(forbidden, src)

    def test_it_names_no_compass_direction(self):
        # Seven existing metrics are one-sided; this one must not be.
        #
        # Matched on word boundaries, because the first version of this test
        # failed on the word "lowest" -- which contains "west". A substring
        # check for a compass direction is exactly the kind of assertion that
        # looks strict and is merely wrong.
        import inspect, re
        src = inspect.getsource(pr.features).lower()
        for word in ('west', 'east', 'north', 'south'):
            self.assertIsNone(re.search(rf'\b{word}\b', src),
                              f'{word} would bake a direction into the measurement')


from terrain_harvest import prominence as P  # noqa: E402


class ReliefShape(unittest.TestCase):
    def test_the_top_prominence_is_the_relief_and_says_so(self):
        """An identity, not a measurement. Comparing rank-1 peak to rank-1 pit
        always reports 'balanced' whatever the terrain is."""
        import random
        rng = random.Random(2)
        W = D = 40
        h = [64 + rng.uniform(0, 30) for _ in range(W * D)]
        r = P.relief_shape(h, W, D, spacing=8)
        self.assertTrue(r['top_prominence_equals_relief'])

    def test_the_sign_measures_what_the_feature_divides(self):
        """A ridge separates the ground into basins; a chasm into plateaus."""
        import random
        rng = random.Random(2)
        W = D = 40
        ridge = [64 + max(0, 25 - abs(i - 20) * 2) + rng.uniform(0, 2)
                 for _ in range(D) for i in range(W)]
        chasm = [64 - max(0, 25 - abs(i - 20) * 2) + rng.uniform(0, 2)
                 for _ in range(D) for i in range(W)]
        a = P.relief_shape(ridge, W, D, spacing=8)
        b = P.relief_shape(chasm, W, D, spacing=8)
        self.assertLess(a['separation_sign'], -0.2)   # divided into basins
        self.assertGreater(b['separation_sign'], 0.2)  # divided into plateaus
        self.assertGreater(a['pit_mass'], a['peak_mass'])
        self.assertGreater(b['peak_mass'], b['pit_mass'])


if __name__ == '__main__':
    unittest.main()
