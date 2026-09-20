# Minecraft MOBA — Plugin Phase 1 Spec

Contract-level specification. Method bodies, Gradle/`plugin.yml` boilerplate, and
Paper API idioms are deliberately omitted — implement those normally.

**What this document is for:** it extracts the ~40 design decisions from a
~2,000-line manuscript that Phase 1 actually depends on, so the design docs do
not need to be read. Where a decision is unsettled it says `[OPEN]` and defines
a seam, not an answer. **Do not invent content for `[OPEN]` items.**

---

## 0. Scope

**In:** player data, progression spine, capacity + inventory slot locking,
ability input, block provenance logging, level-up reward GUI *framework*,
three test abilities.

**Out (do not build):** Supply Lines, Constructs, Development Zones, Routes,
Structural Integrity, objectives, Worksites, day/night, XP *sources*, all
classes except the three test abilities, and the minimap renderer (§11).

**Decide before first commit:** target Minecraft version. Worldgen work is
pinned to 1.21.11, the input prototype ran on 1.21.9. Pick one and record it.

**Stack:** Paper API (not Spigot — needs its events and async chunk APIs),
Java 21, `PersistentDataContainer` for persistence.

---

## 1. Data model

```
PlayerData                        // authoritative; persisted in Player PDC
    uuid
    classId          : string     // "mole" | "gardener" | ... | null
    level            : int        // 1..30
    xp               : int        // toward next level
    choices          : List<ChoiceRecord>   // every level-up decision ever made
    modeState        : transient  // never persisted; holds only a flag and an
                                  // expiry tick. NO saved item state — Phase 1
                                  // never moves an item (see §5).

ChoiceRecord
    level            : int
    choiceId         : string     // opaque; defined by config, not by code

DerivedCapacity                   // NEVER stored; always recomputed
    maxHealth        : double
    effectiveHunger  : int
    unlockedSlots    : int        // 6..36
```

**Invariant — recomputation, not mutation.** `DerivedCapacity` is a pure
function of `(level, choices, config)`. Never incrementally mutate it. This is
what makes reset and respec safe, and is carried over from the datapack
prototype.

```
function recomputeCapacity(data, config) -> DerivedCapacity
    // fold config growth units and choices from level 1 to data.level
    // apply caps
    // return fresh value
```

---

## 2. Config — everything numeric is data-driven

Numbers in the manuscript are `[PROPOSED]` or `[PROTOTYPE]`, not canon. A
capacity-curve rework is under review. **Hardcode nothing.**

```yaml
progression:
  maxLevel: 30
  xpPerLevel: 100          # flat placeholder; XP bands are downstream of
                           # unresolved systems. Do not implement bands.
capacity:
  health:   { start: 9, growthUnit: 1, cap: 20 }
  hunger:   { start: 9, growthUnit: 1, cap: 20 }
  slots:    { start: 6, growthUnit: 3, cap: 36 }
  growthLevels: [2,4,7,8,9,11,13,14,15,16,17]
  # [OPEN] specialization values and levels — see section 7

rewards:                   # [OPEN] — see section 7. Ship EMPTY.
  levels: {}

abilities:
  bindings:
    mode: SWAP_HAND
    a1:   LEFT_CLICK
    a2:   RIGHT_CLICK
    ult:  DROP
  modeTimeoutTicks: 50
```

---

## 3. Progression spine

XP **sources** are out of scope — attribution and anti-farming are downstream of
block provenance and the unresolved day/night economy. Award by command only.

```
command /moba xp <player> <amount>
command /moba setlevel <player> <level>
command /moba setclass <player> <classId>
command /moba reset <player>

on xpGained(player, amount):
    data.xp += amount
    while data.xp >= xpRequired(data.level) and data.level < maxLevel:
        data.xp -= xpRequired(data.level)
        data.level += 1
        onLevelUp(player, data.level)
    syncVanillaXpBar(player, data)      // native bar is the display surface
    applyCapacity(player)
```

`syncVanillaXpBar` sets the vanilla level number and bar fill to mirror MOBA
progression. The native bar is the intended display; do not build a custom one.

---

## 4. Capacity and inventory slot locking

**Highest-risk system in Phase 1.** The datapack implementation evicted items
from locked slots to the ground; that must not be reproduced.

```
function lockedSlots(player) -> Set<int>
    unlocked = recomputeCapacity(...).unlockedSlots
    // hotbar 0..8 fill first, then main inventory 9..35
    return { i for i in 0..35 if i >= unlocked }

function applyCapacity(player):
    player.maxHealthAttribute = derived.maxHealth
    enforceHungerCap(player, derived.effectiveHunger)   // see note
    // slot locking is enforced by events, NOT by a scan
```

**Enforcement is preventive. Cancel, never relocate.**

```
on InventoryClickEvent(e):
    if e.slot in lockedSlots(e.player): cancel(e)

on InventoryDragEvent(e):
    if any(s in lockedSlots) for s in e.rawSlots: cancel(e)

on EntityPickupItemEvent(e):
    if no unlocked slot can accept e.item: cancel(e)

on InventoryMoveItemEvent(e):          // hoppers, droppers
    if destination is a locked player slot: cancel(e)
```

Optionally place a visual marker item in locked slots for legibility — but the
cancellation is the mechanism; the marker is decoration and must never be the
thing that blocks the slot.

**Hunger note.** Effective Hunger above 20 is unimplementable as extra bar rows
on any platform (client-side). Phase 1 stores the surplus and applies the cap
only. Do not implement the exhaustion-multiplier conversion; it is `[OPEN]` and
its formula is more precise than the mechanism can deliver.

---

## 5. Ability input

**Nothing is ever moved.** No sentinel, no hand exchange, no stashing, no
restoration. Mode is a flag. This removes the entire item-safety risk class.

```
offhand: FILLED_MAP — permanent, locked, non-droppable, non-movable
         Renders persistently in the screen corner (vanilla behaviour).
         It is NOT a "class item" and plays no part in ability input.
         Its renderer is Phase 2 (§11); Phase 1 ships a stub.
```

```
F       → cancel; toggle mode            (nothing moves)
M1      → cancel; A1
M2      → cancel; A2
Q       → cancel; Ultimate
F again → exit mode
timeout → exit mode, silent, no cost
```

```
on PlayerSwapHandItemsEvent(e):
    cancel(e)                                  // always — the offhand map never leaves
    toggleMode(e.player)

function enterMode(p):
    mode.active   = true
    mode.expiresAt = now + config.modeTimeoutTicks
    showAbilityBar(p)
    playSound(p, ENTER)

function exitMode(p, reason):
    mode.active = false
    clearAbilityBar(p)
    playSound(p, EXIT)
```

**Click capture — two handlers per button.** Clicking an *entity* does not raise
`PlayerInteractEvent`. Missing this shows up as "abilities don't fire when
aiming at someone," i.e. exactly when they matter.

```
on PlayerInteractEvent(e):                     // air and block
    if not inMode(e.player): return
    cancel(e)
    if e.action is LEFT_*:  fireAbility(e.player, A1)
    if e.action is RIGHT_*: fireAbility(e.player, A2)

on EntityDamageByEntityEvent(e):               // LEFT click on entity
    if damager is player and inMode(damager):
        cancel(e); fireAbility(damager, A1)

on PlayerInteractEntityEvent(e):               // RIGHT click on entity
    if inMode(e.player): cancel(e); fireAbility(e.player, A2)
```

**Q requires a packet listener — this is a hard requirement, not an edge case.**
`PlayerDropItemEvent` only fires when the mainhand holds something, so an
empty-handed player (Level 1, or any respawn) could not use their ultimate.
Read the drop *action* at packet level, which arrives regardless of what is
held.

```
on packet ServerboundPlayerAction(DROP_ITEM | DROP_ALL_ITEMS):
    if inMode(player): cancel; fireAbility(player, ULT)

on PlayerDropItemEvent(e):                     // Bukkit-level safety net
    if inMode(e.player):        cancel(e)
    if isOffhandMap(e.item):    cancel(e)      // never droppable
```

**VERIFY FIRST.** Confirm the client sends the drop action with an empty hand
before building on it. If it does not, move the ultimate off Q — do **not**
reintroduce item movement to make Q detectable.

```
on tick:
    for p in playersInMode where now > mode.expiresAt: exitMode(p, TIMEOUT)

on PlayerDeathEvent / PlayerQuitEvent:
    clear mode flag                            // nothing to restore
```

Consuming clicks while in mode is **intended** — spending a swing is a legible
cost. Do not add an exception.

**Mode feedback** is the only mode indicator, since no item appears in hand:

```
showAbilityBar(p):
    actionbar: "M1 <a1name>   M2 <a2name>   Q <ultname>"
               // grey entries whose cooldown is active
```

**[OPEN] Kitfighter.** Offhander places a real weapon in the offhand, which
would displace the map — so Kitfighter trades persistent map awareness for
their A1. That may be a good class identity, but it is a design decision and is
not settled here.

## 6. Ability registry and test abilities

```
interface Ability:
    id, displayName, cooldownTicks
    execute(player, context)

registry: Map<classId, {a1, a2, ult}>
```

Three test abilities, chosen for **mechanism coverage**, not class coverage.

```
Lunge            // proves the input path end to end
    execute: player.velocity += look * config.power

SinkholeLite     // proves provenance is load-bearing in play
    execute:
        region = sphere(raytraceGround(player), config.radius)
        for block in region:
            if isPlayerPlaced(block): SKIP      // section 7 — the point of it
            else: schedule staged removal
    // MUST visibly spare player-built structures

ChannelUlt       // proves the hold/cancel path
    execute: begin channel of config.channelTicks
             abort if player moves beyond threshold or mode exits
```

---

## 7. Block provenance

**The Phase 1 go/no-go.** Five documented systems depend on distinguishing
player-placed from world-generated blocks; vanilla records nothing. This test
decides whether they are viable.

```
on BlockPlaceEvent(e):
    markPlaced(e.block)

on BlockBreakEvent(e):
    unmarkPlaced(e.block)                      // reclaim; keeps storage bounded

function markPlaced(block):
    chunk.pdc[KEY_PLACED] |= bit(localIndex(block))   // bitset per chunk

function isPlayerPlaced(block) -> bool
```

**Instrument it.** The deliverable is the measurement, not the feature:
memory per chunk, tick cost of place/break at building speed, total growth over
a full session of ordinary play, and whether reclamation on break actually
bounds it.

`[OPEN]` Blocks placed then covered, moved by pistons, or destroyed by
explosions. Record behaviour; do not invent policy.

### 7.1 Read ordering — a constraint on every dependent system

**Reclamation destroys the answer.** `unmarkPlaced` runs inside Provenance's own
`BlockBreakEvent` handler at `MONITOR`, and Provenance is registered first in
`onEnable`. Any consumer that also listens at `MONITOR` therefore asks
`isPlayerPlaced` *after* the mark is gone, and every player-placed block reads
as world-generated.

```
on BlockBreakEvent(e):
    priority HIGHEST   -> consumers read isPlayerPlaced(e.block)   // mark intact
    priority MONITOR   -> Provenance.unmarkPlaced(e.block)         // mark cleared
```

**Rule.** A system that needs block provenance *during a break* must listen at a
priority earlier than `MONITOR` — `HIGHEST` is the conventional choice, with
`ignoreCancelled = true` so a cancelled break is not counted. Reading at
`MONITOR` alongside Provenance is a silent wrong answer, not an error.

This is not local to one feature. All five dependent systems inherit it, and the
failure is invisible at both call sites because the ordering lives in
registration order in `MobaPlugin.onEnable`, not in either handler.

Found live: the Regenerative Sources seam counted a player's own farm as a wild
patch because it read at `MONITOR`. See `validation/plugin/renewables-status.md`.
A break handler that only ever sees world-generated blocks will pass every test
written with world-generated blocks, so a dependent system needs at least one
case that breaks a *player-placed* block and asserts the negative.

`[OPEN]` Whether reclamation should be deferred by a tick, or moved behind an
explicit "consumers have read" phase, so ordering stops being load-bearing.

---

## 8. Deliberately left open — provide the seam, not the content

**Level-up rewards.** The framework only.

```
on onLevelUp(player, level):
    rewards = config.rewards.levels[level]     // ships EMPTY
    if rewards is empty:
        toast(player, "Level " + level); return
    enqueuePendingChoice(player, level, rewards)
    marker(player)                             // persistent unspent count

on openRewardGui(player):                      // live chest GUI
    render pending choice as N buttons from config
    on click: record ChoiceRecord
              applyCapacity(player)            // must be immediate
              advance queue or close
```

Choice *content* is unresolved: the capacity curve is `[PROPOSED — NOT CANON]`
and only the Hunger cadence is settled. **Ship with an empty reward table** and
one hand-authored example in a separate test config to exercise the GUI.

**Ability branch trees.** Not in Phase 1.

```
Ability                    // flat for now
    // [OPEN] branches: each ability gains three mutually exclusive upgrades.
    // Model as Ability.variants later; do NOT design the tree here.
```

---

## 9. Invariants — these are the acceptance bar

1. **No item is ever moved.** Ability mode relocates nothing. Slot locking
   cancels rather than relocates. No code path drops a player item to the
   ground, stashes one, or relies on restoring one.
2. **Capacity is always recomputed** from `(level, choices, config)`. No
   incremental mutation anywhere.
3. **The offhand map cannot leave the player** — not droppable, not movable,
   not storable. Re-issued if ever missing.
4. **One input resolves exactly one ability.** No double-fire.
5. **Provenance storage is bounded** by reclamation on break.
6. **No numeric value is hardcoded.** Every table is config.
7. **Nothing in section 8 is invented.**

---

## 10. Acceptance tests

| # | Test | Pass |
|---|---|---|
| 1 | Fill inventory at L1 (6 slots), pick up items, shift-click, hopper-feed | Nothing enters locked slots; nothing drops |
| 2 | Level to 2 (9 slots) mid-session | New slots usable immediately, no relog |
| 3 | `/moba reset` after many choices | Capacity returns exactly to L1 baseline |
| 4 | F → M1 / M2 / Q, each 50 times | Correct ability every time; no double-fire |
| 4b | F → Q **with an empty mainhand** | Ultimate fires (packet path) |
| 5 | F, wait past timeout | Mode exits silently; no ability consumed; nothing moved |
| 6 | Die in mode; log out in mode | Mode clears; map still in offhand; inventory untouched |
| 7 | Try to drop / move / store the offhand map | Impossible by every path |
| 8 | Aim at a mob, F → M1 and F → M2 | Abilities fire; no attack, no interact |
| 9 | Build a structure, then SinkholeLite over it | Player blocks survive; natural terrain collapses |
| 10 | Build for 30 min, measure provenance store | Bounded; cost documented |

Test 9 is the one that decides the platform. Test 1 is the one most likely to
reveal a wrong implementation.

---

## 11. Phase 2 note — minimap renderer

Not in Phase 1, but it determines the item type now, so the offhand map must be
a `FILLED_MAP` from the start; retrofitting means touching every path that
touches the offhand.

A custom `MapRenderer` gives a **128×128 per-player pixel canvas** — the only
arbitrary graphics surface available on a vanilla client. Held in the offhand it
renders continuously in the screen corner, so a genuinely persistent minimap is
possible: terrain, ally and objective markers, fog of war, all computed
server-side and private per player.

Phase 1 ships a stub renderer drawing a placeholder.

Limits: 128×128, Minecraft's map palette rather than RGB, and real redraw cost —
throttle updates and redraw only dirty regions.

This supersedes the earlier plan to build a minimap from font glyphs in stacked
bossbars, which existed only because a datapack cannot render. Do not build that.
