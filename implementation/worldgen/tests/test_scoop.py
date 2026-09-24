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
        self.assertEqual(sym['mirror_deviation_blocks'], 0.0)

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
        self.assertGreater(out['axes']['x']['mirror_deviation_blocks'],
                           out['axes']['z']['mirror_deviation_blocks'])


class Tilt(unittest.TestCase):
    """Two different slopes, and only one of them is harmless.

    A slope running W-E with the teams at the N and S ends is fine and needs
    no special handling -- height varies only with x, so mirroring N onto S
    changes nothing. A slope running ALONG the team axis puts one team above
    the other, which is a real deficit. An earlier version of this module
    treated both as harmless and detrended them away.
    """

    def test_a_slope_across_the_landmass_is_not_a_deficit(self):
        """W-E slope, N-S teams: clean without any correction at all."""
        w, d = 20, 20
        height = [64 + i for _ in range(d) for i in range(w)]
        sym = scoop.symmetry(height, w, d, 'z')
        self.assertEqual(sym['mirror_deviation_blocks'], 0.0)
        self.assertEqual(sym['mirror_tilt_blocks'], 0.0)
        self.assertEqual(sym['mirror_residual_blocks'], 0.0)

    def test_a_slope_along_the_team_axis_IS_a_deficit(self):
        """One team on high ground must not read as a clean mirror."""
        w, d = 20, 20
        height = [64 + j for j in range(d) for _ in range(w)]
        sym = scoop.symmetry(height, w, d, 'z')
        self.assertGreater(sym['mirror_deviation_blocks'], 0)
        self.assertGreater(sym['mirror_tilt_blocks'], 0)
        # the decomposition still says WHY: it is all tilt, no character gap
        self.assertEqual(sym['mirror_residual_blocks'], 0.0)

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


class Search(unittest.TestCase):
    """Finding scoops, with no Default parameter in play."""

    def _region(self, W, D, planted):
        """Ground with no accidental symmetry, and one mirrored band planted.

        The first fixture used sin(i/7) here and the search correctly found an
        x-mirror in it that scored BETTER than the planted z-mirror -- the
        base terrain was periodic in x with a period near the scoop width, so
        it was genuinely mirror-symmetric about its own centre. A smooth
        analytic profile is a bad control: ramps, sinusoids and parabolas are
        all mirror-symmetric about something. Deterministic noise is not.
        """
        import random
        rng = random.Random(11)
        h = [64 + rng.uniform(-12, 12) for _ in range(W * D)]
        j0, j1 = planted
        mid = (j0 + j1) // 2
        for j in range(mid, j1):
            for i in range(W):
                h[j * W + i] = h[(2 * mid - 1 - j) * W + i]
        return h

    def test_it_finds_the_mirrored_band_on_pure_land(self):
        W, D = 60, 60
        h = self._region(W, D, (10, 50))
        # stride 3 so the mirror line at sample 30 is actually REACHABLE: a
        # 30-sample scoop centred there starts at 15. With uncorrelated noise
        # a one-sample offset destroys the match entirely, and at stride 4 the
        # search stepped straight over the band it was meant to find. Real
        # terrain is correlated over tens of blocks so it is far more
        # forgiving, but the stride still bounds how precisely a mirror line
        # can be located, and that is a property of the search, not a bug.
        out = scoop.search(h, W, D, spacing=8, sizes=[(240, 240)],
                           stride=3, budget=6, coarsen=2, probe_blocks=48)
        best = out['scoops'][0]
        worst = out['scoops'][-1]
        self.assertEqual(best['team_axis'], 'z')
        # the planted band spans samples 10..50, so the winner sits inside it
        self.assertGreaterEqual(best['origin_sample'][1], 8)
        # and it is a far cleaner mirror than the worst scoop kept
        self.assertLess(best['mirror_deviation_blocks'],
                        worst['mirror_deviation_blocks'] / 2)

    def test_biome_is_opt_in_and_reported_when_absent(self):
        """A weaker claim than this test used to make, and a true one.

        It used to assert `search` had no biome parameter at all. That became
        false when type tutoring was added, and correctly so -- recognising an
        archipelago needs water. The claim now is that biome is OPT-IN: with
        no biome passed the search reads elevation only, and it says which
        type inputs it lacked rather than letting a water predicate quietly
        match nothing.
        """
        W, D = 40, 40
        out = scoop.search([64] * (W * D), W, D, spacing=8,
                           sizes=[(160, 160)], stride=8, budget=2)
        self.assertTrue(out['uses_no_default_parameters'])
        self.assertEqual(out['type_inputs'], [])
        for k in out['scoops']:
            self.assertNotIn('water_fraction', k)
        import inspect
        params = set(inspect.signature(scoop.search).parameters)
        self.assertEqual(params & {'ocean', 'resource'}, set())
        self.assertIn('height', params)

    def test_the_exact_tier_is_bounded_by_budget(self):
        W, D = 60, 60
        h = self._region(W, D, (10, 50))
        out = scoop.search(h, W, D, spacing=8, sizes=[(160, 160)],
                           stride=2, budget=3, coarsen=2, probe_blocks=48)
        self.assertGreater(out['considered'], 100)
        self.assertLessEqual(out['measured_exactly'], 3)

    def test_a_tilt_along_the_team_axis_worsens_every_scoop(self):
        """Reverses an earlier test, which asserted the opposite.

        That one added a W-E tilt -- harmless, since the teams sit N and S --
        checked the winner had not moved, and the ranking was then built to
        ignore tilt in general. Tilting along the TEAM axis puts a team on
        high ground, and must make the region measurably worse.
        """
        W, D = 60, 60
        h = self._region(W, D, (10, 50))
        along_team_axis = [v + 0.8 * (i // W) for i, v in enumerate(h)]
        across = [v + 0.8 * (i % W) for i, v in enumerate(h)]
        kw = dict(spacing=8, sizes=[(240, 240)], stride=3, budget=4,
                  coarsen=2, probe_blocks=48)
        a = scoop.search(h, W, D, **kw)['scoops'][0]
        b = scoop.search(along_team_axis, W, D, **kw)['scoops'][0]
        c = scoop.search(across, W, D, **kw)['scoops'][0]
        self.assertEqual(a['team_axis'], 'z')
        self.assertGreater(b['mirror_deviation_blocks'],
                           a['mirror_deviation_blocks'])
        self.assertGreater(abs(b['mirror_tilt_blocks']),
                           abs(a['mirror_tilt_blocks']))
        # the harmless tilt leaves the best z-scoop essentially untouched
        self.assertAlmostEqual(c['mirror_deviation_blocks'],
                               a['mirror_deviation_blocks'], delta=0.5)


class Capability(unittest.TestCase):
    """What kind of place a scoop is, which symmetry alone cannot say."""

    def _domes(self, W, D):
        import random
        rng = random.Random(3)
        h = []
        for j in range(D):
            for i in range(W):
                b = 0
                for (ci, cj) in ((15, 15), (45, 15), (15, 45), (45, 45), (30, 30)):
                    b = max(b, max(0, 40 - ((i - ci) ** 2 + (j - cj) ** 2) / 8))
                h.append(64 + b + rng.uniform(-1, 1))
        return h

    def test_search_cannot_tell_an_archipelago_from_a_highland(self):
        """The blindness, asserted rather than described.

        One heightmap of five domes is an archipelago at sea level 64 and a
        hill field at sea level 0. `search` reads elevation only, so it must
        return the identical vector -- and this test exists to fail the day
        someone quietly adds a water term and thinks the gap has closed.
        """
        W = D = 60
        h = self._domes(W, D)
        kw = dict(spacing=8, sizes=[(320, 320)], stride=4, budget=2,
                  coarsen=2, probe_blocks=48)
        a = scoop.search(h, W, D, **kw)['scoops'][0]
        b = scoop.search(list(h), W, D, **kw)['scoops'][0]
        self.assertEqual(a, b)
        self.assertNotIn('submerged_fraction', a)

    def test_sea_level_makes_water_visible(self):
        W = D = 60
        h = self._domes(W, D)
        self.assertGreater(scoop.capability(h, W, D, sea_level=80)
                           ['water']['submerged_fraction'], 0.2)
        self.assertEqual(scoop.capability(h, W, D, sea_level=0)
                         ['water']['submerged_fraction'], 0.0)

    def test_a_missing_input_is_reported_not_assumed(self):
        """Absent biome data must read as a gap, never as 'no desert here'."""
        W = D = 40
        c = scoop.capability([64] * (W * D), W, D)
        self.assertIsNone(c['material_mix'])
        self.assertIn('material_gap', c)
        self.assertIsNone(c['water'])
        self.assertIn('water_gap', c)

    def test_material_is_summarised_when_given(self):
        W = D = 20
        mat = ['sand'] * 300 + ['grass'] * 100
        c = scoop.capability([64] * (W * D), W, D, material=mat)
        self.assertAlmostEqual(c['material_mix']['sand'], 0.75)

    def test_no_type_is_assigned(self):
        W = D = 40
        flat = repr(scoop.capability(self._domes(40, 40), W, D)).lower()
        for name in ('archipelago', 'chasm', 'valley', 'default', 'desert'):
            self.assertNotIn("'" + name + "'", flat)


class WaterStructure(unittest.TestCase):
    """Arrangement, not amount. A fraction cannot separate these."""

    W = D = 60

    def _domes(self, centres, r2, amp, seed=3):
        import random
        rng = random.Random(seed)
        h = []
        for j in range(self.D):
            for i in range(self.W):
                b = 0
                for (ci, cj) in centres:
                    b = max(b, max(0, amp - ((i - ci) ** 2 + (j - cj) ** 2) / r2))
                h.append(64 + b + rng.uniform(-0.4, 0.4))
        return h

    def test_an_archipelago_and_a_flooded_plain_differ_in_structure(self):
        """Not in submerged fraction, which is why the fraction is not enough."""
        import random
        rng = random.Random(3)
        five = self._domes(((14, 14), (46, 14), (14, 46), (46, 46), (30, 30)), 3.0, 40)
        plain = [70 + rng.uniform(-0.4, 0.4) for _ in range(self.W * self.D)]
        for j in range(self.D):
            for i in range(self.W):
                if (i - 30) ** 2 + (j - 30) ** 2 < 21 ** 2:
                    plain[j * self.W + i] = 60
        a = scoop.water_structure(five, self.W, self.D, 80)
        b = scoop.water_structure(plain, self.W, self.D, 65)
        self.assertEqual(a['land_bodies'], 5)
        self.assertEqual(b['land_bodies'], 1)
        self.assertLess(a['largest_land_share'], 0.4)
        self.assertEqual(b['largest_land_share'], 1.0)
        self.assertGreater(a['coastline_per_1k_blocks2'],
                           2 * b['coastline_per_1k_blocks2'])

    def test_the_fraction_alone_cannot_do_it(self):
        """Two arrangements at essentially the same submerged fraction."""
        import random
        rng = random.Random(3)
        five = self._domes(((14, 14), (46, 14), (14, 46), (46, 46), (30, 30)), 8.0, 40)
        plain = [70 + rng.uniform(-0.4, 0.4) for _ in range(self.W * self.D)]
        for j in range(self.D):
            for i in range(self.W):
                if (i - 30) ** 2 + (j - 30) ** 2 < 17 ** 2:
                    plain[j * self.W + i] = 60
        a = scoop.water_structure(five, self.W, self.D, 80)
        b = scoop.water_structure(plain, self.W, self.D, 65)
        self.assertAlmostEqual(a['submerged_fraction'],
                               b['submerged_fraction'], delta=0.02)
        self.assertGreater(a['coastline_per_1k_blocks2'],
                           2 * b['coastline_per_1k_blocks2'])

    def test_a_dry_highland_has_no_water_bodies_at_all(self):
        one = self._domes(((30, 30),), 60.0, 40)
        w = scoop.water_structure(one, self.W, self.D, 0)
        self.assertEqual(w['water_bodies'], 0)
        self.assertEqual(w['submerged_fraction'], 0.0)
        self.assertEqual(w['coastline_per_1k_blocks2'], 0.0)

    def test_components_are_four_connected(self):
        """Diagonal touching is not contiguity: two islands, not one."""
        m = [True, False, False, True]
        self.assertEqual(scoop._components(m, 2, 2), [1, 1])


class Tutoring(unittest.TestCase):
    """Type predicates steering the budget, without becoming a discard rule."""

    def _sea(self, W, D, water_rows):
        """Flat land, with `water_rows` at the top drowned and featureless."""
        h = []
        import random
        rng = random.Random(9)
        for j in range(D):
            for i in range(W):
                h.append(40 if j < water_rows else 70 + rng.uniform(-4, 4))
        return h

    def test_blind_ranking_prefers_featureless_water(self):
        """The degeneracy tutoring exists to answer.

        Open water is flat, so it mirrors itself perfectly and wins a
        symmetry ranking outright. Same failure as `window_search` scoring
        ocean upward, reached from the opposite direction.
        """
        W, D = 60, 60
        h = self._sea(W, D, 40)
        out = scoop.search(h, W, D, spacing=8, sizes=[(240, 240)], stride=4,
                           budget=4, coarsen=2, probe_blocks=48,
                           biome=[0] * (W * D), sea_level=63)
        self.assertGreater(out['scoops'][0]['water_fraction'], 0.9)

    def test_tutoring_surfaces_the_land_the_blind_search_misses(self):
        W, D = 60, 60
        h = self._sea(W, D, 40)
        kw = dict(spacing=8, sizes=[(240, 240)], stride=4, coarsen=2,
                  probe_blocks=48, biome=[0] * (W * D), sea_level=63)
        blind = scoop.search(h, W, D, budget=4, **kw)
        tut = scoop.search(h, W, D, budget=2, types={
            'wet': lambda c: c['water_fraction'] > 0.5,
            'dry': lambda c: c['water_fraction'] <= 0.5}, **kw)
        self.assertFalse(any(k['water_fraction'] <= 0.5 for k in blind['scoops']))
        self.assertTrue(any(k['water_fraction'] <= 0.5 for k in tut['scoops']))

    def test_an_unmatched_scoop_is_measured_not_discarded(self):
        """A sixteenth type nobody defined must still come back described."""
        W, D = 60, 60
        h = self._sea(W, D, 40)
        out = scoop.search(h, W, D, spacing=8, sizes=[(240, 240)], stride=4,
                           budget=2, coarsen=2, probe_blocks=48,
                           biome=[0] * (W * D), sea_level=63,
                           types={'impossible': lambda c: c['water_fraction'] > 2})
        self.assertIn('unlabelled', out['partitions'])
        self.assertTrue(out['scoops'])
        self.assertIn('mirror_deviation_blocks', out['scoops'][0])

    def test_a_type_matching_nothing_is_reported_not_silent(self):
        W, D = 60, 60
        out = scoop.search(self._sea(W, D, 40), W, D, spacing=8,
                           sizes=[(240, 240)], stride=4, budget=2, coarsen=2,
                           probe_blocks=48, biome=[0] * (W * D), sea_level=63,
                           types={'impossible': lambda c: c['water_fraction'] > 2})
        self.assertEqual(out['types_matching_nothing'], ['impossible'])

    def test_without_biome_the_missing_inputs_are_named(self):
        """An archipelago predicate must not silently match nothing."""
        W, D = 40, 40
        out = scoop.search([64] * (W * D), W, D, spacing=8, sizes=[(160, 160)],
                           stride=8, budget=2, coarsen=2, probe_blocks=48)
        self.assertEqual(out['type_inputs'], [])

    def test_a_returned_scoop_can_be_relabelled_with_the_same_predicate(self):
        """The exact tier must not drop the facts the screen steered on."""
        W, D = 60, 60
        pred = lambda c: c['water_fraction'] > 0.5
        out = scoop.search(self._sea(W, D, 40), W, D, spacing=8,
                           sizes=[(240, 240)], stride=4, budget=2, coarsen=2,
                           probe_blocks=48, biome=[0] * (W * D), sea_level=63,
                           types={'wet': pred})
        for k in out['scoops']:
            self.assertIn('water_fraction', k)
            pred(k)


class Partition(unittest.TestCase):
    def test_a_rare_kind_survives_ranking_only_when_bucketed(self):
        """A global budget spent on symmetry returns plains, and only plains."""
        import random
        rng = random.Random(5)
        W = D = 90
        h = [64 + rng.uniform(-1, 1) for _ in range(W * D)]
        for j in range(10, 34):                      # one small rugged pocket
            for i in range(10, 34):
                h[j * W + i] = 64 + rng.uniform(-1, 1) + 18 * ((i // 3 + j // 3) % 2)
        kw = dict(spacing=8, sizes=[(160, 160)], stride=4, budget=3,
                  coarsen=2, probe_blocks=48)
        rugged = lambda c: c['roughness_gap'] + c['mean_elevation'] - 64 > 2
        glob = scoop.search(h, W, D, **kw)
        part = scoop.search(h, W, D, partition=lambda c: 'rugged' if rugged(c) else 'plain', **kw)

        def inside(s):
            i, j = s['origin_sample']
            return 8 <= i <= 34 and 8 <= j <= 34
        self.assertFalse(any(inside(s) for s in glob['scoops']))
        self.assertTrue(any(inside(s) for s in part['scoops']))
        self.assertEqual(set(part['partitions']), {'rugged', 'plain'})

    def test_budget_is_per_bucket(self):
        import random
        rng = random.Random(5)
        W = D = 60
        h = [64 + rng.uniform(-1, 1) for _ in range(W * D)]
        out = scoop.search(h, W, D, spacing=8, sizes=[(160, 160)], stride=4,
                           budget=2, coarsen=2, probe_blocks=48,
                           partition=lambda c: c['i'] % 2)
        for n in out['partitions'].values():
            self.assertLessEqual(n, 2)


if __name__ == '__main__':
    unittest.main()
