from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path

WORLDGEN = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(WORLDGEN))

from terrain_harvest import author_profile as ap

PROFILES = WORLDGEN / 'profiles'


def opportunity(cells=6, surface=70.0):
    return {
        'cell_size_blocks': 128,
        'cells': [{
            'cell': [i, 0], 'world_origin': [-2480 + i * 128, -528],
            'ore': {'iron': 500}, 'vegetation': {}, 'fauna': {}, 'hostiles': {},
            'farmable_surface_samples': 700, 'water_surface_samples': 0,
            'sampled_columns': 1024, 'biomes': {'minecraft:plains': 900},
            'candidate_density': 500, 'regenerative_vocabulary': [],
            'mean_surface_y': surface,
        } for i in range(cells)],
    }


class ProfileFileTests(unittest.TestCase):
    """The four playtest profiles must stay loadable and honestly labelled."""

    NAMES = ('balanced_baseline', 'exploration_centric',
             'consolidative', 'resource_light')

    def test_all_four_exist_and_parse(self):
        for name in self.NAMES:
            body = json.loads((PROFILES / f'{name}.json').read_text())
            self.assertEqual(body['schema'], 'map_profile/1')
            self.assertEqual(body['id'], name)

    def test_every_profile_declares_its_source(self):
        # A profile whose numbers came from a name rather than the optimizer
        # must say so, or a later reader will treat it as a finalist.
        for name in self.NAMES:
            body = json.loads((PROFILES / f'{name}.json').read_text())
            self.assertIn('source', body)
            self.assertNotEqual(body['source'], 'UNDECLARED')

    def test_profiles_only_reference_known_consumers(self):
        for name in self.NAMES:
            body = json.loads((PROFILES / f'{name}.json').read_text())
            ap.consumers_for(body)  # raises on an unknown consumer id

    def test_baseline_changes_nothing(self):
        body = json.loads((PROFILES / 'balanced_baseline.json').read_text())
        self.assertEqual(ap.consumers_for(body),
                         [dict(c) for c in ap.CONSUMERS])

    def test_profiles_actually_differ(self):
        counts = {}
        for name in self.NAMES:
            body = json.loads((PROFILES / f'{name}.json').read_text())
            counts[name] = {c['id']: c['count'] for c in ap.consumers_for(body)}
        self.assertEqual(len(set(map(str, counts.values()))), len(self.NAMES))


class ConsumerOverrideTests(unittest.TestCase):
    def test_override_replaces_only_named_fields(self):
        base = {c['id']: c for c in ap.CONSUMERS}['crop_patch']
        got = {c['id']: c for c in ap.consumers_for(
            {'id': 'p', 'consumers': [{'id': 'crop_patch', 'count': 3}]})}['crop_patch']
        self.assertEqual(got['count'], 3)
        self.assertEqual(got['span'], base['span'])

    def test_unknown_consumer_is_an_error(self):
        with self.assertRaises(ValueError):
            ap.consumers_for({'id': 'p', 'consumers': [{'id': 'nope'}]})


class AuthoringTests(unittest.TestCase):
    def author(self, profile, opp):
        with tempfile.TemporaryDirectory() as tmp:
            o, r = Path(tmp) / 'o.json', Path(tmp) / 'r.json'
            o.write_text(json.dumps(opp))
            return ap.author(profile, o, None, r, dry_run=True)

    def test_plans_without_a_world(self):
        r = self.author({'id': 'p', 'source': 'test', 'consumers': []}, opportunity())
        self.assertGreater(r['placed'], 0)
        self.assertEqual(r['blocks_written'], 0)
        self.assertTrue(r['dry_run'])

    def test_missing_surface_height_is_skipped_not_guessed(self):
        opp = opportunity()
        for c in opp['cells']:
            c['mean_surface_y'] = None
        r = self.author({'id': 'p', 'source': 'test', 'consumers': []}, opp)
        self.assertEqual(r['placed'], 0)
        self.assertTrue(any('surface height' in s['reason'] for s in r['skipped']))

    def test_team_structures_are_left_to_build_structures(self):
        r = self.author({'id': 'p', 'source': 'test', 'consumers': []}, opportunity())
        skipped = {s['id'] for s in r['skipped']}
        for kind in ('aether_fountain', 'end_tower', 'nether_bastion',
                     'pillager_outpost', 'homeland'):
            self.assertIn(kind, skipped)

    def test_undeclared_source_is_reported(self):
        r = self.author({'id': 'p', 'consumers': []}, opportunity())
        self.assertEqual(r['profile_source'], 'UNDECLARED')


class TemplateTests(unittest.TestCase):
    def test_crop_patch_irrigates_only_within_vanilla_range(self):
        t = ap.crop_patch(span=24)
        self.assertEqual(t[(0, -1, 0)], ap.block('water'))
        self.assertEqual(t[(4, -1, 0)], ap.block('farmland', moisture='7'))
        # Beyond hydration range the field is dry dirt, and grows nothing.
        self.assertEqual(t[(5, -1, 0)], ap.block('dirt'))
        self.assertNotIn((5, 0, 0), t)

    def test_pen_has_a_gap_on_each_side(self):
        t = ap.pen(span=40)
        r = 20
        for dx, dz in ((0, -r), (0, r), (-r, 0), (r, 0)):
            self.assertNotIn((dx, 0, dz), t)
        self.assertIn((r, 0, r), t)  # corner still fenced

    def test_worksite_shaft_is_open(self):
        t = ap.worksite(span=32)
        self.assertEqual(t[(0, -3, 0)], ap.block('air'))

    def test_every_template_is_non_empty(self):
        for name, fn in ap.PROFILE_TEMPLATES.items():
            self.assertTrue(fn(), f'{name} produced no blocks')


if __name__ == '__main__':
    unittest.main()
