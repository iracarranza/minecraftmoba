package com.minecraftmoba.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * The testing-mode sidebar: loadout, the tester's health and armor, the dummy's health and armor.
 *
 * It gets its own scoreboard so nobody else's screen shows it (the main scoreboard is shared), and gives the
 * tester's previous scoreboard back when testing ends. The lines are {@link TestingHudLines}.
 *
 * NOT VERIFIED beyond the live acceptance run at the time of writing; what it LOOKS like on a client has not been seen.
 */
final class TestingHud {
    private static final String OBJECTIVE = "moba_testing";
    private static final long PERIOD = 10L;

    private final MobaPlugin plugin;
    private final LoadoutStore store;
    private final Function<Player, Player> dummyOf;
    private final Map<UUID, Scoreboard> previous = new HashMap<>();
    private BukkitTask task;

    TestingHud(MobaPlugin plugin, LoadoutStore store, Function<Player, Player> dummyOf) {
        this.plugin = plugin;
        this.store = store;
        this.dummyOf = dummyOf;
    }

    boolean showing(Player p) { return previous.containsKey(p.getUniqueId()); }

    void start(Player p) {
        if (showing(p)) return;
        previous.put(p.getUniqueId(), p.getScoreboard());
        p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        refresh(p);
        if (task == null) task = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, PERIOD, PERIOD);
    }

    void stop(Player p) {
        var board = previous.remove(p.getUniqueId());
        if (board == null) return;
        var mine = p.getScoreboard();
        var obj = mine.getObjective(OBJECTIVE);
        if (obj != null) obj.unregister();
        p.setScoreboard(board);
        if (previous.isEmpty() && task != null) { task.cancel(); task = null; }
    }

    private void refreshAll() {
        for (UUID id : List.copyOf(previous.keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) { previous.remove(id); continue; }
            refresh(p);
        }
        if (previous.isEmpty() && task != null) { task.cancel(); task = null; }
    }

    /** The lines as they would be drawn now, for assertions. */
    List<String> linesFor(Player p) {
        var book = store.book(p.getUniqueId());
        var dummy = dummyOf.apply(p);
        return TestingHudLines.lines(book.selectedName(), book.current().count(),
                vitals("You", p), dummy == null || !dummy.isOnline() ? null : vitals("Dummy", dummy));
    }

    private TestingHudLines.Vitals vitals(String label, Player who) {
        double capacity = plugin.effectiveMaxHealth(who);
        double health = capacity > 0 ? Vitals.toEffective(who.getHealth(), capacity) : who.getHealth();
        double max = capacity > 0 ? capacity : who.getHealth();
        var armor = who.getAttribute(Attribute.ARMOR);
        var toughness = who.getAttribute(Attribute.ARMOR_TOUGHNESS);
        return new TestingHudLines.Vitals(label, health, max, armor == null ? 0 : armor.getValue(), toughness == null ? 0 : toughness.getValue());
    }

    private void refresh(Player p) {
        Scoreboard board = p.getScoreboard();
        var obj = board.getObjective(OBJECTIVE);
        if (obj == null) obj = board.registerNewObjective(OBJECTIVE, Criteria.DUMMY, ChatColor.AQUA + "" + ChatColor.BOLD + "TESTING");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (String entry : board.getEntries()) board.resetScores(entry);
        var lines = linesFor(p);
        int score = lines.size();
        for (String line : lines) obj.getScore(line).setScore(score--);
    }
}
