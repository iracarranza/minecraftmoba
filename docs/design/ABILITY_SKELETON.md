# Prototype/test: the ability skeleton

Eighteen classes are designed in `classes.md`. Six appear in `config.yml` and
nine abilities are implemented. This records what every class's abilities
declare, so that building one is filling in a slot rather than inventing the
slot.

## The four axes

`classes.md` settled three of these on 28 September 2026 and named the fourth
as deliberately outside them.

| Axis | Question | Owner | In code |
|---|---|---|---|
| Target form | what is aimed at | the ability | `TargetForm` |
| Input form | how the gesture is made | the ability | `InputForm` |
| Cast mode | whether the aim is verified first | the **player** | `CastMode` |
| Charges | how many activations bank | the ability | `Ability.charges()` |
| Affects | who else the output acts on | the ability | `Recipients` |
| Caster | whether it also lands on you | the ability | `AbilityOutput.affectsCaster` |

Only the third is a preference, and it never changes what is cast.

## What is aimed at: block or creature

`TargetForm` split on 9 October. `UNIT` covered both — its own doc said *"Wax
this block. Heal this teammate."* — while `AbilityContext` had always carried
the clicked block and the interacted entity as separate, mutually exclusive
fields. The code distinguished them and the words did not.

| | Aims at | Previews for free |
|---|---|---|
| `SELF` | nothing | — |
| `UNIT_BLOCK` | one block | yes |
| `UNIT_ENTITY` | one creature | no |
| `AREA_BLOCKS` | a set of blocks | yes |
| `AREA_ENTITIES` | a set of creatures | **no** |
| `DIRECTION` | a facing | — |
| `VECTOR` | a start and an end | not supported |

`AREA_ENTITIES` is the half that mattered most. `AREA` promised *"this is
exactly what `Ability.preview` returns, which means an area ability gets a
targeting preview for free"* — and `preview` returns blocks. Crash Landing
damaging everyone near the impact, or Irresistible Buffet seating every enemy
the table rolls through, would have inherited a guarantee that is false for
them, and a cast mode would have offered a preview with nothing in it.

The split also fixed a live defect in the combat chamber: `CombatBehavior`
asked `AREA || DIRECTION` for "can this be aimed at a marked point", refusing
unit targets with *"a unit target is a creature, not a point."* A block is a
unit target and **is** a point. It now asks `TargetForm.pointable()`.

## Who an output reaches is its own question

`Recipients` — `NONE`, `ENEMIES`, `ALLIES`, `BOTH` — added 8 October at the
combat chamber's request, because nothing else answers it:

- **Not `TargetForm`.** Crash Landing is `SELF` — it ends the caster's own
  movement and aims at nothing — and damages every enemy near the impact.
- **Not `combat`.** Deathly Clutches declares `combat: true` and touches nobody
  but the caster, because combat-ness is about acting on a combatant's
  capacity to fight, which includes mitigating your own.

### And the caster is asked separately

`Recipients.NONE` used to be documented as *"the caster, or nobody"* — two
different facts in one value. Tunneling lands on no one; Deathly Clutches drops
its caster to near-death. A chamber measuring self-damage could not tell them
apart.

Crash Landing is the case an enum could not have held at all: it damages nearby
enemies **and** takes fixed fall damage itself. A value per combination does
not scale, so `affectsCaster` is a separate boolean and `NONE` now means
"nobody *else*".

Locomotion is not an effect landing on you: Runway launches its caster and is
`false`; Crash Landing ends the same movement and takes damage for it, and is
`true`.

Of the nine built abilities: four `NONE`, five `ENEMIES`, **no `ALLIES`** — a
test asserts that last one, so the first ally-facing ability is a deliberate
edit rather than a quiet one.

Chef is the roster's first ally-facing kit, and it is ally-facing **on a
branch**: Food Fight damages enemies and Super Nutritious heals allies it hits.
Nothing built reaches allies yet, so the ally side of any chamber is still
untested by anything that exists.

### A branch is its own declaration, not a delta

A branch carries the **whole triple** — target, input and affects — because any
of the three can differ. `classes.md` records that a branch may change an
ability's input form, so a branch holding only the field that happens to differ
today would quietly become wrong the first time another one moves.

There is no base-plus-override to resolve. The ability's row is what it does
unbranched; a branch row is what *that branch* does, and they are simply allowed
to differ:

```yaml
a1:
  id: "runway"
  target: "SELF"   input: "INSTANT"   affects: "NONE"
  branches:
    pop_rocket: { target: "SELF", input: "INSTANT", affects: "NONE" }
    trampoline: { target: "SELF", input: "INSTANT", affects: "NONE" }
    suplex:     { target: "SELF", input: "INSTANT", affects: "ENEMIES" }
```

**Built abilities list every branch**, so a reader needs no union and no second
file. Two branches may be identical — three branches that happen to reach the
same people is a fact about the kit, and suppressing it would make "the same"
indistinguishable from "nobody checked". Crash Landing is that case: all three
trade self-damage against impact damage, and all three hit enemies.

Chef's Food Fight is the opposite, and the clearest argument for the shape —
three branches, three different answers:

| Branch | Affects | |
|---|---|---|
| Super Nutritious | `BOTH` | heals allies it hits |
| Extra Spicy | `ENEMIES` | sets them alight |
| Reckless Rationing | `NONE` | leaves a raw copy on a cooking station |

`Ability.outputs(player, context)` remains the runtime authority and must agree
with what the manifest says.

## The manifest holds a SET of outputs

Every slot carries an `outputs` map, even when it holds one, so there is a
single shape to read rather than two. The key is the output's id; for a
single-output ability that is the ability's own id, which is what the Java
passes to `AbilityOutput.single`.

```yaml
a1:
  id: "graveyard_shift"
  outputs:
    strike: { target: "UNIT_ENTITY", input: "?", affects: "ENEMIES", caster: "false" }
    raise:  { target: "SELF",        input: "?", affects: "NONE",    caster: "false" }
  branches:
    field_work:
      outputs:
        strike: { target: "DIRECTION", input: "?", affects: "ENEMIES", caster: "false" }
        raise:  { target: "DIRECTION", input: "?", affects: "NONE",    caster: "false" }
```

### The selector is the gesture OR what was aimed at

`AbilityOutput.of` used to require **distinct input forms**, on the reading that
a tap and a hold are what choose between outputs. The roster disagrees:

- **Graveyard Shift** strikes a targeted enemy and *otherwise* raises a Crew
  Member.
- **Flip and Press** flips a targeted enemy, *or* advances a targeted cooking
  station.

One gesture, two outputs, chosen by **what the activation found**. That is
already how the ability layer works — `AbilityContext` carries the clicked block
and the interacted entity separately, documented as "an ability reads whichever
its form expects and refuses cleanly when neither is there." Selection by target
was in the code before it was in the vocabulary.

So what must be distinct is the **pair**. Two outputs sharing both a target form
and an input form have nothing to tell them apart, and that is now the only case
refused.

### A branch declares every output

Not only the ones it changes. Skeleton Crew's **Field Work** is why: it turns
Graveyard Shift into a projectile, moving *both* outputs from `UNIT_ENTITY` and
`SELF` to `DIRECTION`. A branch recording only what differs would have to say
which output it meant.

## The unit is an OUTPUT, not an ability

A tap that stabs and a hold that throws are two outputs of one ability, with
different forms, and a single pair on the ability cannot describe that. So
`Ability.outputs()` returns a list of `AbilityOutput(id, target, input)`, and
where there is more than one the **input form is the selector** — which is why
`AbilityOutput.of` refuses two outputs that share one.

Most abilities have exactly one output and the distinction never shows.

## What was dead, and is now wired

`TargetForm` and `InputForm` were written, tested against each other, and
**referenced by no production code at all**. The vocabulary existed; nothing
spoke it. `Ability` now declares outputs, and `holdDependent` is *derived* from
`InputForm.CHARGED` rather than declared a second time — so an ability can no
longer state a form and then contradict it.

`Ability.outputs()` defaults to **empty, meaning undeclared** — not to a
guessed single output. `SELF` is how an ability says it targets nothing; an
empty list is how it says nothing at all, and `AbilitySkeletonTest` names every
one.

## The designed roster, filled in 9 October

Every designed ability's target form was read off `classes.md` rather than
inferred from its name. Across all 72 slots:

| Form | Count |
|---|---|
| `SELF` | 51 |
| `DIRECTION` | 18 |
| `AREA_ENTITIES` | 6 |
| `AREA_BLOCKS` | 5 |
| `UNIT_BLOCK` | 5 |
| `UNIT_ENTITY` | 5 |
| `?` | 0 |

Every output declares all four fields. Nothing in the manifest is `?`.

**All four new values are used**, which is the check that the split was real
and not a tidy-up: `AREA_ENTITIES` alone carries Irresistible Buffet, Eternity
Mountain, the Racing Line, Talisman of Undying, Retinue and Burning Out — six
abilities that would have had to claim a free block preview they cannot give.

### What this surfaced

- **Fungal Assassin's Creeping Colony is the roster's only enemy-facing
  passive** — attacking applies Fungal Growth. A test asserted every passive
  was `NONE`; that test would have forced this one to lie, and now pins the
  count at exactly one instead.
- **Looming Talismaniac's Talisman of Undying is the first `ALLIES` output**,
  and Chef's Super Nutritious the only `BOTH`. Nothing else in 72 slots reaches
  an ally. A test names both, so a third arrives deliberately.
- **Paver's A1 and A2 are both marked `**Passive.**` in `classes.md`** — Rebar
  Chain and Chamfering are properties of placed concrete, not castable
  abilities. Recorded as `PASSIVE` input because that is what the design says,
  but a class whose A1 and A2 are passives has no active kit below its
  Ultimate, which is worth a look. [OPEN]
- **Chauffeur's Racing Line is vector-shaped and needs no vector targeting.**
  It draws a line from Chauffeur to their Parking Spot — a start and an end,
  but both already fixed, so there is no second designation to make. It is
  `AREA_ENTITIES` over the corridor. Worth knowing before `VECTOR` gets built
  for something that did not need it.

### Input forms: a press unless the design says hold

Filled 9 October. `INSTANT` is **not** a default applied for want of evidence —
`classes.md` records the preference directly:

> prefer press to a deterministic state sequence over hold and release, unless
> continuous charging is indispensable

and spells it out where it could be misread, as in Stalking Pounce: *"There is
no hold-and-release input ... it cannot be charged indefinitely."* So an ability
described without hold language is one that fires on a press, and recording
`INSTANT` reports that rather than guessing.

**Three outputs are `CHANNELED`**, all on explicit "hold to channel" wording:
Bloodmason's Hemorrhage and Teratoma, and Quarryman's Eternity Mountain.

**Nothing is `CHARGED`.** That means "fires once at whatever a held aim
reached" — a beam that widens while aimed. Bloodmason's channels are not that;
they repeat an effect every half second held. A test pins it, because
`CastMode.effectiveFor` and `InputForm.resolve` both exist to handle `CHARGED`
and **neither has ever run against a real ability**. The first charged ability
makes both live at once.

### An Ultimate can re-express an ability

Bloodmason's Anatomb turns Capillary Flow into **Hemorrhage** and Viscerwall
into **Teratoma**, both held channels. They are recorded as outputs of *their
own abilities*, not of the Ultimate, because that is where the gesture lands.

That makes four multi-output abilities, of two kinds:

| | Selected by |
|---|---|
| Graveyard Shift, Flip and Press | what the cast **found** — an enemy or terrain |
| Capillary Flow, Viscerwall | whether the **Anatomb is up** |

The second is a third selector, after gesture and target. It needs no contract
change — the `(target, input)` pair differs from the base either way — but it is
worth naming, because an ability whose outputs depend on a *match state* is not
something `Ability.outputs(player, context)` currently has a way to ask about.
[OPEN]

### The last six, closed 9 October

All of them were answered in the classes' **developed sections**, below the kit
blocks I had read:

| | | Found in |
|---|---|---|
| `animate` | `AREA_BLOCKS` | "Target terrain and consume blocks from the area" |
| `assemble` | `UNIT_BLOCK` | "construct a wall at the targeted location" |
| `wither_golem` | `SELF` | "Create a temporary Wither Golem" — no aim, as with A-Head of Schedule |
| `talisman_core` | `UNIT_BLOCK` | the banner, whose supporting block and contiguous region become cursed |
| `wax_on` | two outputs | below |
| `viscerwall` | `UNIT_BLOCK` | the weakest of the six — see below |

**Wax-On turned out to be two outputs**: *"Interactive **blocks** that
accumulate enough Wax become Sealed"* and *"Applying the first Wax to damaged,
previously unwaxed **equipment** also restores some durability."* A block and a
piece of gear are different targets, not one effect described twice, so it
declares `seal` (`UNIT_BLOCK`) and `restore` (`SELF`).

[OPEN] Its Sticky branch mentions *"Attacking Waxed enemy armor"*, which implies
a path by which an enemy's equipment becomes waxed. Nothing says what that path
is, so no third output is declared for it.

**Assemble is the roster's third ally-facing output.** The wall mitigates for
the team behind it and slows enemies shot through it, and `classes.md` records
its three branches as landing combat "by mitigation, crowd control and buffing
respectively" — two of those face allies. `BOTH`.

**Viscerwall rests on a pairing, not a statement.** Nothing in its section says
what it aims at. Capillary Flow — its pair, on the same Hemostasis axis — says
"Targets the block Bloodmason is looking at", and Viscerwall's mature form is
described as "concave toward Bloodmason", which only means anything if it is
placed away from him. `UNIT_BLOCK` on that reading. It is the one of the six I
would most expect to be corrected.

### Where a designated block expands into many

`UNIT_BLOCK` rather than `AREA_BLOCKS` whenever the **aim** designates one
block and the ability grows from it — Assemble's wall, Capillary Flow's
construction, Hotswap's web. `AREA_BLOCKS` is for an aim that selects a set:
Animate consuming terrain, Wax-Off splashing an area, Sinkhole destabilising a
region.

Both preview as blocks, so nothing is lost either way; the distinction is what
the player points at.

## The manifest holds a SET of outputs

Every slot carries an `outputs` map, even when it holds one, so there is a
single shape to read rather than two. The key is the output's id; for a
single-output ability that is the ability's own id, which is what the Java
passes to `AbilityOutput.single`.

```yaml
a1:
  id: "graveyard_shift"
  outputs:
    strike: { target: "UNIT_ENTITY", input: "?", affects: "ENEMIES", caster: "false" }
    raise:  { target: "SELF",        input: "?", affects: "NONE",    caster: "false" }
  branches:
    field_work:
      outputs:
        strike: { target: "DIRECTION", input: "?", affects: "ENEMIES", caster: "false" }
        raise:  { target: "DIRECTION", input: "?", affects: "NONE",    caster: "false" }
```

### The selector is the gesture OR what was aimed at

`AbilityOutput.of` used to require **distinct input forms**, on the reading that
a tap and a hold are what choose between outputs. The roster disagrees:

- **Graveyard Shift** strikes a targeted enemy and *otherwise* raises a Crew
  Member.
- **Flip and Press** flips a targeted enemy, *or* advances a targeted cooking
  station.

One gesture, two outputs, chosen by **what the activation found**. That is
already how the ability layer works — `AbilityContext` carries the clicked block
and the interacted entity separately, documented as "an ability reads whichever
its form expects and refuses cleanly when neither is there." Selection by target
was in the code before it was in the vocabulary.

So what must be distinct is the **pair**. Two outputs sharing both a target form
and an input form have nothing to tell them apart, and that is now the only case
refused.

### A branch declares every output

Not only the ones it changes. Skeleton Crew's **Field Work** is why: it turns
Graveyard Shift into a projectile, moving *both* outputs from `UNIT_ENTITY` and
`SELF` to `DIRECTION`. A branch recording only what differs would have to say
which output it meant.

## The unit is an OUTPUT, not an ability

A tap that stabs and a hold that throws are two outputs of one ability, with
different forms, and a single pair on the ability cannot describe that. So
`Ability.outputs()` returns a list of `AbilityOutput(id, target, input)`, and
where there is more than one the **input form is the selector** — which is why
`AbilityOutput.of` refuses two outputs that share one.

Most abilities have exactly one output and the distinction never shows.

## What was dead, and is now wired

`TargetForm` and `InputForm` were written, tested against each other, and
**referenced by no production code at all**. The vocabulary existed; nothing
spoke it. `Ability` now declares outputs, and `holdDependent` is *derived* from
`InputForm.CHARGED` rather than declared a second time — so an ability can no
longer state a form and then contradict it.

`Ability.outputs()` defaults to **empty, meaning undeclared** — not to a
guessed single output. `SELF` is how an ability says it targets nothing; an
empty list is how it says nothing at all, and `AbilitySkeletonTest` names every
one.

## The designed roster, filled in 9 October

Every designed ability's target form was read off `classes.md` rather than
inferred from its name. Across all 72 slots:

| Form | Count |
|---|---|
| `SELF` | 51 |
| `DIRECTION` | 18 |
| `AREA_ENTITIES` | 6 |
| `AREA_BLOCKS` | 5 |
| `UNIT_BLOCK` | 5 |
| `UNIT_ENTITY` | 5 |
| `?` | 0 |

Every output declares all four fields. Nothing in the manifest is `?`.

**All four new values are used**, which is the check that the split was real
and not a tidy-up: `AREA_ENTITIES` alone carries Irresistible Buffet, Eternity
Mountain, the Racing Line, Talisman of Undying, Retinue and Burning Out — six
abilities that would have had to claim a free block preview they cannot give.

### What this surfaced

- **Fungal Assassin's Creeping Colony is the roster's only enemy-facing
  passive** — attacking applies Fungal Growth. A test asserted every passive
  was `NONE`; that test would have forced this one to lie, and now pins the
  count at exactly one instead.
- **Looming Talismaniac's Talisman of Undying is the first `ALLIES` output**,
  and Chef's Super Nutritious the only `BOTH`. Nothing else in 72 slots reaches
  an ally. A test names both, so a third arrives deliberately.
- **Paver's A1 and A2 are both marked `**Passive.**` in `classes.md`** — Rebar
  Chain and Chamfering are properties of placed concrete, not castable
  abilities. Recorded as `PASSIVE` input because that is what the design says,
  but a class whose A1 and A2 are passives has no active kit below its
  Ultimate, which is worth a look. [OPEN]
- **Chauffeur's Racing Line is vector-shaped and needs no vector targeting.**
  It draws a line from Chauffeur to their Parking Spot — a start and an end,
  but both already fixed, so there is no second designation to make. It is
  `AREA_ENTITIES` over the corridor. Worth knowing before `VECTOR` gets built
  for something that did not need it.

### Input forms: a press unless the design says hold

Filled 9 October. `INSTANT` is **not** a default applied for want of evidence —
`classes.md` records the preference directly:

> prefer press to a deterministic state sequence over hold and release, unless
> continuous charging is indispensable

and spells it out where it could be misread, as in Stalking Pounce: *"There is
no hold-and-release input ... it cannot be charged indefinitely."* So an ability
described without hold language is one that fires on a press, and recording
`INSTANT` reports that rather than guessing.

**Three outputs are `CHANNELED`**, all on explicit "hold to channel" wording:
Bloodmason's Hemorrhage and Teratoma, and Quarryman's Eternity Mountain.

**Nothing is `CHARGED`.** That means "fires once at whatever a held aim
reached" — a beam that widens while aimed. Bloodmason's channels are not that;
they repeat an effect every half second held. A test pins it, because
`CastMode.effectiveFor` and `InputForm.resolve` both exist to handle `CHARGED`
and **neither has ever run against a real ability**. The first charged ability
makes both live at once.

### An Ultimate can re-express an ability

Bloodmason's Anatomb turns Capillary Flow into **Hemorrhage** and Viscerwall
into **Teratoma**, both held channels. They are recorded as outputs of *their
own abilities*, not of the Ultimate, because that is where the gesture lands.

That makes four multi-output abilities, of two kinds:

| | Selected by |
|---|---|
| Graveyard Shift, Flip and Press | what the cast **found** — an enemy or terrain |
| Capillary Flow, Viscerwall | whether the **Anatomb is up** |

The second is a third selector, after gesture and target. It needs no contract
change — the `(target, input)` pair differs from the base either way — but it is
worth naming, because an ability whose outputs depend on a *match state* is not
something `Ability.outputs(player, context)` currently has a way to ask about.
[OPEN]

### The six still open

`animate`, `assemble`, `wither_golem` (Golem Master never says what any of the
three aims at), `wax_on` ("apply Wax to a target" — block or creature is not
stated), `viscerwall`, and Looming Talismaniac's unnamed A2. Left `?` rather
than guessed.

Both multi-output abilities now carry both outputs — see below.

## The manifest is not config

`implementation/plugin/src/test/resources/ability-skeleton.yml` holds all 18
classes × 4 slots. It is under **test** resources on purpose. `config.yml`
already recorded the decision this would otherwise break:

> An unbound input does nothing, which is honest; a placeholder would be a lie
> that looked like a feature.

Stubbing sixty unbuilt abilities into the live config would also make
`AbilityCombat` demand a `combat` answer for each — a design decision forced by
a scaffolding exercise. The manifest gives every class a named slot and a
stable id without giving any class an ability a player could press.

### Status, per ability

| | |
|---|---|
| `built` | an `Ability` implementation exists and declares its outputs |
| `dispatched` | a passive with a live listener; no `Ability`, so no outputs |
| `declared` | `config.yml` names a `passiveHook` that **nothing dispatches** |
| `designed` | `classes.md` has the kit; nothing is implemented |

`declared` exists because **three of four hooks were dead on 8 October 2026**,
and `PassiveHookDispatchTest` now holds the list -- which is empty:

| Hook | Class | Was | Now |
|---|---|---|---|
| `mole_digging` | Mole | no passive in game, and misnamed | renamed `sifth_sense` and dispatched |
| `undead_affinity` | Skeleton Crew | no passive in game | pursuit half dispatched; Crew half deliberately unbuilt |
| `utility_belt` | Toolbox | passive ran, via a hardcoded `classId == "toolbox"`; hook inert | dispatched by hook |

Toolbox was the one that made the others hard to see: a config key describing a
mechanism it did not use, sitting beside two that described a mechanism nobody
had implemented. From the config they looked alike.

Dispatching the belt by hook has one deliberate consequence — it now honours
`passives.enabled` like every other passive. A switch labelled "passives" that
left the biggest one running would not be a switch.

The list stays, empty and checked. The test fails if a new dead hook appears,
and fails just as loudly if a closed one is left in it — so it cannot go stale
in either direction.

### `?` is a real value

A form is `?` wherever `classes.md` has not answered. Inferring one from an
ability's *name* is how a guess gets laundered into a decision, so unanswered
cells stay unanswered — and `?` is legal only while the status is `designed`.
Anything `built` has answered by existing.

The one exception needs no reading: a **passive** has no gesture, so it permits
no cast mode. That is a different statement from "every mode applies", which is
what leaving it blank would imply.

## What the skeleton exposed

- **No shipped ability overrides `preview()`.** Cast modes, `AimState`, the
  grace windows and `TargetPreview` are all built, and every ability is
  effectively Quick, because an ability opts into Hold and Double by returning
  blocks and none does.
- **Every built ability is `INSTANT`.** Nothing is `CHANNELED` or `CHARGED`, so
  `InputForm.resolve` and `CastMode.effectiveFor` — two spellings of the same
  decision — have never both been exercised against a real ability.
- **Nine of 72 slots are built**, across four classes: Toolbox is whole, Mole
  and Daredevil have A1/A2/ult, Lightfooted has A2.

## [OPEN] Which spelling of the Quick-to-Hold upgrade survives

`CastMode.effectiveFor(boolean)` and `InputForm.resolve(CastMode)` decide the
same thing. `effectiveFor` predates the forms and takes the boolean the forms
replace. They agree today only because nothing is `CHARGED`; the first charged
ability is when that stops being free.
