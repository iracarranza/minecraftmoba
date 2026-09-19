"""Stage 2: exhaustive preservation auditing on small synthetic fixtures.

The audit recomputes expected destination state from the source and mask alone.
These tests deliberately damage a good export to prove the audit is not vacuous.
"""
import copy
import tempfile
import unittest
from pathlib import Path

from serialization.nbt import (COMPOUND, DOUBLE, byte, compound, double, dump_gzip,
    int_array, integer, list_tag, long, plain, string)
from serialization.region import read_region, write_region
from serialization.world import _block_states_tag, block, AIR
from terrain_harvest.materialize import DATA_VERSION, empty_chunk, export_volume
from terrain_harvest.model import make_volume, Mask
from terrain_harvest.preservation import audit_volume, fingerprint

SEED = 77
BOUNDS = {'x': [-4, 19], 'y': [-64, 15], 'z': [-4, 19]}

def volume(geometry='box', parameters=None, bounds=None):
    return make_volume({'source_seed': SEED, 'source_dimension': 'minecraft:overworld',
        'source_bounds': copy.deepcopy(bounds or BOUNDS), 'minecraft_version': '1.21.11',
        'data_version': DATA_VERSION, 'worldgen_settings': {'status': 'UNRESOLVED'},
        'source_analysis_record': {'path': 'fixture', 'sha256': 'fixture'}},
        'local_section', geometry, parameters or {})

def source_chunk(cx, cz, *, block_entity=None, ticks=True):
    root = empty_chunk(cx, cz)
    sections = []
    for sy in (-4, -1, 0):
        states = []
        for y in range(sy*16, sy*16+16):
            for z in range(cz*16, cz*16+16):
                for x in range(cx*16, cx*16+16):
                    # Deterministic but varied, including properties and a fluid.
                    if y < -60: states.append(block('bedrock'))
                    elif y < 0: states.append(block('deepslate', axis='y') if (x+z) % 3 else block('iron_ore'))
                    elif y < 8: states.append(block('water', level='0') if (x*z) % 7 == 0 else block('stone'))
                    else: states.append(AIR)
        sections.append(compound(Y=byte(sy), block_states=_block_states_tag(states),
            biomes=compound(palette=list_tag(8, [string('minecraft:plains')]))))
    root.value['sections'] = list_tag(COMPOUND, sections)
    if block_entity:
        # Chunk-local, as vanilla requires; a shared position would hide per-chunk loss.
        root.value['block_entities'] = list_tag(COMPOUND, [compound(
            id=string('minecraft:chest'), x=integer(cx*16 + block_entity[0]), y=integer(block_entity[1]),
            z=integer(cz*16 + block_entity[2]), keepPacked=byte(0))])
    if ticks:
        root.value['block_ticks'] = list_tag(COMPOUND, [compound(
            i=string('minecraft:stone'), x=integer(cx*16+1), y=integer(2), z=integer(cz*16+1),
            t=integer(0), p=integer(0))])
        root.value['fluid_ticks'] = list_tag(COMPOUND, [compound(
            i=string('minecraft:water'), x=integer(cx*16+2), y=integer(3), z=integer(cz*16+2),
            t=integer(0), p=integer(0))])
    return root

def entity(x, y, z, passengers=()):
    e = compound(id=string('minecraft:cow'), Pos=list_tag(DOUBLE, [double(x), double(y), double(z)]),
                 UUID=int_array([1, 2, 3, int(abs(x*100)+abs(z))]))
    if passengers: e.value['Passengers'] = list_tag(COMPOUND, list(passengers))
    return e

def build_source(root, chunks=((0, 0), (1, 0), (0, 1), (1, 1), (-1, -1), (-1, 0), (0, -1), (-1, 1), (1, -1)), **kw):
    (root/'region').mkdir(parents=True, exist_ok=True)
    (root/'entities').mkdir(parents=True, exist_ok=True)
    dump_gzip(root/'level.dat', '', compound(Data=compound(
        DataVersion=integer(DATA_VERSION),
        WorldGenSettings=compound(seed=long(SEED)))))
    buckets = {}
    for cx, cz in chunks:
        buckets.setdefault((cx//32, cz//32), {})[(cx, cz)] = ('', source_chunk(cx, cz, **kw))
    for (rx, rz), dest in buckets.items():
        write_region(root/'region'/f'r.{rx}.{rz}.mca', dest)
    write_region(root/'entities'/'r.0.0.mca', {(0, 0): ('', compound(
        DataVersion=integer(DATA_VERSION), Position=int_array([0, 0]), Entities=list_tag(COMPOUND, [
            entity(5.5, 1.0, 5.5),
            entity(6.5, 1.0, 6.5, [entity(100.5, 1.0, 100.5)]),
            entity(100.5, 1.0, 100.5)])))})
    return root

class PreservationAuditTests(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory(prefix='harvest-preservation-')
        self.tmp = Path(self._tmp.name)
        self.addCleanup(self._tmp.cleanup)

    def export(self, v, **kw):
        src = build_source(self.tmp/'source', block_entity=(5, 1, 5), **kw)
        dest = self.tmp/'dest'
        export_volume(v, src, dest)
        return src, dest

    def test_clean_export_audits_exhaustively(self):
        v = volume()
        src, dest = self.export(v)
        r = audit_volume(v, src, dest)
        self.assertTrue(r['pass'], r['failures'][:5])
        self.assertTrue(r['exhaustive'])
        self.assertEqual(r['coverage']['sections_clipped_skipped'], 0)
        self.assertGreater(r['coverage']['cells_compared'], 0)
        self.assertGreater(r['coverage']['sections_retained'], 0)
        self.assertEqual(r['entities']['expected_kept'], 1)
        self.assertEqual(r['entities']['destination_roots'], 1)
        self.assertEqual(r['entities']['excluded_root_outside_mask'], 1)
        self.assertEqual(r['entities']['excluded_passenger_outside_mask'], 1)

    def test_scoop_export_audits_exhaustively(self):
        v = volume('scoop', {'radius_scale_by_y': [[-64, '1/4'], [0, '3/4'], [15, '1']]})
        src, dest = self.export(v)
        r = audit_volume(v, src, dest)
        self.assertTrue(r['pass'], r['failures'][:5])
        self.assertTrue(r['exhaustive'])

    def _corrupt(self, dest, mutate):
        path = dest/'region'/'r.0.0.mca'
        roots = {(cx, cz): (name, root) for cx, cz, name, root in read_region(path)}
        mutate(roots)
        write_region(path, roots)

    def test_single_changed_block_is_caught(self):
        v = volume()
        src, dest = self.export(v)
        def mutate(roots):
            section = next(s for s in roots[(1, 1)][1].value['sections'].value if s.value['Y'].value == 0)
            section.value['block_states'] = _block_states_tag([block('gold_block')]*4096)
        self._corrupt(dest, mutate)
        r = audit_volume(v, src, dest)
        self.assertFalse(r['pass'])
        self.assertTrue(any(f['kind'] == 'cell_differs' for f in r['failures']))

    def test_retained_section_tamper_is_caught(self):
        v = volume()
        src, dest = self.export(v)
        def mutate(roots):
            section = next(s for s in roots[(0, 0)][1].value['sections'].value if s.value['Y'].value == -4)
            section.value['biomes'] = compound(palette=list_tag(8, [string('minecraft:desert')]))
        self._corrupt(dest, mutate)
        r = audit_volume(v, src, dest)
        self.assertFalse(r['pass'])
        self.assertTrue(any(f['kind'] in ('retained_section_differs', 'cell_differs') for f in r['failures']))

    def test_dropped_block_entity_is_caught(self):
        v = volume()
        src, dest = self.export(v)
        def mutate(roots):
            roots[(0, 0)][1].value['block_entities'] = list_tag(COMPOUND, [])
        self._corrupt(dest, mutate)
        r = audit_volume(v, src, dest)
        self.assertFalse(r['pass'])
        self.assertTrue(any(f['kind'] == 'block_entities_differs' for f in r['failures']))

    def test_altered_scheduled_tick_is_caught(self):
        v = volume()
        src, dest = self.export(v)
        def mutate(roots):
            roots[(0, 0)][1].value['fluid_ticks'].value[0].value['i'] = string('minecraft:lava')
        self._corrupt(dest, mutate)
        r = audit_volume(v, src, dest)
        self.assertFalse(r['pass'])
        self.assertTrue(any(f['kind'] == 'fluid_ticks_differs' for f in r['failures']))

    def test_missing_destination_chunk_is_caught(self):
        v = volume()
        src, dest = self.export(v)
        path = dest/'region'/'r.0.0.mca'
        roots = {(cx, cz): (name, root) for cx, cz, name, root in read_region(path) if (cx, cz) != (1, 1)}
        write_region(path, roots)
        r = audit_volume(v, src, dest)
        self.assertFalse(r['pass'])
        self.assertTrue(any(f['kind'] == 'missing_destination_chunk' for f in r['failures']))

    def test_smuggled_entity_is_caught(self):
        v = volume()
        src, dest = self.export(v)
        path = dest/'entities'/'r.0.0.mca'
        roots = {(cx, cz): (name, root) for cx, cz, name, root in read_region(path)}
        roots[(0, 0)][1].value['Entities'].value.append(entity(7.5, 1.0, 7.5))
        write_region(path, roots)
        r = audit_volume(v, src, dest)
        self.assertFalse(r['pass'])
        self.assertTrue(any(f['kind'] == 'entity_multiset_differs' for f in r['failures']))

    def test_coverage_limit_is_reported_not_implied(self):
        v = volume()
        src, dest = self.export(v)
        r = audit_volume(v, src, dest, max_clipped_sections=1)
        self.assertFalse(r['exhaustive'])
        self.assertGreater(r['coverage']['sections_clipped_skipped'], 0)
        self.assertEqual(r['coverage']['sections_clipped_compared'], 1)

class MalformedSourceTests(unittest.TestCase):
    """Unsupported or damaged source input must fail loudly, never become air."""
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory(prefix='harvest-malformed-')
        self.tmp = Path(self._tmp.name)
        self.addCleanup(self._tmp.cleanup)

    def test_missing_chunk_refuses_export(self):
        src = build_source(self.tmp/'source', chunks=((0, 0), (1, 0), (0, 1)), block_entity=(5, 1, 5))
        with self.assertRaisesRegex(ValueError, 'missing source chunks'):
            export_volume(volume(), src, self.tmp/'dest')

    def test_non_full_chunk_refuses_export(self):
        src = build_source(self.tmp/'source', block_entity=(5, 1, 5))
        path = src/'region'/'r.0.0.mca'
        roots = {(cx, cz): (name, root) for cx, cz, name, root in read_region(path)}
        roots[(1, 1)][1].value['Status'] = string('minecraft:features')
        write_region(path, roots)
        with self.assertRaisesRegex(ValueError, 'non-full source chunk'):
            export_volume(volume(), src, self.tmp/'dest')

    def test_version_mismatch_refuses_export(self):
        src = build_source(self.tmp/'source', block_entity=(5, 1, 5))
        path = src/'region'/'r.0.0.mca'
        roots = {(cx, cz): (name, root) for cx, cz, name, root in read_region(path)}
        roots[(0, 0)][1].value['DataVersion'] = integer(DATA_VERSION - 1)
        write_region(path, roots)
        with self.assertRaisesRegex(ValueError, 'version mismatch'):
            export_volume(volume(), src, self.tmp/'dest')

    def test_truncated_entity_region_is_not_silently_empty(self):
        src = build_source(self.tmp/'source', block_entity=(5, 1, 5))
        data = (src/'entities'/'r.0.0.mca').read_bytes()
        (src/'entities'/'r.0.0.mca').write_bytes(data[:len(data)//2])
        with self.assertRaises(Exception):
            export_volume(volume(), src, self.tmp/'dest')

    def test_seed_mismatch_refuses_export(self):
        src = build_source(self.tmp/'source', block_entity=(5, 1, 5))
        dump_gzip(src/'level.dat', '', compound(Data=compound(
            DataVersion=integer(DATA_VERSION), WorldGenSettings=compound(seed=long(SEED + 1)))))
        with self.assertRaisesRegex(ValueError, 'seed mismatch'):
            export_volume(volume(), src, self.tmp/'dest')

class FingerprintTests(unittest.TestCase):
    def test_types_are_not_collapsed(self):
        self.assertNotEqual(fingerprint(compound(a=byte(1))), fingerprint(compound(a=integer(1))))
        self.assertNotEqual(fingerprint(list_tag(COMPOUND, [])), fingerprint(list_tag(DOUBLE, [])))
        self.assertEqual(fingerprint(compound(a=byte(1), b=integer(2))),
                         fingerprint(compound(b=integer(2), a=byte(1))))

if __name__ == '__main__':
    unittest.main()
