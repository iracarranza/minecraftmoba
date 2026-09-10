# Datapacks

Implementation artifacts. Build outputs (`*.zip`) are gitignored; commit the
pack folder and zip it for installation.

## moba_input_test — Prototype

Minimal input-feasibility datapack. **Not a game feature.** Its only purpose is
to test whether the proposed ability-input architecture can work in vanilla.

Target: **Minecraft Java 1.21.9+** (`pack_format` 88). Install by copying the
folder (or a zip of its contents) into `<world>/datapacks/`, then `/reload`.

### Inputs under test

| Input | Result |
| --- | --- |
| Swap Offhand (`F`) | prints `A1` |
| Sneak + Swap Offhand | prints `A2` |
| Sprint + Swap Offhand | begins Ultimate channel |
| channel held ~2s | prints `ULT CAST` |
| channel released early | prints `ULT CANCEL` |
| Quick Actions (`G`) -> Test | prints `DIALOG TEST` |

`/function moba:diag` prints a tick heartbeat and armed state.
`/trigger moba_arm` re-arms after death.

### Established by testing (2026-09-10)

- Swap Offhand can be repurposed as an ability key.
- Offhand sentinel interception preserves item state; nothing is lost or
  duplicated. Visible hand jitter appears at high tapping speed, consistent
  with two swaps landing inside one tick.
- One input resolves to exactly one ability; no double-resolution observed.
- Channel start / completion / cancellation all work.
- `flags.is_sneaking` evaluates correctly in all player states.
- Dialog -> `/trigger` -> function is a working path from the
  `minecraft:quick_actions` entry point.

### Failed by testing (2026-09-10) — Open

Reading **raw key input** via predicate does not work on 1.21.9. All four
plausible shapes failed to register, so every function referencing them failed
to load:

- `input` at the entity sub-predicate top level
- `input` under `type_specific` / `minecraft:player`
- `keys` at the entity sub-predicate top level
- `keys` under `type_specific` / `minecraft:player`

`if predicate` itself is healthy (`equipment` and `flags` predicates both load
and evaluate), so the `input`/`keys` field specifically is the failure.

Consequence: the sprint modifier currently falls back to
`flags.is_sprinting`, which is a **movement state**, not a key. The Ultimate
therefore cannot be started while stationary, rooted, or submerged. Verified
modifiers reduce to one (`is_sneaking`).

Unresolved: whether the field is absent in 1.21.9 or uses a schema not yet
tried. A reload parse error in `logs/latest.log` would settle it.

## Chunk 2 — Progression / physical capacity (v7)

Datapack-owned Level 1–30 progression. Minecraft's native XP bar and XP level
number are **output only**; all authoritative state lives in scoreboards
(`moba_xp`, `moba_lvl`, `moba_uu`/`moba_ubonus`, `moba_shp`/`moba_sfood`/
`moba_sinv`, `moba_specc`). Effective capacity is always *recalculated* from
that state by `moba:prog/recalc`, never mutated in place, so respec and
retuning cannot drift.

Wired into the existing pipeline: `load` calls `moba:prog/config`, `tick` calls
`#moba:prog` (a `required: false` tag, same convention as `#moba:detect`).
No second load/tick loop, no second player-init path.

### Test controls (`/trigger`, extending the Chunk 1 convention)

| Control | Effect |
| --- | --- |
| `/trigger moba_prog` | print full progression state + live `max_health` |
| `/trigger moba_xpadd set 50` | award 50 prototype MOBA XP |
| `/trigger moba_lvlup` | award exactly enough XP to cross the next level |
| `/trigger moba_setlvl set 17` | jump to a level (clamped 1–30, XP reset) |
| `/trigger moba_reset` | reset progression to Level 1 |
| `/trigger moba_spec set 1` | claim a Health specialization (+2 HP) |
| `/trigger moba_spec set 2` | claim a Hunger specialization (+2 food) |
| `/trigger moba_spec set 3` | claim an Inventory specialization (+6 slots) |
| `/trigger moba_respec` | clear all specialization selections |
| `/trigger moba_growth set 3` | grant N extra universal growth units (testing Lv18+) |

### Temporary XP behaviour

Flat **100 XP per level**, one line in `moba:prog/config`. Deliberately *not*
vanilla's escalating formula — that formula is used only to convert our
fraction into a bar fill. Excess XP carries into the next level; multi-level
awards recurse. At Level 30 XP is pinned at the threshold (bar reads full) and
further XP is discarded.

Bar fill = `moba_xp / 100 * req(level)`, clamped to `req(level) - 1` so vanilla
can never auto-level the display out from under us.

### Implemented capacity values

Level 1: **9 HP / 9 food / 6 inventory slots**. Universal growth unit:
+1 HP / +1 food / +3 slots. Specialization (levels 3 / 18 / 24): +2 HP *or*
+2 food *or* +6 slots.

**Universal capacity vs specialization.** These are separate ceilings:

- Universal Health growth stops at 20.
- Universal Hunger growth stops at 20.
- Universal Inventory growth stops at 36.

Specialization is **additive above those universal floors**:

- Health specialization *may* raise maximum Health above 20 (up to 26).
- Hunger specialization *may* raise maximum Hunger above 20 (up to 26).
- Inventory specialization *cannot* raise Inventory above 36 - the only true
  hard cap on effective capacity.

`moba:prog/recalc` therefore clamps `moba_uhp`/`moba_ufood`/`moba_uinv` (the
universal floors) and clamps the *effective* value only for Inventory.
Verified specialization outcomes at Lv24:

| Choices | Health | Hunger | Inventory |
| --- | --- | --- | --- |
| none | 20 | 20 | 36 |
| Lv18 Hunger | 20 | 22 | 36 |
| Lv18 + Lv24 Hunger | 20 | 24 | 36 |
| Lv18 + Lv24 Health | 24 | 20 | 36 |
| one Health, one Hunger | 22 | 22 | 36 |
| Lv3 + Lv18 + Lv24 one axis | 26 | 26 | 36 (capped) |

Universal growth breakpoints live in `moba:prog/uni_units` — one line per
level, edit that file alone to retune.

### Breakpoint structure (design reference, Lv1-30)

| Level | Progression event |
| ---: | --- |
| 1 | Ability 1; begin at 9 Health, 9 Hunger, 6 inventory slots |
| 2 | Ability 2; universal capacity growth |
| 3 | Capacity specialization I |
| 4 | Efficiency I; universal capacity growth |
| 5 | Ability 1 upgrade |
| 6 | Infrastructure specialization / recognition entry |
| 7 | Yield I; universal capacity growth |
| 8 | Passive scaling; universal capacity growth |
| 9 | Task specialization; universal capacity growth |
| 10 | Ability 2 upgrade |
| 12 | Infrastructure role-expression upgrade |
| 14 | Task advancement; universal capacity growth |
| 15 | Ultimate unlock |
| 16 | Passive scaling; current working mobility breakpoint |
| 17 | Universal Health and Hunger floors reach vanilla capacity |
| 18 | Capacity specialization II |
| 19 | Task advancement |
| 20 | Advanced class-authored reward I |
| 24 | Capacity specialization III; passive scaling |
| 25 | Advanced class-authored reward II |
| 30 | Capstone, exact form unresolved |

Levels not listed may still grant universal capacity growth under the
universal growth rules. Only the capacity rows of this table are implemented
in this chunk; abilities, Efficiency, Yield, Tasks, Infrastructure, mobility
and the capstone are out of scope.

Cross-checked against the implemented schedule: the 6 levels that state
universal growth outright (2, 4, 7, 8, 9, 14) plus L17 are all present;
L11 and L13 are unlisted levels covered by the growth rule, and L15/L16 are
listed levels whose growth is not spelled out in the event column. The
specialization milestones (3 / 18 / 24) match `moba:prog/spec_avail`.

### Universal growth schedule (complete through Lv17)

Universal growth occurs on ordinary growth levels **and** stacks with certain
breakpoint rewards. Growth levels: **2, 4, 7, 8, 9, 11, 13, 14, 15, 16, 17**
— 11 units. Verified to reproduce the design table row-for-row:

| Lv | H | Hunger | Inv | | Lv | H | Hunger | Inv |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 9 | 9 | 6 | | 10 | 14 | 14 | 21 |
| 2 | 10 | 10 | 9 | | 11 | 15 | 15 | 24 |
| 3 | 10 | 10 | 9 | | 12 | 15 | 15 | 24 |
| 4 | 11 | 11 | 12 | | 13 | 16 | 16 | 27 |
| 5 | 11 | 11 | 12 | | 14 | 17 | 17 | 30 |
| 6 | 11 | 11 | 12 | | 15 | 18 | 18 | 33 |
| 7 | 12 | 12 | 15 | | 16 | 19 | 19 | 36 |
| 8 | 13 | 13 | 18 | | 17 | 20 | 20 | 36 |
| 9 | 14 | 14 | 21 | | | | | |

Health/Hunger reach the vanilla baseline of 20 at Lv17 (`9 + 11`). Inventory
hard-caps at 36 on Lv16 (`6 + 10 x 3`); Lv17's nominal +3 is absorbed by the
cap. Lv18–30 growth is not yet defined and is intentionally absent from
`moba:prog/uni_units`.

### Known limitations

- **Hunger cap is an enforcement workaround, not a native maximum.** Vanilla
  exposes no max-hunger attribute and `/data modify` is refused on players, so
  `foodLevel` cannot be written. `moba:prog/food` reads it (reads are allowed)
  and, while it exceeds the cap, applies `hunger 1 255`. Consequences: food can
  sit above cap for up to ~1s after eating; saturation is drained to zero at
  the cap, so there is no saturation buffer; a brief hunger effect icon shows.
  `moba_hclamp` ensures we only ever clear the effect we applied ourselves.
  Caps above 20 are unrepresentable and are clamped to 20.
- **Inventory locking is eviction, not a locked slot.** Locked slots stay
  visually present; `moba:prog/inv/sweep` drops their contents on the ground
  each tick. The stack is copied to a ground item and the copy is verified
  (`store success`) *before* the slot is cleared, so items are never deleted.
  A locked slot therefore cannot be used as storage, but items do land at the
  player's feet rather than being refused on pickup.
- **Extra Hunger rows are BLOCKED in vanilla (design intent recorded).**
  Intended presentation: surplus Hunger displays as an extra row above the
  hunger bar, mirroring how Health above 20 wraps into extra heart rows.
  Health gets this natively - `max_health` above 20 makes the vanilla client
  stack heart rows with no work from us, already visible with two Health
  choices. Hunger has no equivalent: the client's food renderer draws exactly
  10 icons from a `foodLevel` hard-capped at 20. A datapack cannot draw HUD
  elements, and a resource pack can only retexture those 10 icons, not add a
  row - so the symmetry with Health is not reachable in vanilla. Realistic
  paths, both deferred: a fake row drawn with custom font glyphs in the action
  bar (resource pack, excluded from this chunk), or a client mod for a true
  extra row. Note this is TWO problems, not one - see the surplus-pool item
  below.
- **Effective Hunger above 20 has no HUD representation yet.** Vanilla's
  foodLevel holds at most 20, so `moba:prog/food` enforces `moba_fenf`
  (= `min(effective, 20)`), not the effective value. Above 20 the clamp simply
  never fires. The design value is preserved in `moba_capfood` and the surplus
  is reported by `/trigger moba_prog`. Deciding how to surface it (extended
  bar, separate readout, other mechanism) is an OPEN question deferred out of
  this chunk - it is a presentation gap, not a capacity cap.
- **Hunger surplus needs a mechanic, not just a display.** `foodLevel` cannot
  hold more than 20, so even with an extra row drawn, capacity above 20 does
  nothing until a datapack-owned surplus pool refills `foodLevel` as it drains.
  That is a mechanic change and is deliberately not built here; it wants its
  own feasibility chunk. `moba_capfood` / `moba_fenf` already keep the design
  value and the enforceable value separate, so that work needs no changes to
  progression logic.
- Offhand and armor are outside the capacity model — the offhand holds the
  Chunk 1 input sentinel.
- `max_health` is set via attribute `base set`. Anything else writing that
  attribute base would be overwritten on the next recalc.
- Lowering a cap relies on vanilla clamping current health, and on the hunger
  clamp / inventory sweep converging over the following ticks.

### Persistence

Scoreboards persist across `/reload`, and `moba_init` gates first-time setup,
so a reload re-derives caps for initialized players without resetting
progression. New players initialize to Level 1 on their first tick. Use
`/trigger moba_reset` for a deliberate reset.

### Structural note

`tick.mcfunction` calls `#moba:modifiers`, `#moba:detect` and `#moba:ultchan`
as function tags whose entries are `required: false`. A child that fails to
parse leaves its tag empty instead of preventing `tick` from loading. Keep new
or unverified syntax behind such a tag — a parse error anywhere in a function
stops that whole function from loading.
