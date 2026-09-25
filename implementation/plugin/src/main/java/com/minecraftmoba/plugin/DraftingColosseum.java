package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;

/** Catalogue and match arenas share geometry, but never share draft entities. */
public final class DraftingColosseum implements Listener {
    private final MobaPlugin plugin;
    private final NamespacedKey classKey, shortlistKey;
    private final Set<UUID> debug = new HashSet<>();
    private final Map<UUID, String> stations = new HashMap<>();
    private World catalogue, arena;
    private final List<Entity> centre = new ArrayList<>();
    private final Map<UUID, UUID> podiumOwners = new HashMap<>();
    private String rendered = "";
    private final PoolPreview previews;
    private final ColosseumMaps maps;
    private ClassDraft activeDraft;
    private boolean mapsShown;

    public DraftingColosseum(MobaPlugin plugin) {
        this.plugin = plugin;
        previews = new PoolPreview(plugin);
        maps = new ColosseumMaps(plugin);
        classKey = new NamespacedKey(plugin, "catalogue_class");
        shortlistKey = new NamespacedKey(plugin, "shortlist");
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, 10, 10);
    }

    public World ensure(boolean match) {
        World w = match ? arena : catalogue;
        if (w != null) return w;
        w = new WorldCreator(match ? "moba_drafting_colosseum" : "moba_class_catalogue")
                .generator(new VoidGenerator()).generateStructures(false).createWorld();
        if (w == null) throw new IllegalStateException("Could not load colosseum");
        w.setDifficulty(Difficulty.PEACEFUL);
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setTime(6000);
        for (Entity e : w.getEntities()) if (e.getPersistentDataContainer().has(classKey)) e.remove();
        for (int x = -38; x <= 38; x++) for (int z = -38; z <= 38; z++) {
            double r = Math.hypot(x, z);
            if (r > 38) continue;
            int y = 64 + Math.max(0, (int)((r - 25) / 3));
            w.getBlockAt(x, y, z).setType(r > 37 ? Material.SEA_LANTERN : Material.SMOOTH_QUARTZ, false);
            if (r > 37) for (int h = 1; h <= 4; h++) w.getBlockAt(x, y+h, z).setType(Material.GLASS, false);
        }
        var section = plugin.getConfig().getConfigurationSection("abilities.classes");
        List<String> ids = section == null ? List.of() : section.getKeys(false).stream().sorted().toList();
        for (int i = 0; i < Math.max(60, ids.size()); i++) {
            double angle = i * Math.PI * 2 / Math.max(60, ids.size());
            Location at = new Location(w, Math.cos(angle)*32, 67, Math.sin(angle)*32);
            String id = i < ids.size() ? ids.get(i) : "";
            ArmorStand stand = w.spawn(at, ArmorStand.class);
            stand.setGravity(false); stand.setInvulnerable(true); stand.setArms(true);
            stand.setCustomName(id.isEmpty() ? "Unfilled " + (i+1) : id.toUpperCase(Locale.ROOT));
            stand.setCustomNameVisible(true);
            stand.getPersistentDataContainer().set(classKey, PersistentDataType.STRING, id);
            stand.getEquipment().setHelmet(new ItemStack(id.isEmpty() ? Material.GRAY_STAINED_GLASS : Material.CARVED_PUMPKIN));
            stations.put(stand.getUniqueId(), id);
        }
        w.setSpawnLocation(0, 65, 0);
        if (match) arena = w; else catalogue = w;
        return w;
    }

    public void visit(Player p, boolean match) {
        if (match && activeDraft != plugin.match().draft()) {
            activeDraft = plugin.match().draft(); mapsShown = false; rendered = "";
            centre.forEach(Entity::remove); centre.clear(); podiumOwners.clear();
            arena = null;
            maps.prepare(activeDraft.firstPick());
        }
        p.teleport(new Location(ensure(match), .5, 65, .5));
        p.setGameMode(GameMode.ADVENTURE);
        p.sendMessage(match ? "Strike a class to ban; on your pick turn, click a class to choose." : "Click a class to add/remove it from your shortlist (maximum 5).");
        if (match) p.sendMessage("Map types on this board: " + maps.types());
    }

    private void text(World w, double x, double y, double z, String value) {
        TextDisplay t = w.spawn(new Location(w, x, y, z), TextDisplay.class);
        t.setText(value); t.setBillboard(Display.Billboard.CENTER);
        t.getPersistentDataContainer().set(classKey, PersistentDataType.STRING, "");
        centre.add(t);
    }

    private void refresh() {
        Match m = plugin.match();
        if (arena != null && m.preMatch() && activeDraft != null && activeDraft.phase() == ClassDraft.Phase.COMPLETE && !mapsShown) {
            activeDraft.picks().forEach((id, cl) -> {
                Player p = Bukkit.getPlayer(id); if (p != null) plugin.applyDraftedClass(p, cl);
            });
            mapsShown = true; centre.forEach(Entity::remove); centre.clear(); maps.show(arena);
        }
        if (arena == null || !m.selectingClasses()) return;
        ClassDraft d = m.draft();
        String state = d.phase() + ":" + d.onTurn() + d.picks() + d.personalBans();
        if (!state.equals(rendered)) {
            rendered = state;
            centre.forEach(Entity::remove); centre.clear(); podiumOwners.clear();
            for (Entity e : arena.getEntities()) {
                String cl = e.getPersistentDataContainer().get(classKey, PersistentDataType.STRING);
                if (cl != null && !cl.isEmpty() && (d.banned().contains(cl) || d.picks().containsValue(cl))) e.remove();
            }
            text(arena, 0, 70, -12, "NORTH"); text(arena, 0, 70, 12, "SOUTH");
            for (Team team : Team.values()) {
                int i = 0; double z = team == Team.NORTH ? -12 : 12;
                text(arena, -12, 66, z, "BANNED"); text(arena, 12, 66, z, "BANNED");
                for (var entry : m.participantsByTeam().entrySet()) if (entry.getValue() == team) {
                    UUID id = entry.getKey(); double x = (i++ - 3) * 3;
                    Player player = Bukkit.getPlayer(id);
                    String name = player == null ? Bukkit.getOfflinePlayer(id).getName() : player.getName();
                    ArmorStand roster = arena.spawn(new Location(arena, x, 68, z), ArmorStand.class);
                    roster.setGravity(false); roster.setInvulnerable(true); roster.setCustomNameVisible(true);
                    roster.setCustomName(name + " — " + d.picks().getOrDefault(id, "UNPICKED"));
                    centre.add(roster);
                    String ban = d.personalBans().get(id);
                    if (ban != null) {
                        ArmorStand banned = arena.spawn(new Location(arena,x,65,z),ArmorStand.class);
                        banned.setGravity(false); banned.setInvulnerable(true); banned.setCustomNameVisible(true);
                        banned.setCustomName("BANNED: " + ban); centre.add(banned);
                    }
                }
            }
            if (d.phase() == ClassDraft.Phase.PICK) {
                int slot = 0;
                for (UUID id : d.onTurn()) {
                    Player p = Bukkit.getPlayer(id); if (p == null) continue;
                    double z = (slot++ - 1) * 5;
                    text(arena, 0, 68, z, p.getName() + " — CLICK A CLASS TO PICK");
                    int index = 0;
                    for (String cl : shortlist(p)) {
                        double x = (index++ - 2)*3;
                        if (d.banned().contains(cl) || d.picks().containsValue(cl)) continue;
                        ArmorStand s = arena.spawn(new Location(arena, x, 65, z), ArmorStand.class);
                        s.setGravity(false); s.setInvulnerable(true); s.setCustomName(cl); s.setCustomNameVisible(true);
                        s.getPersistentDataContainer().set(classKey, PersistentDataType.STRING, cl);
                        podiumOwners.put(s.getUniqueId(), id); centre.add(s);
                        s.setVisibleByDefault(false);
                        for (var viewer : m.participantsByTeam().entrySet()) {
                            Player v = Bukkit.getPlayer(viewer.getKey());
                            if (v != null && viewer.getValue() == m.participantsByTeam().get(id)) v.showEntity(plugin, s);
                        }
                    }
                }
            }
        }
        // This response is sent only to the viewer; opponents never receive shortlist names.
        for (var entry : m.participantsByTeam().entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey()); if (p == null || p.getWorld() != arena) continue;
            Entity target = p.getTargetEntity(6);
            if (target == null) continue;
            String cl = target.getPersistentDataContainer().get(classKey, PersistentDataType.STRING);
            if (cl == null || cl.isEmpty()) continue;
            List<String> allies = new ArrayList<>();
            for (var ally : m.participantsByTeam().entrySet()) if (ally.getValue() == entry.getValue()) {
                Player a = Bukkit.getPlayer(ally.getKey());
                if (a != null && shortlist(a).contains(cl)) allies.add(a.getName());
            }
            p.sendActionBar(cl + " | Allied shortlists: " + (allies.isEmpty() ? "none" : String.join(", ", allies)));
        }
    }

    public List<String> shortlist(Player p) {
        String value = p.getPersistentDataContainer().getOrDefault(shortlistKey, PersistentDataType.STRING, "");
        return value.isEmpty() ? List.of() : List.of(value.split(","));
    }

    public boolean command(Player p, String[] args) {
        if (args.length == 2 && (args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off"))) {
            if (args[1].equalsIgnoreCase("on")) debug.add(p.getUniqueId()); else debug.remove(p.getUniqueId());
            p.sendMessage("Debug " + args[1]); return true;
        }
        if (!debug.contains(p.getUniqueId())) { p.sendMessage("Use /moba debug on first."); return true; }
        if (args.length == 4 && args[1].equalsIgnoreCase("go") && args[2].equalsIgnoreCase("map")) {
            previews.visit(p, args[3]); return true;
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("go") && args[2].equalsIgnoreCase("colosseum")) { visit(p, false); return true; }
        p.sendMessage("/moba debug on|off; /moba debug go colosseum"); return true;
    }

    @EventHandler public void click(PlayerInteractAtEntityEvent e) {
        if (!e.getRightClicked().getPersistentDataContainer().has(classKey)) return;
        e.setCancelled(true);
        if (e.getHand() == EquipmentSlot.HAND) interact(e.getPlayer(), e.getRightClicked(), false);
    }
    @EventHandler public void hit(EntityDamageByEntityEvent e) {
        if (!e.getEntity().getPersistentDataContainer().has(classKey)) return;
        e.setCancelled(true);
        if (e.getDamager() instanceof Player p) interact(p, e.getEntity(), true);
    }
    private void interact(Player p, Entity entity, boolean attack) {
        String id = entity.getPersistentDataContainer().get(classKey, PersistentDataType.STRING);
        if (id == null || id.isEmpty()) { p.sendMessage("This class slot is not implemented yet."); return; }
        if (entity.getWorld().equals(catalogue)) {
            if (attack) return;
            var list = new ArrayList<>(shortlist(p));
            if (!list.remove(id)) { if (list.size() >= 5) { p.sendMessage("Shortlist full: remove a class first."); return; } list.add(id); }
            p.getPersistentDataContainer().set(shortlistKey, PersistentDataType.STRING, String.join(",", list));
            p.sendMessage("Shortlist: " + list); return;
        }
        Match match = plugin.match();
        if (!match.selectingClasses()) return;
        UUID owner = podiumOwners.get(entity.getUniqueId());
        if (owner != null && !owner.equals(p.getUniqueId())) { p.sendMessage("That is another player's shortlist."); return; }
        ClassDraft d = match.draft();
        String refusal;
        if (d.phase() == ClassDraft.Phase.BAN) {
            if (!attack) { p.sendMessage("Attack this stand to ban " + id); return; }
            refusal = d.ban(p.getUniqueId(), id);
        } else refusal = d.pick(p.getUniqueId(), id);
        if (refusal != null) { p.sendMessage(refusal); return; }
        entity.remove();
        p.sendMessage("Committed: " + id);
        if (d.phase() == ClassDraft.Phase.COMPLETE) { p.sendMessage(match.classSelectionComplete()); plugin.draftHall().release(); }
        else plugin.draftHall().refresh(match);
    }
}
