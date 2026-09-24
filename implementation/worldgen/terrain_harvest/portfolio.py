"""Step 4: derive a map's regenerative portfolio from its own geography.

Sources are authored by hand into `config.yml` against the frozen Alpha map,
so a generated map has none and `Renewables` refuses on one rather than
applying Alpha's coordinates. This derives them instead.

MACHINERY, NOT CALIBRATION, and the distinction has to hold. Exact patch
sizes, herd sizes and regeneration cadence are OPEN in maps.md; the per-crop
effective-nourishment figures are recalled from an unrecoverable conversation
and need re-deriving. So every quantity below is a DECLARED FIXTURE and the
ordering is what carries the design.

WHAT THE RECOVERED VOCABULARY ACTUALLY SAYS (maps.md, 13 September, recovered
24 September):

    near     chickens, rabbits   -- rabbits deliberately awkward, so near is
                                   NOT the same as staple
    farther  cows, pigs          -- "cows follow geography", "pigs follow
                                   crop access"
    deeper   carrots, potatoes   -- founder crops: food AND planting stock AND
                                   husbandry control
    special  sheep               -- off the ladder; needs reliable strategic
                                   access, because wool leads to Banners and
                                   Exploration

    > Depth increases economic SPECIFICITY, not simply raw yield.

THREE THINGS THIS MUST NOT DO.

It must not require named species in both openings. That would reintroduce
ecological symmetry through the back door, against the rule that functional
opportunity is balanced while Wilderness is not mirrored, and against
"discovery uncertainty is good, existence uncertainty is often bad" -- which
permits a resource to exist without being mirrored or equally abundant. One
opening satisfying Development with rabbits and the other with cows is the
intended case.

It must not sprinkle. maps.md: "do not sprinkle enough regenerative nodes onto
empty cells to satisfy a percentage." A cell hosts a manifestation only where
its own measured vocabulary already supports that kind. An absent opportunity
is a real answer.

It must not equalise counts. Patch and population scale should reflect
resource EFFECTIVENESS -- output, propagation, processing, secondary products
-- so a carrot patch and a wheat patch need not be the same size to be
comparable opportunities.

ORDINARY VANILLA INCIDENCE SURVIVES BENEATH THIS. These are deliberate
strategic manifestations layered over background Minecraft ecology, not a set
of generation bans. A cow generating naturally in a Hinterland is not a fault.
"""
from __future__ import annotations

# Depth BANDS ARE RANKS, NOT DISTANCES. The recovered near/farther/deeper was
# explicitly an A/B/C ordering with no block figures decided, and maps.md marks
# depth bands OPEN. So a cell's band comes from its rank among the measured
# cells of this map, never from a constant. NON-CANON FIXTURE: the thirds.
BANDS = ('near', 'farther', 'deeper')

# NEAR IS THE OPENING, not a percentile of the map.
#
# A first version made near the first third of cells by cost, and measured
# against five real maps that put the near band at costs of 625 to 1328 while
# the opening the rest of the compiler uses ends at 120. Only 3 to 7 cells of
# 238 lie inside it. Two fixtures describing entirely different regions, and
# "near" then meant something different here from everywhere else.
#
# So near is bounded by the same `opening_cost` `opening_access`,
# `opening_floor` and `resource_validity` use, and farther/deeper split what
# remains. NON-CANON FIXTURE, but now ONE fixture instead of two that disagree.
BAND_QUANTILES = (0.5,)          # splits the non-opening remainder

# Which strategic manifestations belong at which rank. Membership constrains
# WHERE a kind may manifest; it does not oblige any cell to hold one.
VOCABULARY = {
    'near': ('chicken', 'rabbit'),
    'farther': ('cow', 'pig'),
    'deeper': ('carrot', 'potato'),
}

# Sheep sit outside the ladder by the recovered decision: reliable strategic
# access, because wool -> Banners -> Exploration makes them a material source
# rather than livestock. Placed where BOTH teams can reach comparably, which is
# a different rule from the others and is why it is not in VOCABULARY.
STRATEGIC_ACCESS = ('sheep',)

TYPES = {'chicken': 'ANIMAL', 'rabbit': 'ANIMAL', 'cow': 'ANIMAL',
         'pig': 'ANIMAL', 'sheep': 'ANIMAL', 'carrot': 'CROP',
         'potato': 'CROP', 'wheat': 'CROP'}

# HERD CORE + HARVESTABLE SURPLUS, from the recovered animal model:
#     Population        = Herd Core + Harvestable Surplus
#     Opportunity Value = Surplus + Reproductive Capital + Ongoing Producer
# Killing an animal and maintaining a population are different economic acts,
# so a manifestation states both rather than one number a player can zero.
#
# EVERY FIGURE HERE IS A DECLARED FIXTURE. None is calibrated, none is
# recovered, and the crop scale deliberately does NOT follow the recalled
# nourishment ratios -- those describe a processed ceiling, and carrots and
# potatoes are their own planting stock, so harvest and propagation are coupled
# in a way a per-plant food number does not capture.
SCALE = {
    'chicken': {'core': 4, 'surplus': 6, 'radius': 24, 'recover_ticks': 12000},
    'rabbit':  {'core': 3, 'surplus': 3, 'radius': 24, 'recover_ticks': 12000},
    'cow':     {'core': 4, 'surplus': 4, 'radius': 32, 'recover_ticks': 18000},
    'pig':     {'core': 4, 'surplus': 4, 'radius': 32, 'recover_ticks': 18000},
    'sheep':   {'core': 4, 'surplus': 4, 'radius': 32, 'recover_ticks': 18000},
    'carrot':  {'core': 0, 'surplus': 9, 'radius': 16, 'recover_ticks': 24000},
    'potato':  {'core': 0, 'surplus': 9, 'radius': 16, 'recover_ticks': 24000},
    'wheat':   {'core': 0, 'surplus': 12, 'radius': 16, 'recover_ticks': 18000},
}


def _band_of(cost: float, opening_cost: float, rank_beyond: float) -> str:
    """Which band a cell falls in, for one team.

    Inside the opening is near. Beyond it, the remainder splits in half:
    farther and deeper. The split is a rank so it adapts to a map's own
    spread, which is what the recovered A/B/C ordering was.
    """
    if cost <= opening_cost:
        return 'near'
    return 'farther' if rank_beyond <= BAND_QUANTILES[0] else 'deeper'


def _supports(cell, kind: str) -> bool:
    """Does this cell's OWN measured ecology already support the kind?

    The no-sprinkling rule in code. `regenerative_vocabulary` comes from what
    `opportunity_map` actually observed in the cell -- vegetation, fauna and
    hostiles present -- so a kind absent from it is absent from the ground.
    """
    vocab = {str(v).lower() for v in (cell.get('regenerative_vocabulary') or ())}
    fauna = {str(k).lower() for k in (cell.get('fauna') or {})}
    veg = {str(k).lower() for k in (cell.get('vegetation') or {})}
    return any(kind in token for token in (vocab | fauna | veg))


def derive(cells, *, teams=('north', 'south'), per_band: int = 3,
           world: str | None = None, opening_cost: float = 120.0) -> dict:
    """A portfolio of Source specs, derived rather than authored.

    `cells` are `cell_grid.build` rows carrying `strategic_depth_cost` per
    team and the opportunity counts. Returns specs the runtime can bind, plus
    the reasoning, because a portfolio nobody can audit is a table again.
    """
    usable = [c for c in (cells or ())
              if any(v is not None for v in (c.get('strategic_depth_cost') or {}).values())]
    if not usable:
        return {'derived': False, 'sources': [],
                'why': 'no cells carry per-team depth; run cell_grid.build first'}

    # RANKED PER TEAM, because "near" means near to A TEAM.
    #
    # A first version ranked by the minimum cost across teams, so a cell 30
    # from north and 900 from south counted as near -- near to someone. With a
    # cap of three sources per band per kind, all three could then fall on one
    # team's side, and the other team's opening got nothing. Tested against
    # five real compiled maps that produced portfolios of 12 to 18 sources
    # where NEITHER team could reach one, while a 6-source map passed. The
    # counts were fine and the distribution was not.
    sources, skipped = [], []
    for team in teams:
        def cost(c, _t=team):
            return (c.get('strategic_depth_cost') or {}).get(_t)

        ordered = sorted((c for c in usable if cost(c) is not None), key=cost)
        beyond = [c for c in ordered if cost(c) > opening_cost]
        n_beyond = len(beyond) or 1
        for cell in ordered:
            this = cost(cell)
            rank = ((beyond.index(cell) + 1) / n_beyond) if this > opening_cost else 0.0
            band = _band_of(this, opening_cost, rank)
            for kind in VOCABULARY[band]:
                if not _supports(cell, kind):
                    skipped.append({'cell': cell.get('cell'), 'kind': kind,
                                    'band': band, 'team': team,
                                    'why': 'the cell\'s own ecology does not '
                                           'support it; an absent opportunity '
                                           'is a real answer'})
                    continue
                if sum(1 for s in sources if s['band'] == band
                       and s['kind'] == kind and s.get('near_team') == team) >= per_band:
                    continue
                if any(s['cell'] == cell.get('cell') and s['kind'] == kind
                       for s in sources):
                    continue          # one manifestation per cell per kind
                spec = SCALE[kind]
                x, z = cell.get('centroid') or cell.get('world_origin') or (0, 0)
                sources.append({
                    'id': f"{band}_{kind}_{team}_{cell.get('cell')}",
                    'cell': cell.get('cell'),
                    'type': TYPES[kind], 'kind': kind, 'band': band,
                    'near_team': team,
                    'world': world,
                    'x': int(x), 'y': int(cell.get('mean_surface_y') or 64),
                    'z': int(z),
                    'radius': spec['radius'],
                    # `capacity` is what the runtime may manifest. The core is
                    # stated beside it so the two are not conflated: a
                    # population reduced to its core is depleted, not extinct.
                    'capacity': spec['core'] + spec['surplus'],
                    'herd_core': spec['core'],
                    'harvestable_surplus': spec['surplus'],
                    'recover_ticks': spec['recover_ticks'],
                    'strategic_depth_cost': cell.get('strategic_depth_cost'),
                })

    # Sheep: reliable strategic access, so placed where the two teams' costs
    # are closest rather than by band.
    def disparity(c):
        vals = [v for v in (c.get('strategic_depth_cost') or {}).values()
                if v is not None]
        if len(vals) < 2:
            return None
        return abs(vals[0] - vals[1]) / (sum(vals) or 1)

    fair = sorted((c for c in usable
                   if disparity(c) is not None and _supports(c, 'sheep')),
                  key=disparity)
    for cell in fair[:per_band]:
        spec = SCALE['sheep']
        x, z = cell.get('centroid') or cell.get('world_origin') or (0, 0)
        sources.append({
            'id': f"access_sheep_{cell.get('cell')}", 'cell': cell.get('cell'),
            'near_team': None, 'type': 'ANIMAL',
            'kind': 'sheep', 'band': 'strategic_access', 'world': world,
            'x': int(x), 'y': int(cell.get('mean_surface_y') or 64), 'z': int(z),
            'radius': spec['radius'],
            'capacity': spec['core'] + spec['surplus'],
            'herd_core': spec['core'], 'harvestable_surplus': spec['surplus'],
            'recover_ticks': spec['recover_ticks'],
            'team_cost_disparity': round(disparity(cell), 4),
            'strategic_depth_cost': cell.get('strategic_depth_cost'),
        })

    by_band = {}
    for s in sources:
        by_band.setdefault(s['band'], []).append(s['kind'])
    return {
        'derived': True,
        'sources': sources,
        'cells_ranked': len(usable),
        'by_band': {b: sorted(set(k)) for b, k in by_band.items()},
        'skipped': skipped[:40],
        'skipped_total': len(skipped),
        'quantities_are': 'DECLARED FIXTURES. Patch sizes, herd sizes and '
                          'recovery cadence are OPEN in maps.md and the '
                          'recalled nourishment figures need re-derivation; '
                          'the ORDERING is what is recovered, not the numbers.',
        'opening_cost': opening_cost,
        'bands_are': 'near is the opening the rest of the compiler uses; '
                     'farther and deeper split the remainder by RANK among '
                     'this map\'s own cells, not by block distance. The '
                     'recovered near/farther/deeper was an A/B/C ordering '
                     'with no distances decided.',
        'no_species_floor': 'nothing here requires a kind to appear in either '
                            'opening. Requiring named species in both would '
                            'reintroduce ecological symmetry that doctrine '
                            'denies.',
        'beneath_this': 'ordinary vanilla incidence, which these strategic '
                        'manifestations are layered over rather than replacing.',
    }


def certify(derived: dict, cells=None, *, opening_cost: float = 120.0,
            teams=('north', 'south')) -> dict:
    """Is this portfolio enough for a match to start, and on what grounds?

    A FUNCTIONAL FLOOR, NOT A COUNT AND NOT A SPECIES LIST. maps.md requires
    the Hinterland to permit every fundamental verb, and Development needs
    something developable, so the question is whether EACH TEAM can reach a
    regenerative opportunity in its own opening -- not how many sources the
    map has, and not which kinds.

    Both halves of that matter. A count would let one team's twelve sources
    cover for the other team's none. A species list would reintroduce the
    ecological symmetry doctrine denies, where one opening must hold the same
    kinds as the other.

    Derived portfolios really do differ this much: across five compiled maps
    they ranged from six sources (rabbit and sheep only, on ground whose
    vocabulary offered nothing else) to eighteen. The small one is not
    automatically a failure -- it is a failure only if a team cannot reach
    one.
    """
    if not (derived or {}).get('derived'):
        return {'certified': False, 'problems': [
            {'code': 'NO_PORTFOLIO_DERIVED',
             'detail': (derived or {}).get('why', 'no portfolio')}]}

    sources = derived.get('sources') or []
    reach, problems = {}, []
    developable = set(sum(VOCABULARY.values(), ())) | set(STRATEGIC_ACCESS)
    for team in teams:
        near_cells = [c for c in (cells or ())
                      if (c.get('strategic_depth_cost') or {}).get(team) is not None
                      and c['strategic_depth_cost'][team] <= opening_cost]
        # BACKGROUND INCIDENCE COUNTS, and that is the whole correction.
        #
        # An earlier version asked whether a DERIVED source sat in the
        # opening, and failed four of five real maps. Their openings were full
        # of cows, pigs and sheep -- which the recovered vocabulary places in
        # the FARTHER band, so the derivation rightly declined to manifest one
        # there and the floor then read the opening as barren.
        #
        # It is not barren. maps.md is explicit that ordinary vanilla
        # incidence survives beneath strategic manifestations and that "a cow
        # generating naturally in a Hinterland is not a fault". The two layers
        # were being conflated: the ordering governs where a DELIBERATE
        # manifestation belongs, not whether a team can begin Development.
        wild = sorted({k for c in near_cells
                       for k in ((c.get('fauna') or {}) | (c.get('vegetation') or {}))
                       if any(d in str(k).lower() for d in developable)})
        placed = [s for s in sources
                  if (s.get('strategic_depth_cost') or {}).get(team) is not None
                  and s['strategic_depth_cost'][team] <= opening_cost]
        reach[team] = {'opening_cells': len(near_cells),
                       'wild_developable': wild,
                       'manifestations_in_opening': len(placed),
                       'manifestation_kinds': sorted({s['kind'] for s in placed})}
        if not wild and not placed:
            problems.append({
                'code': 'NO_OPENING_RENEWABLE', 'team': team,
                'detail': f'{team} has neither a wild developable population '
                          f'nor a derived manifestation within its opening, '
                          f'so Development cannot begin there. maps.md '
                          f'requires the Hinterland to permit every '
                          f'fundamental verb.'})
    return {
        'certified': not problems,
        'problems': problems,
        'sources': len(sources),
        'per_team': reach,
        'floor_is': 'each team reaching one developable opportunity in its '
                    'own opening, WILD OR MANIFESTED. Not a count -- one '
                    'team\'s twelve cannot cover another team\'s none -- and '
                    'not a species list, which would require both openings to '
                    'hold the same kinds. Ordinary vanilla incidence counts, '
                    'because strategic manifestations are layered over it '
                    'rather than replacing it.',
        'opening_cost': opening_cost,
    }
