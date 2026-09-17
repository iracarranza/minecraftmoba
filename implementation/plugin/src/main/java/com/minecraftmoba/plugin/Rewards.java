package com.minecraftmoba.plugin;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import java.time.Duration;
import java.util.*;

public final class Rewards implements Listener {
    private final MobaPlugin plugin;
    private final RewardCatalog catalog;
    private final Map<UUID,BossBar> markers=new HashMap<>();
    private final Map<UUID,Menu> menus=new HashMap<>();
    private static final class Menu implements InventoryHolder {
        final UUID player; final int level; final List<RewardCatalog.Option> options;
        Inventory inventory;
        Menu(UUID player,int level,List<RewardCatalog.Option> options) { this.player=player; this.level=level; this.options=options; }
        public Inventory getInventory() { return inventory; }
    }
    public Rewards(MobaPlugin plugin,RewardCatalog catalog) {
        this.plugin=plugin; this.catalog=catalog;
        for(var options:catalog.levels().values()) for(var option:options) {
            Material material=Material.matchMaterial(option.icon());
            if(material==null || !material.isItem() || material.isAir()) throw new IllegalArgumentException("Invalid reward icon: "+option.icon());
        }
    }
    public void levelUp(Player p,int level) {
        var c=plugin.getConfig();
        p.showTitle(Title.title(Component.text("Level "+level),Component.empty(),Title.Times.times(
            ticks(c.getLong("rewards.toast.fadeInTicks")),ticks(c.getLong("rewards.toast.stayTicks")),ticks(c.getLong("rewards.toast.fadeOutTicks")))));
        refresh(p);
    }
    private static Duration ticks(long ticks) { return Duration.ofMillis(Math.multiplyExact(ticks,50)); }
    public void refresh(Player p) {
        if(!plugin.enrolled(p)) return;
        int count=catalog.pending(plugin.data(p)).size();
        if(count==0) {
            BossBar bar=markers.remove(p.getUniqueId()); if(bar!=null) p.hideBossBar(bar);
        } else {
            BossBar bar=markers.computeIfAbsent(p.getUniqueId(),id->BossBar.bossBar(Component.empty(),1,BossBar.Color.YELLOW,BossBar.Overlay.PROGRESS));
            bar.name(Component.text("Unspent choices: "+count+" — /moba rewards")); p.showBossBar(bar);
        }
    }
    public void open(Player p) {
        if(!plugin.enrolled(p)) { p.sendMessage("Use /moba join first."); return; }
        var pending=catalog.pending(plugin.data(p));
        if(pending.isEmpty()) { p.sendMessage("No pending rewards."); return; }
        int level=pending.getFirst(); var options=catalog.levels().get(level);
        Menu menu=new Menu(p.getUniqueId(),level,options);
        menu.inventory=Bukkit.createInventory(menu,((options.size()+8)/9)*9,Component.text("Level "+level+" choice"));
        for(int i=0;i<options.size();i++) {
            var option=options.get(i); var icon=new ItemStack(Objects.requireNonNull(Material.matchMaterial(option.icon())));
            var meta=icon.getItemMeta(); meta.displayName(Component.text(option.label())); icon.setItemMeta(meta);
            menu.inventory.setItem(i,icon); // GUI icons only; never player inventory or cursor
        }
        p.openInventory(menu.inventory); menus.put(p.getUniqueId(),menu);
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void click(InventoryClickEvent e) {
        if(!(e.getView().getTopInventory().getHolder() instanceof Menu menu)) return;
        e.setCancelled(true); // cancel top AND bottom interactions, all transfer modes
        if(!(e.getWhoClicked() instanceof Player p) || !menu.player.equals(p.getUniqueId())
                || menus.get(p.getUniqueId())!=menu || !plugin.enrolled(p)) return;
        if(e.getClick()!=ClickType.LEFT && e.getClick()!=ClickType.RIGHT) return;
        int slot=e.getRawSlot();
        if(slot<0 || slot>=menu.options.size()) return;
        var data=plugin.data(p); var pending=catalog.pending(data);
        if(pending.isEmpty() || pending.getFirst()!=menu.level) return;
        menus.remove(p.getUniqueId()); // consume this menu token before applying; cannot double-spend
        data.choices.add(new PlayerData.ChoiceRecord(menu.level,menu.options.get(slot).id()));
        plugin.applyAndSave(p); // capacity change is immediate, not deferred until close
        Bukkit.getScheduler().runTask(plugin,()-> {
            if(!p.isOnline()) return;
            p.closeInventory();
            if(plugin.enrolled(p) && !catalog.pending(plugin.data(p)).isEmpty()) open(p);
        });
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void drag(InventoryDragEvent e) { if(e.getView().getTopInventory().getHolder() instanceof Menu) e.setCancelled(true); }
    @EventHandler public void close(InventoryCloseEvent e) {
        if(e.getInventory().getHolder() instanceof Menu menu) menus.remove(e.getPlayer().getUniqueId(),menu);
    }
    @EventHandler public void quit(PlayerQuitEvent e) { cleanup(e.getPlayer()); }
    public void cleanup(Player p) {
        menus.remove(p.getUniqueId()); var bar=markers.remove(p.getUniqueId()); if(bar!=null) p.hideBossBar(bar);
    }
    public void invalidate(Player p) {
        menus.remove(p.getUniqueId());
        if(p.getOpenInventory().getTopInventory().getHolder() instanceof Menu) p.closeInventory();
        refresh(p);
    }
}
