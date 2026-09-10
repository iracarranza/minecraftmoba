"""Task B block-writing scope, geometry, preservation and physical readback."""
import json
from pathlib import Path
import sys
import unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from vanilla_search.task_b import dense_path,state_string,profile,local_bypass,BlockWorld


class FlatBlocks:
    def column(self,x,z):
        y=56 if (x,z)==(2,0) else 64
        return {'x':x,'z':z,'ground_y':y,'surface_y':y,'water_y':None,'water_depth':0,'substrate':'minecraft:stone','overhead':[]}
    def state(self,x,y,z): return {'Name':'minecraft:air' if y>self.column(x,z)['ground_y'] else 'minecraft:stone'}


class TaskBTests(unittest.TestCase):
    def test_dense_path_is_cardinal_and_preserves_anchors(self):
        path=dense_path([[0,64,0],[8,66,8],[-8,64,8]])
        self.assertEqual((0,0),path[0]); self.assertEqual((-8,8),path[-1]); self.assertIn((8,8),path)
        self.assertTrue(all(abs(x-a)+abs(z-b)==1 for (x,z),(a,b) in zip(path,path[1:])))

    def test_state_command_preserves_properties(self):
        self.assertEqual('minecraft:oak_log[axis=x]',state_string({'Name':'minecraft:oak_log','Properties':{'axis':'x'}}))

    def test_block_resolution_exposes_between_sample_pit(self):
        result=profile(FlatBlocks(),[(x,0) for x in range(5)])
        self.assertEqual(8,result['maximum_adjacent_rise'])
        self.assertEqual(2,len(result['adjacent_rises_over_one']))

    def test_local_bypass_is_evidence_not_applied_revision(self):
        result=local_bypass(FlatBlocks(),[(x,0) for x in range(5)])
        self.assertTrue(result['found']); self.assertFalse(result['applied'])
        self.assertNotIn([2,0],result['path_xz'])
        self.assertLessEqual(result['path_steps'],6)

    def test_missing_chunks_are_not_invented_air(self):
        reader=object.__new__(BlockWorld); reader.chunks={}
        with self.assertRaises(ValueError): reader.state(0,64,0)

    def test_both_completed_builds_have_exact_readback_and_no_freeze(self):
        root=Path(__file__).resolve().parents[1]/'results/default_task_b_2026-09-10'
        summary=json.loads((root/'summary.json').read_text())
        self.assertTrue(summary['clean_worlds_unchanged'])
        self.assertIsNone(summary['frozen_candidate']); self.assertFalse(summary['b2_performed'])
        self.assertEqual([930010639,930012642],[r['seed'] for r in summary['candidates']])
        for seed in (930010639,930012642):
            p=root/str(seed)
            plan=json.loads((p/'build_plan.json').read_text()); v=json.loads((p/'validation.json').read_text())
            server=json.loads((p/'server_validation.json').read_text())
            self.assertEqual([],v['readback_mismatches']); self.assertEqual([],server['errors'])
            self.assertEqual(0,server['exit_code']); self.assertEqual(6,len(v['routes']))
            self.assertFalse(plan['macro_terrain_edits']); self.assertEqual([],plan['b2_corrections'])
            self.assertIsNone(v['candidate_pass']); self.assertFalse(v['skeleton_frozen'])
            self.assertLessEqual(len(plan['changes']),plan['parameters']['maximum_edits'])
            self.assertTrue(all(h['two_air_blocks'] for h in v['fountains'].values()))
            self.assertTrue(all(h['support']=='minecraft:sea_lantern' for h in v['fountains'].values()))
            self.assertTrue((p/'apply.mcfunction').is_file()); self.assertTrue((p/'undo.mcfunction').is_file())
            audit=json.loads((p/'preservation_audit.json').read_text())
            self.assertEqual([],audit['new_chunks']); self.assertEqual([],audit['removed_chunks'])
            self.assertTrue(audit['transient_server_simulation_changes'])
            self.assertIn('authoring_discrepancies',audit)


if __name__=='__main__': unittest.main()
