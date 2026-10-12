package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * The loadout editor: a chest window that edits one saved loadout, opened from the ender chest in
 * the combat chamber.
 *
 * Items you move into it are the loadout; the window saves when it closes or when you switch
 * loadouts. Moving items takes them out of your own inventory, as a chest does; the buttons along
 * the bottom COPY instead, so a loadout can be built from what you carry, or from what the dummy
 * wears, without losing anything. The layout is {@link LoadoutEditorLayout}.
 *
 * NOT VERIFIED beyond the live acceptance run at the time of writing.
 */
final class LoadoutEditor implements Listener {

    /** Marks the window as ours, and says whose it is. */
    static final class Holder implements InventoryHolder {
        final UUID owner;
        private Inventory inventory;
        Holder(UUID owner) { this.owner = owner; }
        @Override public Inventory getInventory() { return inventory; }
    }

    private final MobaPlugin plugin;
    private final LoadoutStore store;
    private final Function<Player, Player> dummyOf;
    private final java.util.function.BiFunction<Player, Integer, ItemStack> realSlot;
    private final Function<Player, Loadout<ItemStack>> kitOf;

    /** @param realSlot what a player really carries in a slot, seeing through any menu on their hotbar */
    LoadoutEditor(MobaPlugin plugin, LoadoutStore store, Function<Player, Player> dummyOf,
                  java.util.function.BiFunction<Player, Integer, ItemStack> realSlot,
                  Function<Player, Loadout<ItemStack>> kitOf) {
        this.kitOf = kitOf;
        this.plugin = plugin;
        this.store = store;
        this.dummyOf = dummyOf;
        this.realSlot = realSlot;
    }

    boolean viewing(Player p) { return p.getOpenInventory().getTopInventory().getHolder() instanceof Holder; }

    /** The editor window the player has open, or null. */
    Inventory window(Player p) {
        var top = p.getOpenInventory().getTopInventory();
        return top.getHolder() instanceof Holder ? top : null;
    }

    void open(Player p) {
        var holder = new Holder(p.getUniqueId());
        var inv = Bukkit.createInventory(holder, LoadoutEditorLayout.SIZE, Component.text("Loadouts"));
        holder.inventory = inv;
        render(p, inv);
        p.openInventory(inv);
        retitle(p, inv);
    }

    // ---- drawing --------------------------------------------------------------------------

    private void render(Player p, Inventory inv) {
        var book = store.book(p.getUniqueId());
        var loadout = book.current();
        for (int slot = 0; slot < LoadoutEditorLayout.SIZE; slot++) {
            switch (LoadoutEditorLayout.kind(slot)) {
                case ITEM -> inv.setItem(slot, copy(loadout.at(slot)));
                case RESERVED -> inv.setItem(slot, named(Material.BARRIER, "Reserved", NamedTextColor.RED,
                        "The last hotbar slot holds the Exit testing item while you test."));
                case SPACER -> inv.setItem(slot, named(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY));
                case CONTROL -> inv.setItem(slot, control(slot, book));
            }
        }
    }

    /** The title says which loadout is open and how many items it holds. Only possible once the window is open. */
    private void retitle(Player p, Inventory inv) {
        if (p.getOpenInventory().getTopInventory() != inv) return;
        var book = store.book(p.getUniqueId());
        p.getOpenInventory().setTitle(book.selectedName() + " (" + read(inv).count() + " items)");
    }

    private ItemStack control(int slot, LoadoutBook<ItemStack> book) {
        return switch (slot) {
            case LoadoutEditorLayout.SELECT -> {
                var lines = new ArrayList<String>();
                for (int i = 0; i < LoadoutBook.SLOTS; i++)
                    lines.add((i == book.selected() ? "> " : "  ") + LoadoutBook.name(i) + ": " + book.get(i).count() + " items");
                lines.add("Click for the next loadout.");
                yield named(Material.COMPASS, book.selectedName(), NamedTextColor.GOLD, lines.toArray(new String[0]));
            }
            case LoadoutEditorLayout.FILL_ME -> named(Material.CHEST, "Copy my inventory in", NamedTextColor.GREEN,
                    "Replaces this loadout with what you carry.", "Your own items stay with you.");
            case LoadoutEditorLayout.FILL_DUMMY -> named(Material.ARMOR_STAND, "Copy the dummy's gear in", NamedTextColor.AQUA,
                    "Replaces this loadout with what the dummy carries.");
            case LoadoutEditorLayout.FILL_KIT -> named(Material.BARREL, "Fill with my class's kit", NamedTextColor.YELLOW,
                    "Replaces this loadout with what the class consumes", "(alpha.lab.classKits in config; prototype fixtures).");
            case LoadoutEditorLayout.CLEAR -> named(Material.LAVA_BUCKET, "Empty this loadout", NamedTextColor.RED,
                    "Removes every item from the window.");
            default -> named(Material.EMERALD, "Done", NamedTextColor.GREEN, "Saves and closes.");
        };
    }

    private static ItemStack named(Material m, String name, NamedTextColor color, String... lore) {
        var item = new ItemStack(m);
        item.editMeta(meta -> {
            meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
            if (lore.length > 0) meta.lore(java.util.Arrays.stream(lore)
                    .map(l -> (Component) Component.text(l, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)).toList());
        });
        return item;
    }

    private static ItemStack copy(ItemStack s) { return s == null ? null : s.clone(); }

    // ---- reading and saving -----------------------------------------------------------------

    /** The loadout the window currently shows. */
    static Loadout<ItemStack> read(Inventory inv) {
        var l = Loadout.<ItemStack>empty();
        for (int slot = 0; slot < Loadout.SIZE; slot++) {
            if (!LoadoutEditorLayout.editable(slot)) continue;
            var it = inv.getItem(slot);
            if (it != null && !it.getType().isAir() && it.getAmount() > 0) l = l.with(slot, it.clone());
        }
        return l;
    }

    private void saveFrom(Player p, Inventory inv) {
        var book = store.book(p.getUniqueId());
        book.set(book.selected(), read(inv));
        store.save(p.getUniqueId());
    }

    private void fill(Inventory inv, Loadout<ItemStack> l) {
        for (int slot = 0; slot < Loadout.SIZE; slot++)
            if (LoadoutEditorLayout.editable(slot)) inv.setItem(slot, copy(l.at(slot)));
    }

    // ---- the window's rules ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        var top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof Holder)) return;
        if (e.getAction() == InventoryAction.COLLECT_TO_CURSOR) { e.setCancelled(true); return; }
        int raw = e.getRawSlot();
        if (raw < 0 || raw >= LoadoutEditorLayout.SIZE) return;          // the player's own inventory: ordinary
        var kind = LoadoutEditorLayout.kind(raw);
        if (kind == LoadoutEditorLayout.Kind.ITEM) return;
        e.setCancelled(true);
        if (kind != LoadoutEditorLayout.Kind.CONTROL) return;
        if (e.getClick() != ClickType.LEFT && e.getClick() != ClickType.RIGHT) return;
        switch (raw) {
            case LoadoutEditorLayout.SELECT -> {
                saveFrom(p, top);
                store.book(p.getUniqueId()).next();
                store.save(p.getUniqueId());
                render(p, top);
                retitle(p, top);
            }
            case LoadoutEditorLayout.FILL_ME -> { fill(top, LoadoutGear.capture(p, slot -> realSlot.apply(p, slot))); refreshTitle(p, top); }
            case LoadoutEditorLayout.FILL_DUMMY -> {
                Player dummy = dummyOf.apply(p);
                if (dummy == null) { p.sendMessage("There is no dummy to copy."); return; }
                fill(top, LoadoutGear.capture(dummy));
                refreshTitle(p, top);
            }
            case LoadoutEditorLayout.CLEAR -> { fill(top, Loadout.empty()); refreshTitle(p, top); }
            case LoadoutEditorLayout.FILL_KIT -> { fill(top, kitOf.apply(p)); refreshTitle(p, top); }
            case LoadoutEditorLayout.DONE -> Bukkit.getScheduler().runTask(plugin, () -> p.closeInventory());
            default -> { }
        }
    }

    private void refreshTitle(Player p, Inventory top) { retitle(p, top); }

    @EventHandler(priority = EventPriority.HIGH)
    public void drag(InventoryDragEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof Holder)) return;
        for (int raw : e.getRawSlots())
            if (raw < LoadoutEditorLayout.SIZE && !LoadoutEditorLayout.editable(raw)) { e.setCancelled(true); return; }
    }

    @EventHandler
    public void close(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p) || !(e.getInventory().getHolder() instanceof Holder)) return;
        saveFrom(p, e.getInventory());
        var book = store.book(p.getUniqueId());
        p.sendMessage(org.bukkit.ChatColor.GREEN + book.selectedName() + " saved: " + book.current().count() + " item(s).");
    }

    /** Close every open editor, saving, for plugin shutdown. */
    void closeAll() {
        for (Player p : Bukkit.getOnlinePlayers()) if (viewing(p)) p.closeInventory();
    }
}
