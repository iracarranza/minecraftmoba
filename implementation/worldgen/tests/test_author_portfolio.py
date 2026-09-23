from __future__ import annotations

import sys
import unittest
from pathlib import Path

WORLDGEN = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(WORLDGEN))

from terrain_harvest import massing
from terrain_harvest.author_portfolio import (UNAUTHORED, allocate,
                                              cell_index, footprint_chunks,
                                              radius_of, sites_of, template_for,
                                              unwritable)


def site(kind, detail=None, origin=(0, 0), cell=(0, 0)):
    template, reason = template_for({'kind': kind, 'detail': detail})
    assert template is not None, reason
    return {'kind': kind, 'detail': detail, 'cell': list(cell),
            'origin': list(origin), 'centre': list(origin), 'template': template,
            'surface_y': 70.0, 'coverage': 1.0, 'bias': None}


class TemplateSelectionTests(unittest.TestCase):
    def test_regenerative_opportunities_author_no_blocks_at_all(self):
        # These tests used to assert that every crop mapped to a block and
        # every species to a FENCE. The regenerative manifestation doctrine
        # rejects fences, rectangular layouts and prepared farmland: a Patch is
        # a resource occurrence, not infrastructure implying the site is
        # already Developed. So nothing is authored, and the mapping tables
        # they asserted over are gone rather than left as a trap.
        for kind in UNAUTHORED:
            for detail in ('wheat', 'sheep', 'axolotl', None):
                t, reason = template_for({'kind': kind, 'detail': detail})
                self.assertEqual({}, t, f'{kind}/{detail} must author no blocks')
                self.assertIsNone(reason)

    def test_the_placement_still_exists_even_though_nothing_is_built(self):
        # Empty is deliberate, not a failure: the cell the optimizer chose is
        # still the opportunity, and the runtime manifests it at match time on
        # terrain that is eligible then.
        t, reason = template_for({'kind': 'renewable_range', 'detail': 'cow'})
        self.assertIsNotNone(t)
        self.assertIsNone(reason)

    def test_unknown_kind_is_reported(self):
        t, reason = template_for({'kind': 'sawmill', 'detail': None})
        self.assertIsNone(t)
        self.assertIn('sawmill', reason)


class AllocationTests(unittest.TestCase):
    """The collision bug a readback caught: several sites can share a cell."""

    def test_single_site_stays_centred(self):
        placed, rejected = allocate([site('poi', origin=(0, 0))], 128, None)
        self.assertEqual(rejected, [])
        self.assertEqual(placed[0]['world_xz'], [0, 0])

    def test_sites_in_one_cell_do_not_overlap(self):
        sites = [site('poi'), site('renewable_range', 'sheep'),
                 site('mining_worksite')]
        placed, rejected = allocate(sites, 128, None)
        self.assertEqual(rejected, [], 'three sites should fit a 128 cell')
        for i, a in enumerate(placed):
            for b in placed[i + 1:]:
                gap = max(abs(a['world_xz'][0] - b['world_xz'][0]),
                          abs(a['world_xz'][1] - b['world_xz'][1]))
                self.assertGreaterEqual(gap, a['radius'] + b['radius'],
                                        f"{a['kind']} overlaps {b['kind']}")

    def test_no_two_sites_land_on_the_same_point(self):
        sites = [site('founder_crop', 'wheat') for _ in range(4)]
        placed, _ = allocate(sites, 128, None)
        spots = [tuple(p['world_xz']) for p in placed]
        self.assertEqual(len(spots), len(set(spots)))

    def test_oversized_site_is_rejected_not_stacked(self):
        # Nine sites means 3x3 slots of 42 blocks; a POI needs 25 of half-21.
        sites = [site('poi') for _ in range(9)]
        placed, rejected = allocate(sites, 128, None)
        self.assertTrue(rejected)
        self.assertEqual(len(placed) + len(rejected), 9)

    def test_absent_chunks_block_a_slot(self):
        s = site('mining_worksite', origin=(0, 0))
        everywhere = footprint_chunks(0, 0, s['template'])
        placed, rejected = allocate([s], 128, chunks=everywhere)
        self.assertEqual(len(placed), 1)
        placed, rejected = allocate([s], 128, chunks=set())
        self.assertEqual(len(rejected), 1, 'no chunk anywhere means no slot')


class PreflightTests(unittest.TestCase):
    def test_unwritable_lists_only_missing_chunks(self):
        t = massing.worksite(span=32)
        need = footprint_chunks(0, 0, t)
        self.assertEqual(unwritable(0, 0, t, need), [])
        self.assertEqual(set(unwritable(0, 0, t, set())), need)

    def test_footprint_covers_the_whole_template(self):
        t = massing.poi(span=48)
        r = radius_of(t)
        chunks = footprint_chunks(0, 0, t)
        for dx in (-r, r):
            for dz in (-r, r):
                self.assertIn((dx >> 4, dz >> 4), chunks)


class CellIndexTests(unittest.TestCase):
    def test_partial_cell_exposes_coverage_and_centroid(self):
        opp = {'cell_size_blocks': 128, 'cells': [
            {'cell': [0, 8], 'mean_surface_y': 70.0, 'coverage': 0.25,
             'sampled_centroid': [-2418, 510]}]}
        info = cell_index(opp)[(0, 8)]
        self.assertEqual(info['coverage'], 0.25)
        self.assertEqual(info['centroid'], [-2418, 510])


class SiteFlatteningTests(unittest.TestCase):
    def test_every_configuration_section_is_authored(self):
        cfg = {
            'founders': [{'cell': [0, 0], 'center': [0, 0], 'crop': 'wheat'}],
            'renewables': [{'cell': [0, 0], 'center': [0, 0], 'species': 'sheep'}],
            'worksites': [{'cell': [1, 0], 'center': [128, 0]}],
            'pois': [{'cell': [2, 0], 'center': [256, 0]}],
            'route_targets': {'north': [{'cell': [3, 0], 'center': [384, 0]}]},
        }
        kinds = [s['kind'] for s in sites_of(cfg)]
        self.assertEqual(sorted(kinds), ['founder_crop', 'mining_worksite',
                                         'poi', 'renewable_range', 'route_target'])

    def test_route_target_carries_its_team(self):
        cfg = {'route_targets': {'south': [{'cell': [0, 0], 'center': [0, 0]}]}}
        self.assertEqual(next(sites_of(cfg))['detail'], 'south')


class RouteTests(unittest.TestCase):
    """The contract says author physical path quality, not a marker."""

    def test_densify_walks_one_block_at_a_time(self):
        from terrain_harvest.routes import densify
        line = densify([(0, 0), (8, 0), (8, 8)])
        self.assertEqual(line[0], (0, 0))
        self.assertEqual(line[-1], (8, 8))
        for a, b in zip(line, line[1:]):
            self.assertLessEqual(max(abs(a[0] - b[0]), abs(a[1] - b[1])), 1)

    def test_densify_has_no_duplicate_columns(self):
        from terrain_harvest.routes import densify
        line = densify([(0, 0), (4, 0), (4, 0), (4, 4)])
        self.assertEqual(len(line), len(set(line)))

    def test_crossings_report_sites_the_corridor_runs_through(self):
        from terrain_harvest.routes import crossings_of
        sites = [{'kind': 'poi', 'detail': None, 'cell': [0, 0],
                  'world_xyz': [10, 70, 0], 'radius': 25},
                 {'kind': 'poi', 'detail': None, 'cell': [9, 9],
                  'world_xyz': [9000, 70, 9000], 'radius': 25}]
        hit = crossings_of([(0, 0), (1, 0)], sites)
        self.assertEqual(len(hit), 1)
        self.assertEqual(hit[0]['world_xz'], [10, 0])

    def test_route_authoring_grants_no_speed(self):
        # The contract forbids an intrinsic movement-speed bonus, so the module
        # must not reference one at all.
        src = (WORLDGEN / 'terrain_harvest' / 'routes.py').read_text()
        for token in ('movement_speed', 'MOVEMENT_SPEED', 'speed_bonus',
                      'GENERIC_MOVEMENT'):
            self.assertNotIn(token, src)


class ContractTests(unittest.TestCase):
    """Clauses from claude-authoring-handoff.json that code can enforce."""

    def test_no_module_consumes_hostile_counts(self):
        # hostiles == 0 -> underground is safe is a forbidden inference, so the
        # authoring path must not read the field at all.
        for name in ('terrain_harvest/author_portfolio.py',
                     'terrain_harvest/routes.py',
                     'terrain_harvest/massing.py'):
            self.assertNotIn("'hostiles'", (WORLDGEN / name).read_text(), name)

    def test_worksite_massing_is_generic(self):
        src = (WORLDGEN / 'terrain_harvest' / 'massing.py').read_text()
        for token in ('mining_site', 'MiningSite', 'industrial_factory'):
            self.assertNotIn(token, src)


class RescanTests(unittest.TestCase):
    """The rescan must be comparable to the proxy, not merely plausible."""

    def test_cost_model_is_the_unchanged_shared_parameters(self):
        from terrain_harvest import rescan
        from vanilla_search.task_a import PARAMETERS
        # Comparing measured reach against the proxy is only meaningful if both
        # use the weights that sited the structures. A local copy would drift.
        self.assertIs(rescan.PARAMETERS, PARAMETERS)

    def test_reach_is_reported_in_the_optimizer_s_units(self):
        from terrain_harvest import rescan
        self.assertAlmostEqual(rescan.SPRINT, 5.612)

    def test_ndiff_matches_the_optimizer_definition(self):
        from terrain_harvest.rescan import ndiff
        self.assertEqual(ndiff(0, 0), 0.0)
        self.assertAlmostEqual(ndiff(100, 200), 2 / 3)
        self.assertAlmostEqual(ndiff(50, 50), 0.0)

    def test_rebalance_is_unresolved_when_sites_are_missing(self):
        from terrain_harvest.rescan import rebalance
        cfg = {'founders': [{'cell': [0, 0], 'bias': 'north'}], 'renewables': [],
               'worksites': [], 'pois': [], 'route_targets': {'north': [], 'south': []}}
        self.assertIn('unresolved', rebalance(cfg, []))

    def test_rotated_grid_is_refused_not_silently_compared(self):
        from terrain_harvest.rescan import measure
        candidate = {'orientation': {'rotation_degrees_clockwise': 90,
                                     'east_west_reflected': False}}
        with self.assertRaises(ValueError):
            measure(Path('/nonexistent'), candidate, {'placements': []}, None)

    def test_water_and_lava_are_not_buildable(self):
        from terrain_harvest import rescan
        self.assertIn('minecraft:water', rescan.WATER)
        self.assertIn('minecraft:lava', rescan.NONBUILDABLE)

    def test_dirt_path_gets_no_intrinsic_bonus(self):
        # The contract forbids a movement-speed bonus. A Route may only measure
        # faster because decking and clearing removed real terrain penalties.
        src = (WORLDGEN / 'terrain_harvest' / 'rescan.py').read_text()
        self.assertNotIn('dirt_path', src.split('"""', 2)[2])


class PublishTests(unittest.TestCase):
    def test_every_class_has_a_colour(self):
        from terrain_harvest.publish_world import CLASSES, MATERIAL, classify
        for label, _ in MATERIAL:
            self.assertIn(label, CLASSES)
        for label in ('water', 'wood', 'other'):
            self.assertIn(label, CLASSES)

    def test_classify_is_total(self):
        from terrain_harvest.publish_world import CLASSES, classify
        for name in ('minecraft:water', 'minecraft:dirt_path', 'minecraft:wheat',
                     'minecraft:oak_log', 'minecraft:something_new'):
            self.assertIn(classify(name), CLASSES)

    def test_authored_blocks_classify_distinctly(self):
        from terrain_harvest.publish_world import classify
        # A Route must not read as ordinary ground, or the render hides it.
        self.assertEqual(classify('minecraft:dirt_path'), 'route')
        self.assertEqual(classify('minecraft:oak_planks'), 'route_deck')
        self.assertNotEqual(classify('minecraft:dirt_path'),
                            classify('minecraft:grass_block'))

    def test_every_site_kind_has_a_marker_colour(self):
        from terrain_harvest.author_portfolio import template_for
        from terrain_harvest.publish_world import SITE_COLOUR
        for kind in ('founder_crop', 'renewable_range', 'mining_worksite',
                     'poi', 'route_target', 'homeland'):
            self.assertIn(kind, SITE_COLOUR)


class PublishRoundTripTests(unittest.TestCase):
    def test_rle_round_trip_is_exact(self):
        from terrain_harvest.publish_world import CLASSES, decode_rows, encode_rows
        labels = sorted(CLASSES)
        grid = [[(64, 'grass'), (64, 'grass'), None],
                [(63, 'water'), (65, 'route'), (65, 'route')]]
        self.assertEqual(decode_rows(encode_rows(grid, labels), labels), grid)

    def test_publisher_preserves_full_crossing_metadata(self):
        src = (WORLDGEN / 'terrain_harvest' / 'publish_world.py').read_text()
        self.assertIn("'crosses': r.get('crosses', [])", src)
        self.assertNotIn("[c['kind'] for c in r.get('crosses', [])]", src)


class OptimizerSpilloverTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        import importlib.util
        path = WORLDGEN.parents[1] / 'tools' / 'analysis' / 'map_authoring_optimizer.py'
        spec = importlib.util.spec_from_file_location('map_authoring_optimizer', path)
        cls.mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(cls.mod)

    def test_rng_is_cross_language_deterministic(self):
        r = self.mod.DeterministicRNG(1)
        self.assertEqual([r.randbelow(1000) for _ in range(3)], [236, 369, 504])

    def test_near_route_can_change_reach_but_far_site_does_not(self):
        target = {'center': [100, 0], 'n': 60.0, 's': 100.0,
                  'direct_n': 10.0, 'direct_s': 10.0}
        cfg = {'route_targets': {'north': [target], 'south': []}}
        near = {'center': [50, 0], 'n': 55.0, 's': 100.0,
                'direct_n': 5.0, 'direct_s': 10.0}
        far = {'center': [50, 500], 'n': 55.0, 's': 100.0,
               'direct_n': 50.0, 'direct_s': 50.0}
        self.assertLess(self.mod.spillover_reach(near, 'north', cfg), near['n'])
        self.assertEqual(self.mod.spillover_reach(far, 'north', cfg), far['n'])

    def test_balance_is_still_measured_but_no_longer_decides_anything(self):
        # `cell` is required: metrics() counts distinct cells, and the fixture
        # predated that. A real export always carries both.
        north = {'center': [50, 0], 'cell': [0, 0], 'n': 60.0, 's': 180.0, 'gap': 120.0,
                 'min': 60.0, 'bias': 'north', 'direct_n': 10.0, 'direct_s': 30.0}
        south = {'center': [50, 100], 'cell': [0, 1], 'n': 180.0, 's': 60.0, 'gap': 120.0,
                 'min': 60.0, 'bias': 'south', 'direct_n': 30.0, 'direct_s': 10.0}
        north_target = {**north, 'center': [100, 0]}
        south_target = {**south, 'center': [100, 100]}
        cfg = {
            'founders': [north, south],
            'renewables': [north, south],
            'worksites': [north, south],
            'pois': [north, south],
            'route_targets': {'north': [north_target], 'south': [south_target]},
        }
        got = self.mod.metrics(cfg)
        # The measurement survives unchanged.
        self.assertEqual(
            got['effective_balance_asymmetry'],
            max(got['balance_asymmetry'],
                got['route_spillover_balance_asymmetry']))
        # What changed is that nothing consumes it. The objective used to be
        # char - 5.5*b - 25*max(0, b-0.14), which made balance the only term
        # that mattered; two portfolios differing only in balance must now
        # score identically.
        low = dict(got, balance_asymmetry=0.01, effective_balance_asymmetry=0.01)
        high = dict(got, balance_asymmetry=0.40, effective_balance_asymmetry=0.40)
        self.assertEqual(self.mod.objective('balanced_baseline', low),
                         self.mod.objective('balanced_baseline', high))

    def test_the_gate_is_unreachability_not_inequality(self):
        # An opportunity strongly biased toward one team is fine. One neither
        # team can attend to inside the night that opens it is not.
        lopsided = {'center': [0, 0], 'cell': [0, 0], 'n': 40.0, 's': 400.0,
                    'gap': 360.0, 'min': 40.0, 'bias': 'north',
                    'direct_n': 10.0, 'direct_s': 90.0}
        remote = dict(lopsided, cell=[1, 1], n=900.0, s=950.0, min=900.0)
        # Both teams must still be able to reach a Worksite: a team with none
        # in opening reach cannot take part in the Worksite night at all, which
        # is functional opportunity failing rather than terrain differing.
        near_ws = dict(lopsided, cell=[2, 2], n=80.0, s=120.0, min=80.0)
        cfg = {'founders': [lopsided], 'renewables': [lopsided],
               'worksites': [near_ws], 'pois': [lopsided],
               'route_targets': {'north': [lopsided], 'south': [lopsided]}}
        self.assertEqual([], self.mod.viability(cfg, self.mod.metrics(cfg)),
                         'a lopsided but reachable portfolio is viable')
        cfg['pois'] = [remote]
        codes = [r['code'] for r in self.mod.viability(cfg, self.mod.metrics(cfg))]
        self.assertIn('OPPORTUNITY_UNREACHABLE', codes)

    def test_a_team_with_no_worksite_in_opening_reach_is_rejected(self):
        ok = {'center': [0, 0], 'cell': [0, 0], 'n': 80.0, 's': 120.0, 'gap': 40.0,
              'min': 80.0, 'bias': 'north', 'direct_n': 10.0, 'direct_s': 20.0}
        stranded = dict(ok, cell=[3, 3], s=500.0)
        cfg = {'founders': [ok], 'renewables': [ok], 'worksites': [stranded],
               'pois': [ok], 'route_targets': {'north': [ok], 'south': [ok]}}
        reasons = self.mod.viability(cfg, self.mod.metrics(cfg))
        self.assertEqual(['NO_OPENING_WORKSITE'], [r['code'] for r in reasons])
        self.assertEqual('south', reasons[0]['team'])

    def test_rejections_are_machine_readable(self):
        far = {'center': [0, 0], 'cell': [0, 0], 'n': 900.0, 's': 950.0,
               'gap': 50.0, 'min': 900.0, 'bias': 'north',
               'direct_n': 90.0, 'direct_s': 95.0}
        cfg = {'founders': [far], 'renewables': [far], 'worksites': [far],
               'pois': [far], 'route_targets': {'north': [far], 'south': [far]}}
        reasons = self.mod.viability(cfg, self.mod.metrics(cfg))
        self.assertTrue(reasons)
        for r in reasons:
            self.assertIn('code', r)
            self.assertEqual(r['code'], r['code'].upper())


if __name__ == '__main__':
    unittest.main()
