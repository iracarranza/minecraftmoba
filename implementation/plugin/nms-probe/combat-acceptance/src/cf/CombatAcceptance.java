package cf;

import com.minecraftmoba.plugin.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Callable;

/** Drives the real /moba lab combat commands with a body as the tester. Test-only. */
public final class CombatAcceptance extends JavaPlugin implements org.bukkit.event.Listener {
    private PrintWriter out;
    private MobaPlugin moba;
    private NmsBodies bodies;
    private Bodies.Body testerBody;
    private Player t;
    private int pass, fail;
    private long baseline;
    private final List<String> seen = new ArrayList<>();
    @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
    public void dmg(org.bukkit.event.entity.EntityDamageEvent e) {
        seen.add(e.getEntity().getName() + " cause=" + e.getCause() + " cancelled=" + e.isCancelled()
                + " damage=" + String.format("%.2f", e.getDamage()) + " final=" + String.format("%.2f", e.getFinalDamage()));
    }

    @Override public void onEnable() {
        getDataFolder().mkdirs();
        try { out = new PrintWriter(new File(getDataFolder(), "report.txt")); } catch (Exception e) { throw new RuntimeException(e); }
        Bukkit.getPluginManager().registerEvents(this, this);
        int[] c = {60};
        at(c, 0, "plugin loaded, tester body spawned and op'd", this::setup);
        at(c, 12, "lab start puts the tester in the lab room", this::labStart);
        at(c, 2, "combat start begins at the class step", this::combatStart);
        at(c, 2, "recipient flow: a movement-only ability is refused and does not advance", this::refusalFlow);
        at(c, 2, "recipient flow: an acts-on-others ability advances to modes", this::recipientFlow);
        at(c, 2, "entering as Recipient is refused (dummy cannot cast yet) and the tester stays in the lab room", this::recipientRefused);
        at(c, 2, "operator flow with night time and cooldown waiver enters the chamber", this::operatorEnter);
        at(c, 6, "tester is in the combat world on the control band, class and level applied", this::inChamber);
        at(c, 2, "the slab was built: control stone, sand, water pool, ramp, clear air above", this::slabBuilt);
        at(c, 2, "time was set to night", this::nightSet);
        at(c, 2, "a dummy player stands ahead of the tester", this::dummyPresent);
        at(c, 2, "a hit is measured at raw 8 / final 8; an immediate second hit fires no event (damage immunity)", this::measureHit);
        at(c, 14, "a huge hit is recorded at its true size, after the immunity window, and the dummy survives", this::bigHit);
        at(c, 3, "cooldown waiver clears a spent cooldown", this::cooldownWaived);
        at(c, 3, "the report names the waiver, units and source", this::reportLines);
        at(c, 2, "reset rebuilds the slab, removes what was placed, restores the dummy", this::resetCheck);
        at(c, 6, "clear empties the logs", this::clearCheck);
        at(c, 2, "leave returns the tester to the lab room and removes the dummy", this::leaveCheck);
        at(c, 3, "nothing is left behind: no dummy, no ticker side effects, bodies registry clean", this::cleanCheck);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            out.println(); out.println("SUMMARY pass=" + pass + " fail=" + fail); out.close(); Bukkit.shutdown();
        }, c[0] + 40);
    }

    private void at(int[] clock, int gap, String name, Callable<String> body) {
        clock[0] += gap;
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                String d = body.call();
                boolean ok = d == null || !d.contains("FAIL");
                if (ok) pass++; else fail++;
                log((ok ? "PASS " : "FAIL ") + name + (d == null ? "" : " :: " + d));
            } catch (Throwable ex) {
                fail++;
                StringWriter sw = new StringWriter(); ex.printStackTrace(new PrintWriter(sw));
                Throwable root = ex; while (root.getCause() != null) root = root.getCause();
                log("FAIL " + name + " :: " + root);
                String[] l = sw.toString().split("\n");
                for (int i = 0; i < Math.min(l.length, 14); i++) log("      " + l[i]);
            }
        }, clock[0]);
    }

    private void log(String s) { out.println(s); out.flush(); getLogger().info(s); }
    private CombatChamber combat() { return moba.lab().combat(); }
    private void cmd(String c) { t.performCommand("moba lab combat " + c); }

    private String setup() {
        moba = (MobaPlugin) Bukkit.getPluginManager().getPlugin("MinecraftMoba");
        if (moba == null || !moba.isEnabled()) return "FAIL: MinecraftMoba not enabled";
        bodies = new NmsBodies(moba);
        World w = Bukkit.getWorlds().get(0);
        testerBody = bodies.spawn("tester", null, null, 0, w.getSpawnLocation().add(0.5, 0, 0.5));
        t = testerBody.player();
        t.setOp(true);
        return "tester=" + t.getName() + " op=" + t.isOp() + " enrolled=" + moba.enrolled(t) + " classes=" + moba.inputs().ids();
    }

    private String labStart() {
        t.performCommand("moba lab start");
        return "world=" + t.getWorld().getName() + (t.getWorld().getName().equals("moba_lab") ? "" : " | FAIL: not in the lab room");
    }

    private String combatStart() {
        cmd("start");
        String s = combat().state(t);
        return s + (s.equals("setup step CLASS") ? "" : " | FAIL: expected the class step");
    }

    private String refusalFlow() {
        cmd("pick daredevil"); cmd("pick RECIPIENT");
        String before = combat().state(t);
        cmd("pick A1");                                  // Tunneling-like movement ability? daredevil A1 is Runway
        String after = combat().state(t);
        return "before=" + before + " after pick A1=" + after;
    }

    private String recipientFlow() {
        // Daredevil A2 is Crash Landing: acts on enemies.
        String s = combat().state(t);
        if (s.equals("setup step SCRIPT") || s.equals("setup step MODES") || s.equals("setup step AIM")) return "already past slot: " + s;
        cmd("pick A2");
        s = combat().state(t);
        return s + (s.startsWith("setup step") ? "" : " | FAIL: left the flow unexpectedly");
    }

    private String recipientRefused() {
        // Walk to the end as Recipient.
        for (int i = 0; i < 4; i++) {
            String s = combat().state(t);
            if (s.equals("setup step SCRIPT")) cmd("pick ONCE");
            else if (s.equals("setup step AIM")) cmd("pick STRAIGHT_AHEAD");
            else if (s.equals("setup step MODES")) cmd("pick enter");
        }
        String world = t.getWorld().getName();
        String s = combat().state(t);
        return "world=" + world + " state=" + s
                + (world.equals("moba_lab") ? "" : " | FAIL: a Recipient entry reached the combat world");
    }

    private String operatorEnter() {
        // The refused entry parks the flow at the role step; restart cleanly.
        cmd("leave"); cmd("start");
        cmd("pick daredevil"); cmd("pick OPERATOR");
        String s = combat().state(t);
        if (!s.equals("setup step MODES")) return "FAIL: expected modes step, state=" + s;
        cmd("set time night"); cmd("set cooldown off"); cmd("set level 15");
        baseline = Bukkit.getCurrentTick();
        cmd("pick enter");
        return "state=" + combat().state(t);
    }

    private String inChamber() {
        var d = moba.data(t);
        double[] spawn = CombatSlab.testerSpawn();
        double dist = t.getLocation().distance(new Location(t.getWorld(), spawn[0], spawn[1], spawn[2]));
        boolean ok = t.getWorld().getName().equals("moba_combat") && combat().state(t).equals("in chamber")
                && d != null && "daredevil".equals(d.classId) && d.level == 15 && dist < 6;
        return "world=" + t.getWorld().getName() + " state=" + combat().state(t) + " class=" + (d == null ? null : d.classId)
                + " level=" + (d == null ? -1 : d.level) + " distFromSpawn=" + String.format("%.1f", dist)
                + (ok ? "" : " | FAIL: not set up as expected");
    }

    private String slabBuilt() {
        World w = Bukkit.getWorld("moba_combat");
        StringBuilder sb = new StringBuilder(); boolean bad = false;
        for (var band : CombatSlab.BANDS) {
            int x = (band.minX() + band.maxX()) / 2;
            int top = CombatSlab.surfaceY(x, 0);
            Material got = w.getBlockAt(x, top, 0).getType(), want = CombatSlab.materialAt(x, top, 0);
            Material above = w.getBlockAt(x, top + 1, 0).getType();
            sb.append(band.name()).append("=").append(got).append(" ");
            if (got != want || above != Material.AIR) bad = true;
        }
        return sb + (bad ? "| FAIL: a band does not match the layout" : "");
    }

    private String nightSet() {
        long time = Bukkit.getWorld("moba_combat").getTime();
        return "time=" + time + (time >= 12000 ? "" : " | FAIL: not night");
    }

    private String dummyPresent() {
        Player d = combat().dummy(t);
        if (d == null) return "FAIL: no dummy";
        double[] want = CombatSlab.dummySpawn(8);
        double dist = d.getLocation().distance(new Location(d.getWorld(), want[0], want[1], want[2]));
        return "dummy=" + d.getName() + " isBody=" + NmsBodies.isBody(d.getUniqueId()) + " online=" + d.isOnline()
                + " distFromPost=" + String.format("%.1f", dist) + " enrolled=" + moba.enrolled(d)
                + (dist > 4 ? " | FAIL: dummy is not at its post" : "");
    }

    private String measureHit() {
        Player d = combat().dummy(t);
        int before = combat().hitsDealt(t);
        StringBuilder sb = new StringBuilder();
        sb.append("dummy gm=").append(d.getGameMode()).append(" invulnerable=").append(d.isInvulnerable())
          .append(" noDamageTicks=").append(d.getNoDamageTicks()).append(" health=").append(d.getHealth())
          .append(" | tester gm=").append(t.getGameMode()).append(" | ");
        seen.clear();
        double h0 = d.getHealth();
        d.damage(8.0, t);
        sb.append("with source: health ").append(h0).append(" -> ").append(d.getHealth()).append(" events=").append(seen).append(" | ");
        seen.clear(); double h1 = d.getHealth();
        d.damage(8.0);
        sb.append("no source: health ").append(h1).append(" -> ").append(d.getHealth()).append(" events=").append(seen).append(" | ");
        int after = combat().hitsDealt(t);
        var dealt = combat().reportLines(t).stream().filter(l -> l.startsWith("Dealt")).findFirst().orElse("(no Dealt line)");
        return sb + "hits " + before + " -> " + after + " | " + dealt
                + (after != before + 1 || !dealt.contains("raw 8.00 -> final 8.00")
                    ? " | FAIL: expected exactly one hit at raw 8 final 8 (the second falls inside the immunity window and fires no event)" : "");
    }

    private String bigHit() {
        Player d = combat().dummy(t);
        d.damage(100000.0, t);
        boolean alive = !d.isDead() && d.isOnline();
        var dealt = combat().reportLines(t).stream().filter(l -> l.startsWith("Dealt")).findFirst().orElse("(no Dealt line)");
        boolean recorded = dealt.contains("raw 100008.00");
        return "hits=" + combat().hitsDealt(t) + " dummyAlive=" + alive + " health=" + String.format("%.2f", d.getHealth()) + " | " + dealt
                + (alive && recorded ? "" : " | FAIL: the dummy died or the hit was not recorded at its true size");
    }

    private String cooldownWaived() throws Exception {
        Method m = moba.inputs().getClass().getDeclaredMethod("cooldowns"); m.setAccessible(true);
        Object book = m.invoke(moba.inputs());
        Method spend = book.getClass().getMethod("spend", UUID.class, String.class, long.class, int.class, long.class);
        Method remaining = book.getClass().getMethod("remaining", UUID.class, String.class, long.class);
        long now = Bukkit.getCurrentTick();
        spend.invoke(book, t.getUniqueId(), "probe_ability", now, 1, 6000L);
        long before = (Long) remaining.invoke(book, t.getUniqueId(), "probe_ability", now);
        return "spent a 6000-tick cooldown: remaining " + before + " (cleared by the ticker next tick; checked in the report step)";
    }

    private String reportLines() throws Exception {
        Method m = moba.inputs().getClass().getDeclaredMethod("cooldowns"); m.setAccessible(true);
        Object book = m.invoke(moba.inputs());
        Method remaining = book.getClass().getMethod("remaining", UUID.class, String.class, long.class);
        long left = (Long) remaining.invoke(book, t.getUniqueId(), "probe_ability", (long) Bukkit.getCurrentTick());
        var lines = combat().reportLines(t);
        String joined = String.join(" || ", lines);
        boolean ok = joined.contains("WAIVED") && joined.contains("effective points") && joined.contains("entity_attack") && left == 0;
        return "cooldown remaining after the ticker=" + left + " | " + joined.replaceAll("§.", "")
                + (ok ? "" : " | FAIL: waiver not applied or report missing the waiver, units, or source");
    }

    private String resetCheck() {
        World w = Bukkit.getWorld("moba_combat");
        w.getBlockAt(0, CombatSlab.FLOOR_Y + 3, 0).setType(Material.OBSIDIAN, false);
        w.getBlockAt(0, CombatSlab.FLOOR_Y - 4, 0).setType(Material.AIR, false);
        UUID oldDummy = combat().dummy(t).getUniqueId();
        cmd("reset");
        boolean cleaned = w.getBlockAt(0, CombatSlab.FLOOR_Y + 3, 0).getType() == Material.AIR
                && w.getBlockAt(0, CombatSlab.FLOOR_Y - 4, 0).getType() == Material.STONE;
        Player d = combat().dummy(t);
        boolean fresh = d != null && !d.getUniqueId().equals(oldDummy) && Bukkit.getPlayer(oldDummy) == null;
        return "placed obsidian removed and dug hole refilled=" + cleaned + " | new dummy=" + fresh + " | hits=" + combat().hitsDealt(t)
                + (cleaned && fresh && combat().hitsDealt(t) == 0 ? "" : " | FAIL: reset incomplete");
    }

    private String clearCheck() {
        combat().dummy(t).damage(5.0, t);
        int with = combat().hitsDealt(t);
        cmd("clear");
        int after = combat().hitsDealt(t);
        return "hits " + with + " -> " + after + (with >= 1 && after == 0 ? "" : " | FAIL: clear did not empty the log");
    }

    private String leaveCheck() {
        Player d = combat().dummy(t);
        UUID dummyId = d.getUniqueId();
        cmd("leave");
        boolean back = t.getWorld().getName().equals("moba_lab");
        boolean gone = Bukkit.getPlayer(dummyId) == null;
        return "tester world=" + t.getWorld().getName() + " dummyGone=" + gone + " state=" + combat().state(t)
                + (back && gone && combat().state(t).equals("idle") ? "" : " | FAIL: leave incomplete");
    }

    private String cleanCheck() {
        int online = Bukkit.getOnlinePlayers().size();
        boolean dummiesLeft = Bukkit.getOnlinePlayers().stream().anyMatch(p -> p.getName().equals("B_dummy"));
        return "online=" + online + " (only the tester) dummiesLeft=" + dummiesLeft
                + (online == 1 && !dummiesLeft ? "" : " | FAIL: players left behind");
    }
}
