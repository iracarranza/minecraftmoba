# Combat chamber acceptance fixture

A test-only plugin that drives the real `/moba lab combat` commands inside a
server with the real `MobaPlugin` loaded, using a `NmsBodies` body as the tester
(op'd by the fixture). It writes `plugins/CombatAcceptance/report.txt` and stops
the server. `report-1.21.11-132.txt` is the last run: 19 of 19.

Set up the throwaway server exactly as in `../bodies-acceptance/README.md`
(Paper `1.21.11-132`, EULA, `maps/*.json.gz`, never the live alpha server), put
`minecraft-moba-*.jar` and `CombatAcceptance.jar` in `plugins/`, and **make sure
no other server process holds the world**: a stale one makes the new server fail
on `world/session.lock` while the old one keeps running.

## What it checks

`lab start` reaches the lab room; the pre-entry flow starts at the class step;
a movement-only ability is refused without advancing; an ability that acts on
others advances; entering as Recipient is refused and the tester stays in the lab
room; an Operator entry with night time and the cooldown waiver lands in the
combat world with the class and level applied; the slab is built exactly as laid
out; a dummy player stands at its post; a hit is recorded at raw 8 and final 8;
an immediate second hit fires no event (damage immunity); a huge hit after the
window is recorded at its true size and the dummy survives; the waiver clears a
spent cooldown; the report names the waiver, units and source; `reset` rebuilds
the slab and replaces the dummy; `clear` empties the logs; `leave` returns the
tester and removes the dummy; nothing is left behind.

## Not covered

Recipient and Observer entry (the dummy cannot cast on command yet), any class
ability driven through the chamber, and team assignment.
