from __future__ import annotations

import json
import copy
import sys
import tempfile
import unittest
from pathlib import Path

WORLDGEN = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(WORLDGEN))

from analysis.default_map_analysis import L1, L3_HUNGER, unsupported_round_trip_radius
from analysis.resource_contract import audit
from successor.grid import unrle
from successor.pipeline import generate_candidate, serializable, write_results
from successor.validation import validate


class SuccessorCandidateTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.candidate = generate_candidate(920261000)

    def test_hard_contract_and_resource_vocabulary(self):
        self.assertTrue(self.candidate["validation"]["hard_contract"]["pass"])
        self.assertTrue(audit(self.candidate)["pass"])

    def test_routes_connect_and_are_not_final_visuals(self):
        routes = self.candidate["routes"]
        self.assertEqual(3, sum(x["team"] == "north" for x in routes["branches"]))
        self.assertEqual(3, sum(x["team"] == "south" for x in routes["branches"]))
        self.assertFalse(routes["visual_language_finalized"])
        self.assertEqual(0.0, routes["speed_bonus"])
        self.assertEqual(.10, routes["locomotion_exhaustion_reduction"])
        self.assertTrue(all(self.candidate["validation"]["descriptive_metrics"]["route_connectivity_detail"].values()))

    def test_hydrology_is_connected_and_not_stamped(self):
        features = self.candidate["terrain"]["hydrology"]
        self.assertTrue(features)
        self.assertTrue(all(x["connected"] and not x["stamped"] for x in features))

    def test_actual_resources_precede_derived_regions(self):
        ids = {x["id"] for x in self.candidate["ecology"]["instances"]}
        self.assertTrue(ids)
        self.assertTrue(all(set(r["instance_ids"]) <= ids for r in self.candidate["ecology"]["opportunity_regions"]))

    def test_round_trip_and_historical_hunger_regression(self):
        access = next(iter(self.candidate["traversal"]["teams"]["north"]["access"].values()))
        self.assertIn("unsupported_exhaustion", access["round_trip"])
        self.assertAlmostEqual(68.4, unsupported_round_trip_radius(L1, 2, 1), places=1)
        self.assertAlmostEqual(102.6, unsupported_round_trip_radius(L3_HUNGER, 2, 1), places=1)

    def test_machine_metadata_round_trip_dimensions(self):
        encoded = serializable(self.candidate)
        grid = encoded["terrain"]["grid_encoding"]
        self.assertEqual(grid["nx"] * grid["nz"], len(unrle(grid["height_rle"])))
        self.assertNotIn("mask", encoded["routes"])

    def test_validation_categories_are_explicit(self):
        validation = self.candidate["validation"]
        self.assertEqual("hard", validation["hard_contract"]["status"])
        self.assertEqual("prototype/test", validation["experimental_thresholds"]["status"])
        self.assertEqual("descriptive; not gates", validation["descriptive_metrics"]["status"])

    def test_validator_catches_required_negative_regressions(self):
        def changed(mutator):
            candidate = copy.deepcopy(self.candidate)
            mutator(candidate)
            return validate(candidate)

        removals = {
            "livestock_species": lambda c: (c["ecology"].__setitem__("instances", [x for x in c["ecology"]["instances"] if x["type"] != "chicken"]), [r.__setitem__("species", [s for s in r["species"] if s != "chicken"]) for r in c["ecology"]["livestock_ranges"]]),
            "crop_starters": lambda c: c["ecology"].__setitem__("instances", [x for x in c["ecology"]["instances"] if x["type"] != "potato"]),
            "horses": lambda c: c["ecology"].__setitem__("instances", [x for x in c["ecology"]["instances"] if x["type"] != "horse"]),
            "sand_gravel_snow": lambda c: c["ecology"].__setitem__("instances", [x for x in c["ecology"]["instances"] if x["type"] != "snow"]),
            "ore_vocabulary": lambda c: c["ecology"].__setitem__("instances", [x for x in c["ecology"]["instances"] if x["type"] != "diamond"]),
            "homeland_baseline_viability": lambda c: c["ecology"].__setitem__("instances", [x for x in c["ecology"]["instances"] if not (x.get("team") == "north" and x["type"] == "wood")]),
        }
        for check, mutator in removals.items():
            with self.subTest(check=check):
                self.assertFalse(changed(mutator)["hard_contract"]["checks"][check])

        def collapse_mountain(c):
            c["terrain"]["mountain_mask"] = [(i % 169) < 2 for i in range(len(c["terrain"]["mountain_mask"]))]
        self.assertFalse(changed(collapse_mountain)["experimental_thresholds"]["checks"]["mountain_depth"])

        self.assertFalse(changed(lambda c: c["terrain"]["hydrology"][0].__setitem__("connected", False))["experimental_thresholds"]["checks"]["connected_downhill_hydrology"])
        self.assertFalse(changed(lambda c: c["packing"]["space_overlays"]["empty_connective_territory"].__setitem__("fraction_of_land", 0))["experimental_thresholds"]["checks"]["wilderness_empty_separation"])
        self.assertFalse(changed(lambda c: c["packing"].__setitem__("packed_hotspot_max", 99))["experimental_thresholds"]["checks"]["resource_packing"])
        self.assertFalse(changed(lambda c: c["routes"]["branches"][0]["centerline"].insert(1, [400, 400]))["hard_contract"]["checks"]["route_connectivity"])
        self.assertFalse(changed(lambda c: c["fairness"].__setitem__("maximum_absolute_bias", .99))["experimental_thresholds"]["checks"]["no_severe_opportunity_bias"])
        self.assertFalse(changed(lambda c: c["fairness"].__setitem__("artificial_correction_count", 99))["experimental_thresholds"]["checks"]["modest_correction"])


if __name__ == "__main__":
    unittest.main()
