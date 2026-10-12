# Kitfighter, Werewolf and Gardener lab pass

11 October 2026. Canonical authority remains `classes.md` sections 7, 29 and 5.
These are **Prototype/test implementations**, not a promotion of Working directions
or a final balance decision. Kitfighter and Werewolf keep their existing design
status; Gardener remains conceptually settled. All three are registered in the
combat chamber and have passive, A1, A2 and ultimate implementations.

## Kitfighter

- **Salvage** settles an actual crafting inventory's Primary Material debit against
  the corresponding new equipment in the player's inventory/cursor. Three or more
  ingredient cells of one Primary Material qualify; one Salvage per accepted craft,
  material-specific thresholds 6/5/4/3 at levels 0/8/16/24, two matching material
  units recovered. Same-tick repeated crafting clicks are refused until the matrix
  settles. Canceled crafts and missing output do not award. Overflow refunds drop
  at the owner. Copper participates under the newer 19 September statement.
- **Offhander** puts the branch's item in the offhand IN PLACE OF THE MAP (Bow, or Crossbow, Fishing Rod or Golden
  Head), and pressing A1 again puts the map back. The item is issued: nothing is needed in the inventory and nothing
  is taken or moved. It cannot be dropped or lifted out of the slot, is removed on death and on quit, and an emptied
  hand (eating a Golden Head) gets the map re-issued by the offhand system. Native offhand interactions run outside
  ability mode; mainhand use priority is still vanilla. **Revised 11 October 2026:** the first version exchanged a
  carried item with the offhand and left the map in an inventory slot; the plugin pauses enrolment when the offhand
  holds anything but its map, so a Kitfighter lost their class a second after pressing it (caught live,
  `enrolled=false paused=true`). The Offhander item is now the offhand's recognised occupant. Golden Head is labelled
  and receives extra native regeneration when consumed by Kitfighter. No free ammunition is introduced: a Bow or
  Crossbow still needs arrows or bolts in the inventory. Whether the item should be issued or carried is a
  Prototype/test choice, made 11 October on the owner's description.
- **Hotswap** places its branch's placeable at once in the cell against the face being aimed at (a click's own face, or
  the face the aim ray hits; the ray version used to always place on top of the block, which a click did not). It deploys in an eligible empty target cell. Base web is short; Cobweb
  lasts longer; Lava is contained against fluid spreading and bucket extraction;
  TNT is a real sourced primed explosive. Web/Lava hold shared reservations and
  restore the exact original air state. Breaking them ends the trap without drops.
  TNT uses native explosion rules, including systemic damage and terrain damage;
  teardown removes unexploded TNT. These are persistent casts, so closing the input
  menu does not end them.
- **Covered With Diamonds** adds only each equipped slot's missing Diamond armor
  and toughness through named attributes, recalculated while active. Above-Diamond
  protection is retained. Real armor, enchantments and durability stay unchanged;
  the speed and knockback resistance modifiers expire or are removed on teardown.
  Golden Head gains an additional Absorption rider while projected. The older
  Notch Apple/Ender Crystal escalation wording does not specify a settled mapping
  or lifecycle and remains Open; this pass does not fabricate transferable items.

Recipe derivative/custom-component accounting, same-tick external inventory
changes, drop-from-result crafts, offhand rod/bow client behavior, and exotic armor
attributes need further acceptance. Settlement is deliberately conservative when
newly crafted output has already left the inventory. Custom non-additive item armor
modifiers are outside this first projection model; normal vanilla tiers are covered.

## Gardener

- **Plant Material** is a class-state meter, never an inventory item. A full meter
  automatically grows the available Cultivar nearby; if there is no eligible soil
  or capacity, the full meter is retained. Prototype placement radius, lifetime,
  cap and rates live under `gardener` in config. No Development Zone is required.
- **Clip** preserves the source block and its growth data exactly. Specimen rewards
  maturity; Proliferation counts nearby same-family plants with a bounded scan;
  Collection rewards new species/families with diminishing repetition. The canonical
  supplied Specimen/Proliferation/Collection names are used.
- **Cultivate** consumes one eligible real plant item at lower efficiency. Its branch
  selects the normal Torchflower, Sweet Berry Bush or Giant Bamboo Cultivar.
- **Torchflower** creates a real plant and an adjacent invisible Light block, and
  grants allied Absorption. **Sweet Berry Bush** provisions allies and applies
  authored enemy thicket damage/Slowness, while suppressing its native contact damage.
  **Giant Bamboo** creates one bounded generation of empowered offspring. Offspring
  project growth acceleration but never reproduce again.
- Cultivars accelerate nearby ageable crops through vetoable `BlockGrowEvent`s;
  bamboo/cane have bounded stalk growth. Other supported plant ecology gets an extra
  native random tick, retaining native sapling/stem/vine/kelp behavior. Growth becomes
  real world development; it is not rolled back when the Cultivar expires.
- **Grafter's Handbook** queues the two unselected forms for subsequent full meters,
  in stable order, without changing specialization. It does not grant free Plant
  Material. After both grow, normal specialization resumes. Queue lifetime and the
  surviving Cultivar lifetime are Prototype/test interpretations of Open sequencing.
- Cultivars reserve their occupied cells, suppress harvesting, and can be destroyed
  by breaking any of their parts; no synthetic cultivar products drop. Lifetime,
  death, disconnect, class change, lab reset and plugin disable restore reserved air.
  Native world growth, enemy counterplay and persistent match ecology need playtests.

The combat chamber includes an ecology plot on the grass band: x12–16, z-8–-4,
with mature/immature wheat, carrots, beetroot and potatoes. Normal soil around it
supports cultivar placement. The archetype conflict remains unresolved.

## Werewolf

- **Lycanthropy** uses the shared CombatState. More than 60 consecutive server ticks
  below 70% maximum health transforms; Transformed persists while Combat persists.
  A Prototype/test smooth time-of-day curve sets natural escalation to Unleashed.
  Unleashed lasts exactly 200 ticks, forces Human plus recovery debuffs, and starts
  the authored 200-tick natural-Unleashed lockout. Howl bypasses the lockout.
- Size changes the real scale/hitbox attribute. Maximum effective health includes
  the level-dependent 8/15/25% form bonus plus an additive 10% Unleashed bonus.
  With vitals scaling enabled the bar remains 20. Transitions preserve existing
  current effective HP, rather than granting new HP. These two Open health choices
  are explicit Prototype/test decisions. Unscaled mode uses a named max-health
  modifier instead.
- **Brawling Slash** uses held-weapon attack damage at 105%, with no critical roll.
  Human/Transformed hit the first ray-traced target and track an eligible player
  through a delayed, bounded, terrain-blocked scripted leap while slipping entities.
  Whirling Slash damages the Human path without knockback; Gnashing Slash pulls
  Transformed path targets along toward its endpoint; base Transformed knocks them
  away. Unleashed sweeps, then follows a committed ballistic direction and crushes
  enemies underneath. Lumbering Crash adds a delayed wider cone slam.
- **Bloodhide** is 20% reduction for 60 ticks; Transformed can reach 40% after taking
  more than a configured health-relative threshold. Human Brawler's Mind cancels
  projectile damage and projectile hit resolution; Transformed Canine Instinct
  removes the critical multiplier. Unleashed retains full incoming damage, gains
  capped size from it, and sweeps attacks after gaining size. Rabid Frenzy can re-arm
  against the next eligible attacker without resetting/spending the original cooldown
  or extending the hide. The recast goes through normal input mode and Stun gates.
- **Monster Howl** cleanses harmful potions and enters Unleashed. Cast Transformed:
  heals 65% missing HP. Cast already Unleashed: grants configured lifesteal without
  restarting its deadline. It supplies public Glow for the original remaining
  Unleashed duration. Other contextual empowerment Glow rules remain Open.

Branches use the existing mutually exclusive branch selector and empower only the
corresponding form: this is a Prototype/test answer to the Open selection question.
Weapon enchantment fidelity, critical removal with enchantments/custom modifiers,
projectile-type/client pass-through behavior, multiplayer leap feel, narrow-gap
hitbox collision, sweep geometry and lifesteal balance still need acceptance.
No enchantment-specific damage is invented by the scripted weapon hit.

## Lab integration and validation

All three classes appear automatically in the combat chamber's class/branch menus.
Use Equipment / controls or `/moba lab combat equipment` to access carried items.
Bring the relevant real tools/plants; human inventories are not overwritten with
synthetic supplies. Fake lab bodies receive explicit gear/seeds for these classes.
The body tick now invokes native equipment detection as well as travel, so their
actual weapon and armor attributes update after inventory changes.

`/moba lab scenario next_kits_skirmish` runs the new four-body encounter on a launched
scoop. It covers protection, plant conversion, Bloodhide, Howl, enemy damage and
allied safety. Its schema is validated with the scenario suite; a launched-scoop
run is separate from the isolated mechanics fixture and is not claimed here.

See `validation/plugin/next-kits/` for native fixture source and saved results.
Pure state-machine tests cover streak interruption, out-of-combat reversion, exact
Unleashed expiry, lockout, repeat Howl and time-of-day wrap. Projection and shared
chunk lease tests cover above-Diamond preservation and overlapping cast lifetimes.
Validation: **1,121 unit tests, 36 native next-kit checks, and 25 earlier-kit native
regression checks passed**, with zero failures. The saved build record identifies
the exact plugin/config hashes and isolated runtime. The native crafting and
projectile tests have the fixture limits described in its README.

Temporary terrain is restored on normal teardown, but the shared reservation
ledger is still in-memory and has no crash recovery. Cultivar lifecycle/persistence
and exact exotic equipment behavior stay Open; no canonical status is promoted.
