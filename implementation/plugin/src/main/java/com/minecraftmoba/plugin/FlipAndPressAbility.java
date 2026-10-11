package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import java.util.*;

final class FlipAndPressAbility implements Ability {
    private record Flip(Player caster, LivingEntity target, int slamAt, int expires) {}
    private final MobaPlugin plugin;
    private final ConfigurationSection config;
    private final ChefCooking cooking;
    private final List<Flip> flips = new ArrayList<>();
    private int clock;
    FlipAndPressAbility(MobaPlugin plugin, ConfigurationSection config, ChefCooking cooking) {
        this.plugin = plugin; this.config = config; this.cooking = cooking;
    }
    public String id() { return "flip_and_press"; }
    public String displayName() { return "Flip and Press"; }
    public long cooldownTicks() { return config.getLong("cooldownTicks"); }
    public Map<String,String> branches() { return Map.of("hibachi_skills", "Hibachi Skills", "rake_the_coals", "Rake the Coals", "broiler_rack", "Broiler Rack"); }
    public List<String> branchIds() { return List.of("hibachi_skills", "rake_the_coals", "broiler_rack"); }
    public List<AbilityOutput> outputs() {
        return AbilityOutput.of(new AbilityOutput("flip", TargetForm.UNIT_ENTITY, InputForm.INSTANT, Recipients.ENEMIES),
                new AbilityOutput("press", TargetForm.UNIT_BLOCK, InputForm.INSTANT, Recipients.NONE));
    }
    public boolean execute(Player p, AbilityContext ctx) {
        Entity entity = ctx.entity(); Block block = ctx.block();
        // An air activation (including a lab body) resolves its aim once. Carried clicks win.
        if (entity == null && block == null) {
            var hit = AbilityTargeting.ray(p, p.getEyeLocation(), p.getEyeLocation().getDirection(),
                    config.getDouble("reach"), config.getDouble("rayWidth"), AbilityTargeting.Relation.ENEMY);
            if (hit != null) { entity = hit.entity(); block = hit.block(); }
        }
        double multiplier = ChefItems.spatula(p, plugin) ? config.getDouble("spatulaMultiplier") : 1;
        if (entity instanceof LivingEntity target && Targetability.status(target, p)
                && target.getWorld().equals(p.getWorld()) && target.getLocation().distanceSquared(p.getLocation()) <= Math.pow(config.getDouble("reach") + 1, 2)) {
            double lift = config.getDouble("lift") * multiplier;
            int delay = config.getInt("slamDelayTicks");
            if ("hibachi_skills".equals(ctx.branchFor(id()))) { lift *= config.getDouble("hibachiMultiplier"); delay *= 2; }
            target.setVelocity(target.getVelocity().setY(lift));
            flips.add(new Flip(p, target, clock + delay, clock + delay + config.getInt("landingTimeoutTicks")));
            return true;
        }
        if (block != null && block.getWorld().equals(p.getWorld())
                && block.getLocation().distanceSquared(p.getLocation()) <= Math.pow(config.getDouble("reach") + 1, 2)) {
            double fraction = "rake_the_coals".equals(ctx.branchFor(id())) ? config.getDouble("fuelFraction") : 1;
            double batch = "broiler_rack".equals(ctx.branchFor(id())) ? config.getDouble("batchPerItem") : 0;
            return cooking.advance(block, (int) Math.round(config.getInt("advanceTicks") * multiplier),
                    fraction, batch, config.getDouble("batchCap")) > 0;
        }
        p.sendMessage("Flip and Press needs an enemy or an actively cooking station."); return false;
    }
    public void tick() {
        clock++;
        for (var f : List.copyOf(flips)) {
            if (!f.caster().isOnline() || f.caster().isDead() || f.target().isDead() || !f.target().isValid()
                    || !f.caster().getWorld().equals(f.target().getWorld())) { flips.remove(f); continue; }
            if (clock == f.slamAt()) f.target().setVelocity(new Vector(0, -config.getDouble("slamSpeed"), 0));
            if (clock >= f.slamAt() && (f.target().isOnGround() || clock >= f.expires())) {
                AbilityDamage.deal(f.target(), config.getDouble("damage"), f.caster());
                if (plugin.stun() != null) plugin.stun().stun(f.target(), config.getLong("stunTicks"));
                flips.remove(f);
            }
        }
    }
    public void cancel(Player p) { flips.removeIf(f -> f.caster().equals(p) || f.target().equals(p)); }
}
