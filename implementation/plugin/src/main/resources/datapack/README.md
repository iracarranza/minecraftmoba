# Companion datapack

The plugin cannot create dialogs or advancements — those are datapack content.
This pack supplies them and does nothing on its own; every button runs a plugin
command, and every advancement is granted by the plugin.

Install into the world's `datapacks/` directory. Without it, Infrastructure Mode
is still reachable via `/moba infra enter`; the pack only adds the Quick Actions
(G) entry point.

Authority stays in the plugin. A missing or corrupt pack removes the menu, not
the state: `ChoiceRecord` in `PlayerData` remains the record of what a player
chose, and the advancement tree is a display derived from it.
