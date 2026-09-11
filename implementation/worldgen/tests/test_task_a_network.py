"""Destination-first analytical regressions: no historical Route dependence."""
import copy
import hashlib
import json
from pathlib import Path
import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET

from test_task_a import fixture
from successor.grid import rle
from vanilla_search.task_a import Terrain,fit_homeland
from vanilla_search.task_a_network import (analyze,terrain_graph,geographic_components,outward_field,
    destination_pool,home_cells,continuation,meeting,select_joint,connective_growth,PARAMETERS,MODES)
from fit_default_task_a_network import OUT
from fit_default_task_a import ROOT,SOURCE


def homes(t):return {team:fit_homeland(t,team)[0] for team in ('north','south')}


class DestinationFirstTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.c=fixture();cls.h=homes(Terrain(cls.c));cls.result=analyze(cls.c,cls.h)

    def test_deleted_scrambled_historical_geometry_cannot_change_analysis(self):
        c=copy.deepcopy(self.c)
        c['routes']=[{'departure':[-999,999],'target':[100000,0],'starter':{'terminus':None,'sample_path':[[0,0]]}}]
        c['greybox']['routes']={'anything':'invalid and deliberately unusable'}
        c['historical_starter_routes']='not even a route data structure'
        with (patch('vanilla_search.task_a.fit_routes',side_effect=AssertionError('Historical fitter invoked')),
              patch('vanilla_search.task_a_destinations.route_options',side_effect=AssertionError('Old spine solver invoked'))):
            actual=analyze(c,self.h)
        self.assertEqual(self.result,actual)
        c.pop('routes');c['greybox'].pop('routes');c.pop('historical_starter_routes')
        self.assertEqual(self.result,analyze(c,self.h))

    def test_cost_increase_does_not_itself_create_homeland_depth(self):
        t=Terrain(fixture(flat=True));h=homes(t);excluded=set.union(*(home_cells(t,x) for x in h.values()))
        g,d,*_=terrain_graph(t,excluded,PARAMETERS);a=outward_field(t,'north',h,g,d)
        # Add burden only, without altering topology, positions or source geometry.
        t.adj=[[(q,c*10) for q,c in edges] for edges in t.adj]
        g,d,*_=terrain_graph(t,excluded,PARAMETERS);b=outward_field(t,'north',h,g,d)
        self.assertEqual(a['depth'],b['depth'])
        node=max(a['depth'],key=a['depth'].get)
        self.assertAlmostEqual(10*a['travel']['major'][node],b['travel']['major'][node])

    def test_all_valid_outward_faces_are_sources_not_opponent_half_plane(self):
        t=Terrain(fixture(flat=True));h=homes(t);excluded=set.union(*(home_cells(t,x) for x in h.values()))
        g,d,*_=terrain_graph(t,excluded,PARAMETERS);field=outward_field(t,'north',h,g,d)
        x0,x1,z0,z1=h['north']['logical_footprint_samples'];points=[t.xy(i) for i in field['sources']]
        self.assertTrue(any(x<x0 for x,z in points));self.assertTrue(any(x>x1 for x,z in points))
        self.assertTrue(any(z<z0 for x,z in points));self.assertTrue(any(z>z1 for x,z in points))

    def test_water_system_unifies_banks_and_marks_crossing_conditional(self):
        c=fixture(flat=True);t=Terrain(c)
        for z in range(t.h):t.v['water'][z*t.w+25]=True
        g,d,e,systems,water_id=terrain_graph(t,set(),PARAMETERS)
        self.assertEqual(1,len(systems));self.assertTrue(systems[0]['crossings'])
        a=20*t.w+24;b=20*t.w+26
        self.assertNotIn(b,dict(g['natural'][a]));self.assertIn(b,dict(g['modest'][a]))
        self.assertTrue(all(c['conditional'] for c in systems[0]['crossings']))
        rr,m,links,_=geographic_components(t,g['major'],e,PARAMETERS)
        pool=destination_pool(t,c,g['major'],rr,m,links,systems,water_id)
        water=[r for r in pool if r['kind']=='inland-water relationship']
        self.assertEqual(1,len(water));self.assertIn(a,water[0]['cells']);self.assertIn(b,water[0]['cells'])
        self.assertTrue(water[0]['water_semantics']['barrier'])

    def test_nearby_cliff_adds_cost_not_geographic_depth(self):
        c=fixture(flat=True);t=Terrain(c);h=homes(t);excluded=set.union(*(home_cells(t,x) for x in h.values()))
        g,d,*_=terrain_graph(t,excluded,PARAMETERS);a=outward_field(t,'north',h,g,d)
        c['feature_grid']['height_rle']=rle([80 if i//t.w==30 else 64 for i in range(t.n)])
        rough=Terrain(c);g,d,*_=terrain_graph(rough,excluded,PARAMETERS);b=outward_field(rough,'north',h,g,d)
        self.assertEqual(a['depth'],b['depth'])
        node=30*t.w+20
        self.assertGreater(b['travel']['major'][node],a['travel']['major'][node])

    def test_local_biome_speck_is_absorbed(self):
        t=Terrain(fixture(flat=True));node=25*t.w+25;t.v['biome'][node]='minecraft:forest';t.v['forest'][node]=True
        g,d,e,*_=terrain_graph(t,set(),PARAMETERS);regions,m,_,stats=geographic_components(t,g['major'],e,PARAMETERS)
        self.assertEqual(m[node],m[node+1]);self.assertGreater(stats['absorbed_noise_components'],0)

    def test_continuation_branches_and_backtracking_are_measured(self):
        graph={0:[(1,1)],1:[(0,1),(2,1),(3,1)],2:[(1,1)],3:[(1,1)]}
        depth={0:0,1:1,2:2,3:3};m={0:'a',1:'b',2:'c',3:'d'}
        regions=[{'id':r,'type':r,'cells':[i]} for i,r in m.items()]
        c=continuation(graph,1,depth,regions,m,set(graph))
        self.assertEqual(2,c['meaningful_branch_count']);self.assertEqual('branching',c['continuation_class'])
        self.assertEqual(['c','d'],c['deeper_regions_reached'])
        c=continuation(graph,2,depth,regions,m,set(graph))
        self.assertEqual('dead_end',c['continuation_class']);self.assertTrue(c['backtracking_dependency'])
        self.assertIn('d',c['additional_deeper_regions_requiring_backtracking'])

    def test_same_region_does_not_fake_same_sample_convergence(self):
        self.assertIsNone(meeting({1:20},{2:20},{1:'large-plain',2:'large-plain'}))
        found=meeting({1:20,3:70},{2:25,3:80},{1:'a',2:'a',3:'b'})
        self.assertEqual((70,80),(found['depth_a'],found['depth_b']))

    def test_connective_geography_grows_at_observed_depths_without_center(self):
        g=connective_growth([{1:15,2:80}],[{1:25,2:90}],{1:'shore',2:'upland'})
        self.assertEqual([25,90],[p['homeland_depth_ceiling'] for p in g['observed_depth_profile']])
        self.assertEqual([1,2],[p['regions_with_opposing_overlap'] for p in g['observed_depth_profile']])

    def test_joint_selection_can_leave_third_choice_unresolved(self):
        t=Terrain(fixture(flat=True))
        def option(i,group):return {'id':str(i),'eligible':True,'opportunity_group':group,'utility':2,
            '_node':i,'_gate':i,'_path':[i],'kind':'water','opening_regions':[str(i)]}
        choices=select_joint(t,[option(100,'same-water'),option(105,'same-water')],PARAMETERS)
        self.assertEqual(1,len(choices))
        self.assertEqual([],select_joint(t,[],PARAMETERS))

    def test_final_corridors_preserve_selected_destinations_not_old_targets(self):
        for team,selected in self.result['selected_handoffs'].items():
            for r,c in zip(selected,self.result['starter_corridors'][team]):
                self.assertEqual(r['id'],c['destination'])
                if 'terminus' in c:self.assertEqual(r['anchor'],c['terminus'])
                self.assertNotIn('target',c)
            groups=[r['opportunity_group'] for r in selected]
            self.assertEqual(len(groups),len(set(groups)));self.assertLessEqual(len(selected),3)


class DestinationFirstArtifacts(unittest.TestCase):
    def test_primary_real_fit_reproduces_without_historical_routes(self):
        expected=json.loads((OUT/'930010639/fit.json').read_text())
        c=json.loads((SOURCE/'finalists/930010639/candidate.json').read_text())
        c['routes']=[];c['historical_starter_termini']='deliberately absent'
        expected.pop('provenance');expected.pop('historical_comparison')
        self.assertEqual(expected,analyze(c,expected['homelands']))

    def test_all_eight_outputs_and_preservation(self):
        v=json.loads((OUT/'verification.json').read_text());comparison=json.loads((OUT/'comparison.json').read_text())
        self.assertEqual(8,len(comparison['candidates']));self.assertTrue(v['worlds_unchanged'])
        for path,sha in v['output_sha256'].items():self.assertEqual(sha,hashlib.sha256((OUT/path).read_bytes()).hexdigest())
        for path,sha in v['protected_sha256'].items():self.assertEqual(sha,hashlib.sha256((ROOT/path).read_bytes()).hexdigest())
        for c in comparison['candidates']:
            f=json.loads((OUT/c['fit']).read_text());ET.parse(OUT/c['svg'])
            for team in ('north','south'):
                self.assertIn('homeland_depth_rle',f['homeland_fields'][team])
                self.assertEqual(len(f['destination_catalog']),len(f['destination_pools'][team]))
                for r in f['destination_pools'][team]:
                    self.assertTrue(r.get('continuation') or r.get('unavailable'))
                    for c in (r.get('continuation') or {}).values():
                        self.assertEqual(c['deeper_regions_reached_count'],len(f['continuation_region_sets'][c['deeper_regions_reached_ref']]))
                for r in f['selected_handoffs'][team]:
                    self.assertNotEqual('major',r['starter_dependency'])
                    for mode in MODES:self.assertIn('backtracking_dependency',r['continuation'][mode])
            self.assertEqual(set(MODES),set(f['topology']['by_dependency']))
            for mode in MODES:
                for matrix in f['topology']['by_dependency'][mode]['same_team_lateral_matrices'].values():
                    for i,row in enumerate(matrix):
                        for j,m in enumerate(row):
                            if m:self.assertEqual(m['connection_homeland_depth'],matrix[j][i]['connection_homeland_depth'])


if __name__=='__main__':unittest.main()
