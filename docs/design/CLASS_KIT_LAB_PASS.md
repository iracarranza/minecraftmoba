# Class kit lab pass — 11 October 2026

**Prototype/test implementation.** Canonical class statuses and Open design
questions remain those in [classes.md](../../classes.md). This pass starts the
requested Chef + Daredevil + Lightfooted + Mole lab expansion; it does not
claim final balance, finished progression curves, or exhaustive live acceptance
of every branch.

## What is available

| Class | New implementation in this pass |
|---|---|
| Chef | Trusty Spatula; Food Fight and all three branches; Flip and Press and all three branches; Irresistible Buffet |
| Mole | Temporary restoring Sinkhole; Armored Emergence; Burrow Chain; explicit burrow movement/timeout cleanup |
| Lightfooted | Rabbit's Lucky Foot; animal-side passive bonuses; wolf recharge scaling, fox disable scaling and Stalking Pounce hit; landing-driven Bounding and snow invisibility window |
| Daredevil | Skydiver measured in server ticks; Trampoline landing rebound; Suplex downward burst; Pop Rocket gust/fall-protection prototype |

All four classes now bind three real actives. Toolbox remains available as the
additional comparison kit. Skeleton Crew's test bindings were outside this
pass and still do not implement its canonical kit.

## Chef

Food selection walks inventory storage slots 0–35 deterministically. Rationed
copies remain in that order, are distinctly named/tagged and cannot create more
rationed copies. Food Fight travels as a visible item with collision checked
along each segment. It damages enemies, heals allies under Super Nutritious,
ignites enemies under Extra Spicy, and loads supported cookers. Rationing pays
one extra labeled raw input only when loading an already active compatible
cooker. Misses drop the thrown food. Session cleanup removes outstanding
projectiles without creating refunds.

Supported cooking targets are Furnaces, Smokers and lit Campfires, using the
server's cooking recipes. Flip and Press advances productive time immediately;
Furnace/Smoker completion carries excess time into the next input while paying
fuel and respecting output capacity. Campfire slots advance independently and
complete through cook events. Rake the Coals reduces paid fuel; Broiler Rack
increases advancement with batch size. Supplemental fuel supported by the
prototype is listed in `chef.fuelTicks`; other fuel is left for vanilla rather
than guessed. Remaining burning fuel is usable regardless of fuel material.

The Spatula is the highest ranked carried shovel. Prototype ordering is wood,
gold, stone, copper, iron, diamond, netherite. Its range/strength bonuses and all
cooldowns, health amounts, durations and branch coefficients are config fixtures.
Healing converts effective health through `Vitals`, like the other class heals.

Buffet rolls out visible temporary tables and seats enemy players. **Prototype
resource decision:** each invitation spends nine actual items matching Chef's
first stored food; insufficient food prevents that invitation. Chef remains free
to act. The real hotbar is cloned into custody and serialized in the player's
PDC before servings are installed. Inventory movement, drops, swaps and pickups
are guarded while seated. Capacity refresh leaves the overlay alone. Servings
are ordinary edible items, and consumption does not export bowls or bottles.

Dismount/jump restores the hotbar and applies a minor uneaten-serving penalty.
Expiration, teleport, reset, caster/session teardown and disconnect restore it
without an escape penalty. Death removes servings from drops and preserves the
original hotbar through Paper's kept-item path; ordinary non-hotbar death rules
remain independent. Reconnect can recover a saved hotbar loan. These choices
are implementations for the lab, not settled answers to Chef's remaining
resource/progression questions.

## Lab use

Choose Chef in combat setup. The **Equipment / controls** hotbar action, its
control-deck button, or `/moba lab combat equipment` swaps the control menu for
your real equipment. Consumed or rearranged gear survives toggling back to the
menu. Commands and the deck remain available while using equipment. Supply your
test inventory with a shovel and food; the chamber does not overwrite the
operator's gear with a canned loadout.

Chef fake players receive an iron shovel, cooked beef and raw beef as **lab
fixtures**, so commanded casts and scenarios have resources. The new
`/moba lab scenario chef_skirmish` uses two teams of two and checks Food Fight
execution, enemy damage, allied safety and Buffet execution on a launched lab
scoop. Its configuration loads under the normal scenario validation. The native
acceptance run below exercises the underlying mechanics in a separate flat
fixture; it is **not** evidence that this new scenario passed on every scoop.

Flip and Press has a close-range enemy output and a cooker output. Explicit
block/entity clicks carry their target; air casts, including lab bodies, resolve
their aim once. The chamber dummy begins eight blocks away; move into the
ability's reach when testing the close-range output.

## Other kits and remaining limits

- **Sinkhole:** public block telegraph, a delay computed from the slowest native
  `Block.getBreakSpeed(player)`, a bounded jagged block selection, No-Build
  reservations, zero-drop removal and exact restoration. Shared displacement
  now saves `BlockState` snapshots including container contents and retains
  provenance. Chunk tickets keep live holes loaded. Unbreakable blocks, liquids,
  portal/objective components are excluded as a prototype protected-terrain
  policy. Snapshots are not crash-persistent; Structural Integrity/Wax hardness
  integration still depends on those world systems being implemented.
- **Drill Rush:** protected burrow traversal is server-driven, with jump/sneak
  steering and a bounded timeout returning to entry. Armored Emergence is a
  temporary damage multiplier. Wall emergence can grant Burrow Chain speed;
  approaching a different wall within the window clears only Drill Rush's
  cooldown. The wall/entry detection, movement feel and armor values need
  playtesting; this is not a claim of native client noclip.
- **Lightfooted:** animal bonuses take the strongest qualifying nearby
  Lightfooted contribution, rather than multiplying identical auras. Bounding
  launches again after landing, keeps the chosen horizontal heading in flight,
  allows building and refuses attack cancellation; a timeout handles stuck arcs.
  Snow invisibility occupies the landing-to-next-takeoff tick. Rabbit's Lucky
  Foot uses scripted high arcs, aborts at obstruction and restores animal
  invulnerability afterward. It needs collision/feel tuning in varied terrain.
  Thieving Swipe uses native material cooldowns plus server-side refusal for the
  captured matching mainhand item; identical replacement items and item transfer
  semantics need further refinement.
- **Daredevil:** Skydiver grants controlled airborne acceleration after sustained
  airtime. Trampoline redirects/rebounds on landing, and Suplex adds a downward
  impulse to its captured enemies. **Pop Rocket remains a stand-in:** amplified
  launch, gust visuals and fall protection, rather than a native Wind Charge /
  Wind Burst transaction. That is outstanding fidelity work. Existing Crash
  Landing and Deathly Clutches remain provisional implementations.

## Verification

The plugin builds with the repository's Java 21 / Paper 1.21.11 configuration.
The full unit suite passes (1,111 tests after this pass's initial additions).
Checks cover hotbar loan idempotence, cooking carryover/fuel boundaries,
independent Campfire progress, leap geometry, declarations, config, inputs and
the existing regression suite.

[Native acceptance report](../../validation/plugin/kits/results/2026-10-11/report.txt):
**25 checks passed** on Paper 1.21.11 build 132. This includes real food projectile
damage/healing, Buffet seating/dismount, death-event dispatch, actual fake-player
disconnect, capacity refresh during seating, cooking, container custody,
Sinkhole opening/restoration and forced animal protection cleanup. Death custody is checked both by direct Paper event dispatch and by an actual
death/respawn with keepInventory disabled, preserving originals exactly once. Human eating/dismount controls, all six Chef branch interactions,
the new scoop skirmish and movement feel still need live playtesting.

The fixture is isolated, temporarily disables lobby protection because it runs
outside a match, and stops itself. No running user server was replaced or
restarted. See [fixture instructions](../../validation/plugin/kits/README.md).
