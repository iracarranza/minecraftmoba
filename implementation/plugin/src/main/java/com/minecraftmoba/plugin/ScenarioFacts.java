package com.minecraftmoba.plugin;

/**
 * What a scenario's checks are allowed to know, as an interface.
 *
 * The judge ({@link ScenarioJudge}) asks only these questions, so the assertion vocabulary is
 * tested against a fake without a server, and the live implementation ({@link ScenarioBench}) has
 * one place where "health" and "position" are read from the world.
 *
 * Past values come from a snapshot taken at the START of every tick, before that tick's acts: a
 * check that says {@code since: 20} means "compared with the moment tick 20 began", so an act at
 * tick 20 is inside the span being judged.
 */
public interface ScenarioFacts {
    /** Current health of a roster member. */
    double health(String id);
    /** Health at the start of a tick, or NaN when that tick was not recorded. */
    double healthAt(String id, long tick);
    double maxHealth(String id);
    boolean alive(String id);
    /** {x, y, z} now. */
    double[] position(String id);
    /** {x, y, z} at the start of a tick, or null when that tick was not recorded. */
    double[] positionAt(String id, long tick);
    boolean stunned(String id);
    long stunRemaining(String id);
    boolean rooted(String id);
    /** How many times a roster member's ability has executed (a real cast, not a refusal). */
    int executions(String id, String abilityId);
}
