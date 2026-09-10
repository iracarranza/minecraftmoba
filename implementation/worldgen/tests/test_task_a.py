"""General Task A properties on small analytical test fixtures, not seed tuning."""
import copy
import json
import math
from pathlib import Path
import sys
import unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from successor.grid import rle
from vanilla_search.task_a import (Terrain, fit, fit_homeland, edge_depth, starter,
                                   depth_band, network_graph, shortest)
from vanilla_search.task_a_render import svg


def fixture(w=48,h=64,flat=False):
    water=[False if flat else i%w>=w-6 for i in range(w*h)]
    heights=[64 if flat else 64+max(0,12-i%w)*2 for i in range(w*h)]
    forest=[False if flat else 28<=i%w<34 and 20<i//w<40 for i in range(w*h)]
    canopy=[False if flat else forest[i] and i%w==30 for i in range(w*h)]
    values={'height_rle':heights,'biome_rle':['minecraft:ocean' if q else 'minecraft:plains' for q in water],
            'surface_block_rle':['minecraft:water' if q else 'minecraft:grass_block' for q in water],
            'actual_surface_water_rle':water,'forest_mask_rle':forest,'canopy_mask_rle':canopy,
            'open_ground_mask_rle':[not water[i] and not canopy[i] for i in range(w*h)],
            'buildable_mask_rle':[not q for q in water],'highland_mask_rle':[i%w<10 for i in range(w*h)]}
    return {'seed':123,'region':{'block_bounds':[0,w*8-1,0,h*8-1]},
            'orientation':{'rotation_degrees_clockwise':0,'east_west_reflected':False,'logical_dimensions_blocks':[w*8,h*8]},
            'feature_grid':{'width':w,'height':h,'sample_spacing_blocks':8,'order':'oriented row-major',**{k:rle(v) for k,v in values.items()}},
            'greybox':{'landscape_regions':[{'id':name,'role':name,'logical_sample':[x,z],'area_blocks2':6400}
                                           for name,x,z in [('upland / mountain',4,32),('open country',24,32),('coast',40,32)]]}}


class TaskATests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.c=fixture();cls.result=fit(cls.c)

    def test_fit_determinism_and_input_immutability(self):
        original=copy.deepcopy(self.c)
        self.assertEqual(json.dumps(self.result,sort_keys=True),json.dumps(fit(self.c),sort_keys=True))
        self.assertEqual(original,self.c)

    def test_homeland_footprints_and_north_south(self):
        homes=self.result['homelands']
        for team in ('north','south'):
            x0,x1,z0,z1=homes[team]['world_bounds']
            self.assertEqual((72,72),(x1-x0+1,z1-z0+1))
            self.assertGreaterEqual(x0,0);self.assertLess(x1,48*8)
            self.assertGreaterEqual(z0,0);self.assertLess(z1,64*8)
        self.assertLess(homes['north']['world_bounds'][3],homes['south']['world_bounds'][2])

    def test_fountains_are_valid_inside_development_space(self):
        t=Terrain(self.c)
        for home in self.result['homelands'].values():
            x,z=home['fountain']['sample'];i=z*t.w+x
            x0,x1,z0,z1=home['logical_footprint_samples']
            self.assertTrue(x0<=x<=x1 and z0<=z<=z1)
            self.assertFalse(t.v['water'][i]);self.assertFalse(t.v['canopy'][i])
            self.assertTrue(t.v['buildable'][i]);self.assertLessEqual(t.grade[i],2)

    def test_fountain_not_forced_to_geometric_centroid(self):
        t=Terrain(fixture(flat=True));t.v['canopy']=[True]*t.n
        usable=12*t.w+20;t.v['canopy'][usable]=False
        home,_,_,fountain=fit_homeland(t,'north')
        self.assertEqual(usable,fountain)
        self.assertEqual(1,home['fountain_diagnostics']['valid_samples'])

    def test_effective_distance_and_edge_origin(self):
        t=Terrain(fixture(flat=True));a=20*t.w+20
        self.assertEqual(8,t.cost(a,a+1));self.assertAlmostEqual(8*math.sqrt(2),t.cost(a,a+t.w+1))
        depths=edge_depth(t,t.cells(a))
        self.assertEqual(4,depths[a+5]);self.assertEqual(44,depths[a+10]);self.assertEqual(0,depths[a])
        rough=fixture(flat=True);heights=[64]*t.n;heights[a+1]=72;rough['feature_grid']['height_rle']=rle(heights)
        self.assertGreater(Terrain(rough).cost(a,a+1),t.cost(a,a+1))

    def test_depth_band_boundaries(self):
        for value,expected in [(0,'fringe'),(40,'opening'),(70,'secondary_core'),(110,'secondary_transition'),
                               (150,'tertiary_core'),(210,'deep_transition'),(250,'deep_core'),(350,'deep_core_350_plus'),(math.inf,'unreachable')]:
            self.assertEqual(expected,depth_band(value))

    def test_starter_target_and_feasibility_are_separate_from_corridor(self):
        t=Terrain(fixture(flat=True));path=list(range(15*t.w+10,15*t.w+30))
        result,last=starter(t,path,4)
        self.assertTrue(60<=result['effective_blocks_from_edge']<=70)
        self.assertEqual(1,result['legibility_construction_score'])
        self.assertNotEqual(path[-1],last)
        t.v['water']=[True]*t.n
        poor,_=starter(t,path,4)
        self.assertLess(poor['legibility_construction_score'],result['legibility_construction_score'])
        self.assertIn('severe_starter_construction',poor['failures'])

    def test_three_distinct_departures_and_starter_measurement(self):
        self.assertEqual(6,len(self.result['routes']))
        for team in ('north','south'):
            routes=[r for r in self.result['routes'] if r['team']==team]
            self.assertEqual(3,len({tuple(r['departure']['sample']) for r in routes}))
            self.assertEqual({'highland','interior','coast'},{r['role'] for r in routes})
            self.assertIn('maximum_opening_overlap_fraction',self.result['network_metrics']['route_divergence'][team])
            for r in routes:
                self.assertIsNotNone(r['starter'])
                supported=r['starter']['supported_sample_path']
                self.assertEqual(self.result['homelands'][team]['fountain']['sample'],supported[0])
                self.assertEqual(r['starter']['terminus']['sample'],supported[-1])
                self.assertEqual(r['sample_path'][:len(supported)],supported)
                self.assertLess(r['starter']['effective_blocks_from_edge'],r['effective_blocks_from_edge'])
                self.assertAlmostEqual(2*r['starter']['effective_blocks_from_edge'],r['starter']['round_trip_effective_blocks'],places=2)

    def test_diagonal_network_intersections_are_joined(self):
        t=Terrain(fixture(flat=True));paths=[({'id':'a'},[0,t.w+1]),({'id':'b'},[1,t.w])]
        graph,members=network_graph(t,paths)
        self.assertEqual({'a','b'},members[(1,1)])
        distances,_=shortest(graph,{(0,0):0.})
        self.assertAlmostEqual(8*math.sqrt(2),distances[(2,0)])

    def test_cross_connections_and_shortcut_savings(self):
        connections={c['id']:c for c in self.result['cross_connections']}
        self.assertGreater(len(connections),0)
        for shortcut in self.result['shortcuts']:
            c=connections[shortcut['connection']]
            self.assertLess(c['effective_blocks'],c['existing_network_effective_blocks']*.85)
        self.assertGreaterEqual(self.result['network_metrics']['route_network_components'],1)

    def test_regional_depth_and_no_gameplay_content(self):
        for team in ('north','south'):
            self.assertEqual({'secondary','tertiary','deep'},set(self.result['regional_depth'][team]['core_access']))
        for forbidden in ('objectives','rewards','worksites','ore_guarantees','authored_villages'):
            self.assertNotIn(forbidden,self.result)
        self.assertIsNone(self.result['network_metrics']['resource_equivalence'])

    def test_failure_is_not_forced_into_success(self):
        c=fixture();n=48*64
        c['feature_grid']['actual_surface_water_rle']=rle([True]*n)
        c['feature_grid']['buildable_mask_rle']=rle([False]*n)
        result=fit(c)
        self.assertFalse(result['network_metrics']['complete_skeleton'])
        self.assertTrue(result['failures']);self.assertEqual(6,len(result['routes']))

    def test_svg_is_well_formed_and_contains_required_anchors(self):
        import xml.etree.ElementTree as ET
        result=svg(self.result,self.c,b'fixture-image')
        ET.fromstring(result)
        for label in ('NORTH HOME','SOUTH HOME','Aether Fountain','Starter','INTERIOR','EFFECTIVE DEPTH'):
            self.assertIn(label,result)


class CompletedTaskATests(unittest.TestCase):
    def test_all_eight_artifacts_and_immutability_record(self):
        import hashlib
        import xml.etree.ElementTree as ET
        root=Path(__file__).resolve().parents[1]/'results'
        out=root/'default_task_a_2026-09-10'
        summary=json.loads((root/'staged_default_2026-09-09/search_summary.json').read_text())
        comparison=json.loads((out/'comparison.json').read_text())
        self.assertEqual(sorted(summary['finalist_seeds']),[c['seed'] for c in comparison['candidates']])
        for row in comparison['candidates']:
            result=json.loads((out/row['fit']).read_text())
            ET.parse(out/row['svg'])
            self.assertEqual(row['network_metrics'],result['network_metrics'])
            self.assertEqual(result['source_bounds'],result['playable_bounds'])
            self.assertEqual(6,len(result['routes']))
            for route in result['routes']:
                self.assertIsNotNone(route['starter'])
                self.assertEqual(route['starter']['terminus']['sample'],route['starter']['supported_sample_path'][-1])
            for field in ('regional_depth','intersections','cross_connections','crossings','chokepoints',
                          'shortcuts','key_locations','landmarks','center','uncertainties','failures'):
                self.assertIn(field,result)
        verification=json.loads((out/'verification.json').read_text())
        self.assertTrue(verification['candidate_inputs_unchanged'])
        self.assertTrue(verification['available_worlds_unchanged'])
        self.assertEqual(16,len(verification['output_sha256']))
        for name,expected in verification['output_sha256'].items():
            self.assertEqual(expected,hashlib.sha256((out/name).read_bytes()).hexdigest())


if __name__=='__main__': unittest.main()
