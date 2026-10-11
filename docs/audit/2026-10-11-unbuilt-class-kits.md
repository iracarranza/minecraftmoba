# Documented class kits still to build

Compiled 11 October 2026 against `main` at `12c3ac9`.

This is the **pre-build snapshot**. The subsequent Chef/Mole/Lightfooted/Daredevil
prototype work is recorded in [the lab pass](../design/CLASS_KIT_LAB_PASS.md);
the counts and missing-feature descriptions below describe the baseline.

## Scope and meaning

This is an implementation inventory, not a design promotion or a build request.
“Accepted” is not a single status used by the repository: the list therefore
distinguishes conceptually settled kits, established individual mechanics, and
documented Working directions/proposals. Open mechanics stay Open. Historical
and superseded variants are excluded.

[classes.md](../../classes.md) is the design authority. Build status comes from
the current plugin's [registration](../../implementation/plugin/src/main/java/com/minecraftmoba/plugin/AbilityInputs.java),
[class bindings](../../implementation/plugin/src/main/resources/config.yml),
[passive dispatch](../../implementation/plugin/src/main/java/com/minecraftmoba/plugin/Passives.java),
and ability bodies. Generic test abilities and a declared branch name do not
count as implementation of the corresponding canonical mechanic. “Built” here
means code exists in the current checkout, not that live-server acceptance or
balance testing has passed. Unmerged Git branches are not counted as delivered;
this is not an exhaustive audit of historical branch implementations.

## Settled kits with outstanding implementation

### Mole — conceptually settled

Source: [§4](../../classes.md#4-mole).

- **Ultimate — Sinkhole: unbuilt.** Use the **27 September revision**: publicly
  highlight a jagged affected region, delay destruction, reserve the opened
  volume as No-Build, then restore exactly; zero drops, including protection
  against falling-block duplication. Player construction is eligible. Hardness
  controls mining/destruction timing. The older permanent natural-terrain-only
  version is Historical/superseded. `channel_ult` is only a generic channel;
  `sinkhole_lite` is a separate test prototype.
- **A2 Drill Rush / Armored Emergence: unbuilt.** Major damage reduction after
  emerging; magnitude and duration Open.
- **A2 Drill Rush / Burrow Chain: unbuilt.** Speed after wall emergence and a
  reset for quickly entering another wall; timing and magnitudes Open.

Already present: Sifth Sense, Tunneling with Bore/Gallery/Dig In, base Drill Rush
and Undermine. Drill Rush explicitly documents its two missing branches in
[the implementation](../../implementation/plugin/src/main/java/com/minecraftmoba/plugin/DrillRushAbility.java).
These existing mechanics still need fidelity/playtest review; presence is not
proof that all their intended behavior is complete.

### Gardener — conceptually settled

Source: [§5](../../classes.md#5-gardener). Entire kit unbuilt and not registered.

| Slot | Accepted mechanic | Branches still to build |
|---|---|---|
| Passive — Plant Material | Abilities accumulate Plant Material; full stacks grow a Cultivar that accelerates neighboring plants | — |
| A1 — Clip | Clip living plants without destroying/resetting them; mature plants yield more | Specimen; Proliferation; Collection |
| A2 — Cultivate | Convert eligible inventory plants into Plant Material less efficiently than Clip; select normal Cultivar | Torchflower; Sweet Berry Bush; Giant Bamboo |
| Ultimate — Grafter's Handbook | Sequential access to the two unselected Cultivar types without replacing specialization | — |

Torchflower supplies light/Absorption; Berry Bush supplies provisioning and
defensive thickets; Giant Bamboo extends growth acceleration through empowered,
non-recursively reproducing offspring. Open: numbers, resource eligibility,
Cultivar geometry/lifecycle and Development Zone integration. The archetype
conflict around “Control/Support” remains unresolved; it does not erase the kit.

### Skeleton Crew — conceptually settled

Source: [§15](../../classes.md#15-skeleton-crew).

| Slot/system | Accepted mechanic still to build | Branches still to build |
|---|---|---|
| Passive — Undead Affinity, resource half | Undead kills generate Crew; Crew stacks, Crew Limit, persistent worker entities and cargo | — |
| Base workforce | Combat → Logistics → self-preservation priorities; physical transport/deposit and workforce allocation to Supply Lines | — |
| A1 — Graveyard Shift | Contextual direct Strike/provoke against hostile targets, otherwise consume Crew to Raise | Field Work; Hard Hat Zone; Labored Union |
| A2 — Burning Out | Ignite active Crew; burning accelerates movement without overriding priorities; seek extinguishing when higher duties end | Fire Drill; Deadline; Water Break |
| Ultimate — A-Head of Schedule | Horseman plus temporary specialized workforce; commit once to Combat or Logistics based on ordinary Crew activity | — |

Field Work is projectile Strike **or** remote Raise. Hard Hat Zone uses friendly
Strikes to maintain sequential armor. Labored Union permits autonomous provocation
and Crew-generated Crew. Fire Drill substitutes Alarmed emergency relocation;
Deadline requires sustained burning **combat** to explode; Water Break rewards
successful extinguishing. Horseman workers do not grant persistent Supply Line
Capacity. Open: resource limits, stats, armor/durations, extinguishing criteria,
ultimate quantities and infrastructure progression. Only the passive's extended
undead pursuit is built. Its current three active bindings are generic test
abilities, not this kit.

### Lightfooted — conceptually settled, partially built

Source: [§16](../../classes.md#16-lightfooted).

- **Ultimate — Rabbit's Lucky Foot: unbuilt.** Area-targeted extreme animal
  leaps, delayed caster leap, invincibility through forced animal travel/landing,
  animal impact damage and a stronger caster impact. `channel_ult` is a placeholder.
- **Passive — Animal Senses: partial.** Caster bonuses exist; matching nearby
  animals also need the authored species bonuses. Current dispatch only modifies
  player damage/fall damage and player speed.
- **A1 Lunge / Swarming Bite: incomplete.** Nearby wolves must enable more
  frequent/repeated Lunges. Current code instead adds leap power per wolf.
- **A1 Lunge / Thieving Swipe: incomplete.** Mainhand disruption exists, but
  nearby-fox scaling of its duration is missing; current duration is fixed.
- **A1 Lunge / Stalking Pounce: incomplete.** Fixed windup/current-aim launch
  exists, but it never starts the damage sweep used by the other branches. The
  branch removes extra damage/control riders, not the base Lunge hit.
- **A2 Bounding: implemented prototype with fidelity work remaining.** All
  three branch paths exist (Drift to Drift, Branch to Branch, Track to Track),
  but successive launches use a fixed tick interval rather than detected
  landings. Drift invisibility starts at snow takeoff rather than the authored
  landing-to-next-takeoff window. Complete the committed-airborne/redirect-on-
  landing sequence and snow window rather than listing these as wholly absent.

Open: counts, radii, velocities, damage, windup, disable duration/cap, ultimate
geometry and protection end timing. Evidence:
[TestAbilities](../../implementation/plugin/src/main/java/com/minecraftmoba/plugin/TestAbilities.java),
[BoundingAbility](../../implementation/plugin/src/main/java/com/minecraftmoba/plugin/BoundingAbility.java),
and Passives.

## Documented directions with entire kits unbuilt

These are present in canonical design, but their labels do **not** all assert a
settled or finally accepted class. None is registered as a production class in
the current plugin. Every named branch below is outstanding unless explicitly
marked Open.

| Class / design status | Passive | A1 and branches | A2 and branches | Ultimate |
|---|---|---|---|---|
| [Golem Master, §6](../../classes.md#6-golem-master) — Working draft; supplied kit explicitly not promoted to canon | Pumpkin Supply | Animate — Iron Golem; Snow Golem; Copper Golem | Assemble — Iron Wall; Snow Wall; Copper Wall | Wither Golem |
| [Kitfighter, §7](../../classes.md#7-kitfighter) — current documented direction | Salvage | Offhander — Crossbow; Fishing Rod; Golden Head | Hotswap — Cobweb; Lava; TNT | Covered With Diamonds |
| [Merchant, §13](../../classes.md#13-merchant--active-design) — active design, not settled | Work | Swindle — Invisible Hand; Credit Line **Open**; Community Chest **Open** | Retinue — Seize the Means; Toll Patrol; Overtime **Open** | Procession / Silk Road; final name/package **Open** |
| [Waxer, §18](../../classes.md#18-waxer) — supplied kit; completed conceptual pass Open | Waxed Recipes | Wax-On — Amber; Restorative; Sticky | Wax-Off — Enzymatic; Preserving; Floral | Amber |
| [Bloodmason, §20](../../classes.md#20-bloodmason) — Working proposal | Hemostasis | Capillary Flow — Nausea Wave; Bloodtrail; Lifeblood | Viscerwall — Render Flesh; Fracture; Adipose | Athanor Anatomb, including Hemorrhage and Teratoma replacements |
| [Chef, §22](../../classes.md#22-chef) — proposal | Trusty Spatula | Food Fight — Super Nutritious; Extra Spicy; Reckless Rationing | Flip and Press — Hibachi Skills; Rake the Coals; Broiler Rack | Irresistible Buffet |
| [Fungal Assassin, §23](../../classes.md#23-fungal-assassin) — documented kit | Creeping Colony / Fungal Growth | Bursting Spore — Submersive; Airborne Spread; Root Network | Myceliate — Hardy; Nutrient Cycle; Hyphae Lashers | Destroying Angel's Blessing |
| [Looming Talismaniac, §24](../../classes.md#24-looming-talismaniac) — documented, incomplete; individual cores Established | Pattern Linking | Imprint — Copy; Cut / Discharge; third branch **Open/unnamed** | Contact-reactive cursed construction, final ability name absent — Infestation; Explosive; third branch **Open/unnamed** | Talisman of Undying; economy **Open** |
| [Paver, §25](../../classes.md#25-paver) — proposal | Concrete Mixer | Rebar Chain — Galvanized; Beveled Edges; Pitons | Chamfering — Steep Grading; Pozzolan Patch; Natural Mix | Concrete Shoes |
| [Chauffeur, §26](../../classes.md#26-chauffeur) — proposal | Parking Spot | Cargo Boat — Jetski; Freight Engine; Front Propeller | Muscle Cart — Pop the Hopper; Diesel Engine; Tactical Dismount | Highly Irresponsible Racing Line |
| [Quarryman, §28](../../classes.md#28-quarryman) — Working direction; quantities Prototype/test | Quarry Quota | Ceaseless Swing — continuous-mining harvest-tier passive plus active Scan; no named branch tree | Miner's Strike — toggle; no named branch tree | Eternity Mountain |
| [Werewolf, §29](../../classes.md#29-werewolf) — Working direction; quantities Prototype/test | Lycanthropy | Brawling Slash — Whirling Slash; Gnashing Slash; Lumbering Crash | Bloodhide — Brawler's Mind; Canine Instinct; Rabid Frenzy | Monster Howl |

### Mechanics and unresolved gates for those kits

- **Golem Master:** material-dependent workers and construction; independent
  Animate/Assemble choices. Final Assemble rule, material-stat model and Wither
  behavior need the explicitly requested conceptual pass. Do not treat the
  supplied kit's audit-flagged wording as Established.
- **Kitfighter:** material-specific crafting Salvage; real offhand equipment
  behavior; temporary web/lava/TNT; temporary Diamond protection projection with
  speed/knockback resistance. Equipment already above Diamond and unusual
  equipment interactions remain Open.
- **Merchant:** per-villager reputation, payroll, material processing and Mastery;
  once-per-restock Swindle; workers diverted into Retinue; moving paid procession.
  Open branch concepts above are not implementable specifications yet.
- **Waxer:** waxed crafting/products, per-target Wax, sealing and first-application
  durability repair; productive Honey Solution zones/Honeycomb; Amber stasis.
  The canonical feasibility assessment flags player stasis, sealed interactions
  and crafting behavior that do not work exactly as written. Retain those
  decisions as Open rather than claiming the kit is ready unchanged.
- **Bloodmason:** 0–6 Hemostasis with dual stable points, endpoint self-Stun and
  reset, post-cast P-dependent architecture; moving/truncated Flow segments and
  five Viscerwall forms. Anatomb adds spatial stages, consumable allied healing,
  enemy-damage self-healing, held replacement channels and completion Stun.
  Open: Bloodtrail nominal/effective geometry, structural parameters, costs,
  durations, manifestation and fitting details. Root/Stun support now exists;
  the older feasibility note's “Stun must be built” is no longer a current
  prerequisite blocker.
- **Chef:** highest-tier shovel identity; deterministic inventory food projectiles;
  actual cooker loading/instant productive-time advancement with carryover and
  fuel accounting; non-recursive labeled ration copies; physically seated Buffet
  with eating and escape counterplay. Amounts, stations and Buffet details remain
  Open where the source leaves them so.
- **Fungal Assassin:** persistent connected Networks, infection, maturation and
  territory traversal; pre-Primed ultimate destruction safely prevents bloom.
  Open: Airborne Spread widens Burst versus Mycelium spread, network limits,
  intangible traversal and trigger/maturation parameters.
- **Looming Talismaniac:** individual patterns define overlapping networks; only
  initiating patterns propagate and reached banners are consumed non-recursively.
  Complete-design Imprint and bounded contiguous-material curse geometry are
  established cores. Missing branches, curse bounds and resurrection expenditure/
  propagation are Open, not hidden accepted work.
- **Paver:** reinforced Concrete, chain attachment/traversal, structurally
  supported non-falling Powder and player-authored release propagation. Open:
  “furthest Piton” semantics and implementation/geometry parameters. General-player
  edge behavior and physical hazards must not silently become enemy-only.
- **Chauffeur:** persistent parking anchor; cargo vehicles, variant minecart
  interactions, rails treated as powered and momentum handling; visible straight
  Racing Line with a wider marking corridor and discrete collision acceleration.
  Open: vehicle handling, cooldown/lifecycle and Supply Line proof. The ultimate
  does not create a terrain-ignoring route.
- **Quarryman:** distinct Stage, current progress, LQC and lifetime CQ; harvest
  tier rather than speed in Ceaseless Swing; quota-pausing pickaxe combat;
  interruptible mountain excavation with two vulnerable manifestations and
  persistent partial real-world destruction. Open: quota demand/timer, scan
  rounding, strike lifecycle, dual-body routing, return/protection rules. Its
  authored numerical formulas remain Prototype/test.
- **Werewolf:** Human/Transformed/Unleashed state machine; low-health combat trigger,
  nighttime-dependent escalation, fixed 10-second Unleashed and recovery lockout;
  weapon-based state-dependent slash/leap geometry; state-dependent defense/size/
  sweep; ultimate cleanse, Transformed missing-health heal and Unleashed lifesteal.
  Open: branch selection semantics, health grants/bonus composition, time curve,
  damage threshold, size cap, lifesteal and timer refresh. Use the 1 October kit,
  not the superseded 30 September state-only model.

## Other partially built directions

### Daredevil — supplied kit, conceptual-pass status Open

Source: [§17](../../classes.md#17-daredevil).

- **Passive — Skydiver: unbuilt.** Sustained airborne time grants extreme
  airborne movement. `DaredevilState` records airborne ticks but applies no
  Skydiver movement payoff; no passive hook is bound.
- **Runway branches: authored physical behavior incomplete.** Pop Rocket and
  Trampoline are currently launch multipliers, without their Wind Charge/Wind
  Burst and slime rebound behaviors. Suplex applies the initial launch vector
  to touched enemies; its corresponding downward burst is not separately
  implemented. These are prototypes, not absent branch declarations.

Runway, Crash Landing (Crater/Combat Roll/Superhero), and Deathly Clutches have
real ability implementations. Remaining passive thresholds, movement and branch
calibration are Open; this inventory does not claim those existing actives passed
live testing.

### Toolbox — kit built; unresolved ownership and deferred capstone

Source: [§19](../../classes.md#19-toolbox). Utility Belt, Reconfiguratron!
(Speedy Swap!! / More Config!! / Spare Parts??), Jumpstartinator!
(Overcharging!! / Super Circuit!! / Short Circuit??), and Gizmo are built.
Do not put them on the unbuilt kit list because the config's old comment still
says “only the passive is built.” Current bindings and ability implementations
supersede that delivery comment.

Gizmo currently substitutes a radius for “the next circuit Toolbox powers.”
Ownership is explicitly Open. The Lv30 capstone is deliberately deferred and
has no accepted mechanic to compile. Neither is a missing named branch.

## Exclusions and totals

- **Wayfinder and Groundskeeper:** names/archetype questions only; no accepted
  kits or branches to build. Stationmaster is superseded by Chauffeur.
- Generic `test` class, `sinkhole_lite` and `channel_ult`: validation fixtures,
  not accepted production kits.
- Stale browser wiki and superseded draft branch names are not design authority.

Across **18 documented classes with kits**, **13 entire kits are absent** from
the current production registry, **5 classes have outstanding implementation or
fidelity work** (Mole, Skeleton Crew, Lightfooted, Daredevil, Toolbox), and
Toolbox alone has all four kit slots implemented. Toolbox is included in the
five only for its explicit ownership gap; no named Toolbox ability/branch is
wholly unbuilt. These delivery counts do not promote the 13 absent kits to
settled design status.

No design contradictions were resolved by this inventory; no reconciliation
record or canonical design was changed.
