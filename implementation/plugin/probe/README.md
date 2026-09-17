# Step 2: empty-mainhand drop action gate

Throwaway diagnostic for **Paper 1.21.11 build 132**. Build with `./gradlew :probe:build`
from the plugin root. Install the separate
`probe/build/libs/probe-0.1.0.jar` only on an isolated test server.

This attaches before Paper's decoded packet handler using a version-pinned reflective
lookup. It logs every `ServerboundPlayerActionPacket`, counts drop actions, and logs
both hands on the main server thread. **It cancels drop and swap packets** to protect
control items. It never writes an inventory or creates/moves an item. A Bukkit drop
event is logged and cancelled as an extra diagnostic. Remove the jar after the test.

1. Use an unmodified Java 1.21.11 client in survival. Operator permission is required
   for `/dropprobe <label>`. Confirm `ATTACHED` appears; errors invalidate the test.
2. With an empty mainhand, run `/dropprobe empty_begin`, close chat, press F as a
   positive packet-listener control, then Q repeatedly and Ctrl+Q repeatedly.
   Run `/dropprobe empty_end`. Log the exact counts pressed and timestamps.
3. Repeat with a held item, labeling `held_begin` / `held_end`. The held item must
   remain held; the probe cancels all drop actions. Its client-side prediction may
   require an inventory refresh. Do not confuse that prediction with server movement.
4. Compare `DROP_ITEM` / `DROP_ALL_ITEMS` packet counts across boundaries, and check
   the hand-state snapshots. A synthetic packet client cannot satisfy this gate.
5. If no drop packets arrive with an empty hand while controls work, stop Phase 1.
   Do not add a sentinel or implement an alternate binding without a user decision.

No pass/fail conclusion exists until the live test is run. The main plugin does not
load this diagnostic. Server logs belong in a separate validation directory.
