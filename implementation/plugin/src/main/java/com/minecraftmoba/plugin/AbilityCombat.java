package com.minecraftmoba.plugin;

import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * Which abilities are combat, declared per branch, and required rather than defaulted.
 *
 * <h2>Why this is declared and not inferred</h2>
 *
 * Combat-ness cannot be read off an ability's existence, because several
 * abilities are the opposite of fighting: Mole's Tunneling is a digging mode,
 * Gardener's Clip harvests a plant, Merchant's Work employs villagers, Golem
 * Master's Assemble builds a wall, Skeleton Crew Raises a worker. A blanket
 * rule would lock each of those classes out of its own progression while it
 * performed its defining activity.
 *
 * The working line from the design: an ability is combat when it acts on a
 * combatant's capacity to fight -- damaging, healing, mitigating, buffing,
 * granting speed -- and non-combat when it acts on the world or the economy.
 * Classify by effect on combatants, not by the medium: Sinkhole is combat
 * because it slows enemies and denies them building, while Tunneling moves the
 * same blocks and is not, because it does nothing to anyone's capacity to
 * fight.
 *
 * <h2>Why per branch</h2>
 *
 * A branch can change the answer. Daredevil's Runway is pure mobility and its
 * Suplex branch subjects an enemy to the launch. Golem Master's Assemble is the
 * opposite case: all three branches land combat, but by mitigation, crowd
 * control and buffing respectively -- so per-branch declaration records WHY,
 * not only whether, and stops a later branch edit silently inheriting a stale
 * answer.
 *
 * <h2>Why a missing key is fatal</h2>
 *
 * This repository has already paid for the alternative. {@code
 * ConfigKeysDefinedTest} exists because six features shipped silently disabled
 * when their keys were absent -- {@code getBoolean(path)} returns false for a
 * key that is not there, with nothing in the log to say so. Here a flag
 * defaulting to non-combat is a silent exploit, and one defaulting to combat is
 * a silent lockout that reads as a broken feature. Failing at load is the
 * convention, so load fails.
 */
public final class AbilityCombat {

    private final Map<String, Boolean> byAbility;
    private final Map<String, Map<String, Boolean>> byBranch;

    private AbilityCombat(Map<String, Boolean> byAbility, Map<String, Map<String, Boolean>> byBranch) {
        this.byAbility = byAbility;
        this.byBranch = byBranch;
    }

    /**
     * Read every declaration under {@code abilities.definitions}.
     *
     * @throws IllegalStateException naming every definition and branch that
     *         failed to declare {@code combat}, all of them at once. Reporting
     *         the first alone would mean one restart per missing key.
     */
    public static AbilityCombat load(ConfigurationSection definitions) {
        var abilities = new LinkedHashMap<String, Boolean>();
        var branches = new LinkedHashMap<String, Map<String, Boolean>>();
        var missing = new TreeSet<String>();
        if (definitions == null) return new AbilityCombat(abilities, branches);

        for (String id : definitions.getKeys(false)) {
            ConfigurationSection ability = definitions.getConfigurationSection(id);
            if (ability == null) continue;
            String path = "abilities.definitions." + id + ".combat";
            if (!ability.isBoolean("combat")) missing.add(path);
            else abilities.put(id, ability.getBoolean("combat"));

            ConfigurationSection branchSection = ability.getConfigurationSection("branches");
            if (branchSection == null) continue;
            var perBranch = new LinkedHashMap<String, Boolean>();
            for (String branch : branchSection.getKeys(false)) {
                ConfigurationSection b = branchSection.getConfigurationSection(branch);
                String branchPath = "abilities.definitions." + id + ".branches." + branch + ".combat";
                if (b == null || !b.isBoolean("combat")) missing.add(branchPath);
                else perBranch.put(branch, b.getBoolean("combat"));
            }
            branches.put(id, perBranch);
        }

        if (!missing.isEmpty())
            throw new IllegalStateException(
                    "Every ability and branch must declare whether it is combat, and these do not: "
                    + missing + ". See docs/design/COMBAT_STATE.md -- there is deliberately no "
                    + "default, because either default fails silently.");
        return new AbilityCombat(abilities, branches);
    }

    /**
     * Whether activating this ability on this branch is combat.
     *
     * A null or unknown branch falls back to the ability's own declaration,
     * which is the ability's normal case. A branch that declares its own answer
     * overrides it -- that is the whole point of declaring per branch.
     */
    public boolean combat(String abilityId, String branch) {
        if (branch != null) {
            Map<String, Boolean> perBranch = byBranch.get(abilityId);
            if (perBranch != null) {
                Boolean declared = perBranch.get(normalize(branch, perBranch));
                if (declared != null) return declared;
            }
        }
        Boolean declared = byAbility.get(abilityId);
        return declared != null && declared;
    }

    /**
     * Match a branch id against the config's spelling of it.
     *
     * The ability layer names branches in snake_case ("swarming_bite", stored
     * in PlayerData.classState) while config.yml keys them in camelCase
     * ("swarmingBite"), because the existing branch parameters are read by the
     * camelCase path. Rather than change either and break persisted player
     * state or every branch parameter lookup, the two spellings are reconciled
     * here, in the one place that has to see both.
     */
    private static String normalize(String branch, Map<String, Boolean> perBranch) {
        if (perBranch.containsKey(branch)) return branch;
        String collapsed = branch.replace("_", "");
        for (String key : perBranch.keySet())
            if (key.equalsIgnoreCase(collapsed)) return key;
        return branch;
    }
}
