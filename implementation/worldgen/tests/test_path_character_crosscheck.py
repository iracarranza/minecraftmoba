import json
import sys
import unittest
from pathlib import Path

WORLDGEN = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(WORLDGEN))

from terrain_harvest.path_pieces import SPAN_DEVIATION, column_character

FIXTURE = WORLDGEN / 'fixtures' / 'path-character-crosscheck.json'

SHARED = {'entrance': 'ENTRANCE', 'bridge': 'BRIDGE', 'slope_up': 'STAIR',
          'slope_down': 'STAIR', 'bend': 'BEND', 'straight': 'STRAIGHT'}


class PathCharacterCrossCheck(unittest.TestCase):
    """The Python half of the per-column classification.

    The Java half is LabGeometry.character, and the two had drifted into
    answering different questions: BRIDGE meant water there and a void here,
    LANDING meant a terminus there and a slope reversal here, and Java had no
    ENTRANCE at all. Java also tested slope before water, so a sloping column
    over a river classified as a staircase.
    """

    @classmethod
    def setUpClass(cls):
        cls.fixture = json.loads(FIXTURE.read_text())

    def character(self, case, i):
        return SHARED[column_character(case['profile'], case['raw'], i,
                                       case['water'], case['turns'],
                                       SPAN_DEVIATION, case['termini'])]

    def test_every_case_reproduces_its_recorded_characters(self):
        for case in self.fixture['cases']:
            with self.subTest(case['note']):
                got = [self.character(case, i) for i in range(len(case['profile']))]
                self.assertEqual(case['characters'], got)

    def test_the_priority_order_is_the_one_the_fixture_declares(self):
        # Each rung beating the next, by a case that only that order explains.
        order = self.fixture['priority']
        self.assertEqual(['ENTRANCE', 'BRIDGE', 'STAIR', 'BEND', 'STRAIGHT'], order)

        # ENTRANCE over BRIDGE: a terminus standing in water is still the join.
        self.assertEqual('ENTRANCE', SHARED[column_character([70, 70], [70, 70], 0,
                                                             [True, True], (), 3, True)])
        # BRIDGE over STAIR: a staircase into a river is not a staircase.
        self.assertEqual('BRIDGE', SHARED[column_character([70, 71, 72], [70, 71, 72], 1,
                                                           [False, True, False], (), 3, True)])
        # STAIR over BEND: a column that climbs and turns is a stair.
        self.assertEqual('STAIR', SHARED[column_character([70, 71, 72], [70, 71, 72], 1,
                                                          [], [1], 3, True)])
        # BEND over STRAIGHT.
        self.assertEqual('BEND', SHARED[column_character([70, 70, 70], [70, 70, 70], 1,
                                                         [], [1], 3, True)])

    def test_a_bridge_has_two_reasons_and_one_name(self):
        # Water was Java's definition and a void was Python's. Both are true,
        # and a path over a ravine and a path over a river need the same piece.
        over_water = column_character([70, 70, 70], [70, 70, 70], 1, [False, True, False], (), 3, True)
        over_void = column_character([70, 70, 70], [70, 55, 70], 1, [], (), 3, True)
        self.assertEqual('bridge', over_water)
        self.assertEqual('bridge', over_void)

    def test_landing_is_not_a_column_character(self):
        # It is synthesised between two reversing stair runs, which is a
        # grouping decision. Cross-checking it would force the lab to implement
        # run-grouping it has no use for.
        produced = {c for case in self.fixture['cases'] for c in case['characters']}
        self.assertNotIn('LANDING', produced)
        self.assertIn('landing', self.fixture['rules'])

    def test_termini_are_opt_in(self):
        # A corridor cut for inspection has no structure at its ends, and
        # calling those columns entrances would demand thresholds that do not
        # exist.
        with_ends = column_character([70, 70, 70], [70, 70, 70], 0, [], (), 3, True)
        without = column_character([70, 70, 70], [70, 70, 70], 0, [], (), 3, False)
        self.assertEqual('entrance', with_ends)
        self.assertEqual('straight', without)

    def test_the_fixture_exercises_every_character(self):
        # A fixture that never produced a BEND would pass against an
        # implementation that had dropped bends entirely.
        produced = {c for case in self.fixture['cases'] for c in case['characters']}
        for expected in self.fixture['priority']:
            self.assertIn(expected, produced, f'no case produces {expected}')


if __name__ == '__main__':
    unittest.main()
