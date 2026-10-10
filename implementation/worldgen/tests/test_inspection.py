"""The map inspection overlay's data: nothing new is measured, only assembled."""

import sys
import unittest
from pathlib import Path

WORLDGEN = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(WORLDGEN))

from terrain_harvest import inspection, reach_fields


def cell(i, j, north, south, y=70):
    return {'cell': [i, j], 'world_origin': [i * 64, j * 64], 'centroid': [i * 64 + 32, j * 64 + 32],
            'strategic_depth_cost': {'north': north, 'south': south}, 'mean_surface_y': y}


def evidence():
    return {'cell_grid': {'measured': True, 'units': 'cost', 'cells': [
        cell(0, 0, 40.0, 700.0), cell(1, 0, 100.0, 600.0), cell(2, 0, 400.0, 380.0),
        cell(3, 0, 700.0, 90.0), cell(0, 1, None, 500.0)]}}


BINDINGS = {
    'fountains': {'north': [-10, 67, 5], 'south': [900, 67, 5]},
    'objectives': {'north': {'end_spike': [1, 2]}, 'south': {'end_spike': [3, 4]}},
    'lair': {'anchor': {'xyz': [400, 68, 10]}},
    'worksites': [{'id': 'ws_1', 'world_xz': [10, 20], 'reach': {'north': 1, 'south': 2}}, {'id': 'no_pos'}],
    'renewables': {'sources': [{'id': 'near_rabbit_north_[0, 0]', 'kind': 'rabbit', 'type': 'ANIMAL', 'band': 'near',
                                'near_team': 'north', 'cell': [0, 0], 'x': 5, 'y': 70, 'z': 6, 'radius': 24,
                                'capacity': 6, 'strategic_depth_cost': {'north': 40.0, 'south': 700.0}}]},
}


class Inspection(unittest.TestCase):

    def test_there_is_no_overlay_without_a_cell_grid(self):
        self.assertIsNone(inspection.build({}))
        self.assertIsNone(inspection.build({'cell_grid': {'cells': []}}))

    def test_every_cell_is_carried_with_both_costs_and_unreachable_stays_unreachable(self):
        out = inspection.build(evidence(), BINDINGS)
        self.assertEqual(5, len(out['cells']))
        unreachable = [c for c in out['cells'] if c['cell'] == [0, 1]][0]
        self.assertIsNone(unreachable['cost']['north'])
        self.assertEqual(500.0, unreachable['cost']['south'])
        self.assertEqual('only_south', unreachable['relation'])

    def test_relation_and_bands_are_the_compilers_own_rule_not_a_restatement(self):
        ev = evidence()
        out = inspection.build(ev, BINDINGS)
        cells = ev['cell_grid']['cells']
        expected_bands = reach_fields.bands(cells, inspection.OPENING_COST)
        for row, c in zip(out['cells'], cells):
            self.assertEqual(reach_fields.relation(c, inspection.OPENING_COST), row['relation'])
            self.assertEqual(expected_bands.get(tuple(c['cell']), {}), row['band'])

    def test_a_cheap_cell_for_one_team_reads_as_that_teams(self):
        out = inspection.build(evidence(), BINDINGS)
        rel = {tuple(c['cell']): c['relation'] for c in out['cells']}
        self.assertEqual('cheap_north', rel[(0, 0)])
        self.assertEqual('cheap_south', rel[(3, 0)])

    def test_points_objectives_lair_worksites_and_fountains_come_from_the_bindings(self):
        out = inspection.build(evidence(), BINDINGS)
        self.assertEqual({'north': [-10, 67, 5], 'south': [900, 67, 5]}, out['fountains'])
        self.assertEqual(2, len(out['objectives']))
        self.assertEqual([400, 68, 10], out['lair'])
        self.assertEqual(1, len(out['worksites']), 'a worksite with no position is not drawn')
        p = out['points'][0]
        self.assertEqual(('rabbit', 'near', 'north', 5, 6), (p['kind'], p['band'], p['near_team'], p['x'], p['z']))

    def test_missing_bindings_still_give_the_cell_fields(self):
        out = inspection.build(evidence())
        self.assertEqual(5, len(out['cells']))
        self.assertEqual([], out['points']); self.assertIsNone(out['lair']); self.assertEqual({}, out['fountains'])

    def test_the_schema_and_cell_size_are_stated(self):
        out = inspection.build(evidence(), BINDINGS)
        self.assertEqual('moba_map_inspection/1', out['schema']); self.assertEqual(64, out['cell_size'])
        self.assertIn('absent point is a real answer', out['note'])


if __name__ == '__main__':
    unittest.main()
