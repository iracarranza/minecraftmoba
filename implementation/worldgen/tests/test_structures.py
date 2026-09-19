"""Team structure siting: scoring, depth ordering and measured symmetry."""
import unittest
from vanilla_search.structures import (LAYERS, approach_cost, evaluate, footprint,
                                       pad_quality, score_site, symmetry)
from vanilla_search.task_a import Terrain


def rle(values):
    out = []
    for v in values:
        if out and out[-1][0] == v: out[-1][1] += 1
        else: out.append([v, 1])
    return [[v, n] for v, n in out]


def candidate(w=24, h=24, hill=False):
    n = w * h
    height = []
    for z in range(h):
        for x in range(w):
            # A ridge on one side makes relief and approach cost non-uniform.
            height.append(70 + (abs(x - w // 2) if hill else 0))
    return {
        'region': {'block_bounds': [0, w * 8 - 1, 0, h * 8 - 1]},
        'orientation': {'rotation_degrees_clockwise': 0, 'east_west_reflected': False},
        'feature_grid': {
            'width': w, 'height': h, 'sample_spacing_blocks': 8, 'order': 'oriented row-major',
            'height_rle': rle(height),
            'actual_surface_water_rle': rle([0] * n),
            'biome_rle': rle(['minecraft:plains'] * n),
            'surface_block_rle': rle(['minecraft:grass_block'] * n),
            'buildable_mask_rle': rle([1] * n),
            'canopy_mask_rle': rle([0] * n),
            'open_ground_mask_rle': rle([1] * n),
            'highland_mask_rle': rle([0] * n),
            'forest_mask_rle': rle([0] * n),
        },
    }


class FootprintTests(unittest.TestCase):
    def setUp(self): self.t = Terrain(candidate())

    def test_footprint_is_a_full_square_inland(self):
        cells = footprint(self.t, 12 * self.t.w + 12, 2)
        self.assertEqual(len(cells), 25)

    def test_edge_footprint_is_clipped_and_rejected(self):
        self.assertIsNone(pad_quality(self.t, 0, 2))

    def test_flat_ground_reports_zero_relief(self):
        pad = pad_quality(self.t, 12 * self.t.w + 12, 2)
        self.assertEqual(pad['relief_y'], 0)
        self.assertEqual(pad['buildable_fraction'], 1.0)

    def test_relief_is_measured_on_a_ridge(self):
        t = Terrain(candidate(hill=True))
        pad = pad_quality(t, 12 * t.w + 4, 2)
        self.assertGreater(pad['relief_y'], 0)


class ScoringTests(unittest.TestCase):
    def setUp(self): self.t = Terrain(candidate())

    def test_approach_cost_is_reported(self):
        a = approach_cost(self.t, 12 * self.t.w + 12, 2)
        self.assertGreater(a['ring_samples'], 0)
        self.assertGreaterEqual(a['approach_spread'], 0)

    def test_site_at_intended_depth_beats_one_at_the_wrong_depth(self):
        layer = LAYERS[3]           # fountain, intended deep at 0.15
        band = 100.0
        good = score_site(self.t, 12 * self.t.w + 12, layer, band * 0.15, band)
        bad = score_site(self.t, 12 * self.t.w + 12, layer, band * 0.95, band)
        self.assertGreater(good['quality'], bad['quality'])

    def test_unreachable_site_is_rejected(self):
        self.assertIsNone(score_site(self.t, 12 * self.t.w + 12, LAYERS[0], float('inf'), 100.0))


class SymmetryTests(unittest.TestCase):
    def test_identical_terrain_reports_small_gaps(self):
        result = evaluate(candidate(), {'a': (6, 6), 'b': (17, 17)}, per_layer=3)
        self.assertEqual(len(result['symmetry']), len(LAYERS))
        for row in result['symmetry']:
            self.assertIn('quality_gap', row)
            self.assertLess(row['quality_gap'], 1.0)

    def test_every_layer_is_proposed_for_each_team(self):
        result = evaluate(candidate(), {'a': (6, 6), 'b': (17, 17)}, per_layer=2)
        for team in ('a', 'b'):
            for layer in LAYERS:
                self.assertIn(layer['id'], result['teams'][team])
                self.assertLessEqual(len(result['teams'][team][layer['id']]['candidates']), 2)

    def test_missing_site_is_unresolved_not_zero(self):
        rows = symmetry({l['id']: {'candidates': []} for l in LAYERS},
                        {l['id']: {'candidates': []} for l in LAYERS})
        self.assertTrue(all(r['status'] == 'UNRESOLVED' for r in rows))

    def test_limits_are_stated_in_the_record(self):
        result = evaluate(candidate(), {'a': (6, 6), 'b': (17, 17)}, per_layer=1)
        joined = ' '.join(result['not_covered'])
        self.assertIn('block-exact placement', joined)
        self.assertIn('final selection', joined)


if __name__ == '__main__':
    unittest.main()
