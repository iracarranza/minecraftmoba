"""Generic semantic regressions required by spec3, plus completed-run integrity."""
import copy
import hashlib
import json
import math
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

from test_task_a import fixture
from vanilla_search.task_a import Terrain, fit
from vanilla_search.task_a_refine import (
    PARAMETERS, refine, conservative_graph, formations, route_differentiation,
    homeland_equivalence, connection_classification, central_area, operational_depth,
)
from vanilla_search.task_a_review import readiness
from fit_default_task_a import run, OUTPUT


def route(rid, path, terminus=3):
    return {'id':rid,'team':rid.split('-')[0],'sample_path':path,
            'departure':{'sample':path[0]},
            'starter':{'sample_path':path[:terminus+1],'terminus':{'sample':path[terminus]}}}


class RefinementTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.c=fixture(); cls.a0=fit(cls.c); cls.result=refine(cls.c,cls.a0)

    def test_determinism_and_preserved_geometry(self):
        before=copy.deepcopy(self.a0)
        self.assertEqual(self.result,refine(self.c,self.a0))
        self.assertEqual(before,self.a0)
        for field in ('playable_bounds','logical_orientation','homelands'):
            self.assertEqual(self.a0[field],self.result[field])
        for old,new in zip(self.a0['routes'],self.result['routes']):
            self.assertEqual(old['sample_path'],new['sample_path'])
            self.assertEqual(old['starter']['terminus'],new['starter']['terminus'])

    def test_separated_termini_can_collapse_into_one_branch(self):
        t=Terrain(fixture(flat=True))
        a=route('north-1',[[10,z] for z in range(5,41)],7)
        b=route('north-2',[[14,z] for z in range(5,13)]+[[x,12] for x in range(13,9,-1)]+[[10,z] for z in range(13,41)],7)
        pair=route_differentiation(t,[a,b],{},PARAMETERS)['north']['pairs'][0]
        self.assertEqual(32,pair['terminus_separation_blocks'])
        self.assertTrue(pair['functional_branch_collapse'])

    def test_distinct_wilderness_branches_not_collapsed(self):
        t=Terrain(fixture(flat=True))
        routes=[route(f'north-{k}',[[x,z] for z in range(5,41)]) for k,x in enumerate((10,24,38),1)]
        result=route_differentiation(t,routes,{},PARAMETERS)['north']
        self.assertTrue(result['three_meaningful_choices_supported'])
        self.assertTrue(all(not p['functional_branch_collapse'] for p in result['pairs']))

    def test_incidental_connection_is_not_shortcut_even_with_saving(self):
        self.assertEqual('incidental_traversability',connection_classification(80,80,80,400,1,False,PARAMETERS))
        self.assertEqual('useful_lateral_connection',connection_classification(80,80,80,90,1,True,PARAMETERS))
        self.assertEqual('strategic_shortcut',connection_classification(80,80,80,400,1,True,PARAMETERS))
        self.assertEqual('strategic_shortcut',connection_classification(80,80,80,None,1,True,PARAMETERS))
        self.assertEqual('incidental_traversability',connection_classification(80,80,80,400,.1,True,PARAMETERS))

    def test_moderate_disparity_not_definitive_competitive_failure(self):
        homes=copy.deepcopy(self.a0['homelands'])
        homes['north']['quality']=.9; homes['south']['quality']=.65
        report=homeland_equivalence(homes,PARAMETERS)
        self.assertEqual('Moderate',report['classification'])
        self.assertIn('component_disparities',report)
        self.assertIsNone(report['resource_equivalence'])
        homes['south']['quality']=.2
        self.assertEqual('Severe',homeland_equivalence(homes,PARAMETERS)['classification'])
        homes['south']['diagnostics']['usable_fraction']=.1
        self.assertFalse(homeland_equivalence(homes,PARAMETERS)['independently_usable']['south'])

    def test_disparity_threshold_boundaries(self):
        homes=copy.deepcopy(self.a0['homelands']); homes['north']['quality']=0
        for value,label in ((.1,'Low'),(.3,'Moderate'),(.5,'High'),(.501,'Severe')):
            homes['south']['quality']=value
            self.assertEqual(label,homeland_equivalence(homes,PARAMETERS)['classification'])

    def test_shared_interior_is_area_not_six_spoke_reward(self):
        t=Terrain(fixture(flat=True)); graph=conservative_graph(t,PARAMETERS)
        routes=[route('north-1',[[18,z] for z in range(8,49)]),route('south-1',[[30,z] for z in range(55,15,-1)])]
        central=central_area(t,routes,graph,[],PARAMETERS)
        self.assertTrue(central['north_south_interaction_supported'])
        self.assertTrue(central['west_east_crossing_supported'])
        self.assertFalse(central['overcentralization_warning'])
        self.assertGreater(central['shared_traversable_area_blocks2'],1000)
        self.assertEqual(2,len(central['routes_approaching']))

    def test_under_convergence_and_overcentralization_regressions(self):
        t=Terrain(fixture(flat=True)); graph=conservative_graph(t,PARAMETERS)
        north=route('north-1',[[24,z] for z in range(8,49)])
        self.assertFalse(central_area(t,[north],graph,[],PARAMETERS)['north_south_interaction_supported'])
        six=[route(f'{team}-{k}',[[24,z] for z in range(8,49)]) for team in ('north','south') for k in (1,2,3)]
        self.assertTrue(central_area(t,six,graph,[],PARAMETERS)['overcentralization_warning'])

    def test_traversable_band_is_not_geographic_band(self):
        t=Terrain(fixture(flat=True)); graph=conservative_graph(t,PARAMETERS)
        result=operational_depth(t,self.a0['homelands'],graph,[],PARAMETERS)
        self.assertTrue(result['north']['bands']['secondary']['traversable_depth_supported'])
        self.assertFalse(result['north']['bands']['secondary']['geographic_depth_supported'])

    def test_ordinary_open_country_not_a_distinguished_connection(self):
        t=Terrain(fixture(flat=True)); graph=conservative_graph(t,PARAMETERS)
        regions,_=formations(t,graph,PARAMETERS)
        opens=[r for r in regions if r['kind']=='open region']
        self.assertTrue(opens)
        self.assertTrue(all(not r['connection_formation'] and not r['distinct_geography'] for r in opens))

    def test_conservative_graph_does_not_cut_water_corners(self):
        t=Terrain(fixture(flat=True)); t.v['water'][1]=True
        graph=conservative_graph(t,PARAMETERS)
        self.assertNotIn(t.w+1,[q for q,_ in graph[0]])
        self.assertNotIn(1,graph)

    def test_highland_nonbuildability_does_not_forbid_travel(self):
        t=Terrain(fixture(flat=True)); t.v['buildable']=[False]*t.n
        graph=conservative_graph(t,PARAMETERS)
        self.assertEqual(t.n,len(graph))
        self.assertIn(1,[q for q,_ in graph[0]])

    def test_full_starter_construction_burden_is_separate(self):
        c=copy.deepcopy(self.c); baseline=copy.deepcopy(self.a0)
        from successor.grid import unrle,rle
        heights=unrle(c['feature_grid']['height_rle'])
        x,z=baseline['routes'][0]['starter']['supported_sample_path'][0]
        heights[z*48+x]+=30; c['feature_grid']['height_rle']=rle(heights)
        result=refine(c,baseline)
        self.assertEqual(0,result['routes'][0]['starter']['refined_feasibility']['score'])
        self.assertEqual(baseline['routes'][0]['corridor_quality_score'],result['routes'][0]['corridor_quality_score'])
        self.assertTrue(any('forced Starter' in f for f in result['failures']))

    def test_human_selection_gate_cannot_be_fabricated(self):
        gate=readiness([self.result])
        self.assertFalse(gate['task_b_ready']); self.assertEqual([],gate['selected_candidates'])
        self.assertIsNone(gate['candidates'][0]['checks']['human_review_selects_candidate']['human_validation'])
        with self.assertRaises(ValueError): run(OUTPUT,refined=True)


class CompletedRefinementTests(unittest.TestCase):
    def test_eight_outputs_provenance_and_gate(self):
        root=Path(__file__).resolve().parents[1]/'results'
        out=root/'default_task_a_1_2026-09-10'
        summary=json.loads((out/'comparison.json').read_text())
        self.assertEqual([930005557,930006815,930007222,930010639,930012642,930015734,930016664,930019528],[c['seed'] for c in summary['candidates']])
        for c in summary['candidates']:
            f=json.loads((out/c['fit']).read_text()); ET.parse(out/c['svg'])
            self.assertEqual(c['network_metrics'],f['network_metrics'])
            self.assertEqual(6,len(f['routes']))
            self.assertTrue(all(r.get('starter') for r in f['routes']))
            self.assertTrue(all(link['classification']!='incidental_traversability' for link in f['cross_connections']))
        gate=json.loads((out/'readiness_gate.json').read_text())
        self.assertFalse(gate['task_b_ready']); self.assertEqual(8,len(gate['candidates']))
        verification=json.loads((out/'verification.json').read_text())
        for flag in ('a0_artifacts_unchanged','available_worlds_unchanged','candidate_inputs_unchanged'):
            self.assertTrue(verification[flag])
        for name,digest in verification['output_sha256'].items():
            self.assertEqual(digest,hashlib.sha256((out/name).read_bytes()).hexdigest())


if __name__=='__main__': unittest.main()
