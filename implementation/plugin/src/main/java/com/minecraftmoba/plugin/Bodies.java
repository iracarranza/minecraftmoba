package com.minecraftmoba.plugin;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Where a scenario's participants come from.
 *
 * One narrow seam, deliberately. Everything else in the harness -- the
 * timeline, the roster, teardown, the assertions -- is ordinary logic that can
 * be tested without a server. Creating a player cannot be, so it lives behind
 * this interface and nothing else has to know how it was done.
 *
 * <h2>Connected versus not, and why it is on the interface</h2>
 *
 * A server-side fake player has no network connection. That is usually fine --
 * it is a real {@link Player}, so enrolment, {@link PlayerData},
 * {@link Capacity}, {@link AbilityInputs}, team assignment, {@link Provenance}
 * and {@link Stun} all work on it unchanged.
 *
 * It is <b>not</b> fine for anything enforced in {@code PlayerMoveEvent}, which
 * is driven by an inbound client movement packet. A body with no connection
 * never sends one, so it never moves, so the handler never runs -- and an
 * assertion that "the rooted player did not move" would pass without the rule
 * ever being exercised.
 *
 * <b>A false pass is worse than no test</b>, so {@link #connected()} is part of
 * the contract rather than an implementation detail. A scenario step that needs
 * real movement declares it, and the runner refuses to run that scenario
 * against unconnected bodies instead of quietly succeeding.
 */
public interface Bodies {

    /** A participant created for a scenario. */
    record Body(String id, Player player, Team team, String classId, int level) {}

    /**
     * Create a participant at a position, enrolled, teamed, classed and levelled.
     *
     * @throws IllegalStateException if the body cannot be created, which the
     *         runner turns into an aborted scenario rather than a partial one.
     */
    Body spawn(String id, Team team, String classId, int level, Location at);

    /** Remove a participant. Must tolerate being called twice, and on a dead or absent body. */
    void despawn(Body body);

    /**
     * Whether bodies from this source generate real client input -- movement
     * packets above all.
     *
     * False for connectionless fake players. True for a protocol client, or for
     * fake players given a dummy connection and packet listener.
     */
    boolean connected();

    /** A short name for reports, so a failure says what kind of body produced it. */
    String describe();
}
