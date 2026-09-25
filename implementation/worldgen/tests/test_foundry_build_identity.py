import unittest
from unittest.mock import patch
from pathlib import Path
from terrain_harvest import foundry_run

class BuildIdentity(unittest.TestCase):
    def test_repeated_seed_windows_keep_their_own_authored_build(self):
        with patch.object(foundry_run.compile_batch, 'run', return_value={'runs': []}):
            a=foundry_run._compile_one((12,'a.json','/raw/a','/work/12_0'))
            b=foundry_run._compile_one((12,'b.json','/raw/b','/work/12_1'))
        self.assertEqual(Path('/work/12_0/build-12'),Path(a['build_world']))
        self.assertEqual(Path('/work/12_1/build-12'),Path(b['build_world']))
        self.assertNotEqual(a['build_world'],b['build_world'])
