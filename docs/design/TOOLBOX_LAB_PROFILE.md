# The Toolbox lab profile — scope

**Status:** Working. Scoped, not built.
**Purpose:** run and iterate on Toolbox in isolation, without a match, a map
pool, a draft, or objectives — while keeping ability mode and every level-up
consequence exactly as they behave in a real match.

---

## The finding that sets the scope

The question was posed as "a separate plugin with only the necessary pieces".
Reading `MobaPlugin.onEnable()` (997 LOC file, ~130-line enable) says that is
the wrong shape, for a reason worth recording:

> **Almost everything in `onEnable` is inert until commanded.**

`Match`, `MapPool`, `MapConfigurations`, `Renewables`, `Lair`,
`LairLifecycle`, `DraftHallView`, `DraftingColosseum`, `TeamObjectives`,
`Worksites`, `ApplyBench`, `TestBed` are all *constructed* and registered as
listeners, and then do nothing at all until `moba match open` or a sibling
command reaches them. Constructing them costs a few objects and some event
registrations.

So there is nothing to strip. Exactly **two** calls do eager work:

| Call | What it does |
|---|---|
| `mapConfigurations.reload()` | reads `maps/*.json.gz` off disk |
| `lobbyWorld.ensure()` | **creates or loads a world**, builds the hall in it |

…plus five repeating schedulers (selection/combat sweep at 1t, passives,
hunger enforcement, the offhand-enrollment check, and `WorkPoints`).

A fork would therefore have extracted ~1,600–1,900 LOC across ~14 files to
avoid two method calls, and would have inherited `LockedSlots`,
`InventoryGuard` and `VitalsScaling` — the three files Toolbox most depends on
and the three most likely to keep changing. **Fork rejected.** The profile is
a config gate in the same jar.

---

## What the gate actually is

One key, `features.toolboxLab.enabled`, default **false**.

### 1. Suppress the two eager calls

```
if (!lab()) { mapConfigurations.reload(); lobbyWorld.ensure(); }
```

`mapConfigurations.reload()` is suppressed because the lab has no maps and a
missing `maps/` directory should not be a warning every start.
`lobbyWorld.ensure()` is suppressed because the lab supplies its own world.

Everything else in `onEnable` is left **exactly as it is**. This is the whole
point of the finding above: not touching it is what keeps the lab honest, and
a restructured enable is a second thing that can diverge from the real one.

### 2. Stand up a lab world

Reuse `LobbyWorld`'s own recipe rather than writing a second one —
`VoidGenerator`, `World.Environment.NORMAL`, `WorldType.NORMAL` (FLAT makes
Paper log `No key layers in MapLike[{}]` at every start),
`generateStructures(false)`.

World rules, which are *not* the lobby's:

| Rule | Lobby | Lab | Why |
|---|---|---|---|
| Difficulty | PEACEFUL | **NORMAL** | Toolbox's passive procs on mob and environmental damage |
| `DO_MOB_SPAWNING` | false | **false** | spawn mobs deliberately, not ambiently |
| `DO_DAYLIGHT_CYCLE` | — | **false**, noon | daylight sensors are a circuit component; a drifting sun makes every trial non-reproducible |
| `DO_TILE_DROPS` / `DO_ENTITY_DROPS` | — | **false** | a trap that flattens terrain should not bury the floor in items |
| `KEEP_INVENTORY` | — | **true** | a circuit *is* the inventory; dying must not scatter it |
| `MOB_GRIEFING` | — | **true** | TNT block destruction is a designed Ultimate outcome |

A platform, not a hall: flat, large enough to build and to be launched across
by a piston bank. Dimensions are a **declared fixture** — start at 64×64 and
expect to change it the first time something gets launched off the edge.

### 3. Relax two admin gates, in the lab only

- `moba setlevel` already exists and already validates against
  `settings.maxLevel()`. **Keep it as is.** It is the level-up consequence
  path under test; weakening it would test something else.
- `moba setclass` already exists. Keep.
- The **offhand-enrollment pause** loop drops a player from enrollment when
  their offhand is occupied. Toolbox's kit will put things in hands
  constantly. This is the one live-behaviour difference worth allowing, and it
  should be a *separate* key so it is never on by accident, not folded into
  `toolboxLab`.

---

## What the lab does NOT change

Stated explicitly, because each is a temptation and each would invalidate the
test:

- **`LockedSlots` / `InventoryGuard`** stay fully live. Toolbox's every
  designed breakpoint (6 → 18 → 24 → 36) is a claim about slot counts, and
  the tripwire-parity problem exists *only* because row A begins at slot 9. A
  lab with all 36 slots open tests a class we did not design.
- **`Capacity` / `Vitals` / `VitalsScaling`** stay live. Toolbox is the
  squishiest class on the roster; without the health curve the machine's
  risk profile is wrong.
- **`CombatState` / `AbilityCombat`** stay live. Utility Belt's cooldown is
  combat-gated.
- **The three clocks and the firewall.** Growth still does not touch
  abilities. The lab is not a place to try a shortcut around that.

---

## What has to be built, which is the real work

None of this exists. It is new regardless of where it lives, and it dwarfs the
plumbing above.

1. **The circuit reader** — walk the inventory from slot 9, resolve the
   component dictionary, apply the link rules (dust, repeater, comparator).
2. **The 5-tick absolute grid** — every component seeks the next multiple of
   5; tripwire hooks and daylight sensors fire their triggered component one
   tick after.
3. **Parity** — repeaters and doubled dust shifting the sequence, and the
   row-boundary case where flow cannot continue because there is no dust at
   the start of the next row.
4. **The abilities** — Utility Belt (14/12/10/8/6), A2 on 3 charges / 8s
   recharge (designed in `TOOLBOX_CIRCUIT_GRAMMAR.md` §10D, not coded),
   Short Circuit.
5. **The Ultimate** — 67s, 20 component activations, 6s Root ceiling, off the
   5-tick grid but concurrent where possible.

`docs/design/TOOLBOX_CIRCUIT_GRAMMAR.md` is the specification for 1–3 and
§7A/§7B for 5.

---

## Verification

- Both suites green (`402` plugin, `591` worldgen at time of writing).
- **A test that the gate is default-off**, in the shipped `config.yml`. A lab
  profile that ships on is a lab profile that silently disables the lobby.
- **A test that the lab does not bypass `InventoryGuard`.** This is the
  invariant most likely to be quietly relaxed later for convenience, and the
  one whose loss would be hardest to notice.
- Standing check, manual: `moba setclass toolbox` → `moba setlevel 6` →
  `moba setlevel 24`, confirm unlocked slot count moves 6 → 24 and the locked
  markers repaint.

---

## Open

- **Lab platform size** — declared fixture, 64×64, untested.
- **Does the lab need a second player?** Root, knockback and arrow geometry all
  need a target. An armour stand is not a player for Root. Probably wants a
  dummy-spawn command; not scoped here.
- **Toolbox's Constructs I–IV + capstone** (Lv6/12/21/24/30) still have
  breakpoints with no content, so the Growth half of "level-up consequences"
  is only partly testable.
