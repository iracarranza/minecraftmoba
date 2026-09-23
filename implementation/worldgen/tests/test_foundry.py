"""The READY map pool: only verified maps go in, and provenance goes with them."""
import json
import shutil
import tempfile
import unittest
from pathlib import Path

from terrain_harvest import foundry


def world(root: Path):
    (root / 'region').mkdir(parents=True)
    (root / 'region' / 'r.0.0.mca').write_bytes(b'not really a region, but stable')
    return root


def compilation(playable=True, stage='ready', ready=True, bindings=True):
    return {
        'playable': playable,
        'verified': playable,
        'ready': ready,
        'deepest_stage_reached': stage,
        'evidence': {
            'runtime_bindings': {
                'world': {'name': 'gen', 'map_type': 'default'},
                'homelands': {'north': [0, 0, 0, 0], 'south': [1, 1, 1, 1]},
                'fountains': {'north': [0, 64, 0], 'south': [1, 64, 1]},
                'objectives': {t: {k: [0, 0] for k in
                                   ('pillager_outpost', 'nether_bastion', 'end_spike')}
                               for t in ('north', 'south')},
                'lair': {'anchor': {'xyz': [5, 65, 5]}, 'count': 1},
                'worksites': [{'id': f'ws_{i}'} for i in range(12)],
            } if bindings else None,
            'readiness': {'certified': ready,
                          'problems': [] if ready else [{'code': 'LAIR_UNCONFIGURED'}]},
            'homelands': {'north': 0.74, 'south': 0.85},
            'hinterland': {'north': 0.02, 'south': 0.03},
            'objective_world_xz': {'north': {'end_spike': [1, 2]}},
            'lair_site': {'access_asymmetry': 0.01},
            'physical_verification': {'verified': True},
            'readback': {'verified': True},
            'authored': {'blocks_written': 90000},
        },
    }


class Publishing(unittest.TestCase):

    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self._tmp.cleanup)
        self.tmp = Path(self._tmp.name)
        self.src = world(self.tmp / 'src')
        self.pool = self.tmp / 'pool'

    def test_only_a_verified_map_is_published(self):
        # The pool's whole value is that claiming is unconditional. A map that
        # might be fine does not belong in it.
        with self.assertRaises(ValueError):
            foundry.publish(self.pool, 1, self.src,
                            compilation(playable=False, stage='lair', ready=False))
        self.assertFalse(self.pool.exists())

    def test_a_verified_but_unready_map_is_refused(self):
        # The distinction this pass exists for. A map can satisfy every physical
        # check and still be unusable -- an unmanifested Lair would have reached
        # UNCONFIGURED on night 2 of a map the pool called READY.
        with self.assertRaises(ValueError) as caught:
            foundry.publish(self.pool, 1, self.src,
                            compilation(stage='verify', ready=False))
        self.assertIn('NOT READY', str(caught.exception))
        self.assertIn('LAIR_UNCONFIGURED', str(caught.exception))

    def test_a_ready_map_without_bindings_is_refused(self):
        with self.assertRaises(ValueError):
            foundry.publish(self.pool, 1, self.src, compilation(bindings=False))

    def test_the_entry_carries_everything_a_match_needs_to_bind(self):
        m = foundry.publish(self.pool, 1, self.src, compilation())
        b = m['runtime_bindings']
        self.assertEqual({'world', 'homelands', 'fountains', 'objectives',
                          'lair', 'worksites'}, set(b))
        self.assertIsNotNone(b['lair']['anchor'])

    def test_characteristics_are_broad_classification_only(self):
        # Players know the classification and discover the realization, so the
        # seed, the geography and the Lair location stay out of what a draft
        # could show.
        c = foundry.publish(self.pool, 1, self.src, compilation())['characteristics']
        self.assertEqual({'map_type', 'map_scale', 'resource_density', 'note'}, set(c))
        self.assertEqual('unmeasured', c['resource_density'])

    def test_a_published_entry_is_ready_and_carries_provenance(self):
        m = foundry.publish(self.pool, 99887766, self.src, compilation())
        self.assertEqual(foundry.READY, m['state'])
        p = m['provenance']
        self.assertEqual(99887766, p['seed'])
        self.assertIn('max_lair_access_asymmetry', p['compiler_constants'])
        self.assertIn('end_spike', p['objective_forms'])
        # What was NOT reproduced travels with the map, so a claimed map never
        # claims more than was actually built.
        self.assertIn('nether_bastion', p['not_reproduced'])

    def test_the_world_is_fingerprinted_so_a_claim_can_prove_it_is_intact(self):
        m = foundry.publish(self.pool, 1, self.src, compilation())
        self.assertEqual(64, len(m['world_fingerprint']))
        again = foundry.publish(self.pool, 2, self.src, compilation())
        self.assertEqual(m['world_fingerprint'], again['world_fingerprint'],
                         'the same blocks fingerprint the same')

    def test_the_id_reflects_what_was_built_not_only_the_seed(self):
        a = foundry.map_id(1, compilation())
        moved = compilation()
        moved['evidence']['objective_world_xz'] = {'north': {'end_spike': [99, 99]}}
        self.assertNotEqual(a, foundry.map_id(1, moved))

    def test_republishing_the_same_map_is_refused(self):
        foundry.publish(self.pool, 1, self.src, compilation())
        with self.assertRaises(FileExistsError):
            foundry.publish(self.pool, 1, self.src, compilation())

    def test_inventory_counts_by_state(self):
        foundry.publish(self.pool, 1, self.src, compilation())
        foundry.publish(self.pool, 2, self.src, compilation())
        inv = foundry.inventory(self.pool)
        self.assertEqual(2, inv['counts'][foundry.READY])
        self.assertEqual(0, inv['counts'][foundry.IN_USE])
        # The runtime moves state; the foundry only ever writes READY, so the
        # two cannot race over the same file.
        entry = next(self.pool.glob('*/map.json'))
        data = json.loads(entry.read_text())
        data['state'] = foundry.IN_USE
        entry.write_text(json.dumps(data))
        self.assertEqual(1, foundry.inventory(self.pool)['counts'][foundry.IN_USE])


if __name__ == '__main__':
    unittest.main()
