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


DEEPSLATE_COAL = 'minecraft:deepslate_coal_ore'


class Veins(unittest.TestCase):

    def veins(self, blocks, **kw):
        return run(blocks, **kw)['veins']

    def test_face_edge_and_corner_neighbours_all_join_a_vein(self):
        for second in ((2, 10, 1), (2, 11, 1), (2, 11, 2)):       # face, edge, corner
            v = self.veins({(1, 10, 1): COAL, second: COAL})['coal']
            self.assertEqual(1, v['veins'], second)
            self.assertEqual(2, v['size']['max'])

    def test_a_gap_of_one_block_separates_veins(self):
        v = self.veins({(1, 10, 1): COAL, (3, 10, 1): COAL})['coal']
        self.assertEqual(2, v['veins'])

    def test_different_materials_never_join(self):
        v = self.veins({(1, 10, 1): COAL, (2, 10, 1): IRON})
        self.assertEqual(1, v['coal']['veins'])
        self.assertEqual(1, v['iron']['veins'])

    def test_stone_and_deepslate_variants_of_one_ore_are_one_vein(self):
        v = self.veins({(1, 10, 1): COAL, (2, 10, 1): DEEPSLATE_COAL})['coal']
        self.assertEqual(1, v['veins'])

    def test_a_vein_crosses_a_chunk_boundary(self):
        a = chunk({(15, 10, 5): COAL})
        b = chunk({(0, 10, 5): COAL})
        r = os_.survey_chunks({(0, 0): a, (1, 0): b}, cell_size=32)
        self.assertEqual(1, r['veins']['coal']['veins'])
        self.assertEqual(2, r['veins']['coal']['size']['max'])

    def test_every_ore_block_belongs_to_exactly_one_vein(self):
        blocks = {(x, 10, z): COAL for x in range(0, 6, 2) for z in range(0, 6, 2)}
        blocks.update({(9, 20, 9): COAL, (10, 21, 9): COAL, (10, 22, 10): COAL})
        r = run(blocks)
        self.assertEqual(r['totals']['ore'], sum(v['blocks'] for v in r['veins'].values()))

    def test_size_statistics_and_buckets(self):
        blocks = {(1, 10, 1): COAL}                                      # a single
        blocks.update({(5 + i, 20, 5): COAL for i in range(5)})          # five in a row
        v = self.veins(blocks)['coal']
        self.assertEqual(2, v['veins'])
        self.assertEqual({'min': 1, 'median': 5, 'mean': 3.0, 'p90': 5, 'max': 5}, v['size'])
        self.assertEqual(1, v['size_buckets']['1'])
        self.assertEqual(1, v['size_buckets']['4-7'])
        self.assertAlmostEqual(1 / 6, v['ore_in_veins_of_3_or_fewer'], places=3)

    def test_a_vein_touching_a_cave_is_counted_as_exposed(self):
        blocks = {(5, 10, 5): COAL, (6, 10, 5): COAL, (7, 10, 5): AIR, (1, 30, 1): COAL}
        v = self.veins(blocks)['coal']
        self.assertEqual(2, v['veins'])
        self.assertEqual(1, v['veins_with_cave_exposure'])

    def test_a_vein_is_clipped_by_the_y_range_and_says_so(self):
        c = {(0, 0): chunk({(1, y, 1): COAL for y in range(30, 50)})}
        self.assertEqual(20, os_.survey_chunks(c, 16, (-60, 90))['veins']['coal']['size']['max'])
        self.assertEqual(11, os_.survey_chunks(c, 16, (-60, 40))['veins']['coal']['size']['max'])
        self.assertTrue(any('clipped' in n or 'cut by' in n for n in run({}).get('not_covered', [])))


class Spacing(unittest.TestCase):

    def test_nearest_neighbour_distances(self):
        d = os_._nearest([(0, 0, 0), (10, 0, 0), (30, 0, 0)])
        self.assertEqual([10, 10, 20], [round(x) for x in d])

    def test_a_lone_point_is_isolated(self):
        self.assertEqual([None], os_._nearest([(0, 0, 0)]))

    def test_a_neighbour_beyond_the_cap_is_isolated(self):
        self.assertEqual([None, None], os_._nearest([(0, 0, 0), (500, 0, 0)]))

    def test_the_search_finds_a_neighbour_in_a_distant_grid_cell(self):
        d = os_._nearest([(0, 0, 0), (100, 0, 0), (100, 0, 3)])
        self.assertEqual([100, 3, 3], [round(x) for x in d])

    def test_it_matches_a_brute_force_search(self):
        import random
        rng = random.Random(3)
        pts = [(rng.uniform(0, 120), rng.uniform(0, 60), rng.uniform(0, 120)) for _ in range(80)]
        brute = [min(sum((a - b) ** 2 for a, b in zip(p, q)) ** 0.5 for q in pts if q is not p) for p in pts]
        for got, want in zip(os_._nearest(pts), brute):
            self.assertAlmostEqual(want, got, places=6)

    def test_spacing_is_measured_within_a_material_and_depth_band(self):
        blocks = {(1, 10, 1): COAL, (11, 10, 1): COAL,              # 10 apart, same band
                  (3, -50, 1): COAL, (13, -50, 1): COAL,            # another band
                  (2, 10, 8): IRON, (3, 11, 12): IRON}
        r = run(blocks)['vein_spacing']['by_material']
        self.assertEqual(10.0, r['coal']['0']['nearest_median'])
        self.assertEqual(10.0, r['coal']['-64']['nearest_median'])

    def test_veins_of_different_materials_do_not_count_as_neighbours(self):
        r = run({(1, 10, 1): COAL, (11, 10, 1): IRON})['vein_spacing']['by_material']
        self.assertIsNone(r['coal']['0']['nearest_median'])
        self.assertEqual(1, r['coal']['0']['isolated_within_cap'])


class Distance(unittest.TestCase):

    def steps(self, blocks, at, cap=12, **kw):
        c = {(0, 0): chunk(blocks, **kw)}
        return os_.steps_to_cave_air(c, *at, cap)

    def test_air_next_to_the_start_is_one_step(self):
        self.assertEqual(1, self.steps({(6, 10, 5): AIR}, (5, 10, 5)))

    def test_it_counts_the_rock_dug_through(self):
        self.assertEqual(3, self.steps({(8, 10, 5): AIR}, (5, 10, 5)))

    def test_it_goes_around_corners_by_path_not_straight_line(self):
        # air diagonally up-and-over is three moves away on a six-neighbour path
        self.assertEqual(3, self.steps({(6, 11, 6): AIR}, (5, 10, 5)))

    def test_air_beyond_the_cap_is_not_found(self):
        self.assertIsNone(self.steps({(15, 10, 5): AIR}, (1, 10, 5), cap=12))
        self.assertEqual(14, self.steps({(15, 10, 5): AIR}, (1, 10, 5), cap=15))

    def test_sky_air_above_the_surface_is_not_a_cave(self):
        self.assertIsNone(self.steps({}, (5, 55, 5), cap=4))      # surface 60: no air within 4 below it

    def test_air_in_an_ungenerated_chunk_is_not_found(self):
        self.assertIsNone(self.steps({}, (0, 10, 0), cap=3))

    def test_the_report_buckets_every_sampled_block(self):
        blocks = {(5, 10, 5): COAL, (8, 10, 5): AIR, (5, 30, 5): COAL}
        r = run(blocks)['distance_to_cave']['by_material']['coal']
        self.assertEqual(2, r['sampled'])
        self.assertEqual(2, sum(r['buckets'].values()))
        self.assertEqual(1, r['buckets']['3-4'])
        self.assertEqual(1, r['buckets']['>12'])

    def test_an_exposed_ore_block_is_distance_one(self):
        r = run({(5, 10, 5): COAL, (6, 10, 5): AIR})['distance_to_cave']['by_material']['coal']
        self.assertEqual(1, r['buckets']['1'])

    def test_sampling_is_a_deterministic_stride(self):
        blocks = {(x, 10, z): COAL for x in range(0, 16, 2) for z in range(0, 16, 2)}     # 64 singles
        a = os_.survey_chunks({(0, 0): chunk(blocks)}, 16, distance_samples=8)['distance_to_cave']['by_material']['coal']
        b = os_.survey_chunks({(0, 0): chunk(blocks)}, 16, distance_samples=8)['distance_to_cave']['by_material']['coal']
        self.assertEqual(a, b)
        self.assertEqual(8, a['sampled'])
        self.assertEqual(8, a['stride'])

    def test_the_bucket_labels_follow_the_cap(self):
        self.assertEqual(['1', '2', '3-4', '5-8', '9-12'], [b[2] for b in os_._distance_buckets(12)])
        self.assertEqual(['1', '2', '3-4', '5-6'], [b[2] for b in os_._distance_buckets(6)])


class Keys(unittest.TestCase):

    def test_positions_round_trip_through_their_integer_key(self):
        for p in ((0, 0, 0), (-1000, -64, 1000), (123456, 300, -98765), (-3, 90, -3)):
            self.assertEqual(p, os_.pos_of(os_.key_of(*p)))

    def test_neighbour_offsets_are_constant_arithmetic(self):
        k = os_.key_of(10, 20, 30)
        self.assertEqual(os_.key_of(11, 21, 29), k + os_._delta(1, 1, -1))
        self.assertEqual(26, len(os_.CONNECT))


if __name__ == '__main__':
    unittest.main()
