import unittest

from terrain_harvest import portfolio


def cell(n, north, south, **kw):
    base = {'cell': [n, 0], 'centroid': [n * 64, 0], 'mean_surface_y': 70,
            'strategic_depth_cost': {'north': north, 'south': south},
            'fauna': {}, 'vegetation': {}, 'regenerative_vocabulary': []}
    base.update(kw)
    return base


ALL = ['chicken', 'rabbit', 'cow', 'pig', 'sheep', 'carrot', 'potato']


def ladder(n=9, vocab=ALL):
    return [cell(i, 50.0 + i * 80, 900.0 - i * 80, regenerative_vocabulary=vocab)
            for i in range(n)]


class Ordering(unittest.TestCase):
    """The recovered near/farther/deeper vocabulary, as an ordering."""

    def test_each_band_gets_its_own_kinds(self):
        p = portfolio.derive(ladder())
        self.assertEqual(p['by_band']['near'], ['chicken', 'rabbit'])
        self.assertEqual(p['by_band']['farther'], ['cow', 'pig'])
        self.assertEqual(p['by_band']['deeper'], ['carrot', 'potato'])

    def test_farther_and_deeper_are_ranks_not_distances(self):
        """SUPERSEDED IN PART. This asserted that scaling every cost tenfold
        moved no cell's band, which was true when all three bands were
        percentiles and is deliberately false now: `near` is the opening the
        rest of the compiler uses, an absolute cost, because a percentile near
        band sat at 625-1328 on real maps while the opening ends at 120.

        What remains a rank, and is still asserted, is the split of everything
        BEYOND the opening into farther and deeper.
        """
        beyond = [cell(i, 500.0 + i * 800, 9000.0 - i * 800,
                       regenerative_vocabulary=ALL) for i in range(9)]
        scaled = [cell(i, 5000.0 + i * 8000, 90000.0 - i * 8000,
                       regenerative_vocabulary=ALL) for i in range(9)]
        a = [s['band'] for s in portfolio.derive(beyond)['sources']]
        b = [s['band'] for s in portfolio.derive(scaled)['sources']]
        self.assertEqual(a, b)
        self.assertNotIn('near', a)          # nothing is inside the opening

    def test_sheep_are_placed_for_access_not_by_band(self):
        """Off the ladder by the recovered decision: wool leads to Banners
        and Exploration, so sheep are a material source needing reliable
        strategic access rather than livestock at a distance."""
        p = portfolio.derive(ladder())
        sheep = [s for s in p['sources'] if s['kind'] == 'sheep']
        self.assertTrue(sheep)
        for s in sheep:
            self.assertEqual(s['band'], 'strategic_access')
            self.assertLess(s['team_cost_disparity'], 0.5)
        self.assertNotIn('sheep', sum(portfolio.VOCABULARY.values(), ()))


class NoSprinkling(unittest.TestCase):
    def test_a_cell_whose_ecology_lacks_the_kind_gets_nothing(self):
        """maps.md forbids sprinkling nodes onto empty cells to hit a
        percentage. An absent opportunity is a real answer."""
        barren = [cell(i, 50.0 + i * 80, 900.0 - i * 80) for i in range(9)]
        p = portfolio.derive(barren)
        self.assertEqual(p['sources'], [])
        self.assertGreater(p['skipped_total'], 0)
        self.assertIn('real answer', p['skipped'][0]['why'])

    def test_measured_fauna_counts_as_support(self):
        cells = [cell(i, 50.0 + i * 80, 900.0 - i * 80, fauna={'chicken': 2})
                 for i in range(9)]
        p = portfolio.derive(cells)
        self.assertTrue([s for s in p['sources'] if s['kind'] == 'chicken'])
        self.assertFalse([s for s in p['sources'] if s['kind'] == 'cow'])


class NoSpeciesFloor(unittest.TestCase):
    def test_two_openings_may_carry_different_species(self):
        """Requiring named species in both would reintroduce ecological
        symmetry that doctrine denies."""
        mixed = []
        for i in range(9):
            vocab = ['chicken', 'carrot'] if i % 2 else ['rabbit', 'potato']
            mixed.append(cell(i, 50.0 + i * 80, 900.0 - i * 80,
                              regenerative_vocabulary=vocab))
        p = portfolio.derive(mixed)
        self.assertTrue(p['derived'])
        kinds = {s['kind'] for s in p['sources']}
        self.assertTrue(kinds & {'chicken', 'rabbit'})
        self.assertIn('symmetry', p['no_species_floor'])


class Floor(unittest.TestCase):
    """The functional floor: can each team begin Development in its opening?"""

    def _opening(self, **fauna):
        # Vocabulary follows the fauna. Passing ALL with no animals made a
        # 'barren' fixture that `_supports` happily manifested into, so the
        # test was asserting against ground that was not barren.
        return [cell(0, 30.0, 40.0, fauna=fauna,
                     regenerative_vocabulary=sorted(fauna))]

    def test_wild_incidence_satisfies_the_floor(self):
        """Ordinary vanilla ecology counts, because strategic manifestations
        are layered over it rather than replacing it.

        An earlier version asked only whether a DERIVED source sat in the
        opening and failed four of five real maps whose openings were full of
        cows, pigs and sheep -- farther-band kinds the derivation rightly
        declined to manifest near. The openings were not barren; the two
        layers were being conflated.
        """
        cells = self._opening(cow=6, pig=5)
        cert = portfolio.certify(portfolio.derive(cells), cells)
        self.assertTrue(cert['certified'])
        self.assertIn('cow', cert['per_team']['north']['wild_developable'])

    def test_a_genuinely_barren_opening_still_fails(self):
        cells = self._opening()
        cert = portfolio.certify(portfolio.derive(cells), cells)
        self.assertFalse(cert['certified'])
        self.assertEqual(cert['problems'][0]['code'], 'NO_OPENING_RENEWABLE')

    def test_one_team_cannot_cover_for_the_other(self):
        cells = [cell(0, 30.0, 4000.0, fauna={'cow': 4},
                      regenerative_vocabulary=ALL),
                 cell(1, 4000.0, 4000.0, fauna={'cow': 4},
                      regenerative_vocabulary=ALL)]
        cert = portfolio.certify(portfolio.derive(cells), cells)
        self.assertFalse(cert['certified'])
        self.assertEqual([p['team'] for p in cert['problems']], ['south'])

    def test_the_two_openings_need_not_hold_the_same_kinds(self):
        cells = [cell(0, 30.0, 4000.0, fauna={'rabbit': 4},
                      regenerative_vocabulary=ALL),
                 cell(1, 4000.0, 30.0, fauna={'cow': 4},
                      regenerative_vocabulary=ALL)]
        cert = portfolio.certify(portfolio.derive(cells), cells)
        self.assertTrue(cert['certified'])
        self.assertNotEqual(cert['per_team']['north']['wild_developable'],
                            cert['per_team']['south']['wild_developable'])


class Bands(unittest.TestCase):
    def test_near_is_the_opening_not_a_percentile(self):
        """Measured against five real maps, a 33rd-percentile near band sat at
        costs of 625-1328 while the opening the rest of the compiler uses ends
        at 120: only 3-7 cells of 238 lie inside it. Two fixtures describing
        different regions."""
        cells = [cell(i, 50.0 if i < 2 else 2000.0 + i,
                      50.0 if i < 2 else 2000.0 + i,
                      regenerative_vocabulary=ALL) for i in range(12)]
        p = portfolio.derive(cells, opening_cost=120.0)
        near = [s for s in p['sources'] if s['band'] == 'near']
        for s in near:
            self.assertLessEqual(min(s['strategic_depth_cost'].values()), 120.0)


class Quantities(unittest.TestCase):
    def test_capacity_separates_core_from_surplus(self):
        """Killing an animal and maintaining a population are different acts,
        so a population reduced to its core is depleted, not extinct."""
        p = portfolio.derive(ladder())
        for s in p['sources']:
            self.assertEqual(s['capacity'],
                             s['herd_core'] + s['harvestable_surplus'])
        cow = next(s for s in p['sources'] if s['kind'] == 'cow')
        self.assertGreater(cow['herd_core'], 0)
        carrot = next(s for s in p['sources'] if s['kind'] == 'carrot')
        self.assertEqual(carrot['herd_core'], 0)   # a crop has no breeding core

    def test_quantities_are_declared_fixtures(self):
        p = portfolio.derive(ladder())
        self.assertIn('DECLARED FIXTURES', p['quantities_are'])

    def test_no_cells_is_stated_not_an_empty_portfolio(self):
        p = portfolio.derive([])
        self.assertFalse(p['derived'])
        self.assertIn('cell_grid', p['why'])


if __name__ == '__main__':
    unittest.main()
