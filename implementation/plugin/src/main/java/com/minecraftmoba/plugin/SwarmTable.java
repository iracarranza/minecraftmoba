package com.minecraftmoba.plugin;

import java.util.*;

/**
 * The swarm definitions one opportunity may manifest, across both times of day.
 *
 * Opportunity identity is spatial; this table is what is temporal. The same
 * opportunity therefore keeps its place and its history while the definition
 * that can occupy it changes with the biome under the locus and the current
 * time. It is consulted only when the opportunity next manifests, so a living
 * daytime swarm is never replaced at sunset -- the night table simply applies
 * to the following manifestation.
 *
 * Which definition wins among several eligible ones is uniform. Weighting them
 * would assert something about relative frequency that the design has not set.
 */
public record SwarmTable(List<SwarmDefinition> definitions) {

    /** A chosen place and the swarm that will occupy it. */
    public record Choice(Eligibility.Locus locus, SwarmDefinition definition) {}

    public SwarmTable {
        if (definitions == null || definitions.isEmpty())
            throw new IllegalArgumentException("a swarm table needs at least one definition");
        definitions = List.copyOf(definitions);
    }

    /** Cheap pre-check, so an opportunity outside its window costs no terrain scan. */
    public boolean anyAtTime(boolean night) {
        for (var d : definitions) if (d.time().matches(night)) return true;
        return false;
    }

    public List<SwarmDefinition> eligible(String biome, boolean night) {
        var out = new ArrayList<SwarmDefinition>();
        for (var d : definitions) if (d.eligibleAt(biome, night)) out.add(d);
        return out;
    }

    /**
     * Locus and definition together, or null when nothing is currently possible.
     *
     * Loci are first filtered to those where SOME definition is eligible now, so
     * the ordinary displacement and player-exclusion rules of
     * {@link Eligibility#select} still choose among places that can actually
     * host a swarm. A biome the terrain view cannot report never qualifies.
     */
    public Choice pick(OpportunityRegion region, TerrainView world, Eligibility.Rules rules,
                       Eligibility.Locus previous, boolean night, Random random) {
        if (!anyAtTime(night)) return null;
        var hostable = new ArrayList<Eligibility.Locus>();
        for (var l : Eligibility.loci(region, world, rules))
            if (!eligible(world.biomeAt(l.x(), l.z()), night).isEmpty()) hostable.add(l);
        var locus = Eligibility.select(hostable, rules, previous, world, random);
        if (locus == null) return null;
        var options = eligible(world.biomeAt(locus.x(), locus.z()), night);
        return new Choice(locus, options.get(random.nextInt(options.size())));
    }

    public String ids() {
        var ids = new ArrayList<String>();
        for (var d : definitions) ids.add(d.id());
        return String.join(",", ids);
    }
}
