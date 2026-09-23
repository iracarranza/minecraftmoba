"""The spatial roles, the objective contract, and the things that stay unknown."""
import unittest

from terrain_harvest import spatial_contract as sc
from terrain_harvest import objective_forms as of


class SpatialRoles(unittest.TestCase):

    def roles(self, core=1, hinter=1, wild=8):
        cells = {}
        i = 0
        for role, n in ((sc.CORE, core), (sc.HINTERLAND, hinter), (sc.WILDERNESS, wild)):
            for _ in range(n):
                cells[i] = role; i += 1
        return sc.classify(cells)

    def test_wilderness_is_the_residual_and_may_surround_the_hinterland(self):
        s = self.roles()
        self.assertTrue(s['residual_is_wilderness'])
        self.assertGreater(s['fractions'][sc.WILDERNESS], s['fractions'][sc.HINTERLAND])
        self.assertEqual([], sc.hinterland_problems(s, 0.5))

    def test_the_hinterland_cannot_consume_the_team_end(self):
        # The superseded model made a team's half its opening ground. A
        # Hinterland that reaches the end of the map is that model returning.
        s = self.roles(core=1, hinter=49, wild=0)
        problems = sc.hinterland_problems(s, 0.5)
        self.assertTrue(any('compact opening envelope' in p for p in problems))
        self.assertTrue(any('surrounded, including poleward' in p for p in problems))

    def test_nothing_compares_the_two_ends_for_similarity(self):
        # Wilderness is not mirrored. A socket check that demanded matching
        # terrain would be the balance target doctrine explicitly rejects, so
        # the signature takes each socket's own viability and nothing else.
        good = {'core_fits': True, 'landscape_surgery': ()}
        different = {'core_fits': True, 'landscape_surgery': ()}
        self.assertEqual([], sc.socket_problems(good, different))

    def test_a_socket_needing_landscape_surgery_is_rejected_not_repaired(self):
        problems = sc.socket_problems(
            {'core_fits': True, 'landscape_surgery': ('cliff reconstruction',)},
            {'core_fits': True, 'landscape_surgery': ()})
        self.assertEqual(1, len(problems))
        self.assertIn('reject the socket rather than repair', problems[0])

    def test_unknown_role_is_an_error_not_a_default(self):
        with self.assertRaises(ValueError):
            sc.classify({0: 'homeland'})


class Objectives(unittest.TestCase):

    def test_defensive_objectives_belong_to_wilderness_and_the_fountain_to_core(self):
        self.assertEqual([], sc.objective_placement_problems({
            'pillager_outpost': sc.WILDERNESS,
            'nether_bastion': sc.WILDERNESS,
            'end_spike': sc.WILDERNESS,
            'aether_fountain': sc.CORE}))

    def test_an_objective_in_the_hinterland_is_a_violation(self):
        problems = sc.objective_placement_problems({'end_spike': sc.HINTERLAND})
        self.assertIn('Wilderness structures', problems[0])

    def test_the_order_is_ordinal_and_spacing_is_free(self):
        # Equal gaps, a straight lane and mirrored coordinates are all NOT
        # required: only midline -> Outpost -> Bastion -> Spike -> Fountain.
        self.assertEqual([], of.ordinal_ok(
            {'pillager_outpost': 0.99, 'nether_bastion': 0.98,
             'end_spike': 0.10, 'aether_fountain': 0.01}))

    def test_an_out_of_order_objective_is_reported(self):
        problems = of.ordinal_ok({'pillager_outpost': 0.2, 'end_spike': 0.8})
        self.assertEqual(1, len(problems))

    def test_all_three_coexist_so_an_absent_one_fails_certification(self):
        result = of.certify(['pillager_outpost', 'nether_bastion'])
        self.assertFalse(result['certified'])
        self.assertTrue(any('end_spike absent' in m for m in result['missing_evidence']))

    def test_the_old_end_tower_is_historical_and_is_not_renamed(self):
        self.assertTrue(of.is_historical('end_tower'))
        self.assertNotIn('end_tower', of.FORMS)
        evidence = of.missing_evidence('end_tower')
        self.assertIn('superseded by end_spike', evidence[0])

    def test_no_current_form_has_a_guessed_footprint(self):
        # The spec says dimensions must be measured from the selected vanilla
        # form. An invented span here would travel into siting and clearance
        # wearing the clothes of evidence.
        for sid, form in of.FORMS.items():
            self.assertIsNone(form['span_samples'], sid)
            self.assertTrue(of.missing_evidence(sid))


class LairAndCeiling(unittest.TestCase):

    def test_there_is_exactly_one_lair(self):
        self.assertEqual([], sc.lair_problems(['basin']))
        self.assertIn('exactly one Lair', sc.lair_problems(['a', 'b'])[0])
        self.assertIn('unconfigured', sc.lair_problems([])[0])

    def test_lair_access_is_measured_from_both_teams_with_no_threshold(self):
        a = sc.lair_access_asymmetry(1000, 1000)
        self.assertEqual(0.0, a['asymmetry'])
        self.assertEqual('unknown', a['acceptable'])
        b = sc.lair_access_asymmetry(800, 1200)
        self.assertAlmostEqual(0.4, b['asymmetry'])
        self.assertEqual('unknown', b['acceptable'],
                         'no canonical parity threshold has been established')

    def test_unmeasured_reach_does_not_become_a_number(self):
        self.assertIsNone(sc.lair_access_asymmetry(0, 0)['asymmetry'])

    def test_known_ceiling_exclusions_are_recognised(self):
        found = sc.ceiling_findings({'village': True, 'carrot': True,
                                     'equipment_sufficient_iron': False})
        self.assertEqual(2, len(found['violations']))
        self.assertEqual([], found['unmeasured'])

    def test_unmeasured_iron_stays_unmeasured_rather_than_passing(self):
        # "No iron seen" is not "not enough iron to equip a player", and basic
        # Extraction must remain possible, so a bare count would fail the wrong
        # maps. Without a declared equipment target this stays unknown.
        found = sc.ceiling_findings({'village': False, 'carrot': False})
        self.assertEqual([], found['violations'])
        self.assertEqual(['equipment_sufficient_iron'], found['unmeasured'])

    def test_the_exclusion_list_is_a_seam_not_a_closed_set(self):
        self.assertFalse(sc.ceiling_findings({})['exhaustive'])

    def test_the_hinterland_permits_every_fundamental_verb_without_quotas(self):
        # Qualitative by design: the spec forbids one quota per archetype.
        self.assertEqual(7, len(sc.FUNDAMENTAL_VERBS))
        for verb in ('construction', 'extraction', 'development', 'production',
                     'exploration', 'logistics', 'combat'):
            self.assertIn(verb, sc.FUNDAMENTAL_VERBS)


if __name__ == '__main__':
    unittest.main()
