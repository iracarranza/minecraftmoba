"""Structure building writes real blocks and refuses to invent terrain."""
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from serialization.nbt import COMPOUND, byte, compound, list_tag, plain, string
from serialization.region import read_region, write_region
from serialization.world import AIR, _block_states_tag, block
from terrain_harvest.build_structures import (TEMPLATES, WorldEditor, aether_fountain, build)
from terrain_harvest.materialize import empty_chunk, state_tuple
from vanilla_search.extract import _palette_value


def world_with_chunks(root, chunks, sections=range(-1, 6)):
    (root / 'region').mkdir(parents=True, exist_ok=True)
    by_region = {}
    for cx, cz in chunks:
        chunk = empty_chunk(cx, cz)
        parts = []
        for sy in sections:
            states = [block('stone') if y < 0 else AIR
                      for y in range(sy * 16, sy * 16 + 16)
                      for _ in range(256)]
            parts.append(compound(Y=byte(sy), block_states=_block_states_tag(states),
                                  biomes=compound(palette=list_tag(8, [string('minecraft:plains')]))))
        chunk.value['sections'] = list_tag(COMPOUND, parts)
        by_region.setdefault((cx >> 5, cz >> 5), {})[(cx, cz)] = ('', chunk)
    for (rx, rz), dest in by_region.items():
        write_region(root / 'region' / f'r.{rx}.{rz}.mca', dest)
    return root


def read_block(world, x, y, z):
    path = world / 'region' / f'r.{(x >> 4) >> 5}.{(z >> 4) >> 5}.mca'
    for cx, cz, _, root in read_region(path):
        if (cx, cz) != (x >> 4, z >> 4): continue
        for s in root.value['sections'].value:
            if s.value['Y'].value != y >> 4: continue
            container = plain(s).get('block_states')
            if not container: return AIR
            return state_tuple(_palette_value(container, (y & 15) * 256 + (z & 15) * 16 + (x & 15), 4))
    raise AssertionError('chunk not found')


class EditorTests(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory(); self.addCleanup(self._tmp.cleanup)
        self.world = world_with_chunks(Path(self._tmp.name) / 'w',
                                       [(cx, cz) for cx in range(-2, 3) for cz in range(-2, 3)])

    def test_written_block_reads_back(self):
        e = WorldEditor(self.world)
        e.set(5, 3, 5, block('glowstone'))
        e.flush()
        self.assertEqual(read_block(self.world, 5, 3, 5), block('glowstone'))

    def test_untouched_neighbour_is_preserved(self):
        before = read_block(self.world, 6, -1, 5)
        e = WorldEditor(self.world); e.set(5, 3, 5, block('glowstone')); e.flush()
        self.assertEqual(read_block(self.world, 6, -1, 5), before)

    def test_missing_chunk_refuses_rather_than_inventing(self):
        e = WorldEditor(self.world)
        e.set(5000, 3, 5000, block('glowstone'))
        with self.assertRaises((FileNotFoundError, KeyError)):
            e.flush()


class TemplateTests(unittest.TestCase):
    def test_every_named_structure_has_a_template(self):
        self.assertEqual(set(TEMPLATES), {
            'aether_fountain', 'pillager_outpost', 'nether_bastion', 'end_spike',
            # Historical, kept under its own name so old artifacts stay readable.
            'end_tower',
            # The hand-modelled massing the vanilla NBT replaced, kept for quick
            # greybox work but no longer what gets built.
            'pillager_outpost_greybox', 'nether_bastion_greybox'})

    def test_the_current_forms_are_vanilla_geometry_not_massing(self):
        # The greybox Outpost was a 7x7 dark oak box standing in for a 15x15
        # watchtower, so a site was verified against the measured contract and
        # then authored to a different size. Real NBT removes the disagreement.
        tower = TEMPLATES['pillager_outpost']()
        self.assertEqual(1156, len(tower), 'the vanilla watchtower, block for block')
        xs = [d[0] for d in tower]
        self.assertEqual(15, max(xs) - min(xs) + 1)
        self.assertGreater(len(TEMPLATES['nether_bastion']()),
                           len(TEMPLATES['nether_bastion_greybox']()) * 10)

    def test_fountain_is_water_lined_with_glowstone(self):
        t = aether_fountain()
        names = {s[0] for s in t.values()}
        self.assertIn('minecraft:water', names)
        self.assertIn('minecraft:glowstone', names)
        self.assertEqual(t[(0, 4, 0)], block('water', level='0'))   # the source

    def test_templates_are_nonempty_and_bounded(self):
        # The old ceiling was 40, written when every template was greybox
        # massing. The measured forms are taller than that on purpose: the
        # Bastion body is two stacked vanilla pieces and the arena spike is
        # 76-103 blocks by measurement. The bound is now the tallest thing the
        # measured contract admits.
        for name, fn in TEMPLATES.items():
            cells = fn()
            self.assertGreater(len(cells), 50, name)
            self.assertLessEqual(max(dy for _, dy, _ in cells), 110, name)


class BuildTests(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory(); self.addCleanup(self._tmp.cleanup)
        self.tmp = Path(self._tmp.name)
        self.world = world_with_chunks(self.tmp / 'w',
                                       [(cx, cz) for cx in range(-3, 4) for cz in range(-3, 4)])

    def test_dry_run_writes_nothing(self):
        r = build(self.world, [{'structure': 'aether_fountain', 'team': 'a', 'world_xyz': [8, 2, 8]}],
                  self.tmp / 'dry.json', dry_run=True)
        self.assertTrue(r['dry_run'])
        self.assertEqual(read_block(self.world, 8, 2, 8), AIR)

    def test_fountain_appears_in_the_world(self):
        build(self.world, [{'structure': 'aether_fountain', 'team': 'a', 'world_xyz': [8, 2, 8]}],
              self.tmp / 'built.json')
        self.assertEqual(read_block(self.world, 8, 6, 8), block('water', level='0'))
        self.assertEqual(read_block(self.world, 8, 1, 8), block('polished_diorite'))

    def test_report_states_what_is_not_covered(self):
        r = build(self.world, [{'structure': 'end_tower', 'team': 'b', 'world_xyz': [8, 2, 8]}],
                  self.tmp / 'r.json')
        joined = ' '.join(r['not_covered'])
        self.assertIn('unresolved in objectives.md', joined)
        self.assertIn('greybox massing', r['nature'])

    def test_unknown_structure_is_refused(self):
        with self.assertRaisesRegex(ValueError, 'unknown structure'):
            build(self.world, [{'structure': 'nexus', 'world_xyz': [8, 2, 8]}], self.tmp / 'x.json')


if __name__ == '__main__':
    unittest.main()
