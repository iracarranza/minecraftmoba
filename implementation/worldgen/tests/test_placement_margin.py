"""The offline simulation derives a source's region the way the plugin does: its radius, less a margin."""

import sys
import unittest
from pathlib import Path

WORLDGEN = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(WORLDGEN))

from terrain_harvest import simulate_regenerative as sim

CONFIG = """
  patch:
    spread: 6                   # how far
  herd:
    cluster: 5                  # how tightly
"""


class PlacementMargin(unittest.TestCase):

    def test_it_is_the_larger_of_the_patch_spread_and_half_the_herd_cluster(self):
        self.assertEqual(6, sim.placement_margin(CONFIG))
        self.assertEqual(6, sim.placement_margin(CONFIG.replace('cluster: 5', 'cluster: 11')))   # ceil(11/2)
        self.assertEqual(3, sim.placement_margin(CONFIG.replace('spread: 6', 'spread: 1')))

    def test_it_reads_the_plugins_own_shipped_config(self):
        text = sim.CONFIG.read_text()
        self.assertEqual(6, sim.placement_margin(text))

    def test_the_shipped_config_no_longer_carries_the_old_default_span(self):
        self.assertNotIn('migratedRegionHalfSpan', sim.CONFIG.read_text())


if __name__ == '__main__':
    unittest.main()
