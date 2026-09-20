"""Team structure siting: scoring, depth ordering and measured symmetry."""
import unittest
from vanilla_search.structures import (LAYERS, advance_of, approach_cost, evaluate,
                                       footprint, pad_quality, score_site, symmetry)
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
        layer = LAYERS[3]           # fountain, intended rearmost at depth 0.15
        good = score_site(self.t, 12 * self.t.w + 12, layer, 10.0, 0.5 * 0.15)
        bad = score_site(self.t, 12 * self.t.w + 12, layer, 10.0, 0.5)
        self.assertGreater(good['quality'], bad['quality'])

    def test_unreachable_site_is_rejected(self):
        self.assertIsNone(score_site(self.t, 12 * self.t.w + 12, LAYERS[0], float('inf'), 0.2))

    def test_site_without_an_advance_value_is_rejected(self):
        self.assertIsNone(score_site(self.t, 12 * self.t.w + 12, LAYERS[0], 10.0, None))


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

    def test_a_crowded_layer_states_why_it_has_no_site(self):
        # Four layers cannot be separated on a narrow board; the record must say so.
        result = evaluate(candidate(w=40, h=16), {'a': (4, 8), 'b': (35, 8)}, per_layer=1)
        empty = [e for team in result['teams'].values() for e in team.values()
                 if not e['candidates']]
        self.assertTrue(empty, 'expected at least one crowded-out layer on a narrow board')
        for e in empty:
            self.assertIn('refusal', e)
            self.assertIn('clearance', e['refusal'] + str(e.get('excluded_by_clearance')))

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


class ContainmentTests(unittest.TestCase):
    """A team's structures must never be proposed in enemy territory."""

    def test_sites_stay_on_each_team_s_own_side(self):
        result = evaluate(candidate(w=32, h=32), {'a': (6, 16), 'b': (25, 16)}, per_layer=3)
        for team, home_x in (('a', 6), ('b', 25)):
            rival_x = 25 if team == 'a' else 6
            for layer in LAYERS:
                for site in result['teams'][team][layer['id']]['candidates']:
                    sx = site['sample'][0]
                    self.assertLess(abs(sx - home_x), abs(sx - rival_x),
                                    f"{team}/{layer['id']} at sample {site['sample']} "
                                    f"is nearer the rival homeland")

    def test_own_half_is_reported(self):
        result = evaluate(candidate(w=32, h=32), {'a': (6, 16), 'b': (25, 16)}, per_layer=1)
        self.assertIn('own_half_samples', result)
        for team in ('a', 'b'):
            self.assertGreater(result['own_half_samples'][team], 0)
        self.assertIn('enemy territory', result['containment'])


class CollisionTests(unittest.TestCase):
    """Two structures must not claim the same ground."""

    def test_best_sites_are_mutually_clear(self):
        result = evaluate(candidate(w=32, h=32), {'a': (6, 16), 'b': (25, 16)}, per_layer=1)
        for team in ('a', 'b'):
            chosen = []
            for layer in LAYERS:
                c = result['teams'][team][layer['id']]['candidates']
                if c: chosen.append((c[0]['sample'], layer['radius_samples']))
            for i, (pa, ra) in enumerate(chosen):
                for pb, rb in chosen[i + 1:]:
                    gap = max(abs(pa[0] - pb[0]), abs(pa[1] - pb[1]))
                    self.assertGreater(gap, ra + rb,
                                       f"{team}: {pa} and {pb} overlap")


class AdvanceAxisTests(unittest.TestCase):
    """Forward must mean toward the rival, not merely far from home."""

    def test_advance_is_zero_at_home_and_half_at_the_midline(self):
        self.assertAlmostEqual(advance_of(0.0, 100.0), 0.0)
        self.assertAlmostEqual(advance_of(50.0, 50.0), 0.5)
        self.assertIsNone(advance_of(float('inf'), 10.0))
        self.assertIsNone(advance_of(10.0, None))

    def test_forward_layers_sit_nearer_the_rival_than_rear_layers(self):
        result = evaluate(candidate(w=96, h=24), {'a': (8, 12), 'b': (87, 12)}, per_layer=1)
        order = ['aether_fountain', 'end_tower', 'nether_bastion', 'pillager_outpost']
        for team in ('a', 'b'):
            advances = []
            for layer_id in order:
                c = result['teams'][team][layer_id]['candidates']
                self.assertTrue(c, f'{team}/{layer_id} produced no site')
                advances.append(c[0]['advance'])
            # Rear to front, advance must increase toward the midline.
            self.assertLess(advances[0], advances[-1],
                            f'{team}: fountain {advances[0]} should be behind outpost {advances[-1]}')

    def test_no_site_crosses_the_midline(self):
        result = evaluate(candidate(w=96, h=24), {'a': (8, 12), 'b': (87, 12)}, per_layer=2)
        for team in ('a', 'b'):
            for layer in LAYERS:
                for site in result['teams'][team][layer['id']]['candidates']:
                    self.assertLessEqual(site['advance'], 0.5, f"{team}/{layer['id']} crossed the midline")


class BehindBaseTests(unittest.TestCase):
    """A defensive layer must not sit further from the threat than its own base."""

    def test_no_site_is_further_from_the_rival_than_home(self):
        import math
        from vanilla_search.structures import LAYERS as L
        from vanilla_search.task_a import Terrain, shortest
        c = candidate(w=96, h=24)
        result = evaluate(c, {'a': (8, 12), 'b': (87, 12)}, per_layer=2)
        t = Terrain(c)
        homes = {'a': 12 * t.w + 8, 'b': 12 * t.w + 87}
        costs = {k: shortest(t.adj, {v: 0.0})[0] for k, v in homes.items()}
        for team, rival in (('a', 'b'), ('b', 'a')):
            limit = costs[rival][homes[team]]
            for layer in L:
                for site in result['teams'][team][layer['id']]['candidates']:
                    idx = site['sample'][1] * t.w + site['sample'][0]
                    self.assertLessEqual(costs[rival].get(idx, math.inf), limit + 1e-6,
                                         f"{team}/{layer['id']} sits behind its own base")
