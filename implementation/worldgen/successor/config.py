"""Explicit prototype parameters; none are final balance constants."""

CELL_SIZE = 5
X_MIN, X_MAX = -420, 420
Z_MIN, Z_MAX = -520, 520
NX = (X_MAX - X_MIN) // CELL_SIZE + 1
NZ = (Z_MAX - Z_MIN) // CELL_SIZE + 1
SEA_LEVEL = 64
NORTH_HOME = (0, -365)
SOUTH_HOME = (0, 365)
HOME_RADIUS = 55

TOPOLOGIES = (
    "twin_massif", "split_spine", "horseshoe", "broken_peaks",
    "long_ridge", "highland_ravines", "offset_basin", "sawtooth_valleys",
)

SCALE_MODEL = {
    "status": "prototype/test",
    "name": "nonuniform_homeland_depth_v1",
    "macro_linear_hypothesis": 1.25,
    "analytical_bounds_blocks": {"x": [X_MIN, X_MAX], "z": [Z_MIN, Z_MAX]},
    "cell_size_blocks": CELL_SIZE,
    "feature_scale": 1.0,
    "near_home_spacing": [1.0, 1.1],
    "local_to_regional_spacing": [1.2, 1.3],
    "deep_wilderness_spacing": [1.3, 1.4],
    "mountain_internal_depth": 1.34,
    "depth_multiplier_curve": [
        {"normalized_homeland_depth": 0.0, "spacing_multiplier": 1.0},
        {"normalized_homeland_depth": 0.2, "spacing_multiplier": 1.08},
        {"normalized_homeland_depth": 0.55, "spacing_multiplier": 1.25},
        {"normalized_homeland_depth": 1.0, "spacing_multiplier": 1.36}
    ],
    "implementation": "The curve allocates generation/placement space by homeland depth; it does not rescale inherited coordinates.",
    "allocation_note": (
        "Added area is assigned to connective Wilderness, Route divergence, forest "
        "and mountain interiors, opportunity separation, and empty territory; it is "
        "not a uniform coordinate transform."
    ),
}

# These are intentionally labeled experimental rather than hard contracts.
EXPERIMENTAL_THRESHOLDS = {
    "mountain_depth_blocks_min": 125,
    "mountain_commitment_gain_y_min": 8,
    "forest_interior_depth_blocks_min": 20,
    "empty_connective_fraction_min": 0.22,
    "deep_off_route_fraction_min": 0.08,
    "packed_hotspot_max": 9,
    "mean_absolute_bias_max": 0.24,
    "severe_opportunity_bias_max": 0.58,
    "corrections_max": 5,
    "hydrology_downhill_fraction_min": 0.72,
}
