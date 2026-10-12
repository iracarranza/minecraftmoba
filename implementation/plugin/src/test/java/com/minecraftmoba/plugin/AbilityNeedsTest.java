package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/** What a refused cast tells the player, with no server. */
class AbilityNeedsTest {
    private static Ability.Need need(String label, boolean met, String have) {
        return new Ability.Need(label, (p, c) -> met, have == null ? null : (p, c) -> have);
    }

    @Test void onlyTheUnmetNeedsAreReportedAndMetOnesAreNot() {
        var unmet = AbilityNeeds.unmet(List.of(need("4 Sand", false, null), need("a pickaxe", true, null)), null, null);
        assertEquals(List.of("4 Sand"), unmet);
    }

    @Test void whatThePlayerHasIsAppended() {
        assertEquals(List.of("food in your inventory (you carry none)"),
                AbilityNeeds.unmet(List.of(need("food in your inventory", false, "you carry none")), null, null));
        assertEquals(List.of("food"), AbilityNeeds.unmet(List.of(need("food", false, "  ")), null, null), "a blank have adds nothing");
    }

    @Test void nothingMissingMeansNothingToSay() {
        assertEquals(List.of(), AbilityNeeds.unmet(List.of(need("x", true, null)), null, null));
        assertEquals(List.of(), AbilityNeeds.unmet(List.of(), null, null));
        assertNull(AbilityNeeds.sentence("Food Fight", List.of()));
        assertNull(AbilityNeeds.hud(List.of()));
    }

    @Test void aNeedThatCannotBeCheckedIsSkippedNotReported() {
        var broken = new Ability.Need("a thing", (p, c) -> { throw new IllegalStateException("no world"); }, null);
        assertEquals(List.of(), AbilityNeeds.unmet(List.of(broken), null, null));
    }

    @Test void theSentenceNamesTheAbilityAndJoinsTheNeedsLikeEnglish() {
        assertEquals("Food Fight needs food in your inventory.", AbilityNeeds.sentence("Food Fight", List.of("food in your inventory")));
        assertEquals("Shoes needs Concrete and a block.", AbilityNeeds.sentence("Shoes", List.of("Concrete", "a block")));
        assertEquals("Mixer needs 4 Sand, 4 Gravel, and a dye.", AbilityNeeds.sentence("Mixer", List.of("4 Sand", "4 Gravel", "a dye")));
    }

    @Test void theHudLineIsTheFirstNeedOnly() {
        assertEquals("Needs 4 Sand", AbilityNeeds.hud(List.of("4 Sand", "4 Gravel")));
    }

    @Test void anAbilityThatDeclaresNothingDeclaresAnEmptyList() {
        Ability a = new Ability() {
            public String id() { return "x"; } public String displayName() { return "X"; } public long cooldownTicks() { return 0; }
            public boolean execute(org.bukkit.entity.Player p, AbilityContext c) { return false; }
        };
        assertEquals(List.of(), a.needs(null, null));
    }
}
