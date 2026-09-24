import os
import struct
import tempfile
import unittest

from terrain_harvest import scan as S


def write(path, seed=42, n=4, half=64, step=32, version=2, height=None, biome=None):
    height = height if height is not None else list(range(n * n))
    biome = biome if biome is not None else [0] * (n * n)
    with open(path, 'wb') as fh:
        fh.write(f'mobascan {version} {seed} {n} {half} {step}\n'.encode())
        for h, b in zip(height, biome):
            fh.write(struct.pack('<hh', h, b))
    return path


class Format(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.mkdtemp()

    def path(self, name='s.bin'):
        return os.path.join(self.dir, name)

    def test_round_trip(self):
        s = S.read(write(self.path(), height=[10, 20, 30, 40] * 4))
        self.assertEqual(s.seed, 42)
        self.assertEqual(s.n, 4)
        self.assertEqual(s.height[:4], [10, 20, 30, 40])
        self.assertEqual(len(s.biome), 16)

    def test_negative_heights_survive(self):
        """Deep ocean floor is below zero and must not wrap."""
        s = S.read(write(self.path(), height=[-40] * 16))
        self.assertEqual(set(s.height), {-40})

    def test_block_coordinates_are_centred_on_the_origin(self):
        s = S.read(write(self.path(), n=4, half=64, step=32))
        self.assertEqual(s.block_at(0, 0), (-64, -64))
        self.assertEqual(s.block_at(2, 2), (0, 0))

    def test_v1_is_refused_and_says_why(self):
        """v1 carried ocean/land bytes and no height; it cannot feed a search."""
        with self.assertRaises(ValueError) as e:
            S.read(write(self.path(), version=1))
        self.assertIn('height', str(e.exception))

    def test_a_truncated_scan_is_refused(self):
        p = write(self.path())
        with open(p, 'rb') as fh:
            data = fh.read()
        with open(p, 'wb') as fh:
            fh.write(data[:-8])
        with self.assertRaises(ValueError):
            S.read(p)

    def test_a_foreign_file_is_refused(self):
        p = self.path('x.bin')
        with open(p, 'wb') as fh:
            fh.write(b'not a scan at all\n' + b'\0' * 64)
        with self.assertRaises(ValueError):
            S.read(p)


class Derived(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.mkdtemp()

    def test_ocean_mask_uses_the_library_ids(self):
        p = os.path.join(self.dir, 's.bin')
        # ocean, deep_ocean, plains-ish, river
        S.read(write(p, n=2, biome=[0, 24, 1, 7], height=[60] * 4))
        s = S.read(p)
        self.assertEqual(s.ocean_mask(), [True, True, False, False])

    def test_feature_grid_matches_what_scoop_reads(self):
        from terrain_harvest import scoop
        p = os.path.join(self.dir, 'g.bin')
        s = S.read(write(p, n=8, step=8, half=32, height=[64] * 64))
        out = scoop.describe(s.feature_grid())
        self.assertEqual(out['scoop_blocks'], [64, 64])


class Absence(unittest.TestCase):
    def test_a_missing_scanner_raises_instead_of_returning_nothing(self):
        """The opposite of `seed_screen`, deliberately.

        A screen that fails open costs throughput. A search with no scan has
        nothing to search, and an empty result would read as 'this seed has no
        symmetric region' when it means 'nothing looked'.
        """
        old = os.environ.pop(S.SCAN_ENV, None)
        try:
            with unittest.mock.patch('shutil.which', return_value=None):
                with self.assertRaises(RuntimeError) as e:
                    S.scan(1)
            self.assertIn('nothing to search', str(e.exception))
        finally:
            if old:
                os.environ[S.SCAN_ENV] = old


import unittest.mock  # noqa: E402

if __name__ == '__main__':
    unittest.main()
