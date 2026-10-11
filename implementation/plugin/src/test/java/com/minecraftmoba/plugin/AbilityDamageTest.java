package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Ability damage goes through one door, and the door is exactly as wide as the call. */
class AbilityDamageTest {

    @Test void nothingIsMarkedOutsideACall() { assertFalse(AbilityDamage.active()); }

    @Test void aScopeMarksDamageOnlyWhileItIsOpen() {
        try (var s = AbilityDamage.open()) { assertTrue(AbilityDamage.active()); }
        assertFalse(AbilityDamage.active());
    }

    @Test void scopesNestAndOnlyTheOutermostClearsIt() {
        try (var outer = AbilityDamage.open()) {
            try (var inner = AbilityDamage.open()) { assertTrue(AbilityDamage.active()); }
            assertTrue(AbilityDamage.active(), "closing the inner scope must not unmark the outer call");
        }
        assertFalse(AbilityDamage.active());
    }

    @Test void closingTwiceDoesNotUndercount() {
        var a = AbilityDamage.open();
        a.close(); a.close();
        try (var b = AbilityDamage.open()) { assertTrue(AbilityDamage.active()); }
        assertFalse(AbilityDamage.active());
    }

    @Test void aListenerThatThrowsStillClosesTheScope() {
        assertThrows(IllegalStateException.class, () -> {
            try (var s = AbilityDamage.open()) { throw new IllegalStateException("a listener failed"); }
        });
        assertFalse(AbilityDamage.active());
    }

    @Test void theMarkIsPerThread() throws Exception {
        try (var s = AbilityDamage.open()) {
            boolean[] other = new boolean[1];
            var t = new Thread(() -> other[0] = AbilityDamage.active());
            t.start(); t.join();
            assertFalse(other[0], "another thread's damage is not this ability's");
        }
    }

    /**
     * The rule that makes the door worth having: no ability, and nothing else, calls {@code damage(amount, source)}
     * directly. A source-less {@code damage(amount)} is fine (it has no damager, so no click to mistake it for).
     */
    @Test void noSourceCallsDamageWithASourceExceptTheDoor() throws IOException {
        var offenders = new ArrayList<String>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (f.getFileName().toString().equals("AbilityDamage.java")) continue;
                String src = Files.readString(f);
                int from = 0;
                while (true) {
                    int at = src.indexOf(".damage(", from);
                    if (at < 0) break;
                    from = at + 8;
                    if (topLevelArguments(src, at + 8) > 1) offenders.add(f.getFileName() + " offset " + at);
                }
            }
        }
        assertEquals(List.of(), offenders, "use AbilityDamage.deal(target, amount, source) so the input layer does not read the hit as a click");
    }

    /** How many top-level arguments the call starting after its opening parenthesis has. */
    static int topLevelArguments(String src, int start) {
        int depth = 1, args = 0;
        boolean any = false;
        for (int i = start; i < src.length() && depth > 0; i++) {
            char c = src.charAt(i);
            if (c == '(' || c == '[' || c == '{') depth++;
            else if (c == ')' || c == ']' || c == '}') depth--;
            else if (c == ',' && depth == 1) args++;
            else if (!Character.isWhitespace(c)) any = true;
        }
        return any ? args + 1 : 0;
    }

    @Test void theArgumentCounterReadsNestedCalls() {
        assertEquals(1, topLevelArguments("selfDamage)", 0));
        assertEquals(2, topLevelArguments("config.getDouble(\"a\"), f.caster())", 0));
        assertEquals(2, topLevelArguments("Math.max(0, 9 - x[0]) * y, chef)", 0));
        assertEquals(0, topLevelArguments(")", 0));
    }
}
