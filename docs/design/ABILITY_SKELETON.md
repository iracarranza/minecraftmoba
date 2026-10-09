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
| **Affects** | **who the output acts on** | the ability | `Recipients` |

Only the third is a preference, and it never changes what is cast.

## Who an output reaches is its own question

`Recipients` — `NONE`, `ENEMIES`, `ALLIES`, `BOTH` — added 8 October at the
combat chamber's request, because nothing else answers it:

- **Not `TargetForm`.** Crash Landing is `SELF` — it ends the caster's own
  movement and aims at nothing — and damages every enemy near the impact.
- **Not `combat`.** Deathly Clutches declares `combat: true` and touches nobody
  but the caster, because combat-ness is about acting on a combatant's
  capacity to fight, which includes mitigating your own.

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
