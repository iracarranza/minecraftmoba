# Lab hub, combat pre-entry menu, and the lab scoop

**Prototype/test. Built 9 October 2026.** Three changes that make the chambers usable by a
person rather than only by someone who already knows the commands.

## 1. The hub: how a tester reaches a chamber

Until now every chamber was reachable only by typing its command, and nothing in the lab room
mentioned the new ones. The room now has a **pedestal and a label for every bench**, and the
setup menu has a **Benches** page.

| Pedestal block | Bench | Needs |
|---|---|---|
| Netherite block | Combat chamber | nothing |
| Lodestone | Legibility bench | nothing |
| Hay bale | Opportunity bench | nothing |
| Grass block | Terrain chamber | a launched scoop |

Right-click a pedestal, or open **Benches** from the setup menu (the compass), or type
`/moba lab bench <id>`. All three run the same code, so there is one way in. The terrain
chamber is shown with its requirement and refused with the reason instead of being hidden: a
bench that quietly disappears reads as missing. `LabHub` holds the layout as pure data and its
tests check that pedestals are distinct, inside the hall, apart from each other and from the
three existing ones, and that exactly the terrain chamber needs a scoop. The setup menu is
still admin-only (`moba.admin`).

## 2. The combat chamber's pre-entry as a menu

The flow (class, role, ability, output, aim, script, modes) was command-driven. It is now an
inventory menu opened by the pedestal, `/moba lab combat`, or `status`. `CombatPreEntryView`
(pure) lays each step out and decides what each slot means; `CombatPreEntryMenu` draws it.
Every slot's action is the typed command it stands for (`pick mole`, `set cooldown waive`), so
the screen adds no second grammar and the commands still work.

- An option that cannot be chosen is greyed with its reason and cannot be pressed.
- At the modes step each setting is one button that cycles, and its label shows the CURRENT
  value, so a tester reads what will be measured before entering: cooldowns normal or WAIVED,
  time of day, level (presets up to the configured maximum), and the dummy's side where more
  than one is meaningful. The dummy button is absent for an Operator, who has no dummy side.
- Closing the menu does not cancel the flow; `status` reopens it.

## 3. A launchable scoop

The terrain chamber and the scoop clock had never run because no scoop could be launched: the
trial-chamber publication rule had rejected all 16 maps of the last batch. There is now a
**lab scoop**: a verified, READY, bound map published with exactly that one rule waived.

- `foundry.publish(..., lab_only=True)` waives only the trial-chamber rule and records it in
  the manifest (`lab_only`, `waivers` with the findings, and a label saying so). Everything
  else is still required: verified, READY, runtime bindings.
- `foundry_run.run(..., lab_pool=...)` publishes the playable maps there alongside the normal
  pool. Run on seeds 910001 to 910008 it published three scoops (910003, 910004, 910006), the
  same three the rule had rejected.
- **A match can never claim one.** `MapPool.claim` skips any entry marked `lab_only`, even by
  name and even if a lab directory were ever configured as the pool.

### What launching it found

Running the terrain chamber for the first time found one real defect, now fixed. **The
CERTIFIED SCOOP button could never work on a real scoop.** It sampled a stretch "near the
world spawn", but a compiled scoop generates a window thousands of blocks from its spawn (the
first three sit around x -2100, z 1500). Every attempt found no generated chunks and refused.
`BayTerrain` now samples around the places the map itself certified (its fountains,
objectives and Lair) via `CertifiedCentres`. The random-seed path, which creates and deletes a
temporary world, was the riskiest piece of the chamber and worked first time.

## Verification

Layout, menu screens, mode cycling, centre sampling, the lab-only publish and claim rules:
unit-tested (Java and Python). Live, on a real server with a real launched scoop: the hub
(pedestals and labels, the Benches menu, the terrain refusal, the combat pedestal opening the
class screen, a full Mole/Operate/modes/Enter walk through the menu landing in the chamber
with the waiver and level applied, and the other two pedestals entering their benches); the
scoop launching with the tester as Mole; the REAL match clock (`time dusk`, `night 2`, pause,
resume, skip 10); and the terrain chamber (bay and platform, certified and random terrain,
entering with the hotbar menu, previewing and placing a Fountain, and the Clock page jumping to
dusk). **Not verified:** that any of it feels right to a person; every live step was a
scripted body firing real events.
