package com.minecraftmoba.plugin;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Hunger readout, drawn by the plugin because the vanilla row cannot say it.
 *
 * Capacity caps a player's Hunger well below twenty, so their bar has three
 * meaningfully different kinds of drumstick: filled, empty but refillable, and
 * beyond the cap entirely. The vanilla HUD renders food level against a fixed
 * maximum of twenty and has no notion of the third, and a resource pack cannot
 * add one: an empty-but-available drumstick and an unavailable one are the same
 * client-side state, so one static texture must serve both.
 *
 * The pack therefore blanks the vanilla row (see registry.json,
 * hidden_vanilla_sprites) and this draws the readout from glyphs that do
 * distinguish the three. Codepoints come from the pack's generated manifest;
 * emitting one the pack does not define renders as tofu rather than as a wrong
 * icon, which is why both sides read the same registry.
 *
 * Without the pack installed the glyphs are unmapped and the readout is
 * useless, so this is disabled by default and turned on alongside the pack.
 */
public final class HungerDisplay implements Listener {
    /** Must match implementation/resourcepack/registry.json, group "hunger". */
    static final String FULL = "";
    static final String HALF = "";
    static final String EMPTY = "";
    static final String UNAVAILABLE = "";

    private static final Key FONT = Key.key("moba", "glyphs");

    private final MobaPlugin plugin;
    private final Map<UUID, BossBar> bars = new HashMap<>();

    public HungerDisplay(MobaPlugin plugin) {
        this.plugin = plugin;
        long period = plugin.getConfig().getLong("features.hungerDisplay.refreshTicks", 10L);
        if (period > 0) Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, period, period);
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("features.hungerDisplay.enabled");
    }

    /**
     * The readout as a glyph string.
     *
     * Vanilla draws ten drumsticks for twenty food points, so a point is half a
     * drumstick and the cap is converted the same way. A cap on an odd point
     * leaves a half drumstick available, which is drawn as empty rather than
     * unavailable: the player really can put one point there.
     */
    static String render(int food, int cap, int maximum) {
        int drumsticks = maximum / 2;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < drumsticks; i++) {
            int low = i * 2 + 1;          // the two food points this drumstick shows
            int high = i * 2 + 2;
            if (food >= high) out.append(FULL);
            else if (food >= low) out.append(HALF);
            else if (cap >= low) out.append(EMPTY);
            else out.append(UNAVAILABLE);
        }
        return out.toString();
    }

    public void refresh(Player p) {
        if (!enabled() || !plugin.enrolled(p)) { clear(p); return; }
        int cap = plugin.effectiveHunger(p);
        Component title = Component.text(render(p.getFoodLevel(), cap, 20))
                .style(Style.style().font(FONT).build());
        BossBar bar = bars.get(p.getUniqueId());
        if (bar == null) {
            bar = BossBar.bossBar(title, 0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
            bars.put(p.getUniqueId(), bar);
            p.showBossBar(bar);
        } else {
            bar.name(title);
        }
    }

    public void clear(Player p) {
        BossBar bar = bars.remove(p.getUniqueId());
        if (bar != null) p.hideBossBar(bar);
    }

    private void refreshAll() {
        for (Player p : Bukkit.getOnlinePlayers()) refresh(p);
    }

    @EventHandler public void onJoin(PlayerJoinEvent e) { refresh(e.getPlayer()); }
    @EventHandler public void onQuit(PlayerQuitEvent e) { bars.remove(e.getPlayer().getUniqueId()); }

    public String report(Player p) {
        int cap = plugin.effectiveHunger(p);
        return "HUNGER_DISPLAY enabled=" + enabled() + " food=" + p.getFoodLevel()
                + " cap=" + cap + " drumsticks=full/half/empty/unavailable"
                + " render=" + render(p.getFoodLevel(), cap, 20)
                .replace(FULL, "F").replace(HALF, "h").replace(EMPTY, ".").replace(UNAVAILABLE, "x");
    }
}
