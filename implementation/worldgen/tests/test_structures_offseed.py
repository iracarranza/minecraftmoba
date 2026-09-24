import unittest
import unittest.mock

from terrain_harvest import structures_offseed as St

BOUNDS = (-432, 431, -528, 527)          # the compiler window, centred


def at(kind, x, z):
    return {'type': kind, 'x': x, 'z': z, 'y': 70, 'biome': 29}


class Location(unittest.TestCase):
    """The location half of a (feature, location) Type term."""

    def test_a_central_mansion_and_an_edge_mansion_are_different_maps(self):
        centre = [at('mansion', 10, -20)]
        edge = [at('mansion', 420, 500)]
        self.assertTrue(St.central(centre, 'mansion', BOUNDS))
        self.assertFalse(St.central(edge, 'mansion', BOUNDS))
        # both are CONTAINED; only containment would call them the same map
        self.assertEqual(St.contained(centre, 'mansion', BOUNDS), 1)
        self.assertEqual(St.contained(edge, 'mansion', BOUNDS), 1)

    def test_a_structure_outside_the_scoop_does_not_count(self):
        far = [at('mansion', 3000, 3000)]
        self.assertEqual(St.contained(far, 'mansion', BOUNDS), 0)

    def test_kind_is_respected(self):
        found = [at('village', 0, 0)]
        self.assertEqual(St.contained(found, 'mansion', BOUNDS), 0)
        self.assertEqual(St.contained(found, 'village', BOUNDS), 1)

    def test_offset_is_reported_as_a_fraction_of_the_scoop(self):
        o = St.offset_from_centre(at('mansion', 0, 0), BOUNDS)
        self.assertAlmostEqual(o['x_offset'], 0.0, places=2)
        self.assertAlmostEqual(o['blocks_from_centre'], 0.7, delta=1.0)
        far = St.offset_from_centre(at('mansion', 431, 527), BOUNDS)
        self.assertGreater(far['blocks_from_centre'], 600)

    def test_central_scales_with_the_scoop(self):
        """A third of a big scoop is a bigger area than a third of a small one."""
        small = (-100, 100, -100, 100)
        self.assertFalse(St.central([at('mansion', 200, 0)], 'mansion', small))
        self.assertTrue(St.central([at('mansion', 20, 0)], 'mansion', small))


class Inputs(unittest.TestCase):
    def test_an_unknown_kind_is_refused_not_silently_empty(self):
        with self.assertRaises(ValueError):
            St.find(1, kinds=('woodland_manor',), finder='/nonexistent')

    def test_a_missing_finder_raises_instead_of_reporting_no_structures(self):
        """Otherwise every structural Map Type looks unsatisfiable by nature."""
        with unittest.mock.patch.dict('os.environ', {}, clear=True):
            with unittest.mock.patch('shutil.which', return_value=None):
                with self.assertRaises(RuntimeError) as e:
                    St.find(1)
        self.assertIn('unsatisfiable', str(e.exception))

    def test_every_advertised_kind_is_spelled_consistently(self):
        self.assertEqual(len(St.KINDS), len(set(St.KINDS)))
        for k in St.KINDS:
            self.assertRegex(k, r'^[a-z_]+$')


if __name__ == '__main__':
    unittest.main()
