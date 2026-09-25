package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the class draft happens: two opposed team ranks and a sunken ban pit.
 *
 * Arithmetic only, like {@link LobbyHall}, so the properties that matter can
 * be asserted without a server -- that both teams get equal and mirrored
 * positions, that nothing overlaps, and that every stand is reachable. Those
 * are the ways a draft room fails, and none is visible by reading the loop
 * that places the blocks.
 *
 * A GREYBOX. It decides arrangement, which is function. Materials and the two
 * team colours come from config, the same way `LobbyHall` leaves them and the
 * resource pack ships placeholders.
 *
 * THE ARRANGEMENT, and why each part is what it is.
 *
 * TWENTY-EIGHT STANDS, FIXED. Seven per team plus fourteen banned, one ban per
 * player. The count does not grow with the class roster -- the 25 September
 * amendment moved the stand from the class to the PLAYER precisely so a
 * fifty-class catalogue does not become a fifty-station rack.
 *
 * OPPOSED RANKS AT DIFFERENT RANGE, which is a free information gradient
 * rather than a limitation worked around. Armour-stand gear is legible to
 * roughly 16-24 blocks; name tags render through blocks at much greater range.
 * So a player standing at their own rank READS THEIR TEAMMATES' KIT and READS
 * THE ENEMY'S NAMES. Two channels, two ranges, no mechanism.
 *
 * A SUNKEN BAN PIT, below floor level down the centre. Looking down is the
 * natural Minecraft camera, so a recessed rank reads without effort; it is a
 * visual sink that does not compete with the team ranks; and "removed from
 * play" acquires a spatial meaning instead of merely a location. The floor
 * around it stays walkable, which is where idle and warmup movement goes now
 * that walking is no longer the selection act.
 *
 * BANS ARE ATTRIBUTED. Each half of the pit holds the seven bans one team
 * spent. Bans are global in EFFECT -- a banned class is gone for everyone --
 * but who spent one is strategically real, the draft is open information, and
 * splitting the pit carries that fact for free.
 */
public final class DraftHall {

    /** One placement: a position, and what stands there. */
    public record Stand(int x, int y, int z, Slot slot, Team team, int index) {}

    public enum Slot { TEAM, BAN }

    public static final int PER_TEAM = 7;
    public static final int BANS_PER_TEAM = 7;

    private DraftHall() {}

    /**
     * The twenty-eight stand positions, centred on (0, floorY, 0).
     *
     * `rankOffset` is how far each team's rank sits from the centre line, so
     * the gap between the ranks is twice it. `pitDepth` is how far the ban
     * floor drops below the hall floor.
     */
    public static List<Stand> stands(int floorY, int rankOffset, int pitDepth, int spacing) {
        if (rankOffset < 3) throw new IllegalArgumentException(
                "the ranks must clear the pit");
        if (pitDepth < 1) throw new IllegalArgumentException("a pit must be sunken");
        if (spacing < 1) throw new IllegalArgumentException("stands cannot share a block");
        var out = new ArrayList<Stand>();

        // Team ranks: seven abreast, facing each other across the pit. North
        // takes negative z and south positive, matching the team axis used
        // everywhere else rather than inventing a second convention.
        for (Team team : new Team[]{Team.NORTH, Team.SOUTH}) {
            int z = team == Team.NORTH ? -rankOffset : rankOffset;
            for (int i = 0; i < PER_TEAM; i++)
                out.add(new Stand(centred(i, PER_TEAM, spacing), floorY + 1, z,
                        Slot.TEAM, team, i));
        }

        // Ban pit: two ranks of seven inside the recess, each attributed to
        // the team that spent them, on that team's own side of the centre.
        for (Team team : new Team[]{Team.NORTH, Team.SOUTH}) {
            int z = team == Team.NORTH ? -1 : 1;
            for (int i = 0; i < BANS_PER_TEAM; i++)
                out.add(new Stand(centred(i, BANS_PER_TEAM, spacing),
                        floorY - pitDepth + 1, z, Slot.BAN, team, i));
        }
        return out;
    }

    /** Centre a rank of `count` items spaced `spacing` apart on x = 0. */
    private static int centred(int index, int count, int spacing) {
        return (index - (count - 1) / 2) * spacing;
    }

    /**
     * The pit's excavation: floor blocks to remove, and the floor beneath it.
     *
     * Returned as geometry rather than cut in place so a caller can build the
     * hall first and the pit second, and so a test can assert the pit does not
     * undercut either rank.
     */
    public static List<LobbyHall.Piece> pit(int floorY, int pitDepth, int halfWidth, int halfLength) {
        var out = new ArrayList<LobbyHall.Piece>();
        for (int x = -halfWidth; x <= halfWidth; x++)
            for (int z = -halfLength; z <= halfLength; z++) {
                for (int y = floorY; y > floorY - pitDepth; y--)
                    out.add(new LobbyHall.Piece(x, y, z, LobbyHall.Role.PLATFORM));
                out.add(new LobbyHall.Piece(x, floorY - pitDepth, z, LobbyHall.Role.FLOOR));
            }
        return out;
    }

    /** Where a team's players arrive: behind their own rank, facing the pit. */
    public static int[] spawn(Team team, int floorY, int rankOffset) {
        return new int[]{0, floorY + 1, team == Team.NORTH
                ? -(rankOffset + 3) : rankOffset + 3};
    }

    /** The minimum interior half-span a hall needs to contain this arrangement. */
    public static int requiredRadius(int rankOffset, int spacing) {
        int rankHalfSpan = ((PER_TEAM - 1) / 2) * spacing;
        return Math.max(rankOffset + 4, rankHalfSpan + 2);
    }
}
