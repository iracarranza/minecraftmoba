from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

WORLDGEN = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(WORLDGEN))

from successor.grid import unrle
from vanilla_search import SERVER_SHA1, VERSION
from vanilla_search.evaluate import orient
from vanilla_search.extract import VanillaChunk


class VanillaSearchResultTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.root = WORLDGEN / "results" / "vanilla_default_poc_2026-09-09"
        cls.summary = json.loads((cls.root / "search_summary.json").read_text())
        cls.candidates = [json.loads((cls.root / "shortlist" / str(seed) / "candidate.json").read_text()) for seed in cls.summary["shortlist_seeds"]]

    def test_truth_source_and_proof_of_concept_scope(self):
        self.assertEqual(VERSION, self.summary["minecraft_java_version"])
        self.assertEqual(SERVER_SHA1, self.summary["server_jar_sha1"])
        self.assertEqual(10, self.summary["regions_evaluated"])
        self.assertEqual(8, self.summary["orientations_per_region"])
        self.assertEqual(3, len(self.candidates))
        for candidate in self.candidates:
            self.assertEqual("official Mojang dedicated server", candidate["worldgen_truth"]["source"])
            self.assertFalse(candidate["worldgen_truth"]["terrain_modified"])
            self.assertNotIn("topology_family", candidate)
            self.assertNotIn("hydrological_terrain", candidate)

    def test_feature_adapter_keeps_actual_evidence_distinct(self):
        for candidate in self.candidates:
            metrics = candidate["metrics"]
            self.assertGreater(metrics["actual_surface_water_fraction"], 0)
            self.assertGreater(metrics["actual_river_water_cells"], 0)
            self.assertGreater(metrics["forest_region_count"], 0)
            self.assertTrue(candidate["actual_structures"])
            self.assertEqual(candidate["data_completeness"]["expected_chunks"], candidate["data_completeness"]["full_chunks"])
            self.assertIn("deferred", candidate["data_completeness"]["ores"])

    def test_compact_feature_grids_have_expected_size(self):
        for candidate in self.candidates:
            grid = candidate["feature_grid"]
            size = grid["width"] * grid["height"]
            for key in ("height_rle", "biome_rle", "surface_block_rle", "actual_surface_water_rle", "forest_mask_rle", "buildable_mask_rle", "highland_mask_rle"):
                self.assertEqual(size, len(unrle(grid[key])))

    def test_routes_are_analysis_only(self):
        for candidate in self.candidates:
            routes = candidate["routes"]
            self.assertIn("analytical compatibility only", routes["status"])
            self.assertEqual(6, len(routes["branches"]))
            self.assertTrue(routes["all_six_connected"])

    def test_screen_boundary_is_reported_not_hidden(self):
        passes = [c for c in self.candidates if not c["experimental_screen"]["warnings"]]
        near = [c for c in self.candidates if c["experimental_screen"]["warnings"]]
        self.assertEqual(2, len(passes))
        self.assertEqual([920270003], [c["seed"] for c in near])
        self.assertEqual(["ocean_gradient_points_east"], near[0]["experimental_screen"]["warnings"])

    def test_orientation_preserves_raw_cells(self):
        rows = [[{"id": 0}, {"id": 1}, {"id": 2}], [{"id": 3}, {"id": 4}, {"id": 5}]]
        rotated = orient(rows, 90, False)
        self.assertEqual([[3, 0], [4, 1], [5, 2]], [[cell["id"] for cell in row] for row in rotated])
        reflected = orient(rows, 0, True)
        self.assertEqual([[2, 1, 0], [5, 4, 3]], [[cell["id"] for cell in row] for row in reflected])

    def test_omitted_block_states_are_vanilla_air(self):
        chunk = VanillaChunk({"sections": [{"Y": 19, "biomes": {"palette": ["minecraft:plains"]}}]})
        self.assertEqual("minecraft:air", chunk.block(0, 319, 0))


if __name__ == "__main__":
    unittest.main()
