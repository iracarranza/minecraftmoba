package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The combat chamber's pre-entry flow as an inventory menu, so a tester picks a class, a
 * role, an ability and the modes by clicking instead of typing.
 *
 * What is on the screen and what each slot means is {@link CombatPreEntryView}, pure and
 * tested; this draws it and runs the slot's action as the typed command it is. Each press
 * runs the command, and the chamber reopens the menu on whatever step the flow is now at.
 *
 * Closing the menu does not cancel the flow: {@code /moba lab combat status} reopens it.
 *
 * NOT VERIFIED beyond the live acceptance run at the time of writing.
 */
public final class CombatPreEntryMenu implements Listener {
    private static final class Holder implements InventoryHolder {
        Inventory inventory;
        @Override public Inventory getInventory() { return inventory; }
    }

    private final MobaPlugin plugin;
    private final CombatChamber chamber;
    /** What each slot does, per tester, for the screen currently drawn. */
    private final Map<UUID, Map<Integer, String>> actions = new HashMap<>();

    CombatPreEntryMenu(MobaPlugin plugin, CombatChamber chamber) {
        this.plugin = plugin;
        this.chamber = chamber;
    }

    void open(Player p, CombatPreEntry flow) {
        var screen = CombatPreEntryView.of(flow, plugin.settings().maxLevel());
        var holder = new Holder();
        holder.inventory = Bukkit.createInventory(holder, screen.size(), Component.text(screen.title()));
        var map = new HashMap<Integer, String>();
        for (var slot : screen.slots()) {
            holder.inventory.setItem(slot.index(), stack(slot));
            if (slot.action() != null) map.put(slot.index(), slot.action());
        }
        actions.put(p.getUniqueId(), map);
        p.openInventory(holder.inventory);
    }

    void close(Player p) {
        actions.remove(p.getUniqueId());
        if (p.getOpenInventory().getTopInventory().getHolder() instanceof Holder) p.closeInventory();
    }

    void shutdown() { actions.clear(); HandlerList.unregisterAll(this); }

    /** What a slot does right now, or null. Public for acceptance runs that cannot click a real client. */
    public String actionAt(Player p, int slot) {
        var map = actions.get(p.getUniqueId());
        return map == null ? null : map.get(slot);
    }

    /** Press a slot: run its action as the typed command. */
    public void clickSlot(Player p, int slot) {
        String action = actionAt(p, slot);
        if (action == null) return;
        var full = new java.util.ArrayList<String>(List.of("lab", "combat"));
        full.addAll(List.of(action.split(" ")));
        chamber.command(p, full.toArray(new String[0]));
    }

    private static ItemStack stack(CombatPreEntryView.Slot slot) {
        Material m = switch (slot.kind()) {
            case BACK -> Material.ARROW;
            case ENTER -> Material.EMERALD_BLOCK;
            case INFO -> Material.WRITABLE_BOOK;
            case MODE -> switch (slot.id()) {
                case "cooldown" -> Material.CLOCK; case "time" -> Material.DAYLIGHT_DETECTOR;
                case "level" -> Material.EXPERIENCE_BOTTLE; default -> Material.ZOMBIE_HEAD;
            };
            case OPTION -> slot.available() ? Material.PAPER : Material.GRAY_DYE;
        };
        var stack = new ItemStack(m);
        stack.editMeta(meta -> {
            meta.displayName(Component.text(slot.label(), slot.available() ? NamedTextColor.WHITE : NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            if (slot.reason() != null)
                meta.lore(List.of(Component.text("Unavailable: " + slot.reason(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        });
        return stack;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void click(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof Holder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() != e.getView().getTopInventory()) return;
        int slot = e.getSlot();
        if (actionAt(p, slot) == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> clickSlot(p, slot));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void drag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Holder) e.setCancelled(true);
    }

    @EventHandler
    public void closed(InventoryCloseEvent e) {
        // A reopened screen replaces its predecessor, so a close only forgets the screen if nothing newer is open.
        if (e.getInventory().getHolder() instanceof Holder && !(e.getPlayer().getOpenInventory().getTopInventory().getHolder() instanceof Holder))
            actions.remove(e.getPlayer().getUniqueId());
    }
}
