package com.minecraftmoba.plugin;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Where a tester's loadouts live, and where their real inventory waits while they are in testing mode.
 *
 * <h2>Loadouts</h2>
 *
 * One file per tester, {@code <uuid>.yml}: which loadout is selected and the five loadouts, each as
 * Paper's own item serialisation, base64. They survive a restart, so a loadout built once is there
 * next session.
 *
 * <h2>The testing-mode snapshot is written to disk before anything is cleared</h2>
 *
 * Testing mode empties the tester's inventory. The real one is held in memory AND written to
 * {@code <uuid>.restore} first, so a crash or a kill during testing cannot cost anyone their gear:
 * {@link TestingMode} restores from the file at the next join. The file is deleted only after the
 * inventory has been given back.
 */
final class LoadoutStore {
    private final File dir;
    private final Map<UUID, LoadoutBook<ItemStack>> books = new HashMap<>();

    LoadoutStore(File dir) { this.dir = dir; }

    // ---- loadouts -------------------------------------------------------------------------

    LoadoutBook<ItemStack> book(UUID id) {
        return books.computeIfAbsent(id, this::load);
    }

    private LoadoutBook<ItemStack> load(UUID id) {
        var book = new LoadoutBook<ItemStack>();
        File f = new File(dir, id + ".yml");
        if (!f.exists()) return book;
        var yaml = YamlConfiguration.loadConfiguration(f);
        for (int i = 0; i < LoadoutBook.SLOTS; i++) {
            String data = yaml.getString("loadout" + i);
            if (data != null && !data.isBlank()) {
                try { book.set(i, decode(data)); } catch (RuntimeException ex) { /* a corrupt loadout is empty, not fatal */ }
            }
        }
        book.select(Math.max(0, Math.min(LoadoutBook.SLOTS - 1, yaml.getInt("selected", 0))));
        return book;
    }

    void save(UUID id) {
        var book = books.get(id);
        if (book == null) return;
        var yaml = new YamlConfiguration();
        yaml.set("selected", book.selected());
        for (int i = 0; i < LoadoutBook.SLOTS; i++) yaml.set("loadout" + i, encode(book.get(i).items()));
        try {
            Files.createDirectories(dir.toPath());
            yaml.save(new File(dir, id + ".yml"));
        } catch (IOException ex) {
            throw new IllegalStateException("Could not save loadouts: " + ex.getMessage(), ex);
        }
    }

    // ---- the real inventory, parked ----------------------------------------------------------

    void park(UUID id, List<ItemStack> items) {
        try {
            Files.createDirectories(dir.toPath());
            Files.writeString(new File(dir, id + ".restore").toPath(), encode(items), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not park your inventory, so testing mode was NOT started: " + ex.getMessage(), ex);
        }
    }

    boolean hasParked(UUID id) { return new File(dir, id + ".restore").exists(); }

    /** The parked inventory, or null. Does not delete it: call {@link #discardParked} once it is safely back. */
    List<ItemStack> parked(UUID id) {
        File f = new File(dir, id + ".restore");
        if (!f.exists()) return null;
        try { return decodeItems(Files.readString(f.toPath(), StandardCharsets.UTF_8)); }
        catch (IOException | RuntimeException ex) { return null; }
    }

    void discardParked(UUID id) { new File(dir, id + ".restore").delete(); }

    // ---- encoding ---------------------------------------------------------------------------

    static String encode(List<ItemStack> items) {
        var padded = new ArrayList<ItemStack>(Loadout.SIZE);
        for (int i = 0; i < Loadout.SIZE; i++) {
            var it = i < items.size() ? items.get(i) : null;
            padded.add(it == null ? new ItemStack(Material.AIR) : it);
        }
        return Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(padded));
    }

    static Loadout<ItemStack> decode(String data) { return Loadout.of(decodeItems(data)); }

    /** Every slot as stored, including the reserved one: a parked real inventory may legitimately use it. */
    static List<ItemStack> decodeItems(String data) {
        var raw = ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(data));
        var items = new ArrayList<ItemStack>(Loadout.SIZE);
        for (int i = 0; i < Loadout.SIZE; i++) {
            ItemStack it = i < raw.length ? raw[i] : null;
            items.add(it == null || it.getType().isAir() ? null : it);
        }
        return items;
    }
}
