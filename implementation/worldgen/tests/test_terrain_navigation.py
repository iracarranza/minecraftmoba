"""Stage 3 unit coverage. The live probe itself needs a server and is run separately."""
import unittest
import uuid
from terrain_harvest.navigation_probe import offline_uuid

class OfflineUuidTests(unittest.TestCase):
    def test_shape_and_determinism(self):
        u = uuid.UUID(offline_uuid('HarvestNav'))
        self.assertEqual(u.version, 3)
        self.assertEqual(u.variant, uuid.RFC_4122)
        self.assertEqual(offline_uuid('HarvestNav'), offline_uuid('HarvestNav'))
        self.assertNotEqual(offline_uuid('HarvestNav'), offline_uuid('harvestnav'))

    def test_matches_vanilla_construction(self):
        # Vanilla hashes the raw string with MD5; it is not a namespaced UUIDv3.
        import hashlib
        d = bytearray(hashlib.md5(b'OfflinePlayer:HarvestNav').digest())
        d[6] = (d[6] & 0x0f) | 0x30; d[8] = (d[8] & 0x3f) | 0x80
        self.assertEqual(offline_uuid('HarvestNav'), str(uuid.UUID(bytes=bytes(d))))
        self.assertNotEqual(offline_uuid('HarvestNav'), str(uuid.uuid3(uuid.NAMESPACE_DNS, 'OfflinePlayer:HarvestNav')))

if __name__ == '__main__':
    unittest.main()
