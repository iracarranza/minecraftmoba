# Class kit acceptance fixture

Java 21, Paper 1.21.11 build 132. Test only. Produces a report and stops its
own server; never deploy it to a live/user server.

Build the plugin with `JAVA_HOME=/path/to/java21 implementation/plugin/gradlew
-p implementation/plugin test jar`. Then run:

```sh
python3 validation/plugin/kits/run.py \
  --runtime /path/to/existing-paper-runtime \
  --server /private/tmp/a-new-kit-fixture-server \
  --results validation/plugin/kits/results/a-new-run \
  --java-home /path/to/java21
```

The runtime must contain `paper.jar`, `cache`, `versions`, `libraries` and an
already accepted `eula.txt`. The script refuses existing server directories,
copies runtime dependencies, compiles the test-only fixture from local Gradle
dependencies and starts a server bound to localhost port 25668. It uses a native
flat validation platform, not a playable-map build. Lobby damage protection is
disabled only inside this fixture so out-of-match ability checks can take damage.

The checked report is [25/25 passing](results/2026-10-11/report.txt). Death is
tested by Paper event dispatch and actual death/respawn; disconnect is a real
NMS-body despawn. The fixture does not establish human-client eating, all
branches, every game-mode death path, or balance/feel acceptance.
