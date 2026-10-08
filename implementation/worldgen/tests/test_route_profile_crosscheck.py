import json
import sys
import unittest
from pathlib import Path

WORLDGEN = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(WORLDGEN))

from terrain_harvest.routes import anchors_reachable, profile_faults, walkable_profile

FIXTURE = WORLDGEN / 'fixtures' / 'route-profile-crosscheck.json'


class RouteProfileCrossCheck(unittest.TestCase):
    """The Python half of a predicate that exists twice.

    The walkable height profile is implemented here and in the plugin's
    LabGeometry, because the compiler authors Routes into a world file without
    a server and the lab fits paths at runtime against live blocks. Neither can
    call the other.

    Two copies of a rule drift, and these two had: measured over 2,000 random
    profiles they disagreed on 86%, because Java pinned the path to its anchors
    and this copy did not. Java was right. This copy was always walkable only
    because it was free to ignore the anchor, and a path that does not reach
    its door is not a walkable path but a different path.

    Both sides now run this fixture. The Java half is
    RouteProfileCrossCheckTest.
    """

    @classmethod
    def setUpClass(cls):
        cls.fixture = json.loads(FIXTURE.read_text())

    def test_every_case_reproduces_its_recorded_profile(self):
        for case in self.fixture['cases']:
            with self.subTest(case['note']):
                self.assertEqual(case['profile'], walkable_profile(case['raw']))

    def test_every_case_reproduces_its_recorded_faults(self):
        for case in self.fixture['cases']:
            with self.subTest(case['note']):
                got = profile_faults(walkable_profile(case['raw']), case['raw'])
                self.assertEqual(case['faults'], got)

    def test_the_anchors_are_always_met_exactly(self):
        # The half the Python copy was missing. A median takes an endpoint's
        # height from its neighbours, so an unpinned profile began up to three
        # blocks off the thing the path was meant to meet -- a floating
        # doorway, invisible to every measurement until entrance_faults.
        for case in self.fixture['cases']:
            with self.subTest(case['note']):
                profile, raw = case['profile'], case['raw']
                self.assertEqual(raw[0], profile[0])
                self.assertEqual(raw[-1], profile[-1])

    def test_a_feasible_case_is_walkable_everywhere(self):
        # True of both copies, and measured: over 2,000 random profiles neither
        # produced an unclimbable step on a feasible input. What neither did
        # was SAY when the input was infeasible.
        for case in self.fixture['cases']:
            if not anchors_reachable(case['raw']):
                continue
            with self.subTest(case['note']):
                steps = [abs(b - a) for a, b in zip(case['profile'], case['profile'][1:])]
                self.assertTrue(all(s <= 1 for s in steps), case['profile'])
                self.assertEqual([], case['faults'])

    def test_infeasibility_is_reported_as_itself(self):
        # When two anchors are further apart than the columns allow there is no
        # valid profile, and whichever end absorbs the shortfall does so
        # arbitrarily. Reporting "a step of 12 at column 0" names a symptom at a
        # position the algorithm chose, and invites fixing the step rather than
        # the route.
        infeasible = [c for c in self.fixture['cases'] if not anchors_reachable(c['raw'])]
        self.assertTrue(infeasible, 'the fixture must carry infeasible cases')
        for case in infeasible:
            with self.subTest(case['note']):
                self.assertEqual(1, len(case['faults']))
                self.assertIn('no walkable profile meets both', case['faults'][0])

    def test_the_reachability_rule_is_the_arithmetic_it_claims(self):
        self.assertTrue(anchors_reachable([70, 71]))
        self.assertFalse(anchors_reachable([70, 72]), 'one step cannot cross two blocks')
        self.assertTrue(anchors_reachable([70, 72, 72]), 'two steps can')

    def test_the_fixture_covers_both_outcomes(self):
        # A fixture of only clean cases would pass against an implementation
        # that never reported anything.
        faults = [c for c in self.fixture['cases'] if c['faults']]
        clean = [c for c in self.fixture['cases'] if not c['faults']]
        self.assertTrue(faults and clean,
                        f'{len(clean)} clean and {len(faults)} faulted')


if __name__ == '__main__':
    unittest.main()
