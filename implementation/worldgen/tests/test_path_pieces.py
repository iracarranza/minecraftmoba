import unittest
import sys
from pathlib import Path

WORLDGEN = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(WORLDGEN))

from terrain_harvest import path_pieces as pp


class Segmentation(unittest.TestCase):
    """Cutting a fitted profile into pieces that say what the path IS."""

    def test_flat_ground_is_one_straight_and_not_many(self):
        # The current system classifies COLUMNS; this classifies runs. A flat
        # corridor is one piece, not forty identical ones.
        profile = [70] * 40
        pieces = pp.segment(profile, profile)
        self.assertEqual(1, len(pieces))
        self.assertEqual('straight', pieces[0].kind)
        self.assertEqual(40, pieces[0].columns)

    def test_a_climb_becomes_stairs_carrying_its_own_rise(self):
        profile = [70, 70, 71, 72, 73, 73]
        pieces = pp.segment(profile, profile)
        kinds = [p.kind for p in pieces]
        self.assertEqual(['straight', 'stairs', 'straight'], kinds)
        self.assertEqual(3, pieces[1].rise, 'the stairs own the whole climb')

    def test_ground_far_below_the_profile_becomes_a_bridge(self):
        # A ravine: the profile holds its line while the ground falls away.
        profile = [70] * 7
        raw = [70, 70, 64, 62, 63, 70, 70]
        pieces = pp.segment(profile, raw)
        self.assertEqual(['straight', 'bridge', 'straight'], [p.kind for p in pieces])
        self.assertEqual(3, pieces[1].columns)

    def test_a_shallow_hollow_is_not_a_bridge(self):
        # Within the span threshold the ground is made up, not spanned. A
        # bridge over a two-block dip would be absurd construction.
        profile = [70] * 5
        raw = [70, 69, 68, 69, 70]
        self.assertEqual(['straight'], [p.kind for p in pp.segment(profile, raw)])

    def test_a_turn_becomes_a_bend_where_the_caller_says_it_turns(self):
        # Geometry belongs to the caller; this module is given the indices.
        profile = [70] * 6
        pieces = pp.segment(profile, profile, turns=[3])
        self.assertEqual(['straight', 'bend', 'straight'], [p.kind for p in pieces])

    def test_unknown_ground_does_not_provoke_construction(self):
        # An unmeasured column is a gap in knowledge, not a ravine.
        profile = [70, None, 70]
        raw = [70, None, 70]
        self.assertEqual(['straight'], [p.kind for p in pp.segment(profile, raw)])

    def test_an_empty_profile_yields_no_pieces(self):
        self.assertEqual([], pp.segment([], []))


class Connections(unittest.TestCase):
    """The half vanilla gets from jigsaw and this project never had."""

    def test_two_runs_of_stairs_cannot_abut(self):
        # The rule that earns its place. The step between two slopes is bounded
        # by neither piece, so nothing could measure it.
        up = pp.Piece('stairs', 0, 2, 'slope', 'slope', 2)
        down = pp.Piece('stairs', 3, 5, 'slope', 'slope', -2)
        self.assertFalse(pp.connects(up, down))

    def test_a_ridge_gets_a_landing_rather_than_a_refusal(self):
        # A reversal of slope is an ordinary thing for terrain to do. Refusing
        # it would describe the terrain's difficulty rather than solve it.
        profile = [70, 71, 72, 71, 70]
        pieces = pp.segment(profile, profile)
        self.assertIn('landing', [p.kind for p in pieces])
        self.assertEqual([], pp.validate(pieces), 'and the result is sound')

    def test_the_landing_does_not_displace_the_pieces_around_it(self):
        # Zero-length on purpose: giving it width would move every later piece
        # and the pieces would stop describing the profile they were cut from.
        profile = [70, 71, 72, 71, 70]
        pieces = pp.segment(profile, profile)
        landing = next(p for p in pieces if p.kind == 'landing')
        self.assertEqual(landing.start, landing.end)
        self.assertEqual(len(profile) - 1, max(p.end for p in pieces))

    def test_validate_names_a_path_whose_pieces_do_not_meet(self):
        left = pp.Piece('straight', 0, 4, 'flat', 'flat')
        right = pp.Piece('straight', 9, 12, 'flat', 'flat')
        faults = pp.validate([left, right])
        self.assertEqual(1, len(faults))
        self.assertIn('gap', faults[0])

    def test_only_stairs_may_change_height(self):
        # A straight that climbs is a straight that is lying about itself, and
        # it is how a jump gets back into a path that passed segmentation.
        lying = pp.Piece('straight', 0, 3, 'flat', 'flat', rise=2)
        faults = pp.validate([lying])
        self.assertEqual(1, len(faults))
        self.assertIn('only stairs', faults[0])

    def test_a_sound_path_has_nothing_to_say(self):
        self.assertEqual([], pp.validate(pp.segment([70] * 10, [70] * 10)))


class TraversalMeasurement(unittest.TestCase):
    """The step vanilla does not need: does the finished path actually walk?"""

    def test_the_audits_metric_is_preserved_exactly(self):
        # Kept identical so before and after are comparable. Changing it here
        # would make the improvement unprovable.
        flat = pp.traverse([70] * 10)
        self.assertEqual(9, flat.steps)
        self.assertEqual(0, flat.jumps)
        self.assertEqual(0, flat.unclimbable)
        self.assertTrue(flat.walkable)

    def test_a_single_block_rise_is_a_jump_but_is_climbable(self):
        t = pp.traverse([70, 71])
        self.assertEqual(1, t.jumps)
        self.assertEqual(0, t.unclimbable)
        self.assertTrue(t.walkable)

    def test_a_two_block_rise_cannot_be_climbed(self):
        t = pp.traverse([70, 72])
        self.assertEqual(1, t.unclimbable)
        self.assertEqual(2, t.worst_step)
        self.assertFalse(t.walkable)

    def test_the_frozen_maps_cliff_is_what_this_would_have_caught(self):
        # The audit found individual steps of 16, 18 and 34 blocks, where a
        # corridor ran over a cliff edge and laid path blocks down the face.
        t = pp.traverse([70, 70, 36, 36])
        self.assertFalse(t.walkable)
        self.assertEqual(34, t.worst_step)

    def test_a_step_limited_profile_is_walkable_by_construction(self):
        # Which is the point of fitting one. Every adjacent pair differs by at
        # most one, so nothing is unclimbable however rough the ground was.
        profile = [70, 71, 72, 72, 71, 70, 70, 71]
        t = pp.traverse(profile)
        self.assertTrue(t.walkable)
        self.assertEqual(1, t.worst_step)

    def test_unknown_columns_are_skipped_rather_than_counted_as_falls(self):
        t = pp.traverse([70, None, 70])
        self.assertEqual(0, t.steps, 'nothing is known about either side of a gap')
        self.assertTrue(t.walkable)

    def test_jump_share_reports_the_number_the_audit_led_with(self):
        t = pp.traverse([70, 71, 71, 72])
        self.assertAlmostEqual(2 / 3, t.jump_share)


class Reporting(unittest.TestCase):

    def test_a_summary_says_what_was_built_not_only_how_much(self):
        # A true ridge, so the summary has to account for the landing that was
        # SYNTHESISED rather than segmented -- the count must cover pieces the
        # profile did not itself contain.
        profile = [70, 71, 72, 71, 70]
        pieces = pp.segment(profile, profile)
        counts = pp.summarise(pieces)
        self.assertIn('stairs', counts)
        self.assertIn('landing', counts)
        self.assertEqual(sum(counts.values()), len(pieces))

    def test_a_plateau_separates_two_climbs_without_needing_a_landing(self):
        # The landing exists for a REVERSAL, not for every pair of stairs. Flat
        # ground at the top already provides the legal join, and inserting one
        # anyway would be construction nobody asked for.
        profile = [70, 70, 71, 72, 72, 71, 70]
        pieces = pp.segment(profile, profile)
        self.assertEqual(['straight', 'stairs', 'straight', 'stairs'],
                         [p.kind for p in pieces])
        self.assertEqual([], pp.validate(pieces))


if __name__ == '__main__':
    unittest.main()
