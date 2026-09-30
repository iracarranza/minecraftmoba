package com.minecraftmoba.plugin;

import org.bukkit.entity.EntityType;
import java.util.*;

/**
 * What a Mob Swarm IS, separately from where it can be.
 *
 * Biome and time eligibility, composition, an optional behavior modifier, an
 * optional resource payload and an encounter value all belong to the swarm, not
 * to the opportunity that hosts it. The opportunity is spatial and ecological
 * and persists; which definition manifests in it depends on the biome under the
 * chosen locus and on the current temporal state (2026-09-29 reconciliation).
 *
 * `behavior` and `payload` are ids into registries that are EMPTY today, so a
 * definition naming one is refused rather than accepted and silently inert.
 * That is deliberate: Sand Creeper and Infested Slime are specified but need a
 * swarm-scoped listener keyed on the member marker, which does not exist yet.
 *
 * `value` is a relative encounter value, unset by any balance decision.
 */
public record SwarmDefinition(String id, List<Member> members, int size, Set<String> biomes,
                              Time time, String behavior, String payload, int value) {

    /** Day, night, or all-day. Night is the world's own phase, as recovery uses. */
    public enum Time {
        DAY, NIGHT, ALL;
        public boolean matches(boolean night) {
            return this == ALL || (this == NIGHT) == night;
        }
    }

    public record Member(EntityType type, int weight) {
        public Member {
            if (weight <= 0) throw new IllegalArgumentException("member weight must be positive");
        }
    }

    /** Behavior and payload ids that exist. Empty until swarm listeners are built. */
    public static final Set<String> KNOWN_BEHAVIORS = Set.of();
    public static final Set<String> KNOWN_PAYLOADS = Set.of();

    public SwarmDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("a swarm needs an id");
        if (members == null || members.isEmpty())
            throw new IllegalArgumentException(id + ": a swarm needs at least one member type");
        for (var m : members)
            if (RenewableKinds.FORBIDDEN_SWARM.contains(m.type()))
                throw new IllegalArgumentException(id + ": boss-class entity is not an ordinary "
                        + "regenerative Swarm: " + m.type());
        if (size <= 0) throw new IllegalArgumentException(id + ": size must be positive");
        if (biomes == null || biomes.isEmpty())
            throw new IllegalArgumentException(id + ": a swarm needs biome eligibility");
        BiomeGroups.validate(biomes);
        if (time == null) throw new IllegalArgumentException(id + ": a swarm needs a time");
        if (behavior != null && !KNOWN_BEHAVIORS.contains(behavior))
            throw new IllegalArgumentException(id + ": unknown behavior '" + behavior + "'");
        if (payload != null && !KNOWN_PAYLOADS.contains(payload))
            throw new IllegalArgumentException(id + ": unknown payload '" + payload + "'");
        members = List.copyOf(members);
        biomes = Set.copyOf(biomes);
    }

    public boolean eligibleAt(String biome, boolean night) {
        return time.matches(night) && BiomeGroups.matches(biomes, biome);
    }

    /** One entity type per member of a fresh manifestation, drawn by weight. */
    public List<EntityType> compose(Random random) {
        int total = members.stream().mapToInt(Member::weight).sum();
        var out = new ArrayList<EntityType>(size);
        for (int i = 0; i < size; i++) {
            int roll = random.nextInt(total);
            for (var m : members) {
                roll -= m.weight();
                if (roll < 0) { out.add(m.type()); break; }
            }
        }
        return out;
    }
}
