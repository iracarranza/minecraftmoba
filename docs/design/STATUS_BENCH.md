# Status bench

**Prototype/test. Built 11 October 2026.** A lab bench for the six control statuses. Stunned and
Rooted already existed and were checked only by scripted bodies; Lured and Frightened did not
exist; Blinded and Weakened were never wired. A status is only as real as someone having watched
it hold, so the bench applies each one to a subject body, or to the tester, and shows what it
does.

```
/moba lab effects                                   start (no scoop needed)
  [stunned|rooted|blinded|weakened|lured|frightened] [seconds]   select a status and a length
  status | time | apply | self | source | check | clear | reset | leave
```

Reached from the hub's Status effects pedestal (sculk, in the room), the Benches page, or the
command. The hotbar menu, the deck to the west of the arena and the command all do the same
things; Leave is in the inventory.

## The statuses

| Status | Definition (the owner's) | How it is built |
|---|---|---|
| Stunned | Cannot move or act. | Existing `Stun`. |
| Rooted | Cannot move, can otherwise act. | Existing `ToolboxStatuses` Root, now with a chosen length (`root(entity, ticks)`). |
| Blinded | Vision substantially impaired. | **Minecraft's Blindness**, level I (`statuses.blinded.amplifier`). The platform's strength is the design's. |
| Weakened | Offensive effectiveness reduced. | **Minecraft's Weakness**, level I (`statuses.weakened.amplifier`). |
| Lured | Looks at the source and moves toward it, uncontrollably. Cannot sprint, crouch or use abilities. May jump and attack. | `ForcedMovement` (new). **[PROTOTYPE]**: from the unofficial Wayfinder and Hollow classes. |
| Frightened | The same, walking away from the source (still looking at it). | `ForcedMovement`: one sign flipped in `ForcedMotion`. **[PROTOTYPE]** |

`classes.md` records Wayfinder as a name and an archetype question only and has no Hollow, so the
two forced statuses are the owner's description, not a settled design, and are marked so in the
code and in config.

## What the bench shows

Two kinds of evidence, kept apart in the `check` output:

- **Checked**: `StatusRules` holds each status's definition as data (what it refuses, what it
  allows). The check prints, for the status last applied, each capability next to what the server
  answers, and flags a mismatch. This says the plugin agrees with the definition. Capabilities
  that only a client can exercise (jump, attack, the camera) are reported as "cannot be asked
  without a client", not as agreement.
- **Measured**: what happened to the target since the status was applied: blocks moved, distance
  to the source before and after (nearer, further, unchanged), and how many degrees the camera is
  off the source. This is the evidence that the entity behaved.

## How Lured and Frightened are delivered

`ForcedMotion` is the arithmetic, pure: the step toward or away, the yaw that faces the source
(south is 0), the pitch, and a Lured entity's stop distance. `ForcedMovement` does the rest:

- every tick the entity is turned to face the source and a Lured entity stops at 1.5 blocks;
- a real player is pushed with a velocity, and any step the client reports is rewritten in
  `PlayerMoveEvent` so held keys cannot argue with it; vertical movement is left alone, which is
  what lets a Lured player jump;
- a scripted body is moved by reporting a position, as a client would, and a mob is given a path;
- a solid block stops the step (the entity may jump a one-block rise itself, it is not made to);
- sprint and crouch are cancelled when toggled and cleared each tick;
- `AbilityInputs` refuses abilities, in the same place as a Stun, so a new ability cannot forget.

Defaults, all `statuses.forced.*` in config: speed 0.2158 blocks a tick (a walk), stop distance 1.5.
A Stun or Root overrides the forced movement (movement is held), and the Lure resumes if it
outlasts them.

**One forced direction at a time**: applying Lured to a Frightened entity, or the reverse,
replaces it; the same kind again replaces its source and restarts the clock. That is an
assumption, so it can be overruled.

## Self-application

The only way to feel forced movement on a real client. Capped at 10 seconds
(`StatusArena.SELF_MAX_TICKS`) because a Stunned tester cannot use the hotbar that would clear it;
`/moba lab effects clear` always works.

## Scenarios

The skirmish harness gained a `status` act and five assertions: `status_active`,
`status_inactive`, `moved_toward`, `moved_away`, `facing_source`, plus `ability_not_executed`.
Two scenarios ship:

- `lure_and_frighten`: a Lured body closes on its source facing it, a Frightened one backs away
  facing it, a body with no status stays put (the control), a Stunned-and-Lured body does not
  move, a Lured body can attack, and the Lure ends on time.
- `lured_refuses_abilities`: a Lured Chef cannot cast Food Fight; an unaffected Chef can.

## Verified, and not

Live on Paper 1.21.11 build 132 with scripted bodies: Lured closed a 23-block gap and Frightened
opened a gap, both with the camera on the source; Blindness and Weakness are applied at level I
and cleared; every status's definition matches the server where it can be asked; both scenarios
pass; leaving removes the subject and clears every status. Java: 1,154 tests pass (1,112 before this bench).

**Not verified, and what that means:**

- **Nobody has felt it.** Forced movement on a real client is a velocity push plus a rewritten
  move event. It will rubber-band and may feel jerky. Sprint and crouch are suppressed, not taken,
  so the client may briefly believe it is sprinting. A scripted body cannot show any of this.
- **The tester's velocity path was not exercised**: the acceptance fixture's own tester body is
  not one the lab can move, so only the turn and the refusal were checked on the tester.
- **Weakened is native Weakness, which lowers MELEE damage only.** Ability damage dealt through
  `Entity#damage` is not lowered, so a Weakened caster's abilities are not weakened. That is a gap
  between "offensive effectiveness is reduced" and what the platform provides.
- **Blinded** is not tested against anything that reads vision; whether abilities or mobs should
  consult it is open.
- **[OPEN]** how the statuses combine beyond the cases above (two forced directions, Lured at a
  moving source that is itself stunned), whether Lured should end at an obstacle or path around
  it, and the exact Wayfinder and Hollow rules when those classes are designed.
