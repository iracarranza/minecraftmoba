# Experimental base snapshot — 930010639

**Not a cleared playable release.** The resource_light fixture loads, but its
existing Route endpoint readback still fails and the proposed wheat task has no
wheat source. See the [fixture report](../../../implementation/worldgen/reports/nearmiss_fixture_2026-09-22/REPORT.md).

`packages/base-terrain.tar.gz` contains the unchanged harvested terrain and
seed-specific original level.dat. Its SHA-256 and all 16 file hashes are in
[package.json](../../../implementation/worldgen/reports/nearmiss_fixture_2026-09-22/package.json).
This deliberate experimental archive keeps generated worlds apart from code and
validation reports. Rebuild the authored world with the report's explicit inputs;
never apply the near-miss diff to Alpha's base.
