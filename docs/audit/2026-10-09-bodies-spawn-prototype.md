# Bodies: the spawn prototype

**9 October 2026.** A throwaway probe plugin (`implementation/plugin/nms-probe/body-probe/`)
run on Paper `1.21.11-132`, to learn whether a connected fake player is
buildable before committing to `Bodies`. **Prototype only: nothing here is in
the plugin, and the real `MobaPlugin` was not loaded.** 19 of 19 checks pass in
the final run.

## Result

A body is a real `ServerPlayer` placed with `PlayerList.placeNewPlayer` over a
netty `EmbeddedChannel` whose pipeline is built by
`Connection.configurePacketHandler`. That yields a pipeline of
`[hackfix, packet_handler]`, so `PacketInputs.attach` works on it **unchanged**:
the same reflection chain resolves, and a handler inserted before
`packet_handler` sees inbound packets.

| Check | Result |
|---|---|
| Spawn: online, in the player list, join event fires | pass |
| `PacketInputs`' reflection chain and handler insertion | pass |
| Join teleport accepted (`ServerboundAcceptTeleportationPacket`) | pass |
| **Movement packet drives `PlayerMoveEvent`** | pass, **only after** `ServerboundPlayerLoadedPacket` |
| Swap-hand packet reaches the handler, and `PlayerSwapHandItemsEvent` fires | pass |
| Swap through `server.packetProcessor().scheduleIfPossible` (the `NativePacketQueue` path) | pass |
| Key-input packet fires `PlayerInputEvent`; swing fires `PlayerAnimationEvent` | pass |
| Despawn (`PlayerList.remove`): offline, quit event fires | pass |
| Four bodies spawned and despawned | pass |
| **Gravity and `setVelocity` act on the body** | pass, **only with a per-tick `travel(Vec3.ZERO)`** |

`connected()` can therefore be **true**: injected packets drive the same events a
client would.

## Three things that were not obvious

1. **1.21.x ignores movement until the client reports it has loaded.** Before
   `ServerboundPlayerLoadedPacket`, the movement and swap packets were dropped
   silently (no event, no location change). The join teleport must also be
   accepted first.
2. **A body does not move itself.** Players are client-authoritative, so a body
   has no gravity and `setVelocity` does nothing: the first probe run showed a
   body hanging at y=-45 and ignoring a velocity. Abilities that move a player
   with `setVelocity` (Tunneling, Runway, Bounding) would not move a body.
   **Calling the entity's own `travel(Vec3.ZERO)` once per tick fixes both**: it
   fell 15 blocks to the ground, and a velocity of 0.6 moved it 1.3 blocks over
   about 15 ticks, with drag.
3. **Spawn position must be set with `setPos` before joining.** `snapTo` throws
   a `NullPointerException` because it calls `connection.resetPosition()` and the
   connection does not exist until `placeNewPlayer`.

## Costs to design for

- **Outbound packets pile up.** An `EmbeddedChannel` retains everything the server
  sends it: 190 to 581 packets within a couple of seconds. Add an outbound
  handler that discards writes (releasing them) instead of draining periodically.
- **`PlayerList.remove` writes a playerdata file.** It must be deleted, or the
  write avoided.
- **Join and quit events fire.** Every join handler (lobby teleport, resource-pack
  push, enrollment) will react to a body. Bodies should be created only after a
  lab or match is running, and any handler that misfires needs a body registry.
- Each body is a full ticking `ServerPlayer`.

## Not verified

- The real `MobaPlugin` with a body in it: enrollment, HUD and scoreboard attach,
  ability dispatch from a swap packet end to end.
- **Walking.** Held WASD movement needs `aiStep` or `travel` with an input
  vector, not `Vec3.ZERO`. Only gravity and a set velocity were tested.
- Collision and slopes beyond a flat floor.
- Long-running behavior: memory with many bodies, repeated spawn and despawn.
- Anything after a Paper update. Re-run `nms-probe` to see what moved.

## Implication for the combat chamber

A passive dummy that stands and casts needs only the connection and inputs, which
work. A dummy that runs a movement ability needs the per-tick `travel` hook,
which is one call. Replaying walking inputs needs more.
