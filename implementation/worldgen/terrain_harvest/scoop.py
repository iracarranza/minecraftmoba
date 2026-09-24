"""A scoop is a REGION plus the team axis drawn across it, at any size.

`window_search` already finds WHERE on a seed to look, but it fixes two things
this does not. It searches a single window size -- the compiler's 864x1056 --
and it chooses the split axis by which orientation best satisfies the OCEAN
gradient. A scoop makes the size a variable and re-reads the axis choice.

THE AXIS COLLISION, which is the real integration problem. There is only one
axis choice per region: once the team split is fixed, the resource gradient is
forced onto the perpendicular. `window_search` spends that single choice on
ocean ("either side may be the east"), so by the time a window is returned, the
team axis is already decided as a side effect of a resource check that never
looked at the two team ends. Nothing downstream ever revisits it. This module
scores BOTH readings of an axis and returns them, so the choice is made once
with both considerations visible instead of twice with one each.

TWO DIFFERENT THINGS GET CALLED SYMMETRY, and they are not close.

  MIRROR similarity asks whether terrain at p resembles terrain at p's
  reflection. In fractal noise this is essentially never true, and demanding it
  would reject everything.

  DISTRIBUTIONAL similarity asks whether the two halves have comparable RELIEF
  and ROUGHNESS -- whether both ends are the same kind of place to play on.
  "The north is flat while the south is dotted with hills" is this one, not the
  first. It is far weaker, it is satisfiable, and it is what the discard is
  actually reaching for.

Both are computed, and the mirror term is DECOMPOSED into tilt and residual.
Decomposed, not corrected. The distinction matters and an earlier version of
this module got it backwards.

A slope running W-E across the landmass, with the teams at the N and S ends,
needs no correction at all: height varies only with x, mirroring N onto S
leaves it unchanged, and tilt and residual both read 0.0. That case was always
clean.

The case detrending actually removed was a slope running ALONG the team axis,
one team simply higher than the other. That is not a difference of altitude
without consequence -- it is one team holding the high ground, which is a real
competitive deficit. Fitting it away and ranking on what remained meant
preferring scoops with that deficit over scoops without it.

So a scoop is ranked on the RAW mirror deviation, tilt included. The
decomposition is kept because it says WHY a scoop scores badly -- a tilt might
be answerable by authoring where a residual cannot be -- but it does not
discount anything.

WHAT THE CORRECTION CHANGED, on 14 finalists. Ranking on the detrended
residual made the two axes look indistinguishable (18.9 against 19.7) and
suggested the axis choice barely mattered. Ranking on the raw deviation, z
wins 13 of 14 -- median 22.1 against 33.1 -- and the reason is the tilt that
was being fitted away: x tilt runs -28 to -76 blocks on these maps while z
tilt stays within +/-27. They sit on a strong east-west regional slope, so
splitting the teams E-W would put one of them up to 76 blocks above the other.
The raw measure was right, and it independently agrees with the orientation
the pipeline already uses -- without consulting ocean at all.

The decomposition still earns its place. On 930012642 the x axis has the
LOWER residual (18.2 against 20.1): on character alone it would be the better
split, and only its -43 tilt rules it out. One number could not have said that.

Nothing here is bounded: the numbers are reported and what counts as too
lopsided is a template's decision, not this module's.

MAP SIZE IS DERIVED, NOT CONFIGURED. There is no map-size axis anywhere in the
compiler today -- 864x1056 is a constant, and Normal/Large/Vast do not exist.
Rather than adding a size setting and generating to it, the two scalars that
would distinguish them fall out of a scoop for free: its playable AREA and the
SEPARATION of the two Homebase sites inside it. Those are reported. Where to
cut them into named sizes is a labelling decision and is deliberately absent,
for the same reason prominence reports a number and not "a mountain range".

WHAT THIS PROVES. Height and the flatness proxy only. A mirrored pair of
low-grade discs is a pair of PLACES WORTH TESTING as Homebase sockets, not two
acceptable sockets -- developability, water, resources and buildability all
still need the scoop generated.
"""
from __future__ import annotations

from statistics import median

# The current compiler window, which becomes the smallest scoop rather than the
# only one. NON-CANON FIXTURE: the upper bound is "slightly larger volume" with
# no measurement behind the multiplier yet.
BASE_SCOOP = (864, 1056)
MAX_SCOOP_SCALE = 1.5

# Homebase flatness probe radius. 96 blocks is borrowed from `opening_ceiling`,
# which chose it to exceed the authored footprints. NON-CANON FIXTURE.
HOMEBASE_PROBE_BLOCKS = 96


def _pct(values, q):
    if not values:
        return 0.0
    ordered = sorted(values)
    return float(ordered[min(len(ordered) - 1, int(q * len(ordered)))])


def _relief(values):
    return _pct(values, 0.95) - _pct(values, 0.05)


def _roughness(height, width, depth, i0, i1, j0, j1):
    """Mean absolute neighbour difference -- how broken up the ground is.

    Relief alone cannot separate one long ramp from a field of hillocks; both
    span the same vertical range. Roughness is the part of "dotted with hills"
    that relief misses.
    """
    total, n = 0.0, 0
    for j in range(j0, j1):
        for i in range(i0, i1):
            h = height[j * width + i]
            if i + 1 < i1:
                total += abs(height[j * width + i + 1] - h); n += 1
            if j + 1 < j1:
                total += abs(height[(j + 1) * width + i] - h); n += 1
    return total / n if n else 0.0


def _gap(a, b):
    total = abs(a) + abs(b)
    return 0.0 if total == 0 else abs(a - b) / total


def _half_bounds(width, depth, axis):
    """(half A, half B) as (i0, i1, j0, j1), B being the mirrored end."""
    if axis == 'z':
        h = depth // 2
        return (0, width, 0, h), (0, width, depth - h, depth)
    h = width // 2
    return (0, h, 0, depth), (width - h, width, 0, depth)


def _mirror(i, j, width, depth, axis):
    return (i, depth - 1 - j) if axis == 'z' else (width - 1 - i, j)


def symmetry(height: list, width: int, depth: int, axis: str,
             *, spacing_hint: int = 8) -> dict:
    """How alike the two team ends are, mirror-wise and distribution-wise."""
    a, b = _half_bounds(width, depth, axis)
    va = [height[j * width + i] for j in range(a[2], a[3]) for i in range(a[0], a[1])]
    vb = [height[j * width + i] for j in range(b[2], b[3]) for i in range(b[0], b[1])]
    overall = _relief(height) or 1.0

    # TILT AND RESIDUAL. Raw mirror deviation conflates two unrelated things.
    #
    # A scoop laid across a slope has one end higher than the other. Every
    # mirrored pair then differs -- but the two ends are the SAME KIND OF
    # PLACE at different altitudes, and a mirror line down the middle of a
    # slope is exactly the case that should survive.
    #
    # Subtracting a mean OFFSET does not remove a slope: on a ramp the mirrored
    # difference is near zero at the midline and largest at the edges, so one
    # constant cannot cancel it. A first attempt did this and left a pure ramp
    # reporting residual 5.0 out of a 19-block span -- a clean mirror flagged as
    # a mismatch. The trend has to be FITTED and removed, not averaged away.
    #
    # Tilt and residual are also different kinds of problem. A tilt is a
    # gameplay question -- one team holds the high ground -- fixable by
    # authoring or simply accepted. A residual means the halves are different
    # terrain, which nothing downstream can repair.
    coords = []
    for j in range(depth):
        for i in range(width):
            coords.append(j if axis == 'z' else i)
    mean_c = sum(coords) / len(coords)
    mean_h = sum(height) / len(height)
    num = sum((c - mean_c) * (h - mean_h) for c, h in zip(coords, height))
    den = sum((c - mean_c) ** 2 for c in coords) or 1.0
    slope = num / den                       # blocks of height per sample step
    flat = [h - slope * (c - mean_c) for c, h in zip(coords, height)]

    deviation, n = 0.0, 0
    for j in range(a[2], a[3]):
        for i in range(a[0], a[1]):
            mi, mj = _mirror(i, j, width, depth, axis)
            deviation += abs(flat[j * width + i] - flat[mj * width + mi])
            n += 1
    mad = deviation / n if n else 0.0
    span = (depth if axis == 'z' else width) - 1
    tilt = slope * span                     # total rise across the team axis

    # The RAW deviation, which is what a scoop is ranked on. Detrending is a
    # decomposition, not a correction: see the module note on tilt.
    raw, _n = 0.0, 0
    for j in range(a[2], a[3]):
        for i in range(a[0], a[1]):
            mi, mj = _mirror(i, j, width, depth, axis)
            raw += abs(height[j * width + i] - height[mj * width + mi])
            _n += 1
    raw = raw / _n if _n else 0.0

    ra, rb = _relief(va), _relief(vb)
    ga = _roughness(height, width, depth, *[a[0], a[1], a[2], a[3]])
    gb = _roughness(height, width, depth, *[b[0], b[1], b[2], b[3]])
    return {
        'team_axis': axis,
        # mirror, decomposed. Tilt is the constant height offset across the
        # split -- a slope, not a difference in character. Residual is what
        # remains, and near-zero residual would mean literal reflection, which
        # noise never is.
        'mirror_deviation_blocks': round(raw, 2),
        'mirror_tilt_blocks': round(tilt, 2),
        'mirror_tilt_blocks_per_block': round(slope / spacing_hint, 5),
        'mirror_residual_blocks': round(mad, 2),
        # NEARLY A CONSTANT, and reported with that warning attached. Across
        # 28 readings of 14 finalists this sat at 0.23 for 27 of them. Residual
        # scales with relief, so the ratio divides the signal out; it is kept
        # because a genuine outlier does show (0.388 on 920270003) but it must
        # not be mistaken for a discriminating score. The raw residual and the
        # tilt carry the information.
        'mirror_residual_over_relief': round(mad / overall, 4),
        # distributional: the "one end flat, one end hilly" reading
        'relief_blocks': {'a': round(ra, 2), 'b': round(rb, 2)},
        'relief_gap': round(_gap(ra, rb), 4),
        'roughness': {'a': round(ga, 3), 'b': round(gb, 3)},
        'roughness_gap': round(_gap(ga, gb), 4),
        'median_elevation': {'a': round(median(va), 1) if va else 0.0,
                             'b': round(median(vb), 1) if vb else 0.0},
        'bounded': 'no. Both gaps are reported; no discard threshold is set here.',
    }


def _grade(height, width, depth, ci, cj, r):
    """Worst deviation from the local median inside a disc -- a flatness proxy."""
    cells = []
    for j in range(max(0, cj - r), min(depth, cj + r + 1)):
        for i in range(max(0, ci - r), min(width, ci + r + 1)):
            if (i - ci) ** 2 + (j - cj) ** 2 <= r * r:
                cells.append(height[j * width + i])
    if not cells:
        return None
    mid = median(cells)
    return max(abs(c - mid) for c in cells)


def homebase_pair(height: list, width: int, depth: int, axis: str, *,
                  spacing: int = 8, probe_blocks: int = HOMEBASE_PROBE_BLOCKS,
                  stride: int = 2) -> dict:
    """The MIRRORED pairs of flat discs, as a separation/flatness frontier.

    Scored on the WORSE of the two, because a scoop qualifies only if both ends
    can host a Homebase -- a superb north paired with a cliff face is not half
    a map, it is no map. Searching pairs rather than each end separately is the
    point: the mirror constraint is what makes the two ends comparable.

    WHY A FRONTIER AND NOT A BEST PAIR. The first version returned the single
    flattest pair, and on four finalists it returned separations of 216 to 856
    blocks inside an IDENTICAL window -- because nothing asked the pair to be
    far apart, so it cheerfully put both Homebases either side of the midline.
    216 blocks apart is not a map, and worse, it made "separation" useless as
    the size scalar it is supposed to be: the number was reporting where the
    flattest ground happened to sit, not how big the scoop plays.

    Separation and flatness genuinely trade off -- pushing the pair to the far
    corners finds worse ground -- so there is no single right answer to return.
    The frontier reports, for each separation, the flattest pair achieving it,
    and lets a template choose. `max_separation_blocks` is then a real property
    of the scoop rather than an accident of the search.
    """
    r = max(1, probe_blocks // spacing)
    a, _ = _half_bounds(width, depth, axis)
    best_by_sep = {}
    for j in range(a[2] + r, a[3] - r, stride):
        for i in range(a[0] + r, a[1] - r, stride):
            mi, mj = _mirror(i, j, width, depth, axis)
            ga = _grade(height, width, depth, i, j, r)
            gb = _grade(height, width, depth, mi, mj, r)
            if ga is None or gb is None:
                continue
            worse = max(ga, gb)
            sep = (abs(mj - j) if axis == 'z' else abs(mi - i)) * spacing
            prior = best_by_sep.get(sep)
            if prior is None or worse < prior['worst_grade_blocks']:
                best_by_sep[sep] = {
                    'separation_blocks': sep,
                    'a_sample': [i, j], 'b_sample': [mi, mj],
                    'grade_blocks': {'a': round(ga, 1), 'b': round(gb, 1)},
                    'worst_grade_blocks': round(worse, 1)}
    if not best_by_sep:
        return {'frontier': [], 'why': 'scoop smaller than two probe discs'}
    # Keep only pairs no other pair beats on BOTH separation and flatness.
    ordered = sorted(best_by_sep.values(), key=lambda p: -p['separation_blocks'])
    frontier, floor = [], None
    for p in ordered:
        if floor is None or p['worst_grade_blocks'] < floor:
            frontier.append(p)
            floor = p['worst_grade_blocks']
    return {
        'frontier': frontier,
        'max_separation_blocks': frontier[0]['separation_blocks'],
        'grade_at_max_separation': frontier[0]['worst_grade_blocks'],
        'flattest': min(frontier, key=lambda p: p['worst_grade_blocks']),
        'tradeoff': 'separation vs flatness. No pair on this frontier is chosen '
                    'here; picking one is a template decision.',
    }


def describe(feature_grid: dict, *, spacing: int | None = None) -> dict:
    """Both axis readings of one scoop, plus the two size scalars.

    Returns BOTH candidate team axes rather than picking one. Which axis is the
    team split is a joint decision with the resource gradient (see the module
    docstring), and this module does not hold half of that decision.
    """
    from .prominence import from_rle
    width = feature_grid['width']
    depth = feature_grid['height']
    spacing = spacing or feature_grid.get('sample_spacing_blocks', 8)
    height = from_rle(feature_grid['height_rle'])

    axes = {}
    for axis in ('z', 'x'):
        sym = symmetry(height, width, depth, axis, spacing_hint=spacing)
        pair = homebase_pair(height, width, depth, axis, spacing=spacing)
        axes[axis] = {**sym, 'homebase_pair': pair,
                      'homebase_max_separation_blocks': pair.get('max_separation_blocks')}
    return {
        'scoop_blocks': [width * spacing, depth * spacing],
        'area_blocks2': width * depth * spacing * spacing,
        'base_scoop_blocks': list(BASE_SCOOP),
        'area_over_base': round(width * depth * spacing * spacing
                                / (BASE_SCOOP[0] * BASE_SCOOP[1]), 3),
        'axes': axes,
        'size_scalars': 'area_blocks2 and axes[*].homebase_max_separation_blocks. '
                        'Where these cut into Normal/Large/Vast is not decided here.',
        'proves': 'elevation and a flatness proxy only. Developability, water, '
                  'resources and buildability need the scoop generated.',
    }


# ---------------------------------------------------------------------------
# FINDING scoops, as opposed to measuring one.
#
# `window_search` slides ONE size and ranks by ocean. This slides many sizes and
# ranks by symmetry, with no Default parameter involved: no ocean fraction, no
# east gradient, no land-body count. A scoop that a landmass map would offer --
# a symmetric stretch of pure inland terrain, or a clean mirror down the middle
# of a slope -- is invisible to the ocean search and is exactly what this finds.
#
# TWO TIERS, because the cost is real. The exact residual needs one pass per
# mirrored pair, so evaluating it at every offset and size is expensive.
# `budget` bounds the second tier explicitly rather than letting the scan size
# decide it.
#
# THE SCREEN HAS TO MEASURE THE SAME THING THE RANKING DOES. The first version
# screened on summed-area statistics -- variance and roughness gaps -- because
# they are nearly free. They are also blind to mirror quality: a planted,
# exactly-mirrored band did not survive to the exact tier at all, because
# nothing in the screen could see it, and the exact tier can only ever reorder
# what the screen passed. A cheap screen that optimises a different quantity
# than the ranking does not save work, it changes the answer.
#
# So the screen is a COARSE version of the ranking: the same raw mirror
# deviation on a subsampled grid. It costs 1/k^2 of the exact pass and measures
# the right thing. The summed-area statistics are still computed and reported,
# just not ranked on.
#
# STRIDE BOUNDS PRECISION. A mirror is only as good as the offset the search
# happens to land on. On uncorrelated noise a one-sample offset destroys the
# match completely, and a stride of 4 stepped straight over a planted mirror
# band in testing. Real terrain is correlated over tens of blocks so it is far
# more forgiving, but the stride still sets how precisely a mirror LINE can be
# located, and a scoop's residual is a lower bound on what a finer search
# would find, never an upper one.


def _sat(values, width, depth):
    table = [[0] * (width + 1) for _ in range(depth + 1)]
    for j in range(depth):
        rowsum = 0
        for i in range(width):
            rowsum += values[j * width + i]
            table[j + 1][i + 1] = table[j][i + 1] + rowsum
    return table


def _box(table, i0, j0, i1, j1):
    return table[j1][i1] - table[j0][i1] - table[j1][i0] + table[j0][i0]


def _cheap(sums, i0, j0, i1, j1, axis):
    """Tilt magnitude and roughness gap between the halves, from sums alone."""
    h, hh, rough = sums
    n = (i1 - i0) * (j1 - j0)
    if n <= 0:
        return None
    if axis == 'z':
        mid = (j0 + j1) // 2
        ba = (i0, j0, i1, mid); bb = (i0, mid, i1, j1)
    else:
        mid = (i0 + i1) // 2
        ba = (i0, j0, mid, j1); bb = (mid, j0, i1, j1)
    na = (ba[2] - ba[0]) * (ba[3] - ba[1])
    nb = (bb[2] - bb[0]) * (bb[3] - bb[1])
    if na == 0 or nb == 0:
        return None
    mean_a = _box(h, *ba) / na
    mean_b = _box(h, *bb) / nb
    # Variance stands in for relief here: it is a sum, and relief is not.
    var_a = max(0.0, _box(hh, *ba) / na - mean_a ** 2)
    var_b = max(0.0, _box(hh, *bb) / nb - mean_b ** 2)
    rough_a = _box(rough, *ba) / na
    rough_b = _box(rough, *bb) / nb
    return {
        'tilt_blocks': abs(mean_a - mean_b),
        'spread_gap': _gap(var_a ** 0.5, var_b ** 0.5),
        'roughness_gap': _gap(rough_a, rough_b),
        'mean_elevation': (mean_a + mean_b) / 2,
    }


def _coarse_deviation(height, width, i0, j0, cw, cd, axis, k):
    """Raw mirror deviation on every k-th sample. Same measure as `symmetry`.

    NOT detrended. An earlier version fitted the trend out here to match a
    ranking that did the same, and both were wrong in the same direction: a
    slope along the team axis is one team on high ground, and discounting it
    made the search prefer scoops carrying that deficit.
    """
    flat, w = [], 0
    for j in range(0, cd, k):
        for i in range(0, cw, k):
            flat.append(height[(j0 + j) * width + i0 + i])
    if len(flat) < 4:
        return None
    w = len(range(0, cw, k))
    d = len(range(0, cd, k))
    total, m = 0.0, 0
    if axis == 'z':
        for j in range(d // 2):
            for i in range(w):
                total += abs(flat[j * w + i] - flat[(d - 1 - j) * w + i]); m += 1
    else:
        for j in range(d):
            for i in range(w // 2):
                total += abs(flat[j * w + i] - flat[j * w + (w - 1 - i)]); m += 1
    return total / m if m else None


def _type_features(sums, i0, j0, i1, j1):
    """Cheap per-candidate facts a Map Type is recognised by.

    Only summed-area quantities, so each is four lookups. This is what lets a
    type definition steer the search instead of being applied to whatever the
    search happened to return.
    """
    water, edge, dom = sums
    n = (i1 - i0) * (j1 - j0)
    if n <= 0 or water is None:
        return {}
    wet = _box(water, i0, j0, i1, j1) / n
    return {
        'water_fraction': wet,
        'land_fraction': 1 - wet,
        # Coastline per cell. High means fragmented, whatever the fraction.
        'coast_density': _box(edge, i0, j0, i1, j1) / n,
        'dominant_biome_share': _box(dom, i0, j0, i1, j1) / n,
    }


def search(height: list, width: int, depth: int, *, spacing: int = 8,
           sizes=None, stride: int = 8, budget: int = 24, coarsen: int = 4,
           partition=None, types=None, biome=None, sea_level=None,
           probe_blocks: int = HOMEBASE_PROBE_BLOCKS) -> dict:
    """Scoops of several sizes, ranked by how alike their two ends are.

    `sizes` are (w, d) in BLOCKS; the default sweeps the current compiler
    window up to `MAX_SCOOP_SCALE`. Returns the exact reading for the best
    `budget` candidates and the count of everything considered, so the search
    is reported rather than only its winners.

    `partition` is a callable taking a screened candidate and returning a
    bucket key; `budget` is then spent PER BUCKET rather than globally.

    Why that is not a convenience. A single global ranking on symmetry keeps
    whatever is most symmetric, and the most symmetric regions of a world are
    overwhelmingly its plains. A rare symmetric archipelago would be outranked
    by a thousand symmetric flats and never reach the exact tier at all -- so
    filtering hardest-first on symmetry, then asking what type survived, can
    only ever return the types that co-occur with easy symmetry. Spending the
    budget per bucket makes the scarce kind compete against its own kind.

    `types` is {name: predicate}, each predicate taking a screened candidate
    and returning whether that scoop could support that Map Type. Budget is
    spent per matching type, and a scoop matching none lands in `unlabelled`.

    TUTORING IS NOT THE SAME AS BOUNDING, and an earlier version of this
    docstring confused them. The Default pipeline failed because it applied
    bounds to ONE FIXED WINDOW: a seed whose geography suited it three
    thousand blocks away was rejected for what sat at spawn. The bounds were
    never the problem, the absence of search was. Bounds plus search is this
    function, and telling it what several types look like is strictly more
    informative than telling it about one.

    The single thing worth preserving from that objection: a type definition
    must steer the search WITHOUT becoming the discard rule. So every scoop
    keeps its full measured vector whatever it matched, unmatched scoops are
    still measured and returned under `unlabelled`, and a type that matches
    nothing is reported as matching nothing rather than vanishing. A
    sixteenth type nobody has defined still shows up as a described scoop.

    AND TUTORING FIXES A DEGENERACY, not merely a coverage gap. Ranked blind
    on symmetry, the best scoops on seed 1010101 are 97-99% WATER: open ocean
    is featureless, and featureless terrain mirrors itself perfectly. The top
    three contain no land at all and the top eight contain no landmass scoop.
    This is the same failure as the original `window_search` scoring bug,
    where "more ocean is better" drove every result to a drowned window --
    arrived at from the opposite direction, since nothing here mentions ocean.
    A measure with no notion of what a map needs will optimise toward
    emptiness. Tutored, landmass scoops at deviation 6.1 and 8.9 surface
    immediately; the blind search never reached them.

    `partition` remains available for a bucket key that is not a type.

    WHAT A PREDICATE CAN SEE depends on what was passed. With `biome` and
    `sea_level` it gets water fraction, coast density and dominant-biome
    share alongside the elevation terms. Without them it gets elevation only
    -- so an archipelago predicate silently matches nothing, and the result
    says so rather than reporting an absence of archipelagos.
    """
    sizes = sizes or [(int(BASE_SCOOP[0] * s), int(BASE_SCOOP[1] * s))
                      for s in (1.0, 1.25, MAX_SCOOP_SCALE)]
    hsum = _sat(height, width, depth)
    hhsum = _sat([h * h for h in height], width, depth)
    rough = []
    for j in range(depth):
        for i in range(width):
            h = height[j * width + i]
            dx = abs(height[j * width + i + 1] - h) if i + 1 < width else 0
            dz = abs(height[(j + 1) * width + i] - h) if j + 1 < depth else 0
            rough.append(dx + dz)
    sums = (hsum, hhsum, _sat(rough, width, depth))

    # Type-recognition tables, built only when the inputs for them exist.
    tsums, type_inputs = (None, None, None), []
    if biome is not None and sea_level is not None:
        wet = [1 if h < sea_level else 0 for h in height]
        edge = []
        for j in range(depth):
            for i in range(width):
                k = j * width + i
                e = 0
                if i + 1 < width and wet[k] != wet[k + 1]:
                    e += 1
                if j + 1 < depth and wet[k] != wet[k + width]:
                    e += 1
                edge.append(e)
        counts = {}
        for b in biome:
            counts[b] = counts.get(b, 0) + 1
        top = max(counts, key=counts.get)
        tsums = (_sat(wet, width, depth), _sat(edge, width, depth),
                 _sat([1 if b == top else 0 for b in biome], width, depth))
        type_inputs = ['water_fraction', 'land_fraction', 'coast_density',
                       'dominant_biome_share']

    considered, screened = 0, []
    for bw, bd in sizes:
        for orient_w, orient_d in {(bw, bd), (bd, bw)}:
            cw, cd = orient_w // spacing, orient_d // spacing
            if cw > width or cd > depth:
                continue
            for j in range(0, depth - cd + 1, stride):
                for i in range(0, width - cw + 1, stride):
                    for axis in ('z', 'x'):
                        c = _cheap(sums, i, j, i + cw, j + cd, axis)
                        considered += 1
                        if c is None:
                            continue
                        cr = _coarse_deviation(height, width, i, j, cw, cd,
                                               axis, coarsen)
                        if cr is None:
                            continue
                        screened.append({'i': i, 'j': j, 'cw': cw, 'cd': cd,
                                         'axis': axis, 'coarse_deviation': cr,
                                         **c,
                                         **_type_features(tsums, i, j,
                                                          i + cw, j + cd)})
    # Rank on the coarse deviation -- the same quantity the exact tier ranks on.
    #
    # TILT IS INCLUDED, which reverses an earlier decision here. The comment
    # this replaces argued that "a slope is a legitimate scoop" and excluded
    # tilt from the ranking. That confused two different slopes. One running
    # W-E with the teams at N and S is legitimate and already scores 0.0
    # without any special handling. One running along the team axis puts a
    # team on high ground, and excluding it from the ranking meant actively
    # preferring scoops with that deficit.
    #
    # `spread_gap` stays out, for its own reason: it compares the two halves'
    # standard deviations, so a slope inflates whichever half it falls on and
    # it double-counts tilt in an uninterpretable way. It is still reported.
    screened.sort(key=lambda c: c['coarse_deviation'])
    # Overlapping scoops are the same place.
    buckets = {}
    for c in screened:
        keys = []
        if types:
            keys = [name for name, ok in types.items() if ok(c)]
            if not keys:
                keys = ['unlabelled']
        if partition:
            part = partition(c)
            keys = [f'{k}/{part}' for k in keys] if keys else [part]
        if not keys:
            keys = [None]
        for key in keys:
            kept, taken = buckets.setdefault(key, ([], []))
            if len(kept) >= budget:
                continue
            if all(abs(c['i'] - t['i']) > t['cw'] // 2
                   or abs(c['j'] - t['j']) > t['cd'] // 2 for t in taken):
                kept.append(c); taken.append(c)
    seen, spread = set(), []
    for kept, _ in buckets.values():
        for c in kept:
            mark = (c['i'], c['j'], c['cw'], c['cd'], c['axis'])
            if mark not in seen:
                seen.add(mark); spread.append(c)

    found = []
    for c in spread:
        sub, sw, sd = [], c['cw'], c['cd']
        for j in range(c['j'], c['j'] + sd):
            sub.extend(height[j * width + c['i']: j * width + c['i'] + sw])
        sym = symmetry(sub, sw, sd, c['axis'], spacing_hint=spacing)
        pair = homebase_pair(sub, sw, sd, c['axis'], spacing=spacing,
                             probe_blocks=probe_blocks)
        found.append({
            'origin_sample': [c['i'], c['j']],
            'scoop_blocks': [sw * spacing, sd * spacing],
            'area_blocks2': sw * sd * spacing * spacing,
            **sym,
            'coarse_deviation_blocks': round(c['coarse_deviation'], 3),
            # Carried through so a caller can label a returned scoop with the
            # same predicate that steered the search. Without this the exact
            # tier drops the type facts and a scoop cannot say what it is.
            **{k: round(c[k], 4) for k in
               ('water_fraction', 'land_fraction', 'coast_density',
                'dominant_biome_share') if k in c},
            'homebase_pair': pair,
            'homebase_max_separation_blocks': pair.get('max_separation_blocks'),
        })
    found.sort(key=lambda f: f['mirror_deviation_blocks'])
    return {
        'considered': considered,
        'measured_exactly': len(found),
        'partitions': {str(k): len(v[0]) for k, v in buckets.items()},
        'type_inputs': type_inputs,
        'types_matching_nothing': sorted(
            set(types or ()) - {str(k).split('/')[0] for k in buckets}),
        'tutoring': 'type predicates steer the budget. They do not discard: '
                    'every scoop keeps its full vector, unmatched scoops are '
                    'measured and returned under "unlabelled", and a type '
                    'nobody defined still appears as a described scoop.',
        'budget': budget,
        'sizes_blocks': [list(s) for s in sizes],
        'scoops': found,
        'coarsen': coarsen,
        'ranked_by': 'raw mirror deviation, screened on the same measure at '
                     '1/%d resolution. Tilt is INCLUDED: a slope along the '
                     'team axis is one team on high ground.' % coarsen,
        'uses_no_default_parameters': True,
        'proves': 'elevation only. No ocean, resource, water or buildability '
                  'check happens here.',
    }


# ---------------------------------------------------------------------------
# WHAT KIND OF PLACE a scoop is, as opposed to how symmetric it is.
#
# Everything above reads elevation and nothing else, and that turns out to be
# blind in a way worth stating precisely. Take one heightmap of five domes.
# With sea level at 64 it is an archipelago; with sea level at 0 it is a hill
# field; built from sand it is a desert. `search` returns the IDENTICAL vector
# for all three -- not a close one, the same one, because no key it reports
# could differ. Symmetry is type-neutral to the point of being type-blind.
#
# Three separate blindnesses, with three different remedies:
#
#   WATER         archipelago against highland. A sea level is only what makes
#                 water VISIBLE; it is not the discriminator. `submerged_
#                 fraction` is the same weak scalar `highland_fraction` was --
#                 0.25 describes one lagoon inside one island and twenty
#                 scattered islets equally well. What separates them is the
#                 COMPONENT STRUCTURE: how many land bodies, how the area is
#                 split between them, how many separate water bodies, and how
#                 much coastline per unit area. An archipelago is many
#                 comparable land bodies in one connected sea; a flooded plain
#                 is one land body around one lake; a highland is one land body
#                 and no water at all. All three can share a fraction.
#   MATERIAL      desert against plains. Needs the biome or surface block, and
#                 nothing in this path reads either. This is the real gap.
#   SIGN          chasm against ridge. `prominence` already answers it with
#                 invert=True and was simply never wired in.
#
# And prominence alone is weaker here than it looks: on a field of domes the
# gaps BETWEEN the domes are pits, so peak and pit prominence come back nearly
# equal (41.8 against 41.8). What separates a chasm from a highland is the
# asymmetry between the two distributions, not either one by itself.
#
# NOTHING HERE ASSIGNS A TYPE. It reports the axes a type would be read off,
# for the same reason prominence reports a number and not "a mountain range".


def _components(mask, width, depth):
    """Sizes of the 4-connected true regions in `mask`, largest first."""
    seen = [False] * len(mask)
    sizes = []
    for start in range(len(mask)):
        if seen[start] or not mask[start]:
            continue
        stack, n = [start], 0
        seen[start] = True
        while stack:
            k = stack.pop(); n += 1
            i, j = k % width, k // width
            for ni, nj in ((i - 1, j), (i + 1, j), (i, j - 1), (i, j + 1)):
                if 0 <= ni < width and 0 <= nj < depth:
                    m = nj * width + ni
                    if mask[m] and not seen[m]:
                        seen[m] = True; stack.append(m)
        sizes.append(n)
    return sorted(sizes, reverse=True)


def _coastline(mask, width, depth):
    """Adjacent pairs straddling the land/water boundary."""
    edges = 0
    for j in range(depth):
        for i in range(width):
            k = j * width + i
            if i + 1 < width and mask[k] != mask[k + 1]:
                edges += 1
            if j + 1 < depth and mask[k] != mask[k + width]:
                edges += 1
    return edges


def water_structure(height: list, width: int, depth: int, sea_level: int,
                    *, spacing: int = 8) -> dict:
    """How land and water are ARRANGED, not merely how much of each there is.

    The distinction this exists for: one lagoon inside one island and twenty
    scattered islets both read 0.25 submerged. A fraction cannot tell them
    apart and the component structure can, which is the same argument that
    put prominence next to `highland_fraction`.
    """
    land = [h >= sea_level for h in height]
    n = len(land)
    land_sizes = _components(land, width, depth)
    water_sizes = _components([not v for v in land], width, depth)
    land_total = sum(land_sizes) or 1
    cell = spacing * spacing
    return {
        'sea_level': sea_level,
        'submerged_fraction': round(sum(water_sizes) / n, 4),
        'land_bodies': len(land_sizes),
        'water_bodies': len(water_sizes),
        # How the land is SPLIT. Near 1.0 is one mass; low is a scattering.
        'largest_land_share': round((land_sizes[0] / land_total) if land_sizes else 0.0, 4),
        'land_body_sizes_blocks2': [s * cell for s in land_sizes[:8]],
        'water_body_sizes_blocks2': [s * cell for s in water_sizes[:8]],
        # Edge per unit area: an archipelago is mostly coastline, a plain is not.
        'coastline_per_1k_blocks2': round(
            _coastline(land, width, depth) * spacing / (n * cell) * 1000, 4),
        'labels': 'none. Many comparable land bodies in one sea, one body '
                  'around one lake, and one body with no water are three '
                  'different arrangements that can share a fraction.',
    }


def capability(height: list, width: int, depth: int, *, spacing: int = 8,
               sea_level: int | None = None, material=None) -> dict:
    """The axes that separate one Map Type from another, unlabelled.

    `material` is a per-sample sequence of anything hashable -- biome id or
    surface block. It is accepted and summarised rather than required, so the
    absence of a biome scan shows up as a stated gap in the output instead of
    silently becoming "no desert here".
    """
    from .prominence import features
    peaks = features(height, width, depth, spacing=spacing, keep=8)
    pits = features(height, width, depth, spacing=spacing, keep=8, invert=True)
    top_peak = peaks[0]['prominence'] if peaks else 0.0
    top_pit = pits[0]['prominence'] if pits else 0.0

    out = {
        'relief_blocks': round(_relief(height), 2),
        # SIGN. Positive leans highland, negative leans chasm. Near zero means
        # the two are balanced, which a dome field is too -- see the note above.
        'peak_prominence': round(top_peak, 2),
        'pit_prominence': round(top_pit, 2),
        'relief_sign': round(_gap(top_peak, top_pit) *
                             (1 if top_peak >= top_pit else -1), 4),
    }
    if sea_level is None:
        out['water'] = None
        out['water_gap'] = ('no sea level given, so nothing about land and '
                            'water arrangement can be read here')
    else:
        out['water'] = water_structure(height, width, depth, sea_level,
                                       spacing=spacing)
    if material is None:
        out['material_mix'] = None
        out['material_gap'] = ('no biome or surface block given. Desert, '
                               'plains and savanna are the same scoop to this '
                               'module; nothing in the elevation path can '
                               'separate them.')
    else:
        counts = {}
        for m in material:
            counts[m] = counts.get(m, 0) + 1
        n = len(material) or 1
        out['material_mix'] = {k: round(v / n, 4) for k, v in
                               sorted(counts.items(), key=lambda kv: -kv[1])[:8]}
    out['labels'] = 'none. These are the axes a Map Type is read off, not a type.'
    return out
