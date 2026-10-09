# NmsBodies acceptance fixture

A test-only plugin that exercises `NmsBodies` inside a server with the **real**
`MobaPlugin` loaded, and writes `plugins/BodiesAcceptance/report.txt`, then stops
the server. Not for production. `report-1.21.11-132.txt` is the last run: 18 of 18.

## Set up a throwaway server (never the live alpha server)

1. A Paper `1.21.11` jar named `paper.jar`. The tested build is `132`
   (`c5eb079`); its SHA-256 is `5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba`.
2. `eula.txt` with `eula=true`, accepted by whoever runs it.
3. `server.properties`: a free `server-port`, `online-mode=false`,
   `level-type=minecraft\\:flat`, `generate-structures=false`, `spawn-protection=0`.
4. `maps/*.json.gz` copied from `artifacts/worldgen/alpha-0.1/maps/`, as
   `deploy.sh` does. Without them `MobaPlugin` fails to enable with
   `could not load map configuration`. A missing `alpha.templatePath` only warns.
5. Put `minecraft-moba-*.jar` (built with `./gradlew jar`) and
   `BodiesAcceptance.jar` in `plugins/`.

## Build the fixture

Compile `src/bf/BodiesAcceptance.java` against the plugin jar, `paper-api`, the
`net.kyori` jars and `netty-transport` (all in the Gradle cache), `--release 21`,
and jar it with `plugin.yml` (it `depend`s on `MinecraftMoba`).

## What it checks

`connected()` starts verification without claiming an answer, then verifies true;
unsettled input is refused; bodies are enrolled with the right class and level and
are not sent to the lobby; F reaches `AbilityInputs` through `PacketInputs` and
toggles ability mode; movement fires `PlayerMoveEvent`; a drop is consumed;
gravity acts; despawn leaves nothing behind; repeated spawn and despawn leaves
nothing live.

## Not covered

Team assignment into a **running** match (needs a bound map), walking input, and
collision beyond a flat floor.
