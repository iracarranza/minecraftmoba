package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * The real roster, as the pre-entry flow sees it.
 *
 * Reads what abilities declare: a slot is BUILT when an Ability is bound to it and
 * carries that ability's outputs and its combat flag; a passive is recognised by the
 * class's passive hook and has no outputs; anything else is designed and says so.
 */
final class CombatCatalog implements CombatPreEntry.Catalog {
    private final MobaPlugin plugin;

    CombatCatalog(MobaPlugin plugin) { this.plugin = plugin; }

    @Override public List<String> classes() { return plugin.inputs().ids(); }

    @Override public String label(String classId) {
        var d = plugin.inputs().definition(classId);
        return d == null ? classId : d.displayName();
    }

    @Override public int defaultLevel() { return 15; }

    @Override public List<CombatAvailability.SlotInfo> slots(String classId) {
        var out = new ArrayList<CombatAvailability.SlotInfo>();
        var def = plugin.inputs().definition(classId);
        boolean hasPassive = def != null && !def.passiveHook().isBlank();
        out.add(new CombatAvailability.SlotInfo(CombatAvailability.Slot.PASSIVE,
                hasPassive ? def.passiveHook() : "passive", hasPassive ? def.passiveHook() : "no passive hook",
                false, true, false, List.of()));
        for (var slot : List.of(CombatAvailability.Slot.A1, CombatAvailability.Slot.A2, CombatAvailability.Slot.ULT)) {
            Ability ability = plugin.inputs().abilityFor(classId, slot.name().toLowerCase(java.util.Locale.ROOT));
            if (ability == null) {
                out.add(new CombatAvailability.SlotInfo(slot, "?", "not built", false, false, false, List.of()));
                continue;
            }
            String branch = def == null ? null : def.branchFor(ability.id());
            boolean combat = plugin.inputs().combatRules().combat(ability.id(), branch);
            out.add(new CombatAvailability.SlotInfo(slot, ability.id(), ability.displayName(), true, false,
                    combat, ability.outputs()));
        }
        return out;
    }
}
