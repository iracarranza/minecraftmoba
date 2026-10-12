package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.potion.*;
import java.util.*;

/** Fixed-windup pounce, swept travel damage, and species-specific branch scaling. */
final class LungeAbility implements Ability, org.bukkit.event.Listener {
    private record Sweep(Player player, String branch, int until, Set<UUID> struck) {}
    private record Windup(Player player, AbilityContext context, int until, PotionEffect previous) {}
    private final MobaPlugin plugin; private final ConfigurationSection config;
    private final List<Sweep> sweeps = new ArrayList<>();
    private final Map<UUID,Windup> windups = new HashMap<>(); private int clock;
    private record Disabled(org.bukkit.inventory.ItemStack item, int until) {}
    private final Map<UUID,Disabled> disabled = new HashMap<>();
    LungeAbility(MobaPlugin plugin, ConfigurationSection config) { this.plugin = plugin; this.config = config; }
    public String id() { return "lunge"; }
    public String displayName() { return "Lunge"; }
    public long cooldownTicks() { return config.getLong("cooldownTicks"); }
    public List<AbilityOutput> outputs() {
        return AbilityOutput.single("lunge", TargetForm.DIRECTION, InputForm.INSTANT, Recipients.ENEMIES);
    }
    public Map<String,String> branches() { return Map.of("swarming_bite","Swarming Bite", "thieving_swipe","Thieving Swipe", "stalking_pounce","Stalking Pounce"); }
    public List<String> branchIds() { return List.of("swarming_bite", "thieving_swipe", "stalking_pounce"); }
    private ConfigurationSection branch(String id) { return config.getConfigurationSection("branches").getConfigurationSection(id); }
    public long rechargeTicks(Player p, AbilityContext ctx) {
        if (!"swarming_bite".equals(ctx.branchFor(id()))) return cooldownTicks();
        int wolves = AnimalSenses.nearby(p, config.getDouble("animalRadius")).wolves();
        return Math.max(1, Math.round(cooldownTicks() / (1 + Math.min(4, wolves) * branch("swarmingBite").getDouble("rechargePerWolf"))));
    }
    public boolean execute(Player p, AbilityContext ctx) {
        String selected = ctx.branchFor(id());
        if ("stalking_pounce".equals(selected)) {
            if (windups.containsKey(p.getUniqueId())) return false;
            int ticks = branch("stalkingPounce").getInt("windupTicks");
            windups.put(p.getUniqueId(), new Windup(p, ctx, clock + ticks, p.getPotionEffect(PotionEffectType.SLOWNESS)));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks + 1, 4, false, false, true));
        } else launch(p, selected);
        return true;
    }
    private void launch(Player p, String selected) {
        double power = config.getDouble("power");
        if ("swarming_bite".equals(selected)) power = branch("swarmingBite").getDouble("power");
        if ("thieving_swipe".equals(selected)) power = branch("thievingSwipe").getDouble("power");
        if ("stalking_pounce".equals(selected)) power = branch("stalkingPounce").getDouble("power");
        p.setVelocity(p.getVelocity().add(p.getLocation().getDirection().multiply(power)));
        sweeps.add(new Sweep(p, selected, clock + config.getInt("sweepTicks"), new HashSet<>()));
    }
    public void tick() {
        clock++;
        disabled.values().removeIf(d -> d.until() <= clock);
        for (var w : List.copyOf(windups.values())) if (clock >= w.until()) {
            windups.remove(w.player().getUniqueId()); restoreSlow(w);
            if (w.player().isOnline() && !w.player().isDead() && plugin.data(w.player()) == w.context().playerData())
                launch(w.player(), "stalking_pounce");
        }
        for (var s : List.copyOf(sweeps)) {
            if (clock >= s.until() || !s.player().isOnline() || s.player().isDead()) { sweeps.remove(s); continue; }
            double radius = config.getDouble("damageRadius");
            for (var target : AbilityTargeting.around(s.player(), s.player(), radius, radius, radius, AbilityTargeting.Relation.ENEMY, false))
                if (s.struck().add(target.getUniqueId())) {
                    AbilityDamage.deal(target, config.getDouble("damage"), s.player());
                    if ("thieving_swipe".equals(s.branch()) && target instanceof Player victim) {
                        var b = branch("thievingSwipe"); var item = victim.getInventory().getItemInMainHand();
                        int foxes = AnimalSenses.nearby(s.player(), config.getDouble("animalRadius")).foxes();
                        int duration = Math.min(b.getInt("disableCapTicks"), b.getInt("disableTicks") + foxes * b.getInt("perFoxTicks"));
                        if (!item.isEmpty()) {
                            victim.setCooldown(item.getType(), Math.max(victim.getCooldown(item.getType()), duration));
                            disabled.put(victim.getUniqueId(), new Disabled(item.clone(), clock + duration));
                        }
                    }
                }
        }
    }
    private void restoreSlow(Windup w) {
        var current = w.player().getPotionEffect(PotionEffectType.SLOWNESS);
        if (current != null && current.getAmplifier() == 4 && current.getDuration() <= branch("stalkingPounce").getInt("windupTicks") + 1) {
            w.player().removePotionEffect(PotionEffectType.SLOWNESS);
            if (w.previous() != null) {
                int remaining = w.previous().getDuration() - Math.max(0, branch("stalkingPounce").getInt("windupTicks") - (w.until() - clock));
                if (remaining > 0) w.player().addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, remaining,
                        w.previous().getAmplifier(), w.previous().isAmbient(), w.previous().hasParticles(), w.previous().hasIcon()));
            }
        }
    }
    public void cancel(Player p) {
        var w = windups.remove(p.getUniqueId()); if (w != null) restoreSlow(w);
        sweeps.removeIf(s -> s.player().equals(p));
    }
    public void forget(Player p) { cancel(p); disabled.remove(p.getUniqueId()); }
    private boolean blocked(Player p) {
        var d = disabled.get(p.getUniqueId());
        return d != null && d.until() > clock && p.getInventory().getItemInMainHand().isSimilar(d.item());
    }
    @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void attack(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p && blocked(p)) e.setCancelled(true);
    }
    @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void use(org.bukkit.event.player.PlayerInteractEvent e) { if (blocked(e.getPlayer())) e.setCancelled(true); }
    @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void mine(org.bukkit.event.block.BlockDamageEvent e) { if (blocked(e.getPlayer())) e.setCancelled(true); }
}
