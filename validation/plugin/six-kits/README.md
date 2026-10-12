# Six-class native acceptance

An isolated Java 21 / Paper 1.21.11 server validates Hollow, Paver, Fungal Assassin,
Chauffeur, Bloodmason and Quarryman. Existing EULA acceptance is reused; the runner
refuses an existing destination server and binds loopback only. Generated worlds
stay under the selected temporary server directory. Source, compiled fixture and
validation results use separate directories.

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@21 implementation/plugin/gradlew -p implementation/plugin test jar --offline
python3 validation/plugin/six-kits/run.py \
  --runtime /path/to/existing/accepted-paper-runtime \
  --server /private/tmp/new-six-kit-server \
  --results validation/plugin/six-kits/results/new-run --port 25679
```

The fixture uses native Paper players, inventory, damage, terrain, passengers,
persistence and movement status. It additionally calls production state directly
for controlled repeated ticks, quota setups and Bedrock work; these do not replace
connected-client hold, driving or terrain feel testing. Failed exploratory runs
are retained as diagnostic history; the final accepted run is named explicitly
in `results/verification.json`.

Canonical and prototype decisions: [six-class pass](../../../docs/design/SIX_CLASS_KIT_LAB_PASS.md).
