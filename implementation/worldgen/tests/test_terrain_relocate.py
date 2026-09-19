"""Stage 6: relocation on fixtures only. Nothing here touches corpus export."""
import unittest
from serialization.nbt import (COMPOUND, DOUBLE, FLOAT, compound, double, float_tag,
                               integer, list_tag, plain, string)
from terrain_harvest.relocate import (UnsupportedState, place_point, relocate_block_entity,
    relocate_entity, relocate_tick, rotate_direction, rotate_point, rotate_properties)

class DirectionTests(unittest.TestCase):
    def test_cardinals_rotate_clockwise(self):
        self.assertEqual(rotate_direction('north', 1), 'east')
        self.assertEqual(rotate_direction('west', 1), 'north')
        self.assertEqual(rotate_direction('north', 4), 'north')

    def test_vertical_is_untouched(self):
        for v in ('up', 'down'):
            self.assertEqual(rotate_direction(v, 1), v)

    def test_unknown_direction_is_rejected(self):
        with self.assertRaises(UnsupportedState): rotate_direction('sideways', 1)

class PropertyTests(unittest.TestCase):
    def roundtrip(self, name, props):
        out = props
        for _ in range(4): out = rotate_properties(name, out, 1)
        self.assertEqual(out, props, name)

    def test_four_quarter_turns_are_identity(self):
        for name, props in [
            ('minecraft:oak_stairs', {'facing': 'north', 'half': 'bottom', 'shape': 'inner_left', 'waterlogged': 'false'}),
            ('minecraft:oak_log', {'axis': 'x'}),
            ('minecraft:oak_sign', {'rotation': '3', 'waterlogged': 'false'}),
            ('minecraft:rail', {'shape': 'ascending_north'}),
            ('minecraft:rail', {'shape': 'north_east'}),
            ('minecraft:oak_door', {'facing': 'east', 'hinge': 'left', 'half': 'upper', 'open': 'true', 'powered': 'false'}),
            ('minecraft:oak_fence', {'north': 'true', 'east': 'false', 'south': 'true', 'west': 'false', 'waterlogged': 'false'}),
            ('minecraft:crafter', {'orientation': 'down_east', 'crafting': 'false', 'triggered': 'false'}),
        ]:
            self.roundtrip(name, props)

    def test_axis_swaps_only_on_odd_quarters(self):
        self.assertEqual(rotate_properties('l', {'axis': 'x'}, 1), {'axis': 'z'})
        self.assertEqual(rotate_properties('l', {'axis': 'x'}, 2), {'axis': 'x'})
        self.assertEqual(rotate_properties('l', {'axis': 'y'}, 1), {'axis': 'y'})

    def test_sign_rotation_advances_by_four(self):
        self.assertEqual(rotate_properties('s', {'rotation': '0'}, 1), {'rotation': '4'})
        self.assertEqual(rotate_properties('s', {'rotation': '14'}, 1), {'rotation': '2'})

    def test_multiface_booleans_permute_without_loss(self):
        got = rotate_properties('f', {'north': 'true', 'east': 'false', 'south': 'false', 'west': 'true'}, 1)
        self.assertEqual(got, {'east': 'true', 'south': 'false', 'west': 'false', 'north': 'true'})

    def test_rail_corner_rotates(self):
        self.assertEqual(rotate_properties('r', {'shape': 'north_east'}, 1), {'shape': 'south_east'})

    def test_unclassified_property_is_rejected_not_guessed(self):
        with self.assertRaisesRegex(UnsupportedState, 'unclassified property'):
            rotate_properties('minecraft:future_block', {'some_new_direction': 'north'}, 1)

    def test_unknown_shape_is_rejected(self):
        with self.assertRaises(UnsupportedState):
            rotate_properties('r', {'shape': 'spiral'}, 1)

    def test_identity_rotation_passes_anything_through(self):
        props = {'some_new_direction': 'north'}
        self.assertEqual(rotate_properties('x', props, 0), props)

class PointTests(unittest.TestCase):
    def test_rotation_is_clockwise_and_periodic(self):
        self.assertEqual(rotate_point((1, 5, 0), 1), (0, 5, 1))
        self.assertEqual(rotate_point((1, 5, 0), 4), (1, 5, 0))

    def test_translation_applies_after_rotation(self):
        self.assertEqual(place_point((1, 0, 0), 1, (10, 0, 0)), (10, 0, 1))

class NbtTests(unittest.TestCase):
    def test_block_entity_moves(self):
        be = compound(id=string('minecraft:chest'), x=integer(2), y=integer(3), z=integer(0))
        got = plain(relocate_block_entity(be, 1, (10, 0, 5)))
        self.assertEqual((got['x'], got['y'], got['z']), (10, 3, 7))

    def test_block_entity_with_external_target_is_rejected(self):
        be = compound(id=string('minecraft:beehive'), x=integer(0), y=integer(0), z=integer(0),
                      flower_pos=compound(X=integer(1), Y=integer(2), Z=integer(3)))
        with self.assertRaisesRegex(UnsupportedState, 'beehive flower position'):
            relocate_block_entity(be, 1, (0, 0, 0))

    def test_tick_moves(self):
        t = compound(i=string('minecraft:water'), x=integer(0), y=integer(1), z=integer(4), t=integer(0), p=integer(0))
        got = plain(relocate_tick(t, 1, (0, 0, 0)))
        self.assertEqual((got['x'], got['y'], got['z']), (-4, 1, 0))

    def entity(self, **extra):
        e = compound(id=string('minecraft:cow'),
                     Pos=list_tag(DOUBLE, [double(2.5), double(1.0), double(0.5)]),
                     Motion=list_tag(DOUBLE, [double(0.1), double(0.0), double(0.0)]),
                     Rotation=list_tag(FLOAT, [float_tag(0.0), float_tag(10.0)]))
        e.value.update(extra)
        return e

    def test_entity_position_motion_and_yaw_rotate_together(self):
        got = plain(relocate_entity(self.entity(), 1, (0, 0, 0)))
        self.assertAlmostEqual(got['Pos'][0], -0.5)
        self.assertAlmostEqual(got['Pos'][2], 2.5)
        self.assertAlmostEqual(got['Motion'][2], 0.1, places=6)
        self.assertAlmostEqual(got['Rotation'][0], 90.0)
        self.assertAlmostEqual(got['Rotation'][1], 10.0)

    def test_entity_four_turns_return_to_start(self):
        e = self.entity(); out = e
        for _ in range(4): out = relocate_entity(out, 1, (0, 0, 0))
        a, b = plain(e), plain(out)
        for i in range(3):
            self.assertAlmostEqual(a['Pos'][i], b['Pos'][i], places=6)
            self.assertAlmostEqual(a['Motion'][i], b['Motion'][i], places=6)
        self.assertAlmostEqual(a['Rotation'][0] % 360, b['Rotation'][0] % 360, places=4)

    def test_passengers_rotate_with_the_root(self):
        rider = self.entity(); rider.value['id'] = string('minecraft:pig')
        root = self.entity(Passengers=list_tag(COMPOUND, [rider]))
        got = plain(relocate_entity(root, 1, (100, 0, 0)))
        self.assertAlmostEqual(got['Passengers'][0]['Pos'][0], 99.5)

    def test_leashed_entity_is_rejected(self):
        with self.assertRaisesRegex(UnsupportedState, 'leash anchor'):
            relocate_entity(self.entity(leash=compound(X=integer(1), Y=integer(2), Z=integer(3))), 1, (0, 0, 0))

    def test_villager_brain_is_rejected(self):
        with self.assertRaisesRegex(UnsupportedState, 'memories'):
            relocate_entity(self.entity(Brain=compound(memories=compound())), 1, (0, 0, 0))

if __name__ == '__main__':
    unittest.main()
