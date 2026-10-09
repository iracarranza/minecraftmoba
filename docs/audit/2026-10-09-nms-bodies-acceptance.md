# NmsBodies: acceptance against the real plugin

**9 October 2026.** `NmsBodies` (a `Bodies` implementation over embedded-channel
fake players) loaded into a throwaway Paper `1.21.11-132` server alongside the
**real** `MobaPlugin`, driven by a test-only fixture
(`implementation/plugin/nms-probe/bodies-acceptance/`). **18 of 18 checks pass.**

This follows the spawn prototype (`2026-10-09-bodies-spawn-prototype.md`), which
ran without `MobaPlugin`.

## What passes with the real plugin

| Check | Result |
|---|---|
| Bodies are enrolled, with the requested class and level | pass (`daredevil` L5, `lightfooted` L10) |
| Not sent to the lobby, although `lobbyWorld.enabled` is true | pass: world and position exact |
| F (swap-hand) reaches `AbilityInputs` through `PacketInputs` | pass: ability mode toggles on and back off |
| Vanilla swap does **not** also run | pass: `PlayerSwapHandItemsEvent` +0, as `PacketInputs` consumes it |
| Drop packet is consumed | pass: held item stays |
| Movement packet moves the body and fires `PlayerMoveEvent` | pass |
| Gravity acts | pass: fell to the floor |
| Despawn: offline, unenrolled, no playerdata file | pass, and idempotent |
| Four bodies spawned and despawned, twice | pass: nothing live |
| `connected()` | verified true after its self-test settles |

## The finding that changed the design: a settle period

A new body's input is **silently dropped for its first few ticks**. Measured with
the real plugin loaded, movement injected at 1, 5 and 6 ticks after spawn did
nothing, and from 7 ticks onward it worked (4 of 4 at 7, 8 and 9; 4 of 4 at 10,
20, 30 and 40). The spawn prototype had not found this because it injected
input about 28 ticks after spawning.

It matters because the drop is silent: the packet is accepted, no event fires and
nothing moves, which is exactly what input that works but has no effect looks
like. So:

- `NmsBodies.SETTLE_TICKS = 10` (a margin over the observed 7).
- `inject` **throws** for a body that has not settled, and the fixture asserts it.
- `connected()` cannot be answered in one tick. The first call starts a
  verification and returns **false with the reason "verification in progress"**;
  `prepare(callback)` runs it and notifies when done. Callers should prepare
  early (lab entry or plugin start) and wait, so a scenario that needs movement
  finds the answer ready.

## Two of my own mistakes, kept because they were instructive

- The first real-plugin run showed three failures. Two were the fixture reading
  state too early: the swap packet is queued through `PacketProcessor` and
  applies later in the tick, so reading immediately saw the old value. The step
  after it showed the toggle had worked. The third (`connected()`) was real.
- An earlier probe run reported 11 passes because the check only counted a
  message that *started* with "FAIL". Two steps had failed. Test harnesses need
  their own checking.

## Integration points

- `LobbyWorld.join` and `ResourcePackPush.join` skip bodies (`NmsBodies.isBody`).
  Nothing else needed a special case.
- netty is now a test dependency (`build.gradle.kts`), for the sink tests.
- `MobaPlugin` needs `maps/*.json.gz` on a test server to enable at all (the
  known test-server requirement, unchanged).

## Not covered

- **Team assignment into a running match**: `spawn` adds the body as a
  participant only if a match is running, and none was bound on the test server.
- **Walking** (held WASD) and collision beyond a flat floor.
- Long-running memory behavior.
- Re-verification after a Paper update (`nms-probe` is the tool for what moved).
