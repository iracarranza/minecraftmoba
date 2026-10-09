# NMS signature probe

Dumps the signatures of the server internals a fake-player implementation
(`Bodies`) needs, so they are **read, not guessed**. Re-run it on every Paper
update: the harness doc records that reflective NMS "will break on Paper
updates", and this is how to find out what moved.

`nms-signatures-1.21.11-132.txt` is the dump from Paper `1.21.11-132`
(`c5eb079`, 11 May 2026). Mojang-mapped names resolve at runtime, which is why
`PacketInputs` can reflect on `ServerboundPlayerActionPacket` by name.

## Run it

Not a Gradle subproject: it needs the Paper API and its Adventure dependencies on
the compile classpath, then a server to execute in.

1. Compile `src/probe/NmsProbe.java` against `paper-api` plus the `net.kyori`
   jars from the Gradle cache, `--release 21`; jar it with `plugin.yml`.
2. Put the jar in a throwaway Paper server's `plugins/`. Do not use the live
   alpha server.
3. Start the server. The probe waits two seconds, writes
   `plugins/NmsProbe/nms-signatures.txt`, and stops the server.

To probe another class, add it to `TARGETS` with a method-name regex (`*` for all,
empty for constructors and fields only).

## What the 1.21.11-132 dump established

| Needed for | Found |
|---|---|
| Creating the player | `ServerPlayer(MinecraftServer, ServerLevel, GameProfile, ClientInformation)`; `ClientInformation.createDefault()` |
| Joining | `PlayerList.placeNewPlayer(Connection, ServerPlayer, CommonListenerCookie)`; `CommonListenerCookie.createInitial(GameProfile, boolean)` |
| The connection | `Connection(PacketFlow)`, public `channel` field, `channelActive(ChannelHandlerContext)`, `configurePacketHandler(ChannelPipeline)` |
| Removing | `PlayerList.remove(ServerPlayer)` |
| Movement input | `ServerboundMovePlayerPacket.PosRot(x,y,z,yaw,pitch,onGround,horizontalCollision)`, `.Rot`, `.Pos`, `.StatusOnly` |
| Key input | `ServerboundPlayerInputPacket(Input(forward,backward,left,right,jump,shift,sprint))` |
| Swap hand and drop | `ServerboundPlayerActionPacket(Action, BlockPos, Direction)`; `Action` includes `SWAP_ITEM_WITH_OFFHAND`, `DROP_ITEM` |
| Swing, use, slot | `ServerboundSwingPacket(InteractionHand)`, `ServerboundUseItemPacket(InteractionHand,int,float,float)`, `ServerboundSetCarriedItemPacket(int)` |

Nothing the design needs was missing.
