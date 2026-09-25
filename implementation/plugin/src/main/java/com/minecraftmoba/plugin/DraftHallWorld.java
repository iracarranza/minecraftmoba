package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/** Builds the authored class-draft room in the dedicated lobby world. */
public final class DraftHallWorld {
    private final MobaPlugin plugin;
    private final NamespacedKey marker;
    private final NamespacedKey labelMarker;
    private World world;
    private int ox, oz;

    public DraftHallWorld(MobaPlugin plugin) {
        this.plugin = plugin;
        marker = new NamespacedKey(plugin, "draft_hall");
        labelMarker = new NamespacedKey(plugin, "draft_hall_label");
    }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.draftHall.enabled", true); }
    public World world() { return world != null ? world : plugin.lobbyWorld().world(); }
    private int floorY() { return plugin.getConfig().getInt("features.draftHall.floorY", 64); }
    private int rankOffset() { return plugin.getConfig().getInt("features.draftHall.rankOffset", 6); }
    private int pitDepth() { return plugin.getConfig().getInt("features.draftHall.pitDepth", 2); }
    private int spacing() { return plugin.getConfig().getInt("features.draftHall.spacing", 2); }
    private int height() { return plugin.getConfig().getInt("features.draftHall.height", 8); }
    private int radius() { return DraftHall.requiredRadius(rankOffset(), spacing()); }

    public void ensure() {
        if (!enabled() || plugin.lobbyWorld() == null) return;
        world = plugin.lobbyWorld().world();
        ox = plugin.getConfig().getInt("features.draftHall.originX", 40);
        oz = plugin.getConfig().getInt("features.draftHall.originZ", 0);
        if (world == null) return;
        build();
    }

    public Location spawn(Team team) {
        World w = world();
        if (w == null) return null;
        int[] p = DraftHall.spawn(team, floorY(), rankOffset());
        return new Location(w, ox + p[0] + .5, p[1], oz + p[2] + .5);
    }

    public DraftHall.Stand nearest(Team team, int index) {
        return DraftHall.stands(floorY(), rankOffset(), pitDepth(), spacing()).stream()
                .filter(s -> s.slot() == DraftHall.Slot.TEAM && s.team() == team && s.index() == index)
                .findFirst().orElseThrow();
    }

    public ArmorStand stand(DraftHall.Stand s) {
        if (world == null) return null;
        Location at = new Location(world, ox + s.x() + .5, s.y(), oz + s.z() + .5);
        for (Entity e : world.getNearbyEntities(at, .4, .8, .4))
            if (e instanceof ArmorStand a && a.getPersistentDataContainer().has(marker, PersistentDataType.BYTE)) return a;
        ArmorStand a = (ArmorStand) world.spawnEntity(at, EntityType.ARMOR_STAND);
        a.getPersistentDataContainer().set(marker, PersistentDataType.BYTE, (byte) 1);
        a.setGravity(false); a.setInvulnerable(true); a.setCollidable(false);
        a.setVisible(true); a.setArms(true); a.setBasePlate(false);
        a.setCustomNameVisible(true);
        return a;
    }

    public void clear() {
        if (world == null) return;
        for (Entity e : new ArrayList<>(world.getEntities()))
            if (e instanceof ArmorStand a && a.getPersistentDataContainer().has(marker, PersistentDataType.BYTE)) a.remove();
    }

    private void build() {
        Material floor = material("floorMaterial", "SMOOTH_STONE");
        Material wall = material("wallMaterial", "STONE_BRICKS");
        Material ceiling = material("ceilingMaterial", "STONE_BRICKS");
        Material light = material("lightMaterial", "SEA_LANTERN");
        Material platform = material("platformMaterial", "POLISHED_ANDESITE");
        Material pit = material("pitMaterial", "DEEPSLATE_TILES");
        for (var p : LobbyHall.hall(radius(), floorY(), height(), 4)) {
            Material m = switch (p.role()) { case FLOOR -> floor; case WALL -> wall; case CEILING -> ceiling; case LIGHT -> light; case PLATFORM -> platform; };
            world.getBlockAt(ox + p.x(), p.y(), oz + p.z()).setType(m, false);
        }
        for (var p : DraftHall.pit(floorY(), pitDepth(), 7, 3))
            world.getBlockAt(ox + p.x(), p.y(), oz + p.z()).setType(pit, false);
        for (var s : DraftHall.stands(floorY(), rankOffset(), pitDepth(), spacing())) stand(s);
        label("NORTH", -rankOffset(), floorY() + 2);
        label("SOUTH", rankOffset(), floorY() + 2);
        label("BANNED", -8, floorY() + 2);
        label("BANNED", 8, floorY() + 2);
    }

    private void label(String text, int z, int y) {
        Location at = new Location(world, ox + .5, y, oz + z + .5);
        for (Entity e : world.getNearbyEntities(at, 1.2, 1.0, 1.2))
            if (e instanceof TextDisplay t && t.getPersistentDataContainer().has(labelMarker, PersistentDataType.BYTE)) {
                t.text(net.kyori.adventure.text.Component.text(text)); return;
            }
        TextDisplay t = (TextDisplay) world.spawnEntity(at, EntityType.TEXT_DISPLAY);
        t.getPersistentDataContainer().set(labelMarker, PersistentDataType.BYTE, (byte) 1);
        t.text(net.kyori.adventure.text.Component.text(text));
        t.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
        t.setSeeThrough(false);
        t.setShadowed(true);
    }

    private Material material(String key, String fallback) {
        Material m = Material.matchMaterial(plugin.getConfig().getString("features.draftHall." + key, fallback));
        if (m == null) throw new IllegalArgumentException("features.draftHall: not a material: " + key);
        return m;
    }
}
