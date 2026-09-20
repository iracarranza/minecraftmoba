# Generated optimizer artifacts

The reference-run candidate catalog is committed as:

- `candidate-catalog.json.gz`

The reference-run scenario frontier is larger and is committed as four byte-for-byte gzip segments:

- `scenario-frontier.json.gz.part-000`
- `scenario-frontier.json.gz.part-001`
- `scenario-frontier.json.gz.part-002`
- `scenario-frontier.json.gz.part-003`

Reconstruct it with:

```bash
cat scenario-frontier.json.gz.part-* > scenario-frontier.json.gz
gzip -dc scenario-frontier.json.gz > scenario-frontier.json
```

These are generated reference-run artifacts. The optimizer and realized-map export remain the authoritative reproducible source. The zero-hostiles snapshot must not be interpreted as zero underground hazard.
