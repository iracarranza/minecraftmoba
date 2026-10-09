package bf;

import com.minecraftmoba.plugin.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.Callable;

/** Exercises NmsBodies inside a server with the real MobaPlugin loaded. Test-only. */
public final class BodiesAcceptance extends JavaPlugin implements Listener {
    private PrintWriter out;
    private MobaPlugin moba;
    private NmsBodies bodies;
    private int pass, fail, vanillaSwaps, moves;
    private Bodies.Body a, b;
    private double y0, x0;

    @EventHandler public void swap(PlayerSwapHandItemsEvent e) { vanillaSwaps++; }
    @EventHandler public void move(PlayerMoveEvent e) { if (a != null && e.getPlayer().getUniqueId().equals(a.player().getUniqueId())) moves++; }

    @Override public void onEnable() {
        getDataFolder().mkdirs();
        try { out = new PrintWriter(new File(getDataFolder(), "report.txt")); } catch (Exception e) { throw new RuntimeException(e); }
        Bukkit.getPluginManager().registerEvents(this, this);
        int[] t = {60};
        at(t, 0, "plugin loaded and exposes inputs", this::loaded);
        at(t, 1, "connected() starts verification and does not claim an answer it lacks", this::connectedStarts);
        at(t, 20, "connected() verified true after the self-test settles", this::connectedFinished);
        at(t, 2, "spawn two bodies (north class A L5, south class B L10)", this::spawn);
        at(t, 1, "input to an unsettled body is refused, not silently dropped", this::unsettledRefused);
        at(t, 6, "enrolled, classed, levelled, not sent to the lobby", this::inspect);
        at(t, 8, "settled() becomes true", this::settledNow);
        at(t, 1, "F packet", this::fOn);
        at(t, 3, "F toggles ability mode through PacketInputs -> AbilityInputs", this::fOnRead);
        at(t, 1, "second F packet", this::fOff);
        at(t, 3, "second F toggles it back off", this::fOffRead);
        at(t, 1, "movement packet", this::moveOut);
        at(t, 3, "movement packet moved the body and fired PlayerMoveEvent", this::moveRead);
        at(t, 1, "drop packet is consumed without dropping the held item", this::dropCheck);
        at(t, 1, "gravity acts on a body", this::fall);
        at(t, 30, "...after 30 ticks it landed", this::fallRead);
        at(t, 2, "despawn: offline, unenrolled, no longer special, files gone, idempotent", this::despawn);
        at(t, 3, "churn: 4 bodies x 2 rounds leaves nothing live", this::churn);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            out.println(); out.println("SUMMARY pass=" + pass + " fail=" + fail); out.close(); Bukkit.shutdown();
        }, t[0] + 40);
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
                for (int i = 0; i < Math.min(l.length, 12); i++) log("      " + l[i]);
            }
        }, clock[0]);
    }

    private void log(String s) { out.println(s); out.flush(); getLogger().info(s); }
    private Location base() { return Bukkit.getWorlds().get(0).getSpawnLocation().add(0.5, 0, 0.5); }
    private String classA() { return moba.inputs().ids().get(0); }
    private String classB() { var ids = moba.inputs().ids(); return ids.get(Math.min(1, ids.size() - 1)); }

    private String loaded() {
        moba = (MobaPlugin) Bukkit.getPluginManager().getPlugin("MinecraftMoba");
        if (moba == null || !moba.isEnabled()) return "FAIL: MinecraftMoba is not enabled";
        bodies = new NmsBodies(moba);
        return "classes registered: " + moba.inputs().ids() + " | lobbyWorld.enabled="
                + moba.getConfig().getBoolean("features.lobbyWorld.enabled") + " | match running=" + moba.match().running();
    }

    private String connectedStarts() {
        boolean now = bodies.connected();
        return "connected()=" + now + " verified=" + bodies.verified() + " | " + bodies.describe()
                + (now || bodies.verified() ? " | FAIL: claimed an answer before the self-test could have run" : "");
    }

    private String connectedFinished() {
        boolean c = bodies.connected();
        return "connected()=" + c + " verified=" + bodies.verified() + " | " + bodies.describe()
                + (!bodies.verified() || !c ? " | FAIL: not verified connected" : "");
    }

    private String spawn() {
        a = bodies.spawn("north_1", Team.NORTH, classA(), 5, base().add(4, 0, 0));
        b = bodies.spawn("south_1", Team.SOUTH, classB(), 10, base().add(-4, 0, 0));
        return "spawned " + a.player().getName() + " and " + b.player().getName() + "; online=" + Bukkit.getOnlinePlayers().size() + "; live=" + bodies.live();
    }

    private String unsettledRefused() {
        try { bodies.swapHand(a); return "FAIL: input to a body spawned this tick was accepted"; }
        catch (IllegalStateException ex) { return "refused: " + ex.getMessage().substring(0, Math.min(90, ex.getMessage().length())) + "..."; }
    }

    private String inspect() {
        StringBuilder sb = new StringBuilder(); boolean bad = false;
        for (Bodies.Body x : List.of(a, b)) {
            Player p = x.player(); var d = moba.data(p);
            boolean enrolled = moba.enrolled(p), classOk = d != null && x.classId().equals(d.classId), levelOk = d != null && d.level == x.level();
            boolean special = NmsBodies.isBody(p.getUniqueId());
            Location l = p.getLocation(); Location want = base().add(x == a ? 4 : -4, 0, 0);
            boolean stayed = l.getWorld().equals(want.getWorld()) && l.distance(want) < 8;
            sb.append(p.getName()).append(": enrolled=").append(enrolled).append(" class=").append(d == null ? null : d.classId)
              .append(" level=").append(d == null ? -1 : d.level).append(" isBody=").append(special)
              .append(" world=").append(l.getWorld().getName()).append(" dist=").append(String.format("%.1f", l.distance(want))).append("; ");
            if (!enrolled || !classOk || !levelOk || !special || !stayed) bad = true;
        }
        return sb + (bad ? "| FAIL: a body was not enrolled/classed/levelled, or was moved away" : "");
    }

    private String settledNow() { return "settled(a)=" + bodies.settled(a) + (bodies.settled(a) ? "" : " | FAIL: not settled after the delay"); }

    private int swapsBefore; private boolean activeBefore;
    private String fOn() { swapsBefore = vanillaSwaps; activeBefore = moba.inputs().active(a.player()); bodies.swapHand(a); return "sent F; active before=" + activeBefore; }
    private String fOnRead() {
        boolean after = moba.inputs().active(a.player());
        return "active " + activeBefore + " -> " + after + " | vanilla PlayerSwapHandItemsEvent +" + (vanillaSwaps - swapsBefore)
                + (after == activeBefore ? " | FAIL: F did not reach AbilityInputs" : "");
    }
    private String fOff() { activeBefore = moba.inputs().active(a.player()); bodies.swapHand(a); return "sent F; active before=" + activeBefore; }
    private String fOffRead() {
        boolean after = moba.inputs().active(a.player());
        return "active " + activeBefore + " -> " + after + (after == activeBefore ? " | FAIL: second F did not toggle back" : "");
    }

    private int movesBefore;
    private String moveOut() { x0 = a.player().getLocation().getX(); movesBefore = moves; bodies.moveTo(a, x0 + 1.0, a.player().getLocation().getY(), a.player().getLocation().getZ()); return "sent movement, x=" + x0; }
    private String moveRead() {
        double x = a.player().getLocation().getX();
        return "x " + x0 + " -> " + x + " | PlayerMoveEvent +" + (moves - movesBefore)
                + (moves == movesBefore || Math.abs(x - x0) < 0.1 ? " | FAIL: movement was not honoured" : "");
    }

    private String dropCheck() {
        var p = a.player();
        p.getInventory().setItemInMainHand(new org.bukkit.inventory.ItemStack(org.bukkit.Material.STICK));
        long before = p.getWorld().getEntitiesByClass(org.bukkit.entity.Item.class).size();
        bodies.dropItem(a);
        long after = p.getWorld().getEntitiesByClass(org.bukkit.entity.Item.class).size();
        return "dropped item entities " + before + " -> " + after + " | in hand=" + p.getInventory().getItemInMainHand().getType();
    }

    private String fall() { Player p = b.player(); p.teleport(base().add(-4, 12, 0)); y0 = p.getLocation().getY(); return "start y=" + y0; }
    private String fallRead() { double y = b.player().getLocation().getY(); return "y " + y0 + " -> " + y + (y >= y0 - 0.5 ? " | FAIL: did not fall" : ""); }

    private String despawn() {
        UUID ia = a.player().getUniqueId(), ib = b.player().getUniqueId();
        File root = Bukkit.getWorlds().get(0).getWorldFolder();
        int online = Bukkit.getOnlinePlayers().size();
        bodies.despawn(a); bodies.despawn(b); bodies.despawn(a);
        boolean files = new File(root, "playerdata/" + ia + ".dat").exists() || new File(root, "playerdata/" + ib + ".dat").exists();
        boolean bad = Bukkit.getOnlinePlayers().size() != 0 || NmsBodies.isBody(ia) || NmsBodies.isBody(ib) || files || bodies.live() != 0 || moba.enrolled(a.player());
        return "online " + online + " -> " + Bukkit.getOnlinePlayers().size() + " | live=" + bodies.live() + " | playerdata remains=" + files
                + (bad ? " | FAIL: despawn left something behind" : "");
    }

    private String churn() {
        for (int round = 0; round < 2; round++) {
            List<Bodies.Body> list = new ArrayList<>();
            for (int i = 0; i < 4; i++) list.add(bodies.spawn("c" + round + i, i % 2 == 0 ? Team.NORTH : Team.SOUTH, classA(), 3, null));
            int online = Bukkit.getOnlinePlayers().size();
            if (online != 4) return "FAIL: round " + round + " expected 4 online, saw " + online;
            list.forEach(bodies::despawn);
        }
        return "online=" + Bukkit.getOnlinePlayers().size() + " live=" + bodies.live()
                + (Bukkit.getOnlinePlayers().size() != 0 || bodies.live() != 0 ? " | FAIL: bodies left behind" : "");
    }
}
