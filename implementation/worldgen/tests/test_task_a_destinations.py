"""Destination objective regressions; fixture data only, never world authoring."""
import copy
import hashlib
import json
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

from test_task_a import fixture
from vanilla_search.task_a import Terrain, fit
from vanilla_search.task_a_refine import conservative_graph, PARAMETERS as A1, indices
from vanilla_search.task_a_destinations import (PARAMETERS, destinations, refit,
                                               choose_joint, distance_cost, route_options)
from fit_default_task_a_destinations import OUT, SOURCE, ROOT


class DestinationTests(unittest.TestCase):
    def test_empty_flat_terrain_is_not_a_destination(self):
        c=fixture(flat=True);t=Terrain(c);t.v['highland']=[False]*t.n
        self.assertEqual([],destinations(t,c,conservative_graph(t,A1),PARAMETERS))

    def test_anchors_are_interfaces_not_arbitrary_region_interiors(self):
        c=fixture();t=Terrain(c);records=destinations(t,c,conservative_graph(t,A1),PARAMETERS)
        self.assertTrue(records)
        for r in records:
            if r['kind']=='highland approach':
                for i in r['cells']:
                    self.assertFalse(t.v['highland'][i])
                    self.assertTrue(any(t.v['highland'][q] for q in t.neighbors(i)))
            if r['kind']=='forest entrance':
                for i in r['cells']:
                    self.assertFalse(t.v['forest'][i])
                    self.assertTrue(any(t.v['forest'][q] for q in t.neighbors(i)))

    def test_actual_villages_only_not_underground_structure_projection(self):
        c=fixture(flat=True);t=Terrain(c)
        c['actual_structures']=[{'type':'minecraft:trial_chambers','chunk':[8,8],'bounding_boxes':[[80,-30,80,110,-10,110]]}]
        self.assertFalse(any(r['kind']=='settlement approach' for r in destinations(t,c,conservative_graph(t,A1),PARAMETERS)))
        c['actual_structures'][0]['type']='minecraft:village_plains'
        self.assertTrue(any(r['kind']=='settlement approach' for r in destinations(t,c,conservative_graph(t,A1),PARAMETERS)))

    def test_joint_choice_rejects_cheapest_duplicate_set(self):
        t=Terrain(fixture(flat=True))
        def option(node,fid,kind,cost):
            return {'node':node,'feature':{'id':fid,'kind':kind},'opening':[node-t.w,node], 'cost':cost}
        first=option(10*t.w+12,'bank-a','water',0)
        duplicate=option(10*t.w+13,'bank-a','water',0)
        distinct=option(15*t.w+25,'forest-b','forest',.8)
        third=option(20*t.w+35,'coast-c','coast',.1)
        chosen=choose_joint(t,[[first],[duplicate,distinct],[third]],PARAMETERS)
        self.assertIs(chosen[1],distinct)

    def test_soft_depth_can_prefer_meaningful_destination_outside_band(self):
        t=Terrain(fixture(flat=True))
        weak={'node':500,'opening':[452,500],'feature':{'id':'interface','kind':'biome'},
              'cost':distance_cost(65,t,PARAMETERS)+.5}
        strong={'node':502,'opening':[454,502],'feature':{'id':'river','kind':'water'},
                'cost':distance_cost(90,t,PARAMETERS)}
        self.assertIs(choose_joint(t,[[weak,strong]],PARAMETERS)[0],strong)
        self.assertGreater(distance_cost(140,t,PARAMETERS),distance_cost(90,t,PARAMETERS))

    def test_route_options_end_on_feature_and_leave_unsupported_tail(self):
        c=fixture(flat=True);t=Terrain(c)
        # Explicit straight fixture corridor: homeland-facing gate then Wilderness.
        gate=8*t.w+20;outside=[z*t.w+20 for z in range(9,40)]
        route={'departure':t.point(gate)};entry=(route,[gate]+outside,outside,outside[7])
        node=19*t.w+20
        records=[{'id':'forest-boundary','kind':'forest entrance','cells':[node],
                  'strength':1.,'evidence':'test interface','weak_evidence':[]}]
        options,_=route_options(t,entry,records,conservative_graph(t,A1,{gate}),PARAMETERS)
        self.assertTrue(options)
        self.assertGreater(options[0]['depth'],t.p['starter_soft_max'])
        self.assertEqual(node,options[0]['opening'][-1])
        self.assertEqual(outside[-1],options[0]['outside'][-1])
        self.assertGreaterEqual(t.length(options[0]['tail']),32)

    def test_refit_deterministic_preserves_inputs_and_anchors(self):
        c=fixture();saved=copy.deepcopy(c);a=fit(c);b=refit(c)
        self.assertEqual(b,refit(c));self.assertEqual(saved,c)
        for key in ('homelands','playable_bounds','logical_orientation'): self.assertEqual(a[key],b[key])
        t=Terrain(c)
        for old,new in zip(a['routes'],b['routes']):
            for key in ('departure','target'): self.assertEqual(old.get(key),new.get(key))
            s=new.get('starter')
            if not s: continue
            if s.get('destination'):
                self.assertEqual(s['terminus'],s['destination']['anchor'])
                self.assertEqual(s['terminus']['sample'],s['sample_path'][-1])
                self.assertIn(s['terminus']['sample'],new['sample_path'])
                self.assertNotEqual(s['terminus']['sample'],new['sample_path'][-1])
                full=indices(t,new['sample_path']);self.assertEqual(len(full),len(set(full)))
                self.assertAlmostEqual(s['effective_blocks_from_edge'],t.length(indices(t,s['sample_path']))+t.cost(indices(t,[new['departure']['sample']])[0],indices(t,s['sample_path'])[0])/2,places=2)
                self.assertFalse(any('outside soft band' in failure for failure in b['failures']))
            else:
                self.assertIn('no_supported_destination',s['failures'])


class DestinationArtifacts(unittest.TestCase):
    def test_eight_finalist_refits_reproduce_committed_results(self):
        comparison=json.loads((OUT/'comparison.json').read_text())
        for c in comparison['candidates']:
            candidate=json.loads((SOURCE/'finalists'/str(c['seed'])/'candidate.json').read_text())
            expected=json.loads((OUT/c['fit']).read_text())
            actual=refit(candidate)
            for key in ('source_candidate_sha256','source_a1_fit_sha256'): expected.pop(key)
            self.assertEqual(expected,actual,str(c['seed']))

    def test_all_eight_outputs_and_preservation_hashes(self):
        verification=json.loads((OUT/'verification.json').read_text())
        comparison=json.loads((OUT/'comparison.json').read_text())
        self.assertEqual(8,len(comparison['candidates']))
        self.assertTrue(verification['available_clean_and_task_b_worlds_unchanged'])
        for name,expected in verification['output_sha256'].items():
            self.assertEqual(expected,hashlib.sha256((OUT/name).read_bytes()).hexdigest())
        for name,expected in verification['protected_sha256'].items():
            self.assertEqual(expected,hashlib.sha256((ROOT/name).read_bytes()).hexdigest())
        for c in comparison['candidates']:
            f=json.loads((OUT/c['fit']).read_text());ET.parse(OUT/c['svg'])
            self.assertEqual(6,len(f['routes']))
            self.assertEqual(c['seed'],f['seed'])
            self.assertIn('destination_sets',f)
        self.assertTrue((OUT/'REPORT.md').is_file())


if __name__=='__main__': unittest.main()
