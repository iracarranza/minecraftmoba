"""The Opening Hinterland ceiling, which was doctrine with no implementation."""
import unittest

from terrain_harvest import opening_ceiling


class FakeWorld:
    """Columns with a surface and optional built blocks standing on them."""

    def __init__(self, built=()):
        self.built = {tuple(xz): block for xz, block in built}

    def surface(self, x, z):
        return 64

    def block(self, x, y, z):
        if y == 64 and (x, z) in self.built:
            return self.built[(x, z)]
        return 'minecraft:air'


class OpeningCeiling(unittest.TestCase):

    def test_a_clean_opening_certifies(self):
        hits = opening_ceiling.survey(FakeWorld(), 0, 0)
        self.assertEqual([], hits)

    def test_a_village_in_the_opening_is_found(self):
        # Bells and hay blocks are village, not terrain. This is the block set
        # actually found 68 blocks from the south Fountain on 99887766.
        world = FakeWorld([((32, 0), 'minecraft:bell'),
                           ((40, 8), 'minecraft:hay_block'),
                           ((48, 0), 'minecraft:acacia_planks')])
        hits = opening_ceiling.survey(world, 0, 0)
        self.assertTrue(hits, 'a village inside the opening envelope must be found')
        self.assertIn('minecraft:bell', {h['block'] for h in hits})

    def test_natural_ground_is_not_a_structure(self):
        # Levelling a hillside is terrain work; demolishing a wall is not. Stone
        # and dirt must never read as a ceiling violation or every map fails.
        world = FakeWorld([((32, 0), 'minecraft:stone'),
                           ((40, 0), 'minecraft:dirt'),
                           ((48, 0), 'minecraft:oak_log')])
        self.assertEqual([], opening_ceiling.survey(world, 0, 0),
                         'stone, dirt and a tree trunk are not buildings')

    def test_only_the_envelope_is_searched(self):
        world = FakeWorld([((opening_ceiling.DEFAULT_RADIUS + 64, 0), 'minecraft:bell')])
        self.assertEqual([], opening_ceiling.survey(world, 0, 0),
                         'a village well outside the opening is not a ceiling violation')

    def test_what_is_not_checked_is_stated(self):
        # The iron rule needs a declared equipment target that does not exist.
        # Silence would look like coverage.
        self.assertIn('accessible_iron', opening_ceiling.UNCHECKED)
        self.assertIn('carrots', opening_ceiling.UNCHECKED)


if __name__ == '__main__':
    unittest.main()
