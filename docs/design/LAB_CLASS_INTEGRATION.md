# Classes in the lab

**Prototype/test. 11 October 2026.** Is every registered class reachable through the combat chamber, the scenario
bench, loadouts and testing mode, and does each have what it needs to be tested? Checked after the six newest classes
(Quarryman, Bloodmason, Chauffeur, Fungal Assassin, Paver, Hollow) landed.

## The sweep

The acceptance run now ends with one step per registered class (fifteen, `test` excluded). For each it checks that the
class is offered by the pre-entry flow; which slots a Recipient can receive; that it enters as Operator and is enrolled
with the class; that all three slots have an ability; that pressing each by the real input path runs it or is refused;
that a body of the class spawns enrolled; and that a body can be told to cast each slot.
**A class that cannot be offered, entered or spawned, or is missing an ability, fails the run.** What a cast
does after that depends on targets and resources, so "ran" and "refused" are recorded, not judged.

Result: all fifteen pass. The six newest were already selectable (the roster feeds both lab selectors without a
whitelist, as the six-class record says), enter, enrol, and have all three slots built. Hollow's Frighten uses the
shared forced-movement status, so it shows up in the status bench's checks and the scenario harness like any other.

## What was missing: a way to get the items, and a way to be told

A class's abilities consume ordinary items (sand and gravel for the Paver's Mixer, Concrete for its Shoes, food for the
Chef) and need targets and tools. A first attempt at this wrote a kit per class into config. That was the wrong shape:
nobody should need a kit authored for their class, and a tester should be able to get what they need themselves. It was
removed. Two things replaced it.

**The item shelf.** The loadout editor (the ender chest) has a **Get items** control that opens every item in the game:
by category (Combat, Armor, Tools, Food, Redstone and machines, Plants, Materials, Blocks, Everything else) or by name,
paged. Left-click adds a full stack to the loadout being edited, right-click adds one. Armor goes to its own slot while
that is empty, other items top up a matching stack before taking a free slot (never the reserved Exit slot), and Back
returns to the editor with everything saved. Search is a word or two typed in chat, or
`/moba lab combat loadout find <words>`. It adds to the selected loadout and never to the tester's own inventory, so
Start testing then gives it, and the same loadout can be given to the dummy.

**Refusals that say what is missing.** An ability can declare its needs (`Ability.needs`): a carried item, a target in
reach, a tool. When a cast is refused the dispatcher asks which are unmet and says so, on the HUD line and in chat:
"Food Fight needs food in your inventory (any edible item) (you carry none)." It speaks only when the ability states
needs and one is unmet, so an ability refused for its own rule is not given a made-up reason. Declared so far: Food Fight,
Irresistible Buffet, Flip and Press, Offhander, Hotswap, Clip, Cultivate, Sinkhole, Rabbit's Lucky Foot, Brawling Slash,
Concrete Shoes, Bursting Spore, Myceliate, Runway, Crash Landing and Burning Out. Runway and Crash Landing's old "TOO
SLOW" notice and the Chef abilities' own chat lines were replaced by it.

The sweep records, for every slot it could not run, what the ability says it needs, which gives a punch list of refusals
that still say nothing: Bloodmason's two actives, Chef's none, Daredevil's body casts, Mole's Drill Rush, Paver's Rebar
Chain, Fungal Assassin's ultimate, Toolbox's Reconfiguratron (which has its own message), and Skeleton Crew's body
casts. Those are the ones to give needs next, where a need can be stated.

## A scenario

`six_kits_skirmish` runs Quarryman, Hollow, Chauffeur and Paver casting a real ability each, with Bloodmason as a target,
and passes 7 of 7. Bloodmason is not asserted because its A1 and ultimate are channelled and do not register as a body
cast; that is a limit of how a body casts, not a verdict on the class.

## Verified, and not

Live on Paper 1.21.11 build 132 with scripted bodies; the acceptance run is 244 of 244 and the kit fixtures pass
(25, 36, 37 and the six-class fixture's 54). **Not verified:** that the shelf is comfortable with a mouse, that a body's "cast" for a held ability means the ability did its work, or any class's feel at a real client.

The sweep runs last on purpose: placed before the scoop steps it changed their timing and failed nine of them, so it
sits at the end where its load cannot disturb anything.
