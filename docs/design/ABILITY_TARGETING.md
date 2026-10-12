# Ability targeting

**Prototype/test. Surveyed and first pass built 11 October 2026.** How every ability finds what it hits,
what was wrong, and the two pieces of shared code that came out of it. The target vocabulary itself
(`TargetForm`, `InputForm`, `Recipients`, `Targetability`) is not changed here; see
[ABILITY_SKELETON.md](ABILITY_SKELETON.md).

## What the survey found

Twenty-six abilities, and until this pass no shared way to find a target: each wrote its own `rayTrace` or
`getNearbyEntities`. The declared forms are sound and the cleanest abilities already agree on a shape:

- **Food Fight** is the model for a projectile: a ray along each tick's path, hit radius, speed, range and
  gravity all in config, allies accepted only on the branch that heals them.
- **Brawling Slash** is the model for a targeted attack, with caveats: it declares a direction form but acts
  as "first enemy on a ray, then a homing leap, or a cone slam when Unleashed".
- **Flip and Press** and **Graveyard Shift** are the best examples of how a real player aims: they prefer the
  click's block or entity and fall back to a ray.

Inconsistencies found, with what has been done about each:

| Finding | State |
|---|---|
| Ability damage landing in ability mode was cancelled and read as a left-click | **Fixed and verified live** (below) |
| Ray widths of .2, .25, .3, .35 and 1.0, only Food Fight in config | **Fixed**: Food Fight, Brawling, Flip and Press, Graveyard Shift and the Observer all read config |
| Each ability decides for itself whether an ally blocks its ray | Fixed for the three moved: an ENEMY ray passes through allies |
| Area attacks pass through walls | A line-of-sight option exists and is verified; **no ability uses it yet**, see decisions |
| Sinkhole and Rabbit's Lucky Foot ignore the clicked block and always re-ray | Open |
| Hotswap's ray fallback always places on top; a click places on the face clicked | Now testable and shown live: a body that clicks a wall gets the web in front of it, an AIR cast gets it on top. The difference remains, it is no longer hidden |
| Scripted bodies send no block or entity, so the click path is never tested | **Fixed and verified live** (below) |
| `tunneling.targetDistance` is read and the result discarded | **Removed**: the dead ray call and its config key |
| The caster's own tamed animals are not "allied" (Lunge, Brawling and Rabbit can hurt them); Rabbit pulls in every wolf, fox and cat nearby whoever owns it | Open, a design call |
| Observer sees through walls | Open, a design call |
| Hotswap's TNT and lava are not in `TeamDamage`'s allow-list | Open |

## The damage door

`AbilityInputs.attack` read every damage event with a player damager as a **left-click**, because a melee
swing is the only left-click the server can see, and a scripted hit looks identical. An ability's own damage,
landing while its caster had ability mode on, was cancelled and fired another ability, closing the mode.
That is the normal rapid-fire rotation, not an edge: a projectile lands after the caster has pressed F again.

Caught live: a scripted hit of 3 landed twice with mode off (20 to 13.33 to 6.67); with mode on it did
nothing and the mode closed.

`AbilityDamage.deal(target, amount, source)` is now the only way ability damage is dealt. It holds a
per-thread counter open across the call, events are dispatched synchronously inside it, and the input layer
stands down while it is open. Nothing else changes: team protection, stuns, armor, combat state and every
other listener still see an ordinary player-caused hit. **A real melee swing is not marked, so it still reads
as a left-click** (checked live). A unit test fails the build if any source calls `damage(amount, source)`
outside the door, which found a test-only ability the first search missed.

Sixteen call sites moved: Brawling (three), Crash Landing, Drill Rush, Flip and Press, Lunge, Food Fight,
Irresistible Buffet, Rabbit's Lucky Foot, Jumpstartinator, the Gardener thicket, the Werewolf maul and sweep,
the opportunity bench's harvest, the scenario bench's `hit` act, and the test abilities.

**Not changed, on purpose:** Werewolf lifesteal, the Daredevil clutch restore and Lunge's disabled-hand check
still react to any player-caused damage, including an ability's, because "a successful attack" plausibly
includes one. Whether it should is a design question.

## The targeting helper

`AbilityTargeting` is the shape the cleanest abilities had in common, written once:

- `ray(caster, from, direction, range, width, relation)`: the first creature or block on a line, stopped by
  walls. `Relation` is ENEMY, ALLY or EITHER, resolved through `Targetability`; the caster is never a target.
- `within(caster, centre, rx, ry, rz, relation, lineOfSight)`: every creature whose box overlaps a box.
- `inCone(...)`: the flat radius-and-cone test, pure.

Food Fight (per-tick ray), Brawling Slash (first-hit ray and its cone slam) and Flip and Press (aimed
fallback) are on it and keep the values they had: ray widths moved from code to config
(`brawling_slash.rayWidth` .35, `flip_and_press.rayWidth` .25, Food Fight's existing `hitRadius`), with no
change to what any of them hits.

## The remaining migrations

Every creature-targeting area query now goes through `AbilityTargeting`, with the reach each ability already had:

- **Around the caster** (`around`, which reads `Entity#getNearbyEntities`, a box that includes the caster's own size):
  Lunge, Crash Landing (damage and Crater's stun), Jumpstartinator's blast, Rabbit's Lucky Foot's impact (from the animal
  it launched) and the Werewolf's hidden-sweep.
- **Around a point** (`within`): Brawling Slash's leap path and slams, Drill Rush's emerge (damage and Undermine's stun),
  Irresistible Buffet's table.
- **A ray that ignores walls** (`rayIgnoringWalls`): the Observer, now named as a choice, so decision 3 below is one call.
- Brawling Slash's path box and slam geometry moved to config (`pathRadius` 1.2, `pathHeight` 1.8, `areaHeight` 2,
  `sweepConeDot` .4, `thirdSlamConeDot` .15) and the Observer's width to `observeWidth` (1.0).

`around` and `within` are different boxes on purpose: moving an ability from one to the other would change how far it
reaches by the caster's own width, and none of them was changed.

**Deliberately not moved:**

- **Physics-axis queries** (Runway's Suplex, the Toolbox Piston, the Hopper's vacuum) act on anything that can be moved,
  including dropped items. The helper is creatures only, by design; `Targetability.impulse` stays the right tool there.
- **The Gardener's aura** treats its owner as an ally (Absorption, food), and the helper never returns the caster.
- **Animal scans** (Rabbit's pull-in, Bounding, passives) pick species, not a relation.
- **Skeleton Crew's worker aggro** is that class's own targeting.

## Bodies that carry a click

`BodyCaster.cast` now sends what a client's click would have carried, through `ClickResolver`:

- Reach is vanilla survival: 4.5 blocks for a block, 3.0 for a creature. The crosshair is on a creature when one is in
  reach and no block is nearer, otherwise on the block, otherwise on air.
- What the event carries depends on the input: a right-click carries the block and face, or the creature; a
  left-click carries the block and face but **not** a creature (hitting one is a damage event); the hand swap and the
  drop carry nothing. This matches `AbilityInputs`' own handlers.
- `ClickMode.RESOLVE` is the default for the chamber's dummies and the scenario `cast` act. `ClickMode.AIR` (and
  `click: air` on the act) sends no click, which is how every cast used to be sent. The result reports what was clicked.

Live: a Kitfighter body right-clicking a one-block wall puts Hotswap's web in front of the wall (the clicked face); the
same cast as AIR puts it on top (the ray fallback); a body two blocks from the tester clicks the tester, and six blocks
away clicks air. A held ability still repeats an air click each tick.

## Verified, and not

- Java suite passes; the combat chamber acceptance run is 208 of 208; the kit fixtures pass 25 of 25
  (Food Fight damage and healing among them) and 36 of 36 (Human Slash hitting an enemy among them).
- Live: the damage door in all three cases (control, plain call still swallowed, `deal` lands and keeps the
  mode); an ENEMY ray passing through an ally and hitting the dummy, EITHER stopping at the ally, ALLY
  hitting the ally; a wall stopping a ray; line of sight leaving out a creature behind a wall; the caster
  never a target.
- **Not verified:** Brawling Slash's Unleashed cone slam and homing leap were exercised only through the
  next-kits fixture, which does not cover every branch; a real player's aimed casts; and that the three moved
  abilities *feel* the same.

## Decisions for the owner

1. **Should area attacks need line of sight?** The option exists and works; turning it on changes what
   Lunge, Crash Landing, Runway, Drill Rush, Rabbit and the Werewolf sweep hit.
2. **Are a caster's tamed animals allies?** Today they can be hurt by the caster's own area attacks.
3. **Should the Observer see through walls?** Today it does; every other entity ray stops at them.
4. **Should "a successful attack" effects (lifesteal, clutch restore) count an ability's damage?**
