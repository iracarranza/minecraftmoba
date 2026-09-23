"""VERIFIED is not READY, and the difference is runtime bindings."""
import unittest

from terrain_harvest import readiness


def bindings(**overrides):
    base = {
        'world': {'name': 'gen', 'map_type': 'default'},
        'homelands': {'north': [0, 0, 0, 0], 'south': [1, 1, 1, 1]},
        'fountains': {'north': [0, 64, 0], 'south': [1, 64, 1]},
        'objectives': {t: {k: [0, 0] for k in
                           ('pillager_outpost', 'nether_bastion', 'end_spike')}
                       for t in ('north', 'south')},
        'lair': {'anchor': {'xyz': [5, 65, 5]}, 'count': 1},
        'worksites': [{'id': f'ws_{i}'} for i in range(12)],
    }
    base.update(overrides)
    return base


class Readiness(unittest.TestCase):

    def codes(self, b):
        return [p['code'] for p in readiness.certify(b)['problems']]

    def test_a_complete_realization_certifies(self):
        self.assertTrue(readiness.certify(bindings())['certified'])

    def test_a_verified_map_with_an_unmanifested_lair_is_not_ready(self):
        # The exact defect this module exists for. The compiler was not wrong
        # about the geography; nothing was asking whether the runtime could
        # bind to it, so the cadence would have reached UNCONFIGURED on night 2
        # of a map the pool called READY.
        self.assertIn('LAIR_UNCONFIGURED', self.codes(bindings(lair={'count': 1})))

    def test_there_is_exactly_one_lair(self):
        self.assertIn('LAIR_NOT_SINGULAR',
                      self.codes(bindings(lair={'anchor': {'xyz': [0, 0, 0]}, 'count': 2})))

    def test_a_missing_fountain_blocks_ready(self):
        # Respawn and reconstruction bind to it; without one a team cannot play.
        self.assertIn('NO_FOUNTAIN', self.codes(bindings(fountains={'north': [0, 64, 0]})))

    def test_every_defensive_objective_must_bind(self):
        partial = {'north': {'pillager_outpost': [0, 0]},
                   'south': {k: [0, 0] for k in
                             ('pillager_outpost', 'nether_bastion', 'end_spike')}}
        codes = self.codes(bindings(objectives=partial))
        self.assertEqual(2, codes.count('NO_OBJECTIVE_BINDING'))

    def test_the_worksite_portfolio_must_cover_three_nights(self):
        self.assertIn('WORKSITE_PORTFOLIO_TOO_SMALL',
                      self.codes(bindings(worksites=[{'id': 'only'}])))

    def test_permanent_activation_drains_the_pool_so_three_sites_is_not_enough(self):
        # Three sites passed while sunrise returned an activated Worksite to
        # Dormant and refilled the eligible pool. Activation is permanent now,
        # so the pool only shrinks: a three-site map opens nothing on night 5.
        three = [{'id': f'ws_{i}'} for i in range(3)]
        self.assertIn('WORKSITE_PORTFOLIO_TOO_SMALL', self.codes(bindings(worksites=three)))
        seven = [{'id': f'ws_{i}'} for i in range(readiness.MIN_WORKSITES)]
        self.assertNotIn('WORKSITE_PORTFOLIO_TOO_SMALL', self.codes(bindings(worksites=seven)))

    def test_nothing_missing_is_silently_defaulted(self):
        codes = self.codes({})
        self.assertIn('MISSING_BINDING', codes)
        self.assertFalse(readiness.certify({})['certified'])

    def test_readiness_is_about_bindings_not_quality(self):
        # A VerifiedMap that fails here is still a VerifiedMap. Readiness is
        # not a second opinion about the geography.
        note = readiness.certify(bindings())['note']
        self.assertIn('not about map quality', note)


if __name__ == '__main__':
    unittest.main()
