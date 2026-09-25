# Pre-Match Selection Flow — classes, then maps

**Status:** Working design direction
**Date:** 2026-09-24
**Scope:** What happens between `/moba match start` and players being sent to their Fountains.

This record resolves the class/map ordering question left open by
[`MATCH_LIFECYCLE_OBJECTIVES_AND_OPENING_DECISIONS.md`](MATCH_LIFECYCLE_OBJECTIVES_AND_OPENING_DECISIONS.md)
§1 and describes the selection flow in enough detail to be built. Values —
counts, timers, bans per team — are deliberately not fixed here.

---

## 1. Order: classes first, maps after

**Decided.** Teams draft classes without knowing which map they will play. The
map option is selected afterwards.

This is the deliberate choice §1 asked for, and it assigns each layer a distinct
strategic job:

- **The class draft is committed blind.** Teams cannot tailor their composition
  to terrain, so a composition must be defensible across the Map Types in
  rotation. Drafting a comp that only works on one geography is a real risk the
  player takes on.
- **The map draft becomes the counterpick layer.** Once compositions are known,
  choosing between Default, Archipelago, Underground and future Types is a
  decision made with full information about both teams. A mobility-heavy comp
  and an excavation-heavy comp want different geography, and the map phase is
  where that argument gets settled.

Map-first would have inverted this, making classes the counterpick and the map a
blind commitment. Interleaving would have blurred both. Class-first is chosen so
that the blind commitment is the one with more recovery available: a class is a
kit, while a map is the entire physical world the match happens in.

The resulting lifecycle:

```
/moba match start
    -> create/lock match and teams
    -> CLASS SELECTION          (physical hall: ban phase, then pick phase)
    -> MAP SELECTION            (map-option draft; format per §1)
    -> claim compatible READY realization
    -> instantiate/bind match systems
    -> send players to Fountains
    -> start active play / match clock
```

Class selection happens before any realization is claimed, so the hall cannot
live on the played map. It is a separate staging area.

---

## 2. The Class Selection Hall

Class selection is a **place**, not a menu. Both teams are present in one shared
hall containing one physical station per class — an armour stand or equivalent
display wearing that class's characteristic gear.

Selection is performed by walking to a station and interacting with it.

### Why physical

- **Contention is visible.** A banned or taken class is physically gone or
  visibly claimed. Nobody has to be told what is still available.
- **Teammates read each other's intent from geometry.** Where people are
  standing communicates a developing composition without anyone speaking,
  which matters for a format with no assumption of voice comms.
- **The roster teaches itself.** Players walk past nine classes to reach the one
  they want.
- **It is Minecraft-native**, consistent with the project's standing preference
  for world state and physical verbs over parallel abstract systems.

### Shared hall, public information

Both teams occupy the same hall and watch each other act. The draft is
therefore **open information**, as in the map phase that follows. A team sees
enemy bans land and enemy picks get claimed as they happen.

[OPEN] Whether the two teams are physically separated within the hall — facing
galleries around a shared central rack, or free mingling — is undecided. It
affects how readable the enemy's developing composition is, and whether players
can physically crowd or obstruct a station.

### What a station carries: the class, and only the class

**Decided.** The unit of selection is the **class**. Branches and upgrades are
not chosen here — they are chosen during the match, as progression unlocks
them.

This follows from the progression schedule rather than being a separate
convenience: ability upgrades, the second ability, and the ultimate arrive at
authored levels during play, so a pre-match draft could not select them without
pre-empting the curve. The draft picks who you are; the match decides what you
become.

A station therefore needs only enough to identify and characterise its class —
the physical display, the name, and a short identity line such as archetypes
and hook. It does not need to teach three branch trees.

This keeps the pre-match short, and keeps the deep-kit reference problem where
it belongs: in-match, for a player deciding an upgrade, rather than in a hall
where everyone else is waiting.

[OPEN] Exact presentation of the identity line, and whether a fuller kit
reference is browsable in the hall for players who want it.

---

## 3. Ban phase

**Every player may ban.** Each player on each team can ban a class by
interacting with its station. A banned class is removed from the hall for the
remainder of that match's selection.

Bans are **global**: a banned class is unavailable to both teams, not merely
denied to the enemy. This is what the physical model already communicates — a
station that is gone is gone for everyone in the room.

[OPEN] The following are undecided and change the phase's character
substantially:

- **Bans per team.** One per player is the obvious reading of "all players may
  ban", giving a total equal to the roster size, but the total may want to be
  smaller than the number of players.
- **Simultaneous or sequential.** Simultaneous bans are faster and can collide,
  with two teammates spending bans on one class. Sequential bans are slower and
  fully legible.
- **Reveal timing.** Whether a ban is visible the instant it lands, or all bans
  resolve together at phase end. This only matters if bans are simultaneous.
- **Collision handling.** If two players ban the same class, whether the second
  ban is refused, wasted, or returned.
- **Whether banning is mandatory.** An unspent ban at timeout may lapse or be
  assigned.

---

## 4. Pick phase: the turn is the ability to move

**Only some players from each team may act at a time.** All players remain in
the hall throughout; the ones who are not on turn can watch but cannot claim a
station.

This makes the draft turn physically legible. A team's turn is not an on-screen
prompt — it is the moment its players can walk. Spectating players see exactly
who is deciding, and a team under pressure to hurry is visibly a team standing
still.

[OPEN] Undecided:

- **Window size.** How many players per team may act simultaneously. One
  produces a strict, slow, highly readable draft; two or more produce a faster
  phase with intra-team coordination inside each window.
- **Alternation.** Whether teams take windows alternately, or both teams hold an
  open window at once.
- **Window duration**, and whether a window closes early once its players have
  all locked in.
- **Exclusivity.** Whether a class taken by one team is unavailable to the other
  — no mirrors — or exclusive only within a team. Global exclusivity makes each
  pick simultaneously a denial; team-only exclusivity allows mirror matchups.
- **Locking.** Whether a claimed station can be released and re-picked within
  the same window, and whether a pick is final once the window closes.

### Enforcement: off-turn players are ghosts

**Decided.** Players who are not on turn are put into a ghost state within the
hall: **mostly invisible, freely flying, unable to interact, and passing through
everyone else.** Players on turn are solid, grounded, and able to claim a
station.

This is not Minecraft's spectator mode. Ghosts keep a body and remain in the
world — faintly visible, located somewhere specific, confined to the hall.
Spectator mode would let them leave, see through terrain, and disappear
entirely, which would cost the properties below.

It resolves the legibility problem in the strongest available direction. Turn
state stops being an invisible permission and becomes **the most visible
property in the room**: the solid players are the ones deciding. Nobody has to
be told whose turn it is.

Three consequences worth keeping:

- **Ghosts cannot obstruct.** Passing through is required, not cosmetic.
  Without it, idle players crowding a station would be an obvious grief and the
  hall would need physical crowd control.
- **Ghosts still broadcast intent.** A ghost hovering at a station is reading
  it, and teammates and opponents can both see that. Off-turn players keep the
  pre-commitment channel the physical hall exists for, instead of going dark
  until their window opens.
- **Solidifying is a moment.** Dropping into the world at the start of a window
  and lifting out of it at the end marks the turn boundary without a timer
  having to announce it.

Flight is bounded by the hall. [OPEN] Whether that bound is physical, a soft
push-back, or a return teleport; the exact visual treatment of a ghost; whether
allied and enemy ghosts are distinguishable; and whether ghosts are audible.

---

## 5. Timeouts and fallbacks

The phase must terminate regardless of player behaviour. A single absent player
cannot be allowed to hold a match.

[OPEN] Required rules, none of which are decided:

- what a player receives if their pick window expires unspent;
- what happens to a disconnected player's pick and to their team's remaining
  windows;
- whether a late-joining or reconnecting player inherits an assigned class;
- whether an unspent ban lapses or is auto-assigned.

Whatever is chosen should be a **default assignment rather than a stall**, and
should be visible in the hall so the rest of the match knows an assignment
happened rather than a choice.

---

## 6. Map selection

Map selection follows class selection unchanged from
[`MATCH_LIFECYCLE_OBJECTIVES_AND_OPENING_DECISIONS.md`](MATCH_LIFECYCLE_OBJECTIVES_AND_OPENING_DECISIONS.md)
§1: teams draft **Map Types and map options**, never visible generated worlds,
and a compatible hidden READY realization is claimed only after the option is
settled.

Two consequences of running it second:

- **Both compositions are known** when the map is chosen, so the map phase
  carries the counterpick weight the class phase deliberately gave up.
- **The claimed realization is still hidden.** Teams choose a Type and its broad
  classification, not a world they have seen. Class-first does not make the map
  phase an informed choice about terrain detail — only about geography grammar.

[OPEN] The map phase's own format — ban order, option-set overlap, final-choice
rights — remains open per §1. If it lands on alternating ban/counterpick, the
class phase's turn-window structure and the map phase's turn structure should be
made to resemble each other, so the two halves of the pre-match read as one
process rather than two unrelated games.

---

## Explicitly open

This record does **not** decide: bans per team; ban simultaneity, reveal timing
or collision handling; pick window size, alternation or duration; whether class
exclusivity is global or per-team; lock and re-pick rules; ghost bounding,
visual treatment and audibility; timeout and disconnect fallbacks; hall layout
and team separation; station identity-line presentation; or the map phase's
draft format.

It decides that **classes are drafted first and blind, maps second and
informed**; that class selection is a shared physical hall in which every player
may ban; that turn order is expressed as permission to move, with off-turn
players ghosted so the players on turn are the visibly solid ones; and that the
unit of selection is the class alone, with branches and upgrades left to
in-match progression.

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

---

# Amendment — the type set is revealed, and niches are the point

**Status:** Working, 25 September 2026.

## The board's Type set is revealed before class selection

Immediately before the class draft opens, both teams see **which Map Types
are on the board** — the types alone. No thumbnails, no Scale, no Resource
Density, no Terrain Symmetry, and no indication which will survive.

**Before, not during.** Information arriving mid-draft raises a timing
question — at bans or at picks? — and hands an advantage to whoever's window
it lands in. Revealing it first is clean, and it lets class *bans* be informed
by the type set, which seems right rather than wrong.

**It does not break class-first.** It narrows the defensibility requirement
without collapsing it:

| what is known | a composition must work across |
|---|---|
| nothing (previous) | the whole rotation |
| **the Type set** | **these six Types** |
| everything | one map |

The middle is genuinely distinct from both. Teams still do not know which of
the six survives, nor any map's scale, density, symmetry or terrain, so the
map phase keeps every bit of its counterpick weight — and both teams orient
against the same six, so it is symmetric.

## What it is actually for: pricing niches

A first reading of this was that it lets players *calibrate preferences*
rather than hope, which is true and is the smaller half. The larger half:

> **The reveal is what makes specialised classes negotiable instead of a
> coinflip.**

A class that is transformative in one condition and ordinary outside it — a
Mermaid with water, a Mole underground — is a design asset, not a defect.
Picking it with the Type set known is a **declared strategy**, and the draft
becomes the mechanism that prices it:

- the pick *signals*, and the opponent must respond
- the opponent can spend strikes denying the condition, or let it through
- the specialist keeps lines either way — a prepared off-condition game if
  denied, or a choice of win condition if a serving map survives
- and every intermediate posture is available to both

That is the draft doing its job. Without the reveal, the same pick is a
gamble on whether the condition appears at all, and gambling is not
strategy.

### The correction this makes to an earlier position

An earlier reading here held that "a class that only works on one Type is a
class problem, not a draft problem". That is too strong and it conflates two
different things:

- **Dependency** — the class does nothing without its condition. The draft
  becomes a coinflip on whether the condition appears, and the reveal would
  merely make a broken class *feel* fine. Still a class problem.
- **Specialisation** — the class is capable without its condition and
  exceptional with it. The draft becomes a negotiation. **This is the case
  worth building for**, and the reveal is what lets it be negotiated.

The line is whether the class has a game when denied.

## The strike economy this creates, and the lever inside it

On a six-board struck `A1 · B1 · A1`, **A has two strikes and B has one**. So:

> **A niche is deniable exactly when the number of board maps serving it does
> not exceed A's strike count — and denying it at that limit consumes A's
> entire strike budget.**

| maps serving a niche | outcome |
|---|---|
| 1 | deniable for 1 of A's 2 strikes |
| 2 | deniable, but costs **both** of A's strikes |
| 3+ | **cannot be fully denied** |

Two watery maps out of six is therefore the sharpest case: A can shut the
Mermaid line down completely, and pays for it by having no strike left for
anything else. B knew that when it picked, and B's own strike can protect the
fallback by removing a map that is bad for the off-condition game.

**Board composition is therefore a balance lever**, not just a diversity
target: how many maps serve a condition sets whether that condition is
deniable at all, and at what price.

## The stocking goal, stated plainly

> **Usable rare Map Types are gathered in equal quantities to usable common
> Types.**

That is what `map_types.allocate` produces with equal `wanted`: budget is
`wanted / rate`, so expected output is `budget x rate = wanted`, equal by
construction. It is checkable — count the pool by Type and they should match.

The price is worth stating before someone reads the allocation as a bug:
equal quantities of a 3% Type and a 44% Type sends roughly **93% of
generation** to the rare one.

And the reveal raises the stakes on delivering it. Board composition is now
**publicly visible every match**, so an allocator that underdelivers puts that
failure in front of fourteen players rather than in a batch log. A forcing
function, and a promise.
