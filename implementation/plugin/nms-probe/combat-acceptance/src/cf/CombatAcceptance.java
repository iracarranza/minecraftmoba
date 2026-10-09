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
        at(c, 2, "entering as Recipient (Crash Landing, enemy dummy) reaches the combat world", this::recipientRefused);
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
        at(c, 4, "recipient: a Mole dummy enters, is the class, and is enrolled", this::recipientEnter);
        at(c, 14, "recipient: commanded cast runs the real ability and the report counts it", this::recipientCast);
        at(c, 3, "recipient: a second command is the ability's own recast (Drill Rush emerges)", this::recipientRecast);
        at(c, 3, "recipient: a passive slot is refused with a reason", this::passiveRefused);
        at(c, 6, "recipient: leave removes the dummy caster", this::recipientLeave);
        at(c, 4, "toolbox: a Toolbox dummy enters (the belt was null at ability construction)", this::toolboxEnter);
        at(c, 14, "toolbox: Jumpstartinator (A2) actually executes", this::toolboxCast);
        at(c, 3, "toolbox: leave", this::recipientLeave);
        at(c, 4, "ghost: operate as Mole with the waiver, then start recording", this::ghostRecordStart);
        at(c, 6, "ghost: the tester's first press runs the real Drill Rush", this::ghostPressOne);
        at(c, 12, "ghost: a second press (the recast) emerges", this::ghostPressTwo);
        at(c, 3, "ghost: stop saves a take of two inputs", this::ghostStop);
        at(c, 3, "ghost: replay starts a Mole ghost", this::ghostReplay);
        at(c, 75, "ghost: the ghost ran the real Drill Rush and recast it, from inputs alone", this::ghostResult);
        at(c, 3, "ghost: leave", this::recipientLeave);
        at(c, 4, "ui: entering puts the menu on the hotbar, Leave in the inventory, and builds the control deck", this::uiEnter);
        at(c, 4, "ui: Operator sees 'Dummy casts' greyed with a reason", this::uiCastGreyed);
        at(c, 3, "ui: with ability mode on, right-click is the ability's: the menu does not act", this::uiModeGuard);
        at(c, 3, "ui: using the Record item opens its page", this::uiOpenRecord);
        at(c, 3, "ui: Start recording on the hotbar starts a take", this::uiStartRecording);
        at(c, 6, "ui: the tester's real casts are recorded while the menu is on the hotbar", this::uiPerform);
        at(c, 14, "ui: the STOP RECORDING world button saves the take", this::uiStopByButton);
        at(c, 3, "ui: the GHOST ONCE world button starts the ghost", this::uiReplayByButton);
        at(c, 75, "ui: the ghost reproduced the take", this::uiGhostResult);
        at(c, 3, "ui: the LEAVE world button leaves; hotbar restored, deck removed", this::uiLeave);
        at(c, 4, "interference: record a two-input Mole take as Operator", this::intRecordStart);
        at(c, 6, "interference: the tester performs it", this::intPerform);
        at(c, 14, "interference: the take is two inputs", this::intStop);
        at(c, 3, "interference: a looping ghost starts and casts", this::intReplay);
        at(c, 50, "interference: the ghost is casting from its script", this::intCasting);
        at(c, 10, "interference: striking the ghost stands it down (STRUCK)", this::intStruck);
        at(c, 60, "interference: it stays passive while still in combat (no casts)", this::intPassive);
        at(c, 130, "interference: out of combat it resumes and restarts its script", this::intResumed);
        at(c, 3, "interference: turning interference off, a strike no longer stands it down", this::intIgnoreOn);
        at(c, 12, "interference: ...and it carried on", this::intIgnoreResult);
        at(c, 3, "interference: back on, a stun stands it down (STUNNED)", this::intStun);
        at(c, 8, "interference: reported as stunned", this::intStunResult);
        at(c, 3, "interference: leave", this::recipientLeave);
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
                + (world.equals("moba_combat") ? "" : " | FAIL: the Recipient entry did not reach the combat world");
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

    private String recipientEnter() {
        cmd("leave"); cmd("start");
        cmd("pick mole"); cmd("pick RECIPIENT");
        String s0 = combat().state(t);
        cmd("pick A2");
        for (int i = 0; i < 4; i++) {
            String s = combat().state(t);
            if (s.equals("setup step SCRIPT")) cmd("pick ONCE");
            else if (s.equals("setup step AIM")) cmd("pick STRAIGHT_AHEAD");
            else if (s.equals("setup step MODES")) { cmd("set cooldown off"); cmd("set level 15"); cmd("pick enter"); }
        }
        Player d = combat().dummy(t);
        if (d == null) return "FAIL: no dummy; state=" + combat().state(t) + " (at role step: " + s0 + ")";
        var dd = moba.data(d);
        boolean ok = t.getWorld().getName().equals("moba_combat") && dd != null && "mole".equals(dd.classId) && dd.level == 15;
        return "world=" + t.getWorld().getName() + " dummyClass=" + (dd == null ? null : dd.classId) + " level=" + (dd == null ? -1 : dd.level)
                + (ok ? "" : " | FAIL: not entered as a Recipient with a Toolbox dummy");
    }

    private String recipientCast() {
        Player d = combat().dummy(t);
        var inputs = moba.inputs();
        int before = inputs.executions(d, "drill_rush");
        seen.clear();
        cmd("cast");
        int after = inputs.executions(d, "drill_rush");
        var line = combat().reportLines(t).stream().filter(l -> l.startsWith("Dummy casts")).findFirst().orElse("(no cast line)");
        return "executions " + before + " -> " + after + " | " + line + " | events=" + seen
                + (after == before + 1 && line.contains("1 ran") && combat().dummy(t).isInvulnerable() ? "" : " | FAIL: the real ability did not run exactly once");
    }

    private String recipientRecast() {
        Player d = combat().dummy(t);
        boolean heldBefore = d.isInvulnerable();
        cmd("cast");
        var line = combat().reportLines(t).stream().filter(l -> l.startsWith("Dummy casts")).findFirst().orElse("(no cast line)");
        return "invulnerable " + heldBefore + " -> " + d.isInvulnerable() + " | " + line
                + (heldBefore && !d.isInvulnerable() ? "" : " | FAIL: the second command did not recast (emerge)");
    }

    private String passiveRefused() {
        cmd("leave");
        cmd("start"); cmd("pick mole"); cmd("pick RECIPIENT");
        String s = combat().state(t);
        cmd("pick PASSIVE");
        String after = combat().state(t);
        return "before=" + s + " after picking passive=" + after
                + (after.equals(s) ? "" : " | FAIL: a passive advanced as Recipient");
    }

    private String recipientLeave() {
        cmd("leave");
        boolean none = Bukkit.getOnlinePlayers().stream().noneMatch(p -> p.getName().equals("B_dummy"));
        return "dummiesGone=" + none + " state=" + combat().state(t) + (none ? "" : " | FAIL: dummy left behind");
    }

    private String toolboxEnter() {
        cmd("leave"); cmd("start"); cmd("pick toolbox"); cmd("pick RECIPIENT"); cmd("pick A2");
        for (int i = 0; i < 4; i++) {
            String s = combat().state(t);
            if (s.equals("setup step SCRIPT")) cmd("pick ONCE");
            else if (s.equals("setup step AIM")) cmd("pick STRAIGHT_AHEAD");
            else if (s.equals("setup step MODES")) { cmd("set cooldown off"); cmd("set level 15"); cmd("pick enter"); }
        }
        Player d = combat().dummy(t);
        return d == null ? "FAIL: no dummy; state=" + combat().state(t) : "dummy class=" + moba.data(d).classId + " belt=" + (moba.utilityBelt() != null);
    }

    private String toolboxCast() {
        Player d = combat().dummy(t);
        int before = moba.inputs().executions(d, "jumpstartinator");
        cmd("cast");
        int after = moba.inputs().executions(d, "jumpstartinator");
        return "executions " + before + " -> " + after + (after == before + 1 ? "" : " | FAIL: Jumpstartinator did not execute");
    }

    private String ghostRecordStart() {
        cmd("leave"); cmd("start"); cmd("pick mole"); cmd("pick OPERATOR");
        cmd("set cooldown off"); cmd("set level 15"); cmd("pick enter");
        if (!combat().state(t).equals("in chamber")) return "FAIL: not in chamber: " + combat().state(t);
        cmd("record start");
        return "recording; class=" + moba.data(t).classId;
    }

    private String ghostPressOne() {
        var in = moba.inputs();
        int before = in.executions(t, "drill_rush");
        in.input(t, in.modeInput());
        in.input(t, in.inputFor("a2"));
        int after = in.executions(t, "drill_rush");
        return "tester drill_rush " + before + " -> " + after + " invulnerable=" + t.isInvulnerable()
                + (after == before + 1 ? "" : " | FAIL: the tester's own cast did not run");
    }

    private String ghostPressTwo() {
        var in = moba.inputs();
        boolean activeBefore = in.abilityFor("mole", "a2").active(t);
        double[] sp = CombatSlab.testerSpawn();
        var loc = t.getLocation();
        in.input(t, in.inputFor("a2"));
        return "activeBefore=" + activeBefore + " activeAfter=" + in.abilityFor("mole", "a2").active(t)
                + " pos=" + String.format("%.1f,%.1f,%.1f", loc.getX(), loc.getY(), loc.getZ()) + " yaw=" + loc.getYaw() + " pitch=" + loc.getPitch()
                + (in.abilityFor("mole", "a2").active(t) ? " | FAIL: the recast did not end the burrow" : "");
    }

    private String ghostStop() {
        cmd("record stop");
        var line = combat().reportLines(t).stream().filter(l -> l.startsWith("Take:")).findFirst().orElse("(no take line)");
        return line + (line.contains("2 input(s)") && line.contains("mole") ? "" : " | FAIL: expected a take of two inputs as mole");
    }

    private String ghostReplay() {
        cmd("replay");
        Player d = combat().dummy(t);
        return "ghost class=" + (d == null ? null : moba.data(d).classId) + (d != null && "mole".equals(moba.data(d).classId) ? "" : " | FAIL: no mole ghost");
    }

    private String ghostResult() {
        Player d = combat().dummy(t);
        int ran = moba.inputs().executions(d, "drill_rush");
        var line = combat().reportLines(t).stream().filter(l -> l.startsWith("Dummy casts")).findFirst().orElse("(no cast line)");
        boolean ok = ran == 1 && !d.isInvulnerable() && line.contains("2 ran, 0 refused");
        return "ghost drill_rush executions=" + ran + " invulnerable=" + d.isInvulnerable() + " | " + line
                + (ok ? "" : " | FAIL: the ghost did not reproduce the take");
    }

    private String itemId(int slot) {
        var st = t.getInventory().getItem(slot);
        if (st == null || !st.hasItemMeta()) return null;
        return st.getItemMeta().getPersistentDataContainer().get(new org.bukkit.NamespacedKey(moba, "combat_item"),
                org.bukkit.persistence.PersistentDataType.STRING);
    }

    private org.bukkit.event.player.PlayerInteractEvent rightClick(int heldSlot) {
        t.getInventory().setHeldItemSlot(heldSlot);
        var e = new org.bukkit.event.player.PlayerInteractEvent(t, org.bukkit.event.block.Action.RIGHT_CLICK_AIR,
                t.getInventory().getItemInMainHand(), null, org.bukkit.block.BlockFace.SELF, org.bukkit.inventory.EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(e);
        return e;
    }

    private boolean pressButton(String verb) {
        for (var i : Bukkit.getWorld("moba_combat").getEntitiesByClass(org.bukkit.entity.Interaction.class))
            if (i.getScoreboardTags().contains("verb:" + verb)) {
                Bukkit.getPluginManager().callEvent(new org.bukkit.event.player.PlayerInteractEntityEvent(t, i));
                return true;
            }
        return false;
    }

    private String uiEnter() {
        cmd("leave"); cmd("start"); cmd("pick mole"); cmd("pick OPERATOR");
        cmd("set cooldown off"); cmd("set level 15"); cmd("pick enter");
        World w = Bukkit.getWorld("moba_combat");
        long buttons = w.getEntitiesByClass(org.bukkit.entity.Interaction.class).stream()
                .filter(i -> i.getScoreboardTags().contains("combat_button")).count();
        var deck = w.getBlockAt(-8, CombatSlab.FLOOR_Y, -10).getType();
        var first = w.getBlockAt(-7, CombatSlab.FLOOR_Y + 1, -19).getType();
        boolean ok = "record".equals(itemId(1)) && "replay".equals(itemId(2)) && "return".equals(itemId(9))
                && buttons == 11 && deck == Material.POLISHED_ANDESITE && first == Material.PURPLE_CONCRETE;
        return "slot0=" + itemId(0) + " slot1=" + itemId(1) + " slot2=" + itemId(2) + " slot9=" + itemId(9)
                + " buttons=" + buttons + " deck=" + deck + " firstButton=" + first
                + (ok ? "" : " | FAIL: menu, leave item or control deck missing");
    }

    private String uiCastGreyed() {
        var st = t.getInventory().getItem(0);
        boolean greyed = st != null && st.getType() == Material.GRAY_DYE;
        String lore = st == null || st.lore() == null ? "" : st.lore().toString();
        return "slot0 material=" + (st == null ? null : st.getType()) + (greyed && lore.contains("caster") ? "" : " | FAIL: expected greyed with a reason; lore=" + lore);
    }

    private String uiOpenRecord() {
        var e = rightClick(1);
        boolean ok = "record.start".equals(itemId(0)) && "record.stop".equals(itemId(1)) && e.isCancelled();
        return "slot0=" + itemId(0) + " slot1=" + itemId(1) + " slot8=" + itemId(8) + " cancelled=" + e.isCancelled()
                + (ok ? "" : " | FAIL: the Record page did not open");
    }

    private String uiStartRecording() {
        var e = rightClick(0);
        // The recorder is private to the chamber; its effect is that a later stop saves a take.
        return "used record.start, cancelled=" + e.isCancelled() + (e.isCancelled() ? "" : " | FAIL: the menu press was not consumed");
    }

    private String uiModeGuard() {
        // At the root, slot 5 is Reset. If the menu acted the dummy would be replaced.
        var in = moba.inputs();
        var dummyBefore = combat().dummy(t).getUniqueId();
        int before = in.executions(t, "drill_rush");
        in.input(t, in.modeInput());
        boolean on = in.active(t);
        rightClick(5);
        in.input(t, in.inputFor("a2"));            // emerge again, so the take starts clean
        in.exit(t, true);
        boolean sameDummy = combat().dummy(t).getUniqueId().equals(dummyBefore);
        int after = in.executions(t, "drill_rush");
        return "abilityModeOn=" + on + " dummyUntouched=" + sameDummy + " abilityCast " + before + " -> " + after
                + (on && sameDummy && after == before + 1 ? "" : " | FAIL: with ability mode on the click must be the ability's alone");
    }

    private String uiPerform() {
        var in = moba.inputs();
        in.input(t, in.modeInput());
        in.input(t, in.inputFor("a2"));
        Bukkit.getScheduler().runTaskLater(this, () -> in.input(t, in.inputFor("a2")), 10);
        return "tester drill_rush executions=" + in.executions(t, "drill_rush");
    }

    private String uiStopByButton() {
        boolean pressed = pressButton("record.stop");
        var line = combat().reportLines(t).stream().filter(l -> l.startsWith("Take:")).findFirst().orElse("(no take line)");
        return "pressed=" + pressed + " | " + line
                + (pressed && line.contains("2 input(s)") ? "" : " | FAIL: expected a take of two inputs from the real casts");
    }

    private String uiReplayByButton() {
        boolean pressed = pressButton("replay.once");
        Player d = combat().dummy(t);
        return "pressed=" + pressed + " ghost=" + (d == null ? null : moba.data(d).classId)
                + (pressed && d != null && "mole".equals(moba.data(d).classId) ? "" : " | FAIL: no ghost");
    }

    private String uiGhostResult() {
        Player d = combat().dummy(t);
        int ran = moba.inputs().executions(d, "drill_rush");
        return "ghost drill_rush executions=" + ran + " invulnerable=" + d.isInvulnerable()
                + (ran == 1 && !d.isInvulnerable() ? "" : " | FAIL: the ghost did not reproduce the take");
    }

    private String uiLeave() {
        boolean pressed = pressButton("leave");
        World w = Bukkit.getWorld("moba_combat");
        long buttons = w.getEntitiesByClass(org.bukkit.entity.Interaction.class).stream()
                .filter(i -> i.getScoreboardTags().contains("combat_button")).count();
        boolean ok = pressed && t.getWorld().getName().equals("moba_lab") && itemId(0) == null && itemId(9) == null
                && buttons == 0 && w.getBlockAt(-8, CombatSlab.FLOOR_Y, -10).getType() == Material.AIR;
        return "pressed=" + pressed + " world=" + t.getWorld().getName() + " slot0=" + itemId(0) + " slot9=" + itemId(9)
                + " buttons=" + buttons + (ok ? "" : " | FAIL: leave incomplete");
    }

    private int mark;
    private String ghostLine() {
        return combat().reportLines(t).stream().filter(l -> l.startsWith("Ghost:")).findFirst().orElse("(no ghost line)");
    }

    /** Strike the ghost on the first tick it is not burrowed (an invulnerable ghost takes no damage). */
    private void strikeWhenVulnerable() {
        Player d = combat().dummy(t);
        var task = new org.bukkit.scheduler.BukkitTask[1];
        int[] tries = {60};
        task[0] = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!d.isInvulnerable() || tries[0]-- <= 0) { d.damage(1.0, t); task[0].cancel(); }
        }, 0L, 1L);
    }

    private String intRecordStart() {
        cmd("leave"); cmd("start"); cmd("pick mole"); cmd("pick OPERATOR");
        cmd("set cooldown off"); cmd("set level 15"); cmd("pick enter");
        cmd("record start");
        return "state=" + combat().state(t);
    }

    private String intPerform() {
        var in = moba.inputs();
        in.input(t, in.modeInput()); in.input(t, in.inputFor("a2"));
        Bukkit.getScheduler().runTaskLater(this, () -> in.input(t, in.inputFor("a2")), 10);
        return "cast";
    }

    private String intStop() {
        cmd("record stop");
        var line = combat().reportLines(t).stream().filter(l -> l.startsWith("Take:")).findFirst().orElse("(no take)");
        return line + (line.contains("2 input(s)") ? "" : " | FAIL: expected two inputs");
    }

    private String intReplay() { cmd("replay loop"); return "ghost=" + combat().dummy(t).getName(); }

    private String intCasting() {
        int ran = moba.inputs().executions(combat().dummy(t), "drill_rush");
        strikeWhenVulnerable();
        return "ghost drill_rush executions=" + ran + " | " + ghostLine()
                + (ran >= 1 && ghostLine().contains("SCRIPTING") ? "" : " | FAIL: the ghost was not casting from its script");
    }

    private String intStruck() {
        mark = moba.inputs().executions(combat().dummy(t), "drill_rush");
        var line = ghostLine();
        return line + " | executions=" + mark + (line.contains("INTERFERED by STRUCK") ? "" : " | FAIL: a strike did not stand the ghost down");
    }

    private String intPassive() {
        int now = moba.inputs().executions(combat().dummy(t), "drill_rush");
        var line = ghostLine();
        return "executions " + mark + " -> " + now + " | " + line
                + (now == mark && line.contains("INTERFERED") ? "" : " | FAIL: it cast, or resumed, while still in combat");
    }

    private String intResumed() {
        int now = moba.inputs().executions(combat().dummy(t), "drill_rush");
        var line = ghostLine();
        return "executions " + mark + " -> " + now + " | " + line
                + (now > mark && line.contains("SCRIPTING") && line.contains("interfered 1 time") ? "" : " | FAIL: it did not resume and restart");
    }

    private String intIgnoreOn() {
        cmd("interference");
        mark = moba.inputs().executions(combat().dummy(t), "drill_rush");
        strikeWhenVulnerable();
        return "ignoring now; " + ghostLine();
    }

    private String intIgnoreResult() {
        var line = ghostLine();
        int now = moba.inputs().executions(combat().dummy(t), "drill_rush");
        return "executions " + mark + " -> " + now + " | " + line
                + (line.contains("IGNORED") && line.contains("SCRIPTING") && line.contains("interfered 1 time") && now > mark
                    ? "" : " | FAIL: an ignoring ghost must carry on when struck");
    }

    private String intStun() {
        cmd("interference");
        moba.stun().stun(combat().dummy(t), 60);
        return "stunned";
    }

    private String intStunResult() {
        var line = ghostLine();
        return line + (line.contains("INTERFERED by STUNNED") ? "" : " | FAIL: a stun did not stand the ghost down");
    }
}
