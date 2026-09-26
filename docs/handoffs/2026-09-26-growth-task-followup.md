# Follow-up — Growth task persistence verification

**26 September 2026.** Follow-up to
[`2026-09-26-growth-task-slice.md`](2026-09-26-growth-task-slice.md).

## Verification

The plugin was built with the installed Java 21 runtime:

```
JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH ./gradlew test
```

Result: **BUILD SUCCESSFUL; 320 tests completed.**

The first run exposed one fixture interaction with the intentional Lv0
enrolment change: `AbilityInputsTest` created a default Lv0 player while
testing Lunge, which is unlocked at Lv1. The fixture now explicitly uses
`data.level = 1`; production behaviour and the Lv0 rule are unchanged.

## Persistence follow-up

`PlayerDataCodec.decode` now accepts the configured `TaskLedger` and projects
the persisted `ChoiceRecord` list into the runtime `PlayerData.task` map.
This prevents TaskEffects tiers from disappearing after a restart. The
canonical Slaying tree is translated to the legacy persisted `DAMAGE` key so
existing saves remain compatible; a persisted-key migration can be handled
later without silently breaking old data.

Implementation and regression coverage are in commit `22da434`.

## Defect found while reviewing the menu path — FIXED

`projectTask` runs **only** inside `PlayerDataCodec.decode`. `Rewards.click`
appends the `ChoiceRecord` and calls `plugin.applyAndSave(p)`, which syncs
capacity and writes the save but never refreshes `PlayerData.task`.

A Task allocation is therefore persisted correctly and has **no effect until the
player reconnects**: `TaskEffects.tier()` keeps reading the pre-allocation value
for the rest of the session. Capacity choices do not have this problem, because
`sync` recomputes capacity from `choices` directly.

**Fixed in `applyAndSave`** rather than in the reward menu, so allocation and
reload share one path and any future writer of `choices` is covered without
knowing it has to be.

`TaskProjectionTest` covers what is unit-testable: an allocation is visible
without reconnecting, projection is idempotent — `applyAndSave` runs many times
per session — Slaying lands on the legacy `DAMAGE` key, and a reload agrees with
the live projection.

[UNVERIFIED] The change was written in the web session, which still cannot run
Gradle. The projection logic was compiled and exercised standalone against a
stub of `TaskEffects.Domain`; the three-line `MobaPlugin` edit was not compiled.
**Re-run `./gradlew test`.**

This is item 5 of the original handoff, and it is the reason that item existed:
no test covers the Bukkit menu path, so the gap between "persisted" and
"in effect" was invisible to the 320-test run. The manual walkthrough is still
worth doing, to confirm the bossbar, the menu and the HUD's Eff/Yld/Dmg line all
move at Lv4.
