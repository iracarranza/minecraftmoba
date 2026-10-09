# Combat chamber acceptance fixture

A test-only plugin that drives the real `/moba lab combat` commands inside a
server with the real `MobaPlugin` loaded, using a `NmsBodies` body as the tester
(op'd by the fixture). It writes `plugins/CombatAcceptance/report.txt` and stops
the server. `report-1.21.11-132.txt` is the last run: 44 of 44.

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

## Not covered

An ally dummy (no team assignment; the refusal is in code, not exercised live),
marked-point aim, looping replay, replay of held inputs, held (charged or channelled) abilities through
`BodyCaster`'s hold path, Observer's own controls, and walking input.

Aim matters to what the real ability does: Drill Rush aimed at the tester (slightly
downward) is refused by the ability itself with NO EXIT, because the emerge point is
inside the floor. That is the ability's own rule, faithfully reproduced.
