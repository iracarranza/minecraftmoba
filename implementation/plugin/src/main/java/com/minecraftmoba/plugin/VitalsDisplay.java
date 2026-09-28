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
    /**
     * Vanilla's own HUD geometry, which this draws into rather than beside.
     *
     * The hotbar is 182 GUI pixels wide and centred, so its left edge is 91
     * left of centre. The native health row starts there and the food row ends
     * 91 right of centre, each about 81 wide with a gap between them for the
     * air/armour rows. Blanking those rows and drawing here puts the glyph
     * bars exactly where the player already looks.
     *
     * [FIXTURE -- confirm by probe] The 81 in particular is read off a
     * screenshot, not out of the client.
     */
    private static final int HOTBAR_HALF = 91;
    private static final int NATIVE_ROW = 81;

    /**
     * Build the whole title with a NET ADVANCE OF ZERO.
     *
     * A bossbar title is centred, so its position depends on its own width --
     * which would make every element move whenever any other one changed
     * length. Returning to zero at the end fixes the centre at the screen
     * centre, and then every offset inside is an ABSOLUTE screen coordinate
     * measured from there.
     *
     * That is the difference between a layout and a pile of nudges, and it is
     * why the numeral can be drawn over the bar without the bar shifting.
     */
    public Component render(Player p) { return render(p, null); }

    public Component render(Player p, InWorldSelection.Clock clock) {
        double maxHealth = plugin.effectiveMaxHealth(p);
        int interval = plugin.getConfig().getInt("features.vitalsBar.tickInterval");
        int scale = plugin.getConfig().getInt("features.vitalsBar.displayScale");
        VitalsBar.Style style = style();
        int width = style.fillWidth();

        // p.getHealth() is the DISPLAYED 0..20 under scaling, not effective
        // points: VitalsScaling sets every player's attribute to DISPLAY_MAX
        // and lets Capacity decide what a point is worth. Dividing by Capacity
        // instead put the numerator on one scale and the denominator on the
        // other, and reported a full-health player at 105% of their maximum.
        int healthLevel = style.level(p.getHealth(), Vitals.DISPLAY_MAX);
        int hungerLevel = style.level(p.getFoodLevel(), Vitals.DISPLAY_MAX);
        var ticks = style.tickUnits(maxHealth * scale, interval);

        // Left edge of each bar, in pixels from screen centre. Inline sits
        // exactly on the native rows; wide keeps the 128 the ticks want and
        // overhangs, centred on the same two places.
        int healthLeft = -HOTBAR_HALF - (width - NATIVE_ROW) / 2;
        int hungerLeft = HOTBAR_HALF - NATIVE_ROW - (width - NATIVE_ROW) / 2;

        var out = Component.text();
        int at = 0;
        at = move(out, at, healthLeft);
        out.append(bar(healthLevel, ticks, colour("health"), style));
        at += width;

        // The numeral sits a few pixels ABOVE its bar rather than on the same
        // line, so it has its own ascent. Defaults to the bar's plus six,
        // which is where a sweep put it.
        int numeralAscent = plugin.getConfig().getInt(
                "features.vitalsBar.numeralAscent", style.ascent() + 6);
        if (numeralOverride != null) numeralAscent = numeralOverride;

        if (plugin.getConfig().getBoolean("features.vitalsBar.showNumerals")
                && HudText.ASCENTS.contains(numeralAscent)) {
            String numeral = Math.round(Vitals.toEffective(p.getHealth(), maxHealth) * scale)
                             + "/" + Math.round(maxHealth * scale);
            if (HudText.canDraw(numeral)) {
                // Back over the bar just drawn, then in by half the difference,
                // so the numeral is centred ON the bar rather than trailing it.
                //
                // Drawn in the PACK font at the bar's own ascent. Default-font
                // text cannot come down here at all -- ascent belongs to a
                // provider, and vanilla's puts it on the bossbar's line. That
                // is why the numeral sat at the top of the screen, and it was
                // never a positioning bug.
                int inset = Math.max(0, (width - HudText.width(numeral)) / 2);
                at = move(out, at, healthLeft + inset);
                out.append(Component.text(HudText.at(numeralAscent, numeral))
                                    .font(FONT).color(colour("numeral")));
                at += HudText.width(numeral);
            }
        }

        at = move(out, at, hungerLeft);
        out.append(bar(hungerLevel, java.util.Set.of(), colour("hunger"), style));
        at += width;

        // NO COMBAT LINE HERE.
        //
        // One was added and then removed the same night. InWorldSelection's
        // bossbar already renders combat -- red, named, and with its progress
        // ticking down -- and has since it was written; it was invisible only
        // because the pack had blanked the bossbar graphic. Adding a second
        // readout did not fix that, it just put a number beside an empty bar.
        //
        // A ticking bar is also the better form: the question is "how long
        // until I can leave", which a shrinking bar answers at a glance and a
        // number makes you read.

        // The clock's own label. The bossbar it belongs to is now this one,
        // and a bossbar's only text is its title -- so the words have to be
        // glyphs here even though the countdown is the bar underneath.
        if (clock != null && HudLabel.ASCENTS.contains(clockAscent())) {
            String label = clock.label();
            at = move(out, at, -HudLabel.width(label) / 2);
            out.append(Component.text(HudLabel.at(clockAscent(), label))
                                .font(FONT).color(colour("clock")));
            at += HudLabel.width(label);
        }

        // The charge meter, on the subtext line.
        //
        // A BOSSBAR COULD NOT GO HERE. Its graphic is fixed to the bossbar
        // strip and only its title moves, so a meter at a chosen height has to
        // be drawn -- which is what the vitals bars already are, unit glyphs
        // repeated. The same units at a second ascent give a meter anywhere,
        // any width, and beside text rather than above it.
        //
        // Drawn INSTEAD of the refusal line rather than beside it: they share
        // a line, and a charge in progress is the more urgent of the two.
        boolean charging = false;
        var inputs = plugin.inputs();
        Charge charge = inputs == null ? null : inputs.charging(p);
        if (charge != null && VitalsBar.ASCENTS.contains(noticeBarAscent())) {
            at = drawCharge(out, at, charge);
            charging = true;
        }

        // The refusal line, on the same canvas and its own line.
        //
        // Here rather than in its own bossbar because bars stack and each has
        // its own baseline, so a notice in a separate bar would land somewhere
        // different from the vitals it sits above. One canvas is what makes an
        // ascent mean one thing.
        // Not while charging: the two share this line, and a charge in
        // progress is the more urgent of the two readings.
        var notice = plugin.hudNotice();
        String message = charging || notice == null ? null : notice.current(p);
        if (message != null && HudLabel.ASCENTS.contains(notice.ascent())) {
            int centred = -HudLabel.width(message) / 2;
            at = move(out, at, centred);
            out.append(Component.text(HudLabel.at(notice.ascent(), message))
                                .font(FONT).color(colour("notice")));
            at += HudLabel.width(message);
        }

        move(out, at, 0);   // net zero, so the centre is the screen's centre
        return out.build();
    }

    /** Emit the offset that moves the cursor from {@code from} to {@code to}. */
    private int move(net.kyori.adventure.text.TextComponent.Builder out, int from, int to) {
        int delta = to - from;
        if (delta != 0)
            out.append(Component.text(delta > 0 ? HudLayer.right(delta) : HudLayer.left(-delta))
                                .font(FONT));
        return to;
    }

    /**
     * The charge meter: a percentage, a band colour, and a bar.
     *
     * The percentage is floored so the meter never reads 100% before the
     * charge completes -- the one reading a player would act on and be wrong
     * about. The band supplies both the colour and the word, so an ability
     * that gains an effect at four fifths says so rather than leaving the
     * player to watch a colour change and guess what it bought.
     */
    private int drawCharge(net.kyori.adventure.text.TextComponent.Builder out, int at, Charge charge) {
        long now = plugin.getServer().getCurrentTick();
        double progress = charge.progress(now);
        Charge.Band band = charge.band(progress);

        int width = plugin.getConfig().getInt("features.vitalsBar.meterWidth", 48);
        var style = new VitalsBar.Style(width, noticeBarAscent());
        int level = style.level(progress, 1.0);

        String text = Charge.percent(progress) + "%"
                + (band == null || band.label().isBlank() ? "" : " " + band.label());
        int total = HudLabel.width(text) + LABEL_GAP + width;

        at = move(out, at, -total / 2);
        out.append(Component.text(HudLabel.at(noticeAscentFor(text), text))
                            .font(FONT).color(bandColour(band)));
        at += HudLabel.width(text);
        at = move(out, at, at + LABEL_GAP);

        var empty = colour("empty");
        out.append(Component.text(style.filled(level, java.util.Set.of()))
                            .font(FONT).color(bandColour(band)));
        out.append(Component.text(style.unfilled(level, java.util.Set.of()))
                            .font(FONT).color(empty));
        return at + width;
    }

    /** Pixels between the percentage and the bar it describes. */
    private static final int LABEL_GAP = 4;

    private TextColor bandColour(Charge.Band band) {
        if (band == null) return colour("notice");
        TextColor parsed = TextColor.fromHexString(band.colour());
        return parsed == null ? colour("notice") : parsed;
    }

    private int noticeBarAscent() {
        return plugin.getConfig().getInt("features.vitalsBar.meterAscent", 73);
    }

    private int noticeAscentFor(String text) {
        var notice = plugin.hudNotice();
        return notice == null ? 73 : notice.ascent();
    }

    private Integer clockOverride;
    public void overrideClockAscent(Integer ascent) { this.clockOverride = ascent; }

    private int clockAscent() {
        return clockOverride != null ? clockOverride
                : plugin.getConfig().getInt("features.vitalsBar.clockAscent", 256);
    }

    /** Which width to draw. See VitalsBar.Style for what the choice costs. */
    /**
     * A live override for the ascent, set by {@code /moba hudprobe bars <n>}.
     *
     * Dialling this in took a restart per guess, and the guesses came from
     * measuring screenshots -- which is how the placement law ended up refitted
     * three times and still not landing a bar. The pack already emits every
     * candidate position, so the only thing standing between a guess and an
     * answer was a server restart. Now it is a command.
     *
     * Not persisted. The config value is the decision; this is the search.
     */
    private Integer ascentOverride;

    public void overrideAscent(Integer ascent) { this.ascentOverride = ascent; }
    public Integer ascentOverride() { return ascentOverride; }

    /** The same search, for the numeral's own height. */
    private Integer numeralOverride;
    public void overrideNumeralAscent(Integer ascent) { this.numeralOverride = ascent; }

    private VitalsBar.Style style() {
        String layout = plugin.getConfig().getString("features.vitalsBar.layout", "inline");
        var base = "wide".equalsIgnoreCase(layout) ? VitalsBar.WIDE : VitalsBar.INLINE;
        int ascent = ascentOverride != null ? ascentOverride
                : plugin.getConfig().getInt("features.vitalsBar.ascent", 50);
        try {
            return base.withAscent(ascent);
        } catch (IllegalArgumentException ex) {
            // A bar at an ascent the pack never emitted renders as tofu, which
            // looks like nothing. Say which values exist and draw anyway.
            plugin.getLogger().warning("features.vitalsBar.ascent: " + ex.getMessage());
            return base;
        }
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
    private Component bar(int level, java.util.Set<Integer> ticks, TextColor colour,
                          VitalsBar.Style style) {
        TextColor empty = colour("empty");
        // No caps: they would add two pixels the layout has not budgeted, and
        // the bar now sits in the slot vanilla's own row occupied, which has
        // no caps either.
        return Component.text(style.filled(level, ticks)).font(FONT).color(colour)
                .append(Component.text(style.unfilled(level, ticks)).font(FONT).color(empty));
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

    /**
     * One bossbar: the glyphs in its title, the clocks in its colour and bar.
     *
     * There used to be two -- this one, deliberately blank because the pack
     * blanks WHITE, and InWorldSelection's carrying combat and the level-up
     * beat. Bossbars stack in the order the server sends them, so the
     * invisible one sat on top and pushed the visible one down a slot: a
     * player saw a bar one row lower than it belonged, with nothing above it.
     *
     * Merging removes the empty slot AND the coordination problem, since there
     * is no longer an order for two bars to be in. White with no progress is
     * exactly an invisible bar, so the idle state still costs nothing.
     */
    public void refresh(Player p) {
        if (!enabled() || !plugin.enrolled(p)) { clear(p); return; }
        var selection = plugin.selection();
        var clock = selection == null ? null : selection.clock(p);

        Component title = render(p, clock);
        BossBar bar = bars.get(p.getUniqueId());
        if (bar == null) {
            bar = BossBar.bossBar(title, 0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
            bars.put(p.getUniqueId(), bar);
            p.showBossBar(bar);
        } else {
            bar.name(title);
        }
        // WHITE is the blanked colour, so "no clock" is a bar that is not
        // drawn rather than a bar drawn empty.
        bar.color(clock == null ? BossBar.Color.WHITE : clock.colour());
        bar.progress(clock == null ? 0f : Math.min(1f, Math.max(0f, clock.progress())));
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
