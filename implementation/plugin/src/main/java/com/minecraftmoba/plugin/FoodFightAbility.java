package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import java.util.*;

final class FoodFightAbility implements Ability {
    private record Flight(Player caster, ItemStack food, String branch, ItemDisplay display,
                          Vector velocity, double range, double[] travelled) {}
    private final MobaPlugin plugin;
    private final ConfigurationSection config;
    private final ChefCooking cooking;
    private final NamespacedKey ration;
    private final List<Flight> flights = new ArrayList<>();
    FoodFightAbility(MobaPlugin plugin, ConfigurationSection config, ChefCooking cooking) {
        this.plugin = plugin; this.config = config; this.cooking = cooking;
        ration = new NamespacedKey(plugin, "recklessly_rationed");
    }
    public String id() { return "food_fight"; }
    public String displayName() { return "Food Fight"; }
    public long cooldownTicks() { return config.getLong("cooldownTicks"); }
    public Map<String,String> branches() { return Map.of("super_nutritious", "Super Nutritious", "extra_spicy", "Extra Spicy", "reckless_rationing", "Reckless Rationing"); }
    public List<String> branchIds() { return List.of("super_nutritious", "extra_spicy", "reckless_rationing"); }
    public List<AbilityOutput> outputs() {
        return AbilityOutput.single("food_fight", TargetForm.DIRECTION, InputForm.INSTANT, Recipients.ENEMIES);
    }
    public List<AbilityOutput> outputs(Player p, AbilityContext c) {
        return AbilityOutput.single("food_fight", TargetForm.DIRECTION, InputForm.INSTANT,
                "super_nutritious".equals(c.branchFor(id())) ? Recipients.BOTH : Recipients.ENEMIES);
    }
    public boolean execute(Player p, AbilityContext ctx) {
        int slot = ChefItems.foodSlot(p);
        if (slot < 0) { p.sendMessage("Food Fight needs food in your inventory."); return false; }
        var stack = p.getInventory().getItem(slot); var food = stack.asOne();
        p.getInventory().setItem(slot, stack.getAmount() == 1 ? null : stack.asQuantity(stack.getAmount() - 1));
        var start = p.getEyeLocation();
        var display = start.getWorld().spawn(start, ItemDisplay.class, d -> {
            d.setItemStack(food); d.setPersistent(false); d.setViewRange(1);
        });
        double range = config.getDouble("range") * (ChefItems.spatula(p, plugin) ? config.getDouble("spatulaRangeMultiplier") : 1);
        flights.add(new Flight(p, food, ctx.branchFor(id()), display,
                start.getDirection().multiply(config.getDouble("speed")), range, new double[1]));
        return true;
    }
    public void tick() {
        for (var f : List.copyOf(flights)) {
            if (!f.caster().isOnline() || f.caster().isDead() || !f.display().isValid()
                    || !f.caster().getWorld().equals(f.display().getWorld())) { finish(f, false); continue; }
            Location start = f.display().getLocation(); double distance = f.velocity().length();
            if (distance < 1e-6) { finish(f, true); continue; }
            var hit = start.getWorld().rayTrace(start, f.velocity().clone().normalize(), distance,
                    FluidCollisionMode.NEVER, true, config.getDouble("hitRadius"), e -> e instanceof LivingEntity
                            && !e.equals(f.caster()) && (Targetability.status(e, f.caster())
                            || "super_nutritious".equals(f.branch()) && Targetability.allied(e, f.caster())));
            if (hit != null) {
                f.display().teleport(hit.getHitPosition().toLocation(start.getWorld()));
                if (hit.getHitEntity() instanceof LivingEntity target) {
                    if (Targetability.allied(target, f.caster())) ChefItems.heal(target, target instanceof Player ally ? Vitals.scaleHealing(config.getDouble("heal"), plugin.effectiveMaxHealth(ally)) : config.getDouble("heal"));
                    else {
                        AbilityDamage.deal(target, config.getDouble("damage"), f.caster());
                        if ("extra_spicy".equals(f.branch())) target.setFireTicks(Math.max(target.getFireTicks(), config.getInt("fireTicks")));
                    }
                    finish(f, false);
                } else {
                    var block = hit.getHitBlock();
                    boolean wasActive = block != null && cooking.active(block);
                    if (block != null && cooking.load(block, f.food())) {
                        if (wasActive && "reckless_rationing".equals(f.branch()) && !ChefItems.tagged(f.food(), ration))
                            block.getWorld().dropItem(block.getLocation().add(.5, 1.1, .5), ChefItems.ration(f.food(), ration));
                        finish(f, false);
                    } else finish(f, true);
                }
                continue;
            }
            f.display().teleport(start.add(f.velocity()));
            f.travelled()[0] += distance; f.velocity().setY(f.velocity().getY() - config.getDouble("gravity"));
            if (f.travelled()[0] >= f.range()) finish(f, true);
        }
    }
    private void finish(Flight f, boolean drop) {
        if (drop && f.display().isValid()) f.display().getWorld().dropItem(f.display().getLocation(), f.food());
        f.display().remove(); flights.remove(f);
    }
    public void cancel(Player p) { for (var f : List.copyOf(flights)) if (f.caster().equals(p)) finish(f, false); }
}
