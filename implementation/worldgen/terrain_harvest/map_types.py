"""Map Type predicates, defined against a measured distribution.

WHAT B1 FOUND, AND IT IS A NEGATIVE RESULT. Over 2,834 scoops on 24 seeds, no
continuous feature has a natural boundary. The largest gap anywhere inside the
p10-p90 range is 0.4% of that span for water fraction, 1.5% for coastline
density, 0.4% for separation sign, 0.7% for dominant-biome share. These
distributions are unimodal and smooth. Only `land_bodies` shows a large
relative gap (33%) and that is integer granularity -- the step from three
bodies to four -- not a mode.

So the terrain does not contain Map Type boundaries waiting to be discovered.
**Every threshold here is a convention, not a finding**, and the honest way to
write one is as a QUANTILE of the measured distribution rather than a number
that looks derived. `water_fraction > 0.35` reads like a measurement.
"wetter than four scoops in five" says what it is, is re-derivable when the
reference is re-measured, and cannot quietly encode the sample it came from.

The earlier fixtures -- water > 0.35, coast > 0.06, central = the middle third
-- were all invented and are superseded by this file, except `central`, which
is a geometric shape choice and stays a fixture.

THE REFERENCE IS A SAMPLE, NOT A LAW. `scoop_reference.json` holds quantiles
from 24 unscreened seeds at one scoop size, one scan resolution and one sea
level. Re-measure it when any of those change; a predicate quoting a quantile
stays correct across that re-measurement and a hardcoded number does not.

WHAT IS STILL MISSING. Abundance, information, value concentration and
exposure are unmeasured, so no predicate below can express Arid, Jungle or
Crater in the terms that actually define them. Those Types are absent rather
than approximated.
"""
from __future__ import annotations

import json
import os

_HERE = os.path.dirname(os.path.abspath(__file__))
REFERENCE_PATH = os.path.join(_HERE, 'scoop_reference.json')

with open(REFERENCE_PATH) as _fh:
    REFERENCE = json.load(_fh)


def at(feature: str, quantile: str) -> float:
    """The measured value at a quantile, e.g. at('water', 'p75')."""
    try:
        return REFERENCE['quantiles'][feature][quantile]
    except KeyError as exc:
        raise KeyError(
            f'no {quantile} for {feature} in {REFERENCE["schema"]}; '
            f'features are {sorted(REFERENCE["quantiles"])}') from exc


def _get(scoop, feature, default=None):
    """Read a feature from either tier's row shape.

    The two tiers disagree and the predicates have to survive both.
    `scoop.search`'s cheap candidates carry `relief` as a float; its exact rows
    carry `relief_blocks` as {'a': .., 'b': ..} per half. Reading the dict
    against a float threshold raises TypeError, which is how this was caught.
    The worse half is used, because a scoop is as rugged as its rougher end.
    """
    alias = {'water': 'water_fraction', 'coast_1k': 'coastline_per_1k_blocks2',
             'relief': 'relief_blocks', 'sep_sign': 'separation_sign'}
    value = scoop[feature] if feature in scoop else scoop.get(
        alias.get(feature, feature), default)
    if isinstance(value, dict):
        numbers = [v for v in value.values() if isinstance(v, (int, float))]
        return max(numbers) if numbers else default
    return value


# Each entry says which features it reads and at which quantile it cuts, so a
# predicate can be read without opening the reference file.
DEFINITIONS = {
    'archipelago': {
        'reads': ('water', 'land_bodies', 'coast_density'),
        'cut': 'water above p75, land broken into more than one body; where '
               'the body count is unavailable (screening tier) coast density '
               'stands in as the fragmentation proxy',
        'why': 'fragmentation is the premise; a drowned single island is not '
               'an archipelago however wet it is, which is why land_bodies '
               'is here and a fraction alone was never enough',
    },
    'open_water': {
        'reads': ('water',),
        'cut': 'water above p90',
        'why': 'not a Map Type. It exists to NAME the sink a blind symmetry '
               'search falls into, so those scoops are labelled rather than '
               'silently winning',
    },
    'landmass': {
        'reads': ('water',),
        'cut': 'water below p25',
        'why': 'the dry baseline Default is drawn from',
    },
    'shattered_coast': {
        'reads': ('water', 'coast_1k'),
        'cut': 'water between p25 and p75, coastline above p75',
        'why': 'land and sea both viable and repeatedly intersecting. '
               'Distinguished from archipelago by edge density at moderate '
               'water rather than by how much water there is',
    },
    'divided_by_a_cut': {
        'reads': ('sep_sign', 'relief'),
        'cut': 'separation sign above p75, relief above p50',
        'why': 'the scoop is broken into separated HIGH ground, so the '
               'divider is a canyon, river or ravine. See '
               'prominence.relief_shape: the sign reports what a feature '
               'divides the map into, which is the opposite sign from what '
               'the feature is made of',
    },
    'divided_by_a_ridge': {
        'reads': ('sep_sign', 'relief'),
        'cut': 'separation sign below p25, relief above p50',
        'why': 'divided into separated basins, so the divider is raised',
    },
    'arid': {
        'reads': ('arid_share',),
        'cut': 'desert and badlands over 0.45 (reachable to 0.768)',
        'why': 'maps.md: "what if an otherwise viable competitive world '
               'developed around an enormous arid region where biological '
               'abundance is rare". The premise is a KIND OF COUNTRY, so the '
               'recogniser is a biome-family share rather than a shape'},
    'badlands': {
        'reads': ('badlands_share',),
        'cut': 'badlands over a third',
        'why': 'terraced shelves and exposed mineshafts; distinguished from '
               'arid by excluding plain desert'},
    'frozen': {
        'reads': ('frozen_share',),
        'cut': 'snowy, taiga, frozen ocean and river over 0.60 (reaches 1.0)',
        'why': 'ice is the fastest natural route in the game while snow and '
               'vertical terrain complicate everything off it'},
    'swamplands': {
        'reads': ('swamp_share',),
        'cut': 'swamp and mangrove over 0.22 (reachable to 0.343)',
        'why': 'movement technically open and awkward in practice; a quarter '
               'rather than a half because swamp rarely dominates a region'},
    'lost_jungle': {
        'reads': ('jungle_share', 'jungle_temple', 'village'),
        'cut': 'jungle over 0.30 of its measured 0.455 ceiling, AND a temple '
               'AND two villages',
        'why': 'STRUCTURE-DEFINED. The premise is not jungle, it is expensive '
               'information around discovered clearings that become anchors, '
               'so the structures are constitutive rather than incidental'},
    'pale_forest': {
        'reads': ('pale_share', 'central_mansion'),
        'cut': 'pale garden over 0.012 -- most of its measured 0.020 ceiling '
               '-- AND a Mansion at the centre',
        'why': 'STRUCTURE-DEFINED, and the completion of `central_mansion`, '
               'which checked the Mansion and not the forest. maps.md: "what '
               'if poor visibility conceals an exceptionally valuable central '
               'region" -- the forest is the concealment and the Mansion is '
               'the reason, and neither alone is the Type'},
    'central_mansion': {
        'reads': ('central_mansion',),
        'cut': 'at least one Mansion in the middle of the scoop',
        'why': 'the Pale Forest premise is a (feature, LOCATION) pair. A '
               'Mansion at the scoop edge belongs to one team; only a central '
               'one is contested. Not a quantile: presence is not a '
               'distribution',
    },
}


def predicates(*, reference: dict | None = None):
    """The predicate set, bound to a reference distribution."""
    ref = reference or REFERENCE
    Q = lambda f, p: ref['quantiles'][f][p]

    def archipelago(s):
        # TWO TIERS, and the screen has to work without the exact input.
        # `land_bodies` is connectivity, so it is not a summed-area quantity
        # and `scoop.search`'s cheap candidates cannot carry it. Requiring it
        # made this predicate read land_bodies=1 by default at the screening
        # tier, match nothing, and receive no budget -- so across 20 seeds
        # archipelago was never tutored for, which looked like a property of
        # the seeds and was a missing input.
        #
        # Where the count is absent, coast density stands in as the
        # fragmentation proxy: many bodies means much edge. Where it is
        # present -- the exact tier -- it decides.
        if not (_get(s, 'water', 0) > Q('water', 'p75')):
            return False
        bodies = _get(s, 'land_bodies')
        if bodies is not None:
            return bodies > 1
        return _get(s, 'coast_density', 0) > 0.05

    def open_water(s):
        return _get(s, 'water', 0) > Q('water', 'p90')

    def landmass(s):
        return _get(s, 'water', 1) < Q('water', 'p25')

    def shattered_coast(s):
        w = _get(s, 'water', 0)
        if not (Q('water', 'p25') <= w <= Q('water', 'p75')):
            return False
        exact = _get(s, 'coast_1k')
        if exact is not None:
            return exact > Q('coast_1k', 'p75')
        # Cheap tier: `coast_density` is edges per cell, a different scale
        # from the per-1k-blocks reference, so it cannot be compared against
        # that quantile. NON-CANON screening proxy.
        return _get(s, 'coast_density', 0) > 0.08

    def divided_by_a_cut(s):
        return (_get(s, 'sep_sign', 0) > Q('sep_sign', 'p75')
                and _get(s, 'relief', 0) > Q('relief', 'p50'))

    def divided_by_a_ridge(s):
        return (_get(s, 'sep_sign', 0) < Q('sep_sign', 'p25')
                and _get(s, 'relief', 0) > Q('relief', 'p50'))

    def central_mansion(s):
        return bool(_get(s, 'central_mansion', 0))

    # Biome-family Types. The cuts come from MEASURED REACHABILITY, because a
    # first pass set them all at a third and that is impossible for some
    # families. Max window share observed over 25 seeds:
    #
    #     frozen 1.000   arid 0.768   jungle 0.455   swamp 0.343   pale 0.020
    #
    # A pale garden is a SMALL biome -- its best whole-area share across 30
    # seeds was 0.0040 -- so a 0.33 cut asked for something sixteen times
    # larger than the largest that exists, and `pale_forest` matched nothing
    # in 20 seeds for that reason rather than from rarity.
    #
    # So `pale` is cut relative to what the biome can be, not to what a third
    # of a window would mean. "Dominated by pale forest" has to mean
    # "dominated as pale forests go".
    #
    # DECLARED FIXTURES still: reachability says what is possible, not what is
    # good. These want calibration against play.
    def arid(s):
        return _get(s, 'arid_share', 0) > 0.45          # reachable to 0.768

    def badlands(s):
        return _get(s, 'badlands_share', 0) > 0.33

    def frozen(s):
        return _get(s, 'frozen_share', 0) > 0.60        # reachable to 1.000

    def swamplands(s):
        return _get(s, 'swamp_share', 0) > 0.22         # reachable to 0.343

    def lost_jungle(s):
        return (_get(s, 'jungle_share', 0) > 0.30       # reachable to 0.455
                and _get(s, 'jungle_temple', 0) >= 1
                and _get(s, 'village', 0) >= 2)

    def pale_forest(s):
        # 0.012 is most of the 0.020 ceiling, not a third of a window.
        return (_get(s, 'pale_share', 0) > 0.012
                and bool(_get(s, 'central_mansion', 0)))

    return {'archipelago': archipelago, 'open_water': open_water,
            'arid': arid, 'badlands': badlands, 'frozen': frozen,
            'swamplands': swamplands, 'lost_jungle': lost_jungle,
            'pale_forest': pale_forest,
            'landmass': landmass, 'shattered_coast': shattered_coast,
            'divided_by_a_cut': divided_by_a_cut,
            'divided_by_a_ridge': divided_by_a_ridge,
            'central_mansion': central_mansion}


def symmetry_bound(*, quantile: str = 'p25') -> dict:
    """The competitive gate, expressed on the ratio and over contested ground.

    ONE BOUND FOR EVERY TYPE, deliberately. Per-Type bounds were deferred
    because most of the apparent need for them was a denominator error:
    measured over the whole scoop, water buys free parity and a wet scoop
    looks more symmetric than a dry one. Over contested land only, the ratio
    is 0.269 to 0.296 across the whole water range -- close enough to
    Type-independent that fifteen bound-sets would encode noise.
    """
    return {'feature': 'dev_rel', 'quantile': quantile,
            'value': at('dev_rel', quantile),
            'measured_over': 'contested ground only',
            'not_per_type': 'the residual spread across Types is ~10% and '
                            'unexplained; see maps.md Map Type doctrine',
            'not_on_raw_deviation': 'deviation scales with relief, so a raw '
                                    'bound admits only flat ground and water'}


# MEASURED YIELD PER TYPE, and the reason budget is not spread evenly.
#
# From the 59-target batch of 24 September 2026, after the land-sited Homebase
# fix. These are compile-through rates: the fraction of generated windows of
# that Type that reached READY.
#
#     landmass           11/25 = 44%
#     shattered_coast     1/31 =  3%
#     archipelago         1/2       (n too small to act on)
#     divided_by_a_cut    0/2       (n too small)
#     divided_by_a_ridge  0/1       (n too small)
#
# shattered_coast consumed 31 of 59 generations -- 53% of the expensive step --
# to produce one map. Spreading budget evenly across Types whose rates differ
# fifteenfold is the single largest waste in the pipeline.
#
# SOCKET_NOT_INDEPENDENTLY_ACCEPTABLE remains 48 of 61 rejections, almost all
# shattered_coast: fragmented coastline does not offer two independently
# developable Homebase sockets.
#
# [OPEN] Whether the predicate is wrong or shattered_coast genuinely needs
# Homebase rules other than Default's. n=31 says the Type as defined is not
# viable under the current rules; it does not say which of those two is true.
#
# NOT A THRESHOLD AND NOT PERMANENT. This is an observation with its sample
# size attached, re-derivable from any batch, and a Type with too few
# observations is given the neutral prior rather than a number.
OBSERVED_YIELD = {
    'landmass': {'playable': 11, 'generated': 25},
    'shattered_coast': {'playable': 1, 'generated': 31},
    'archipelago': {'playable': 1, 'generated': 2},
    'divided_by_a_cut': {'playable': 0, 'generated': 2},
    'divided_by_a_ridge': {'playable': 0, 'generated': 1},
}
MIN_OBSERVATIONS = 10
NEUTRAL_PRIOR = 0.25


def yield_of(name: str) -> dict:
    """A Type's measured compile-through rate, with its sample size."""
    row = OBSERVED_YIELD.get(name)
    if not row or row['generated'] < MIN_OBSERVATIONS:
        return {'rate': NEUTRAL_PRIOR, 'generated': (row or {}).get('generated', 0),
                'measured': False,
                'why': f'fewer than {MIN_OBSERVATIONS} observations; given the '
                       f'neutral prior rather than a number read off noise'}
    return {'rate': row['playable'] / row['generated'],
            'playable': row['playable'], 'generated': row['generated'],
            'measured': True}


def allocate(names, total: int, *, floor: int = 1, wanted=None) -> dict:
    """Split a generation budget so the BOARD can be filled.

    DEMAND-DRIVEN, NOT YIELD-DRIVEN, and the earlier version had it backwards.
    It weighted budget BY yield, so `landmass` at 44% took most of it and
    `shattered_coast` at 3% was starved. That is right for a throughput goal
    and wrong for a board goal.

    If the board needs one map of a Type, demand is the INVERSE of yield:

        landmass          44.0%  ->   2.3 generations per map
        shattered_coast    3.2%  ->  31.2 generations per map
        a 1% Type          1.0%  -> 100   generations per map

    A rare Type needs MORE budget, not less. Weighting by yield systematically
    starves exactly the Types that are rare BY DESIGN -- which is the
    definition of Pale Forest / Mansion and Sky Islands, not a defect in them.
    maps.md says to "prefer extreme vanilla phenomena over invented terrain"
    and to search for outliers "rather than rejecting them"; that was written
    down and then contradicted here.

    `wanted` is how many maps of each Type the board needs. Absent, one each.
    """
    names = list(names)
    if not names or total <= 0:
        return {'allocation': {}, 'why': 'nothing to allocate'}
    want = {n: (wanted or {}).get(n, 1) for n in names}
    rates = {n: yield_of(n)['rate'] for n in names}
    # Expected generations to produce `want` maps of each Type.
    demand = {n: want[n] / max(rates[n], 0.01) for n in names}
    weight = sum(demand.values()) or 1.0
    spare = max(0, total - floor * len(names))
    allocation = {n: floor + int(spare * demand[n] / weight) for n in names}
    short = total - sum(allocation.values())
    if short > 0:
        allocation[max(names, key=lambda n: demand[n])] += short
    return {'allocation': allocation,
            'rates': {n: round(rates[n], 3) for n in names},
            'generations_per_map': {n: round(demand[n] / max(1, want[n]), 1)
                                    for n in names},
            'wanted': want,
            'measured': {n: yield_of(n)['measured'] for n in names},
            'floor': floor,
            'driven_by': 'board demand divided by measured yield, so a Type '
                         'that is rare BY DESIGN is paid for rather than '
                         'starved. A rare Type needs more budget, not less.'}


# Max window share each biome family actually reaches, over 25 seeds. Recorded
# because a cut above these is not strict, it is impossible -- and a predicate
# that matches nothing looks identical to a Type that is merely rare.
FAMILY_CEILINGS = {'frozen': 1.000, 'arid': 0.768, 'jungle': 0.455,
                   'swamp': 0.343, 'pale': 0.020}
