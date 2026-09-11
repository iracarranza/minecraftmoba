"""Local geography must not inherit distant network topology or old Routes."""
import copy
import json
import unittest

from vanilla_search.task_a_local import local_continuation, horizon, bounded_reach
from vanilla_search.task_a_network import select_joint, PARAMETERS, MODES
from fit_default_task_a_network import OUT, BASELINE


class Geometry:
    s=8
    p={'departure_separation_samples':3}
    def __init__(self):
        self.points={0:(0,0)};self.depth={0:44.};self.graph={0:[]}
    def xy(self,i):return self.points[i]
    def add(self,xy,depth,parent,rank=0):
        i=len(self.points);self.points[i]=xy;self.depth[i]=depth;self.graph[i]=[]
        self.link(parent,i);return i
    def link(self,a,b):
        self.graph[a].append((b,8.));self.graph[b].append((a,8.))
    def arm(self,dx,dz,length=7):
        prev=0;nodes=[]
        for k in range(1,length+1):
            prev=self.add((dx*k,dz*k),44.+8*k,prev);nodes.append(prev)
        return nodes
    def analyze(self,graphs=None,membership=None):
        membership=membership or {i:'plain' for i in self.graph}
        regions=[{'id':r,'type':r,'cells':[i for i,v in membership.items() if v==r]} for r in sorted(set(membership.values()))]
        return local_continuation(self,graphs or {m:self.graph for m in MODES},0,self.depth,regions,membership)


class LocalContinuationTests(unittest.TestCase):
    def test_horizon_reuses_next_existing_operational_layer(self):
        self.assertEqual(110,horizon(44,8)['analysis_depth_limit'])
        self.assertEqual(70,horizon(12,8)['analysis_depth_limit'])
        self.assertEqual(150,horizon(70,8)['analysis_depth_limit'])
        self.assertTrue(horizon(360,8)['open_ended_guard_used'])

    def test_enclosed_handoff_with_distant_network_stays_dead_end(self):
        g=Geometry();a=g.add((1,0),36,0);b=g.add((2,0),28,a)
        for k in range(3,14):b=g.add((k,0),28+(k-2)*8,b)
        c=g.analyze()
        self.assertEqual('dead_end',c['continuation_class']);self.assertEqual(0,c['meaningful_branch_count'])
        self.assertTrue(c['backtracking_dependency'])

    def test_one_direction_corridor_is_directed_even_with_later_network(self):
        g=Geometry();arm=g.arm(1,0,15)
        for sign in (-1,1):
            prev=arm[10]
            for k in range(1,8):prev=g.add((11,sign*k),g.depth[arm[10]]+8*k,prev)
        c=g.analyze();self.assertEqual('directed',c['continuation_class'])
        self.assertLessEqual(c['max_depth_reached'],c['analysis_depth_limit'])

    def test_two_persistent_outward_choices_are_branching(self):
        g=Geometry();g.arm(1,0);g.arm(-1,0)
        self.assertEqual('branching',g.analyze()['continuation_class'])

    def test_three_persistent_outward_choices_are_junction(self):
        g=Geometry();g.arm(1,0);g.arm(-1,0);g.arm(0,1)
        self.assertEqual(3,g.analyze()['meaningful_branch_count'])
        self.assertEqual('junction',g.analyze()['continuation_class'])

    def test_tiny_pockets_do_not_count(self):
        g=Geometry();g.arm(1,0);g.arm(0,1,3)
        c=g.analyze();self.assertEqual('directed',c['continuation_class'])
        self.assertTrue(c['discarded_pockets'])

    def test_immediate_reconvergence_merges_branches(self):
        g=Geometry();a=g.arm(1,0);b=g.arm(0,1)
        self.assertEqual('branching',g.analyze()['continuation_class'])
        g.link(a[2],b[2])
        self.assertEqual('directed',g.analyze()['continuation_class'])

    def test_distant_branching_does_not_inflate_local_count(self):
        g=Geometry();arm=g.arm(1,0,20);before=g.analyze()
        for k in range(1,10):g.add((15,k),164+k*8,arm[14])
        self.assertEqual(before,g.analyze())

    def test_repeated_shallow_backsteps_cannot_escape_fixed_floor(self):
        g=Geometry();prev=0
        for k in range(1,21):prev=g.add((k,0),44-k*2,prev)
        limits=horizon(44,8);reach=bounded_reach(g,g.graph,0,g.depth,limits,limits['depth_floor'])
        self.assertEqual(set(range(6)),set(reach))
        self.assertGreaterEqual(min(g.depth[i] for i in reach),limits['depth_floor'])

    def test_local_extent_not_inflated_by_terrain_cost(self):
        g=Geometry();g.arm(1,0);before=g.analyze()
        g.graph={i:[(q,c*10) for q,c in edges] for i,edges in g.graph.items()}
        after=g.analyze()
        for key in ('max_depth_reached','depth_gain','physical_extent','continuation_class'):
            self.assertEqual(before[key],after[key])
        self.assertGreater(after['effective_extent'],before['effective_extent'])

    def test_allowed_small_backward_detour_is_reported_not_an_exception(self):
        g=Geometry();prev=g.add((1,0),36,0)
        for k in range(2,8):prev=g.add((k,0),36+8*(k-1),prev)
        c=g.analyze()
        self.assertEqual('directed',c['continuation_class'])
        self.assertTrue(c['backtracking_dependency']);self.assertTrue(c['small_backward_detour_dependency'])
        self.assertFalse(c['beyond_local_floor_dependency'])

    def test_local_major_dependency_is_separate_from_deep_reach(self):
        g=Geometry();g.arm(1,0)
        blocked={i:[] for i in g.graph}
        c=g.analyze({'natural':blocked,'modest':blocked,'major':g.graph})
        self.assertEqual('dead_end',c['continuation_class']);self.assertTrue(c['major_intervention_dependency'])
        self.assertEqual((0,0,1),tuple(c[m+'_branch_count'] for m in MODES))

    def test_dead_end_cannot_be_selected_even_if_flag_or_lock_says_yes(self):
        g=Geometry();r={'id':'coast','eligible':True,'utility':999,'opportunity_group':'coast',
            'local_continuation':{'continuation_class':'dead_end'}}
        self.assertEqual([],select_joint(g,[r],PARAMETERS))
        self.assertEqual([],select_joint(g,[r],PARAMETERS,locked=[r]))

    def test_directed_is_not_scored_below_branching_or_junction(self):
        g=Geometry();g.arm(1,0)
        r={'id':'a','eligible':True,'utility':2,'opportunity_group':'same','_node':0,'_gate':0,
            '_path':[0],'kind':'forest','opening_regions':['plain']}
        for shape in ('directed','branching','junction'):
            a={**r,'local_continuation':{'continuation_class':shape}}
            b={**r,'id':'b','local_continuation':{'continuation_class':'junction'}}
            self.assertEqual('a',select_joint(g,[b,a],PARAMETERS)[0]['id'])


class LocalArtifacts(unittest.TestCase):
    def test_all_eight_keep_fields_segmentation_bounds_and_deep_reach(self):
        manifest=json.loads((OUT/'comparison.json').read_text())
        for row in manifest['candidates']:
            f=json.loads((OUT/row['fit']).read_text());old=json.loads((BASELINE/row['fit']).read_text())
            for key in ('homeland_fields','regions','region_grid_rle','terrain_connectivity_graph','water_systems',
                        'destination_catalog','homelands','playable_bounds','logical_orientation','parameters'):
                self.assertEqual(old[key],f[key],(f['seed'],key))
            for team,ss in f['selected_handoffs'].items():
                for r in ss:
                    self.assertNotEqual('dead_end',r['local_continuation']['continuation_class'])
                    self.assertEqual(0,r['selection_terms']['continuation_viability'])
                    self.assertLessEqual(r['local_continuation']['max_depth_reached'],r['local_continuation']['analysis_depth_limit'])
                    prior=next((v for v in old['selected_handoffs'][team] if v['anchor']==r['anchor']),None)
                    if prior:
                        for mode in MODES:
                            d=copy.deepcopy(prior['continuation'][mode])
                            d['legacy_regional_continuation_class']=d.pop('continuation_class')
                            d['legacy_regional_port_count']=d.pop('meaningful_branch_count')
                            d['eventual_network_participation']=bool(d['deeper_regions_reached'])
                            self.assertEqual(d,r['deep_network_reach'][mode])


if __name__=='__main__':unittest.main()
