package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import java.util.*;

/** Buffet owns a hotbar loan, not the victim's items. Every exit returns that loan once. */
final class IrresistibleBuffetAbility implements Ability, Listener {
    private static final class Table {
        final Player chef; final Location origin; final Vector direction; final ItemStack food;
        final List<Entity> furniture = new ArrayList<>(); final Set<UUID> caught = new HashSet<>();
        final int start; int segments;
        Table(Player chef, Location origin, Vector direction, ItemStack food, int start) {
            this.chef = chef; this.origin = origin; this.direction = direction; this.food = food; this.start = start;
        }
    }
    private record Seat(Player victim, Table table, ArmorStand chair, int[] eaten) {}
    private final MobaPlugin plugin;
    private final ConfigurationSection config;
    private final NamespacedKey savedKey, servingKey;
    private final HotbarCustody<ItemStack> custody = new HotbarCustody<>();
    private final List<Table> tables = new ArrayList<>();
    private final Map<UUID, Seat> seats = new HashMap<>();
    private int clock;

    IrresistibleBuffetAbility(MobaPlugin plugin, ConfigurationSection config) {
        this.plugin = plugin; this.config = config;
        savedKey = new NamespacedKey(plugin, "buffet_hotbar"); servingKey = new NamespacedKey(plugin, "buffet_serving");
    }
    public String id() { return "irresistible_buffet"; }
    public String displayName() { return "Irresistible Buffet"; }
    public long cooldownTicks() { return config.getLong("cooldownTicks"); }
    public List<AbilityOutput> outputs() {
        return AbilityOutput.single("irresistible_buffet", TargetForm.AREA_ENTITIES, InputForm.INSTANT, Recipients.ENEMIES);
    }
    boolean seated(Player p) { return seats.containsKey(p.getUniqueId()); }
    boolean serving(ItemStack s) { return ChefItems.tagged(s, servingKey); }

    public List<Need> needs(Player chef, AbilityContext ctx) {
        return List.of(new Need("food in your inventory to serve (any edible item)", (p, c) -> ChefItems.foodSlot(p) >= 0, (p, c) -> "you carry none"));
    }
    public boolean execute(Player chef, AbilityContext ctx) {
        int slot = ChefItems.foodSlot(chef);
        if (slot < 0) return false;                         // AbilityInputs explains it: see needs()
        var food = chef.getInventory().getItem(slot).asOne();
        Vector direction = chef.getLocation().getDirection().setY(0);
        if (direction.lengthSquared() < 1e-6) return false;
        tables.add(new Table(chef, chef.getLocation(), direction.normalize(), food, clock));
        return true; // Chef immediately remains free to act.
    }

    public void tick() {
        clock++;
        for (var t : List.copyOf(tables)) {
            if (!t.chef.isOnline() || t.chef.isDead() || !t.chef.getWorld().equals(t.origin.getWorld())
                    || clock - t.start >= config.getInt("durationTicks")) { clearTable(t); continue; }
            if (t.segments < config.getInt("length") && (clock - t.start) % config.getInt("rollTicks") == 0) {
                Location at = t.origin.clone().add(t.direction.clone().multiply(++t.segments));
                if (!at.getChunk().isLoaded() || !at.getBlock().isPassable() || !at.clone().add(0, 1, 0).getBlock().isPassable()) {
                    t.segments = config.getInt("length"); continue;
                }
                BlockDisplay tabletop = at.getWorld().spawn(at.clone().add(-.5, .7, -.5), BlockDisplay.class,
                        d -> { d.setBlock(Material.OAK_PLANKS.createBlockData()); d.setPersistent(false); });
                t.furniture.add(tabletop);
                at.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, at.clone().add(0, 1.5, 0), 3, .5, .1, .5, 0);
                double width = config.getDouble("catchRadius");
                for (var e : AbilityTargeting.within(t.chef, at, width, 2, width, AbilityTargeting.Relation.ENEMY, false))
                    if (e instanceof Player victim && !t.caught.contains(victim.getUniqueId())) seat(t, victim, at);
            }
        }
        for (var s : List.copyOf(seats.values()))
            if (!s.victim().isOnline() || s.victim().isDead() || !s.chair().isValid()
                    || !s.chair().getPassengers().contains(s.victim())) release(s.victim(), !s.victim().isDead(), true);
    }

    private void seat(Table table, Player victim, Location at) {
        if (seated(victim) || victim.isInsideVehicle() || !victim.getItemOnCursor().isEmpty()) return;
        // Nine real pieces of Chef's chosen stored food per invitation. No synthetic free food.
        if (!spendServings(table.chef, table.food, 9)) return;
        if (plugin.inputs() != null) plugin.inputs().exit(victim, true);
        victim.closeInventory();
        var real = new ArrayList<ItemStack>();
        for (int i = 0; i < 9; i++) { var s = victim.getInventory().getItem(i); real.add(s == null ? null : s.clone()); }
        if (!custody.hold(victim.getUniqueId(), real)) return;
        // Persist the originals before the overlay: reconnect can recover even after a crash.
        victim.getPersistentDataContainer().set(savedKey, PersistentDataType.BYTE_ARRAY,
                ItemStack.serializeItemsAsBytes(real));
        victim.saveData();
        var chair = at.getWorld().spawn(at.clone().add(0, .1, 0), ArmorStand.class,
                e -> { e.setVisible(false); e.setMarker(true); e.setGravity(false); e.setInvulnerable(true); e.setPersistent(false); });
        table.furniture.add(chair);
        var serving = table.food.asOne(); var meta = serving.getItemMeta();
        meta.displayName(Component.text("Buffet serving"));
        meta.getPersistentDataContainer().set(servingKey, PersistentDataType.BYTE, (byte) 1); serving.setItemMeta(meta);
        for (int i = 0; i < 9; i++) victim.getInventory().setItem(i, serving.clone());
        seats.put(victim.getUniqueId(), new Seat(victim, table, chair, new int[1]));
        table.caught.add(victim.getUniqueId());
        if (!chair.addPassenger(victim)) release(victim, false, true);
        else victim.sendMessage("Eat, jump or dismount to leave the Buffet.");
    }

    static boolean spendServings(Player chef, ItemStack chosen, int count) {
        int available = 0;
        for (var s : chef.getInventory().getStorageContents()) if (s != null && s.isSimilar(chosen)) available += s.getAmount();
        if (available < count) return false;
        for (int i = 0; i < 36 && count > 0; i++) {
            var s = chef.getInventory().getItem(i); if (s == null || !s.isSimilar(chosen)) continue;
            int take = Math.min(count, s.getAmount()); count -= take;
            chef.getInventory().setItem(i, s.getAmount() == take ? null : s.asQuantity(s.getAmount() - take));
        }
        return true;
    }

    private List<ItemStack> originals(Player p) {
        var real = custody.release(p.getUniqueId());
        if (real != null) return real;
        byte[] bytes = p.getPersistentDataContainer().get(savedKey, PersistentDataType.BYTE_ARRAY);
        return bytes == null ? null : Arrays.asList(ItemStack.deserializeItemsFromBytes(bytes));
    }
    private void clearOverlay(Player p) {
        for (int i = 0; i < 36; i++) if (serving(p.getInventory().getItem(i))) p.getInventory().setItem(i, null);
        if (serving(p.getInventory().getItemInOffHand())) p.getInventory().setItemInOffHand(null);
        if (serving(p.getItemOnCursor())) p.setItemOnCursor(null);
    }
    private void release(Player p, boolean penalty, boolean writeBack) {
        var seat = seats.remove(p.getUniqueId()); var real = originals(p);
        if (real == null) return;
        clearOverlay(p);
        if (writeBack) for (int i = 0; i < 9; i++) p.getInventory().setItem(i, real.get(i));
        p.getPersistentDataContainer().remove(savedKey);
        if (seat != null) seat.chair().remove();
        p.saveData();
        if (penalty && seat != null && !p.isDead())
            AbilityDamage.deal(p, Math.max(0, 9 - seat.eaten()[0]) * config.getDouble("damagePerServing"), seat.table().chef);
    }
    private void clearTable(Table t) {
        for (var s : List.copyOf(seats.values())) if (s.table() == t) release(s.victim(), false, true);
        t.furniture.forEach(Entity::remove); tables.remove(t);
    }
    public void cancel(Player p) {
        if (seated(p)) release(p, false, true);
        for (var t : List.copyOf(tables)) if (t.chef.equals(p)) clearTable(t);
    }
    public void close() { for (var t : List.copyOf(tables)) clearTable(t); }

    @EventHandler(priority = EventPriority.LOWEST) public void death(PlayerDeathEvent e) {
        var p = e.getPlayer(); if (!seated(p)) return;
        var real = originals(p); if (real == null) return;
        // Keep original hotbar items even when ordinary equipment drops; no duplicate drops.
        e.getDrops().removeIf(this::serving); e.getItemsToKeep().removeIf(this::serving);
        if (!e.getKeepInventory()) {
            for (var s : real) if (s != null && !s.isEmpty() && (plugin.lockedSlots() == null || !plugin.lockedSlots().isMarker(s))) e.getItemsToKeep().add(s);
        }
        // Also restore the live inventory: a later listener may turn keepInventory on.
        custody.hold(p.getUniqueId(), real); release(p, false, true);
    }
    @EventHandler(priority = EventPriority.LOWEST) public void quit(PlayerQuitEvent e) { cancel(e.getPlayer()); }
    @EventHandler(priority = EventPriority.LOWEST) public void join(PlayerJoinEvent e) { release(e.getPlayer(), false, true); }
    @EventHandler(priority = EventPriority.LOWEST) public void teleport(PlayerTeleportEvent e) { if (seated(e.getPlayer())) release(e.getPlayer(), false, true); }
    @EventHandler(ignoreCancelled = true) public void dismount(EntityDismountEvent e) {
        if (e.getEntity() instanceof Player p && seated(p)) release(p, true, true);
    }
    @EventHandler public void jump(PlayerInputEvent e) { if (seated(e.getPlayer()) && e.getInput().isJump()) e.getPlayer().leaveVehicle(); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void click(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p && seated(p)) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void drag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player p && seated(p)) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void drop(PlayerDropItemEvent e) { if (seated(e.getPlayer()) || serving(e.getItemDrop().getItemStack())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void swap(PlayerSwapHandItemsEvent e) { if (seated(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void pickup(EntityPickupItemEvent e) { if (e.getEntity() instanceof Player p && seated(p)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void attack(EntityDamageByEntityEvent e) { if (e.getDamager() instanceof Player p && seated(p)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void use(PlayerInteractEvent e) {
        if (seated(e.getPlayer()) && (!e.getAction().isRightClick() || e.getHand() != org.bukkit.inventory.EquipmentSlot.HAND
                || !serving(e.getItem()))) e.setCancelled(true);
        else if (seated(e.getPlayer())) e.setUseInteractedBlock(Event.Result.DENY);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void useEntity(PlayerInteractEntityEvent e) { if (seated(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void consume(PlayerItemConsumeEvent e) {
        if (!seated(e.getPlayer())) return;
        if (!serving(e.getItem())) { e.setCancelled(true); return; }
        // No bowl/bottle is exported from a temporary serving.
        e.setReplacement(new ItemStack(Material.AIR));
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void eaten(PlayerItemConsumeEvent e) {
        var s = seats.get(e.getPlayer().getUniqueId());
        if (s != null && serving(e.getItem())) s.eaten()[0]++;
    }
}
