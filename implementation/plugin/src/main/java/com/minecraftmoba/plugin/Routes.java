package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import java.util.*;

/**
 * Routes: Banner A, travel, Banner B.
 *
 * infrastructure.md: the endpoints are the durable semantic identity of a
 * Route; the demonstrated traversal is *evidence used to establish it*, not the
 * Route itself. The recorded path is therefore kept as support and simplified,
 * and nothing here claims a block-by-block corridor.
 *
 * The effect follows the canon rule "movement efficiency before movement
 * potency": a recognized Route reduces sprint exhaustion, so the same distance
 * costs less Hunger. It does not add speed. Movement potency is named in
 * classes.md as a later and more cautious axis and is not implemented.
 *
 * Unresolved in canon and therefore config or refusal here: bidirectionality,
 * how much geometry is retained, how effect is calculated, whether a better
 * demonstration upgrades an existing Route, and the exact designate input.
 *
 * Disable with features.routes.enabled.
 */
public final class Routes implements Listener {
    public record Route(String id, UUID world, Location a, Location b, List<Location> path) {}

    private static final class Pending {
        final Location start; final List<Location> path = new ArrayList<>(); final long startedAt;
        Pending(Location start, long startedAt) { this.start = start; this.startedAt = startedAt; }
    }

    private final MobaPlugin plugin;
    private final Map<UUID, Pending> pending = new HashMap<>();
    private final List<Route> routes = new ArrayList<>();

    public Routes(MobaPlugin plugin) {
        this.plugin = plugin;
        long period = plugin.getConfig().getLong("features.routes.effectTicks", 20L);
        if (period > 0) Bukkit.getScheduler().runTaskTimer(plugin, this::applyEffects, period, period);
    }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.routes.enabled"); }
    public boolean hasPending(Player p) { return pending.containsKey(p.getUniqueId()); }
    public void discardPending(Player p) { pending.remove(p.getUniqueId()); }
    public List<Route> routes() { return Collections.unmodifiableList(routes); }

    private boolean isBanner(Block b) { return Tag.BANNERS.isTagged(b.getType()); }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (!enabled() || e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;
        Player p = e.getPlayer();
        if (plugin.infraMode() == null || !plugin.infraMode().isActive(p)) return;
        Block b = e.getClickedBlock();
        if (!isBanner(b)) return;
        // In Infrastructure Mode a Banner is a designation target, not furniture.
        e.setCancelled(true);

        Pending current = pending.get(p.getUniqueId());
        if (current == null) {
            pending.put(p.getUniqueId(), new Pending(b.getLocation(), b.getWorld().getFullTime()));
            p.sendActionBar(ChatColor.AQUA + "Endpoint A set — travel to another Banner");
            return;
        }
        if (current.start.getBlock().equals(b)) {
            p.sendActionBar(ChatColor.YELLOW + "Same Banner — travel to a different one");
            return;
        }
        String refusal = evaluate(current, b.getLocation());
        if (refusal != null) {
            p.sendActionBar(ChatColor.RED + "Not established: " + refusal);
        } else {
            var route = new Route("route_" + routes.size(), b.getWorld().getUID(),
                    current.start, b.getLocation(), simplify(current.path));
            routes.add(route);
            p.sendActionBar(ChatColor.GREEN + "Route established (" + route.path().size() + " samples)");
        }
        pending.remove(p.getUniqueId());
    }

    /** Returns a refusal reason, or null when the demonstration is acceptable. */
    private String evaluate(Pending pendingRoute, Location end) {
        if (!Objects.equals(pendingRoute.start.getWorld(), end.getWorld())) return "different world";
        double straight = pendingRoute.start.distance(end);
        double min = plugin.getConfig().getDouble("features.routes.minLength", 16.0);
        if (straight < min) return "endpoints too close (" + (int) straight + " < " + (int) min + ")";
        if (pendingRoute.path.isEmpty()) return "no traversal demonstrated";
        long maxTicks = plugin.getConfig().getLong("features.routes.maxDesignationTicks", 24000L);
        if (end.getWorld().getFullTime() - pendingRoute.startedAt > maxTicks) return "designation expired";
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (!enabled()) return;
        Pending current = pending.get(e.getPlayer().getUniqueId());
        if (current == null || e.getTo() == null) return;
        double step = plugin.getConfig().getDouble("features.routes.sampleEveryBlocks", 4.0);
        Location last = current.path.isEmpty() ? current.start : current.path.get(current.path.size() - 1);
        if (last.getWorld() != e.getTo().getWorld() || last.distance(e.getTo()) < step) return;
        current.path.add(e.getTo().clone());
        int max = plugin.getConfig().getInt("features.routes.maxSamples", 512);
        if (current.path.size() > max) {
            pending.remove(e.getPlayer().getUniqueId());
            e.getPlayer().sendActionBar(ChatColor.RED + "Designation abandoned: path too long");
        }
    }

    /** Coarse retention only: canon leaves how much geometry a Route keeps open. */
    private List<Location> simplify(List<Location> path) {
        int keepEvery = Math.max(1, plugin.getConfig().getInt("features.routes.keepEverySample", 2));
        var out = new ArrayList<Location>();
        for (int i = 0; i < path.size(); i += keepEvery) out.add(path.get(i));
        return out;
    }

    public boolean nearRoute(Location loc) {
        double r = plugin.getConfig().getDouble("features.routes.corridorRadius", 6.0);
        for (Route route : routes) {
            if (!route.world().equals(loc.getWorld().getUID())) continue;
            if (route.a().distance(loc) <= r || route.b().distance(loc) <= r) return true;
            for (Location sample : route.path()) if (sample.distance(loc) <= r) return true;
        }
        return false;
    }

    /**
     * Sprint efficiency, not speed. Saturation is restored slightly while
     * sprinting on a Route, so the same distance costs less Hunger.
     */
    private void applyEffects() {
        if (!enabled() || routes.isEmpty()) return;
        double relief = plugin.getConfig().getDouble("features.routes.exhaustionRelief", 0.1);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.isSprinting() || !nearRoute(p.getLocation())) continue;
            p.setExhaustion((float) Math.max(0.0, p.getExhaustion() - relief));
        }
    }

    public List<String> report() {
        var out = new ArrayList<String>();
        for (Route r : routes)
            out.add("ROUTE " + r.id() + " a=" + fmt(r.a()) + " b=" + fmt(r.b()) + " samples=" + r.path().size());
        out.add("ROUTE_TOTALS routes=" + routes.size() + " pending=" + pending.size());
        return out;
    }

    private static String fmt(Location l) { return l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ(); }
}
