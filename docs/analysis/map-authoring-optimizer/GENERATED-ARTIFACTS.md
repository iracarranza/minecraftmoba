# Generated optimizer artifacts

Claude can inspect the generated artifacts directly:

- `candidate-catalog.json.gz` — compressed reference-run candidate catalog.
- `scenario-frontier.json` — full committed scenario frontier with ten finalists per strategic profile.
- `optimizer-report.md` — human-readable optimizer report.

The previously staged split frontier segments are intentionally removed; `scenario-frontier.json` is the usable artifact.

The optimizer and realized-map export remain the authoritative reproducible source. The committed frontier records its generator provenance. The zero-hostiles snapshot must never be interpreted as zero underground hazard.
