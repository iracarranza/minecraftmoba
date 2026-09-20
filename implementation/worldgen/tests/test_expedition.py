from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path

WORLDGEN = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(WORLDGEN))

from expedition import benchmark, manifest, vanilla
from expedition.strategy import (Deposit, Kit, branch_mining_seconds,
                                 cave_exploration_seconds, quarry_seconds)


def deposit(**kw):
    base = dict(resource='iron', blocks_exposed=40, blocks_buried=600,
                deepslate_fraction=0.28, mean_y=10.0, cave_volume=60000,
                density_at_depth=0.011)
    base.update(kw)
    return Deposit(**base)


class VanillaFormulaTests(unittest.TestCase):
    """The vanilla half has known-correct answers, so assert them directly."""

    def test_stone_pick_on_iron_ore(self):
        # 4 / 3.0 / 30 = 0.0444 damage/tick -> 23 ticks -> 1.15s.
        self.assertAlmostEqual(vanilla.break_seconds('iron_ore', 'stone'), 1.15, places=2)

    def test_deepslate_is_slower_than_stone_variant(self):
        self.assertGreater(vanilla.break_seconds('deepslate_iron_ore', 'iron'),
                           vanilla.break_seconds('iron_ore', 'iron'))

    def test_efficiency_adds_level_squared_plus_one(self):
        # Efficiency I on an iron pick: speed 6 -> 8.
        self.assertAlmostEqual(vanilla.break_seconds('stone', 'iron', 0), 0.4, places=2)
        self.assertLess(vanilla.break_seconds('stone', 'iron', 1),
                        vanilla.break_seconds('stone', 'iron', 0))

    def test_tier_gate(self):
        self.assertFalse(vanilla.can_harvest('diamond', 'stone'))
        self.assertTrue(vanilla.can_harvest('diamond', 'iron'))
        self.assertTrue(vanilla.can_harvest('coal', 'wood'))

    def test_fortune_multiplies_harvest(self):
        self.assertAlmostEqual(vanilla.expected_harvest(10, 'copper', 0), 35.0)
        self.assertAlmostEqual(vanilla.expected_harvest(10, 'copper', 3), 77.0)


class InvariantTests(unittest.TestCase):
    """Calculator spec section 19."""

    def test_fortune_changes_harvest_not_physical_blocks(self):
        d = deposit()
        plain = benchmark.evaluate(d, Kit(tool='iron'), 100)
        lucky = benchmark.evaluate(d, Kit(tool='iron', fortune=3), 100)
        # Same ore in the ground either way.
        self.assertEqual(plain['blocks_available_total'], lucky['blocks_available_total'])
        # But fewer blocks need breaking to reach the same quantity.
        self.assertLess(lucky['blocks_required'], plain['blocks_required'])

    def test_efficiency_changes_break_time_not_ore_count(self):
        d = deposit()
        slow = benchmark.evaluate(d, Kit(tool='iron', efficiency=0), 100)
        fast = benchmark.evaluate(d, Kit(tool='iron', efficiency=3), 100)
        self.assertEqual(slow['blocks_required'], fast['blocks_required'])
        self.assertLess(fast['ore_break_seconds'], slow['ore_break_seconds'])

    def test_cave_exploration_cannot_exceed_exposed_ore(self):
        d = deposit(blocks_exposed=5)
        r = cave_exploration_seconds(d, Kit(tool='iron'), 100, 300.0)
        self.assertEqual(r['blocks'], 5)
        self.assertEqual(r['short_by'], 95)

    def test_missing_density_is_unresolved_not_inferred(self):
        d = deposit(density_at_depth=0.0)
        for fn in (branch_mining_seconds, quarry_seconds):
            r = fn(d, Kit(tool='iron'), 100)
            self.assertFalse(r['feasible'])
            self.assertIn('reason', r)

    def test_unharvestable_resource_is_unresolved(self):
        r = benchmark.evaluate(deposit(resource='diamond'), Kit(tool='stone'), 10)
        self.assertIn('unresolved', r)

    def test_quarry_is_never_faster_than_branch_mining(self):
        # Branch mining inspects four blocks per block dug; a quarry inspects
        # one. If this ever inverts, the geometry constant is wrong.
        d, kit = deposit(), Kit(tool='iron')
        self.assertLess(branch_mining_seconds(d, kit, 100)['seconds'],
                        quarry_seconds(d, kit, 100)['seconds'])

    def test_slower_sweep_never_finishes_sooner(self):
        d, kit = deposit(), Kit(tool='iron')
        times = [cave_exploration_seconds(d, kit, 40, s)['seconds']
                 for s in (150.0, 300.0, 600.0)]
        self.assertEqual(times, sorted(times, reverse=True))


class ManifestTests(unittest.TestCase):
    def test_best_band_picks_the_richest_window(self):
        ore = {'0': 0, '1': 0, '2': 0, '10': 9, '11': 9, '12': 9}
        solid = {str(y): 100 for y in range(0, 20)}
        band = manifest.best_band(ore, solid, height=3)
        self.assertEqual(band['y'], 10)
        self.assertAlmostEqual(band['density'], 27 / 300)

    def test_best_band_is_none_without_measurement(self):
        self.assertIsNone(manifest.best_band({}, {'0': 10}))

    def test_export_keeps_exposed_and_buried_separate(self):
        caves = {
            'cell_size_blocks': 48, 'cells_scanned': 1, 'volume_id': 'tv_test',
            'cells': [{
                'cell_origin': [0, 0], 'mean_surface_y': 64.0, 'cave_mean_y': 10.0,
                'explorable_cave_volume': 1000, 'cave_components': 3,
                'largest_cave_component': 800, 'exposed_ore_per_1k_cave': 4.0,
                'ore_exposed': {'iron': 4}, 'ore_buried': {'iron': 96},
                'ore_mean_y': {'iron': 10.0}, 'ore_deepslate_fraction': {'iron': 0.3},
                'ore_y_histogram': {'iron': {'10': 100}},
                'solid_y_histogram': {'10': 2000},
            }],
        }
        structures = {'structures': [{'team': 'north', 'structure': 'aether_fountain',
                                      'world_xyz': [1, 2, 3]}], 'world': 'w'}
        with tempfile.TemporaryDirectory() as tmp:
            c, s, o = (Path(tmp) / n for n in ('c.json', 's.json', 'o.json'))
            c.write_text(json.dumps(caves)); s.write_text(json.dumps(structures))
            doc = manifest.run(c, s, o)
        m = doc['manifestations'][0]
        self.assertEqual((m['blocks_exposed'], m['blocks_buried']), (4, 96))
        self.assertEqual(m['physical_blocks'], 100)
        # Routes are not measured by the cave scan and must not be invented.
        self.assertIsNone(m['route_from'])
        self.assertEqual(doc['destinations'][0]['position'], [1, 3])


if __name__ == '__main__':
    unittest.main()
