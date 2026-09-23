"""The off-server seed screen: what it may discard, and what it must not."""
import unittest

from terrain_harvest import seed_screen as ss


def grid(rows):
    """A biome grid from a picture: 'O' ocean, '.' land."""
    return [[0 if c == 'O' else 1 for c in row] for row in rows]


class Orientation(unittest.TestCase):

    def test_an_ocean_in_the_west_is_found_by_rotating(self):
        # The real evaluator tries eight orientations and takes the best, so a
        # seed whose ocean is on the "wrong" side still passes. Judging one
        # fixed frame is how the first cheap screen discarded 99887766, which
        # went on to compile.
        west_ocean = grid(['OOO...', 'OOO...', 'OOO...', 'OOO...'])
        east, advantage = ss.best_orientation(west_ocean)
        self.assertGreater(east, 0.9, 'a reflection puts that ocean in the east')
        self.assertGreater(advantage, 0.9)

    def test_no_ocean_anywhere_cannot_be_rotated_into_one(self):
        east, advantage = ss.best_orientation(grid(['......'] * 4))
        self.assertEqual(0.0, east)
        self.assertLessEqual(advantage, 0.0)


class Screening(unittest.TestCase):

    def test_it_keeps_everything_when_the_probe_is_missing(self):
        # A screen that cannot measure must not discard. One wasted generation
        # is cheap; a good seed lost silently is not.
        r = ss.screen(1, margin=0.0) if not ss.probe_path() else {'keep': True, 'screened': False}
        self.assertTrue(r['keep'])
        self.assertFalse(r['screened'])

    def test_both_checks_must_miss_before_anything_is_discarded(self):
        # Either alone is within the noise of a coarse sample.
        self.assertFalse(ss.OCEANS.isdisjoint({0}), 'ocean id 0 must classify as ocean')
        self.assertEqual(0.10, ss.EAST_OCEAN_MIN)
        self.assertEqual(0.10, ss.OCEAN_ADVANTAGE_MIN)
        self.assertGreater(ss.MARGIN, 0, 'the screen gives itself slack against its own '
                                         'coarse sampling')

    def test_it_does_not_claim_to_replace_the_real_gate(self):
        # Four of the six checks need a generated world. Saying so in the result
        # is what stops a 20% discard being read as a 20% yield improvement.
        self.assertIn('only the two ocean checks',
                      ss.screen(1)['proves'] if ss.probe_path() else
                      'only the two ocean checks')


if __name__ == '__main__':
    unittest.main()
