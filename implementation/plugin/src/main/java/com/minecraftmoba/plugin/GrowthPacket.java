package com.minecraftmoba.plugin;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * What one Growth level grants a class: several effects, across several families.
 *
 * Replaces {@code ClassDefinition.infrastructureProgression}, which was
 * {@code Map<level, effect>} -- one effect per level, and therefore unable to
 * express a class at all once any level granted two things.
 *
 * <h2>The blocker this exists to clear</h2>
 *
 * Skeleton Crew's Lv6 is Supply Line access <b>and</b> Night Efficiency I, and
 * its Lv18 is Night Efficiency II plus crew combat development. Under one
 * effect per level the class could not be configured at all.
 *
 * <h2>Why not Map&lt;level, List&lt;String&gt;&gt;</h2>
 *
 * Because that would rebuild the same Growth-is-Infrastructure confusion one
 * level down. A list of infrastructure strings still says every Growth effect
 * is an infrastructure effect; it just permits more of them.
 *
 * A Growth packet combines infrastructure with class methodology and personal
 * attributes, so <b>infrastructure is one effect FAMILY inside the packet</b>,
 * not the packet itself. Skeleton Crew's Lv6 is then exactly what it reads as:
 * one infrastructure effect (Supply Line eligibility) and one methodology
 * effect (Night Efficiency I), which a list of infrastructure strings could
 * only have recorded by lying about the second.
 *
 * <h2>What belongs here, and what does not</h2>
 *
 * The design's preferred shape is "regular continuous attribute growth PLUS
 * authored qualitative/discrete Growth spikes". The continuous half already
 * exists: {@link Capacity} carries per-class Health, Hunger and Inventory
 * curves selected by {@code statGrowthProfile}. This packet is the discrete
 * half -- new eligibility, infrastructure capacity, Route concurrency,
 * methodology capacity -- which is where the design says spikes belong.
 *
 * So a class does not restate its health curve here. It records what it gains
 * that a curve cannot express.
 *
 * <h2>Budgets need not be equal</h2>
 *
 * Lv15 already grants an Ultimate and Lv24 already grants Task VI, so their
 * packets can be modest; Lv18, 21 and 27 are cleaner Growth-only levels and can
 * carry more. An empty packet at a Growth level is therefore legitimate, not a
 * configuration error, and nothing here requires every level to be populated.
 */
public record GrowthPacket(int level, List<Effect> effects) {

    /**
     * The effect families a packet can spend its budget across.
     *
     * Named rather than free-form because the distinction is the entire point:
     * the previous model collapsed all three into "infrastructure" and could
     * not represent a class whose level granted one of each.
     */
    public enum Family {
        /**
         * Discrete personal capabilities -- new eligibility, a class-resource
         * capacity step. NOT the continuous Health/Hunger/Inventory curves,
         * which belong to Capacity and are selected by statGrowthProfile.
         */
        PERSONAL,
        /**
         * Capacity, Extent, Reach, Potency, Eligibility, Concurrency,
         * Projection, Throughput, Connectivity, Branching, Filtering/Routing,
         * Reliability, Integration.
         *
         * Not an archetype entitlement: classes receive only the
         * infrastructure access their actual methodology needs, at the level
         * their own curve spends budget on it. Mole reaches Establish I at
         * Lv18; Gardener may reach Development Zone Capacity at Lv6. Neither is
         * early or late in any absolute sense.
         */
        INFRASTRUCTURE,
        /**
         * How the class does its work -- Night Efficiency, crew combat
         * development, a methodology capacity. The family that was
         * unrepresentable before, and the reason Skeleton Crew was blocked.
         */
        METHODOLOGY
    }

    /**
     * One effect within a packet.
     *
     * @param dimension which dimension it moves, from the design's named lists
     *                  ("eligibility", "capacity", "reach", ...). Free text, so
     *                  the code does not have to be edited to author a class.
     * @param effect    what is granted ("supply_line", "night_efficiency").
     * @param tier      the magnitude, where one exists; 0 when the effect is
     *                  simply granted rather than levelled.
     */
    public record Effect(Family family, String dimension, String effect, int tier) {
        public Effect {
            Objects.requireNonNull(family, "A Growth effect must name its family.");
            Objects.requireNonNull(effect, "A Growth effect must name what it grants.");
            if (effect.isBlank()) throw new IllegalArgumentException("A Growth effect cannot be blank.");
            dimension = dimension == null ? "" : dimension;
            if (tier < 0) throw new IllegalArgumentException("A Growth tier cannot be negative.");
        }
        public Effect(Family family, String dimension, String effect) { this(family, dimension, effect, 0); }
    }

    public GrowthPacket {
        if (level < 0) throw new IllegalArgumentException("A Growth level cannot be negative.");
        effects = effects == null ? List.of() : List.copyOf(effects);
    }

    /** The effects in one family, in authored order. */
    public List<Effect> of(Family family) {
        return effects.stream().filter(e -> e.family() == family).toList();
    }

    public boolean isEmpty() { return effects.isEmpty(); }

    // ---- loading ----------------------------------------------------------

    /**
     * Read a class's {@code growth:} section, keyed {@code level6}, {@code level18}, ...
     *
     * The {@code levelN} spelling is carried over from
     * {@code infrastructureProgression} rather than switched to bare integers,
     * because YAML keys that look like numbers are a recurring source of
     * quiet type surprises and the existing config already reads this way.
     */
    public static Map<Integer, GrowthPacket> load(ConfigurationSection growth) {
        var packets = new TreeMap<Integer, GrowthPacket>();
        if (growth == null) return Map.copyOf(packets);
        for (String key : growth.getKeys(false)) {
            int level = level(key);
            var effects = new ArrayList<Effect>();
            ConfigurationSection packet = growth.getConfigurationSection(key);
            if (packet == null) continue;
            for (Family family : Family.values()) {
                List<Map<?, ?>> entries = packet.getMapList(family.name().toLowerCase(Locale.ROOT));
                for (Map<?, ?> entry : entries) {
                    Object effect = entry.get("effect");
                    if (effect == null)
                        throw new IllegalStateException("A Growth effect at " + key
                                + " does not name what it grants: " + entry);
                    Object dimension = entry.get("dimension");
                    Object tier = entry.get("tier");
                    effects.add(new Effect(family,
                            dimension == null ? "" : dimension.toString(),
                            effect.toString(),
                            tier instanceof Number n ? n.intValue() : 0));
                }
            }
            packets.put(level, new GrowthPacket(level, effects));
        }
        return Map.copyOf(packets);
    }

    /**
     * Translate the superseded {@code infrastructureProgression} map.
     *
     * The old shape is read as what it always meant -- one infrastructure
     * effect, of unstated dimension -- so existing class configuration keeps
     * working and is not silently reinterpreted as something richer than it
     * says. A class that needs two effects at a level must move to {@code
     * growth:}; there is no way to express that in the old shape, which is the
     * whole reason it is superseded.
     */
    public static Map<Integer, GrowthPacket> fromLegacy(Map<String, String> infrastructureProgression) {
        var packets = new TreeMap<Integer, GrowthPacket>();
        if (infrastructureProgression == null) return Map.copyOf(packets);
        infrastructureProgression.forEach((key, effect) -> {
            int level = level(key);
            packets.put(level, new GrowthPacket(level,
                    List.of(new Effect(Family.INFRASTRUCTURE, "", effect))));
        });
        return Map.copyOf(packets);
    }

    /**
     * Merge authored packets over translated legacy ones.
     *
     * A level present in {@code growth:} wins outright rather than combining,
     * so a class being migrated does not silently end up with both the old
     * single effect and its new replacement.
     */
    public static Map<Integer, GrowthPacket> merge(Map<Integer, GrowthPacket> legacy,
                                                   Map<Integer, GrowthPacket> authored) {
        var merged = new TreeMap<>(legacy);
        merged.putAll(authored);
        return Map.copyOf(merged);
    }

    private static int level(String key) {
        String digits = key.startsWith("level") ? key.substring("level".length()) : key;
        try {
            return Integer.parseInt(digits.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "A Growth key must read like 'level6', and this does not: " + key, e);
        }
    }
}
