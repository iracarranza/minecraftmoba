"""A Route must be walkable, and 'walkable' is a number rather than an opinion.

The frozen map's Routes fail on measurement: 40.8% of adjacent columns differ by
a block and 7.6% differ by two or more, which cannot be climbed. These pin the
corrected profile against the four terrain cases the brief names -- slope, rough
ground, forest and water -- using the same statistic, so a regression fails a
build instead of being discovered by walking into it.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from terrain_harvest.routes import (MAX_STEP, treatment, walkable_profile)


def steps(profile):
    return [abs(b - a) for a, b in zip(profile, profile[1:])]


# --- the four terrain cases -------------------------------------------------

def gentle_slope(n=120):
    return [64 + i // 3 for i in range(n)]


def rough_ground(n=120):
    # ±1 noise on flat ground: the case that produced a jump every 2.5 columns
    return [64 + (i * 7 % 3) - 1 for i in range(n)]


def forest_floor(n=120):
    # mostly flat with occasional roots and hollows
    bumps = {17: 2, 18: 3, 40: -2, 41: -3, 77: 2, 95: -4}
    return [64 + bumps.get(i, 0) for i in range(n)]


def water_crossing(n=120):
    # a shore, a channel eight blocks lower, and the far shore
    return [64 if i < 50 or i > 70 else 56 for i in range(n)]


CASES = {'slope': gentle_slope(), 'rough': rough_ground(),
         'forest': forest_floor(), 'water': water_crossing()}


def test_no_step_is_ever_unclimbable():
    # The hard failure. A 2-block step cannot be climbed at all, and the frozen
    # map has 675 of them.
    for name, raw in CASES.items():
        profile = walkable_profile(raw)
        worst = max(steps(profile), default=0)
        assert worst <= MAX_STEP, f'{name}: step of {worst} blocks'


def test_rough_ground_stops_being_parkour():
    # Terrain noise must not become jumps. This is the headline failure, and it
    # is NOT solved by limiting step size: a one-block step is climbable but
    # still a jump, so bounding alone would hand back a jump every column.
    raw = rough_ground()
    before = sum(1 for s in steps(raw) if s >= 1) / len(steps(raw))
    after = sum(1 for s in steps(walkable_profile(raw)) if s >= 1) / len(steps(raw))
    assert before > 0.4, 'the fixture should be as rough as the real map'
    # Zero is deliberately not the target. The brief is explicit that an
    # occasional obvious Minecraft-scale step belongs on a natural trail and
    # that the failure condition is REPEATED interruption, so this asks for a
    # jump to be rare rather than absent.
    assert after < 0.05, f'{after:.1%} of steps still require a jump'


def test_a_slope_is_still_a_slope():
    # Not "make Routes flatter": a Route may rise and fall, and a profile that
    # flattened this would be solving the wrong problem.
    profile = walkable_profile(gentle_slope())
    assert profile[-1] - profile[0] >= 35, 'the climb was flattened away'
    assert max(steps(profile)) <= MAX_STEP


def test_jumps_are_rare_not_merely_small():
    # The acceptance measure from the audit, applied to every case: steps must
    # be both climbable AND infrequent.
    for name, raw in CASES.items():
        st = steps(walkable_profile(raw))
        share = sum(1 for s in st if s >= 1) / len(st)
        assert share <= 0.35, f'{name}: {share:.0%} of columns need a jump'


def test_the_profile_tracks_terrain_rather_than_averaging_it():
    # It should follow shape, not converge on a mean height.
    raw = gentle_slope()
    profile = walkable_profile(raw)
    drift = [abs(p - r) for p, r in zip(profile, raw)]
    assert max(drift) <= 1, f'profile wandered {max(drift)} blocks from the ground'


def test_treatment_comes_from_deviation():
    # Worn, assimilated and constructed fall out of a number already computed
    # rather than being imposed as categories.
    assert treatment(0) == 'worn'
    assert treatment(1) == 'assimilated'
    assert treatment(-2) == 'assimilated'
    assert treatment(3) == 'constructed'
    assert treatment(-9) == 'constructed'


def test_a_water_channel_is_constructed_and_the_shores_are_not():
    # Construction should be geographically earned: the channel needs a bridge,
    # the shores do not.
    raw = water_crossing()
    profile = walkable_profile(raw)
    kinds = [treatment(p - r) for p, r in zip(profile, raw)]
    assert kinds[10] == 'worn', 'the near shore needed no work'
    assert kinds[110] == 'worn', 'the far shore needed no work'
    assert 'constructed' in kinds[50:71], 'the channel was not recognised as a crossing'


def test_forest_hollows_are_assimilated_not_rebuilt():
    raw = forest_floor()
    profile = walkable_profile(raw)
    kinds = [treatment(p - r) for p, r in zip(profile, raw)]
    assert kinds.count('worn') > len(kinds) * 0.8, 'most of a forest floor is already walkable'
    assert 'assimilated' in kinds, 'small roots and hollows should be made up locally'


def test_unknown_columns_do_not_poison_the_profile():
    raw = [64, 64, None, None, 65, 66]
    profile = walkable_profile(raw)
    assert all(p is not None for p in profile)
    assert max(steps(profile)) <= MAX_STEP
