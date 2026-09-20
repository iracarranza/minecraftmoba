package com.minecraftmoba.plugin;

/**
 * A match team (ALPHA-D1).
 *
 * Every participating player holds exactly one assignment. The frozen Alpha map
 * sites each team's structures on its own half, so the two names are the map's
 * own north/south axis rather than arbitrary labels.
 */
public enum Team {
    NORTH, SOUTH;

    public Team other() { return this == NORTH ? SOUTH : NORTH; }

    public static Team parse(String s) {
        if (s == null) throw new IllegalArgumentException("Team required: north or south.");
        return switch (s.toLowerCase()) {
            case "north", "n" -> NORTH;
            case "south", "s" -> SOUTH;
            default -> throw new IllegalArgumentException("Unknown team '" + s + "'; use north or south.");
        };
    }

    public String lower() { return name().toLowerCase(); }
}
