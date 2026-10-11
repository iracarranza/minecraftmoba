package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Lightable;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.*;

/** Real workstation inputs, outputs, progress and paid fuel; no persistent speed buff. */
final class ChefCooking {
    private final MobaPlugin plugin;
    ChefCooking(MobaPlugin plugin) { this.plugin = plugin; }

    CookingRecipe<?> recipe(Block b, ItemStack input) {
        if (input == null || input.isEmpty() || !input.getType().isEdible()) return null;
        var recipes = Bukkit.recipeIterator();
        while (recipes.hasNext()) {
            Recipe r = recipes.next();
            boolean kind = switch (b.getType()) {
                case FURNACE -> r instanceof FurnaceRecipe;
                case SMOKER -> r instanceof SmokingRecipe;
                case CAMPFIRE, SOUL_CAMPFIRE -> r instanceof CampfireRecipe;
                default -> false;
            };
            if (kind && r instanceof CookingRecipe<?> c && c.getInputChoice().test(input)) return c;
        }
        return null;
    }

    boolean load(Block b, ItemStack food) {
        var recipe = recipe(b, food); if (recipe == null) return false;
        if (b.getState() instanceof Furnace f) {
            var inv = f.getInventory(); var current = inv.getSmelting();
            if (current == null || current.isEmpty()) { inv.setSmelting(food.asOne()); return true; }
            if (current.isSimilar(food) && current.getAmount() < current.getMaxStackSize()) {
                current.setAmount(current.getAmount() + 1); inv.setSmelting(current); return true;
            }
        } else if (b.getState() instanceof Campfire c && b.getBlockData() instanceof Lightable l && l.isLit()) {
            for (int i = 0; i < c.getSize(); i++) if (c.getItem(i) == null || c.getItem(i).isEmpty()) {
                c.setItem(i, food.asOne()); c.setCookTime(i, 0); c.setCookTimeTotal(i, recipe.getCookingTime());
                c.startCooking(i); return c.update(false, false);
            }
        }
        return false;
    }

    boolean active(Block b) {
        if (b.getState() instanceof Furnace f)
            return f.getBurnTime() > 0 && recipe(b, f.getInventory().getSmelting()) != null;
        if (b.getState() instanceof Campfire c && b.getBlockData() instanceof Lightable l && l.isLit())
            for (int i = 0; i < c.getSize(); i++)
                if (!c.isCookingDisabled(i) && recipe(b, c.getItem(i)) != null) return true;
        return false;
    }

    int advance(Block b, int ticks, double fuelFraction, double batchPerItem, double batchCap) {
        if (!active(b)) return 0;
        if (b.getState() instanceof Furnace f) {
            var inv = f.getSnapshotInventory(); var input = inv.getSmelting(); var r = recipe(b, input);
            if (r == null) return 0;
            int budget = (int) Math.round(ticks * Math.min(batchCap, 1 + Math.max(0, input.getAmount() - 1) * batchPerItem));
            int advanced = 0;
            // Each completion rechecks output, events and fuel. Bound by budget and inventory size.
            while (budget > 0 && input != null && !input.isEmpty()) {
                ItemStack output = inv.getResult(), result = r.getResult();
                if (output != null && !output.isEmpty()
                        && (!output.isSimilar(result) || output.getAmount() + result.getAmount() > output.getMaxStackSize())) break;
                if (f.getBurnTime() <= 0 && !ignite(b, f)) break;
                int need = r.getCookingTime() - f.getCookTime();
                var step = CookingAdvance.advance(1, f.getCookTime(), r.getCookingTime(),
                        budget, Math.max(0, f.getBurnTime()), fuelFraction);
                if (step.productiveTicks() == 0) break;
                // An event refusal costs neither the item nor virtual fuel/time.
                if (step.completed() == 1) {
                    var event = new FurnaceSmeltEvent(b, input.asOne(), result.clone());
                    plugin.getServer().getPluginManager().callEvent(event);
                    if (event.isCancelled()) break;
                    result = event.getResult();
                    if (result == null || result.isEmpty() || (output != null && !output.isEmpty()
                            && (!output.isSimilar(result) || output.getAmount() + result.getAmount() > output.getMaxStackSize()))) break;
                    if (output == null || output.isEmpty()) inv.setResult(result.clone());
                    else { output.setAmount(output.getAmount() + result.getAmount()); inv.setResult(output); }
                    input = input.getAmount() == 1 ? null : input.asQuantity(input.getAmount() - 1);
                    inv.setSmelting(input);
                    f.setRecipeUsedCount(r, f.getRecipeUsedCount(r.getKey()) + 1);
                }
                f.setCookTime((short) step.progress()); f.setCookTimeTotal(r.getCookingTime());
                f.setBurnTime((short) Math.max(0, f.getBurnTime() - step.fuelSpent()));
                advanced += step.productiveTicks(); budget -= step.productiveTicks();
                if (step.completed() == 0 && step.productiveTicks() < need && f.getBurnTime() > 0) break;
            }
            f.update(false, false); return advanced;
        }
        if (b.getState() instanceof Campfire c) {
            int count = 0; for (int i = 0; i < c.getSize(); i++) if (recipe(b, c.getItem(i)) != null) count++;
            int budget = (int) Math.round(ticks * Math.min(batchCap, 1 + Math.max(0, count - 1) * batchPerItem));
            int advanced = 0;
            var finished = new java.util.ArrayList<ItemStack>();
            for (int i = 0; i < c.getSize(); i++) {
                var r = recipe(b, c.getItem(i)); if (r == null || c.isCookingDisabled(i)) continue;
                int current = c.getCookTime(i), total = Math.max(1, c.getCookTimeTotal(i));
                int next = Math.min(total, current + budget);
                if (next >= total) {
                    var event = new org.bukkit.event.block.BlockCookEvent(b, c.getItem(i), r.getResult());
                    plugin.getServer().getPluginManager().callEvent(event);
                    if (event.isCancelled()) continue;
                    finished.add(event.getResult().clone()); c.setItem(i, null); c.setCookTime(i, 0);
                } else c.setCookTime(i, next);
                advanced += Math.max(0, next - current);
            }
            if (!c.update(false, false)) return 0;
            for (var result : finished) b.getWorld().dropItem(b.getLocation().add(.5, 1, .5), result);
            return advanced;
        }
        return 0;
    }

    private boolean ignite(Block b, Furnace f) {
        var fuel = f.getSnapshotInventory().getFuel(); if (fuel == null || fuel.isEmpty()) return false;
        var section = plugin.getConfig().getConfigurationSection("chef.fuelTicks");
        int duration = section == null ? 0 : section.getInt(fuel.getType().name());
        if (duration <= 0) return false; // unsupported fuel stays in the real furnace for vanilla
        var event = new FurnaceBurnEvent(b, fuel, duration);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled() || !event.isBurning() || event.getBurnTime() <= 0) return false;
        f.setBurnTime((short) Math.min(Short.MAX_VALUE, event.getBurnTime()));
        f.getSnapshotInventory().setFuel(fuel.getType() == Material.LAVA_BUCKET ? new ItemStack(Material.BUCKET)
                : fuel.getAmount() == 1 ? null : fuel.asQuantity(fuel.getAmount() - 1));
        return true;
    }
}
