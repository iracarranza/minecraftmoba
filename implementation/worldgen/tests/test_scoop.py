import unittest

from terrain_harvest import scoop


def grid(height, width, depth, spacing=8):
    runs = []
    for v in height:
        if runs and runs[-1][0] == v:
            runs[-1][1] += 1
        else:
            runs.append([v, 1])
    return {'width': width, 'height': depth, 'sample_spacing_blocks': spacing,
            'height_rle': runs}


class Symmetry(unittest.TestCase):
    def test_a_literal_mirror_has_zero_deviation(self):
        w, d = 12, 12
        rows = [[i * 3 for i in range(w)] for _ in range(d // 2)]
        height = [v for r in rows for v in r] + [v for r in reversed(rows) for v in r]
        sym = scoop.symmetry(height, w, d, 'z')
        self.assertEqual(sym['mirror_residual_blocks'], 0.0)

    def test_flat_north_hilly_south_passes_relief_but_fails_roughness(self):
        """The discard case, and the reason roughness is measured separately.

        A flat half and a half of hillocks spanning the same vertical range
        have the SAME relief. Relief alone calls them equivalent. Only the
        neighbour-difference term separates them.
        """
        w, d = 16, 16
        flat = [64] * (w * (d // 2))
        hilly = []
        for j in range(d // 2):
            for i in range(w):
                hilly.append(64 + (10 if (i + j) % 2 else 0))
        sym = scoop.symmetry(flat + hilly, w, d, 'z')
        self.assertGreater(sym['relief_blocks']['b'], sym['relief_blocks']['a'])
        self.assertGreater(sym['roughness_gap'], 0.9)

    def test_a_long_ramp_and_a_hillock_field_differ_only_in_roughness(self):
        w, d = 16, 16
        ramp = [j for j in range(d // 2) for _ in range(w)]
        field = []
        for j in range(d // 2):
            for i in range(w):
                field.append((d // 2 - 1) if (i + j) % 2 else 0)
        sym = scoop.symmetry(ramp + field, w, d, 'z')
        self.assertLess(sym['relief_gap'], 0.2)      # same vertical span
        self.assertGreater(sym['roughness_gap'], 0.5)

    def test_both_axes_are_read(self):
        w, d = 20, 20
        height = [64 + (i // 4) for j in range(d) for i in range(w)]
        out = scoop.describe(grid(height, w, d))
        self.assertEqual(set(out['axes']), {'x', 'z'})
        # The gradient runs along x, so an x split separates the halves and a
        # z split does not.
        self.assertGreater(out['axes']['x']['mirror_residual_blocks'],
                           out['axes']['z']['mirror_residual_blocks'])


class Tilt(unittest.TestCase):
    """A scoop laid across a slope is still mirrored, and must read as one."""

    def test_a_pure_slope_across_the_split_is_all_tilt_and_no_residual(self):
        w, d = 20, 20
        height = [64 + j for j in range(d) for _ in range(w)]
        sym = scoop.symmetry(height, w, d, 'z')
        self.assertEqual(sym['mirror_residual_blocks'], 0.0)
        self.assertGreater(sym['mirror_tilt_blocks'], 0)

    def test_a_slope_along_the_split_is_neither(self):
        w, d = 20, 20
        height = [64 + i for _ in range(d) for i in range(w)]
        sym = scoop.symmetry(height, w, d, 'z')
        self.assertEqual(sym['mirror_tilt_blocks'], 0.0)
        self.assertEqual(sym['mirror_residual_blocks'], 0.0)

    def test_residual_is_independent_of_tilt(self):
        """The same mismatch must measure the same on flat and on sloped ground.

        This is what a mean-offset subtraction cannot do, and the reason the
        trend is fitted: an offset leaves a ramp reporting a mismatch it does
        not have, so a mismatch sitting ON a ramp could not be read.
        """
        w, d = 20, 20
        def mismatch(base):
            return [base(i, j) + (9 if j > d // 2 and (i + j) % 2 else 0)
                    for j in range(d) for i in range(w)]
        flat = scoop.symmetry(mismatch(lambda i, j: 64), w, d, 'z')
        sloped = scoop.symmetry(mismatch(lambda i, j: 64 + j), w, d, 'z')
        self.assertAlmostEqual(flat['mirror_residual_blocks'],
                               sloped['mirror_residual_blocks'], places=1)
        self.assertGreater(sloped['mirror_tilt_blocks'], flat['mirror_tilt_blocks'])


class HomebasePair(unittest.TestCase):
    def test_the_pair_is_scored_on_the_worse_end(self):
        """A superb north opposite a cliff is not half a map."""
        w, d = 40, 40
        height = [64] * (w * d)
        for j in range(d - 12, d):          # gouge the southern end
            for i in range(w):
                height[j * w + i] = 64 + (40 if i % 2 else 0)
        pair = scoop.homebase_pair(height, w, d, 'z', spacing=8, probe_blocks=32)
        for p in pair['frontier']:
            self.assertEqual(p['worst_grade_blocks'], max(p['grade_blocks'].values()))
        self.assertGreater(pair['grade_at_max_separation'], 0)

    def test_separation_is_reported_for_size(self):
        w, d = 40, 60
        out = scoop.describe(grid([64] * (w * d), w, d))
        self.assertIsNotNone(out['axes']['z']['homebase_max_separation_blocks'])
        self.assertGreater(out['axes']['z']['homebase_max_separation_blocks'], 0)

    def test_a_scoop_smaller_than_two_discs_says_so(self):
        w, d = 6, 6
        pair = scoop.homebase_pair([64] * (w * d), w, d, 'z', spacing=8, probe_blocks=96)
        self.assertIn('why', pair)
        self.assertEqual(pair['frontier'], [])


class Size(unittest.TestCase):
    def test_area_is_reported_relative_to_the_current_window(self):
        out = scoop.describe(grid([64] * (108 * 132), 108, 132))
        self.assertEqual(out['scoop_blocks'], [864, 1056])
        self.assertEqual(out['area_over_base'], 1.0)

    def test_no_size_label_is_assigned(self):
        out = scoop.describe(grid([64] * (40 * 40), 40, 40))
        flat = repr(out).lower()
        for label in ('"normal"', "'normal'", "'large'", "'vast'"):
            self.assertNotIn(label, flat)


class Frontier(unittest.TestCase):
    def test_the_frontier_is_monotone_in_both_terms(self):
        """Every entry must beat every wider one on flatness, or it is dominated."""
        w, d = 30, 30
        height = [64 + (j // 3) * 2 for j in range(d) for _ in range(w)]
        pair = scoop.homebase_pair(height, w, d, 'z', spacing=8, probe_blocks=32)
        seps = [p['separation_blocks'] for p in pair['frontier']]
        grades = [p['worst_grade_blocks'] for p in pair['frontier']]
        self.assertEqual(seps, sorted(seps, reverse=True))
        self.assertEqual(grades, sorted(grades, reverse=True))

    def test_max_separation_is_a_property_of_the_scoop_not_of_the_terrain(self):
        """The bug this replaced: separation reported where flat ground sat.

        Two scoops of identical shape must report the same MAX separation
        however differently their ground is arranged, because the widest
        mirrored pair is geometry.
        """
        w, d = 30, 30
        flat = [64] * (w * d)
        lumpy = [64 + (7 if (i * j) % 5 else 0) for j in range(d) for i in range(w)]
        a = scoop.homebase_pair(flat, w, d, 'z', spacing=8, probe_blocks=32)
        b = scoop.homebase_pair(lumpy, w, d, 'z', spacing=8, probe_blocks=32)
        self.assertEqual(a['max_separation_blocks'], b['max_separation_blocks'])


if __name__ == '__main__':
    unittest.main()
