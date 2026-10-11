# Skeleton Crew lab pass

11 October 2026. Authority: `classes.md` §15 and shared Logistics in
`infrastructure.md`. Skeleton Crew remains conceptually settled; all quantities
and the single-link infrastructure implementation below are **Prototype/test**.
This pass supplies the resource/workforce layer, all three active slots and all
six named branches in the existing Java 21 / Paper 1.21.11 combat/skirmish labs.

## Workforce and abilities

Undead Affinity retains its extended pursuit and eligible-hostile-undead list.
An enrolled Skeleton Crew player's eligible undead kill now grants stored Crew.
Stored Crew is independent of inventory and Crew Limit. Raising consumes one;
ordinary workers are either active physical Skeletons or committed to one line.
The prototype limit is six ordinary workers, five ordinary item slots each.
Committed workers count against the limit, stored Crew and temporary workers do
not. Mobilizing withdraws Capacity without changing established Item Rate or
spending stored Crew. Ordinary workers keep real cargo while fighting and spill
it on death, with synthetic bones/equipment/XP suppressed.

- **Graveyard Shift** performs a direct held-weapon-attribute Strike with bonus
  damage and provokes active Crew; otherwise it Raises. **Field Work** launches
  one bone projectile: enemy/mob impact Strikes, solid terrain impact may Raise,
  never both. Launch/Raise is noncombat; direct Strike reports a contextual combat
  activation and sourced projectile damage enters the shared CombatState.
- **Hard Hat Zone** Strikes allied Crew for reduced damage, granting helmet,
  chestplate, leggings, boots in sequence and refreshing all granted armor even
  at full armor. Native iron armor and sunlight helmet behavior apply. The
  prototype friendly hit subtracts health directly to avoid the shared authored
  friendly-fire veto; it cannot mint gear or alter cargo capacity.
- **Labored Union** reduces only Strike's bonus. Crew automatically provoke nearby
  eligible living opposition, including cows/villagers, and their kills mint Crew
  regardless of victim undead type. Owned and allied workers inherit allegiance;
  ordinary authored attacks use the shared friendly-fire rule.
- **Burning Out** applies native fire. Any actual fire source supplies the speed
  modifier, without replacing Combat → Logistics → self-preservation priority.
  After effective duties end, workers seek nearby actual water; shade is not an
  extinguish. Empty exhausted Sources are not perpetual productive tasks.
- **Fire Drill** substitutes Alarmed: clear combat, urgently finish carried cargo
  delivery, otherwise return physically to the owner. It neither teleports nor
  ignites and does not begin a new pickup journey while Alarmed.
- **Deadline** counts consecutive burning engagement supported by shared combat
  state; disengagement resets it. Sixty ticks triggers a sourced native explosion,
  spilling real cargo and applying ordinary systemic terrain/damage rules.
- **Water Break** heals proportionally after a burning→extinguished transition in
  actual water and grants temporary speed. Water-only qualification is a prototype;
  rain/player extinguishing remains Open. Burning alone or timer expiry grants no heal.
- **A-Head of Schedule** adds a mounted Wither Skeleton Horseman and three temporary
  workers. It chooses Combat or Logistics once from ordinary workforce activity,
  waiting if idle; Logistics inherits the ordinary explicit assignment. It retains
  that mode, enhances its activity, and expires after 400 ticks. Temporary cargo
  remains real and spills at expiry, but temporary workers never enter the
  ordinary allocation ledger, infrastructure proof, or persistent Capacity.

Workers retain native collision, navigation, sunlight/fire damage, melee weapon
behavior and chunk boundaries. A head-bearing mounted Wither Skeleton is the
current visual approximation of the Headless Horseman. Human targeting, difficult
corridors, native fire melee contact, mounted cargo arrival and multiplayer feel
still need playtesting. Base Strike uses the held attack attribute; enchantment
fidelity is not separately simulated. Exact stats/geometry remain Open.

## Real cargo and shared Supply Line foundation

Outside ability mode, right-click an owned worker with real cargo to load it.
`/moba crew list` shows worker IDs, cargo slots and allocation. Target a Copper
Chest, then `/moba crew source <worker>` and `/moba crew destination <worker>`
assign a physical journey. `/moba crew follow <worker>` clears that assignment.
These commands operate the carrier; they do not create an infrastructure line.
Without an explicit Destination, carried cargo seeks the nearest valid owned
recognized node. Per-item stack limits and metadata apply; lab controls and Buffet
custody/servings cannot become transported inventory. Normal tagged real products
remain eligible. Blocked destinations retain actual worker cargo.

Recognition is deliberately limited to configured prototype lab worlds:

1. Earn the class-authored Supply Line infrastructure effect (Skeleton Crew Lv6,
   without choosing Monster Combat), enter Infrastructure Mode and right-click a
   Copper Chest Source.
2. Physically complete two full-rated-capacity ordinary-worker deliveries from
   that Source to a separate Copper Chest inventory. Partial/temporary deliveries
   cannot prove permanent rated Capacity. Keep Infrastructure Mode active.
3. Right-click Destination. Shared infrastructure recognizes a directional line
   from transport evidence; Capacity starts at zero. The worker does not author
   the line or its performance.
4. `/moba supply` lists line IDs and measured outbound/pulse timings. Use
   `/moba crew commit <worker> <line>` with an empty ordinary worker. Commitment
   removes its body and contributes its inventory slots. The specific method
   worker must remain committed for the line to flow; other committed workers
   cannot substitute for an invalidated defining entity.
5. Automated pulses move real upstream items into real destination inventory at
   the measured interarrival interval, including return/loading time. Capacity is
   the committed ledger; transient buffs never rewrite the measured Item Rate.
   Missing/unloaded endpoints pause retained recognition. Repair resumes it;
   blocked inventory returns unaccepted cargo upstream without overflow drops.
6. `/moba crew mobilize <worker>`, or a contextual Raise, withdraws commitment
   and restores that ordinary body. Destroying a worker removes its capacity.

No production span threshold, relay network, connectivity/capture model, signal
flare improvement, XP reward, universal infrastructure gate, death-disable rule,
node menu or arbitrary capacity/rate scalar is introduced. Those policies stay
Open. The implementation is a single-link lab foundation, not complete production
Supply Lines. Owner-only node selection/assignment and same-rated-slot commitment
are prototype constraints; team-relative integration remains future work.

## Persistence and lab use

`skeleton-crew.yml` stores ordinary logical worker identities, owner resource
stock, allocation, real cargo, health, armor time remaining and assignments.
`supply-lines.yml` stores recognized endpoint identities and measured method/rate.
Both use versioned atomic file replacement and preserve unresolved-world rows.
Logical ordinary workers reconstruct bodies when their chunk becomes available;
ordinary logout does not erase stock/workers/lines. Temporary shifts end at logout
or shutdown. Class change, explicit chamber leave/reset and match teardown clean
up owned workers/recognition. Fake lab body removal also cleans its workforce.
Normal shutdown/checkpoint persistence is implemented; world-inventory and sidecar
writes are not one transaction, so abrupt-crash item accounting is not proven.

The combat chamber seeds **three stored Crew explicitly for an operator session**
and provides empty Copper Chests at x0/x8, y65, z8. Reset replenishes the lab seed;
leaving clears it. Bring actual cargo and equipment via Equipment / controls.
The existing water band supports extinguishing tests. Fake Skeleton Crew bodies
receive the same three-Crew seed and a real iron sword.

`/moba lab scenario skeleton_crew_skirmish` supplies a team encounter for Raise,
Strike, Burning Out and Horseman activation. Its schema is checked by the existing
scenario tests. A launched-scoop run is separate and is not claimed by the isolated
native fixture. No generated playable map or server deployment is part of this pass.

Validation details and exact build hashes are recorded in
`validation/plugin/skeleton-crew/`. Unit tests cover allocation conservation,
method-isolated transport proof and burn/disengagement/recovery lifecycle. Native
acceptance uses actual Paper mobs, navigation, items, inventory, damage, projectile
impacts, persistence reconstruction and teardown. It does not prove a human client's
controls, a full plugin process restart, or final balance.
