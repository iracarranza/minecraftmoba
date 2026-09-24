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

Both are computed, and the mirror term is split into TILT and RESIDUAL: a
scoop laid across a slope has one end higher than the other, which is a
gameplay question about high ground and not a difference in character. The
trend is fitted and removed before the mismatch is measured, so a mirror line
drawn down the middle of a slope reads as the clean mirror it is.

That split turned out to matter more than expected. Before detrending, the
z axis looked markedly more symmetric than x on these finalists (deviation 20
against 34). After it, the two are indistinguishable (18.9 against 19.7): the
entire apparent difference was an east-west slope of up to 76 blocks being
read as terrain asymmetry. The axis choice was being made on a tilt.

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

    ra, rb = _relief(va), _relief(vb)
    ga = _roughness(height, width, depth, *[a[0], a[1], a[2], a[3]])
    gb = _roughness(height, width, depth, *[b[0], b[1], b[2], b[3]])
    return {
        'team_axis': axis,
        # mirror, decomposed. Tilt is the constant height offset across the
        # split -- a slope, not a difference in character. Residual is what
        # remains, and near-zero residual would mean literal reflection, which
        # noise never is.
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
# So the screen is a COARSE version of the ranking: the same detrended mirror
# residual on a subsampled grid. It costs 1/k^2 of the exact pass and measures
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


def _coarse_residual(height, width, i0, j0, cw, cd, axis, k):
    """Detrended mirror residual on every k-th sample. Same shape as `symmetry`."""
    cells, coords = [], []
    for j in range(0, cd, k):
        for i in range(0, cw, k):
            cells.append(height[(j0 + j) * width + i0 + i])
            coords.append(j if axis == 'z' else i)
    n = len(cells)
    if n < 4:
        return None
    mean_c = sum(coords) / n
    mean_h = sum(cells) / n
    den = sum((c - mean_c) ** 2 for c in coords) or 1.0
    slope = sum((c - mean_c) * (h - mean_h) for c, h in zip(coords, cells)) / den
    flat = [h - slope * (c - mean_c) for c, h in zip(coords, cells)]
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


def search(height: list, width: int, depth: int, *, spacing: int = 8,
           sizes=None, stride: int = 8, budget: int = 24, coarsen: int = 4,
           probe_blocks: int = HOMEBASE_PROBE_BLOCKS) -> dict:
    """Scoops of several sizes, ranked by how alike their two ends are.

    `sizes` are (w, d) in BLOCKS; the default sweeps the current compiler
    window up to `MAX_SCOOP_SCALE`. Returns the exact reading for the best
    `budget` candidates and the count of everything considered, so the search
    is reported rather than only its winners.
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
                        cr = _coarse_residual(height, width, i, j, cw, cd,
                                              axis, coarsen)
                        if cr is None:
                            continue
                        screened.append({'i': i, 'j': j, 'cw': cw, 'cd': cd,
                                         'axis': axis, 'coarse_residual': cr, **c})
    # Rank on the coarse residual -- the same quantity the exact tier ranks on.
    #
    # Tilt is excluded deliberately: a slope is a legitimate scoop, and ranking
    # against it would reproduce at the cheap tier the mistake detrending was
    # added to fix at the exact one. The coarse residual is detrended for
    # exactly that reason.
    #
    # `spread_gap` is excluded too, less obviously. It compares the two halves'
    # standard deviations, and a slope across the scoop inflates the variance
    # of whichever half it falls on -- so ranking on it was tilt-sensitive
    # through the back door. Adding a uniform x-slope to a test region moved
    # the winner from [16,0] to [20,0] with the terrain otherwise unchanged.
    screened.sort(key=lambda c: c['coarse_residual'])
    # Overlapping scoops are the same place.
    spread, taken = [], []
    for c in screened:
        if all(abs(c['i'] - t['i']) > t['cw'] // 2 or abs(c['j'] - t['j']) > t['cd'] // 2
               for t in taken):
            spread.append(c); taken.append(c)
        if len(spread) >= budget:
            break

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
            'coarse_residual_blocks': round(c['coarse_residual'], 3),
            'homebase_pair': pair,
            'homebase_max_separation_blocks': pair.get('max_separation_blocks'),
        })
    found.sort(key=lambda f: f['mirror_residual_blocks'])
    return {
        'considered': considered,
        'measured_exactly': len(found),
        'budget': budget,
        'sizes_blocks': [list(s) for s in sizes],
        'scoops': found,
        'coarsen': coarsen,
        'ranked_by': 'mirror residual after detrending, screened on the same '
                     'measure at 1/%d resolution. Tilt is reported and NOT '
                     'ranked on -- a slope is a legitimate scoop.' % coarsen,
        'uses_no_default_parameters': True,
        'proves': 'elevation only. No ocean, resource, water or buildability '
                  'check happens here.',
    }
