"""The ore survey classifies every ore block as cave-exposed, sky-exposed or buried, exactly."""

import sys
import unittest
from pathlib import Path

WORLDGEN = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(WORLDGEN))

from terrain_harvest import ore_survey as os_

STONE, AIR = 'minecraft:stone', 'minecraft:air'
COAL, IRON, DIAMOND = 'minecraft:coal_ore', 'minecraft:iron_ore', 'minecraft:deepslate_diamond_ore'


def pack(indices, palette_size):
    bits = max(4, (palette_size - 1).bit_length())
    per = 64 // bits
    longs = []
    for i in range(0, 4096, per):
        word = 0
        for j, v in enumerate(indices[i:i + per]):
            word |= v << (j * bits)
        longs.append(word - (1 << 64) if word >= 1 << 63 else word)
    return longs


def chunk(blocks, surface=60, default=STONE, lo_section=-4, hi_section=6, heights=True):
    """A chunk of `default` below `surface` and air above, with `blocks` {(x, y, z): name} placed on top.
    Coordinates are chunk-local x and z, absolute y."""
    sections = []
    for sy in range(lo_section, hi_section):
        names = [default, AIR] + sorted({n for n in blocks.values()})
        idx = {n: i for i, n in enumerate(names)}
        grid = [idx[default] if sy * 16 + (i >> 8) <= surface else idx[AIR] for i in range(4096)]
        for (x, y, z), n in blocks.items():
            if y >> 4 == sy:
                grid[(y & 15) * 256 + z * 16 + x] = idx[n]
        sections.append({'Y': sy, 'block_states': {'palette': [{'Name': n} for n in names],
                                                   'data': pack(grid, len(names))}})
    data = {'sections': sections}
    if heights:
        word = 0
        per = 7
        longs = []
        for i in range(0, 256, per):
            w = 0
            for j in range(min(per, 256 - i)):
                w |= (surface - os_.MIN_Y + 1) << (j * 9)
            longs.append(w)
        data['Heightmaps'] = {'WORLD_SURFACE': longs}
    return os_.Chunk(data)


def run(blocks, **kw):
    return os_.survey_chunks({(0, 0): chunk(blocks, **kw)}, cell_size=16)


class Unpack(unittest.TestCase):

    def test_a_single_entry_palette_is_all_that_entry(self):
        self.assertEqual([0] * 4096, os_.unpack({'palette': [{'Name': STONE}]}))

    def test_packing_round_trips_across_palette_sizes(self):
        for size in (2, 5, 17, 40):
            grid = [(i * 7) % size for i in range(4096)]
            got = os_.unpack({'palette': [{'Name': str(i)} for i in range(size)], 'data': pack(grid, size)})
            self.assertEqual(grid, got, size)

    def test_a_truncated_section_is_refused(self):
        with self.assertRaises(ValueError):
            os_.unpack({'palette': [{'Name': 'a'}, {'Name': 'b'}], 'data': [0, 0]})


class Heights(unittest.TestCase):

    def test_heights_read_back_as_the_top_block(self):
        self.assertEqual(60, chunk({}, surface=60).height_at(3, 9))

    def test_a_chunk_without_a_heightmap_has_no_surface(self):
        self.assertIsNone(chunk({}, heights=False).min_height())


class Classification(unittest.TestCase):

    def total(self, r, material, key):
        return r['by_material'][material][key]

    def test_ore_wholly_in_stone_is_buried(self):
        r = run({(5, 10, 5): COAL})
        self.assertEqual((0, 0, 1), tuple(r['by_material']['coal'][k] for k in ('cave', 'sky', 'buried')))

    def test_ore_touching_a_cave_is_cave_exposed(self):
        r = run({(5, 10, 5): COAL, (6, 10, 5): AIR})
        self.assertEqual(1, self.total(r, 'coal', 'cave'))
        self.assertEqual(0, self.total(r, 'coal', 'buried'))

    def test_ore_in_the_open_at_the_surface_is_sky_exposed(self):
        r = run({(5, 60, 5): COAL})        # its upper face is air at y 61, above the surface
        self.assertEqual(1, self.total(r, 'coal', 'sky'))
        self.assertEqual(0, self.total(r, 'coal', 'cave'))

    def test_cave_wins_when_an_ore_touches_both(self):
        r = run({(5, 59, 5): COAL, (5, 60, 5): AIR, (6, 59, 5): AIR}, surface=61)
        # (5,60,5) is air beneath a surface of 61, so it is a cave neighbour.
        self.assertEqual(1, self.total(r, 'coal', 'cave'))

    def test_fluid_is_neither_air_nor_solid(self):
        r = run({(5, 10, 5): COAL, (6, 10, 5): 'minecraft:water'})
        self.assertEqual(1, self.total(r, 'coal', 'buried'))

    def test_every_ore_block_is_classified_exactly_once(self):
        blocks = {(x, 10 + x, 3): (COAL if x % 2 else IRON) for x in range(8)}
        blocks[(9, 11, 3)] = AIR
        r = run(blocks)
        self.assertEqual(8, r['totals']['ore'])
        self.assertEqual(8, r['totals']['cave_exposed'] + r['totals']['sky_exposed'] + r['totals']['buried'])

    def test_a_chunk_without_a_surface_cannot_read_as_sky_exposed(self):
        r = run({(5, 60, 5): COAL}, heights=False)
        self.assertEqual(0, self.total(r, 'coal', 'sky'))

    def test_ore_on_an_ungenerated_edge_is_counted_but_its_neighbour_is_unknown(self):
        r = run({(0, 10, 0): COAL})        # two faces open onto chunks that do not exist
        self.assertEqual(1, r['totals']['ore'])
        self.assertGreaterEqual(r['totals']['unknown_neighbours'], 1)

    def test_a_neighbouring_chunks_air_exposes_ore_across_the_boundary(self):
        a = chunk({(15, 10, 5): COAL})
        b = chunk({(0, 10, 5): AIR})
        r = os_.survey_chunks({(0, 0): a, (1, 0): b}, cell_size=32)
        self.assertEqual(1, r['by_material']['coal']['cave'])


class Aggregation(unittest.TestCase):

    def test_the_y_range_limits_what_is_counted(self):
        blocks = {(1, 10, 1): COAL, (2, 80, 2): COAL}
        c = {(0, 0): chunk(blocks)}
        self.assertEqual(2, os_.survey_chunks(c, 16, (-60, 90))['totals']['ore'])
        self.assertEqual(1, os_.survey_chunks(c, 16, (-60, 40))['totals']['ore'])

    def test_depth_bands_partition_the_same_ore(self):
        r = run({(1, 10, 1): COAL, (2, -40, 2): COAL, (3, 50, 3): IRON})
        banded = sum(sum(v.values()) for m in r['by_depth_band'].values() for v in m.values())
        self.assertEqual(r['totals']['ore'], banded)
        self.assertIn('-64', r['by_depth_band']['coal'])

    def test_cave_volume_counts_air_in_sections_wholly_beneath_the_surface(self):
        r = run({(x, 10, 5): AIR for x in range(4)}, surface=60)
        self.assertEqual(4, r['totals']['cave_volume'])

    def test_sky_air_above_the_surface_is_not_cave_volume(self):
        self.assertEqual(0, run({}, surface=60)['totals']['cave_volume'])

    def test_reward_density_is_exposed_ore_per_thousand_cave_blocks(self):
        blocks = {(5, 10, 5): COAL, (6, 10, 5): AIR}
        r = run(blocks)
        self.assertEqual(1, r['totals']['cave_volume'])
        self.assertEqual(1000.0, r['totals']['cave_exposed_per_1k_cave'])

    def test_the_survey_assigns_no_target_and_says_so(self):
        r = run({(5, 10, 5): COAL})
        self.assertEqual('RAW WORLD OBSERVATION', r['evidence_state'])
        self.assertTrue(any('OPEN' in n for n in r['not_covered']))

    def test_shares_sum_to_one(self):
        r = run({(1, 10, 1): COAL, (2, 10, 2): COAL, (3, 10, 3): IRON})
        self.assertAlmostEqual(1.0, sum(v['share'] for v in r['by_material'].values()), places=3)


if __name__ == '__main__':
    unittest.main()
