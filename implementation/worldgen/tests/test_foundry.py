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


def compilation(playable=True, stage='verify'):
    return {
        'playable': playable,
        'deepest_stage_reached': stage,
        'evidence': {
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
            foundry.publish(self.pool, 1, self.src, compilation(playable=False, stage='lair'))
        self.assertFalse(self.pool.exists())

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
