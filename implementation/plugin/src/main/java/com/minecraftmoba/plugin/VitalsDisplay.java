package com.minecraftmoba.plugin;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Draws the health and hunger bars, both on one bossbar title.
 *
 * Replaces {@link HungerDisplay}, which drew hunger alone as ten glyphs and is
 * switched off. This draws both, fixed-size, with a proportional fill.
 *
 * <h2>Why a bossbar</h2>
 *
 * Because a plugin cannot add a HUD element, and a bossbar title is the
 * drawing surface this project already established for glyph work -- {@link
 * Hud} says so in as many words, and {@code HungerDisplay} already used it.
 * The action bar was the alternative and was rejected: twenty-six call sites
 * across thirteen files already write to it, so a persistent bar there would
 * either flicker against every ability cast and Recall tick, or force all of
 * them through an arbiter. That refactor is worth doing on its own merits and
 * is not worth smuggling into a bar.
 *
 * Both bars share ONE bossbar rather than taking one each, because bossbars
 * stack and this HUD already has two others.
 *
 * <h2>The pack is required, and the vanilla rows stay until it is proven</h2>
 *
 * Without the resource pack these codepoints are unmapped and the bar renders
 * as tofu. That is not hypothetical here: the previous glyph readout shipped
 * with the vanilla row hidden and the replacement illegible, and was reported,
 * accurately, as "hunger is fully invisible".
 *
 * So this ships OFF, and hiding the native rows is a SEPARATE switch that is
 * also off. Turn this on, confirm the bars draw, and only then hide the rows
 * they duplicate. The ordering is the whole lesson from last time.
 *
 * <h2>Colour is applied here, not in the pack</h2>
 *
 * The glyphs are drawn white and uncoloured because the pack assigns identity
 * and never palette. Tinting a white glyph with an Adventure colour is what
 * makes one bar read as health and the other as hunger, and puts the palette
 * decision in config where it can be argued with.
 */
public final class VitalsDisplay implements Listener {

    private static final Key FONT = Key.key("moba", "glyphs");

    /**
     * The vanilla font, named EXPLICITLY on anything that is ordinary text.
     *
     * Adventure children inherit their parent's font, and the numeral is
     * appended to the health bar -- whose root carries {@code moba:glyphs}. So
     * "900/900" inherited the glyph font, where digits and the slash are
     * unmapped, and rendered as seven tofu boxes between the two bars. Visible
     * in the first working screenshot of this HUD.
     *
     * Inheritance is the trap: nothing about the numeral's own code says it is
     * in a custom font, and it is the parent three lines up that decides.
     */
    private static final Key DEFAULT_FONT = Key.key("minecraft", "default");

    private final MobaPlugin plugin;
    private final Map<UUID, BossBar> bars = new HashMap<>();

    public VitalsDisplay(MobaPlugin plugin) {
        this.plugin = plugin;
        long period = plugin.getConfig().getLong("features.vitalsBar.refreshTicks");
        if (period <= 0) throw new IllegalArgumentException("features.vitalsBar.refreshTicks must be positive");
        Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, period, period);
    }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.vitalsBar.enabled"); }

    private void refreshAll() { for (Player p : Bukkit.getOnlinePlayers()) refresh(p); }

    /**
     * Both bars, side by side, as one component.
     *
     * Health is measured against the player's effective maximum rather than
     * the vanilla twenty, so the bar means the same thing for every Capacity --
     * which is the point of VitalsScaling and would be undone by reading
     * getMaxHealth here.
     */
    public Component render(Player p) {
        double maxHealth = plugin.effectiveMaxHealth(p);
        int interval = plugin.getConfig().getInt("features.vitalsBar.tickInterval");
        int scale = plugin.getConfig().getInt("features.vitalsBar.displayScale");

        // p.getHealth() is the DISPLAYED 0..20 under scaling, not effective
        // points: VitalsScaling sets every player's attribute to DISPLAY_MAX
        // and lets Capacity decide what a point is worth. So the fill is
        // measured against DISPLAY_MAX, and the numeral converts back.
        //
        // Dividing by Capacity instead is what produced "2000/1900" on a
        // level 30 Lightfooted -- a player at full health reading as 105% of
        // their own maximum, because the numerator was on one scale and the
        // denominator on the other.
        Component health = bar(VitalsBar.level(p.getHealth(), Vitals.DISPLAY_MAX),
                               VitalsBar.tickUnits(maxHealth * scale, interval),
                               colour("health"));
        Component hunger = bar(VitalsBar.level(p.getFoodLevel(), Vitals.DISPLAY_MAX),
                               java.util.Set.of(), colour("hunger"));

        Component out = health;
        if (plugin.getConfig().getBoolean("features.vitalsBar.showNumerals"))
            out = out.append(Component.text("  "
                                            + Math.round(Vitals.toEffective(p.getHealth(), maxHealth) * scale)
                                            + "/" + Math.round(maxHealth * scale))
                                      .font(DEFAULT_FONT)
                                      .color(colour("health")));
        return out.append(Component.text("   ")).append(hunger);
    }

    /**
     * One bar: caps, then the lit run, then the unlit run, ticks marked across both.
     *
     * The two runs are separate components because they are tinted
     * differently. The pack draws every unit white and assigns no palette, so
     * an unfilled unit only reads as unfilled once the plugin dims it -- which
     * is also why the bar cannot be one string with one colour on it.
     *
     * Ticks are indexed along the WHOLE bar and passed to both runs, so a tick
     * stays at the same pixel whether the fill currently reaches it or not.
     * Indexing per run would make the ticks slide as the player took damage,
     * which is the opposite of a reference mark.
     *
     * Hunger passes an empty tick set: it is twenty points on a fixed scale and
     * has no magnitude to convey, so marks there would be decoration.
     */
    private Component bar(int level, java.util.Set<Integer> ticks, TextColor colour) {
        TextColor empty = colour("empty");
        return Component.text(VitalsBar.CAP_LEFT).font(FONT).color(empty)
                .append(Component.text(VitalsBar.filled(level, ticks)).font(FONT).color(colour))
                .append(Component.text(VitalsBar.unfilled(level, ticks)).font(FONT).color(empty))
                .append(Component.text(VitalsBar.CAP_RIGHT).font(FONT).color(empty));
    }

    private TextColor colour(String which) {
        String hex = plugin.getConfig().getString("features.vitalsBar.colours." + which);
        TextColor parsed = hex == null ? null : TextColor.fromHexString(hex);
        // A malformed colour must not take the bar with it: an unreadable HUD
        // is a worse outcome than an unintended tint, and the log says which.
        if (parsed == null) {
            plugin.getLogger().warning("features.vitalsBar.colours." + which
                    + " is not a hex colour; drawing the bar white.");
            return TextColor.color(0xFFFFFF);
        }
        return parsed;
    }

    public void refresh(Player p) {
        if (!enabled() || !plugin.enrolled(p)) { clear(p); return; }
        Component title = render(p);
        BossBar bar = bars.get(p.getUniqueId());
        if (bar == null) {
            // Progress 0 and an empty overlay: the bossbar is carrying a
            // drawing, and its own coloured bar underneath would be a second
            // meter saying something else.
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

    @EventHandler public void onQuit(PlayerQuitEvent e) { bars.remove(e.getPlayer().getUniqueId()); }

    public String report(Player p) {
        double maxHealth = plugin.effectiveMaxHealth(p);
        return "VITALSBAR enabled=" + enabled()
                + " health=" + String.format("%.2f", p.getHealth()) + "/" + String.format("%.2f", maxHealth)
                + " healthLevel=" + VitalsBar.level(p.getHealth(), maxHealth) + "/" + VitalsBar.FILL_WIDTH
                + " food=" + p.getFoodLevel() + "/" + (int) Vitals.DISPLAY_MAX
                + " hungerLevel=" + VitalsBar.level(p.getFoodLevel(), Vitals.DISPLAY_MAX)
                + " hideNativeRows=" + plugin.getConfig().getBoolean("features.vitalsBar.hideNativeRows");
    }
}
