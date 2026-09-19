import copy
import json
import tempfile
import unittest
from pathlib import Path
from terrain_harvest.model import *
from terrain_harvest.library import write_library,reference_candidate
from terrain_harvest.gallery import navigation
from terrain_harvest.materialize import clip_chunk,empty_chunk,entity_inside,export_volume,read_entity_region
from serialization.nbt import plain,compound,list_tag,byte,string,double,COMPOUND,DOUBLE
from serialization.world import _block_states_tag,block
from vanilla_search.extract import VanillaChunk,_palette_value


def fixture(kind='whole_map',geometry='box'):
    return make_volume({'source_seed':42,'source_dimension':'minecraft:overworld','source_bounds':{'x':[-4,4],'y':[-4,4],'z':[-4,4]},
        'minecraft_version':'1.21.11','data_version':4671,'worldgen_settings':{'status':'UNRESOLVED'},'source_analysis_record':{'path':'fixture','sha256':'fixture'}},
        kind,geometry,{'radius_scale_by_y':[[-4,'1/4'],[0,'1'],[4,'1']]} if geometry=='scoop' else {},
        measurements=[{'evidence_state':'UNRESOLVED','criterion':'Practical Reach'}],criteria=['unresolved coast'] if kind=='near_miss_map' else [])

class ModelTests(unittest.TestCase):
    def test_roundtrip_stability(self):
        v=fixture();self.assertEqual(loads(dumps(v)),v)
        self.assertEqual(identity(json.loads(json.dumps(v,sort_keys=True))),v['id'])
        for key,val in [('source_seed',43),('source_dimension','minecraft:the_nether')]:
            q=copy.deepcopy(v);q['provenance'][key]=val;self.assertNotEqual(identity(q),v['id'])
        q=copy.deepcopy(v);q['provenance']['source_bounds']['x'][0]-=1;self.assertNotEqual(identity(q),v['id'])
    def test_classification_evidence(self):
        for kind in KINDS:self.assertEqual(validate(fixture(kind))['classification']['kind'],kind)
        v=fixture('near_miss_map');v['classification']['failed_or_unresolved_criteria']=[]
        with self.assertRaises(ValueError):validate(v)
        self.assertEqual(fixture()['measurements'][0]['evidence_state'],'UNRESOLVED')
    def test_box(self):
        m=Mask(fixture());self.assertTrue(m.include_block(-4,-4,4));self.assertFalse(m.include_block(0,-5,0));self.assertFalse(m.include_block(5,0,0))
    def test_ellipse(self):
        m=Mask(fixture(geometry='ellipse'));self.assertTrue(m.include_block(4,0,0));self.assertFalse(m.include_block(4,0,4));self.assertFalse(m.include_block(0,5,0))
    def test_scoop_depth(self):
        m=Mask(fixture(geometry='scoop'));self.assertTrue(m.include_block(3,0,0));self.assertFalse(m.include_block(3,-4,0));self.assertTrue(m.include_block(1,-4,0));self.assertFalse(m.include_block(2,-4,0));self.assertFalse(m.include_block(0,-5,0))
    def test_invalid_bounds_and_scoop(self):
        v=fixture();v['provenance']['source_bounds']['y']=[4,-4];v['id']=identity(v)
        with self.assertRaises(ValueError):validate(v)
        v=fixture(geometry='scoop');v['boundary']['parameters']['radius_scale_by_y'][1][1]='2';v['id']=identity(v)
        with self.assertRaises(ValueError):validate(v)
    def test_transform_roundtrip(self):
        v=fixture()
        for r in (0,90,180,270):
            v['transform']={'rotation':r,'translation':[10,20,-30]}
            for p in [(0,0,0),(-7,-64,13),(4,319,-2)]:self.assertEqual(transform_point(v,transform_point(v,p),inverse=True),p)
        v['transform']={'rotation':90,'translation':[10,20,-30]};self.assertEqual(transform_point(v,(2,3,4)),(6,23,-28))
    def test_envelope_separates_every_neighbor(self):
        for shape in ('box','ellipse','scoop'):
            m=Mask(fixture(geometry=shape))
            for y in range(-4,5):
                for z in range(-4,5):
                    for x in range(-4,5):
                        if m.include_block(x,y,z):
                            for dx in (-1,0,1):
                                for dy in (-1,0,1):
                                    for dz in (-1,0,1):
                                        p=(x+dx,y+dy,z+dz)
                                        self.assertTrue(m.include_block(*p) or m.envelope(*p))
            self.assertTrue(m.envelope(0,-5,0));self.assertTrue(m.envelope(0,5,0));self.assertFalse(m.envelope(0,-6,0))
    def test_library_retains_multiple_and_near_miss(self):
        a=fixture();b=fixture('near_miss_map',geometry='ellipse')
        with tempfile.TemporaryDirectory() as d:
            p=Path(d);write_library([a,b],p/'one');write_library([b,a],p/'two')
            self.assertEqual((p/'one/index.json').read_bytes(),(p/'two/index.json').read_bytes())
            self.assertEqual(len(json.loads((p/'one/index.json').read_text())['volumes']),2)
    def test_navigation_deterministic(self):
        v=fixture();a=navigation([v],{v['id']:[0,1,0]});self.assertEqual(a,navigation([v],{v['id']:[0,1,0]}))
        self.assertIn('next',a);self.assertIn('previous',a);self.assertIn('hub',a)
        self.assertFalse(any(line.startswith('tick ') for line in a['load']))
        self.assertTrue(a['visit/'+v['id']][0].startswith('gamemode adventure'))
    def test_generic_retention_missing_evidence(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);path=root/'candidate.json';candidate={'seed':42,'region':{'block_bounds':[-4,4,-4,4]},'minecraft_java_version':'1.21.11','data_version':4671,'generator':'fixture'}
            path.write_text(json.dumps(candidate));bounds=fixture()['provenance']['source_bounds']
            with self.assertRaises(ValueError):reference_candidate(root,path,bounds,'whole_map','box')
            near=reference_candidate(root,path,bounds,'near_miss_map','box',criteria=['Stage C missing'])
            self.assertEqual(near['classification']['kind'],'near_miss_map')
            self.assertEqual(near['measurements'][0]['evidence_state'],'UNRESOLVED')
            candidate['stage_c']={'hard_failures':['coast absent']};path.write_text(json.dumps(candidate))
            with self.assertRaises(ValueError):reference_candidate(root,path,bounds,'whole_map','box')
            candidate['stage_c']={'hard_failures':[]};path.write_text(json.dumps(candidate))
            self.assertEqual(reference_candidate(root,path,bounds,'whole_map','box')['classification']['kind'],'whole_map')
            bounds['x'][0]=-5
            with self.assertRaises(ValueError):reference_candidate(root,path,bounds,'local_section','box')

    def test_tampered_manifest_rejected(self):
        v=fixture();v['provenance']['source_seed']=45
        with self.assertRaises(ValueError):loads(json.dumps(v))
    def test_physical_transform_fails_before_io(self):
        v=fixture();v['transform']['rotation']=90
        with self.assertRaisesRegex(ValueError,'transforms'):export_volume(v,Path('/absent'),Path('/absent'))

class ClipTests(unittest.TestCase):
    def test_source_state_preserved_and_outside_void(self):
        v=fixture();m=Mask(v);root=empty_chunk(0,0)
        water=block('water',level='0');root.value['sections']=list_tag(COMPOUND,[compound(Y=byte(0),block_states=_block_states_tag([water]*4096),biomes=compound(palette=list_tag(8,[string('minecraft:ocean')])) )])
        before=copy.deepcopy(root);out=clip_chunk(root,0,0,m);self.assertEqual(before,root)
        c=VanillaChunk(plain(out));self.assertEqual(c.block(0,0,0),'minecraft:water');self.assertEqual(c.block(5,0,0),'minecraft:bedrock');self.assertEqual(c.block(6,0,0),'minecraft:air');self.assertEqual(c.block(0,5,0),'minecraft:barrier')
        s=c.sections[0]['block_states'];self.assertEqual(_palette_value(s,0,4),{'Name':'minecraft:water','Properties':{'level':'0'}})
    def test_empty_entity_placeholder_not_corrupt_region(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/'r.0.0.mca';p.write_bytes(b'')
            self.assertEqual(list(read_entity_region(p)),[])
            p.write_bytes(b'broken')
            with self.assertRaises(ValueError):list(read_entity_region(p))

    def test_entity_tree_boundary(self):
        m=Mask(fixture());e=compound(Pos=list_tag(DOUBLE,[double(0),double(0),double(0)]));self.assertTrue(entity_inside(e,m))
        e.value['Passengers']=list_tag(COMPOUND,[compound(Pos=list_tag(DOUBLE,[double(5),double(0),double(0)]))]);self.assertFalse(entity_inside(e,m))

if __name__=='__main__':unittest.main()
