"""Exact section counting must agree with the original finite-volume scan."""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from collections import Counter
import copy
import tempfile
import unittest

from serialization.nbt import COMPOUND, byte, compound, list_tag, plain, string
from serialization.region import write_region
from serialization.world import _block_states_tag, block
from terrain_harvest.characterize import window_volume
from terrain_harvest.materialize import empty_chunk
from terrain_harvest.model import make_volume
from terrain_harvest.opportunity_map import _section_counts, scan


class ExactOpportunityScan(unittest.TestCase):
    def states(self):
        # 20 entries exercise 5-bit packing, signed longs and last-long padding.
        names = ['stone', 'iron_ore', 'deepslate_iron_ore', 'bamboo', 'pumpkin']
        names += [f'test_block_{i}' for i in range(15)]
        return [block(names[(i * 7 + i // 23) % len(names)]) for i in range(4096)]

    def test_histogram_preserves_all_counts_across_packed_long_boundaries(self):
        states = self.states()
        self.assertEqual(Counter(s[0] for s in states),
                         _section_counts(plain(_block_states_tag(states))))
        self.assertEqual({'minecraft:diamond_ore': 4096},
                         _section_counts(plain(_block_states_tag([block('diamond_ore')]*4096))))

    def fixture(self, root):
        for cx in (-2, -1):
            for cz in (-1, 0):
                chunk = empty_chunk(cx, cz)
                chunk.value['sections'] = list_tag(COMPOUND, [
                    compound(Y=byte(sy), block_states=_block_states_tag(self.states()),
                             biomes=compound(palette=list_tag(8, [string('minecraft:plains')])))
                    for sy in (-4, 4)])
                path = root / 'region' / f'r.{cx//32}.{cz//32}.mca'
                path.parent.mkdir(parents=True, exist_ok=True)
                from serialization.region import read_region
                chunks = {(x, z): (name, data) for x, z, name, data in read_region(path)} if path.exists() else {}
                chunks[(cx, cz)] = ('', chunk)
                write_region(path, chunks)

    def test_full_and_clipped_volumes_and_strides_match_original_scan(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.fixture(root)
            cases = [((-32, -1, -16, 15), (-64, 79), 64, 1, 'box'),
                     ((-29, -3, -13, 12), (-61, 74), 24, 1, 'box'),
                     ((-32, -1, -16, 15), (-64, 79), 64, 3, 'box'),
                     ((-32, -1, -16, 15), (-64, 79), 64, 1, 'ellipse')]
            for bounds, ys, cell_size, stride, geometry in cases:
                with self.subTest(bounds=bounds, ys=ys, stride=stride, geometry=geometry):
                    volume = window_volume(1, bounds, ys)
                    if geometry != 'box':
                        volume = make_volume(copy.deepcopy(volume['provenance']),
                                             kind='whole_map', geometry=geometry)
                    self.assertEqual(scan(volume, root, cell_size, ys, stride,
                                          fast_sections=False),
                                     scan(volume, root, cell_size, ys, stride))


if __name__ == '__main__':
    unittest.main()
