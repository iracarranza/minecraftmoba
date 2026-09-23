"""Wiring existing measurement to the compiler, and refusing to gate on it."""
import unittest

from terrain_harvest import characterize as ch
from terrain_harvest import map_compiler as mc


class WindowVolume(unittest.TestCase):

    def test_the_volume_describes_the_compiled_window_not_the_origin(self):
        # A searched window need not sit at 0,0, so the measurement volume is
        # built from absolute bounds rather than assumed.
        v = ch.window_volume(1, (1000, 1863, -2000, -945))
        self.assertEqual([1000, 1863], v['provenance']['source_bounds']['x'])
        self.assertEqual([-2000, -945], v['provenance']['source_bounds']['z'])
        self.assertEqual('box', v['boundary']['geometry_type'])

    def test_it_reuses_the_existing_volume_vocabulary(self):
        # Adding a new `kind` would fork a schema that already classifies these.
        v = ch.window_volume(1, (0, 63, 0, 63))
        self.assertEqual('whole_map', v['classification']['kind'])


class Stage(unittest.TestCase):

    def test_without_a_world_it_reports_unmeasured_rather_than_failing(self):
        out = mc.characterize(mc.Compilation(seed=1), None)
        self.assertEqual([], [r.code for r in out.rejections])
        self.assertFalse(out.evidence['characterization']['measured'])
        self.assertIn('no world', out.evidence['characterization']['why'])

    def test_without_bounds_it_says_so(self):
        out = mc.characterize(mc.Compilation(seed=1), '/tmp')
        self.assertFalse(out.evidence['characterization']['measured'])
        self.assertIn('block bounds', out.evidence['characterization']['why'])

    def test_it_never_rejects_a_map(self):
        # maps.md leaves the depth gradient, regional tables and resource
        # vocabulary OPEN. A stage that invented a bound to look decisive would
        # be worse than the gap it filled.
        out = mc.Compilation(seed=1)
        out.evidence['region'] = {'block_bounds': [0, 63, 0, 63]}
        mc.characterize(out, '/tmp/moba-no-such-world')
        self.assertEqual([], [r.code for r in out.rejections],
                         'characterization is evidence, not a gate')
        self.assertFalse(out.evidence['characterization']['measured'])

    def test_characterize_is_a_stage_and_runs_after_verify(self):
        self.assertIn('characterize', mc.STAGES)
        self.assertGreater(mc.STAGES.index('characterize'), mc.STAGES.index('verify'))
        self.assertLess(mc.STAGES.index('characterize'), mc.STAGES.index('ready'))


if __name__ == '__main__':
    unittest.main()
