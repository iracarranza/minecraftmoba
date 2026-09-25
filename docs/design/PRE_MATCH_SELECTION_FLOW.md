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
