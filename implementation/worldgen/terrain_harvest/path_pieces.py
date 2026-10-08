"""Assemble a corridor from declared pieces, and check that it connects.

The 22 September route audit measured the frozen map's corridors and found
40.8% of steps required a jump and 7.6% could not be climbed at all. The fitted
`walkable_profile` in `routes.py` fixed that specific defect. What it did not
address is that the corridor is still *uniform everywhere*: every column gets
the same treatment vocabulary -- worn, assimilated, constructed -- and nothing
says what the path IS at a given place.

Vanilla villages suggest the missing half. A plains street is not carved as a
continuous surface; it is assembled from separate straight and corner templates
that declare what they connect to, with `terrain_matching` so a piece adapts to
the ground it lands on. The transferable idea is not the jigsaw machinery, it
is that **a piece declares its own connections and its own terrain tolerance**.

What vanilla does not need, and this project does, is the last step: village
assembly guarantees pieces fit each other and guarantees nothing about whether
the finished street connects the places it was meant to. The audit exists
because that was never checked, so it is checked here.

This module is pure: it takes a fitted profile and the raw ground beneath it,
and returns pieces. It writes no blocks.
"""
from dataclasses import dataclass
from typing import List, Optional, Sequence

#: Deviation at which the ground can no longer carry a walker and must be spanned.
SPAN_DEVIATION = 3

#: How far a piece may sit from the ground it covers before it is the wrong piece.
DEFAULT_TOLERANCE = 2

#: Columns a deck may cross before the corridor, not the bridge, is the problem.
#:
#: A sixty-column deck is not a crossing, it is a viaduct, and it almost always
#: means the centreline was routed through something it should have gone around.
#: Capping it turns a silent monstrosity into a reported fault.
MAX_SPAN = 24

#: What each kind can accept beneath it.
#:
#: Grounded pieces are limited by VERTICAL deviation: a straight sitting three
#: blocks above the ground is the wrong piece, whatever its length. A bridge is
#: the opposite -- it exists precisely because the ground is far away, so
#: height says nothing about whether it is the right piece and LENGTH says
#: everything. The two are not the same quantity wearing different numbers, and
#: a single tolerance field would have hidden that.
TOLERANCE = {
    'straight': DEFAULT_TOLERANCE,
    'bend': DEFAULT_TOLERANCE,
    'stairs': DEFAULT_TOLERANCE,
    'landing': DEFAULT_TOLERANCE,
    'bridge': None,                 # unbounded vertically, bounded by MAX_SPAN
    'entrance': 0,                  # a threshold is met exactly or it is not met
}


@dataclass(frozen=True)
class Piece:
    """One unit of path, covering a span of centreline columns.

    ``entry`` and ``exit`` are *end kinds*, and they are what connection
    validation reads. They are deliberately coarse -- a path has few ways of
    meeting another path -- because a rich vocabulary here would make every
    piece's compatibility a special case, which is the thing jigsaw's
    connection system exists to avoid.
    """

    kind: str
    start: int
    end: int
    entry: str
    exit: str
    rise: int = 0
    tolerance: int = DEFAULT_TOLERANCE

    @property
    def columns(self) -> int:
        return self.end - self.start + 1

    def __str__(self) -> str:
        return f'{self.kind}[{self.start}:{self.end}]{self.rise:+d}'


#: Which end kinds may meet. Symmetric, and short on purpose.
#:
#: The rule that earns its place is ``slope`` not meeting ``slope``: two runs
#: of stairs cannot abut, because the step between them is unbounded by either
#: piece. A landing between them is what makes the join measurable, and it is
#: exactly the kind of constraint village templates encode and a carved
#: corridor cannot.
COMPATIBLE = {
    ('flat', 'flat'),
    ('flat', 'slope'),
    ('slope', 'flat'),
    ('flat', 'deck'),
    ('deck', 'flat'),
    ('flat', 'portal'),
    ('portal', 'flat'),
}


def connects(left: Piece, right: Piece) -> bool:
    """Whether ``right`` may follow ``left``."""
    return (left.exit, right.entry) in COMPATIBLE


def column_character(profile: Sequence[Optional[int]],
                     raw: Sequence[Optional[int]],
                     i: int,
                     water: Sequence[bool] = (),
                     turns: Sequence[int] = (),
                     span_deviation: int = SPAN_DEVIATION,
                     termini: bool = True) -> str:
    """What the path is at one column, before any grouping.

    The atomic rule, and the one that exists twice -- here and in the plugin's
    LabGeometry. Both run fixtures/path-character-crosscheck.json. Grouping
    into runs is derived from this and is the compiler's business alone.

    <h2>Priority, and why this order</h2>

    ``entrance`` > ``bridge`` > ``stairs`` > ``bend`` > ``straight``.

    A terminus is a STRUCTURAL fact -- it is where the path joins something
    that was there first -- so it outranks everything about the ground. Water
    or a void beneath is a PHYSICAL fact and outranks shape: a column cannot be
    stepped through because it slopes. Slope is shape, a turn is direction, and
    straight is what is left.

    The two implementations disagreed on this before the fixture. Java checked
    slope first, so a sloping column over water read as a stair; Python checked
    the void first and had no terminus at all, so a path's ends were whatever
    the ground happened to be doing there.
    """
    n = len(profile)
    if termini and n and (i == 0 or i == n - 1):
        return 'entrance'
    if i < len(water) and water[i]:
        return 'bridge'
    p, r = (profile[i], raw[i]) if i < len(raw) else (None, None)
    if p is not None and r is not None and p - r >= span_deviation:
        return 'bridge'
    if i > 0 and p is not None and profile[i - 1] is not None and p != profile[i - 1]:
        return 'slope_up' if p > profile[i - 1] else 'slope_down'
    if i in set(turns):
        return 'bend'
    return 'straight'


def segment(profile: Sequence[Optional[int]],
            raw: Sequence[Optional[int]],
            turns: Sequence[int] = (),
            span_deviation: int = SPAN_DEVIATION,
            water: Sequence[bool] = (),
            termini: bool = False) -> List[Piece]:
    """Cut a fitted profile into pieces.

    Each column is first given a *character* from what the path is doing there,
    then runs of like character become pieces. Deciding character per column and
    grouping afterwards keeps the rule simple enough to state: a column is a
    span if the profile stands too far above the ground to walk on, a slope if
    the profile changes height, a turn where the centreline turns, and flat
    otherwise.

    ``turns`` are indices where the centreline changes direction; the caller
    knows the geometry and this module does not.
    """
    n = len(profile)
    if n == 0:
        return []

    character = []
    for i in range(n):
        if profile[i] is None or raw[i] is None:
            character.append('flat')          # unknown ground is not a reason to build
            continue
        # Termini are opt-in. A corridor cut for inspection has no structure at
        # its ends, and calling those columns entrances would demand thresholds
        # that do not exist.
        c = column_character(profile, raw, i, water, turns, span_deviation,
                             termini=termini)
        character.append(_INTERNAL[c])

    pieces: List[Piece] = []
    start = 0
    for i in range(1, n + 1):
        if i < n and character[i] == character[start]:
            continue
        pieces.append(_piece(character[start], start, i - 1, profile))
        start = i
    return _insert_landings(pieces, profile)


#: Shared names to the segmenter's internal ones. The cross-check speaks the
#: shared vocabulary; the run-grouping below is the compiler's own business.
_INTERNAL = {'entrance': 'portal_end', 'bridge': 'span', 'bend': 'turn',
             'straight': 'flat', 'slope_up': 'slope_up', 'slope_down': 'slope_down'}


_KINDS = {
    'flat': ('straight', 'flat', 'flat'),
    'portal_end': ('entrance', 'flat', 'portal'),
    'turn': ('bend', 'flat', 'flat'),
    'span': ('bridge', 'deck', 'deck'),
    'slope_up': ('stairs', 'slope', 'slope'),
    'slope_down': ('stairs', 'slope', 'slope'),
}


def _piece(character: str, start: int, end: int, profile) -> Piece:
    kind, entry, exit_ = _KINDS[character]
    tolerance = TOLERANCE.get(kind, DEFAULT_TOLERANCE)
    rise = 0
    # A slope column is the one you ARRIVE at, so the run's climb includes the
    # step into it. Measuring from the run's own first column loses exactly one
    # block of every stairs piece -- which would then disagree with the profile
    # it was cut from, and validate() would be checking the wrong number.
    base = start - 1 if kind == 'stairs' and start > 0 else start
    a, b = profile[base], profile[end]
    if a is not None and b is not None:
        rise = b - a
    return Piece(kind, start, end, entry, exit_, rise,
                 DEFAULT_TOLERANCE if tolerance is None else tolerance)


def _insert_landings(pieces: List[Piece], profile) -> List[Piece]:
    """Put a landing between runs of stairs that cannot legally abut.

    Rather than refusing the path. A reversal of slope is an ordinary thing for
    terrain to do, and a path authoring system that threw its hands up at a
    ridge would be describing the terrain's difficulty rather than solving it.

    The landing is zero-length: it occupies the boundary column the two runs
    share. Giving it width would move every later piece and make the pieces
    stop describing the profile they were cut from.
    """
    if len(pieces) < 2:
        return pieces
    out = [pieces[0]]
    for right in pieces[1:]:
        left = out[-1]
        if not connects(left, right):
            at = right.start
            out.append(Piece('landing', at, at, 'flat', 'flat', 0,
                             TOLERANCE['landing']))
        out.append(right)
    return out


def validate(pieces: Sequence[Piece]) -> List[str]:
    """Faults in an assembled path, as readable complaints.

    Empty means the path is sound. This is the half of the pipeline vanilla
    gets for free from jigsaw and this project has never had: it is possible
    today to author a corridor whose pieces do not meet, and nothing notices.
    """
    faults = []
    for left, right in zip(pieces, pieces[1:]):
        if left.end + 1 != right.start and not (left.start == left.end == right.start):
            faults.append(f'{left} and {right} leave a gap between them')
        if not connects(left, right):
            faults.append(f'{left} cannot connect to {right}: '
                          f'{left.exit} does not meet {right.entry}')
    for p in pieces:
        if p.kind == 'stairs':
            continue
        if p.rise != 0:
            faults.append(f'{p} changes height by {p.rise:+d}, which only stairs may do')
    return faults


def terrain_faults(pieces: Sequence[Piece],
                   profile: Sequence[Optional[int]],
                   raw: Sequence[Optional[int]],
                   max_span: int = MAX_SPAN) -> List[str]:
    """Pieces placed where they cannot sit.

    Kept apart from {@link validate} on purpose: that one is topology -- do the
    pieces meet -- and this one is ground. A path can be perfectly assembled
    and still be resting on nothing, and conflating the two would make a report
    that says "sound" mean two different things.

    Segmentation derives most pieces from these same numbers, so most of this
    passes by construction. It earns its place on the cases segmentation cannot
    see: the landing, which is SYNTHESISED at a slope reversal and never
    checked against the ground under it, and any piece supplied by a caller
    rather than cut from a profile.
    """
    faults = []
    for p in pieces:
        if p.kind == 'bridge':
            if p.columns > max_span:
                faults.append(f'{p} spans {p.columns} columns, past the {max_span} '
                              f'at which the corridor rather than the crossing is wrong')
            continue
        worst = 0
        for i in range(p.start, min(p.end, len(profile) - 1, len(raw) - 1) + 1):
            if profile[i] is None or raw[i] is None:
                continue                    # unmeasured is not a fault; see below
            worst = max(worst, abs(profile[i] - raw[i]))
        if worst > p.tolerance:
            faults.append(f'{p} sits {worst} from the ground, past its tolerance of '
                          f'{p.tolerance}')
    return faults


def unmeasured(pieces: Sequence[Piece],
               profile: Sequence[Optional[int]],
               raw: Sequence[Optional[int]]) -> int:
    """Columns no judgement could be made about.

    Reported rather than folded into the faults, because an unknown is not a
    defect and counting it as one would make a well-built path over unsurveyed
    ground indistinguishable from a badly built one.
    """
    total = 0
    for p in pieces:
        for i in range(p.start, min(p.end, len(profile) - 1, len(raw) - 1) + 1):
            if profile[i] is None or raw[i] is None:
                total += 1
    return total


def entrance(at: int, threshold: int) -> Piece:
    """The transition from path to building, at one column.

    Its tolerance is **zero**, which is the point of having it as a piece
    rather than as the last straight: a threshold is a specific height that a
    door is actually at, and "close enough" is how a path ends four blocks
    above a doorway.
    """
    return Piece('entrance', at, at, 'flat', 'portal', 0, TOLERANCE['entrance'])


def entrance_faults(pieces: Sequence[Piece],
                    profile: Sequence[Optional[int]],
                    thresholds: dict,
                    max_step: int = 1) -> List[str]:
    """Whether a path can actually be walked INTO the places it goes to.

    This is a gap rather than a refinement. {@link traverse} measures the walk
    ALONG a corridor and nothing measures the step at the end of it, so a Route
    can report a perfect traversal and finish at a wall five blocks under the
    door. The corridor's own statistics would call that a success.

    ``thresholds`` maps a column index to the floor height of the structure
    there -- the height a player actually arrives at, which the caller knows
    and this module cannot.
    """
    faults = []
    for p in pieces:
        if p.kind != 'entrance':
            continue
        threshold = thresholds.get(p.start)
        if threshold is None:
            faults.append(f'{p} is an entrance to nothing: no threshold at column {p.start}')
            continue
        standing = profile[p.start] if p.start < len(profile) else None
        if standing is None:
            continue                        # unmeasured, and counted elsewhere
        step = abs(threshold - standing)
        if step > max_step:
            faults.append(f'{p} arrives {step} from its threshold, which is '
                          f'{"a drop" if standing > threshold else "a climb"} '
                          f'no walker takes in one step')
    # An unserved threshold is the same defect read from the other side: the
    # path was built, the building is there, and they do not meet.
    served = {p.start for p in pieces if p.kind == 'entrance'}
    for column in sorted(thresholds):
        if column not in served:
            faults.append(f'the structure at column {column} has no entrance piece')
    return faults


@dataclass(frozen=True)
class Traversal:
    """What walking the assembled path actually costs."""

    steps: int
    jumps: int
    unclimbable: int
    worst_step: int

    @property
    def jump_share(self) -> float:
        return self.jumps / self.steps if self.steps else 0.0

    @property
    def walkable(self) -> bool:
        return self.unclimbable == 0


def traverse(profile: Sequence[Optional[int]], max_step: int = 1) -> Traversal:
    """Measure the walk, which is the step the piece vocabulary exists to serve.

    The audit's own metric, kept identical so the before and after are
    comparable: adjacent centreline columns, a jump at a step of one, and
    unclimbable at two. Measuring anything else here would make the improvement
    unprovable.
    """
    known = [(a, b) for a, b in zip(profile, profile[1:])
             if a is not None and b is not None]
    deltas = [abs(b - a) for a, b in known]
    return Traversal(
        steps=len(deltas),
        jumps=sum(1 for d in deltas if d >= max_step),
        unclimbable=sum(1 for d in deltas if d > max_step),
        worst_step=max(deltas) if deltas else 0,
    )


def summarise(pieces: Sequence[Piece]) -> dict:
    """Counts by kind, for a report that says what was built and not only how much."""
    out: dict = {}
    for p in pieces:
        out[p.kind] = out.get(p.kind, 0) + 1
    return out
