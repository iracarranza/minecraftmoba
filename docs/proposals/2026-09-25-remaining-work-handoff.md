# Remaining pre-match and compiler work — handoff spec

**For:** Codex, working in `/private/tmp/minecraftmoba-spatial-cadence`, branch
`codex/spatial-cadence-migration`.
**State at handoff:** `13231e2`. Worldgen 589 tests green, plugin 293 green,
tree clean, everything pushed.

---

## Context

The compiler produces READY maps from seeds unattended: a pool of 28 entries
exists at `/private/tmp/mobapool`, each carrying a regenerative portfolio
derived from its own geography, and a match has been run on one with animals
manifesting at derived coordinates. Throughput is ~16 READY maps/hour.

What remains splits in two. The **pre-match flow** (class draft, map draft,
the hall) has been designed in `docs/design/PRE_MATCH_SELECTION_FLOW.md` well
ahead of the code — five amendments were written in one session and only the
class draft state machine exists. The **compiler backlog** is one large yield
problem plus several measurement gaps.

Items 1 and 2 of the original list are done (`1db71f4`, `13231e2`). This spec
covers 3–9 plus building the draft hall in a world.

---

## Four traps that have already cost time

Read these before starting. Each was hit at least once, and two were hit
three times.

**1. The screening tier is where budget is allocated.** `scoop.search` has a
cheap screen over ~16,580 windows and an exact tier on the survivors.
`map_types.predicates()` runs at *both*. A feature computed only on exact
scoops is invisible to tutoring, so the Type needing it gets no budget and
matches nothing — which looks identical to a Type that is rare. This happened
to `land_bodies`, `separation_sign` and structure counts in turn.

**2. The sea-level clamp is right for symmetry and wrong for everything
else.** Callers clamp height to sea level so symmetry reads the playable
surface. That makes `h >= sea_level` true everywhere and every ocean cell
perfectly flat. It broke `water_structure` (one land body, zero coastline, at
every quantile), `water_fraction` (0.00 across eight seeds) and
`homebase_pair` (Homebases sited on open water — the cause of
`SOCKET_NOT_INDEPENDENTLY_ACCEPTABLE` being 26 of 32 rejections at the time).
Water comes from **biome**, never from a height test.

**3. A threshold above what is reachable is impossible, not strict.** All six
biome-family cuts were first set at a third of a window. Measured ceilings:
`frozen 1.000, arid 0.768, jungle 0.455, swamp 0.343, pale 0.020`. The pale
cut asked for sixteen times the largest pale garden that exists.
`map_types.FAMILY_CEILINGS` records these. **Measure reachability before
setting a cut.**

**4. Doctrine and code drift apart silently.** The yield-weighted allocator
contradicted `maps.md`'s "search for outliers rather than rejecting them", and
plain alternation contradicted the decided `2-3-2-3-3-1`. Both were written
down first and contradicted later. When doctrine says something, grep for the
code that implements it.

---

## Standing constraints

- `maps.md`, `objectives.md`, `classes.md`, `infrastructure.md` are canonical.
  Preserve design-state labels (**Established / Working / Prototype / Open /
  Historical**).
- Every quantity is a **declared fixture** unless measured. Say which.
- **Record negative results.** A measurement that kills an idea is worth
  committing — it stops the idea being re-proposed.
- Do not touch Codex's Mole tunnelling work (`TunnelingAbility.java`) —
  unrelated, rides on this branch, has no tests.
- Run both suites before each commit:
  `cd implementation/worldgen && python3 -m unittest discover -s tests -q`
  `cd implementation/plugin && ./gradlew test -q` (needs `JAVA_HOME` set to the
  Minecraft runtime — see `implementation/plugin/deploy.sh` for the path).
- Binaries: `MOBA_CUBIOMES_SCAN` and `MOBA_CUBIOMES_STRUCT` point at
  `/private/tmp/cubiomes-build/{mobascan,mobastruct}`.

---

## Item 3 — Socket siting *(highest value; do first)*

`SOCKET_NOT_INDEPENDENTLY_ACCEPTABLE` is ~44 of 73 rejections. At 35% yield
each recovered rejection is a whole map, so this is worth more than the rest
of the list combined.

### What it actually means

`map_compiler.homebase()` rejects when a socket's `quality` is below
`PROVISIONAL['min_homeland_developable_fraction']` (0.52). That quality comes
from `vanilla_search/task_a.py:fit_homeland`:

```
quality = sum(buildable[i] for i in cells) / len(cells)
```

— the fraction of a nine-sample (72-block) footprint that is **buildable** in
the generated world.

### The likely cause, to confirm first

`fit_homeland` calls `_choose_home(...)` and searches **independently per
team**, knowing nothing about the Homebase pair `prospect` already chose.
Prospect selects a mirrored pair on land by *flatness grade* over approximate
cubiomes height; the compiler then discards that and re-searches by
*buildable fraction* over real blocks. Two different criteria over two
different data sources.

Confirm before fixing:

```
for each pool entry in /private/tmp/mobapool:
    recompute prospect's homebase_pair for its window
    compare to the compiler's chosen homeland positions
    compute the compiler's `quality` at BOTH
report: does prospect's pick score better, worse, or the same?
```

If prospect's pick scores better, the fix is (a). If it scores the same or
worse, the fix is (b).

### (a) Carry the pair forward

`prospect` already computes `homebase_pair` with `a_sample`/`b_sample`.
`foundry_run._generate_one` writes `candidate['prospect_scoop']` but **drops
the positions** — add them. Then `fit_homeland` takes them as a seed/hint and
falls back to `_choose_home` only if the hint fails its own test.

Keep the doctrine: sockets are judged **independently, never against each
other** (`homebase()` says so explicitly). A hint may not become a comparison.

### (b) Align the criteria

If prospect's pick is no better, its criterion is wrong rather than its
result being discarded. `_grade` measures deviation from a local median on
approximate height. `buildable` excludes water, canopy and non-buildable
blocks. The gap is probably **canopy** — a forest floor can be flat and
unbuildable.

Prospect can approximate canopy from biome (forest families) at the screening
tier, the same way biome families were added in item 2.

### Verification

Run a 40-seed batch through `foundry_run.run` and compare
`SOCKET_NOT_INDEPENDENTLY_ACCEPTABLE` as a share of rejections against the
baseline **44/73**. Record the before/after in `docs/audit/`.

---

## Build the draft hall in a world

`DraftHall` (`implementation/plugin/.../DraftHall.java`) computes 28 stand
positions and a pit as pure arithmetic, with 9 tests. **Nothing places
blocks.** `DraftHallView` applies the ghost state but its `stands` map is
never populated — `bindStand` has no caller.

### What to build

Follow `LobbyHall`'s split exactly: arithmetic stays testable without a
server, the world-side builder is separate and untested.

1. A builder that takes `DraftHall.stands(floorY, rankOffset, pitDepth,
   spacing)` and `DraftHall.pit(...)` and places blocks + armour stands.
   Materials and both team colours from **config**, not constants —
   `LobbyHall`'s docstring is explicit that it "decides nothing about what the
   room looks like".
2. Spawn one `ArmorStand` per position; call `DraftHallView.bindStand`.
3. Call it when `Match` enters `CLASS_SELECT`; tear down on
   `classSelectionComplete()`.
4. Hall dimensions: `DraftHall.requiredRadius(rankOffset, spacing)` gives the
   interior half-span (10 at the defaults). Reuse `LobbyHall.hall(...)`.

Defaults measured but **never stood in**: `rankOffset 6` → a 12-block gap
between ranks, `pitDepth 2`, `spacing 2`. The 12 is chosen from armour-stand
render distance (gear legible ~16–24 blocks, name tags much further) so a
player reads *teammates' kit* and *enemies' names*. Expect to tune it.

No thorough examination needed afterwards — the user wants it standing.

---

## Item 4 — `MapDraft`

Nothing exists. Specified in `PRE_MATCH_SELECTION_FLOW.md` under "the map
board".

```
board        = 6 maps, drawn fresh per match, all distinct profiles
strike order = A1 · B1 · A1, then B picks 1 of the 3 remaining
A            = the team that won the class coinflip (ClassDraft.firstPick())
B            = the other team; the final map choice is its compensation
```

**The generating rule, which must survive any change to board size:** a
strike only matters if someone else acts after it, so **the last strike must
belong to A**. An order ending `B strikes, B picks` is a no-op — B removes an
option from its own choice set and then chooses from the remainder.

Board size is a dial at `2n+2`: A strikes n, B strikes n−1, B picks 1 of 3.

Model on `ClassDraft`: a `Rules` record holding the open values, refusals
returned as strings rather than thrown, turn state as `onTurn()` with
`isGhost()` its complement. Only the **played** map is consumed; struck maps
return to the pool, since options carry no identity beyond profile and
thumbnail.

---

## Item 5 — Type-set reveal

Before class selection opens, show both teams **which Map Types are on the
board** — types alone. No thumbnails, no Scale, Density or Symmetry, no
indication which will survive.

Before, not during: information arriving mid-draft advantages whoever's window
it lands in, and revealing first lets class *bans* be informed by it.

Small once item 4 exists — the board knows its types.

---

## Item 6 — Terrain Symmetry from the generated world

Currently `deviation_over_relief` comes from the prospect scan: approximate
cubiomes height at 32-block sampling, over contested ground, with a
`NON-CANON` opening-cost fixture in the denominator. It is about to become
**player-facing** (item 7), so re-derive it from the generated world first.

The 28 pool entries still have their worlds, so this is cheap. Use
`scoop.symmetry(..., contested=land_mask)` against the candidate's
`feature_grid` rather than a fresh scan.

Show as a **band, never a decimal** — Resource Density sets the precedent by
being "light/rich". Bands come from the measured reference distribution
(`terrain_harvest/scoop_reference.json`: dev_rel p25 0.237, p50 0.287,
p75 0.356), not from invented cut points. Band names and count are **Open**.

---

## Item 7 — Thumbnails and Text Displays

**Thumbnail:** whole scoop on one **level-4 map item, 16 blocks per pixel**
(864×1056 renders as 54×66 of a 128×128 canvas). At that scale one pixel
swallows a village, a cave mouth, an ore vein and the Lair site. Shape
survives; contents do not.

> Shape is advertised. Contents are discovered. The render scale is the line
> between them.

Record the scale as a **`[BALANCE FIXTURE — expect to tune]`**. The same
mechanism at level 2 or 3 discloses four times the linear detail and nothing
in-game would flag the difference, so the scale is a control.

**Properties:** four values (Map Type, Scale, Resource Density, Terrain
Symmetry band) as always-visible **Text Display** entities above each option —
not hover-to-reveal. The values are short and the hall's premise is that
contention is visible; making fourteen people each query what could simply be
readable inverts that.

**No seed, no coordinates, no opaque id** during the draft. A seed is fully
reversible. Seed and centre coordinate are revealed **after** the match.

---

## Item 8 — Regenerate the pool

After item 3, so it inherits the improved yield. Use
`foundry_run.run(..., pool=Path('/private/tmp/mobapool'))` — it publishes
playable maps **before** deleting worlds and deletes only what wasn't
published.

The target is **diversity, not volume**: six distinct profiles continuously,
one realization burned per match. A pool of fifty `landmass` maps satisfies
"≥20 maps" and cannot fill a board.

Goal, now canon: **usable rare Map Types are gathered in equal quantities to
usable common Types.** `map_types.allocate` produces that with equal `wanted`
— budget is `wanted / rate`, so expected output is `budget × rate = wanted`.
Check it by counting the pool by Type; they should match. The price is real
and worth stating: equal quantities of a 3% Type and a 44% Type sends ~93% of
generation to the rare one.

---

## Item 9 — Consumption model for `resource_validity`

Open research, no deadline, and it may end in "the rule changes again" —
which is an acceptable outcome, not a failure.

`resource_validity` cannot reject on ore. Raw ore is 269,318 per window
against a recovered budget of ~130–140 per team per match. Accessibility
filtering via `caves.scan_cell` closes that from ~1000× to ~49× (iron 5.44%
exposed, diamond 1.36%, copper 4.25% on seed 2718281) — a 20× improvement and
still not comparable.

So the budget is a **consumption figure**, not a stock: what one team mines
and uses in a match, bounded by time, tools, travel, inventory and hunger.
`REJECTS_ON_ORE = False` and `NEEDS` record this.

A consumption model needs an extraction-rate estimate that does not exist.
Do not pick a threshold to make the comparison pass — that would be inventing
a number to reconcile two unlike quantities.

---

## Verification

**Per item:** both suites green, committed separately, message stating what
was measured and what was assumed.

**End to end**, after items 3 and 8:

1. `foundry_run.run` over ~40 seeds with `pool=`, 3 generate workers, 6
   compile workers.
2. Confirm every published entry has `runtime_bindings.renewables.certified`
   true and a non-empty `sources` list.
3. Count the pool by Map Type — rare and common should be near-equal.
4. Start a server with `pool.enabled: true` and `directory` pointing at the
   pool; run `moba match open` → `select` → `start-test`; confirm the log
   shows `[renewables] bound N derived source(s) from this map's manifest`.
5. Force-load a derived source coordinate and confirm animals or crops
   manifest.

Steps 4–5 have been done once (seed 3141592: 4 chickens, 2 cows, 4 pigs).
Three environment traps: the server needs `maps/*.json.gz` copied from
`/private/tmp/alpha-server/maps/`, `templatePath` in config must be absolute
(`deploy.sh` rewrites it for real servers), and the server must be started
with a writable stdin (a FIFO) or no console command can reach it.

**Do not kill** the Paper process whose cwd is `/private/tmp/alpha-server` —
it is deliberately preserved.
