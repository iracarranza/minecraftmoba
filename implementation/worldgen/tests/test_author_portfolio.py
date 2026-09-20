from __future__ import annotations

import sys
import unittest
from pathlib import Path

WORLDGEN = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(WORLDGEN))

from terrain_harvest import massing
from terrain_harvest.author_portfolio import (CROP_BLOCK, SPECIES_FENCE, allocate,
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
    def test_every_optimizer_crop_maps_to_a_block(self):
        for crop in CROP_BLOCK:
            t, reason = template_for({'kind': 'founder_crop', 'detail': crop})
            self.assertIsNotNone(t, reason)

    def test_every_mapped_species_authors_a_range(self):
        for species in SPECIES_FENCE:
            t, reason = template_for({'kind': 'renewable_range', 'detail': species})
            self.assertIsNotNone(t, reason)

    def test_unmapped_species_is_a_reported_gap_not_a_default(self):
        t, reason = template_for({'kind': 'renewable_range', 'detail': 'axolotl'})
        self.assertIsNone(t)
        self.assertIn('axolotl', reason)

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


if __name__ == '__main__':
    unittest.main()
