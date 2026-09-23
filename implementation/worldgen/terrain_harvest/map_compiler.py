"""The Default-map compiler, as stages a candidate can fail at by name.

The pipeline already existed as a sequence of scripts that each either worked or
raised. What it did not have was a way to say WHERE a seed stopped and WHY, so a
rejected seed produced either a traceback or a scalar, and neither tells you
whether the geography was wrong or the recognizer was.

    MapCandidate -> CompetitiveCandidate -> AuthoredCandidate -> PlayableMap

Every stage returns machine-readable rejections. A candidate that fails carries
the stage it failed at and the codes it failed with, which is the difference
between "this seed is bad" and "this seed has no Lair socket with the vertical
clearance the measured End Spike needs".

Nothing here forces terrain into compliance. A stage that rejects is reporting
geography the authoring layer is not permitted to repair -- the rule is to find
terrain that makes simple authoring reliable, not to make authoring powerful
enough to repair bad terrain.
"""
from __future__ import annotations

import itertools
import json
import math
from dataclasses import dataclass, field
from pathlib import Path

from vanilla_search.task_a import fit
from vanilla_search import structures
from . import (build_structures, column_scan, lair_mark, lair_socket,
               objective_forms, readiness, spatial_contract, worksite_portfolio)

# Stage names, in order. A candidate reaches at most one past where it fails.
STAGES = ('recognize', 'homebase', 'hinterland', 'objectives', 'lair',
          'select', 'author', 'verify', 'ready')

# PROVISIONAL_ALPHA constants. Each is measured or derived from a measurement,
# never chosen from intuition; see docs/audit/2026-09-23-map-compiler-slice.md.
#
# The Lair access-parity bound is deliberately absent: §10 says measure the
# distribution first, and the sample does not exist yet. `lair` therefore
# reports A_L and does not gate on it.
# Screen warnings that are NOT statements about regional shape, and so must not
# reject at this stage.
#
# `six_corridors_connect` fires on all eight screened finalists. It is the
# straight-line corridor proxy an earlier pass already showed to be a false
# positive -- corridors follow terrain, and sampling them as straight lines
# reported a frozen map as having none when it had them. Route fitting measures
# the real thing later, on terrain-following paths, so rejecting here would
# reject every candidate the screen has ever produced on a signal already known
# to be wrong. Recorded as deferred evidence rather than silently dropped.
NOT_REGIONAL_SHAPE = frozenset({'six_corridors_connect'})

# How many candidate sites per objective the joint ordinal search may choose
# among. One is what the siting search used to return, and one per layer cannot
# satisfy a joint constraint except by luck.
#
# Raised from 12 to 24 once footprint separation joined the ordinal as a
# constraint: the two together are tight enough that 12 candidates left one of
# 99887766's two teams with no legal layout, while 24 gives both one and 40
# adds nothing. The cost is a few thousand more combinations in an exhaustive
# search that already runs in well under a second.
OBJECTIVE_POOL = 24

PROVISIONAL = {
    # A socket must be able to accept the Core on its OWN terms -- never by
    # comparison with the other team's. Measured over 28 fitted sockets (eight
    # screened finalists and six locally generated seeds), usable fraction runs
    # 0.110 to 0.950 with a clear break: six sockets cluster at 0.110-0.468,
    # then nothing until 0.559. 0.52 sits in that gap, and any bound in
    # [0.48, 0.55] rejects exactly the same six. Raising it to 0.70 would
    # reject half of all sockets including ones the screen selected, which is a
    # different and much stronger claim than this evidence supports.
    'min_homeland_developable_fraction': 0.52,
    # The Hinterland is compact. Expressed as a share of the candidate's own
    # sampled area, not an absolute radius, because the spec refuses a fixed
    # radius and the sample window varies.
    'max_hinterland_fraction_of_end': 0.5,
    # PROVISIONAL_ALPHA, and the one constant that decides a genuine competitive
    # constraint.
    #
    # HELD AT 0.15, BUT ITS ORIGINAL JUSTIFICATION NO LONGER HOLDS. It was
    # chosen from eight finalists, where best-achievable A_L ran
    # 0.001 0.050 0.073 | 0.226 0.294 0.415 0.678 0.787 and the widest gap by
    # far was 0.073-0.226; the recorded claim was that any bound inside that gap
    # selected the same three seeds, so the choice was insensitive.
    #
    # With 31 seeds (the eight finalists plus the locally generated ones) the
    # distribution is continuous -- 0.000 0.000 0.000 0.001 x4 0.022 0.038 0.050
    # 0.068 0.073 0.119 0.173 0.226 0.272 ... 1.890 -- and there are now values
    # at 0.119 and 0.173 inside the gap that justified the number. The gap is
    # gone. Sensitivity is smooth: 0.10 admits 39% of seeds with a viable
    # socket, 0.15 admits 42%, 0.20 admits 45%.
    #
    # So the value is unchanged, because more data is not by itself a reason to
    # move it, but its status is not: 0.15 is now a judgement about how much
    # access disparity is tolerable, not a break the evidence hands us. It stays
    # PROVISIONAL_ALPHA and the first playtests are what should settle it.
    # Evidence: reports/map_compiler_2026-09-23/lair-access-distribution-expanded.json
    'max_lair_access_asymmetry': 0.15,
    # PROVISIONAL_ALPHA. Blocks of cut plus fill per footprint column to level a
    # site, measured over 1800 random footprints across three generated worlds:
    # median 2.20, p75 3.86, p90 6.26, p95 8.16, max 21.5, and the distribution
    # is stable world to world (p90 6.0-6.9). 10.0 accepts 97.2% and rejects the
    # worst 3% as genuine terrain surgery, which is the doctrine rule -- reject a
    # socket rather than repair it -- applied to the cases it was written for
    # rather than to ordinary undulation. 8.0 would reject 5.6%, 12.0 only 1.7%.
    'max_levelling_moved_per_column': 10.0,
}


@dataclass
class Rejection:
    stage: str
    code: str
    detail: str
    data: dict = field(default_factory=dict)

    def as_dict(self):
        return {'stage': self.stage, 'code': self.code,
                'detail': self.detail, **({'data': self.data} if self.data else {})}


@dataclass
class Compilation:
    seed: int
    reached: str = 'recognize'
    rejections: list = field(default_factory=list)
    evidence: dict = field(default_factory=dict)

    @property
    def verified(self) -> bool:
        """The compiler's own result: the geography satisfies the contract."""
        return self.reached in ('verify', 'ready') and not self.rejections

    @property
    def ready(self) -> bool:
        """Claimable for a real match. Strictly stronger than verified."""
        return self.reached == 'ready' and not self.rejections

    @property
    def playable(self) -> bool:
        # Retained name; it means VERIFIED, and it is deliberately NOT the same
        # thing as claimable. See readiness.py.
        return self.verified

    def fail(self, stage, code, detail, **data):
        self.rejections.append(Rejection(stage, code, detail, data))
        return self

    def as_dict(self):
        return {
            'schema': 'map_compilation/1',
            'seed': self.seed,
            'deepest_stage_reached': self.reached,
            'verified': self.verified,
            'ready': self.ready,
            'playable': self.playable,
            'rejections': [r.as_dict() for r in self.rejections],
            'evidence': self.evidence,
        }


def recognize(candidate, out: Compilation):
    """Is this Default geography at all?

    W-E is the REGIONAL axis and its contrast is intentional; N-S is the TEAM
    axis. This stage reads the regional gradient only. It must NOT reject a
    seed because the two team ends differ -- that is the superseded target.
    """
    out.reached = 'recognize'
    m = candidate.get('metrics', {})
    needed = ('western_mean_elevation_advantage_y', 'east_actual_ocean_fraction',
              'open_buildable_fraction_of_land')
    missing = [k for k in needed if k not in m]
    if missing:
        return out.fail('recognize', 'NO_REGIONAL_METRICS',
                        'the candidate carries no regional measurements to recognize',
                        missing=missing)
    out.evidence['regional'] = {k: m[k] for k in needed}
    warnings = candidate.get('experimental_screen', {}).get('warnings', [])
    shape = [w for w in warnings if w not in NOT_REGIONAL_SHAPE]
    deferred = [w for w in warnings if w in NOT_REGIONAL_SHAPE]
    if deferred:
        out.evidence['deferred_screen_warnings'] = deferred
    if shape:
        out.fail('recognize', 'NO_DEFAULT_REGIONAL_SHAPE',
                 'the regional screen rejected this orientation',
                 warnings=shape)
    return out


def homebase(result, out: Compilation):
    """Two independently acceptable Homebase sockets. They need not resemble each other."""
    out.reached = 'homebase'
    homes = result.get('homelands') or {}
    if len(homes) != 2:
        return out.fail('homebase', 'NO_HOMEBASE_SOCKET_PAIR',
                        'the fitter could not place both homelands',
                        placed=sorted(homes))
    bound = PROVISIONAL['min_homeland_developable_fraction']
    for team, home in homes.items():
        quality = home.get('quality', home.get('diagnostics', {}).get('usable_fraction'))
        if quality is None:
            out.fail('homebase', 'SOCKET_UNMEASURED',
                     f'{team} socket has no usable-fraction measurement')
        elif quality < bound:
            # Judged on its own terms. Never against the other socket.
            out.fail('homebase', 'SOCKET_NOT_INDEPENDENTLY_ACCEPTABLE',
                     f'{team} socket cannot accept the Core with bounded integration',
                     team=team, usable_fraction=quality, bound=bound)
    out.evidence['homelands'] = {t: h.get('quality') for t, h in homes.items()}
    # The Fountain is Core geometry and the respawn/reconstruction anchor, so its
    # world position is a runtime binding rather than a diagnostic.
    out.evidence['fountains'] = {
        t: h['fountain']['world_xyz'] for t, h in homes.items() if h.get('fountain')}
    out.evidence['homeland_world'] = {
        t: h.get('world_bounds') for t, h in homes.items()}
    return out


def hinterland(result, out: Compilation):
    """A compact opening envelope, with Wilderness around it including poleward."""
    out.reached = 'hinterland'
    depth = result.get('regional_depth') or {}
    if not depth:
        return out.fail('hinterland', 'NO_DEPTH_CLASSIFICATION',
                        'no regional depth bands, so the opening envelope cannot be bounded')
    for team, bands in depth.items():
        counts = bands.get('land_band_counts', {})
        total = sum(counts.values()) or 1
        opening = counts.get('opening', 0) + counts.get('fringe', 0)
        share = opening / total
        if share > PROVISIONAL['max_hinterland_fraction_of_end']:
            out.fail('hinterland', 'HINTERLAND_CONSUMES_TEAM_END',
                     f'{team} opening envelope is {share:.1%} of its end; a compact '
                     f'envelope cannot be the whole end of the map',
                     team=team, opening_fraction=round(share, 4))
        out.evidence.setdefault('hinterland', {})[team] = round(share, 4)
    return out


def objectives(candidate, result, out: Compilation):
    """Three coexisting Wilderness structures, ordered along the team axis.

    The siting search already existed; what it lacked was the current forms and
    anything checking the ordinal it produced. Both are wired here. Lateral
    (W-E) displacement and unequal N-S gaps are not checked, because they are
    explicitly permitted -- only the order is.
    """
    out.reached = 'objectives'
    certified = objective_forms.certify(objective_forms.DEFENSIVE)
    if not certified['certified']:
        out.fail('objectives', 'OBJECTIVE_FORM_UNCERTIFIED',
                 'the current physical forms cannot be certified',
                 missing_evidence=certified['missing_evidence'])
        return out
    out.evidence['objective_forms'] = {
        k: objective_forms.site_requirements(k) for k in objective_forms.DEFENSIVE}
    out.evidence['unverifiable_from_samples'] = dict(structures.UNVERIFIABLE_FROM_SAMPLES)

    # The fitter reports the Fountain's own sample, which is the right anchor:
    # objective depth is measured from the Fountain outward to the midline.
    homelands = {t: h['fountain']['sample'] for t, h in result['homelands'].items()
                 if h.get('fountain')}
    if len(homelands) != 2:
        return out.fail('objectives', 'NO_OBJECTIVE_SITING',
                        'objective siting needs both homelands as sample coordinates')
    try:
        # Several candidates per layer, because the best site for each layer
        # INDEPENDENTLY need not be orderable with the others -- on the screened
        # finalists it systematically was not, with the End Spike landing
        # further toward the midline than the Bastion meant to sit outside it.
        # That is the trap of optimizing each slot alone; the ordinal is a joint
        # constraint, so the choice has to be joint too.
        sited = structures.evaluate(candidate, homelands, per_layer=OBJECTIVE_POOL)
    except Exception as failure:                     # noqa: BLE001 - reported, not swallowed
        return out.fail('objectives', 'OBJECTIVE_SITING_FAILED', str(failure))

    placements = {}
    for team, layers in sited.get('teams', {}).items():
        pools = {}
        for layer_id, entry in layers.items():
            sites = [s for s in (entry.get('candidates') or [])
                     if s.get('advance') is not None]
            if not sites and layer_id in objective_forms.DEFENSIVE:
                out.fail('objectives', 'NO_OBJECTIVE_SOCKET',
                         f'{team} has no site for {layer_id}: '
                         + entry.get('refusal', 'no reason recorded'),
                         team=team, objective=layer_id)
            if layer_id in objective_forms.DEFENSIVE:
                pools[layer_id] = sites
        if any(not v for v in pools.values()):
            continue
        chosen = order_constrained(pools)
        if chosen is None:
            best = {k: v[0]['advance'] for k, v in pools.items()}
            out.fail('objectives', 'NO_ORDERED_OBJECTIVE_LAYOUT',
                     f'{team} has no combination of sited objectives that runs '
                     f'midline -> Outpost -> Bastion -> Spike -> Fountain '
                     f'without their authored footprints overlapping',
                     team=team,
                     pool_sizes={k: len(v) for k, v in pools.items()},
                     best_unconstrained=best,
                     problems=objective_forms.ordinal_ok(best))
            continue
        placements[team] = {k: v['advance'] for k, v in chosen.items()}
        out.evidence.setdefault('objective_quality', {})[team] = {
            k: v.get('quality') for k, v in chosen.items()}
        # World coordinates, so authoring and column verification have somewhere
        # to go. Without these the sited objectives exist only as fractions.
        out.evidence.setdefault('objective_world_xz', {})[team] = {
            k: [v['world_xyz'][0], v['world_xyz'][2]] for k, v in chosen.items()
            if v.get('world_xyz')}
    out.evidence['objective_sites'] = placements
    return out


def _footprint_radius(objective):
    """Half the authored template's span, in blocks."""
    req = objective_forms.site_requirements(objective)
    return (req['span_samples'] / 2.0) if req.get('known') else 0.0


def _overlaps(chain, combo):
    """Do any two of these footprints occupy the same ground?

    Checked at the AUTHORED span, not at the siting search's sample radius. The
    search reasons in eight-block samples and allowed a Bastion and an Outpost
    eleven blocks apart; the built Bastion is thirty-two blocks across and
    simply overwrote the Outpost, which a block-level readback caught and
    nothing upstream did.
    """
    for i, a in enumerate(chain):
        for b in chain[i + 1:]:
            pa, pb = combo[chain.index(a)], combo[chain.index(b)]
            wa, wb = pa.get('world_xyz'), pb.get('world_xyz')
            if not wa or not wb:
                continue
            need = _footprint_radius(a) + _footprint_radius(b)
            if math.dist((wa[0], wa[2]), (wb[0], wb[2])) < need:
                return f'{a} and {b} footprints overlap'
    return None


def order_constrained(pools):
    """Pick one site per defensive objective so the team-axis ordinal holds.

    Exhaustive, because it can afford to be: three objectives out of a dozen
    candidates each is a few thousand combinations. A greedy walk was tried
    first and starved -- taking the best Outpost by quality often left no
    Bastion inside it, which is the joint constraint punishing a per-slot
    choice exactly as expected.

    The Fountain is not among the choices. It is Core geometry fixed by the
    homeland fit, at the team's own origin, so it is the fixed inner anchor
    rather than a fourth free slot.

    Lateral displacement and gap size are never considered: both are explicitly
    free, and only the order is invariant.
    """
    chain = [k for k in objective_forms.DEFENSIVE if k in pools]
    if len(chain) != len(objective_forms.DEFENSIVE):
        return None
    best, best_quality = None, -1.0
    for combo in itertools.product(*(pools[k] for k in chain)):
        advances = [c['advance'] for c in combo]
        # Strictly decreasing from the midline inward, and all outside the
        # Fountain itself.
        if any(a <= 0 for a in advances):
            continue
        if any(x <= y for x, y in zip(advances, advances[1:])):
            continue
        if _overlaps(chain, list(combo)):
            continue
        quality = sum(c.get('quality') or 0.0 for c in combo)
        if quality > best_quality:
            best, best_quality = dict(zip(chain, combo)), quality
    return best


def lair(candidate, result, out: Compilation):
    """Exactly one Lair socket, with its access measured from both teams.

    This is the narrow exception to "Wilderness need not be mirrored". Terrain
    contrast between the ends is permitted; structurally privileged access to
    the one indivisible shared objective is not, so here parity really is
    gated -- on a bound taken from the measured distribution, not from taste.
    """
    out.reached = 'lair'
    fountains = {t: h['fountain']['sample'] for t, h in result['homelands'].items()
                 if h.get('fountain')}
    if len(fountains) != 2:
        return out.fail('lair', 'LAIR_ACCESS_UNMEASURED',
                        'A_L needs both Fountains as reach origins')
    found = lair_socket.search(candidate, fountains)
    out.evidence['lair_search'] = {
        'considered': found['considered'], 'viable': found['viable'],
        'parameters': found['parameters'], 'approximations': found['approximations'],
        'best_access_asymmetry': found['selected']['access_asymmetry'] if found['selected'] else None,
    }
    problems = spatial_contract.lair_problems([found['selected']] if found['selected'] else [])
    if problems:
        return out.fail('lair', 'NO_LAIR_VOLUME',
                        'no cell satisfies the encounter-volume contract: ' + problems[0],
                        considered=found['considered'])
    site = found['selected']
    out.evidence['lair_site'] = site
    # The Worksite portfolio is chosen here because it must avoid the Lair: the
    # two systems exist to pull teams in different directions.
    out.evidence['worksites'] = worksite_portfolio.choose(
        candidate, fountains, lair_xz=site['sample'])
    bound = PROVISIONAL['max_lair_access_asymmetry']
    if site['access_asymmetry'] is not None and site['access_asymmetry'] > bound:
        out.fail('lair', 'LAIR_ACCESS_DISPARITY',
                 f"the best viable Lair socket still gives one team "
                 f"{site['access_asymmetry']:.3f} access disparity",
                 access_asymmetry=site['access_asymmetry'], bound=bound,
                 reach=site['reach'])
    return out


def compile_candidate(candidate, world=None, build_world=None) -> Compilation:
    """Run the stages a candidate can currently reach.

    Stops at the first stage with no way forward, so `deepest_stage_reached`
    means what it says. Later stages (select/author/verify) are only attempted
    when every earlier one is clean.
    """
    out = Compilation(seed=candidate.get('seed'))
    recognize(candidate, out)
    if out.rejections:
        return out
    result = fit(candidate)
    out.evidence['fit_failures'] = result.get('failures', [])
    homebase(result, out)
    if out.rejections:
        return out
    hinterland(result, out)
    if out.rejections:
        return out
    objectives(candidate, result, out)
    if out.rejections:
        return out
    lair(candidate, result, out)
    if out.rejections:
        return out
    author(out, world)
    if out.rejections:
        return out
    verify(out, world)
    if out.rejections or build_world is None:
        return out
    author_into(out, build_world)
    return out


def author(out: Compilation, world=None):
    """Can every sited objective actually be built?

    Fails closed on missing geometry. Nothing historical may stand in for a
    current form -- building an End Tower under the Spike's name is the
    masquerade the physical contract forbids.
    """
    out.reached = 'author'
    missing = [k for k in objective_forms.DEFENSIVE
               if k not in build_structures.TEMPLATES]
    if missing:
        out.fail('author', 'OBJECTIVE_MESH_MISSING',
                 'no authored mesh exists for these current forms, and no '
                 'historical mesh may stand in for one',
                 missing=missing,
                 available=sorted(build_structures.TEMPLATES))
    return out


def author_into(out: Compilation, world):
    """Actually build the sited objectives into a world, then read them back.

    Separated from `author`, which only asks whether a mesh exists. This writes
    blocks, so it is only ever given a disposable copy by the caller.
    """
    from .build_structures import TEMPLATES, build
    sites = out.evidence.get('objective_world_xz') or {}
    reader = column_scan.World(Path(world))
    placements = []
    for team, objs in sites.items():
        for kind, (x, z) in objs.items():
            surface = reader.surface(x, z)
            if surface is None:
                return out.fail('author', 'AUTHORING_SITE_UNGENERATED',
                                f'{team}/{kind} has no terrain under it')
            placements.append({'structure': kind, 'team': team,
                               'world_xyz': [x, surface + 1, z]})
    # The Fountains, which are the same defect as the unmanifested Lair one
    # system over. Every team's respawn and reconstruction binds to a Fountain
    # coordinate, and this stage used to build the six objectives and the Lair
    # and nothing else -- so a map could pass every physical check, certify as
    # READY, bind `fountains=2`, and have open air at both of them. The
    # runtime reported "fountain active" the whole time, because it was
    # reporting a binding rather than a structure.
    for team, xyz in (out.evidence.get('fountains') or {}).items():
        x, z = xyz[0], xyz[2]
        surface = reader.surface(x, z)
        if surface is None:
            return out.fail('author', 'FOUNTAIN_SITE_UNGENERATED',
                            f'{team} Fountain has no terrain under it')
        placements.append({'structure': 'aether_fountain', 'team': team,
                           'world_xyz': [x, surface + 1, z]})
    # The Lair, which is the whole reason a map could reach READY unusable.
    # Authored in the same pass as the objectives so a verified world is a
    # complete one rather than one missing the system nobody checked.
    site = out.evidence.get('lair_site')
    if site:
        from .build_structures import WorldEditor
        editor = WorldEditor(Path(world))
        record = lair_mark.build(editor, reader.surface,
                                 site['world_xz'][0], site['world_xz'][1],
                                 seed=out.seed or 0)
        if record is None:
            return out.fail('author', 'LAIR_SITE_UNGENERATED',
                            'the chosen Lair socket has no terrain under it')
        editor.flush()
        record['count'] = 1
        out.evidence['lair_manifestation'] = record
    built = build(Path(world), placements, Path(world) / 'structures-built.json')
    if built.get('refused'):
        return out.fail('author', 'AUTHORING_SITE_BUILT_ON',
                        'one or more sites stand on structure that already exists; '
                        'authoring refuses rather than demolishing or absorbing it',
                        refused=built['refused'])
    out.evidence['authored'] = {
        'blocks_written': built['blocks_written'],
        'structures': built['structures'],
        'not_reproduced': dict(getattr(__import__(
            'terrain_harvest.build_structures', fromlist=['x']),
            'VANILLA_PLACEMENT_GAPS', {})),
    }
    back = column_scan.readback(Path(world), built['structures'], TEMPLATES)
    out.evidence['readback'] = back
    if not back['verified']:
        return out.fail('author', 'AUTHORED_BLOCKS_MISSING',
                        'blocks that were authored are not in the world',
                        problems=back['problems'])
    verify_lair(out, world)
    if out.rejections:
        return out
    ready(out, world)
    return out


def verify_lair(out: Compilation, world):
    """Confirm the Lair is physically where the manifest will say it is.

    Reads the world back rather than trusting the write, and checks the facts a
    runtime binding depends on: the anchor exists, it is open air rather than
    inside the plinth, and the markers actually stand on the ground.
    """
    record = out.evidence.get('lair_manifestation')
    if not record:
        return out.fail('verify', 'LAIR_NOT_MANIFESTED',
                        'the Lair socket was chosen but nothing was built there')
    reader = column_scan.World(Path(world))
    x, y, z = record['anchor']['xyz']
    at_anchor = reader.block(x, y, z)
    below = reader.block(x, y - 1, z)
    problems = []
    if at_anchor not in column_scan.TRANSPARENT:
        problems.append(f'the spawn anchor at {[x, y, z]} is {at_anchor}, not open air; '
                        f'an occupant would suffocate')
    if below in column_scan.TRANSPARENT or below is None:
        problems.append(f'nothing solid under the spawn anchor at {[x, y, z]}')
    standing = sum(1 for m in record['markers']
                   if reader.block(m['xz'][0], m['base_y'] + 1, m['xz'][1])
                   not in column_scan.TRANSPARENT)
    if standing < len(record['markers']):
        problems.append(f'{len(record["markers"]) - standing} Lair markers are not '
                        f'standing on the ground they were placed on')
    out.evidence['lair_verification'] = {
        'anchor_block': at_anchor, 'anchor_support': below,
        'markers_standing': standing, 'markers': len(record['markers']),
        'problems': problems,
        'proves': 'physical and runtime feasibility only; encounter quality is empirical',
    }
    if problems:
        out.fail('verify', 'LAIR_PHYSICALLY_INVALID',
                 'the Lair manifestation does not survive a readback',
                 problems=problems)
    return out


def bindings(out: Compilation, world) -> dict:
    """Everything the match runtime must resolve, from this realization alone.

    No Alpha coordinates, no config fallback: if a value is not here, the map is
    not claimable, which is what readiness certification then enforces.
    """
    e = out.evidence
    lair = e.get('lair_manifestation') or {}
    site = e.get('lair_site') or {}
    return {
        'world': {'name': Path(world).name, 'map_type': 'default'},
        'homelands': e.get('homeland_world') or {},
        'fountains': e.get('fountains') or {},
        'objectives': e.get('objective_world_xz') or {},
        'lair': {
            'anchor': lair.get('anchor'),
            'centre_xz': lair.get('centre_xz') or site.get('world_xz'),
            'count': lair.get('count', 0),
            'access_asymmetry': site.get('access_asymmetry'),
            'treatment': lair.get('treatment'),
        },
        'worksites': e.get('worksites') or [],
    }


def ready(out: Compilation, world):
    """READY certification: can every match system bind to this realization?

    A separate stage from `verify` on purpose. Verification is about the map;
    this is about whether the runtime can use it, and they are different
    questions that came apart badly enough to produce a claimable map whose
    Lair had never been established.
    """
    out.reached = 'ready'
    resolved = bindings(out, world)
    out.evidence['runtime_bindings'] = resolved
    certified = readiness.certify(resolved)
    out.evidence['readiness'] = certified
    if not certified['certified']:
        out.fail('ready', 'NOT_READY',
                 'the map is verified but a match cannot start on it',
                 problems=certified['problems'])
    return out


def verify(out: Compilation, world=None):
    """Read the actual columns, for what surface samples cannot establish.

    The candidate grid samples one surface height every eight blocks, which
    cannot answer whether a 103-block spike has 103 blocks of sky above it or
    whether a footprint spans a ravine. This reads the world.

    Without a world there is nothing to read, and that is reported as
    unverified rather than passed. An unverifiable claim is not a true one.
    """
    out.reached = 'verify'
    sites = out.evidence.get('objective_world_xz')
    if not sites:
        return out.fail('verify', 'PHYSICAL_VERIFICATION_UNAVAILABLE',
                        'no world coordinates were recorded for the sited objectives')
    if world is None:
        return out.fail('verify', 'PHYSICAL_VERIFICATION_UNAVAILABLE',
                        'no world supplied; the column-level facts stay unestablished')
    # The Fountain is verified with the objectives, not after them.
    #
    # It was authored into the world and never checked, because nothing supplied
    # a requirement to check it against and `verify_placements` skips what it
    # has no contract for -- silently. That is how a map certified READY with a
    # savanna village 68 blocks from one Fountain and none within 728 of the
    # other. It stays out of DEFENSIVE, because it is not a defensive objective
    # and the ordinal chain must not gain a fourth link; it is added here, to
    # the set of things that must survive a column scan.
    sites = {team: dict(objs) for team, objs in sites.items()}
    for team, xyz in (out.evidence.get('fountains') or {}).items():
        if team in sites:
            sites[team]['aether_fountain'] = [xyz[0], xyz[2]]
    requirements = {k: objective_forms.site_requirements(k)
                    for k in (*objective_forms.DEFENSIVE, 'aether_fountain')}
    result = column_scan.verify_placements(
        Path(world), sites, requirements,
        max_moved_per_column=PROVISIONAL['max_levelling_moved_per_column'])
    out.evidence['physical_verification'] = result

    # The Opening ceiling, on the PRISTINE world.
    #
    # It must run here and not after authoring, or it reads our own objectives
    # back as buildings. maps.md has excluded villages from the Opening
    # Hinterland since 22 September and said a serious violation should reject
    # a socket; nothing implemented it, and 99887766 certified READY with a
    # savanna village 68 blocks from one Fountain and none within 728 of the
    # other.
    from . import opening_ceiling as _ceiling
    ceiling = _ceiling.certify(Path(world), out.evidence.get('fountains') or {})
    out.evidence['opening_ceiling'] = ceiling
    if not ceiling['certified']:
        out.fail('verify', 'OPENING_CEILING_VIOLATED',
                 'the Opening Hinterland contains built structure, which doctrine '
                 'excludes because it skips meaningful early progression',
                 problems=ceiling['problems'])

    if not result['verified']:
        out.fail('verify', 'PHYSICAL_VERIFICATION_FAILED',
                 'sited objectives do not survive a column scan',
                 problems=result['problems'])
    return out


def main(argv=None):
    import argparse
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('candidate', type=Path, nargs='+')
    p.add_argument('--out', type=Path)
    p.add_argument('--world', type=Path,
                   help='world directory for column-level physical verification')
    p.add_argument('--build-world', type=Path,
                   help='DISPOSABLE world copy to author into and read back')
    a = p.parse_args(argv)
    runs = []
    for path in a.candidate:
        c = json.loads(path.read_text())
        runs.append(compile_candidate(c, a.world, a.build_world).as_dict())
        r = runs[-1]
        print(f"seed={r['seed']} reached={r['deepest_stage_reached']} "
              f"playable={r['playable']} "
              f"codes={[x['code'] for x in r['rejections']]}")
    if a.out:
        a.out.parent.mkdir(parents=True, exist_ok=True)
        a.out.write_text(json.dumps({'schema': 'map_compilation_run/1',
                                     'stages': list(STAGES),
                                     'provisional_constants': PROVISIONAL,
                                     'runs': runs}, indent=1))
    return runs


if __name__ == '__main__':
    main()
