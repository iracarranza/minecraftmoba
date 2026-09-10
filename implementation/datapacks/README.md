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

### Structural note

`tick.mcfunction` calls `#moba:modifiers`, `#moba:detect` and `#moba:ultchan`
as function tags whose entries are `required: false`. A child that fails to
parse leaves its tag empty instead of preventing `tick` from loading. Keep new
or unverified syntax behind such a tag — a parse error anywhere in a function
stops that whole function from loading.
