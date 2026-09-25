
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
