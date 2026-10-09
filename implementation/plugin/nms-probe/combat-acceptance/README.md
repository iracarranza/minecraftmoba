# Combat chamber acceptance fixture

A test-only plugin that drives the real `/moba lab combat` commands inside a
server with the real `MobaPlugin` loaded, using a `NmsBodies` body as the tester
(op'd by the fixture). It writes `plugins/CombatAcceptance/report.txt` and stops
the server. `report-1.21.11-132.txt` is the last run: 27 of 27.

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

## Not covered

An ally dummy (no team assignment; the refusal is in code, not exercised live),
marked-point and recorded aim, held (charged or channelled) abilities through
`BodyCaster`'s hold path, Observer's own controls, and walking input.

Aim matters to what the real ability does: Drill Rush aimed at the tester (slightly
downward) is refused by the ability itself with NO EXIT, because the emerge point is
inside the floor. That is the ability's own rule, faithfully reproduced.
