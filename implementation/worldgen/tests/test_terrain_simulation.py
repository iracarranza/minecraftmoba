"""Stage 4 unit coverage for the drift diff. The live probe needs a server."""
import shutil
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from serialization.nbt import COMPOUND, compound, list_tag, string
from serialization.region import read_region, write_region
from serialization.world import _block_states_tag, block
from terrain_harvest.materialize import export_volume
from terrain_harvest.model import Mask
from terrain_harvest.simulation import _subset, diff_regions
from test_terrain_preservation import build_source, volume

class DiffTests(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory(prefix='harvest-sim-')
        self.tmp = Path(self._tmp.name); self.addCleanup(self._tmp.cleanup)
        self.v = volume()
        src = build_source(self.tmp/'source', block_entity=(5, 1, 5))
        self.before = self.tmp/'before'
        export_volume(self.v, src, self.before)
        self.after = self.tmp/'after'
        shutil.copytree(self.before, self.after)
        self.mask = Mask(self.v)

    def mutate(self, chunk, section_y, states):
        path = self.after/'region'/'r.0.0.mca'
        roots = {(cx, cz): (n, r) for cx, cz, n, r in read_region(path)}
        section = next(s for s in roots[chunk][1].value['sections'].value if s.value['Y'].value == section_y)
        section.value['block_states'] = _block_states_tag(states)
        write_region(path, roots)

    def test_identical_worlds_report_no_drift(self):
        d = diff_regions(self.before, self.after, self.mask, 'none')
        self.assertEqual(d['counts']['cells_changed'], 0)
        self.assertEqual(d['counts']['sections_differing'], 0)
        self.assertGreater(d['counts']['sections_compared'], 0)

    def test_in_mask_change_is_classified(self):
        self.mutate((0, 0), 0, [block('gold_block')]*4096)
        d = diff_regions(self.before, self.after, self.mask, 'in mask')
        self.assertGreater(d['counts']['in_mask_changed'], 0)
        self.assertEqual(d['counts']['envelope_breached'], 0)
        self.assertTrue(any(s['bucket'] == 'in_mask_changed' for s in d['samples']))

    def test_envelope_breach_is_flagged(self):
        # Chunk (1, 1) holds shell cells beyond the mask's +x/+z corner.
        self.mutate((1, 1), 0, [block('gold_block')]*4096)
        d = diff_regions(self.before, self.after, self.mask, 'breach')
        self.assertGreater(d['counts']['envelope_changed'], 0)
        self.assertEqual(d['counts']['envelope_breached'], d['counts']['envelope_changed'])

    def test_shell_replaced_by_shell_is_not_a_breach(self):
        top = self.mask.bounds['y'][1]
        path = self.after/'region'/'r.0.0.mca'
        roots = {(cx, cz): (n, r) for cx, cz, n, r in read_region(path)}
        section = next(s for s in roots[(1, 1)][1].value['sections'].value if s.value['Y'].value == 0)
        states = []
        for y in range(0, 16):
            for z in range(16, 32):
                for x in range(16, 32):
                    if self.mask.include_block(x, y, z): states.append(block('gold_block'))
                    elif self.mask.envelope(x, y, z): states.append(block('barrier') if y > top else block('bedrock'))
                    else: states.append(block('air'))
        section.value['block_states'] = _block_states_tag(states)
        write_region(path, roots)
        d = diff_regions(self.before, self.after, self.mask, 'shell intact')
        self.assertEqual(d['counts']['envelope_breached'], 0)

class NormalizationTests(unittest.TestCase):
    """The server spells out default properties on load; that is not drift."""
    def test_added_default_property_is_normalization(self):
        self.assertTrue(_subset({}, {'waterlogged': 'false'}))
        self.assertTrue(_subset({'waterlogged': 'false'}, {'waterlogged': 'false'}))

    def test_conflicting_value_is_not_normalization(self):
        self.assertFalse(_subset({'waterlogged': 'true'}, {'waterlogged': 'false'}))

    def test_barrier_gaining_waterlogged_is_not_a_breach(self):
        top = self.mask_top()
        path = self.after/'region'/'r.0.0.mca'
        roots = {(cx, cz): (n, r) for cx, cz, n, r in read_region(path)}
        section = next(s for s in roots[(1, 1)][1].value['sections'].value if s.value['Y'].value == 0)
        states = []
        for y in range(0, 16):
            for z in range(16, 32):
                for x in range(16, 32):
                    if self.mask.include_block(x, y, z): states.append(block('stone'))
                    elif self.mask.envelope(x, y, z):
                        states.append(block('barrier', waterlogged='false') if y > top
                                      else block('bedrock'))
                    else: states.append(block('air'))
        section.value['block_states'] = _block_states_tag(states)
        write_region(path, roots)
        d = diff_regions(self.before, self.after, self.mask, 'normalized shell')
        self.assertEqual(d['counts']['envelope_breached'], 0)

    def mask_top(self):
        return self.mask.bounds['y'][1]

    def setUp(self):
        DiffTests.setUp(self)

if __name__ == '__main__':
    unittest.main()
