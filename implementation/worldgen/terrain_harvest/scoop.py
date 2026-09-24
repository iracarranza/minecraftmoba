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
