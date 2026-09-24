"""How much a terrain feature stands out, independent of where it is.

`highland_fraction_west` is 0.31 for a single massif and 0.31 for fifty
scattered hillocks. Same number, completely different map -- the existing
vocabulary can express how MUCH high ground there is and not whether it is one
thing or a scattering of things.

Prominence is the standard topographic answer: a peak's prominence is its
height above the lowest saddle connecting it to any higher ground. A 300-block
massif standing alone has high prominence; a bump on the shoulder of a larger
range has almost none however tall it is in absolute terms.

WHY IT MATTERS HERE. It is TYPE-NEUTRAL, which is what the current vocabulary
is not. Seven of the 25 metrics are measured on one side only --
`highland_fraction_west` cannot describe eastern highlands at all, since it
returns a number about the wrong place rather than a low number. Prominence
asks "is there a feature, how much does it stand out" and leaves WHERE it is to
a separate factor, so one measurement can serve Default, Chasm and Archipelago
instead of one template each.

NO THRESHOLDS. The algorithm needs none: prominence is defined per feature by
the saddle at which it merges into higher terrain, not by a cutoff someone
chose. This reports the features and their numbers; what counts as "a mountain
range" is a template's business and is not decided here.

PEAKS AND PITS. Running the same sweep on negated elevation gives pit
prominence -- depth below the rim -- so a chasm is described by the same
measurement that describes a highland, which is the test of whether a neutral
vocabulary actually works.
"""
from __future__ import annotations


def from_rle(pairs) -> list:
    """Expand [[value, run], ...] as the candidate's feature_grid stores it."""
    out = []
    for value, run in pairs:
        out.extend([value] * run)
    return out


class _Union:
    def __init__(self):
        self.parent = {}
        self.peak = {}          # root -> (elevation, index) of its summit
        self.size = {}

    def add(self, i, elevation):
        self.parent[i] = i
        self.peak[i] = (elevation, i)
        self.size[i] = 1

    def find(self, i):
        while self.parent[i] != i:
            self.parent[i] = self.parent[self.parent[i]]
            i = self.parent[i]
        return i


def features(height: list, width: int, depth: int, *, spacing: int = 8,
             invert: bool = False, keep: int = 12) -> list:
    """Prominence of every local summit, by descending-sweep union-find.

    Cells are added from highest to lowest. When a cell joins two components,
    the lower of the two summits is closed off: the cell being added IS its key
    saddle, so its prominence is its own height minus that saddle. No threshold
    is involved anywhere.

    `invert` measures pits instead -- depth below the rim.
    """
    values = [-h for h in height] if invert else list(height)
    order = sorted(range(len(values)), key=lambda i: -values[i])
    uf = _Union()
    closed = []

    for i in order:
        uf.add(i, values[i])
        x, y = i % width, i // width
        roots = []
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < width and 0 <= ny < depth:
                j = ny * width + nx
                if j in uf.parent:
                    r = uf.find(j)
                    if r not in roots:
                        roots.append(r)
        if not roots:
            continue
        # Merge into the tallest neighbour; every other summit closes here.
        roots.sort(key=lambda r: -uf.peak[r][0])
        main = roots[0]
        for other in roots[1:]:
            elevation, index = uf.peak[other]
            closed.append({
                'summit_index': index,
                'summit_xz': [(index % width) * spacing, (index // width) * spacing],
                'height': -elevation if invert else elevation,
                'prominence': round(elevation - values[i], 2),
                'extent_cells': uf.size[other],
                'extent_blocks2': uf.size[other] * spacing * spacing,
            })
            uf.parent[other] = main
            uf.size[main] += uf.size[other]
        uf.parent[uf.find(i)] = main
        uf.size[main] += 1
        if uf.peak[main][0] < values[i]:
            uf.peak[main] = (values[i], i)

    # The global summit never merges, so it never closes. Its prominence is the
    # full relief of the window, which is the honest value rather than null.
    if uf.parent:
        root = uf.find(order[0])
        elevation, index = uf.peak[root]
        closed.append({
            'summit_index': index,
            'summit_xz': [(index % width) * spacing, (index // width) * spacing],
            'height': -elevation if invert else elevation,
            'prominence': round(elevation - min(values), 2),
            'extent_cells': uf.size[root],
            'extent_blocks2': uf.size[root] * spacing * spacing,
            'global_summit': True,
        })

    closed.sort(key=lambda f: -f['prominence'])
    return closed[:keep]


def describe(feature_grid: dict, *, keep: int = 12) -> dict:
    """Peaks and pits for a candidate's height grid. Locations stay separate."""
    width, depth = feature_grid['width'], feature_grid['height']
    spacing = feature_grid.get('sample_spacing_blocks', 8)
    height = from_rle(feature_grid['height_rle'])
    if len(height) != width * depth:
        return {'measured': False,
                'why': f'height grid is {len(height)} cells, expected {width * depth}'}
    peaks = features(height, width, depth, spacing=spacing, keep=keep)
    pits = features(height, width, depth, spacing=spacing, invert=True, keep=keep)
    return {
        'measured': True,
        'grid': {'width': width, 'height': depth, 'spacing_blocks': spacing},
        'peaks': peaks,
        'pits': pits,
        'relief_blocks': max(height) - min(height),
        'units': 'prominence and height in blocks; extent in blocks squared',
        'neutral': 'no compass direction and no threshold. Where a feature sits is a '
                   'separate factor, and what counts as a range is a template decision.',
    }
