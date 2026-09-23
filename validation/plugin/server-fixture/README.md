# Disposable-server event checks

This is a separate test plugin, never a production dependency. Build from the
repository with `implementation/plugin/gradlew -p implementation/plugin
:acceptance-fixture:build`, then install its jar **only** in the disposable fixture
alongside the main plugin. It does not transport, drop or repair inventory items.

As an enrolled operator with room in storage:

1. `/moba setlevel <name> 1`, then `/mobafixture transfer-partial`.
2. `/moba setlevel <name> 30` (or configured maximum), then `/mobafixture transfer-full`.
3. `/moba setlevel <name> 1`, then `/mobafixture denied-place`.

Run each command after the preceding command completes. The harness dispatches
InventoryMoveItemEvent through the real plugin manager. At partial capacity it
must cancel a player destination; at full capacity with safe room it permits an
ordinary item. Tagged maps are rejected in both directions, and both inventories
must be byte-for-byte equivalent as ItemStacks before/after event dispatch.
Vanilla hoppers do not have a player-inventory destination, so this is explicitly
a synthetic server event compatibility test, not a claim about a vanilla hopper
physically feeding a player.

The denied-placement case dispatches canBuild=false without cancellation and
asserts unchanged chunk provenance. It places no real block.

The separate [Hunger acceptance runner](../hunger/README.md) uses
`hunger-watch`, `hunger-full`, and `hunger-drain` to observe activity and prepare
one controlled exhaustion boundary. The latter two deliberately alter test
player vitals; they must only be used in the disposable server.
