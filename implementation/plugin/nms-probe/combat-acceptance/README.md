# Combat chamber acceptance fixture

A test-only plugin that drives the real `/moba lab combat` commands inside a
server with the real `MobaPlugin` loaded, using a `NmsBodies` body as the tester
(op'd by the fixture). It writes `plugins/CombatAcceptance/report.txt` and stops
the server. `report-1.21.11-132.txt` is the last run: 125 of 125.

Set up the throwaway server exactly as in `../bodies-acceptance/README.md`
(Paper `1.21.11-132`, EULA, `maps/*.json.gz`, never the live alpha server), put
`minecraft-moba-*.jar` and `CombatAcceptance.jar` in `plugins/`, and **make sure
no other server process holds the world**: a stale one makes the new server fail
on `world/session.lock` while the old one keeps running.

## What it checks

`lab start` reaches the lab room; the pre-entry flow starts at the class step;
a movement-only ability is refused without advancing; an ability that acts on
others advances; entering as Recipient (Crash Landing, enemy dummy) reaches the combat world; an Operator entry with night time and the cooldown waiver lands in the
combat world with the class and level applied; the slab is built exactly as laid
out; a dummy player stands at its post; a hit is recorded at raw 8 and final 8;
an immediate second hit fires no event (damage immunity); a huge hit after the
window is recorded at its true size and the dummy survives; the waiver clears a
spent cooldown; the report names the waiver, units and source; `reset` rebuilds
the slab and replaces the dummy; `clear` empties the logs; `leave` returns the
tester and removes the dummy; nothing is left behind.

Commanded casting: a Mole dummy enters as a Recipient, is enrolled as Mole at
level 15, and `cast` runs the REAL Drill Rush (execution count 0 to 1, dummy
invulnerable). A second `cast` is the ability's own recast and the dummy emerges
(invulnerability off). A passive slot is refused with the flow unchanged.

Recording and ghost replay: as Operator the tester records two slot presses (Drill
Rush, then its recast); the take is two inputs as Mole; `replay` spawns a Mole ghost
that performs both from inputs alone (execution count 1, emerged, 2 ran and 0
refused).

In-chamber UI (no typing): entering puts the menu on the hotbar, Leave in inventory
slot 9, and builds a control deck of ten world buttons; Dummy casts is greyed with a
reason for an Operator; with ability mode on, right-click is the ability's alone and
the menu does not act; the Record item opens its page; Start recording, the real casts,
the STOP RECORDING button, the GHOST ONCE button and the LEAVE button all work through
real Bukkit events, and leaving restores the hotbar and removes the deck. The Leave
inventory item's click was not simulated (the world button was).

Interference: a looping Mole ghost casts from its script; a strike stands it down
(STRUCK) and it stays passive and casts nothing while still in combat; once out of
combat it resumes and restarts its script (executions 1 to 3); with interference
toggled off a strike changes nothing and it carries on; with it back on, a stun
stands it down (STUNNED).

Lab time and rules, in the combat chamber: `lab time dusk | midnight | dawn` sets the
world time (12000, 18000, 0); `time skip` is refused there; a frozen hunger cancels a
food LOSS but not a gain; regeneration off cancels satiated healing but not other
healing; leaving clears the tester's rules.

Legibility bench (`/moba lab legibility`): entering builds the marked track, the deck
(nine buttons) and the hotbar, and spawns the subject 16 blocks out; the Next scale item
sets the subject's scale to 1.25; the GLOW button makes it glow; armor, particles and
clutter change the variant and the world (leather helmet, a stone-brick pillar placed then
removed); two marks at 16 and 40 blocks give a median of 28.0 and a pixel height; the
report states the doctrine rule and that per-viewer glow is not delivered; the LEAVE button
leaves and removes the subject and deck. A bug found by running it: the deck's buttons
vanished because nothing held their chunks, so the bench now keeps its chunks loaded.

Opportunity bench (`/moba lab opportunity`): a Herd registers as a RECOVERING source; manifest
and harvest are refused with reasons; skipping recovery manifests five sheep at an eligible
site 43 blocks from the tester; harvest one and all count through the runtime's own
listeners; a standing manifestation cannot be skipped; the second manifestation is displaced
at least 12 blocks from the first; building over the region blocks the next manifestation and
the report names the ground; clearing and forcing works; a wheat patch of eight and a
harvest; a ravager by day in the mountain biome, refused at night ("NOT eligible") and forced
by day; a source with radius 4 against a region of half-span 12 is swept to RECOVERING with
nothing harvested (the Alpha radius/region mismatch); nothing granted by renewal; clean leave.
`ACCEPT_ONLY=opportunity` runs just that group (plus setup) for quick iteration.

Lab hub and pre-entry menu: the room has a pedestal and label for every bench; the Benches
menu opens; the terrain pedestal is refused without a scoop; the combat pedestal opens the
class screen (six classes, no way back); a Mole/Operate walk through the menu reaches the modes
screen, the mode buttons cycle (waiver, time, level) and Enter lands in the chamber with those
modes; the legibility and opportunity pedestals enter their benches.

Launched scoop (needs a lab scoop in the server's `lab-maps`, see `docs/design/LAB_HUB.md`):
the scoop launches with the tester as Mole; `lab time dusk`, `night 2`, pause, resume and
`skip 10` drive the REAL match clock; the terrain chamber allots a bay with its platform and
buttons, fills it from the certified scoop and from a random seed, enters with the hotbar menu,
previews and places a Fountain (undoable), and the Clock page jumps to dusk. `ACCEPT_ONLY` takes
a comma-separated list of name prefixes, e.g. `scoop,terrain`.

## Not covered

The match-clock half of lab time (`time skip`, `time night`, `pause`, `resume` in a
launched scoop) and the terrain chamber's Clock page: no launchable scoop exists on
this machine, so those are compiled and arithmetic-tested but never run.

Particles actually rendering (the variant and its task are set, not seen; likewise the opportunity bench's eligible-site display), the glow being SEEN by a real client, and the terrain chamber's Regenerate and route items, DISPLACED and ROOTED interference (the cause paths exist and are unit-tested, but nothing live pushed or rooted the ghost), an Observer's default of ignoring interference, and an ally dummy (no team assignment; the refusal is in code, not exercised live),
marked-point aim, looping replay, replay of held inputs, held (charged or channelled) abilities through
`BodyCaster`'s hold path, Observer's own controls, and walking input.

Aim matters to what the real ability does: Drill Rush aimed at the tester (slightly
downward) is refused by the ability itself with NO EXIT, because the emerge point is
inside the floor. That is the ability's own rule, faithfully reproduced.
