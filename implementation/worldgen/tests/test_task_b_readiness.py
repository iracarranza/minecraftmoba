"""Selection and physical readiness are distinct spec3 gates."""
import copy
import json
from pathlib import Path
import sys
import unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from vanilla_search.task_a_review import selected_readiness
from audit_task_b_readiness import DECISION, REFINED_OUTPUT


class TaskBReadinessTests(unittest.TestCase):
    def setUp(self):
        self.decision=json.loads(DECISION.read_text())
        self.fits=[json.loads((REFINED_OUTPUT/str(c['seed'])/'fit.json').read_text()) for c in self.decision['selected_candidates']]

    def test_selection_satisfied_without_waiving_other_gate_criteria(self):
        before=copy.deepcopy(self.fits)
        gate=selected_readiness(self.fits,self.decision)
        self.assertEqual(self.decision['selected_candidates'],gate['selected_candidates'])
        self.assertFalse(gate['task_b_ready']); self.assertFalse(gate['skeleton_frozen'])
        for row in gate['candidates']:
            self.assertTrue(row['checks']['human_review_selects_candidate']['human_validation'])
            self.assertNotIn('human_review_selects_candidate',row['pending_human_checks'])
            self.assertIn('major_lateral_connections_plausible',row['unresolved_gate_checks'])
            self.assertIsNone(row['physical_failure'])
        self.assertEqual(before,self.fits)

    def test_wrong_or_duplicate_selection_rejected(self):
        with self.assertRaises(ValueError): selected_readiness(self.fits[:1],self.decision)
        duplicate=copy.deepcopy(self.decision)
        duplicate['selected_candidates'][1]=duplicate['selected_candidates'][0]
        with self.assertRaises(ValueError): selected_readiness(self.fits,duplicate)


if __name__=='__main__': unittest.main()
