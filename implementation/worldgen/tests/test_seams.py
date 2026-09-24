import unittest

from terrain_harvest import (authorability, discovered, opening_floor,
                             resource_validity, traversability)


def cell(team_costs, **kw):
    c = {'cell': [0, 0], 'world_origin': [0, 0],
         'strategic_depth_cost': team_costs}
    c.update(kw)
    return c


class Seam1Ceiling(unittest.TestCase):
    """Resource validity is a ceiling check, because doctrine forbids a floor."""

    def test_a_worksite_scale_concentration_in_the_opening_is_refused(self):
        r = resource_validity.assess(
            [cell({'north': 30.0, 'south': 500.0}, ore={'iron_ore': 46})])
        self.assertTrue(r['rejects'])
        self.assertEqual(r['progression_breaking'][0]['code'],
                         'OPENING_CEILING_WORKSITE_SCALE')

    def test_the_same_concentration_at_depth_is_not_refused(self):
        """maps.md: empty or weak deep terrain is legitimate and density is
        not monotonic with depth, so depth carries no ceiling."""
        r = resource_validity.assess(
            [cell({'north': 900.0, 'south': 950.0}, ore={'iron_ore': 46})])
        self.assertFalse(r['rejects'])

    def test_no_cell_is_ever_refused_for_holding_too_little(self):
        r = resource_validity.assess(
            [cell({'north': 10.0, 'south': 10.0}, ore={})])
        self.assertFalse(r['rejects'])
        self.assertIn('ceiling check', r['ceiling_only'])

    def test_copper_cannot_break_progression(self):
        """Its Worksite was removed precisely because it belongs in the
        opening economy, so it has no tier to skip."""
        r = resource_validity.assess(
            [cell({'north': 10.0, 'south': 500.0}, ore={'copper_ore': 60})])
        self.assertFalse(r['rejects'])
        self.assertIsNone(resource_validity.BUDGETS['copper']['worksite_tier'])

    def test_every_budget_cites_its_source(self):
        for material in resource_validity.BUDGETS:
            self.assertIn('2026-09-14',
                          resource_validity.classify(1, material)['budget_source'])

    def test_an_unbudgeted_material_is_reported_not_judged(self):
        c = resource_validity.classify(999, 'redstone')
        self.assertEqual(c['verdict'], 'unbudgeted')


class OpeningExclusions(unittest.TestCase):
    """Carrots are canon in the ceiling and were never checked."""

    def test_carrots_in_the_opening_are_refused(self):
        r = resource_validity.assess(
            [cell({'north': 30.0, 'south': 900.0}, vegetation={'carrots': 6})])
        self.assertTrue(r['rejects'])
        self.assertEqual(r['progression_breaking'][0]['code'],
                         'OPENING_CEILING_EXCLUDED_RESOURCE')

    def test_carrots_at_depth_are_fine(self):
        """The ceiling keeps carrots out of a compact envelope with Wilderness
        on every side. It does not make them a deep resource."""
        r = resource_validity.assess(
            [cell({'north': 900.0, 'south': 950.0}, vegetation={'carrots': 6})])
        self.assertFalse(r['rejects'])

    def test_other_starter_crops_are_not_excluded(self):
        r = resource_validity.assess(
            [cell({'north': 20.0}, vegetation={'wheat': 40, 'potatoes': 9})])
        self.assertFalse(r['rejects'])

    def test_the_unchecked_exclusion_is_named_rather_than_passing(self):
        r = resource_validity.assess([cell({'north': 20.0}, ore={})])
        self.assertTrue(any('equipment' in x for x in
                            r['opening_exclusions_not_checked']))


class Seam2Floor(unittest.TestCase):
    def test_all_seven_verbs_are_named(self):
        """An earlier reading listed five and dropped Development and
        Exploration -- the two with named infrastructure anchors."""
        self.assertEqual(
            set(opening_floor.VERBS),
            {'extraction', 'construction', 'development', 'exploration',
             'logistics', 'combat', 'production'})

    def test_a_barren_opening_blocks_the_verbs_it_cannot_evidence(self):
        cells = [cell({'north': 10.0, 'south': 900.0},
                      ore={'iron_ore': 4, 'copper_ore': 9},
                      vegetation={'oak_log': 9}, fauna={'cow': 1},
                      hostiles={'zombie': 1}),
                 cell({'north': 900.0, 'south': 10.0}, ore={'iron_ore': 4},
                      vegetation={}, fauna={}, hostiles={})]
        r = opening_floor.permits(cells)
        blocked = {(b['team'], b['verb']) for b in r['blocked']}
        self.assertIn(('south', 'combat'), blocked)
        self.assertIn(('south', 'development'), blocked)
        self.assertIn(('south', 'logistics'), blocked)
        self.assertNotIn(('north', 'combat'), blocked)

    def test_production_is_reported_unevidenced_rather_than_assumed(self):
        cells = [cell({'north': 10.0}, ore={'iron_ore': 9})]
        r = opening_floor.permits(cells)
        self.assertIsNone(r['per_team']['north']['verbs']['production']['permitted'])
        self.assertIn('production', r['unevidenced_verbs'])

    def test_it_counts_nothing(self):
        """maps.md makes the floor qualitative and invents no counts."""
        self.assertIn('presence only', opening_floor.QUALITATIVE)
        one = opening_floor.permits([cell({'north': 10.0}, hostiles={'zombie': 1})])
        many = opening_floor.permits([cell({'north': 10.0}, hostiles={'zombie': 99})])
        self.assertEqual(one['per_team']['north']['verbs']['combat']['permitted'],
                         many['per_team']['north']['verbs']['combat']['permitted'])

    def test_the_floor_requires_opportunity_not_named_species(self):
        """A required-species checklist would reintroduce ecological symmetry
        through the back door. maps.md balances functional opportunity while
        Wilderness is explicitly not mirrored, and species/depth placement is
        OPEN, so two openings may satisfy Development with different animals.
        """
        north = cell({'north': 10.0, 'south': 900.0}, fauna={'rabbit': 2},
                     vegetation={'oak_log': 5}, ore={'copper_ore': 2},
                     hostiles={'zombie': 1})
        south = cell({'north': 900.0, 'south': 10.0}, fauna={'cow': 2},
                     vegetation={'oak_log': 5}, ore={'copper_ore': 2},
                     hostiles={'zombie': 1})
        r = opening_floor.permits([north, south])
        for team in ('north', 'south'):
            self.assertTrue(r['per_team'][team]['verbs']['development']['permitted'])
        self.assertFalse(r['blocked'])

    def test_construction_does_not_require_the_construction_block_category(self):
        why = opening_floor.VERBS['construction']['why']
        self.assertIn('not required for valid construction', why)


class Seam4Traversability(unittest.TestCase):
    def test_terrain_difficulty_is_already_in_the_cost_model(self):
        for term in ('water', 'canopy', 'elevation rise'):
            self.assertIn(term, ' '.join(traversability.COVERED_BY_COST_MODEL))

    def test_danger_and_sightlines_are_named_as_absent(self):
        joined = ' '.join(traversability.NOT_COVERED)
        self.assertIn('hostile exposure', joined)
        self.assertIn('visibility', joined)

    def test_equal_travel_cost_is_not_asserted_as_a_target(self):
        tr = {'sites': [{'id': 'n', 'kind': 'homeland', 'team': 'north'},
                        {'id': 's', 'kind': 'homeland', 'team': 'south'},
                        {'id': 'c1'}],
              'cost_matrix': {'n': {'c1': 100}, 's': {'c1': 400}}}
        r = traversability.profile(tr)
        self.assertGreater(r['comparison']['median_cost_gap'], 0.5)
        self.assertIn('not the goal', r['comparison']['not_a_target'])
        self.assertEqual(r['bound'], 'none.')


class Seam5Authorability(unittest.TestCase):
    def setUp(self):
        self.v = resource_validity.assess([
            cell({'north': 30.0, 'south': 900.0}, ore={'iron_ore': 30}),
            cell({'north': 900.0, 'south': 40.0}, ore={'iron_ore': 6})])

    def test_the_gap_is_per_team_not_per_opening(self):
        """`in_opening` is a MINIMUM across teams -- right for a ceiling,
        wrong here. Reading it credited both teams with every opening cell
        and reported a real 30/6 split as no gap at all."""
        g = authorability.gap(self.v)
        self.assertEqual(g['per_material']['iron']['per_team'],
                         {'north': 30, 'south': 6})
        self.assertGreater(g['per_material']['iron']['gap'], 0.6)

    def test_an_unchanged_terrain_gap_is_refused_however_a_scalar_moved(self):
        """The 930010639 failure: 0.3173 became a passing 0.0161 with the
        gap itself untouched."""
        g = authorability.gap(self.v)
        self.assertEqual(authorability.verify(g, g)['code'],
                         'TERRAIN_GAP_UNCHANGED')

    def test_a_closed_gap_passes(self):
        g = authorability.gap(self.v)
        after = {'per_material': {'iron': {'gap': 0.05}}}
        self.assertIsNone(authorability.verify(g, after)['code'])

    def test_access_disparity_is_marked_unclosable_by_renewables(self):
        access = {'comparison': {'exit_count_gap': 0.4, 'widest_exit_gap': 0.5}}
        g = authorability.gap(self.v, access=access)
        self.assertIn('not closable', ' '.join(
            [g['exit_capacity']['note']] + g['not_closable_by_renewables']).lower()
            .replace('does not answer it', 'not closable'))

    def test_a_blocked_verb_precedes_equalisation(self):
        floor = {'blocked': [{'team': 'south', 'verb': 'combat'}]}
        g = authorability.gap(self.v, floor=floor)
        self.assertIn('floor failure', g['floor_first'])

    def test_no_intervention_budget_is_set(self):
        g = authorability.gap(self.v)
        self.assertIn('none set', authorability.verify(g, g)['budget'])


class Seam6Discovered(unittest.TestCase):
    def test_density_is_per_material_not_a_single_scalar(self):
        v = resource_validity.assess(
            [cell({'north': 30.0}, ore={'iron_ore': 46, 'diamond_ore': 3})])
        d = discovered.resource_density(v)
        self.assertIn('iron', d['per_material'])
        self.assertIn('deliberately absent', d['single_scalar'])

    def test_the_whole_map_reference_doubles_the_per_team_budget(self):
        v = resource_validity.assess([cell({'north': 30.0}, ore={'iron_ore': 1})])
        iron = discovered.resource_density(v)['per_material']['iron']
        self.assertEqual(iron['whole_map_reference'],
                         [b * 2 for b in iron['ordinary_per_team_budget']])

    def test_scale_reports_the_scalars_and_names_nothing(self):
        s = discovered.scale({'area_blocks2': 912384, 'area_over_base': 1.0,
                              'axes': {'z': {'homebase_max_separation_blocks': 664}}})
        self.assertIsNone(s['named_scale'])
        self.assertEqual(s['homebase_max_separation_blocks']['z'], 664)

    def test_density_is_not_claimed_to_be_practical_opportunity(self):
        v = resource_validity.assess([cell({'north': 30.0}, ore={'iron_ore': 1})])
        self.assertIn('caves.py',
                      discovered.resource_density(v)['not_yet_practical'])


if __name__ == '__main__':
    unittest.main()
