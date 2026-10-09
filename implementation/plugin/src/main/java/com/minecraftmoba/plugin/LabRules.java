package com.minecraftmoba.plugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Per-tester world rules for the lab: freeze hunger, stop regeneration.
 *
 * Both exist so a measurement is not contaminated by the player's own state. A bench
 * that watches a health bar for ten minutes should not also be watching hunger
 * drain, and an ability's damage should not be partly healed back between hits.
 *
 * <ul>
 * <li><b>Hunger frozen</b>: food level never goes DOWN. Eating still works.</li>
 * <li><b>Regeneration off</b>: no natural or satiated healing, vanilla or the
 *     plugin's own {@link HungerRegen}, which consults {@link #regenOff}.</li>
 * </ul>
 *
 * A tester's rules are cleared when the lab session ends or they leave, so a flag
 * set in one session never silently follows them into a real match.
 */
public final class LabRules implements Listener {
    private final Set<UUID> hungerFrozen = new HashSet<>();
    private final Set<UUID> regenOff = new HashSet<>();

    public boolean hungerFrozen(Player p) { return hungerFrozen.contains(p.getUniqueId()); }
    public boolean regenOff(Player p) { return regenOff.contains(p.getUniqueId()); }

    public void hungerFrozen(Player p, boolean on) { set(hungerFrozen, p.getUniqueId(), on); }
    public void regenOff(Player p, boolean on) { set(regenOff, p.getUniqueId(), on); }

    /** Forget a tester's rules. */
    public void clear(Player p) { hungerFrozen.remove(p.getUniqueId()); regenOff.remove(p.getUniqueId()); }

    public void clearAll() { hungerFrozen.clear(); regenOff.clear(); }

    private static void set(Set<UUID> s, UUID id, boolean on) { if (on) s.add(id); else s.remove(id); }

    public String describe(Player p) {
        return "hunger " + (hungerFrozen(p) ? "FROZEN" : "normal") + ", regeneration " + (regenOff(p) ? "OFF" : "normal");
    }

    /** Whether a food change is a loss, which is the only direction a freeze stops. */
    static boolean isLoss(int current, int proposed) { return proposed < current; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void food(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p && hungerFrozen(p) && isLoss(p.getFoodLevel(), e.getFoodLevel()))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void regain(EntityRegainHealthEvent e) {
        if (!(e.getEntity() instanceof Player p) || !regenOff(p)) return;
        var why = e.getRegainReason();
        if (why == EntityRegainHealthEvent.RegainReason.SATIATED || why == EntityRegainHealthEvent.RegainReason.REGEN)
            e.setCancelled(true);
    }

    @EventHandler public void quit(PlayerQuitEvent e) { clear(e.getPlayer()); }
}
