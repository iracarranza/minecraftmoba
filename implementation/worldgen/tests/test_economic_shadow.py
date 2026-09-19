"""Synthetic fixtures are decoder/preflight tests, never seed measurements."""
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import economic_shadow as e
from vanilla_search.extract import VanillaChunk

class EconomicShadowTests(unittest.TestCase):
    def test_wrong_repo_stops_before_discovery(self):
        with patch.object(e,'git',return_value='https://github.com/iracarranza/volley-world-manager.git') as call:
            with self.assertRaisesRegex(ValueError,'STOP'): e.inventory(Path('/tmp'))
            self.assertEqual(call.call_count,1)

    def test_identity_rejects_suffix_spoof(self):
        with patch.object(e,'git',return_value='https://github.com/iracarranza/minecraftmoba.evil'):
            with self.assertRaises(ValueError): e.identity(Path('/tmp'))

    def test_inventory_finds_moved_corpus_and_reference_only_material(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            def git(*args): return subprocess.check_output(['git','-C',tmp,*args],text=True)
            git('init','-q'); git('config','user.email','fixture@example.invalid');git('config','user.name','Fixture')
            git('remote','add','origin','https://github.com/iracarranza/minecraftmoba.git')
            (root/'docs').mkdir(); p=root/'docs'/'renamed-economy.md';p.write_text('Practical Reach')
            git('add','.');git('commit','-m','fixture');git('update-ref','refs/remotes/origin/main','HEAD')
            p.unlink();(root/'docs'/'untracked.md').write_text('current manuscript')
            files={v['path']:v for v in e.inventory(root)['files']}
            self.assertIsNone(files['docs/renamed-economy.md']['worktree_sha256'])
            self.assertTrue(files['docs/renamed-economy.md']['reference_sha256'])
            self.assertIn('docs/untracked.md',files)

    def test_missing_candidate_is_unresolved(self):
        with tempfile.TemporaryDirectory() as tmp:
            result=e.seed_analysis(Path(tmp),930010639,Path(tmp))
            self.assertEqual(result['measurement_status'],'UNRESOLVED')
            self.assertEqual(result['handoffs'],[])

    def test_missing_world_is_not_zero_resource(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaisesRegex(ValueError,'UNRESOLVED'): e.load_chunks(Path(tmp),{(0,0)})

    def test_negative_coordinates_and_inclusive_clip(self):
        self.assertEqual(e.bounded_box((-10,60,0),[-20,20,-20,20]),[-20,20,-64,319,-20,20])

    def test_component_adjacency_and_clipped_depletion_stock(self):
        points=[[0,0,0,'minecraft:iron_ore'],[1,0,0,'minecraft:deepslate_iron_ore'],[2,1,0,'minecraft:iron_ore']]
        s=e.summarize(points,[0,2,-1,2,-1,1])['iron']
        self.assertEqual(s['blocks'],3);self.assertEqual(s['six_face_components'],2)
        self.assertEqual(s['largest_component_blocks'],2);self.assertEqual(s['boundary_components'],2)

    def test_packed_states_do_not_count_unused_palette_entries(self):
        # One ore at section-local index 0, then stone; unused copper palette entry.
        chunk=VanillaChunk({'sections':[{'Y':-1,'block_states':{'palette':[{'Name':'minecraft:stone'},{'Name':'minecraft:iron_ore'},{'Name':'minecraft:copper_ore'}],'data':[1]+[0]*255}}]})
        points=e.observe({(-1,-1):chunk},[-16,-1,-16,-1,-16,-1])
        self.assertEqual(points,[[-16,-16,-16,'minecraft:iron_ore']])

    def test_homogeneous_palette_and_no_fortune_multiplier(self):
        chunk=VanillaChunk({'sections':[{'Y':0,'block_states':{'palette':[{'Name':'minecraft:copper_ore'}]}}]})
        points=e.observe({(0,0):chunk},[0,1,0,1,0,1])
        self.assertEqual(len(points),8)
        self.assertEqual(e.summarize(points,[0,1,0,1,0,1])['copper']['blocks'],8)

if __name__=='__main__': unittest.main()
