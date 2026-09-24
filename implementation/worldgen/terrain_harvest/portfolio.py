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
BAND_QUANTILES = (0.33, 0.66)

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


def _band_of(rank: float) -> str:
    if rank <= BAND_QUANTILES[0]:
        return 'near'
    if rank <= BAND_QUANTILES[1]:
        return 'farther'
    return 'deeper'


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
           world: str | None = None) -> dict:
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

    # Rank by the depth a team would actually travel: the NEARER team's cost,
    # because a cell deep for north and shallow for south is shallow to play.
    def nearest(c):
        vals = [v for v in (c.get('strategic_depth_cost') or {}).values()
                if v is not None]
        return min(vals) if vals else None

    ordered = sorted((c for c in usable if nearest(c) is not None), key=nearest)
    total = len(ordered)
    sources, skipped = [], []
    for index, cell in enumerate(ordered):
        band = _band_of((index + 1) / total)
        for kind in VOCABULARY[band]:
            if not _supports(cell, kind):
                skipped.append({'cell': cell.get('cell'), 'kind': kind,
                                'band': band,
                                'why': 'the cell\'s own ecology does not '
                                       'support it; an absent opportunity is '
                                       'a real answer'})
                continue
            if sum(1 for s in sources
                   if s['band'] == band and s['kind'] == kind) >= per_band:
                continue
            spec = SCALE[kind]
            x, z = cell.get('centroid') or cell.get('world_origin') or (0, 0)
            sources.append({
                'id': f"{band}_{kind}_{cell.get('cell')}",
                'type': TYPES[kind], 'kind': kind, 'band': band,
                'world': world,
                'x': int(x), 'y': int(cell.get('mean_surface_y') or 64),
                'z': int(z),
                'radius': spec['radius'],
                # `capacity` is what the runtime may manifest. The core is
                # stated beside it so the two are not conflated: a population
                # reduced to its core is depleted, not extinct.
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
            'id': f"access_sheep_{cell.get('cell')}", 'type': 'ANIMAL',
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
        'cells_ranked': total,
        'by_band': {b: sorted(set(k)) for b, k in by_band.items()},
        'skipped': skipped[:40],
        'skipped_total': len(skipped),
        'quantities_are': 'DECLARED FIXTURES. Patch sizes, herd sizes and '
                          'recovery cadence are OPEN in maps.md and the '
                          'recalled nourishment figures need re-derivation; '
                          'the ORDERING is what is recovered, not the numbers.',
        'bands_are': 'ranks among this map\'s own cells, not block distances. '
                     'The recovered near/farther/deeper was an A/B/C ordering '
                     'with no distances decided.',
        'no_species_floor': 'nothing here requires a kind to appear in either '
                            'opening. Requiring named species in both would '
                            'reintroduce ecological symmetry that doctrine '
                            'denies.',
        'beneath_this': 'ordinary vanilla incidence, which these strategic '
                        'manifestations are layered over rather than replacing.',
    }
