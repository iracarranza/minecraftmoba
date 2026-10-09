package com.minecraftmoba.plugin;

import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Toolbox A1 -- Reconfiguratron! Swap the first components of the Utility Belt
 * circuit for the last components in the inventory.
 *
 * The exchange rule lives in {@link Reconfiguration}, which is pure and tested
 * on its own. This is the part that needs a player: reading the live board,
 * writing the result back, and paying the branches.
 *
 * <h2>Constrained reconfiguration, deliberately</h2>
 *
 * Not free inventory editing. classes.md is explicit that inventory
 * preparation remains the class's first mastery layer, and an ability that let
 * a player rewrite the board arbitrarily mid-fight would retire that layer
 * rather than build on it.
 *
 * <h2>Why reconfigure at all</h2>
 *
 * A1 swaps the first components; A2 fires the first two. So A1 is <b>A2's
 * loadout editor</b> rather than a general circuit editor. That is a design
 * consequence and not ability text, but it is why "first" is the right end to
 * operate on and why nothing here offers to choose a slot.
 */
public final class ReconfiguratronAbility implements Ability {

    /**
     * Swaps two components in your own inventory. classes.md names this as the
     * case that forced SELF into the vocabulary: it targets nothing, which is
     * why it ignores cast modes rather than previewing an empty set.
     */
    @Override public java.util.List<AbilityOutput> outputs() {
        return AbilityOutput.single("reconfiguratron", TargetForm.SELF, InputForm.INSTANT, Recipients.NONE);
    }

    private final MobaPlugin plugin;
    private final ConfigurationSection config;
    private final UtilityBelt belt;

    public ReconfiguratronAbility(MobaPlugin plugin, ConfigurationSection config, UtilityBelt belt) {
        this.plugin = plugin;
        this.config = Objects.requireNonNull(config);
        this.belt = belt;
        if (config.getLong("cooldownTicks") < 0)
            throw new IllegalArgumentException("Negative cooldown: reconfiguratron");
    }

    @Override public String id() { return "reconfiguratron"; }
    @Override public String displayName() { return Objects.requireNonNull(config.getString("displayName")); }
    @Override public long cooldownTicks() { return config.getLong("cooldownTicks"); }

    @Override public Map<String, String> branches() {
        return Map.of("speedy_swap", "Speedy Swap",
                      "more_config", "More Config",
                      "spare_parts", "Spare Parts");
    }
    @Override public List<String> branchIds() {
        return List.of("speedy_swap", "more_config", "spare_parts");
    }

    /** Speedy Swap halves the cooldown, which only a per-player hook can express. */
    @Override
    public long rechargeTicks(Player player, AbilityContext context) {
        ConfigurationSection b = branchConfig(context.branchFor(id()));
        double multiplier = b == null ? 1.0 : b.getDouble("cooldownMultiplier", 1.0);
        return Math.max(0, Math.round(cooldownTicks() * multiplier));
    }

    @Override
    public boolean execute(Player p, AbilityContext ctx) {
        if (belt == null) return false;
        String branch = ctx.branchFor(id());
        ConfigurationSection b = branchConfig(branch);

        List<Integer> componentSlots = componentSlots(p);
        if (componentSlots.isEmpty()) {
            // An empty board is not a failed cast, it is a board with nothing
            // on it -- and saying so is more useful than a silent no-op from an
            // ability whose entire job is rearranging things.
            if (plugin.hudNotice() != null) plugin.hudNotice().unavailable(p, "NO CIRCUIT");
            return false;
        }

        int depth = b == null ? config.getInt("depth") : b.getInt("depth", config.getInt("depth"));
        var plan = Reconfiguration.plan(componentSlots, depth);

        ItemStack[] contents = p.getInventory().getContents();
        Reconfiguration.apply(contents, plan);
        p.getInventory().setContents(contents);

        // Spare Parts pays for what the swap could NOT move. With a short
        // circuit the groups overlap, so the components left alone are exactly
        // the ones the arithmetic could not pair -- the branch turns that
        // shortfall into the payout rather than into a wasted cast.
        if ("spare_parts".equals(branch) && plan.unchanged() > 0) {
            // Section at a time, not a dotted literal: ConfigKeysDefinedTest
            // resolves dotted strings from the config ROOT, so a
            // section-relative one reads as undefined. Same lesson as
            // BoundingAbility's branch values.
            ConfigurationSection spare = config.getConfigurationSection("spareParts");
            double perComponent = spare == null ? 0 : spare.getDouble("healthPerComponent");
            heal(p, Vitals.scaleHealing(perComponent * plan.unchanged(), plugin.effectiveMaxHealth(p)));
        }
        return true;
    }

    /** The circuit's component slots, in the order the belt would run them. */
    private List<Integer> componentSlots(Player p) {
        var circuit = belt.circuitOf(p);
        var out = new ArrayList<Integer>();
        if (circuit == null) return out;
        for (CircuitReader.Step step : circuit.steps()) out.add(step.slot());
        return out;
    }

    private void heal(Player p, double amount) {
        var attr = p.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null || amount <= 0) return;
        p.setHealth(Math.min(attr.getValue(), p.getHealth() + amount));
    }

    private ConfigurationSection branchConfig(String branch) {
        if (branch == null) return null;
        ConfigurationSection all = config.getConfigurationSection("branches");
        return all == null ? null : all.getConfigurationSection(branch);
    }
}
