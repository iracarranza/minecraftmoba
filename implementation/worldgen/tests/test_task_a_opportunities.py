"""Spec 4 semantics and immutable shadow integration, not new fitting rules."""
import copy
import hashlib
import json
import unittest
from unittest.mock import patch

from test_task_a import fixture
from test_task_a_network import homes
from successor.grid import rle
from vanilla_search.task_a import Terrain
from vanilla_search.task_a_network import analyze
from vanilla_search.task_a_opportunities import interpret,analyze_shadow,strip_shadow,ShadowTerrain,rejected_sample
from vanilla_search.task_a_opportunity_report import selected
from fit_default_task_a_opportunities import OUT,BASELINE,ROOT,SOURCE,SEEDS


def facts(**kwargs):return {'handoff_usable':True,'local_viable':True,'observed':[],'potential':[],**kwargs}
def water(**kwargs):return {'interface':True,'corridor':False,'network':True,'distinct_access':True,**kwargs}
def gateway(**kwargs):return {'threshold':True,'persistent':True,'commitment':True,'distinct_from_origin':True,**kwargs}


class OpportunitySemantics(unittest.TestCase):
    def test_coast_no_payoff_never_naturally_complete(self):
        result=interpret(facts(water=water()))
        self.assertNotEqual('complete',result['completion_state'])
        self.assertEqual('weak',result['supported_functions']['transition']['state'])
        self.assertEqual('supported',next(a for a in result['affordances'] if a['type']=='network_access')['evidence_state'])

    def test_large_empty_water_without_distinct_access_is_incomplete(self):
        r=interpret(facts(water=water(distinct_access=False),potential=[{'id':'invented-poi'}]))
        self.assertEqual('incomplete',r['completion_state'])
        self.assertFalse(r['completion_evidence']['authored_completion_allowed'])

    def test_coast_observed_downstream_payoff(self):
        r=interpret(facts(water=water(),observed=[{'id':'remote-village'}]))
        self.assertEqual('complete',r['completion_state'])
        self.assertEqual('supported',r['supported_functions']['transition']['state'])

    def test_structurally_supported_maritime_case(self):
        r=interpret(facts(water=water(),potential=[{'id':'distinct-remote-shore'}]))
        self.assertEqual('structurally_supported',r['completion_state'])
        self.assertTrue(r['completion_evidence']['authored_completion_allowed'])

    def test_forest_strip_does_not_become_gateway_from_generic_reach(self):
        r=interpret(facts(gateway=gateway(persistent=False),observed=[{'id':'distant-unrelated-forest'}]))
        self.assertEqual('weak',r['supported_functions']['gateway']['state'])
        self.assertEqual('incomplete',r['completion_state'])

    def test_persistent_directed_forest_gateway(self):
        r=interpret(facts(gateway=gateway(),observed=[{'id':'useful-entered-system'}]))
        self.assertEqual('supported',r['supported_functions']['gateway']['state'])
        self.assertEqual('complete',r['completion_state'])

    def test_same_system_already_at_home_does_not_prove_access_privilege(self):
        r=interpret(facts(gateway=gateway(distinct_from_origin=False)))
        self.assertEqual('weak',r['supported_functions']['gateway']['state'])

    def test_false_junction_equivalent_or_reconverging_branches(self):
        for branches,merge in (([{'contexts':['plain']},{'contexts':['plain']}],False),
            ([{'contexts':['highland']},{'contexts':['river']}],True)):
            r=interpret(facts(branches=branches,rapid_reconvergence=merge))
            self.assertEqual('weak',r['supported_functions']['junction']['state'])

    def test_real_junction_materially_different_relationships(self):
        r=interpret(facts(branches=[{'contexts':['highland']},{'contexts':['river corridor']}]))
        self.assertEqual('supported',r['supported_functions']['junction']['state'])

    def test_one_supported_branch_plus_unexplained_land_is_not_a_junction(self):
        r=interpret(facts(branches=[{'contexts':['highland']},{'contexts':[]}]))
        self.assertEqual('weak',r['supported_functions']['junction']['state'])

    def test_village_arrival_destination_does_not_require_onward_branches(self):
        r=interpret(facts(direct_interaction=True,local_viable=False,observed=[{'id':'village'}]))
        self.assertEqual('strong',r['supported_functions']['destination']['state'])
        self.assertEqual('complete',r['completion_state'])

    def test_hypothetical_authored_poi_cannot_rescue_generic_terrain(self):
        r=interpret(facts(potential=[{'id':'hypothetical-worksite'}]))
        self.assertEqual('incomplete',r['completion_state'])
        self.assertFalse(r['completion_evidence']['authored_completion_allowed'])

    def test_directed_gets_no_branching_penalty(self):
        a=interpret(facts(gateway=gateway(),observed=[{'id':'system'}]))
        b=interpret(facts(gateway=gateway(),observed=[{'id':'system'}],branches=[{'contexts':['a']},{'contexts':['b']}]))
        self.assertEqual(a['completion_state'],b['completion_state'])
        self.assertEqual(a['supported_functions']['gateway'],b['supported_functions']['gateway'])


class OpportunityIntegration(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.c=fixture();cls.fit=analyze(cls.c,homes(Terrain(cls.c)))
        cls.shadow=analyze_shadow(cls.c,cls.fit)

    def test_additive_only_and_input_immutability(self):
        self.assertEqual(self.fit,strip_shadow(self.shadow))
        for r in selected(self.shadow):
            self.assertEqual([],r['payoff']['authored'])
            self.assertGreaterEqual(r['handoff']['arrival_envelope']['sample_count'],1)
            self.assertEqual(set(('gateway','junction','destination','transition')),set(r['supported_functions']))

    def test_historical_routes_scrambled_deleted_and_selector_never_called(self):
        c=copy.deepcopy(self.c);f=copy.deepcopy(self.fit)
        c['routes']='invalid';c['greybox']['routes']=[{'sample_path':[[99999,-99999]]}]
        f['historical_comparison']={'routes':'deliberately corrupt'}
        with (patch('vanilla_search.task_a_network.analyze',side_effect=AssertionError('active fitter called')),
              patch('vanilla_search.task_a_network.select_joint',side_effect=AssertionError('selector called'))):
            a=analyze_shadow(c,f)
            c.pop('routes');c['greybox'].pop('routes');f.pop('historical_comparison');b=analyze_shadow(c,f)
        for result in (a,b):
            self.assertEqual(self.shadow['opportunity_shadow'],result['opportunity_shadow'])
            self.assertEqual(selected(self.shadow),selected(result))

    def test_unselected_strata_are_bounded_and_not_reselection(self):
        for team in ('north','south'):
            sample=rejected_sample(self.fit,team)
            self.assertLessEqual(len(sample),12)
            self.assertFalse({e['record']['id'] for e in sample}&{r['id'] for r in self.fit['selected_handoffs'][team]})

    def test_water_seabed_not_used_as_embarkation_cliff(self):
        c=fixture(flat=True);g=c['feature_grid'];w=g['width'];h=g['height']
        wet=[i%w>=w-6 for i in range(w*h)]
        g['actual_surface_water_rle']=rle(wet);g['surface_block_rle']=rle(['minecraft:water' if v else 'minecraft:grass_block' for v in wet])
        g['height_rle']=rle([10 if v else 64 for v in wet]);g['biome_rle']=rle(['minecraft:ocean' if v else 'minecraft:plains' for v in wet])
        t=Terrain(c);fit=analyze(c,homes(t));model=ShadowTerrain(c,fit)
        node=30*w+w-7;r={'anchor':t.point(node),'homeland_depth':model.fields['north']['depth'][node]}
        ws,_,_=model.water_reach('north',r,[node])
        self.assertTrue(ws);self.assertTrue(ws[0]['interface'])
        self.assertIn('unverified',ws[0]['interface_confidence'])


class OpportunityArtifacts(unittest.TestCase):
    def test_real_primary_shadow_reproduces_and_records_blocked_mask_entry(self):
        c=json.loads((SOURCE/'finalists/930010639/candidate.json').read_text())
        baseline=json.loads((BASELINE/'930010639/fit.json').read_text())
        expected=json.loads((OUT/'930010639/fit.json').read_text())
        self.assertEqual(expected,analyze_shadow(c,baseline))
        claims=expected['selected_handoffs']['north'][0]['opportunity_relationship']['reach']['geographic_persistence']
        self.assertTrue(any(c['raw_neighbor_feature_present'] and not c['immediate_modest_entry'] for c in claims))

    def test_all_eight_frozen_fields_prior_outputs_and_source_hashes(self):
        v=json.loads((OUT/'verification.json').read_text());manifest=json.loads((OUT/'comparison.json').read_text())
        self.assertEqual(list(SEEDS),v['exact_eight_finalists']);self.assertEqual(8,len(manifest['candidates']))
        self.assertTrue(v['existing_world_files_unchanged']);self.assertTrue(v['active_fit_fields_unchanged'])
        for mapping in ('protected_sha256','implementation_sha256'):
            for path,sha in v[mapping].items():self.assertEqual(sha,hashlib.sha256((ROOT/path).read_bytes()).hexdigest(),path)
        for path,sha in v['output_sha256'].items():self.assertEqual(sha,hashlib.sha256((OUT/path).read_bytes()).hexdigest(),path)
        for seed in SEEDS:
            baseline=json.loads((BASELINE/str(seed)/'fit.json').read_text());f=json.loads((OUT/str(seed)/'fit.json').read_text())
            self.assertEqual(baseline,strip_shadow(f))
            for r in selected(f)+f['opportunity_shadow']['rejected_sample']:
                self.assertEqual([],r['payoff']['authored'])
                if r['completion_state']=='incomplete':self.assertEqual([],r['payoff']['potential'])
                self.assertFalse(r['stopping_principle']['automatic_length_change'])


if __name__=='__main__':unittest.main()
