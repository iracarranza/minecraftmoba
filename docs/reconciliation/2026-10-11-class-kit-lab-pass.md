# Class kit lab implementation reconciliation — 11 October 2026

Authority: `classes.md`; implementation notes:
[CLASS_KIT_LAB_PASS.md](../design/CLASS_KIT_LAB_PASS.md).

## Food Fight's Reckless Rationing output

The test-only ability skeleton previously declared `Recipients.NONE` for
Reckless Rationing. Canonical Chef states that its branches do not create new
operating modes; each rewards an already-correct target, and Food Fight's enemy
damage remains the base behavior. The branch's production reward does not remove
its enemy interaction. The skeleton now declares `Recipients.ENEMIES` for that
branch, matching the implementation. Super Nutritious still declares BOTH.
This reconciles a manifest error with existing canonical behavior; it does not
change the accepted class design.

## Stale delivery comments and statuses

The live config's old Toolbox comment claimed only its passive was built despite
its three actives being registered. The comment is updated to the actual shipped
kit. Mole and Lightfooted's former `channel_ult` bindings now point to their own
real ultimates. Chef is registered with real abilities and the test manifest
records built actives/dispatched passives. None of these delivery changes
promotes proposals or Working/Prototype directions to Established design.

## Explicit prototype implementation choices

- Chef servings cost nine matching actual food items per invitation; timing,
  catch geometry and penalties remain Prototype/test.
- Rationed copies remain in deterministic food selection; they cannot recurse.
- Chef uses fallback Growth until its own curve is authored.
- Supplemental fuel support is explicit and bounded; unsupported fuel remains
  vanilla-managed.
- Multiple Animal Senses sources choose the strongest matching bonus rather
  than stacking the same aura repeatedly.
- Sinkhole temporarily protects live chunks and snapshots full block state, but
  does not persist its loans across process failure. Protected terrain/liquid
  exclusions and jagged geometry are prototype choices.

Canonical Open questions remain Open. The implementation's current behavior is
recorded for repeatable lab experiments, not silently adopted as final balance.
