package com.minecraftmoba.plugin;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The item shelf, as a window: every item in the game, by category or by name, and a click puts it in the loadout
 * being edited.
 *
 * It exists because a class's abilities consume ordinary items and a tester should not need to leave the lab, or to have
 * had a kit written for their class, to get them. It adds to the SELECTED LOADOUT (the editor's contents, saved), never
 * to the tester's own inventory, so it is safe to use in the chamber and while testing.
 *
 * <ul>
 *   <li>Left-click: a full stack. Right-click: one.
 *   <li>Armor goes to its own slot while that slot is empty; everything else tops up a matching stack before it takes a
 *       free slot (never the reserved Exit slot).
 *   <li>Search is a word or two typed in chat (or {@code /moba lab combat loadout find <words>}).
 * </ul>
 *
 * NOT VERIFIED beyond the live acceptance run at the time of writing; no one has used it with a mouse.
 */
final class ItemPicker implements Listener {
    private static final int PAGE = 45;
    private static final int PREV = 45, NEXT = 46, CATEGORY = 47, SEARCH = 48, CLEAR = 49, INFO = 50, BACK = 53;

    static final class Holder implements InventoryHolder {
        final UUID owner;
        Inventory inventory;
        Holder(UUID owner) { this.owner = owner; }
        @Override public Inventory getInventory() { return inventory; }
    }

    private static final class View {
        ItemCatalogue.Category category = ItemCatalogue.Category.ALL;
        String query = "";
        int page;
    }

    private final MobaPlugin plugin;
    private final LoadoutStore store;
    private final Consumer<Player> back;
    private final Map<UUID, View> views = new HashMap<>();
    private final Set<UUID> searching = new HashSet<>();
    private List<ItemCatalogue.Entry> all;

    ItemPicker(MobaPlugin plugin, LoadoutStore store, Consumer<Player> back) {
        this.plugin = plugin;
        this.store = store;
        this.back = back;
    }

    /** Every obtainable item with the shelf it lives on, built once. */
    List<ItemCatalogue.Entry> all() {
        if (all == null) {
            var out = new ArrayList<ItemCatalogue.Entry>();
            for (Material m : Material.values()) {
                if (m.isLegacy() || m.isAir() || !m.isItem()) continue;
                out.add(new ItemCatalogue.Entry(m, ItemCatalogue.categorize(m.name(), new ItemCatalogue.Facts(m.isEdible(), m.isBlock()))));
            }
            all = out;
        }
        return all;
    }

    boolean viewing(Player p) { return p.getOpenInventory().getTopInventory().getHolder() instanceof Holder; }
    Inventory window(Player p) { return viewing(p) ? p.getOpenInventory().getTopInventory() : null; }

    // ---- opening --------------------------------------------------------------------------

    void open(Player p) { open(p, null, null); }

    /** Open the shelf, optionally on a category and a search (null keeps what the player last had). */
    void open(Player p, ItemCatalogue.Category category, String query) {
        var v = views.computeIfAbsent(p.getUniqueId(), k -> new View());
        if (category != null) v.category = category;
        if (query != null) v.query = query;
        v.page = 0;
        show(p, v);
    }

    private void show(Player p, View v) {
        var holder = new Holder(p.getUniqueId());
        var inv = Bukkit.createInventory(holder, 54, Component.text("Items"));
        holder.inventory = inv;
        render(p, inv, v);
        p.openInventory(inv);
        retitle(p, inv, v);
    }

    private List<Material> shelf(View v) { return ItemCatalogue.shelf(all(), v.category, v.query); }

    private void render(Player p, Inventory inv, View v) {
        inv.clear();
        var shelf = shelf(v);
        int pages = ItemCatalogue.pages(shelf.size(), PAGE);
        v.page = Math.max(0, Math.min(v.page, pages - 1));
        int slot = 0;
        for (Material m : ItemCatalogue.page(shelf, v.page, PAGE)) {
            var item = new ItemStack(m);
            item.editMeta(meta -> meta.lore(List.of(
                    Component.text("Left-click: a full stack", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Right-click: one", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))));
            inv.setItem(slot++, item);
        }
        var book = store.book(p.getUniqueId());
        inv.setItem(PREV, named(Material.ARROW, "Previous page", NamedTextColor.WHITE, "Page " + (v.page + 1) + " of " + pages));
        inv.setItem(NEXT, named(Material.ARROW, "Next page", NamedTextColor.WHITE, "Page " + (v.page + 1) + " of " + pages));
        inv.setItem(CATEGORY, named(v.category.icon(), v.category.label(), NamedTextColor.GOLD, shelf.size() + " item(s)", "Click for the next category."));
        inv.setItem(SEARCH, named(Material.OAK_SIGN, v.query.isBlank() ? "Search by name" : "Search: " + v.query, NamedTextColor.AQUA, "Click, then type a word or two in chat."));
        if (!v.query.isBlank()) inv.setItem(CLEAR, named(Material.BARRIER, "Clear the search", NamedTextColor.RED));
        inv.setItem(INFO, named(Material.PAPER, "Adding to " + book.selectedName(), NamedTextColor.GREEN,
                book.current().count() + " of " + (Loadout.INVENTORY - 1 + 4) + " slots used"));
        inv.setItem(BACK, named(Material.EMERALD, "Back to the loadout", NamedTextColor.GREEN, "Your additions are already saved."));
    }

    private void retitle(Player p, Inventory inv, View v) {
        if (p.getOpenInventory().getTopInventory() != inv) return;
        int pages = ItemCatalogue.pages(shelf(v).size(), PAGE);
        p.getOpenInventory().setTitle("Items: " + v.category.label() + (v.query.isBlank() ? "" : " \"" + v.query + "\"") + "  " + (v.page + 1) + "/" + pages);
    }

    private static ItemStack named(Material m, String name, NamedTextColor color, String... lore) {
        var item = new ItemStack(m);
        item.editMeta(meta -> {
            meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
            if (lore.length > 0) meta.lore(Arrays.stream(lore).map(l -> (Component) Component.text(l, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)).toList());
        });
        return item;
    }

    // ---- adding to the loadout --------------------------------------------------------------

    /**
     * Put {@code amount} of {@code material} in the selected loadout. Returns how many went in (0 when it is full).
     * Armor takes its own slot while that is empty; anything else tops up matching stacks first, then free slots.
     */
    int add(Player p, Material material, int amount) {
        var book = store.book(p.getUniqueId());
        var loadout = book.current();
        int max = new ItemStack(material).getMaxStackSize();
        int left = Math.min(amount, max);
        int placed = 0;
        int armor = ItemCatalogue.armorSlot(material.name());
        if (armor >= 0 && loadout.at(armor) == null) {
            loadout = loadout.with(armor, new ItemStack(material));
            book.set(book.selected(), loadout); store.save(p.getUniqueId());
            return 1;
        }
        for (int i = 0; i < Loadout.INVENTORY && left > 0; i++) {            // top up a matching stack first
            var have = loadout.at(i);
            if (Loadout.reserved(i) || have == null || have.getType() != material || have.getAmount() >= have.getMaxStackSize() || have.hasItemMeta()) continue;
            int room = have.getMaxStackSize() - have.getAmount(), take = Math.min(room, left);
            var grown = have.clone(); grown.setAmount(have.getAmount() + take);
            loadout = loadout.with(i, grown); left -= take; placed += take;
        }
        while (left > 0) {
            int free = ItemCatalogue.firstFree(loadout);
            if (free < 0) break;
            int take = Math.min(left, max);
            loadout = loadout.with(free, new ItemStack(material, take)); left -= take; placed += take;
        }
        if (placed > 0) { book.set(book.selected(), loadout); store.save(p.getUniqueId()); }
        return placed;
    }

    // ---- the window's rules -----------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !(e.getView().getTopInventory().getHolder() instanceof Holder)) return;
        e.setCancelled(true);                                                  // nothing is ever taken out of the shelf
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;
        var v = views.computeIfAbsent(p.getUniqueId(), k -> new View());
        var top = e.getView().getTopInventory();
        int raw = e.getRawSlot();
        if (e.getClick() != ClickType.LEFT && e.getClick() != ClickType.RIGHT && e.getClick() != ClickType.SHIFT_LEFT) return;
        if (raw < PAGE) {
            var item = top.getItem(raw);
            if (item == null) return;
            int wanted = e.getClick() == ClickType.RIGHT ? 1 : item.getMaxStackSize();
            int placed = add(p, item.getType(), wanted);
            p.sendActionBar(Component.text(placed == 0 ? "The loadout is full." : "Added " + placed + " x " + item.getType().name().toLowerCase().replace('_', ' ')
                    + " to " + store.book(p.getUniqueId()).selectedName(), placed == 0 ? NamedTextColor.RED : NamedTextColor.GREEN));
            render(p, top, v);
            return;
        }
        switch (raw) {
            case PREV -> { v.page--; render(p, top, v); retitle(p, top, v); }
            case NEXT -> { v.page++; render(p, top, v); retitle(p, top, v); }
            case CATEGORY -> { v.category = v.category.next(); v.page = 0; render(p, top, v); retitle(p, top, v); }
            case CLEAR -> { v.query = ""; v.page = 0; render(p, top, v); retitle(p, top, v); }
            case SEARCH -> Bukkit.getScheduler().runTask(plugin, () -> {
                p.closeInventory();
                searching.add(p.getUniqueId());
                p.sendMessage(net.md_5.bungee.api.ChatColor.AQUA + "Type a word or two of the item's name in chat (or 'cancel').");
            });
            case BACK -> Bukkit.getScheduler().runTask(plugin, () -> back.accept(p));
            default -> { }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void drag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Holder) e.setCancelled(true);
    }

    /** The search prompt: the player's next chat line is the query. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void chat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        if (!searching.remove(p.getUniqueId())) return;
        e.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> { if (!text.equalsIgnoreCase("cancel")) search(p, text); else open(p); });
    }

    void search(Player p, String query) { open(p, null, query); }

    @EventHandler public void quit(PlayerQuitEvent e) { searching.remove(e.getPlayer().getUniqueId()); views.remove(e.getPlayer().getUniqueId()); }

    void closeAll() { for (Player p : Bukkit.getOnlinePlayers()) if (viewing(p)) p.closeInventory(); }
}
