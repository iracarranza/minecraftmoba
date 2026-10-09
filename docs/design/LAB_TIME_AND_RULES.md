# Lab time and world-rule controls

**Prototype/test. Built 9 October 2026.** Shared by every bench, so a measurement is not
contaminated by the clock or by the tester's own hunger and healing.

## Commands

```
/moba lab time status
/moba lab time dawn | noon | dusk | midnight     jump to the NEXT occurrence
/moba lab time night <n>                          jump to the start of night n
/moba lab time skip <minutes>
/moba lab time pause | resume
/moba lab rules status
/moba lab rules hunger freeze | normal
/moba lab rules regen off | normal
```

Where the tester is decides what a command means. In a **launched scoop** it drives the
real match clock, so a skipped night is a real night: sunset, sunrise and Worksite
activation fire as the counter crosses them. In the **combat chamber** there is no match
clock, only a world time, so only the time-of-day words work and the rest are refused.

## Decisions

- **The clock only runs forward** (`LabTime`). Boundaries fire as they are crossed, so
  winding back would re-fire or skip them. A time-of-day request goes to the next one
  (asking for the time it already is moves a full cycle, never zero). `night n` is the one
  target that can be behind, and it is refused with the reason; relaunch the scoop to see
  it again.
- **Resume is new.** Authoring paused the match clock and nothing un-paused it (the
  chamber docs noted "Match has no resume"). `Match.resumeFromLabPause` fixes that, and
  `Match.skipTicks` advances a lab clock even while paused: skipping is deliberate,
  pausing only stops the ticker.
- **Hunger frozen** stops a food LOSS only; eating still works. **Regeneration off**
  stops natural and satiated healing, vanilla and the plugin's own `HungerRegen`, but not
  potions or ability healing. Rules are per tester and cleared when the session ends or
  they leave, so one never follows them into a real match.

## Where it appears

The terrain chamber's hotbar has a **Clock and rules** page (dusk, midnight, dawn, skip 10
minutes, pause, resume, hunger, regeneration). The combat chamber keeps time of day as a
pre-entry mode, by design, so changing it mid-session is the typed command only.

## Verification

Arithmetic and menu rules: unit-tested. Live (combat chamber, real server): time of day,
the refused skip, both rules' listeners, and clearing on leave: 64 of 64 with the
combat acceptance run. **Update, same day:** with a launchable scoop now available (`docs/design/LAB_HUB.md`) the
match-clock path ran live: `time dusk` and `time night 2` land on the real sunsets, pause holds
the clock and resume restarts it, `skip 10` advances 12000 ticks, and the terrain chamber's
Clock page jumps the clock from the hotbar.
