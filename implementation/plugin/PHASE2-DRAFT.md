# Phase 2 draft — features, understanding, pseudocode

**For review before implementation.** Nothing here is built yet. Each feature
lands as its own commit behind its own `features.<name>.enabled` config flag and
its own class, so any one can be reverted or edited without touching the others.

---

## 0. My understanding of the reward vocabulary

### Universal progression — the floor (classes.md, CANON)

> Universal progression establishes the floor. Specialization creates
> differentiation above or ahead of that floor. Class/archetype progression
> establishes the exceptional ceiling.

Common progression grammar, three capacities:

| Capacity | Start | Automatic | Specialization |
| --- | ---: | ---: | ---: |
| Health | 9 | +1 | +2 |
| Hunger | 9 | +1 | +2 |
| Inventory | 6 | +3 | +6 |

Universal component reaches vanilla capability around **Level 19**.

The rule I will not break: generic Health progression does not replace Combat,
generic Hunger does not replace Exploration, generic Inventory does not replace
Logistics. Universal growth makes everyone *physically* competent; it never
supplies an archetype's systemic manipulation.

`docs/proposals/2026-09-12-capacity-curve.md` proposes different endpoints
(Health 8→18, Hunger 9→18, Inventory 6→36) and is explicitly **NOT CANON** —
the handoff says not to treat those numbers as final. I will implement the
canonical grammar and put the proposal's numbers in config as a commented
alternative, not as the default.

### Task progression — enchantment effects (classes.md, working)

| Domain | Vanilla family | Improves |
| --- | --- | --- |
| Efficiency | Efficiency | ordinary block-breaking and worldwork rate |
| Yield | Fortune + Looting | material obtained from resource-producing actions |
| Damage | Sharpness + Power | conventional melee and ranged damage |

Cadence: **L4** Efficiency I universally, **L7** Yield I universally,
**L9 / L14 / L19** choose an eligible task advancement.

Generic maximum is **Tier III**, deliberately, to leave room for class-specific
amplification — Mole's Tunneling amplifies current digging speed and must not
multiply uncontrollably with a high generic tier. Task advancement is additive
and may share a level with capacity growth.

Implementation note: these are *effects*, not literal enchantments on items.
Applying real enchantments would bind progression to the item rather than the
player and would be lost on death or item swap. I will apply them as player
state: a mining-speed modifier, a drop multiplier on qualifying drops, and a
damage modifier — with the enchantment families named in config so the mapping
stays legible and editable.

### What a level-up reward choice looks like

At a choice level the player is offered N options drawn from whichever of these
is eligible:

- a universal capacity step (Health / Hunger / Inventory specialization)
- a task advancement step (Efficiency / Yield / Damage, capped at III)

Not offered: anything infrastructure-recognizing, which stays out of Phase 2.

---

## 1. Custom inventory HUD

**Corrected 20 September 2026.** An earlier draft said a plugin cannot add HUD
elements and proposed a plain sidebar. That was true of the plugin *alone* and
misleading in context, because this project already ships a resource pack.

With a resource pack, **a bossbar title is an arbitrary drawing surface.** Map
private-use codepoints to image glyphs, use negative-space glyphs to position
them, and a title component can render icons, panels and bars — not just text.
A Hypixel Blockwars screenshot shows the technique in production: stacked
bossbars carrying a strip of player heads, a round counter, team icons and a
status banner, each sitting on a drawn background panel, with a conventional
scoreboard sidebar alongside.

### What each surface can actually do

| Surface | With plain text | With resource-pack glyphs | Cost |
| --- | --- | --- | --- |
| Boss bar | label + coloured bar | arbitrary pixels in the title strip | one bar slot each, stacks vertically |
| Action bar | transient text | icon row, drawn meters | ephemeral, one line |
| Sidebar objective | multi-line text | glyph columns, suppressed numbers | blocks other sidebar use |
| Inventory slot locking | — | — | already implemented in `Capacity` |

### The limits that remain real

- **Position is fixed.** Bossbars occupy the top-centre strip and the sidebar
  the right column. Glyphs can draw anything *within* those regions; they cannot
  move a readout to an arbitrary screen coordinate, anchor it to the hotbar, or
  overlay the inventory screen.
- **Bar slots are finite in practice.** Each stacked bossbar pushes the next
  down and eats vertical space.
- **Sidebar numbers** need 1.20.3+ blank number formatting to suppress.
- **It is a shared dependency.** The pack and the plugin must agree on the
  codepoint map, so a pack version mismatch produces tofu boxes rather than a
  degraded readout. The plugin should therefore send text that is *readable
  without* the pack wherever possible.

### Proposed, revised

Keep `features.hud.enabled` gating a plain-text sidebar and a mode bossbar as
the **baseline that works with no pack at all**, and add
`features.hud.glyphs.enabled` as a separate flag layering the drawn version on
top. That keeps the pack dependency revertible on its own and means a missing
or mismatched pack degrades to legible text instead of breaking the readout.

```
on join / on capacity change / on level change:
    if not features.hud.enabled: return
    sidebar(player).set([
        "Lv " + level + "   XP " + xp + "/" + next,
        "Health   " + curMaxHealth + "/" + capMax,
        "Hunger   " + curMaxHunger + "/" + capMax,
        "Slots    " + unlockedSlots + "/36",
        "Eff " + effTier + "  Yield " + yieldTier + "  Dmg " + dmgTier,
        pendingChoices > 0 ? "! " + pendingChoices + " reward(s) - press G" : ""
    ])

on mode enter(kind):  bossbar(player).show(kind.label, kind.colour)
on mode exit:         bossbar(player).hide()

# Layered, only when a pack is present and features.hud.glyphs.enabled:
on refresh(player):
    bossbar("status").title(
        glyph.panel()                      # drawn background
      + glyph.icon(class)                  # class sigil
      + glyph.meter("capacity", used, max) # drawn bar, not text
      + glyph.space(-4)                    # negative space for alignment
      + text(level)
    )
```

`[OPEN]` Whether the drawn version replaces the sidebar or sits above it, and
whether ability cooldowns belong in the bossbar strip or the action bar. Neither
is settled, and the glyph flag lets both be tried without disturbing the
baseline.

`[OPEN]` The codepoint map itself is resource-pack content and is not designed
here.

## 2. Advancement tree for level-up rewards

Advancements are datapack content, not plugin content, so this ships as a
datapack the plugin *reads* and *grants*. One root plus one advancement per
reward, each with `"announce_to_chat": false` and a custom frame.

```
datapack: data/moba/advancement/rewards/{root, health_1, hunger_1, ... eff_2, ...}

on rewardGranted(player, rewardId):
    advancement = "moba:rewards/" + rewardId
    player.getAdvancementProgress(advancement).awardAllCriteria()

on rewardRevoked(player, rewardId):      // for testing / respec
    revokeAllCriteria(advancement)
```

The tree is a *display* of what was chosen. The authority stays `ChoiceRecord`
in `PlayerData`; the advancement is derived, so a corrupt or missing datapack
cannot change a player's actual capacity.

`[OPEN]` Whether the tree should show unchosen branches greyed out. Vanilla
advancement visibility rules make "visible but unearned" awkward without
`hidden: false` on every node.

## 3. Genuine level-up rewards

```
config.rewards.levels:
  4:  [ { id: eff_1,   type: TASK,     domain: EFFICIENCY, tier: 1, automatic: true } ]
  7:  [ { id: yield_1, type: TASK,     domain: YIELD,      tier: 1, automatic: true } ]
  9:  [ eff_2, yield_2, dmg_1 ]           // choose one
  14: [ eff_3, yield_3, dmg_2 ]
  19: [ dmg_3, health_spec, hunger_spec, inventory_spec ]

on levelUp(player, level):
    for r in config.rewards.levels[level]:
        if r.automatic: apply(player, r)
        else:           enqueuePendingChoice(player, level, options)

function apply(player, reward):
    switch reward.type:
      CAPACITY: playerData.capacity[reward.capacity] += reward.step
                Capacity.applyNow(player)           // must be immediate
      TASK:     playerData.task[reward.domain] = min(reward.tier, config.taskTierMax)
                TaskEffects.reapply(player)

// Effects are player state, not item enchantments.
TaskEffects.reapply(player):
    EFFICIENCY -> attribute block_break_speed   *= 1 + 0.3 * tier
    DAMAGE     -> attribute attack_damage       += 1.0 * tier
    YIELD      -> flag consulted in drop handler (no vanilla attribute exists)

on BlockDropItemEvent / EntityDeathEvent:
    if yieldTier > 0 and drop is in config.yield.qualifyingDrops:
        multiply drop count by rollFortuneLike(yieldTier)
```

`[OPEN]` Exact multipliers. Canon fixes the *families* and the Tier III ceiling,
not the numbers, so every coefficient above is config with a placeholder value
and is explicitly not balance.

## 4. Infrastructure Mode via Quick Actions

Dialogs are 1.21.6+ and reachable from the Quick Actions (G) menu.

```
datapack: data/moba/dialog/infra_mode.json     tagged minecraft:quick_actions

dialog "Infrastructure" -> button "Enter Infrastructure Mode" -> /moba infra enter
                        -> button "Exit"                      -> /moba infra exit

on /moba infra enter:
    if not features.infra.enabled: refuse
    playerData.infraMode = true
    bossbar.show("Infrastructure Mode", BLUE)
    actionbar("Interact with a Banner to designate a Route endpoint")

on /moba infra exit:
    playerData.infraMode = false
    discard any partial route designation
    bossbar.hide()
```

Mode state must not survive death or disconnect in a stuck form — the existing
`deathInMode` scenario already proved that for ability mode and the same
assertion applies here.

## 5. Routes — banner endpoints and traversal

Canon: Banner A, designate; travel; Banner B, designate; system evaluates. The
endpoints are the durable identity; **traversal is evidence, not the Route**.

```
on PlayerInteractEvent(right-click block):
    if not playerData.infraMode: return
    if block is not a Banner: return
    cancel(event)                                  // designation, not banner use

    if no pending designation:
        pending = { start: block.location, path: [], startedAt: now }
        actionbar("Endpoint A set — travel to another Banner")
    else if block.location == pending.start:
        actionbar("Same Banner — travel to a different one")
    else:
        route = evaluate(pending, block.location)
        if route.valid: routes.add(route); actionbar("Route established")
        else:           actionbar("Not established: " + route.reason)
        pending = null

on PlayerMoveEvent (throttled to once per N blocks):
    if pending: pending.path.append(coarse(player.location))
                if pending.path.length > config.routes.maxSamples: invalidate

evaluate(pending, endB):
    reject if straightLineDistance < config.routes.minLength
    reject if pending.path is empty                 // must have been walked
    reject if elapsed > config.routes.maxDesignationTicks
    return Route(endA, endB, simplify(pending.path))
```

### Sprint efficiency effect

Canon is explicit: **movement efficiency before movement potency** — a Route
improves *sprint/traversal efficiency*, not raw speed.

```
every config.routes.checkTicks:
    if player is sprinting and nearRoute(player, config.routes.corridorRadius):
        reduce sprint exhaustion accumulation by config.routes.exhaustionFactor
        // i.e. the same distance costs less Hunger; speed is unchanged
```

This is the least invasive reading and the one canon names. Movement potency is
listed as a *later, more cautious* axis and I will not implement it.

`[OPEN]` bidirectionality, retained geometry, effect calculation, whether a
better demonstration upgrades an existing Route, and the exact designate input —
all unresolved in infrastructure.md and all left as config or rejected cases.

## 6. Renewable kinds, particles, and authoring

### Your list, mapped to types

| Type | Your list |
| --- | --- |
| CROP | wheat, potatoes |
| ORE-ish (finite-shaped but regenerating here) | copper, iron, gravel |
| ANIMAL | sheep, rabbit, chicken |
| SWARM | spiders, creepers, undead |

### What else the repo implies

From the economic shadow vocabulary and objectives.md §17B tiers:

- **Ore / stone:** coal, redstone, lapis, diamond, emerald, clay, sand
- **Plants:** carrots, beetroot, melon, pumpkin, sugar cane, bamboo, kelp,
  sweet berries, cocoa, mushrooms
- **Animals:** cow, pig, horse, cod, salmon, squid, turtle, bee
- **Hostiles by tier (objectives.md):** near — zombie, skeleton, spider;
  intermediate — creeper, pillager, witch, husk, stray, cave spider, slime,
  drowned; deep — ravager, charged creeper, dense pillager, guardian

Canon forbids Warden-class bosses as ordinary regenerative Swarms, and forbids
inventing the depth tables, so these ship as an **available vocabulary** with no
depth assignment.

### Legibility

```
on sourceBecameAvailable(source) / every config.renewables.particleTicks:
    if not features.renewableParticles.enabled: return
    if source.available == 0: return                  // depleted shows nothing
    spawn END_ROD particles in a ring at source.origin, radius = source.radius
    density scaled by available/capacity                // legibility, not decoration
```

End rod reads well: bright, slow, visible in daylight, and not already used by
vanilla for anything a player would confuse with a resource marker.

### Authoring, two paths

```
/moba renew spawn <type> <kind> [radius] [capacity]
    -> creates a minimal source at the player's target block and, for CROP,
       lays the minimum viable patch; for ANIMAL/SWARM, registers the volume only

/moba renew capture <type> <kind> [radius]
    -> takes the region the player is looking at, counts qualifying natural
       blocks/entities already present, and registers a source sized to what is
       actually there
```

`capture` is the canon-respecting one: it *recognizes* an opportunity that the
world already contains rather than sprinkling nodes. `spawn` exists for fixtures
and is marked as such.

## 7. Hub as a lobby with map portals

```
on gallery build (terrain harvest tooling) or on /moba hub build:
    for i, volume in volumes:
        frame = nether-portal-shaped structure at hub[i]
        sign  = volume label (classification | seed | footprint)
        on PlayerMoveEvent into frame interior:
            run harvest:visit/<volume id>          // reuse existing functions
```

Reuses the gallery's existing `visit/` functions rather than duplicating
teleport logic, so the hub is presentation only and the navigation authority
stays in one place.

## 8. Sentinel item → skeleton skull

Current sentinel is a stick. Replacing it with a skeleton skull carrying the
class blurb:

```
item: minecraft:skeleton_skull
  custom_name:  "<class display name>"
  lore:         config.classes.<id>.blurb (wrapped)
  custom_data:  { moba_sentinel: 1, moba_class: "<id>" }
  item_model / item_name as needed
  unbreakable, not stackable beyond 1
```

Identification must key on `custom_data.moba_sentinel`, **not** on the material,
so swapping the material later is a config change and cannot be spoofed by a
player holding an ordinary skull.

---

## 9. What else the repo suggests is missing

Drafted as pseudocode for review, not queued for build:

**a. Hunger-gated sprint cutoff.** classes.md fixes sprinting unavailable at
**6 Hunger or below** and builds the whole reserve model on it. Nothing
implements it.

```
every tick (throttled): if foodLevel <= config.hunger.sprintCutoff: cancel sprint
```

**b. Restricted material categories.** classes.md defines Primary Materials
(iron, gold, diamond, netherite, leather) and Construction Blocks with
restricted access. No enforcement exists.

```
on craft / on pickup / on place:
    if material in restricted and not playerData.unlocked(material): refuse + explain
```

**c. XP from legitimate resource attainment.** objectives.md: *"Legitimate
resource attainment grants XP directly."* Currently XP is a bare counter.

```
on qualifying acquisition(player, material, amount):
    grantXp(player, config.xp.values[material] * amount)
    // provenance-gated: a player-placed block re-broken must not pay again
```

That last constraint is the provenance read-ordering rule from SPEC §7.1 again,
and it is the single most reusable thing Phase 1 produced.

**d. Death and respawn policy.** Capacity, task tiers and pending choices all
need defined behaviour on death. `deathInMode` covers mode state only.

**e. Match lifecycle.** No start, end, or reset. Every system above accumulates
state with nothing to clear it between matches.

---

## Build order

1. Sentinel skull (smallest, self-contained, immediately visible)
2. Genuine level-up rewards + task effects
3. Advancement tree
4. HUD baseline (text), then the glyph layer separately
5. Renewable kinds, particles, authoring commands
6. Infrastructure Mode + Quick Actions dialog
7. Routes + sprint efficiency
8. Hub portals

Each behind `features.<name>.enabled`, defaulting **off** except where a
scenario needs it, so reverting is a config flip before it is a git revert.
