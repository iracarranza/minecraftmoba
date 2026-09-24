import unittest

from terrain_harvest import opening_access as OA


class Components(unittest.TestCase):
    """Exits are counted as connected arcs, not as frontier nodes."""

    def test_one_arc_of_many_nodes_is_one_exit(self):
        adj = [[(1, 1.0)], [(0, 1.0), (2, 1.0)], [(1, 1.0), (3, 1.0)], [(2, 1.0)]]
        self.assertEqual(len(OA._components([0, 1, 2, 3], adj)), 1)

    def test_two_arcs_separated_are_two_exits(self):
        """A team walled in on three sides must not read as huge capacity."""
        adj = [[(1, 1.0)], [(0, 1.0)], [(3, 1.0)], [(2, 1.0)]]
        self.assertEqual(len(OA._components([0, 1, 2, 3], adj)), 2)

    def test_edges_leaving_the_set_do_not_join_groups(self):
        adj = [[(1, 1.0)], [(0, 1.0), (9, 1.0)], [(9, 1.0)], [], []]
        adj += [[] for _ in range(6)]
        adj[9] = [(1, 1.0), (2, 1.0)]
        self.assertEqual(len(OA._components([0, 1, 2], adj)), 2)

    def test_adjacency_is_a_list_not_a_dict(self):
        """`Terrain.adj` is indexed by node. `adj.get(k)` looks defensive and
        raises AttributeError on the first real candidate."""
        adj = [[(1, 1.0)], [(0, 1.0)]]
        self.assertEqual(sorted(OA._neighbours(adj, 0)), [1])
        self.assertEqual(OA._neighbours(adj, 99), ())


class Reporting(unittest.TestCase):
    def test_no_fountains_is_stated_not_scored(self):
        r = OA.capacity({}, {})
        self.assertFalse(r['measured'])
        self.assertIn('why', r)

    def test_no_bound_is_asserted(self):
        """maps.md asks for equivalence and does not say how much is acceptable."""
        r = OA.capacity({}, {})
        self.assertNotIn('acceptable', r)
        self.assertNotIn('pass', r)


if __name__ == '__main__':
    unittest.main()
