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

Implementation and regression coverage are in commit `4f422ae`.
