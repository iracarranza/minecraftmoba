"""Meaningful geometry/provenance checks; no live server needed."""
import json
import sys
import unittest
from pathlib import Path

WORLDGEN=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(WORLDGEN))
from vanilla_search.evaluate import orient, _route_analysis
from vanilla_search.staged import select
from vanilla_search.extract import sampled_ground
from successor.grid import unrle

class StagedGeometryTests(unittest.TestCase):
    def test_snow_covered_tree_is_not_open_ground(self):
        class Chunk:
            def block(self,x,y,z):
                return 'minecraft:'+{80:'snow',79:'spruce_leaves',78:'air',77:'spruce_log',76:'grass_block'}.get(y,'stone')
        self.assertEqual((76,True),sampled_ground(Chunk(),0,0,80))

    def test_snow_on_soil_preserves_open_substrate(self):
        class Chunk:
            def block(self,x,y,z): return 'minecraft:snow' if y==80 else 'minecraft:grass_block'
        self.assertEqual((79,False),sampled_ground(Chunk(),0,0,80))

    def test_coastal_targets_stay_on_land_and_remain_distinct(self):
        rows=[[{'x':x*8,'z':z*8,'terrain_y':64,'actual_surface_water':x>=18} for x in range(30)] for z in range(30)]
        buildable=[not c['actual_surface_water'] for row in rows for c in row]
        routes=_route_analysis(rows,((10,4),(10,25)),buildable,uncapped_relief=True,flexible_anchors=True)
        targets={tuple(b['sample_path'][-1]) for b in routes['branches']}
        self.assertEqual(3,len(targets))
        self.assertTrue(all(x<18 for x,z in targets))
        self.assertTrue(all(b['water_steps']==0 for b in routes['branches']))

    def test_all_orientations_preserve_shifted_world_coordinates(self):
        rows=[[(x,z) for x in range(-2480,-1616,8)] for z in range(-528,528,8)]
        source=set(sum(rows,[]))
        for rotation in (0,90,180,270):
            for reflected in (False,True):
                with self.subTest(rotation=rotation,reflected=reflected):
                    result=orient(rows,rotation,reflected)
                    self.assertEqual(source,set(sum(result,[])))
                    self.assertEqual(len(rows)*len(rows[0]),sum(map(len,result)))

    def test_diversity_does_not_repeat_seed(self):
        records=[{'seed':i//2,'score':50-i/10,'features':[.4,.4,.1,.3,.2,.8,.8,.3]} for i in range(20)]
        result=select(records,5)
        self.assertEqual(5,len({r['seed'] for r in result}))

class CompletedFunnelTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.out=WORLDGEN/'results/staged_default_2026-09-09'
        if not (cls.out/'search_summary.json').exists(): raise unittest.SkipTest('run staged search first')
        cls.s=json.loads((cls.out/'search_summary.json').read_text())

    def test_funnel_conserves_counts(self):
        a=self.s['screen']
        self.assertEqual(a['windows'],sum(a['stage_a_rejections'].values())+a['stage_a_survivors'])
        self.assertLessEqual(a['stage_b_survivors'],a['stage_a_survivors'])
        self.assertLessEqual(self.s['full_generation_count'],16)
        self.assertGreaterEqual(self.s['finalist_count'],5)
        self.assertLessEqual(self.s['finalist_count'],10)

    def test_finalist_bounds_grid_and_route_coordinates(self):
        for seed in self.s['finalist_seeds']:
            c=json.loads((self.out/'finalists'/str(seed)/'candidate.json').read_text())
            b=c['region']['block_bounds'];o=c['orientation'];g=c['feature_grid']
            raw=[[ [x,z] for x in range(b[0]+4,b[1]+1,8)] for z in range(b[2]+4,b[3]+1,8)]
            logical=orient(raw,o['rotation_degrees_clockwise'],o['east_west_reflected'])
            self.assertFalse(c['worldgen_truth']['terrain_modified'])
            self.assertEqual(3564,c['data_completeness']['full_chunks'])
            for key in ('open_ground_mask_rle','canopy_mask_rle'):
                self.assertEqual(g['width']*g['height'],len(unrle(g[key])))
            self.assertEqual(6,len(c['routes']['branches']))
            self.assertGreaterEqual(len(c['greybox']['poi_nodes']),3)
            for branch in c['routes']['branches']:
                self.assertEqual([logical[z][x] for x,z in branch['sample_path']],branch['raw_world_path'])
                for a,d in zip(branch['sample_path'],branch['sample_path'][1:]):
                    self.assertEqual(1,max(abs(a[0]-d[0]),abs(a[1]-d[1])))
            for home in c['greybox']['homelands'].values():
                bb=home['raw_footprint_bounds']
                self.assertEqual(72,bb[1]-bb[0]+1);self.assertEqual(72,bb[3]-bb[2]+1)
                self.assertTrue(b[0]<=bb[0]<=bb[1]<=b[1] and b[2]<=bb[2]<=bb[3]<=b[3])

if __name__=='__main__': unittest.main()
