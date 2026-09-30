package com.minecraftmoba.plugin;

import org.bukkit.entity.EntityType;
import java.util.*;

/**
 * Built-in swarm definitions. Content, not balance.
 *
 * Only what needs no new behavior is defined. Sand Creeper (explosions of sand
 * yield quartz) and Infested Slime (splits also produce silverfish) are
 * specified in docs/proposals/2026-09-29-swarm-definition-schema.md and wait on
 * the swarm behavior listener; defining them here now would be accepted and
 * inert, which is worse than absent.
 */
public final class SwarmDefinitions {
    public static final Map<String, SwarmDefinition> DEFAULTS = new LinkedHashMap<>();
    static {
        // A daytime mountain encounter, standard Ravager behavior. Size and
        // value are placeholders; nothing here sets a difficulty tier.
        add(new SwarmDefinition("mountain_ravager",
                List.of(new SwarmDefinition.Member(EntityType.RAVAGER, 1)), 1,
                Set.of("mountain"), SwarmDefinition.Time.DAY, null, null, 3));
    }

    private static void add(SwarmDefinition d) { DEFAULTS.put(d.id(), d); }

    private SwarmDefinitions() {}

    public static SwarmDefinition require(String id) {
        var d = DEFAULTS.get(id);
        if (d == null) throw new IllegalArgumentException("Unknown swarm definition: " + id
                + " (known: " + String.join(", ", DEFAULTS.keySet()) + ")");
        return d;
    }

    public static SwarmTable table(Collection<String> ids) {
        var defs = new ArrayList<SwarmDefinition>();
        for (String id : ids) defs.add(require(id));
        return new SwarmTable(defs);
    }
}
