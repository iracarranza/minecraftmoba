package com.minecraftmoba.plugin;

import java.util.UUID;

/** One permanent site's occupant; no sunset timer, dawn expiry, loot or siege-effect rule. */
public final class LairLifecycle {
    public enum State { DORMANT, ALIVE, UNBOUND, BLOCKED }
    public interface Encounters {
        boolean bound();
        /** Null means the encounter could not manifest; no phantom alive occupant. */
        UUID spawn(OpportunityCadence.Boss boss);
        void remove(UUID id);
    }
    public record SiegeOpportunity(OpportunityCadence.Boss boss, Team victoriousTeam,
                                   TeamObjectives.Kind pairedObjective) {}
    private final Encounters encounters;
    private State state = State.DORMANT;
    private OpportunityCadence.Boss scheduled;
    private UUID occupant;
    private int lastNight;
    private SiegeOpportunity lastVictory;
    public LairLifecycle(Encounters encounters) { this.encounters = encounters; }
    public State state() { return state; }
    public UUID occupant() { return occupant; }
    public OpportunityCadence.Boss scheduled() { return scheduled; }
    public SiegeOpportunity lastVictory() { return lastVictory; }
    public void onNight(int ordinal) {
        if (ordinal <= lastNight) return; // duplicate/stale transition cannot respawn a killed monster
        lastNight = ordinal;
        var boss = OpportunityCadence.atNight(ordinal).boss();
        if (boss == null) return; // Worksite nights and post-sequence nights leave the occupant alone
        if (occupant != null) encounters.remove(occupant); // replacement is not a kill/reward
        occupant = null;
        scheduled = boss;
        if (!encounters.bound()) { state = State.UNBOUND; return; }
        occupant = encounters.spawn(boss);
        state = occupant == null ? State.BLOCKED : State.ALIVE;
    }
    /** Only the current entity's actual death resolves the encounter. Attribution may be unknown. */
    public boolean killed(UUID id, Team victoriousTeam) {
        if (occupant == null || !occupant.equals(id)) return false;
        lastVictory = new SiegeOpportunity(scheduled, victoriousTeam, TeamObjectives.pairedObjective(scheduled));
        occupant = null; state = State.DORMANT;
        return true;
    }
    public void reset() {
        if (occupant != null) encounters.remove(occupant);
        occupant = null; scheduled = null; state = State.DORMANT; lastNight = 0; lastVictory = null;
    }
}
