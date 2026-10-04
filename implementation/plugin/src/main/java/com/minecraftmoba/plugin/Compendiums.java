package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;

/** Issued guides outside matches; never overwrite a player's items or grant command privileges. */
public final class Compendiums implements Listener {
    private static final int REVISION=1;
    private final MobaPlugin plugin;
    private final NamespacedKey kindKey,revisionKey;
    public Compendiums(MobaPlugin plugin) {
        this.plugin=plugin;kindKey=new NamespacedKey(plugin,"compendium");revisionKey=new NamespacedKey(plugin,"compendium_revision");
    }
    static boolean shouldIssue(Match.State state,boolean participant) {
        return !participant || state==Match.State.IDLE;
    }
    private String kind(ItemStack item) {
        if(item==null||!item.hasItemMeta())return null;
        String value=item.getItemMeta().getPersistentDataContainer().get(kindKey,PersistentDataType.STRING);
        return "player".equals(value)||"debug".equals(value)?value:null;
    }
    ItemStack book(String kind) {
        var item=new ItemStack(Material.WRITTEN_BOOK);
        var meta=(BookMeta)item.getItemMeta();
        meta.title(Component.text(kind.equals("debug")?"Debug Compendium":"Player Compendium"));
        meta.author(Component.text("Minecraft MOBA"));
        meta.pages(kind.equals("debug")?CompendiumContent.debugPages():CompendiumContent.playerPages());
        meta.getPersistentDataContainer().set(kindKey,PersistentDataType.STRING,kind);
        meta.getPersistentDataContainer().set(revisionKey,PersistentDataType.INTEGER,REVISION);
        item.setItemMeta(meta);return item;
    }
    public void refresh(Player p) {
        var match=plugin.match();
        boolean issue=shouldIssue(match==null?Match.State.IDLE:match.state(),match!=null&&match.participant(p.getUniqueId())!=null);
        var inventory=p.getInventory();var seen=new java.util.HashSet<String>();
        for(int slot=0;slot<inventory.getSize();slot++) {
            var existing=inventory.getItem(slot);String kind=kind(existing);if(kind==null)continue;
            if(!issue||!seen.add(kind)) { inventory.setItem(slot,null);continue; }
            Integer revision=existing.getItemMeta().getPersistentDataContainer().get(revisionKey,PersistentDataType.INTEGER);
            if(revision==null||revision!=REVISION||existing.getAmount()!=1)inventory.setItem(slot,book(kind));
        }
        if(!issue)return;
        for(String kind:new String[]{"player","debug"})if(!seen.contains(kind)) {
            boolean[] empty=new boolean[36];
            for(int i=0;i<empty.length;i++)empty[i]=inventory.getItem(i)==null||inventory.getItem(i).getType().isAir();
            int slot=freeSlot(empty,kind.equals("player")?6:7,plugin.getConfig().getInt("features.lobbySettings.castModeSlot",8));
            if(slot>=0)inventory.setItem(slot,book(kind));
        }
    }
    static int freeSlot(boolean[] empty,int preferred) {
        return freeSlot(empty,preferred,8);
    }
    static int freeSlot(boolean[] empty,int preferred,int reserved) {
        if(preferred>=0&&preferred<empty.length&&preferred!=reserved&&empty[preferred])return preferred;
        for(int i=0;i<empty.length;i++)if(i!=reserved&&empty[i])return i;
        return -1;
    }
    public boolean command(org.bukkit.command.CommandSender sender,String[] args) {
        if(!(sender instanceof Player p)){sender.sendMessage("Compendiums require an in-game reader.");return true;}
        String kind=args.length==1?"player":args[1].toLowerCase(java.util.Locale.ROOT);
        if(args.length>2||!java.util.Set.of("player","debug").contains(kind)){p.sendMessage("/moba compendium [player|debug]");return true;}
        p.openBook(book(kind));return true;
    }
    @EventHandler public void join(PlayerJoinEvent e){Bukkit.getScheduler().runTask(plugin,()->refresh(e.getPlayer()));}
    @EventHandler public void respawn(PlayerRespawnEvent e){Bukkit.getScheduler().runTask(plugin,()->refresh(e.getPlayer()));}
    @EventHandler public void death(PlayerDeathEvent e){e.getDrops().removeIf(i->kind(i)!=null);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void drop(PlayerDropItemEvent e){if(kind(e.getItemDrop().getItemStack())!=null)e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void click(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p))return;
        boolean numbered=e.getHotbarButton()>=0&&kind(p.getInventory().getItem(e.getHotbarButton()))!=null;
        boolean swap=e.getClick()==ClickType.SWAP_OFFHAND&&kind(p.getInventory().getItemInOffHand())!=null;
        if(kind(e.getCurrentItem())!=null||kind(e.getCursor())!=null||numbered||swap)e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void drag(InventoryDragEvent e){if(kind(e.getOldCursor())!=null)e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void swap(PlayerSwapHandItemsEvent e){if(kind(e.getMainHandItem())!=null||kind(e.getOffHandItem())!=null)e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST)
    public void use(PlayerInteractEvent e){
        String kind=kind(e.getItem());if(kind==null)return;
        if(e.getAction()!=org.bukkit.event.block.Action.RIGHT_CLICK_AIR&&e.getAction()!=org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)return;
        e.setCancelled(true);
        if(e.getHand()==EquipmentSlot.HAND)Bukkit.getScheduler().runTask(plugin,()->e.getPlayer().openBook(book(kind)));
    }
}
