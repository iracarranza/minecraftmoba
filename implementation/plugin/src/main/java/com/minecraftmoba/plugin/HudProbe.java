package com.minecraftmoba.plugin;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Step one of the HUD layer: find out where a layer actually lands.
 *
 * <h2>Why a probe rather than a feature</h2>
 *
 * The documentation cannot say where a glyph at a given {@code ascent} renders
 * on screen, or at what point an extreme one clips. Only a client can, and
 * every ascent in {@code registry.json} is currently a guess labelled as one.
 *
 * Designing a layout against guessed numbers and then discovering they are
 * wrong is precisely the failure this project already has on record: the last
 * glyph readout shipped with the vanilla row hidden and the replacement
 * illegible, and was reported, accurately, as "hunger is fully invisible".
 * Measure first.
 *
 * The pack already carries a sibling of this -- {@code ability_panel_probe} --
 * built to answer the same class of question about item models. Same method.
 *
 * <h2>What to look for</h2>
 *
 * The glyph is a ruler, not a picture: a full-height left edge, a full-width
 * bottom edge, and red ticks every 8px along both. So a screenshot answers
 * three things at once -- where the origin is, where the baseline is, and how
 * many pixels of drift there are -- without trusting any guess about scale.
 *
 * <h2>This draws in its OWN bossbar</h2>
 *
 * Deliberately, and it is the measurement that matters most. Bossbars stack,
 * each with its own baseline, so the same layer lands somewhere different
 * depending on which bar drew it. Run this with the vitals bar on and off and
 * the difference between the two readings IS the drift.
 */
public final class HudProbe {

    /**
     * The font the pack maps these codepoints into.
     *
     * A component carries exactly ONE font, and U+E200 means nothing in the
     * default one -- it renders as tofu, which looks like nothing rather than
     * like a bug. Every emission of a pack glyph must name this.
     */
    private static final Key FONT = Key.key("moba", "glyphs");

    private final MobaPlugin plugin;
    private final Map<UUID, BossBar> bars = new HashMap<>();

    public HudProbe(MobaPlugin plugin) { this.plugin = plugin; }

    public boolean command(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("/moba hudprobe needs an in-game player.");
            return true;
        }
        String verb = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "all";
        if (verb.equals("off")) { clear(p); p.sendMessage("Probe cleared."); return true; }

        // The vitals bars' own height, live.
        //
        // Every candidate ascent already exists in the pack, so the only thing
        // between a guess and an answer was a restart -- and the guesses came
        // from measuring screenshots, which is how the placement law got
        // refitted three times without landing a bar. Sweep it instead.
        if (verb.equals("numeral")) {
            if (args.length < 3) {
                p.sendMessage("/moba hudprobe numeral <ascent|off>   available: " + HudText.ASCENTS);
                return true;
            }
            var display = plugin.vitalsDisplay();
            if (args[2].equalsIgnoreCase("off")) {
                display.overrideNumeralAscent(null);
                p.sendMessage("Numeral back to the configured ascent.");
                return true;
            }
            int wanted = parse(args[2], -1);
            if (!HudText.ASCENTS.contains(wanted)) {
                p.sendMessage("No text block at ascent " + args[2]
                        + ". The pack carries: " + HudText.ASCENTS);
                return true;
            }
            display.overrideNumeralAscent(wanted);
            p.sendMessage("Numeral at ascent " + wanted + ".");
            return true;
        }

        if (verb.equals("bars")) {
            if (args.length < 3) {
                p.sendMessage("/moba hudprobe bars <ascent|off>   available: " + VitalsBar.ASCENTS);
                return true;
            }
            var display = plugin.vitalsDisplay();
            if (args[2].equalsIgnoreCase("off")) {
                display.overrideAscent(null);
                p.sendMessage("Bars back to the configured ascent.");
                return true;
            }
            int wanted = parse(args[2], -1);
            if (!VitalsBar.ASCENTS.contains(wanted)) {
                p.sendMessage("No unit set at ascent " + args[2]
                        + ". The pack carries: " + VitalsBar.ASCENTS);
                return true;
            }
            display.overrideAscent(wanted);
            p.sendMessage("Bars at ascent " + wanted
                    + ". Sweep with /moba hudprobe bars <n>, keep it by setting "
                    + "features.vitalsBar.ascent.");
            return true;
        }

        int x = args.length > 2 ? parse(args[2], 0) : 0;
        Component title = verb.equals("all") ? everyLayer(x) : oneLayer(verb, x);
        if (title == null) {
            p.sendMessage("/moba hudprobe [all|off|<layer>] [x]   layers: " + HudLayer.LAYERS);
            return true;
        }

        BossBar bar = bars.get(p.getUniqueId());
        if (bar == null) {
            // PURPLE is the one colour the pack deliberately does NOT blank, so
            // this probe also shows, in the same screenshot, that blanking the
            // other six worked and that keeping one was possible.
            bar = BossBar.bossBar(title, 1f, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
            bars.put(p.getUniqueId(), bar);
            p.showBossBar(bar);
        } else {
            bar.name(title);
        }
        p.sendMessage("Probe drawing " + verb + " at x=" + x
                + ". Ticks are 8px. /moba hudprobe off to clear.");
        return true;
    }

    /**
     * Every layer at once, each stepped 40px right of the last.
     *
     * Stepped rather than stacked so they cannot be confused with one another:
     * if two layers render at the same height, overlapping them would hide
     * exactly the fact worth learning.
     */
    private Component everyLayer(int x) {
        var out = Component.text();
        int at = x;
        for (String layer : HudLayer.LAYERS) {
            out.append(Component.text(HudLayer.at(layer, at == x ? at : 40)).font(FONT));
            at += 40;
        }
        return out.build();
    }

    private Component oneLayer(String layer, int x) {
        if (!HudLayer.LAYERS.contains(layer)) return null;
        return Component.text(HudLayer.at(layer, x)).font(FONT);
    }

    private static int parse(String text, int fallback) {
        try { return Integer.parseInt(text); } catch (NumberFormatException e) { return fallback; }
    }

    public void clear(Player p) {
        BossBar bar = bars.remove(p.getUniqueId());
        if (bar != null) p.hideBossBar(bar);
    }
}
