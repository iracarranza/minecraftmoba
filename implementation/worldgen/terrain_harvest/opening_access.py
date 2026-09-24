"""Seam 3: how many distinct ways out of each team's opening, and are they equivalent?

maps.md's opening access model is Core -> authored base exits -> compact
Hinterland -> Wilderness -> player Exploration Routes, and it asks for
**equivalent exit capacity, not identical geometry**. Nothing measured that.

WHY THIS IS NOT A NEW MEASUREMENT SYSTEM. It runs on the same terrain graph
that sited the structures, authored the Routes and produced the travel matrix
-- `Terrain(candidate)` and `shortest`, exactly as `cell_grid` uses them. A
team's exit capacity and a Route's chosen crossing therefore agree by
construction rather than by coincidence.

WHAT `routes.author` COULD NOT ANSWER. It already walks the graph from each
homeland, carves a corridor and records "no path from homeland in the terrain
graph" -- a binary reachability verdict, which is the right mechanism and the
wrong question. It takes `route_targets` from a configuration: STRATEGIC
DESTINATIONS, which is the extension into Wilderness a generated map does not
owe. This asks a different question of the same graph -- how many separate
ways lead out of the opening at all -- and needs no targets.

EXITS ARE COUNTED AS COMPONENTS, NOT AS NODES. The frontier at a given
traversal cost is usually a ring of hundreds of nodes; what matters is how
many DISCONNECTED arcs that ring breaks into, because two arcs separated by a
cliff are two ways out and four hundred adjacent nodes on one arc are one.
Counting nodes would report a team walled in on three sides as having enormous
exit capacity.

THE COUNT IS COARSE AND THE WIDTH CARRIES THE SIGNAL. Measured on four
compiled finalists, exit count is 1 for almost every team at every cost, and
reaches 2 or 3 only where terrain actually severs the frontier ring -- an
unobstructed opening has one connected frontier by topology, not by being
generous. The widths are where the disparity shows: on seed 930006815 at cost
240 the north frontier is 78 nodes against the south's 206, a 0.45 gap that
nothing previously measured. Both are reported; reading the count alone would
call that map symmetric.

NO BOUND IS SET. maps.md asks for equivalence and does not say how much
disparity is acceptable, and this module does not invent one. It reports both
teams' capacity and the gap between them. The same discipline as
`lair_access_asymmetry`, which computes a number and asserts no threshold.
"""
from __future__ import annotations

from vanilla_search.task_a import Terrain, shortest


def _nearest_node(terrain: Terrain, x: int, z: int) -> int:
    return min(range(terrain.n),
               key=lambda i: (terrain.coords[i][0] - x) ** 2
                             + (terrain.coords[i][1] - z) ** 2)


def _neighbours(adj, k):
    """`Terrain.adj` is a LIST indexed by node, holding (node, cost) pairs.

    Not a dict. Writing `adj.get(k, ())` looks defensive and raises
    AttributeError on the first real candidate, which is how this was caught.
    """
    if k < 0 or k >= len(adj):
        return ()
    return [e[0] if isinstance(e, (tuple, list)) else e for e in adj[k]]


def _components(nodes, adj):
    """Connected groups within `nodes`, using only edges internal to the set."""
    members = set(nodes)
    seen, groups = set(), []
    for start in nodes:
        if start in seen:
            continue
        stack, group = [start], []
        seen.add(start)
        while stack:
            k = stack.pop()
            group.append(k)
            for nb in _neighbours(adj, k):
                if nb in members and nb not in seen:
                    seen.add(nb)
                    stack.append(nb)
        groups.append(group)
    return groups


def capacity(candidate: dict, fountains: dict, *, opening_cost: float = 120.0) -> dict:
    """Exit capacity per team at the edge of the opening.

    `opening_cost` is the traversal cost that stands for the Hinterland edge.
    NON-CANON FIXTURE: maps.md calls the Hinterland "compact" and gives no
    figure, so this is a parameter to sweep, not a measured boundary. The
    capacity COUNT is meaningful at any value; comparing two runs at different
    values is not.
    """
    if not fountains:
        return {'measured': False, 'why': 'no fountains to measure from'}
    terrain = Terrain(candidate)
    adj = terrain.adj

    per_team = {}
    for team, xyz in fountains.items():
        home = _nearest_node(terrain, xyz[0], xyz[2])
        table, _ = shortest(adj, {home: 0.0})
        inside = {k for k, v in table.items()
                  if v is not None and v != float('inf') and v <= opening_cost}
        if not inside:
            per_team[team] = {'exits': None,
                              'why': 'the Fountain reaches nothing within the '
                                     'opening cost; this is a compiler fault, '
                                     'not a narrow opening'}
            continue
        # The frontier: inside nodes touching something outside.
        frontier = []
        for k in inside:
            for nb in _neighbours(adj, k):
                if nb not in inside:
                    frontier.append(k)
                    break
        groups = _components(frontier, adj)
        widths = sorted((len(g) for g in groups), reverse=True)
        per_team[team] = {
            'exits': len(groups),
            'exit_widths_nodes': widths[:8],
            'widest_exit_nodes': widths[0] if widths else 0,
            'frontier_nodes': len(frontier),
            'opening_nodes': len(inside),
        }

    teams = sorted(per_team)
    measured = [t for t in teams if per_team[t].get('exits') is not None]
    out = {'measured': True, 'opening_cost': opening_cost, 'per_team': per_team}
    if len(measured) == 2:
        a, b = (per_team[t] for t in measured)
        def gap(x, y):
            total = x + y
            return 0.0 if total == 0 else round(abs(x - y) / total, 4)
        out['comparison'] = {
            'teams': measured,
            'exit_count_gap': gap(a['exits'], b['exits']),
            'widest_exit_gap': gap(a['widest_exit_nodes'], b['widest_exit_nodes']),
            'opening_size_gap': gap(a['opening_nodes'], b['opening_nodes']),
        }
    out['bound'] = ('none. maps.md asks for equivalent exit capacity and does '
                    'not say how much disparity is acceptable; no threshold is '
                    'invented here.')
    out['not_covered'] = [
        'whether an exit is USEFUL -- it proves a way out, not somewhere worth going',
        'Routes and infrastructure, which change opening access during play',
        'exits created by boat, water or vertical movement the surface graph misses',
    ]
    return out
