# P2D mountain/geology/hydrology recovery notes

Status: historical implementation evidence, not a current generator contract.

The tracked P2D world archive preserves the resulting terrain and the two text
files extracted beside this note. No P2D generator source was found in the
repository, Git history, attached solution-space bundle, or accessible project
history. The implementation details below are therefore limited to reported
results; missing algorithms and parameters have not been reconstructed.

## Preserved topology and techniques

- The correction pass preserved the basin, passes, southern valley, shoulder
  plateau, ridges, cave approaches, and overall mountain scale.
- Bounded plateau/pass operations were blended through broad irregular
  transition zones.
- Long vertical seams were replaced with stepped ledges, talus, broken slopes,
  localized cliffs, slope-following gravel/scree, and regenerated sparse
  lower-mountain vegetation.
- Fluids were rebuilt against surrounding elevation. A basin either remained
  enclosed or gained a downhill continuation such as a channel, waterfall,
  lavafall, cut, or lower pool.
- The mountain-facing authored Route remained unobstructed.

## Reported correction results

- Mountain columns reshaped: **29,481**.
- Adjacent mountain height changes over 8 blocks: **585 → 5**.
- Adjacent mountain height changes over 12 blocks: **174 → 1**.
- Mountain surface range remained approximately **Y60–109**.
- All **six** existing surface water/lava systems were rebuilt.
- Mountain lake surface columns: **316 → 374**.
- Larger lowland water feature: **223 → 246**.
- Large lava feature: **217 → 229**.
- Smaller western lava feature: **74 → 79**.
- Mountain-facing Route audit: **254 checked columns, 0 obstructed above**.

The nearby `MOUNTAIN_AUDIT.txt` is the audit embedded in the archived P2D
world. `GREYBOX_README.txt` describes the inherited P2C generation vocabulary;
its internal title was preserved exactly even though it was packaged in P2D.

## Reuse boundary

Reuse the terrain vocabulary and invariants, not P2D's exact silhouette or
resource quantities. The next generator should independently express:

- authored macro topology through irregular terrain;
- gradual foothill-to-deep-west difficulty;
- internal ridges, valleys, saddles, basins, ravines, caves, and approaches;
- connected catchments and downhill fluid continuation;
- progressive substrates and slope-aware scree;
- protected but terrain-integrated Routes.

P2D should not be modified into the next Default map.
