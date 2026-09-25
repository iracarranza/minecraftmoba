package com.minecraftmoba.plugin;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;

/** Six numbered portal choices; strikes are immediate, picks require team consensus. */
final class ColosseumMaps implements Listener {
    private final MobaPlugin plugin;
    private MapDraft draft;
    private List<MapDraft.Option> options = List.of();
    private final Map<UUID, Integer> votes = new HashMap<>();
    private World world;
    private boolean resolved;
    @EventHandler public void portal(PlayerPortalEvent e) { if (e.getFrom().getWorld() == world) e.setCancelled(true); }
    ColosseumMaps(MobaPlugin plugin) { this.plugin = plugin; Bukkit.getPluginManager().registerEvents(this, plugin); }
    void prepare(Team first) {
        var entries = new ArrayList<>(plugin.mapPool().entries().stream().filter(e -> MapPool.READY.equals(e.state())).toList());
        Collections.shuffle(entries);
        List<MapDraft.Option> board = new ArrayList<>();
        for (var e : entries) {
            try {
                JsonObject root = JsonParser.parseString(Files.readString(e.directory().resolve("map.json"))).getAsJsonObject();
                JsonObject c = root.getAsJsonObject("characteristics");
                board.add(new MapDraft.Option(e.mapId(), field(c,"map_type"), field(c,"map_scale"), field(c,"resource_density"), field(c,"terrain_symmetry"), ""));
            } catch (Exception ignored) { continue; }
            if (board.size() == 6) break;
        }
        if (board.size() != 6) throw new IllegalStateException("Six READY maps are required for the portal board.");
        options = List.copyOf(board); draft = new MapDraft(MapDraft.Rules.sixBoard(), options, first);
        resolved = false; votes.clear();
    }
    private static String field(JsonObject c, String key) { return c != null && c.has(key) ? c.get(key).getAsString() : "unmeasured"; }
    Set<String> types() { return draft == null ? Set.of() : draft.revealedTypes(); }
    void show(World w) {
        world = w;
        for (int i = 0; i < 6; i++) {
            int x = (i%3-1)*10, z = i<3 ? -8 : 8;
            for (int dx = -2; dx <= 2; dx++) for (int dy = 0; dy <= 5; dy++) {
                boolean edge = Math.abs(dx)==2 || dy==0 || dy==5;
                w.getBlockAt(x+dx,65+dy,z).setType(edge ? Material.OBSIDIAN : Material.NETHER_PORTAL, false);
            }
            var o = options.get(i);
            TextDisplay t = w.spawn(new Location(w,x,72,z), TextDisplay.class);
            t.setBillboard(Display.Billboard.CENTER);
            t.setText((i+1)+" — "+o.type()+"\nScale: "+o.scale()+"\nDensity: "+o.density()+"\nSymmetry: "+o.symmetryBand());
        }
        announce();
    }
    private void announce() {
        for (UUID id : plugin.match().participantsByTeam().keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.sendMessage("MAP " + draft.phase() + " — " + draft.onTurn() + ": walk into a numbered portal. Final pick requires every teammate to choose the same portal.");
        }
    }
    @EventHandler public void move(PlayerMoveEvent e) {
        if (world == null || resolved || !plugin.match().preMatch() || e.getTo().getWorld()!=world) return;
        var participant = plugin.match().participant(e.getPlayer().getUniqueId());
        if (participant == null) return;
        for (int i=0;i<6;i++) {
            int x=(i%3-1)*10,z=i<3?-8:8;
            if (Math.abs(e.getTo().getX()-x) > 1.8 || Math.abs(e.getTo().getZ()-z) > .75 || e.getTo().getY()>70) continue;
            e.setTo(new Location(world,.5,65,.5));
            String id=options.get(i).id();
            if (draft.board().stream().noneMatch(o -> o.id().equals(id))) return;
            Team actor=participant.team;
            // Solo testing can exercise the absent side without changing the normal draft order.
            boolean absent=plugin.match().participantsByTeam().values().stream().noneMatch(t -> t==draft.onTurn());
            if (actor!=draft.onTurn() && !absent) { e.getPlayer().sendMessage("Wait for your team's turn."); return; }
            if (draft.phase()==MapDraft.Phase.STRIKE) {
                draft.strike(draft.onTurn(),id);
                for(int dx=-1;dx<=1;dx++) for(int dy=1;dy<5;dy++) world.getBlockAt(x+dx,65+dy,z).setType(Material.AIR,false);
                announce(); return;
            }
            votes.put(e.getPlayer().getUniqueId(),i);
            int option=i;
            List<UUID> team=plugin.match().participantsByTeam().entrySet().stream().filter(p -> p.getValue()==actor).map(Map.Entry::getKey).toList();
            long agree=team.stream().filter(p -> Objects.equals(votes.get(p),option)).count();
            e.getPlayer().sendMessage("Map "+(i+1)+": "+agree+"/"+team.size()+" agree");
            if (agree!=team.size()) return;
            draft.pick(draft.onTurn(),id); resolved=true;
            Bukkit.getScheduler().runTask(plugin, () -> {
                try { plugin.match().resolveSelectionForTest(id); plugin.match().start(); }
                catch(Exception failure) { e.getPlayer().sendMessage("Selected map could not start: "+failure.getMessage()); }
            });
            return;
        }
    }
}
