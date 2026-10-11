# Skeleton Crew lab pass reconciliation — 11 October 2026

Authority: `classes.md` §15 and `infrastructure.md` Supply Lines / committed
Capacity. Delivery: [Skeleton Crew lab pass](../design/SKELETON_CREW_LAB_PASS.md).

- The 8 October pursuit-only build note is retained as Historical delivery state;
  the resource half now lives in a separate workforce manager with actual consumers.
  The pure eligibility helper still has no Crew counter.
- Field Work's old manifest declared two indistinguishable DIRECTION selectors.
  The built manifest declares one projectile output, with mutually exclusive
  Strike/Raise selected at impact. This preserves canonical behavior and makes
  selection executable in the shared dispatcher.
- The previously unresolved contextual-combat seam now has an optional per-cast
  result. Direct Strike marks combat; Raise/projectile launch do not. Real impact
  damage is still observed by shared CombatState.
- Shared infrastructure observes completed carrier deliveries and owns Source /
  Destination recognition, rating and persistence. Workers only operate logistics.
  The specific defining worker must remain committed; temporary workers cannot
  establish or become permanent Capacity. Committed slots cannot also be physical.
- The source's long-span/relay rules have Open thresholds that explicitly forbid
  fabricated numbers. This build restricts recognition to lab worlds and labels it
  a single-link prototype. It does not settle a production relay or capture policy.

All resource limits, numbers, armor, water-only recovery, mounted visual, proof
sample count, owner-only lab node access and persistence lifecycle interpretations
remain Prototype/test. No conceptual design status is promoted. Ordinary owner death
is not assigned the Open universal line-disable rule, and the stale browser wiki
is not used as authority.

Integration with the newer `main` loadout/targeting pass retains both lab workflows.
Skeleton Crew rays use `AbilityTargeting`, with widths in config, and direct /
projectile Strikes use `AbilityDamage` so rearmed input mode does not consume
scripted damage as another left-click. Native validation checks both paths.
