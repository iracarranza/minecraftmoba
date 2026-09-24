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

    def test_bands_are_ranks_not_distances(self):
        """No block figures were ever decided, and maps.md marks depth OPEN.

        Scaling every cost tenfold must not move a single cell's band.
        """
        near = portfolio.derive(ladder())
        far = portfolio.derive([cell(i, 500.0 + i * 800, 9000.0 - i * 800,
                                     regenerative_vocabulary=ALL)
                                for i in range(9)])
        self.assertEqual([s['band'] for s in near['sources']],
                         [s['band'] for s in far['sources']])

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
