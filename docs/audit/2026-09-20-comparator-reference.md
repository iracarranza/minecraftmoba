# Comparator reference — how other games solve the unmentioned systems

Companion to [the system status audit](2026-09-20-system-status.md). Band F of
that audit found ten systems a MOBA needs that occur **zero times** in the
canonical documents. This records how comparable games solve them.

Organised by system, not by game, because the useful signal is where solutions
diverge — particularly between top-down and first-person, since this project is
first-person and most MOBA design assumes a top-down camera.

**Sourcing caveat.** Values marked `[unverified]` could not be confirmed to
current-patch accuracy and should be checked before use. Games in active
development — Deadlock, Smite 2, MCC — change fastest.

---

## The three findings that matter most

### 1. First-person fog of war is entity culling, never terrain culling

In a top-down MOBA the camera *is* the vision system: fog is literal blackness
painted on terrain you are looking down at, and the minimap is an authoritative
model of everything you know. Denying information means darkening pixels.

A first-person camera cannot do this. The world in front of you is always
rendered; blacking out terrain makes the game unplayable. So Smite and Deadlock
split vision in two:

- **World geometry is always visible.** You can always see walls, lanes, objectives.
- **Only entities are hidden.** Enemy players and their state render or do not.

Smite hides gods outside team vision and relies on genuinely 3D line of sight —
you **cannot see over your own walls**, which makes its jungle far more
dangerous than LoL's. Deadlock leans on verticality: elevation and rooftops are
the information resource in place of a ward economy. `[Deadlock's current
vision rules are unverified and changing.]`

**Consequence here.** Do not attempt terrain fog: Minecraft renders chunks and
will not hide them. Build entity visibility instead — hiding player entities and
nameplates outside team vision maps directly onto vanilla primitives, and
`glowing` covers revealed enemies. **Verticality is the fog**, and Minecraft is
unusually good at sightline-blocking terrain. A ward becomes a placed block with
a vision radius, which is about as Minecraft-native as a mechanic gets.

A **directional damage indicator** is close to mandatory in first person. Smite
has one; top-down games do not need one. "Someone is hitting me and I cannot
tell from where" is a real failure state that a 90-degree camera creates.

### 2. The minimap is load-bearing in first person, and Minecraft cannot render one

Every first-person and third-person MOBA has a minimap in a screen corner, and
it carries information the camera physically cannot. Smite players read it
constantly; it substitutes for the top-down view. Awesomenauts has one even as a
2D side-scroller.

Minecraft has no minimap and client mods cannot be assumed. The offhand
`MapRenderer` this project implements is the closest equivalent, and is the only
arbitrary per-player pixel surface a vanilla client offers.

Where a minimap is impossible, **its information content must be relocated**:
team and objective state to the scoreboard sidebar, timers to boss bars,
revealed enemies to entity glow, and pings to world-space markers. Hypixel
Bedwars' bed-status sidebar is exactly this — it answers "what is the global
state" with no spatial rendering at all.

### 3. Smite's VGS exists *because* the camera is first-person

Smite's **Voice Guided System** is a letter-chord scheme — `V`, then a category
letter, then a command letter — that makes your god **speak the call aloud**.
Well over a hundred commands. `[The full chord table is unverified.]`

It exists precisely because you cannot see the map, cannot see your team, and
cannot look away to type. Deadlock solves the same problem differently: pings are
placed in **3D world space** by aiming the crosshair, and render as persistent
markers **with off-screen edge indicators**, so a ping reaches a teammate whose
camera points elsewhere.

**Consequence here.** A first-person MOBA needs communication that reaches
someone looking the wrong way: audio cues, world-space markers with off-screen
indicators, and a tight vocabulary. Target **6–10 ping types** like LoL's
constrained wheel, not Dota's hundred-entry chat wheel. In Minecraft this is a
raycast from the look vector, a marker entity at the hit point, a sound cue, and
an action-bar directional hint.

**Minecraft servers are the gap.** No true ping wheel was found in Hypixel
Bedwars or Blockwars; Bedrock modes use canned quick-chat menus, and MCC relies
on external voice comms. There is no Minecraft-native precedent to copy here.

---

## Match lifecycle

Three families:

**Destroy the base, with escalating objectives.** LoL and Dota have no timed
end and no draw state; anti-stalemate is objective escalation — turret plating
expiry, Baron, Elder Dragon, Dota's Mega Creeps and Roshan's Aegis.

**Fixed clock with score.** Pokémon Unite runs a hard **10-minute** match whose
final two minutes **double all scoring**, plus a late legendary spawn. Smite's
Arena drains a **ticket pool**. The entire early game can be invalidated —
heavy-handed, extremely effective at keeping matches live.

**Best-of-N rounds.** Battlerite and Bloodline Champions reset to parity every
round, which makes the round itself the comeback mechanic.

**Deadlock's two-phase win** is the most interesting: Guardians → Walkers → Base
Guardians → **Patron**, which at low health becomes a **Weakened Patron** that
retreats and must be killed a second time, healing to full if you fail. An
anti-snowball step baked into the win condition. **Rejuvenator** resurrects all
destroyed friendly objectives.

**Bedwars** is the cleanest Minecraft escalation clock, and the pattern is worth
copying wholesale: generator tier upgrades → **Bed Destruction** removing the
respawn mechanic entirely → **Sudden Death** spawning dragons that actively hunt
campers → a hard **Game End** timer ending in a tie. `[Per-mode minute values
unverified.]`

**First-person players tolerate fixed-clock and round formats better than
destroy-the-base**, because the latter requires map-wide reading a first-person
camera makes expensive.

## Team assignment and queue

LoL uses position-based role queue with a 1-2-2-1 pick order and two ban phases.
Dota's Captains Mode gives one captain all picks against a reserve clock.
Deadlock **assigns lanes automatically**, and the lane count collapses 4 → 3 → 2
as objectives fall — a structural way of forcing teams together without a
grouping mechanic.

**Minecraft's idiom is entirely different and is the one to copy**: party-based
queueing, and **kit selection in the pre-game lobby**. Blockwars and Mineplex
both do lobby GUI → kit → full loadout. That is Minecraft-native champion
select, and it needs no separate draft client.

MCC does not queue at all: ten teams of four, assembled by organisers.

## Respawn

**Scaling models.** LoL scales on champion level (~6s at level 1 to ~50s+ at 18)
multiplied by a **Time Increase Factor** at roughly 15, 30 and 45 minutes; no
gold is lost. Dota scales on hero level to ~100s, **loses gold on death**, and
sells **buyback** as a paid skip. Deadlock combines both and adds a physical
twist: **you drop souls on death** and anyone can pick them up. Pokémon Unite
drops your unbanked Aeos energy — you lose score, not power.

**Binary models.** Bedwars is **5 seconds while your bed lives, permanent
elimination once it falls**. No time scaling at all; the scaling moved from the
timer to a *team objective*. Battlerite has no respawn within a round.

**This is the most important divergence for a Minecraft build.** A scaling timer
needs a visible countdown and player education. A bed needs neither. If late-game
stakes are wanted, gate respawn on **a destructible objective** rather than a
formula.

Awesomenauts is worth noting for a different reason: you re-enter in a
**steerable droppod**, choosing your landing lane — a partial answer to
"respawning far from the fight is boring."

## Spectating

Every top-down MOBA clamps the dead player's camera to **team fog of war**. Dota
and Deadlock do the same.

**Minecraft does not do this for you.** Hypixel's eliminated players become full
spectators who can fly anywhere and see everything, because Bedwars has no fog
to protect.

**If any vision system is built here, spectator free-cam must be clamped**, or a
dead teammate calling positions becomes strictly better than any ward — the
dominant information channel in the game.

## Shop and the recall loop

LoL: fountain shop, **8-second recall channel** broken by damage. Dota removes
the walk home entirely with a **courier** plus Town Portal Scrolls.

**Heroes of the Storm has no shop, no items and no gold at all** — replaced by a
**talent system**, one pick at levels 1, 4, 7, 10, 13, 16, 20. This is the most
relevant model here: it deletes the economy, inventory and shop-UI problem
outright, and replaces "go home and buy" with "pick a perk when you level." In a
game where inventory space is already contested by blocks, tools and weapons,
a talent system avoids fighting the hotbar. Pokémon Unite does the same, moving
held items entirely **pre-match**.

**Deadlock's first-person insight**: walking back is far more tedious in first
person than top-down, so it added **ziplines** along every lane rather than a
recall cast.

**Bedwars is the proven Minecraft economy**: two NPC shops per base — items and
**team upgrades** — four physical currencies from generators that escalate on
the match clock, and nothing beyond a chest GUI. Its key split is worth
stealing: **personal consumables are lost on death, team upgrades are
permanent.**

## Comeback and snowball control

LoL and Dota use **bounties**, with Dota scaling kill gold on the *net worth
difference* so killing a fed enemy while behind pays disproportionately.

**HotS shares all XP team-wide**, so no individual can snowball and a losing
team is never more than a level or two behind. Structural rather than patched
on; the cost is the carry fantasy.

Unite's **double-score final two minutes**. Deadlock's Weakened Patron and
Rejuvenator. Battlerite's round reset.

**MCC's multiplier ladder is the most aggressive anti-snowball device found**:
each of eight games is worth progressively more, and then the **top two teams
play Dodgebolt, whose winner takes the entire event regardless of points**.
`[The exact multiplier ladder and current team roster are unverified.]`

For Minecraft, shared XP and round reset are cheap and need no bounty HUD.
Bounty gold requires per-player economy tracking *and* somewhere to display it.

## Surrender

LoL allows `/ff` at 15:00 with 4 of 5 votes, early surrender at 10:00 with an
AFK, and a **Remake** at ~3:00 voiding the match. HotS and Pokémon Unite have
**no surrender at all**. Minecraft modes have none either.

**The correlation is exact: every game with a hard clock has no surrender.**
Only open-ended games need one. A match timer makes surrender unnecessary.

## MCC and Blockwars specifics

**MCC**: ten teams of four, eight games from a rotating pool, then Dodgebolt.
Coins convert to points; score multipliers escalate per game slot. Teams are
**colour plus an alliterative animal** — Red Rabbits, Orange Ocelots, Lime
Llamas, Cyan Coyotes. That naming does real work: identity, mascot and an
unambiguous visual code at once, and it **survives colourblindness** because the
name disambiguates where colour alone would not. `[Current roster unverified.]`

**Blockwars**: score and time based, not elimination. CTF to a target capture
count or highest at expiry; a dropped flag **auto-returns after a timer**; flag
state lives on the **scoreboard sidebar**. Kits chosen in the lobby and
**re-granted in full on every respawn**, so death costs tempo but not power. Red
versus Blue expressed through dyed armour, nametags, sidebar — and **coloured
building blocks, which double as a build-ownership indicator**. You can see who
built a wall. That is an elegant Minecraft-only affordance and directly relevant
to Construction and block provenance here. `[Current mode list and kit roster
unverified.]`

---

## Items flagged as unverified

Bedwars phase minute table; MCC multiplier ladder and team roster; Blockwars
mode list and kits; Deadlock vision rules and item tier prices; Smite VGS chord
table and Smite 2 changes; LoL's current Baron timing and Atakhan/Void Grubs;
Dota's `gg` concede rule; Pokémon Unite quick-chat entries; Smite and Deadlock
surrender thresholds; exact respawn formulas for Smite and Deadlock.
