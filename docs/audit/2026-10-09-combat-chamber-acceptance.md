# Combat chamber: slice 1 acceptance

**9 October 2026.** The first slice of the combat chamber (`docs/design/COMBAT_CHAMBER.md`)
run against a real server with the real `MobaPlugin`, driven by a `NmsBodies` body as
the tester. **19 of 19 checks pass** (`implementation/plugin/nms-probe/combat-acceptance/`).

## What is built

| Piece | State |
|---|---|
| Availability from declared outputs (Operator, Receive, Counter, Observer) | built, tested against all 18 manifest classes |
| Pre-entry state machine (class, role, slot, output, aim, script, modes) | built, tested |
| Damage log (raw and final, rolling window, time to kill) | built, tested |
| Blank slab venue with terrain bands | built, geometry tested, build verified live |
| `/moba lab combat` commands and Operator mode | built, verified live |
| Cooldown waiver as a pre-entry mode | built, verified live |
| Recipient and Observer entry | **not built**: refused with a reason |

Operator mode uses a real `NmsBodies` player as the dummy, so no temporary
non-player target was needed.

## Two findings from running it

1. **`LobbySafety` cancels all damage to anyone not in a running match.** The design
   assumed the chamber needed no match, on the evidence that `TeamDamage` only cancels
   hits between same-team players. That was true and beside the point: `LobbySafety`
   (priority HIGHEST) cancels every hit on a player who is not a living participant of
   a running match. The first live run recorded zero hits, and the dummy's health did
   not change even after a 100,000-point hit. The fix is an explicit exemption for the
   chamber's tester and their dummy (`CombatChamber.occupies`). It is deliberately
   narrow and a unit-level reading of the code would not have found it.
2. **Final damage can be below raw with no armor involved.** Vanilla gives a hurt
   entity a short damage-immunity window. A second hit inside it applies only the
   amount by which it exceeds the first, and none at all if it is smaller. Two 8-point
   hits in quick succession record one hit, and a 100,000-point hit straight after an
   8-point one had a final of 99,992. After the window passed, a huge hit recorded
   raw 100008 and final 100008. This is the game's behavior, it matters when judging an
   ability's damage, and the chamber's report does not hide it.

## Measurement notes

- A player's Bukkit health is a 0 to 20 bar and `VitalsScaling` rescales incoming
  damage at HIGH. Raw damage is read at LOWEST in effective points and final damage is
  converted back with `Vitals.toEffective`. Both are effective points; the displayed
  value is these times 100. The scaling round-trips exactly.
- A lethal hit on the dummy is recorded at its true size, then clamped, so a very large
  hit is measured and does not end the session.

## Not covered

- Recipient and Observer (need the dummy to cast on command).
- Any class's abilities driven through the chamber; the fixture used direct `damage`
  calls, not Miner's Strike or Brawling Slash.
- Team assignment, walking input, and an in-world menu (the pre-entry flow is
  command-driven for now).
- Level 15 is the default level; the dummy has no class and level 0, so its effective
  capacity is small (9 points), which only matters because the clamp keeps it alive.
