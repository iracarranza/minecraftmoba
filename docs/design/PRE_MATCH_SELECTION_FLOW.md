
---

# Amendment — the stand belongs to the player, not the class

**Status:** Working, 25 September 2026. Amends §2, §3 and §4 above.

## Why the original model does not survive the target roster

§2 gives four reasons for a physical hall. One of them —

> **The roster teaches itself.** Players walk past nine classes to reach the
> one they want.

— is true at nine and **false at fifty**, which is the roster size the
long-term catalogue points at. Walking past fifty stands teaches nobody
anything, and the aesthetic cost of a fifty-station rack is the visible
symptom of an argument that has stopped holding.

That reason is therefore **retired rather than traded away**. It expired with
roster size. The other three survive the change below.

## The inversion

A stand represents a **player**, not a class. The hall holds a fixed **28
stands** in three sections:

| section | stands |
|---|---|
| your team | 7 |
| enemy team | 7 |
| banned | 14 |

Fourteen banned stands is one per player, matching "every player may ban".
The count is **fixed regardless of roster size** — nine classes or five
hundred — which is the property the class-per-station model could not have.

Banned classes keep a physical presence, so §3's global ban survives intact: a
banned class is not merely refused, it is *standing in the banned section
wearing its own gear*, visible to both teams at once. Contention stays visible.

## The verbs

| command | when | effect |
|---|---|---|
| `ban <class>` | your ban turn | bans globally; the class appears in the banned section |
| `hover <class>` | **any time, including off-turn** | your stand wears that class; nothing is committed |
| `pick <class>` | your pick turn | locks in immediately |

`pick` is binding on its own. The right-click-to-lock verb proposed during
design is dropped: a second confirming action adds a failure mode (a player
who typed and did not click) without adding information anyone needed.

## What this changes about the turn — taken deliberately

§4 decides that **the turn is the ability to move**. With a stand of your own
and a chat verb, movement is no longer the selection act, so that sentence no
longer describes enforcement. It is amended to:

> **The turn is the ability to act.** Enforcement is on the verb: `ban` and
> `pick` are refused off-turn. The ghost state remains the *legibility*
> mechanism, which §4 already identified as its strongest property — "turn
> state stops being an invisible permission and becomes the most visible
> property in the room."

Everything §4 asked the ghost state to do still holds. Acting players are
solid and may walk; movement becomes idle and warmup behaviour rather than
selection. Ghosts still cannot obstruct, still broadcast intent — `hover`
works off-turn precisely so they can — and solidifying still marks the turn
boundary without a timer announcing it.

## What `hover` improves

An off-turn player who hovers has **declared a preference in public**. That
makes a better timeout rule than the one first implemented, which assigned the
first available class:

> On timeout, a player receives **the class they are hovering**, if it is
> still available; otherwise the first available. An assignment then follows
> the player's own stated intent wherever one exists, and the spec's
> requirement that a timeout be "a default assignment rather than a stall,
> visible in the hall" is satisfied by the stand already wearing it.

## Bounding and obstruction, resolved by reading rather than deciding

§4 lists ghost bounding as open — physical, soft push-back, or return
teleport. **No mechanism is needed.** Ghosts pass through *"everyone else"* —
players, not blocks — and `LobbyHall` already builds an enclosed room whose
tests assert enclosure, because "a gap you can walk out of" is one of the
three ways a lobby fails. Terrain collision plus the existing walls bound
flight for free.

Obstruction is likewise already answered: ghosts cannot obstruct by
construction, and with a small pick window only one or two players are solid
at once. That removes one of the two reasons §2 offers for separating the
teams, leaving readability — which the facing-gallery layout above serves.

## Still open

Hall layout beyond the three sections; whether `hover` is refused for a banned
or taken class or merely shows it as unavailable; clickable-chat presentation
over the typed minimum; ghost visual treatment; and everything §5 and §6 leave
open.

---

# Amendment — draft order, one hall, and what a map option shows

**Status:** Working, 25 September 2026. Amends §4 and §6.

## Pick order: 2-3-2-3-3-1, after a coinflip

Both phases are ban-then-pick. Every player bans, on a shared clock. A
coinflip then decides pick order, and the windows alternate:

```
A 2   B 3   A 2   B 3   A 3   B 1
```

Seven each. The first proposal was **2-3-3-3-2-1**, and it is not balanced.
Counting how many enemy picks are visible at the moment of each choice:

| order | windows | A sees | B sees | gap | A blind picks |
|---|---|---|---|---|---|
| 2-3-3-3-2-1 | 6 | 21 | 28 | **7** | 2 |
| classic 1-2-2-2-2-2-2-1 | 8 | 24 | 25 | 1 | 1 |
| **2-3-2-3-3-1** | 6 | **24** | **25** | **1** | 2 |

The first gives B a real information edge: B picks all seven with knowledge
while A picks two blind and sees seven fewer enemy picks overall. First-pick
access is the intended compensation, and the classic snake — calibrated by a
decade of MOBA practice — is fair at a gap of about one, which suggests first
pick is worth roughly one point of information rather than seven.

Moving a single pick from A's second window to its third closes the gap to 1
**without adding a window**, so the six-window pacing that suits a physical
hall is kept. The coinflip would have made 2-3-3-3-2-1 fair in expectation
across matches; this is fair within one.

[OPEN] Ban counts and ban ordering for both phases.

## Both phases happen in one hall

§6 asked that, if the map phase landed on alternating ban/counterpick, the two
phases "be made to resemble each other, so the two halves of the pre-match
read as one process rather than two unrelated games". It has. The strongest
way to make two phases read as one process is for them to happen in one room.

**The class ranks stay lit, wearing their picks.** That is not decoration and
it is not optional: choosing a map *in response to* compositions is the entire
reason class-first was chosen, so both compositions have to be standing there
while it happens. Clearing the hall between phases would discard the
information the ordering exists to create.

So the room gains a third element rather than resetting:

| element | where |
|---|---|
| team ranks | unchanged, still wearing the drafted classes |
| map options | a rank on an end wall, perpendicular to the teams |
| banned maps | **the same pit**, alongside banned classes, attributed by side |

One pit means one place that says *removed from play*, for both kinds of
thing. The perpendicular axis distinguishes a new kind of object without
needing a second building.

## What a map option shows

**Decided:** Map Type, Scale, Resource Density — unchanged from
`MATCH_LIFECYCLE_OBJECTIVES_AND_OPENING_DECISIONS.md` — plus a fourth:

### Terrain Symmetry, as a band

Derived from `deviation_over_relief`: how much the two team ends differ as
terrain, measured over contested ground.

**It is character, not a grade, and the distinction is load-bearing.** The
fairness checks are separate and binary — Lair access parity, independently
acceptable Homebase sockets, the opening floor, exit capacity. A map either
passed them and is in the pool or it is not, and nothing in the pool is less
fair than anything else. What remains is the thing doctrine explicitly says is
*not* a deficit: "a difference in canopy, elevation, flat land, biome,
coastline or local resources between the two team ends is NOT by itself a
competitive deficit."

That makes it a draft decision rather than a quality score, and it is most
useful exactly where the map phase now sits — after compositions are known. A
composition that converts a lead wants ground that offers one; a composition
that needs parity to come online wants the opposite.

**Shown as a BAND, never a decimal.** Two reasons. It is measured on the
prospect scan — approximate cubiomes height at 32-block sampling, over
contested ground, with a NON-CANON opening-cost fixture in the denominator —
so a decimal would promote a provisional measurement to a player-facing stat.
And Resource Density already sets the precedent by being "light/rich" rather
than a number, so the advertised properties read as one vocabulary.

Bands come from the measured reference distribution (p25 0.237, p50 0.287,
p75 0.356), not from invented cut points. [OPEN] Band names, and band count.

[OPEN] It should be **re-derived from the generated world** before going
player-facing. The scan is the right instrument for choosing where to
generate, and a proxy once the world exists.

### No seed, no coordinates, and no opaque id

The seed stays forbidden, and the reason is stronger than a preference: **a
seed is fully reversible.** Anyone holding it can load it in singleplayer or
paste it into a seed-mapping tool and see the whole realization in seconds.
The centre coordinate compounds it into "here it is". Rendering the map to an
in-game map item would be the same disclosure by a slower route — and it is
technically possible, since a level-3 map covers 1024x1024 blocks at 8 blocks
per pixel and a scoop is 864x1056, clipping about 3%. Possible, and refused.

The foundry's opaque map id is **also dropped**, which was a proposal and is
now decided against. Without it, two options sharing Type, Scale, Density and
symmetry band are indistinguishable to the drafters — you pick a *profile*,
not a map. That is closer to "players know the broad classification; they
discover the realization" than an id would have been, and it prevents a meta
forming around individual realizations.

**Seed and centre coordinate are revealed after the match**, when the
realization is spent and disclosure costs nothing.

### Nothing else

`MATCH_LIFECYCLE_OBJECTIVES_AND_OPENING_DECISIONS.md` already considered and
declined traversal, verticality, water prevalence, openness and ruggedness as
"substantially consequences of Map Type and/or Scale". Measurement agrees:
water prevalence is how `archipelago` and `landmass` are separated in the
first place. Terrain Symmetry is added because it is the one measured axis
that is **not** a consequence of Type or Scale.

---

# Amendment — shape is advertised, contents are discovered

**Status:** Working, 25 September 2026. Supersedes the "no map render" position
recorded in the previous amendment, and amends the information boundary in
`MATCH_LIFECYCLE_OBJECTIVES_AND_OPENING_DECISIONS.md`.

## The previous position was too coarse

The last amendment recorded map rendering as "possible and refused", treating
it as binary: show the world or show labels. It is not binary. Minecraft map
items have five scales, and the fidelity gap between them is enormous:

| map level | blocks/pixel | an 864x1056 scoop renders as |
|---|---|---|
| 0 | 1 | 864 x 1056 px |
| 2 | 4 | 216 x 264 px |
| 3 | 8 | 108 x 132 px |
| **4** | **16** | **54 x 66 px** |

At 16 blocks per pixel **one pixel swallows a village, a cave mouth, an ore
vein and the Lair site.** What survives is coastline shape, major relief, and
forest-versus-plain massing. What does not survive is contents.

## The rule

> **Shape is advertised. Contents are discovered. The render scale is the line
> between them.**

A map option shows its whole scoop on a single **level-4 map item, 16 blocks
per pixel**. The forbidden list in the lifecycle document stands otherwise:
no seed, no POIs, no Lair location, no objective surroundings, no
renewable-source locations. Those survive a 54x66 thumbnail untouched.

This is consistent with the Established principle it sits under — *discovery
uncertainty is good, existence uncertainty is often bad*. Knowing the shape of
a coastline is not knowing where the iron is.

**[BALANCE FIXTURE — expect to tune.]** 16 blocks per pixel is a stated rule
and a tunable number. The same mechanism at level 2 or 3 discloses far more
and nothing in the game would flag the difference, so the scale is the control
and must be treated as one: a balance lever, revisited with play, not a
constant that happens to be in the code.

## What it costs, stated rather than glossed

**First contact.** Arriving somewhere unseen is part of what makes a new map
land; after a draft spent looking at it, arrival is confirmation rather than
discovery. At 54x66 pixels the loss is small — a thumbnail, not a world — but
it is real and does not come back. Weighed against: the exploration loop's
value was never surprise at the shape, it is finding what is in it, which the
thumbnail does not touch.

## What it buys

**Identical labels become strategically distinct.** Two maps reading
`landmass / normal / light / even` are visibly different terrain, so striking
one is a real decision. The duplicate-profile problem that a striking board
otherwise creates simply dissolves.

**A rotation of three Types can carry strategic variation**, because
differentiation comes from visible terrain rather than from multiplying label
combinations. That is a far cheaper thing for the pipeline to supply.

**Striking works the way striking works everywhere else.** Smash stage
striking, CS vetoes and Dota bans all operate on *known* maps; counterpicking
means something because you can see what you are counterpicking. A blind map
veto was the unusual design.

---

# Amendment — the map board

**Status:** Working, 25 September 2026.

## Board size and strike order

**Six maps, drawn per match, struck `A1 · B1 · A1`, then B picks one of the
three that remain.** A is the team that picked classes first; the final map
choice going to B is the compensation.

### The rule that generates it

> **A strike only matters if someone else acts after it.**

So the last strike must belong to A. An order ending `B strikes, B picks` is a
no-op: B removes an option from its own choice set and then chooses from the
remainder, which is identical to choosing from the larger set. A proposed
`A2 · B1 · pick` had exactly this defect.

Board size is then a dial with no rebalancing at any setting — A strikes n, B
strikes n-1, B picks 1 of 3, board = 2n+2:

| board | order | actions each |
|---|---|---|
| 4 | A1 → pick | 1 |
| **6** | **A1 · B1 · A1 → pick** | **2** |
| 8 | A1 · B1 · A1 · B1 · A1 → pick | 3 |
| 10 | A2 · B3 · A2 → pick | 4 |

Six is chosen on proportionality. The class draft already spends fourteen
actions, and participation should scale with what is *personal*: your class is
yours, so you act; the map is one shared object the team plays on, so the team
acts once. Fourteen people each striking a map they will all play is ceremony
without agency.

**NOTHING IN THIS PHASE IS BLIND.** Class selection completes first, so both
compositions are locked and standing in the hall wearing their picks when the
first strike lands. The only thing a team does not know is the other's *map
preferences*, which their strikes reveal.

## Per-match, and what it actually costs

The board is drawn fresh each match. **Only the played map is consumed.** The
five struck maps were never entered, and since map options carry no identity
beyond their profile and thumbnail, a struck realization can reappear on a
later board.

So the pool requirement is **diversity, not volume**: six distinct options
available at all times, one realization burned per match. A pool of fifty
`landmass` maps would be well-stocked and unable to fill a board.

That corrects the success metric this work was measured against. "At least 20
READY maps" counted volume; what a draft needs is *enough distinct options,
continuously*.

## Presentation

Each option hangs on the end-wall rank: its level-4 thumbnail, and its four
advertised properties as always-visible **Text Display** entities rather than
hover-to-reveal. The values are short and the hall's premise is that
contention is visible — making fourteen people each query what could simply be
readable inverts that. Hover earns its place only when there is more data than
fits.

Struck maps go into the same pit as banned classes, attributed by side.

---

# Amendment — allocation is demand-driven, not yield-driven

**Status:** Working, 25 September 2026. Corrects `map_types.allocate`.

`allocate` weights generation budget by measured compile-through yield, so
`landmass` at 44% receives most of it and `shattered_coast` at 3% is starved.
That is correct for a throughput goal and **backwards for a board goal.**

If the board needs one of a Type, demand is the **inverse** of yield:

| Type | yield | generations per map |
|---|---|---|
| landmass | 44.0% | 2.3 |
| shattered_coast | 3.2% | 31.2 |
| a 1% Type | 1.0% | 100 |

**A rare Type needs more budget, not less.** Yield-weighted allocation
systematically starves exactly the Types that are rare *by design* — which is
the definition of Pale Forest / Mansion and Sky Islands, not a defect in them.
maps.md already says so: "prefer extreme vanilla phenomena over invented
terrain", and a compiler should "search for those outliers rather than
rejecting them". That was written down, and then an allocator was built that
does the opposite.

So allocation states **what the board needs** and spends inversely to yield to
meet it. `shattered_coast` at 3% stops being a Type to starve and becomes one
to pay for.

[OPEN] What the board's Type composition should be — whether all in-rotation
Types must appear, and in what proportion.
