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
    alias = {'water': 'water_fraction', 'coast_1k': 'coastline_per_1k_blocks2',
             'relief': 'relief_blocks', 'sep_sign': 'separation_sign'}
    if feature in scoop:
        return scoop[feature]
    return scoop.get(alias.get(feature, feature), default)


# Each entry says which features it reads and at which quantile it cuts, so a
# predicate can be read without opening the reference file.
DEFINITIONS = {
    'archipelago': {
        'reads': ('water', 'land_bodies'),
        'cut': 'water above p75, land broken into more than one body',
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
        return (_get(s, 'water', 0) > Q('water', 'p75')
                and _get(s, 'land_bodies', 1) > 1)

    def open_water(s):
        return _get(s, 'water', 0) > Q('water', 'p90')

    def landmass(s):
        return _get(s, 'water', 1) < Q('water', 'p25')

    def shattered_coast(s):
        w = _get(s, 'water', 0)
        return (Q('water', 'p25') <= w <= Q('water', 'p75')
                and _get(s, 'coast_1k', 0) > Q('coast_1k', 'p75'))

    def divided_by_a_cut(s):
        return (_get(s, 'sep_sign', 0) > Q('sep_sign', 'p75')
                and _get(s, 'relief', 0) > Q('relief', 'p50'))

    def divided_by_a_ridge(s):
        return (_get(s, 'sep_sign', 0) < Q('sep_sign', 'p25')
                and _get(s, 'relief', 0) > Q('relief', 'p50'))

    def central_mansion(s):
        return bool(_get(s, 'central_mansion', 0))

    return {'archipelago': archipelago, 'open_water': open_water,
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
