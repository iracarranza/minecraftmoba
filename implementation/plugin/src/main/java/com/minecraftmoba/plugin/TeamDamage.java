package com.minecraftmoba.plugin;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.projectiles.ProjectileSource;

/**
 * No friendly fire, as ONE rule in one place.
 *
 * <h2>What "no friendly fire" actually means</h2>
 *
 * [RESOLVED 27 September 2026] <b>No authored damage across teams, while
 * incidental and systemic damage still applies.</b> TNT, suffocation and the
 * like do hit allies.
 *
 * So Sinkhole's restore-suffocation keeps its positioning cost for the
 * caster's own team, and Toolbox's dispensed TNT is not made safe for
 * teammates. That is not an exception list -- it is the same authored-versus-
 * systemic line the whole Toolbox design is built on, where a class can be
 * extraordinarily lethal without any ability saying "deal X damage". One rule
 * derived from a principle, rather than two rules that happen to agree.
 *
 * <h2>Why it is here rather than in each ability</h2>
 *
 * {@code objectives.md} is explicit that this must be one rule in one place
 * and names the defect: the lunge sweep already implements its own team check,
 * "which is how combat state started". Per-ability checks drift, and each new
 * ability has to remember a rule it cannot see.
 *
 * <b>Summons inherit the same answer</b> rather than each deciding, which is
 * what {@link #owner} is for: an arrow, a fireball or a pet resolves to the
 * player responsible for it, and the same comparison then runs once.
 */
public final class TeamDamage implements Listener {

    private final MobaPlugin plugin;

    public TeamDamage(MobaPlugin plugin) { this.plugin = plugin; }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("features.noFriendlyFire.enabled", true);
    }

    /**
     * Damage somebody chose to deal.
     *
     * A melee swing, a sweep, a fired projectile, a thrown potion. These are a
     * player pointing at a target, and pointing at an ally is what the rule
     * forbids.
     *
     * Everything absent from this set is systemic: explosions, suffocation,
     * fall, fire, drowning, contact. Those still hit allies, deliberately, and
     * the omission is the rule rather than an oversight -- which is why this
     * is an allow-list of authored causes rather than a deny-list of
     * incidental ones. A new incidental cause added by Minecraft is systemic
     * by default, which is the safe direction to fail in.
     */
    static boolean authored(DamageCause cause) {
        return switch (cause) {
            case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK, PROJECTILE, MAGIC, THORNS -> true;
            default -> false;
        };
    }

    /**
     * Who is responsible for a damaging entity.
     *
     * Walks one step from a projectile to its shooter, or from a tamed animal
     * to its owner, so a summon inherits its summoner's team rather than
     * deciding for itself. Deliberately ONE step: a chain of ownership is a
     * different question, and guessing at it is how a rule stops being
     * predictable.
     */
    static Player owner(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player p) return p;
        }
        if (damager instanceof Tameable tameable && tameable.getOwner() instanceof Player p) return p;
        return null;
    }

    /**
     * HIGHEST rather than MONITOR, because this cancels.
     *
     * MONITOR is for observers and must not change the outcome; a rule that
     * decides whether damage lands belongs before the observers that read it.
     * CombatState reads damage at MONITOR and would otherwise count a hit that
     * never happened as combat.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void damage(EntityDamageByEntityEvent event) {
        if (!enabled()) return;
        if (!authored(event.getCause())) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        Player source = owner(event.getDamager());
        if (source == null || source.equals(victim)) return;
        if (!Targetability.allied(victim, source)) return;

        event.setCancelled(true);
    }
}
