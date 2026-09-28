package com.minecraftmoba.plugin;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The refusal line: "not that, and here is why".
 *
 * <h2>The problem it exists for</h2>
 *
 * Three refusals had no legible channel -- an ability not yet unlocked, one
 * still on cooldown, and one whose requirements are unmet. The first went to
 * the action bar and the second said nothing at all.
 *
 * The action bar has <b>twenty-six writers across thirteen files and no
 * arbiter</b>, and {@code AbilityInputs.bar()} rewrites it on a cadence -- so
 * a refusal sent there is overwritten within a tick or two. That is the
 * "briefly flashes illegibly" report exactly, and it was never a formatting
 * problem. Drawing here does not arbitrate that surface; it stops using it.
 *
 * <h2>One place for all three</h2>
 *
 * They are the same category of answer, and a player scanning for a reason
 * should not have to know which of three places to look.
 *
 * <h2>Where it draws</h2>
 *
 * Into the vitals bossbar's title, which is the project's nominated canvas.
 * Bossbars stack and each has its own baseline, so a notice in its own bar
 * would land somewhere different from the bars it sits under -- sharing one
 * canvas is what makes an ascent mean one thing.
 */
public final class HudNotice {

    /** A message and the tick it stops being shown. */
    private record Shown(String text, long until) {}

    private final MobaPlugin plugin;
    private final Map<UUID, Shown> notices = new HashMap<>();

    public HudNotice(MobaPlugin plugin) { this.plugin = plugin; }

    private long now() { return plugin.getServer().getCurrentTick(); }

    /** How long a refusal stays up. Long enough to read, short enough not to nag. */
    private int durationTicks() {
        return plugin.getConfig().getInt("features.vitalsBar.noticeTicks", 40);
    }

    /** Where it sits. Swept with /moba hudprobe notice. */
    public int ascent() {
        Integer override = this.override;
        if (override != null) return override;
        return plugin.getConfig().getInt("features.vitalsBar.noticeAscent", 73);
    }

    private Integer override;
    public void overrideAscent(Integer ascent) { this.override = ascent; }

    public void show(Player p, String message) {
        notices.put(p.getUniqueId(), new Shown(message, now() + durationTicks()));
    }

    /** The current message, or null. Expiry is noticed on read, not swept. */
    public String current(Player p) {
        Shown shown = notices.get(p.getUniqueId());
        if (shown == null) return null;
        if (shown.until() > now()) return shown.text();
        notices.remove(p.getUniqueId());
        return null;
    }

    public void clear(Player p) { notices.remove(p.getUniqueId()); }

    // ---- the three refusals ----------------------------------------------

    public void notUnlocked(Player p, int level) {
        show(p, "Ability unlocks at level " + level);
    }

    public void onCooldown(Player p, long ticksLeft) {
        long tenths = Math.max(1, ticksLeft / 2);
        show(p, "On cooldown " + (tenths / 10) + "." + (tenths % 10) + "s");
    }

    public void unavailable(Player p, String why) {
        show(p, why == null || why.isBlank() ? "Cannot be used right now" : why);
    }
}
