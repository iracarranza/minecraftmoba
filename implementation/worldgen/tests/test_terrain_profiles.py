import unittest
from terrain_harvest.profiles import edge_points,observe
from terrain_harvest.model import make_volume,Mask


def volume(geometry):
    return make_volume({'source_seed':1,'source_dimension':'minecraft:overworld','source_bounds':{'x':[-8,8],'y':[-8,8],'z':[-8,8]},
        'minecraft_version':'1.21.11','worldgen_settings':{},'source_analysis_record':{'fixture':True}},
        'local_section',geometry,{'radius_scale_by_y':[[-8,'1/4'],[0,'1'],[8,'1']]} if geometry=='scoop' else {})

class ProfileTests(unittest.TestCase):
    def test_all_geometries_sample_true_edge(self):
        for geometry in ('box','ellipse','scoop'):
            v=volume(geometry);m=Mask(v)
            directions={'west':(-1,0),'east':(1,0),'north':(0,-1),'south':(0,1)}
            for edge,(x,y,z) in edge_points(v,3):
                dx,dz=directions[edge];self.assertTrue(m.include_block(x,y,z));self.assertFalse(m.include_block(x+dx,y,z+dz))
    def test_scoop_deep_edge_moves_inward(self):
        pts={(edge,pos[1]):pos for edge,pos in edge_points(volume('scoop'),8)}
        self.assertEqual(pts['east',-8],(2,-8,0));self.assertEqual(pts['east',0],(8,0,0))
        self.assertIn(('east',8),pts)
    def test_exact_properties_and_unknowns(self):
        state={'Name':'minecraft:oak_slab','Properties':{'waterlogged':'true','type':'bottom'}}
        p=observe(volume('box'),16,lambda *p:state)
        self.assertEqual(p['samples'][0]['block_state'],state)
        self.assertEqual(p['connection_interfaces'],[]);self.assertIn('solid collision shape',p['unresolved'])
        self.assertEqual(p,observe(volume('box'),16,lambda *p:state))
    def test_missing_and_invalid_sampling_fail(self):
        with self.assertRaises(ValueError):observe(volume('box'),16,lambda *p:None)
        for step in (0,-1,1.5,True):
            with self.assertRaises(ValueError):list(edge_points(volume('box'),step))

if __name__=='__main__':unittest.main()
