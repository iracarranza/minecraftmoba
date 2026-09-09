from __future__ import annotations

import math
import sys
import unittest
from pathlib import Path

WORLDGEN = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(WORLDGEN))

from greybox.pipeline import generate_candidate, serializable
from greybox.terrain import generate_terrain
from successor.config import NX, NZ
from successor.grid import unrle


class GreyboxGeneratorTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.horseshoe = generate_candidate(920261010)
        cls.offset_basin = generate_candidate(920261022)

    def test_regression_seeds_are_deterministic_but_not_frozen_to_v1(self):
        repeat = generate_terrain(920261010)
        self.assertEqual(self.horseshoe["terrain"]["heights"], repeat["heights"])
        self.assertEqual(self.horseshoe["terrain"]["terrain"], repeat["terrain"])
        self.assertEqual("horseshoe", self.horseshoe["terrain"]["topology_family"])
        self.assertEqual("offset_basin", self.offset_basin["terrain"]["topology_family"])

    def test_hydrology_is_terrain_analysis_not_physical_water(self):
        for candidate in (self.horseshoe, self.offset_basin):
            terrain = candidate["terrain"]
            self.assertNotIn("water", terrain["terrain"])
            self.assertNotIn("lava", terrain["terrain"])
            self.assertIn("no water or lava", terrain["representation_note"])
            hydrology = terrain["hydrological_terrain"]
            self.assertGreater(hydrology["potential_channel_cells"], 0)
            self.assertGreater(hydrology["tributary_junctions"], 0)
            self.assertGreater(hydrology["coastal_outlets"], 0)
            self.assertTrue(hydrology["basins"])
            self.assertTrue(all(feature["connected"] and not feature["stamped"] for feature in terrain["hydrology"]))
            self.assertTrue(all(feature["maximum_spill_rise_y"] >= 0 for feature in terrain["hydrology"]))

    def test_coast_forest_and_multiscale_screens_cover_serialized_defects(self):
        expected = {
            "terrain_bands_reduced",
            "macro_grade_contains_medium_relief",
            "forest_edges_fragmented",
            "drainage_is_terrain_not_water_stamp",
            "drainage_has_relationships",
            "principal_drainage_not_one_straight_trace",
            "coast_not_uniform_ribbon",
            "routes_not_cardinal_polylines",
        }
        for candidate in (self.horseshoe, self.offset_basin):
            diagnostics = candidate["geography_diagnostics"]
            self.assertEqual(expected, set(diagnostics["experimental_quality_checks"]["checks"]))
            self.assertGreaterEqual(diagnostics["coast"]["shore_type_count"], 3)
            self.assertGreater(diagnostics["coast"]["transition_width_cv"], 0)
            self.assertGreater(diagnostics["forest"]["largest_interior_depth_blocks"], 0)
            self.assertGreater(diagnostics["forest"]["embedded_clearing_cells"], 0)

    def test_routes_are_six_connected_analytical_corridors(self):
        for candidate in (self.horseshoe, self.offset_basin):
            routes = candidate["routes"]
            self.assertEqual(6, len(routes["branches"]))
            self.assertEqual(3, sum(branch["team"] == "north" for branch in routes["branches"]))
            self.assertEqual(3, sum(branch["team"] == "south" for branch in routes["branches"]))
            self.assertFalse(routes["physical_surface"])
            self.assertFalse(routes["visual_language_finalized"])
            self.assertEqual(0.0, routes["speed_bonus"])
            for branch in routes["branches"]:
                self.assertEqual(branch["home"], branch["centerline"][0])
                self.assertEqual(branch["target"], branch["centerline"][-1])
                self.assertTrue(all(math.hypot(b[0] - a[0], b[1] - a[1]) <= 4.5 for a, b in zip(branch["centerline"], branch["centerline"][1:])))

    def test_opportunity_markers_remain_analytical_and_accessible(self):
        for candidate in (self.horseshoe, self.offset_basin):
            markers = candidate["geography_diagnostics"]["opportunity_locations"]
            self.assertTrue(markers)
            self.assertTrue(all(marker["status"] == "analytical location/footprint only" for marker in markers))
            self.assertTrue(all(math.isfinite(marker["route_distance_blocks"]) for marker in markers))
            self.assertTrue(candidate["validation"]["hard_contract"]["checks"]["all_markers_on_traversable_land"])

    def test_compact_metadata_preserves_all_greybox_grids(self):
        encoded = serializable(self.horseshoe)
        grid = encoded["terrain"]["grid_encoding"]
        for key in ("height_rle", "terrain_rle", "mountain_mask_rle", "coast_zone_rle", "potential_channel_mask_rle"):
            self.assertEqual(NX * NZ, len(unrle(grid[key])))
        self.assertNotIn("channel_cells", encoded["terrain"]["hydrological_terrain"])
        self.assertNotIn("mask", encoded["routes"])

    def test_hard_contracts_and_experimental_screens_stay_distinct(self):
        for candidate in (self.horseshoe, self.offset_basin):
            self.assertTrue(candidate["validation"]["hard_contract"]["pass"])
            self.assertEqual("prototype/test; shortlist screen, not design contracts", candidate["geography_diagnostics"]["experimental_quality_checks"]["status"])


if __name__ == "__main__":
    unittest.main()
