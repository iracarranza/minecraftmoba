"""Stage 7: composition manifests validate references and record seams only."""
import copy
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from terrain_harvest.composition import (SCHEMA, mask_overlap, placed_bounds,
                                         stale_measurements, unplace_point, validate_composition)
from terrain_harvest.model import Mask
from test_terrain_preservation import volume

def lib(*volumes):
    return {v['id']: v for v in volumes}

def doc(placements, **extra):
    d = {'schema': SCHEMA, 'overlap_policy': 'forbid', 'placements': placements}
    d.update(extra)
    return d

class BoundsTests(unittest.TestCase):
    def test_identity_placement_keeps_source_bounds(self):
        v = volume()
        self.assertEqual(placed_bounds(v, {'rotation': 0, 'translation': [0, 0, 0]}),
                         v['provenance']['source_bounds'])

    def test_quarter_turn_swaps_horizontal_extent(self):
        v = volume()
        b = placed_bounds(v, {'rotation': 90, 'translation': [0, 0, 0]})
        src = v['provenance']['source_bounds']
        self.assertEqual(b['x'][1] - b['x'][0], src['z'][1] - src['z'][0])
        self.assertEqual(b['y'], src['y'])

    def test_translation_shifts_bounds(self):
        v = volume()
        b = placed_bounds(v, {'rotation': 0, 'translation': [100, 0, -50]})
        src = v['provenance']['source_bounds']
        self.assertEqual(b['x'], [src['x'][0] + 100, src['x'][1] + 100])
        self.assertEqual(b['z'], [src['z'][0] - 50, src['z'][1] - 50])

class StalenessTests(unittest.TestCase):
    def measured(self):
        v = copy.deepcopy(volume())
        v['measurements'] = [{'evidence_state': 'DERIVED MEASUREMENT', 'criterion': 'travel cost'},
                             {'evidence_state': 'UNRESOLVED', 'criterion': 'Practical Reach'}]
        return v

    def test_identity_placement_keeps_measurements(self):
        out = stale_measurements(self.measured(), {'rotation': 0, 'translation': [0, 0, 0]})
        self.assertEqual(out[0]['evidence_state'], 'DERIVED MEASUREMENT')
        self.assertNotIn('stale_reason', out[0])

    def test_moving_a_volume_invalidates_its_measurements(self):
        out = stale_measurements(self.measured(), {'rotation': 90, 'translation': [0, 0, 0]})
        self.assertEqual(out[0]['evidence_state'], 'UNRESOLVED')
        self.assertIn('geometry changed', out[0]['stale_reason'])
        # Already-unresolved evidence is not promoted by being moved.
        self.assertEqual(out[1]['evidence_state'], 'UNRESOLVED')

class ValidationTests(unittest.TestCase):
    def test_identity_composition_validates(self):
        v = volume()
        r = validate_composition(doc([{'volume_id': v['id']}]), lib(v))
        self.assertFalse(r['builds_blocks'])
        self.assertEqual(r['placements'][0]['placement_status'], 'IDENTITY_PLACEMENT')
        self.assertTrue(r['id'].startswith('comp_'))

    def test_unknown_reference_is_rejected(self):
        with self.assertRaisesRegex(ValueError, 'unknown volume reference'):
            validate_composition(doc([{'volume_id': 'tv_missing'}]), lib(volume()))

    def test_duplicate_placement_is_rejected(self):
        v = volume()
        with self.assertRaisesRegex(ValueError, 'placed twice'):
            validate_composition(doc([{'volume_id': v['id']}, {'volume_id': v['id']}]), lib(v))

    def test_moved_placement_is_marked_not_buildable(self):
        v = volume()
        r = validate_composition(doc([{'volume_id': v['id'],
            'transform': {'rotation': 90, 'translation': [0, 0, 0]}}]), lib(v))
        self.assertEqual(r['placements'][0]['placement_status'], 'PLANNED_NOT_BUILDABLE')

    def test_claiming_a_moved_placement_is_buildable_is_rejected(self):
        v = volume()
        with self.assertRaisesRegex(ValueError, 'not implemented'):
            validate_composition(doc([{'volume_id': v['id'], 'placement_status': 'BUILT',
                'transform': {'rotation': 90, 'translation': [0, 0, 0]}}]), lib(v))

    def test_non_quarter_rotation_is_rejected(self):
        v = volume()
        with self.assertRaisesRegex(ValueError, 'quarter turn'):
            validate_composition(doc([{'volume_id': v['id'],
                'transform': {'rotation': 45, 'translation': [0, 0, 0]}}]), lib(v))

    def test_overlap_is_forbidden_by_default_policy(self):
        a = volume(); b = volume('ellipse')
        with self.assertRaisesRegex(ValueError, 'overlap forbidden'):
            validate_composition(doc([{'volume_id': a['id']}, {'volume_id': b['id']}]), lib(a, b))

    def test_overlap_can_be_recorded_instead_of_refused(self):
        a = volume(); b = volume('ellipse')
        r = validate_composition(doc([{'volume_id': a['id']}, {'volume_id': b['id']}],
                                     overlap_policy='record_only'), lib(a, b))
        self.assertEqual(len(r['bounding_box_overlaps']), 1)
        self.assertTrue(r['bounding_box_overlaps'][0]['mask_intersection']['intersects'])

    def test_separated_volumes_do_not_overlap(self):
        a = volume(); b = volume('ellipse')
        r = validate_composition(doc([{'volume_id': a['id']},
            {'volume_id': b['id'], 'transform': {'rotation': 0, 'translation': [10000, 0, 0]}}]), lib(a, b))
        self.assertEqual(r['bounding_box_overlaps'], [])

def placed(v, transform=None):
    t = transform or {'rotation': 0, 'translation': [0, 0, 0]}
    return {'volume_id': v['id'], 'transform': t, 'mask': Mask(v), 'boundary': v['boundary'],
            'placed_bounds': placed_bounds(v, t)}

class MaskOverlapTests(unittest.TestCase):
    """Bounding boxes that touch are not the same claim as masks that collide."""
    def test_unplace_is_the_inverse_of_place(self):
        t = {'rotation': 90, 'translation': [10, 0, -5]}
        from terrain_harvest.relocate import place_point
        for p in [(0, 0, 0), (7, -3, 11), (-4, 2, -9)]:
            self.assertEqual(unplace_point(place_point(p, 1, t['translation']), t), p)

    def test_identical_boxes_are_exact(self):
        v = volume()
        r = mask_overlap(placed(v), placed(v))
        self.assertEqual(r['method'], 'exact_box')
        self.assertTrue(r['intersects'])

    def test_touching_boxes_with_disjoint_ellipses_do_not_intersect(self):
        # Two ellipses whose bounding boxes share only a corner column: the
        # inscribed shapes never meet there.
        import copy
        a = volume('ellipse')
        b = copy.deepcopy(volume('ellipse'))
        src = a['provenance']['source_bounds']
        dx = src['x'][1] - src['x'][0]
        dz = src['z'][1] - src['z'][0]
        r = mask_overlap(placed(a), placed(b, {'rotation': 0, 'translation': [dx, 0, dz]}))
        self.assertEqual(r['method'], 'exact_cells')
        self.assertFalse(r['intersects'])

    def test_overlapping_ellipses_report_a_shared_cell(self):
        a = volume('ellipse')
        r = mask_overlap(placed(a), placed(a, {'rotation': 0, 'translation': [1, 0, 0]}))
        self.assertEqual(r['method'], 'exact_cells')
        self.assertTrue(r['intersects'])
        self.assertEqual(len(r['first_shared_cell']), 3)

    def test_oversized_shared_box_is_reported_unresolved(self):
        v = volume('ellipse')
        r = mask_overlap(placed(v), placed(v), budget=10)
        self.assertEqual(r['method'], 'not_computed')
        self.assertIsNone(r['intersects'])
        self.assertIn('unresolved', r['reason'])

    def test_disjoint_masks_are_not_forbidden_by_policy(self):
        import copy
        a = volume('ellipse'); b = copy.deepcopy(volume('ellipse'))
        src = a['provenance']['source_bounds']
        d = doc([{'volume_id': a['id']},
                 {'volume_id': b['id'], 'transform': {'rotation': 0,
                  'translation': [src['x'][1] - src['x'][0], 0, src['z'][1] - src['z'][0]]}}])
        # Same id twice would be a duplicate; give the second a distinct identity.
        b['provenance']['source_seed'] = 78
        from terrain_harvest.model import identity
        b['id'] = identity(b)
        d['placements'][1]['volume_id'] = b['id']
        r = validate_composition(d, lib(a, b))
        self.assertEqual(len(r['bounding_box_overlaps']), 1)
        self.assertFalse(r['bounding_box_overlaps'][0]['mask_intersection']['intersects'])

class SeamTests(unittest.TestCase):
    def two(self):
        a = volume(); b = volume('ellipse')
        return a, b, doc([{'volume_id': a['id']},
            {'volume_id': b['id'], 'transform': {'rotation': 0, 'translation': [10000, 0, 0]}}])

    def test_unresolved_seam_is_recorded(self):
        a, b, d = self.two()
        d['seams'] = [{'volumes': [a['id'], b['id']], 'evidence_state': 'UNRESOLVED'}]
        r = validate_composition(d, lib(a, b))
        self.assertEqual(r['seams'][0]['evidence_state'], 'UNRESOLVED')
        self.assertIn('seam continuity, elevation matching and traversability', r['unresolved'])

    def test_compatibility_claim_needs_supporting_profiles(self):
        a, b, d = self.two()
        d['seams'] = [{'volumes': [a['id'], b['id']], 'evidence_state': 'OBSERVED_COMPATIBLE_PENDING_BUILD'}]
        with self.assertRaisesRegex(ValueError, 'supporting boundary profile'):
            validate_composition(d, lib(a, b))

    def test_seam_to_unplaced_volume_is_rejected(self):
        a, b, d = self.two()
        d['seams'] = [{'volumes': [a['id'], 'tv_elsewhere'], 'evidence_state': 'UNRESOLVED'}]
        with self.assertRaisesRegex(ValueError, 'unplaced volume'):
            validate_composition(d, lib(a, b))

    def test_seam_needs_exactly_two_distinct_volumes(self):
        a, b, d = self.two()
        d['seams'] = [{'volumes': [a['id'], a['id']], 'evidence_state': 'UNRESOLVED'}]
        with self.assertRaisesRegex(ValueError, 'exactly two'):
            validate_composition(d, lib(a, b))

    def test_unknown_seam_state_is_rejected(self):
        a, b, d = self.two()
        d['seams'] = [{'volumes': [a['id'], b['id']], 'evidence_state': 'LOOKS_FINE'}]
        with self.assertRaises(ValueError):
            validate_composition(d, lib(a, b))

if __name__ == '__main__':
    unittest.main()
