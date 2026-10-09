package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;

import java.util.UUID;

/**
 * Builds a {@link ChamberPlatform} in the world: deck, window, doorway, plate,
 * and the two scoop-source buttons.
 *
 * The geometry is {@link ChamberPlatform}'s and is tested without a server. This
 * only places blocks and entities, so it is deliberately free of decisions.
 *
 * Entities are non-persistent: they are a view onto a disposable scoop, and the
 * workspace that owns them is dropped before that world unloads.
 *
 * NOT VERIFIED AGAINST A LIVE SERVER at the time of writing.
 */
final class ChamberPlatformWorld {
    static final String BUTTON_TAG = "chamber_button";
    static final String CERTIFIED = "certified", RANDOM = "random";

    private ChamberPlatformWorld() {}

    static void build(World w, ChamberPlatform plat, UUID owner) {
        fill(w, plat.deck(), Material.POLISHED_ANDESITE);
        fill(w, plat.window(), Material.GLASS);
        // The doorway: two blocks of air through the glass above the plate.
        for (int dy = 1; dy <= 2; dy++)
            w.getBlockAt(plat.doorX(), plat.deckY() + dy, plat.window().minZ()).setType(Material.AIR, false);
        w.getBlockAt(plat.plate().x(), plat.plate().y(), plat.plate().z())
                .setType(Material.STONE_PRESSURE_PLATE, false);

        button(w, plat.certifiedButton(), owner, CERTIFIED, Material.EMERALD_BLOCK,
                "CERTIFIED SCOOP", "Pre-staged. Click to cycle.", NamedTextColor.GREEN);
        button(w, plat.randomButton(), owner, RANDOM, Material.REDSTONE_BLOCK,
                "RANDOM SEED", "Unvetted terrain. Click to roll.", NamedTextColor.RED);

        w.spawn(new Location(w, plat.doorX() + .5, plat.deckY() + 2.6, plat.window().minZ() - .4),
                TextDisplay.class, t -> {
                    t.text(Component.text("Step on the plate to enter the bay", NamedTextColor.GRAY));
                    t.setBillboard(Display.Billboard.CENTER);
                    t.setPersistent(false);
                });
    }

    /** Update a button's label, e.g. to show which certified scoop is loaded. */
    static void relabel(World w, UUID owner, String which, String line) {
        for (var e : w.getEntitiesByClass(TextDisplay.class)) {
            var tags = e.getScoreboardTags();
            if (tags.contains(BUTTON_TAG) && tags.contains(owner.toString()) && tags.contains(which + ":label"))
                e.text(Component.text(line, "certified".equals(which) ? NamedTextColor.GREEN : NamedTextColor.RED));
        }
    }

    /** Remove a tester's button entities, so a reused bay does not inherit them. */
    static void remove(World w, UUID owner) {
        for (var e : w.getEntities())
            if (e.getScoreboardTags().contains(BUTTON_TAG) && e.getScoreboardTags().contains(owner.toString()))
                e.remove();
    }

    private static void button(World w, ChamberPlatform.Pos at, UUID owner, String which,
                               Material block, String title, String hint, NamedTextColor colour) {
        w.getBlockAt(at.x(), at.y(), at.z()).setType(block, false);
        w.spawn(new Location(w, at.x() + .5, at.y() + 1.3, at.z() + .5), TextDisplay.class, t -> {
            t.text(Component.text(title, colour));
            t.setBillboard(Display.Billboard.CENTER);
            t.setPersistent(false);
            t.addScoreboardTag(BUTTON_TAG);
            t.addScoreboardTag(owner.toString());
            t.addScoreboardTag(which + ":label");
        });
        w.spawn(new Location(w, at.x() + .5, at.y() + 1.0, at.z() + .5), TextDisplay.class, t -> {
            t.text(Component.text(hint, NamedTextColor.GRAY));
            t.setBillboard(Display.Billboard.CENTER);
            t.setPersistent(false);
            t.setTransformation(new org.bukkit.util.Transformation(
                    new org.joml.Vector3f(), new org.joml.AxisAngle4f(),
                    new org.joml.Vector3f(.6f, .6f, .6f), new org.joml.AxisAngle4f()));
        });
        w.spawn(new Location(w, at.x() + .5, at.y(), at.z() + .5), Interaction.class, i -> {
            i.setInteractionWidth(1.0f);
            i.setInteractionHeight(1.0f);
            i.setResponsive(true);
            i.setPersistent(false);
            i.addScoreboardTag(BUTTON_TAG);
            i.addScoreboardTag(owner.toString());
            i.addScoreboardTag(which);
        });
    }

    private static void fill(World w, ChamberPlatform.Box b, Material m) {
        for (int x = b.minX(); x <= b.maxX(); x++)
            for (int y = b.minY(); y <= b.maxY(); y++)
                for (int z = b.minZ(); z <= b.maxZ(); z++)
                    w.getBlockAt(x, y, z).setType(m, false);
    }
}
