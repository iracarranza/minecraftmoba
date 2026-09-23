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
from . import build_structures, lair_socket, objective_forms, spatial_contract

# Stage names, in order. A candidate reaches at most one past where it fails.
STAGES = ('recognize', 'homebase', 'hinterland', 'objectives', 'lair',
          'select', 'author', 'verify')

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
OBJECTIVE_POOL = 12

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
    # constraint, so it is taken from the measured distribution rather than
    # chosen. Best achievable A_L across the eight screened finalists, each over
    # 11.8k-12.3k considered cells: 0.001, 0.050, 0.073, 0.226, 0.294, 0.415,
    # 0.678, 0.787. The widest gap in that distribution by far is 0.073 -> 0.226,
    # so 0.15 sits in the middle of it and any value in [0.08, 0.22] selects
    # exactly the same three seeds -- the threshold is insensitive across a band
    # three times its own width. Tightening below 0.05 would keep one seed in
    # eight; loosening past 0.30 stops discriminating.
    'max_lair_access_asymmetry': 0.15,
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
    def playable(self) -> bool:
        return self.reached == 'verify' and not self.rejections

    def fail(self, stage, code, detail, **data):
        self.rejections.append(Rejection(stage, code, detail, data))
        return self

    def as_dict(self):
        return {
            'schema': 'map_compilation/1',
            'seed': self.seed,
            'deepest_stage_reached': self.reached,
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
                     f'midline -> Outpost -> Bastion -> Spike -> Fountain',
                     team=team,
                     pool_sizes={k: len(v) for k, v in pools.items()},
                     best_unconstrained=best,
                     problems=objective_forms.ordinal_ok(best))
            continue
        placements[team] = {k: v['advance'] for k, v in chosen.items()}
        out.evidence.setdefault('objective_quality', {})[team] = {
            k: v.get('quality') for k, v in chosen.items()}
    out.evidence['objective_sites'] = placements
    return out


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
    bound = PROVISIONAL['max_lair_access_asymmetry']
    if site['access_asymmetry'] is not None and site['access_asymmetry'] > bound:
        out.fail('lair', 'LAIR_ACCESS_DISPARITY',
                 f"the best viable Lair socket still gives one team "
                 f"{site['access_asymmetry']:.3f} access disparity",
                 access_asymmetry=site['access_asymmetry'], bound=bound,
                 reach=site['reach'])
    return out


def compile_candidate(candidate) -> Compilation:
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
    author(out)
    return out


def author(out: Compilation):
    """Can every sited objective actually be built?

    Fails closed on purpose. Siting now uses the measured `end_spike` contract,
    and no End Spike mesh exists -- the repository only has the historical End
    Tower. Building that under the Spike's name would be exactly the
    masquerade the physical contract forbids, so the compiler stops here
    instead.
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


def main(argv=None):
    import argparse
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('candidate', type=Path, nargs='+')
    p.add_argument('--out', type=Path)
    a = p.parse_args(argv)
    runs = []
    for path in a.candidate:
        c = json.loads(path.read_text())
        runs.append(compile_candidate(c).as_dict())
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
