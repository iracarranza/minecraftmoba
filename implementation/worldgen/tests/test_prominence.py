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


if __name__ == '__main__':
    unittest.main()
