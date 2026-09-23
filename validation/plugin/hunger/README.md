# Hunger authority acceptance test

Runs current gameplay code unchanged in a **new disposable Alpha server**, with
real Mineflayer sprint/jump and item-use packets. This is independent of the
930010639 fixture. Do not install the acceptance plugin on the live server.

Requires Java 21, Python with PyYAML 6.0.3, Node, the repository's locked protocol
dependencies, a local Paper 1.21.11 runtime, Alpha map diffs, and pristine Alpha
base terrain. The runtime is read/copied only. Port 25640 must be free.

```sh
export JAVA_HOME='/path/to/java21'
implementation/plugin/gradlew -p implementation/plugin jar test :acceptance-fixture:build
python3 -m venv /tmp/hunger-venv
/tmp/hunger-venv/bin/pip install PyYAML==6.0.3
npm ci --ignore-scripts --prefix validation/plugin/protocol
/tmp/hunger-venv/bin/python validation/plugin/hunger/prepare.py \
  --server /tmp/hunger-acceptance/server \
  --runtime /private/tmp/alpha-server \
  --base /Users/iracarranza/minecraftmoba/artifacts/worldgen/alpha-0.1/base-terrain
NODE_PATH="$PWD/validation/plugin/protocol/node_modules" \
  node validation/plugin/hunger/reproduce.cjs \
  /tmp/hunger-acceptance/server /tmp/hunger-acceptance/report
```

`prepare.py` refuses any existing destination. `reproduce.cjs` requires its
marker, operates only that server process, and stops it in `finally`. It forces
Alpha `resource_light_v2`; only resource-pack delivery and local server transport /
view distances differ from baseline. It does not change Hunger settings.

The test verifies:

- Normal match start gives vanilla food 20 and saturation 20.
- Bread cannot be consumed at actual full Hunger.
- Up to 180 seconds of sprint/jump activity on a loaded stone platform, away
  from fountains and authored Routes, depletes saturation then actual food.
- The player stays alive on the platform; bread then restores food and consumes
  exactly one item.
- A separate controlled saturation=0 / exhaustion=3.9 boundary check reaches an
  uncancelled vanilla food decrease and permits eating afterward.

The boundary setup uses test-only `/mobafixture hunger-full` and `hunger-drain`.
`hunger-watch` logs samples for up to 240 seconds; event listeners log exhaustion,
food changes before/after gameplay listeners, and completed consumption. These
classes compile into the acceptance jar only, never the gameplay jar. No
experiment telemetry, balance score, or progression change is introduced.

Results are `results.json` and `server.log`. A passing test establishes these
conditions for the tested setup, not that every reported live-session symptom
has been explained. Time to depletion is diagnostic, not a balance conclusion.
