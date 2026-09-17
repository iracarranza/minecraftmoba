package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import java.util.*;

public final class AbilityInputs implements Listener {
    public enum Input { SWAP_HAND, LEFT_CLICK, RIGHT_CLICK, DROP }
    private record Channel(Location origin, long end, double distanceSquared) {}
    private final MobaPlugin plugin;
    private final Provenance provenance;
    private final Map<String, Ability> abilities;
    private final Map<String, Map<Input, Ability>> kits = new HashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>(), lastFire = new HashMap<>();
    private final Map<UUID, Channel> channels = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> executionCounts = new HashMap<>();
    private final Input modeInput;
    private final long timeout;
    private long tick;
    public AbilityInputs(MobaPlugin plugin, Provenance provenance) {
        this.plugin=plugin; this.provenance=provenance; abilities=TestAbilities.create(plugin);
        var c=plugin.getConfig(); timeout=c.getLong("abilities.modeTimeoutTicks");
        modeInput=Input.valueOf(c.getString("abilities.bindings.mode"));
        Set<Input> bindings = new HashSet<>(); bindings.add(modeInput);
        for (String slot : List.of("a1","a2","ult")) {
            if (!bindings.add(Input.valueOf(c.getString("abilities.bindings." + slot))))
                throw new IllegalArgumentException("Ability input bindings must be distinct");
        }
        var classes=Objects.requireNonNull(c.getConfigurationSection("abilities.classes"));
        for (String id : classes.getKeys(false)) {
            Map<Input, Ability> kit = new EnumMap<>(Input.class);
            for (String slot : List.of("a1","a2","ult")) {
                String abilityId=classes.getString(id+"."+slot);
                Ability ability=abilities.get(abilityId);
                if (ability == null) throw new IllegalArgumentException("Unknown ability: " + abilityId);
                kit.put(Input.valueOf(c.getString("abilities.bindings."+slot)), ability);
            }
            kits.put(id, Map.copyOf(kit));
        }
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1, 1); // server tick cadence, not a balance constant
    }
    public boolean active(Player p) { return plugin.enrolled(p) && plugin.data(p).modeState.active; }
    public boolean input(Player p, Input input) {
        if (!plugin.enrolled(p)) return false;
        var d=plugin.data(p);
        if (input == modeInput) {
            if (active(p)) exit(p, false);
            else {
                d.modeState.active=true; d.modeState.expiresAt=tick+timeout;
                sound(p,"enter"); bar(p);
            }
            return true;
        }
        if (!active(p)) return false;
        var kit=kits.get(d.classId);
        if (kit == null) return true;
        Ability ability=kit.get(input);
        if (ability == null) return true;
        var last=lastFire.computeIfAbsent(p.getUniqueId(), k->new HashMap<>());
        var ready=cooldowns.computeIfAbsent(p.getUniqueId(), k->new HashMap<>());
        if (last.getOrDefault(ability.id(), Long.MIN_VALUE) == tick || ready.getOrDefault(ability.id(), 0L)>tick) return true;
        last.put(ability.id(), tick);
        if (ability.execute(p,new Ability.AbilityContext(plugin,provenance,this))) {
            ready.put(ability.id(),tick+ability.cooldownTicks());
            executionCounts.computeIfAbsent(p.getUniqueId(),k->new HashMap<>()).merge(ability.id(),1,Integer::sum);
            if (plugin.getConfig().getBoolean("abilities.logExecutions"))
                plugin.getLogger().info("ABILITY player="+p.getName()+" id="+ability.id()+" tick="+tick);
        }
        bar(p); return true;
    }
    public void exit(Player p, boolean silent) {
        if (plugin.enrolled(p)) plugin.data(p).modeState.clear();
        channels.remove(p.getUniqueId());
        p.sendActionBar(Component.empty());
        if (!silent) sound(p,"exit");
    }
    public void forget(Player p) {
        exit(p,true); cooldowns.remove(p.getUniqueId()); lastFire.remove(p.getUniqueId()); executionCounts.remove(p.getUniqueId());
    }
    public void channel(Player p,long duration,double threshold) {
        channels.put(p.getUniqueId(), new Channel(p.getLocation().clone(),tick+duration,threshold*threshold));
        p.sendMessage("Channel started");
    }
    public String debug(Player p) { return "executions="+executionCounts.getOrDefault(p.getUniqueId(),Map.of())+" channel="+channels.containsKey(p.getUniqueId()); }
    private void tick() {
        tick++;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (active(p) && tick > plugin.data(p).modeState.expiresAt) exit(p,true);
            Channel channel=channels.get(p.getUniqueId());
            if (channel != null) {
                if (!active(p) || p.isDead() || !p.getWorld().equals(channel.origin.getWorld())
                        || p.getLocation().distanceSquared(channel.origin)>channel.distanceSquared) {
                    channels.remove(p.getUniqueId()); p.sendMessage("Channel aborted");
                } else if (tick>=channel.end) {
                    channels.remove(p.getUniqueId()); p.sendMessage("Channel complete");
                }
            }
            if (active(p)) bar(p);
        }
    }
    private void sound(Player p,String event) {
        var c=plugin.getConfig(); String path="abilities.feedback."+event;
        p.playSound(p.getLocation(),Objects.requireNonNull(c.getString(path+".sound")),
            (float)c.getDouble(path+".volume"),(float)c.getDouble(path+".pitch"));
    }
    private void bar(Player p) {
        var kit=kits.get(plugin.data(p).classId);
        Component bar=Component.empty();
        for (String slot : List.of("a1","a2","ult")) {
            Input input=Input.valueOf(plugin.getConfig().getString("abilities.bindings."+slot));
            Ability ability=kit==null?null:kit.get(input);
            String label=switch(input) { case LEFT_CLICK -> "M1"; case RIGHT_CLICK -> "M2"; case DROP -> "Q"; case SWAP_HAND -> "F"; };
            boolean cooling=ability!=null && cooldowns.getOrDefault(p.getUniqueId(),Map.of()).getOrDefault(ability.id(),0L)>tick;
            bar=bar.append(Component.text(label+" "+(ability==null?"—":ability.displayName())+"   ",cooling?NamedTextColor.GRAY:NamedTextColor.WHITE));
        }
        p.sendActionBar(bar);
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void swap(PlayerSwapHandItemsEvent e) {
        if (plugin.enrolled(e.getPlayer())) { e.setCancelled(true); input(e.getPlayer(),Input.SWAP_HAND); }
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void interact(PlayerInteractEvent e) {
        boolean inMode=active(e.getPlayer());
        if (inMode) e.setCancelled(true);
        Input input=switch(e.getAction()) {
            case LEFT_CLICK_AIR,LEFT_CLICK_BLOCK -> Input.LEFT_CLICK;
            case RIGHT_CLICK_AIR,RIGHT_CLICK_BLOCK -> Input.RIGHT_CLICK;
            default -> null;
        };
        if (input!=null && input(e.getPlayer(),input)) e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void attack(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p && input(p,Input.LEFT_CLICK)) e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void entity(PlayerInteractEntityEvent e) {
        if (active(e.getPlayer())) e.setCancelled(true);
        if (input(e.getPlayer(),Input.RIGHT_CLICK)) e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void entityAt(PlayerInteractAtEntityEvent e) { entity(e); }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void drop(PlayerDropItemEvent e) { if (active(e.getPlayer()) || plugin.isMap(e.getItemDrop().getItemStack())) e.setCancelled(true); }
    @EventHandler public void death(PlayerDeathEvent e) { exit(e.getPlayer(),true); }
    @EventHandler public void quit(PlayerQuitEvent e) { forget(e.getPlayer()); }
}
