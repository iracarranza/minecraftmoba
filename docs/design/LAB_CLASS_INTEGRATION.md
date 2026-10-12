# Classes in the lab

**Prototype/test. 11 October 2026.** Is every registered class reachable through the combat chamber, the scenario
bench, loadouts and testing mode, and does each have what it needs to be tested? Checked after the six newest classes
(Quarryman, Bloodmason, Chauffeur, Fungal Assassin, Paver, Hollow) landed.

## The sweep

The acceptance run now ends with one step per registered class (fifteen, `test` excluded). For each it checks that the
class is offered by the pre-entry flow; which slots a Recipient can receive; that it enters as Operator and is enrolled
with the class; that all three slots have an ability; that pressing each by the real input path runs it or is refused;
that a body of the class spawns enrolled; that it carries its kit; and that a body can be told to cast each slot.
**A class that cannot be offered, entered or spawned, or is missing an ability or its kit, fails the run.** What a cast
does after that depends on targets and resources, so "ran" and "refused" are recorded, not judged.

Result: all fifteen pass. The six newest were already selectable (the roster feeds both lab selectors without a
whitelist, as the six-class record says), enter, enrol, and have all three slots built. Hollow's Frighten uses the
shared forced-movement status, so it shows up in the status bench's checks and the scenario harness like any other.

## What was missing: kits for bodies and testers

Five classes (Chef, Kitfighter, Skeleton Crew, Werewolf, Gardener) had their items hardcoded into the body factory.
The six newest had none, so a Paver dummy had no sand to mix and a Quarryman dummy no pickaxe: the sweep showed their
bodies refused casts, and a tester had to build a kit by hand.

`alpha.lab.classKits` in config now lists what each class carries, as `MATERIAL:amount` stacks in slot order:

- **Bodies** receive their class's kit at spawn (the hardcoded blocks are gone; the five kits are unchanged, and a test
  pins them).
- **Testers** get it as a loadout: the editor's **Fill with my class's kit** control, or
  `/moba lab combat loadout kit`, which fills the selected loadout with the kit for the class they are testing; Start
  testing then uses it.
- **Every class must be listed**, an empty list meaning it consumes no items. A unit test fails when a new class is
  not, and when a kit names an unknown item, a bad amount, or a class that does not exist.

The new kits are fixtures, not design, and some are guesses at what the class consumes: Paver sand, gravel, dye, Concrete,
Concrete Powder and a water bucket; Quarryman iron and diamond pickaxes; Chauffeur a boat, a minecart and cargo. Classes
with an empty list (Bloodmason, Fungal Assassin, Hollow, Mole, Daredevil, Lightfooted, Toolbox) consume no items or their
resources are not plain items; the Toolbox's belt components in particular are not covered.

## A scenario

`six_kits_skirmish` runs Quarryman, Hollow, Chauffeur and Paver casting a real ability each, with Bloodmason as a target,
and passes 7 of 7. Bloodmason is not asserted because its A1 and ultimate are channelled and do not register as a body
cast; that is a limit of how a body casts, not a verdict on the class.

## Verified, and not

Live on Paper 1.21.11 build 132 with scripted bodies; the acceptance run is 238 of 238 and the kit fixtures pass
(25, 36, 37 and the six-class fixture's 54). **Not verified:** that the kits are the right ones for a human testing the
classes, that a body's "cast" for a held ability means the ability did its work, or any class's feel at a real client.

The sweep runs last on purpose: placed before the scoop steps it changed their timing and failed nine of them, so it
sits at the end where its load cannot disturb anything.
