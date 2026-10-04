package com.minecraftmoba.plugin;

import com.google.gson.JsonParser;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Disposable geometry experiments. No production manifests or match bindings are edited. */
public final class LabAuthoring implements org.bukkit.event.Listener {
    private final MobaPlugin plugin;
    private final Lab lab;
    private final LabUndo<BlockState> undo = new LabUndo<>();
    private final Map<String,List<LabGeometry.TemplateBlock>> templates = new HashMap<>();
    private final Set<LabGeometry.Pos> authoredBlocks = new HashSet<>();
    private record Request(String kind, LabGeometry.XZ from, LabGeometry.XZ to, boolean follow) {}
    private Request request;
    private LabGeometry.Plan preview;
    private final Map<LabGeometry.Pos,BlockData> previewBefore = new LinkedHashMap<>();
    private UUID previewPlayer;
    private LabGeometry.XZ pathStart;
    private long scanGeneration;
    private boolean editing;

    LabAuthoring(MobaPlugin plugin, Lab lab) { this.plugin = plugin; this.lab = lab; }

    public void command(Player p, String[] args) throws IOException {
        lab.requireAuthor(p);
        if (!editing) {
            editing = true;
            p.getWorld().setGameRule(GameRule.RANDOM_TICK_SPEED,0);
            p.getWorld().setGameRule(GameRule.MOB_GRIEFING,false);
            p.getWorld().setGameRule(GameRule.DO_FIRE_TICK,false);
            p.sendMessage("Authoring mode: Creative; match clock paused, random ticks/griefing/fluid flow held for reversible experiments. End the lab to return to setup.");
        }
        String verb = args.length > 2 ? args[2].toLowerCase(Locale.ROOT) : "help";
        switch (verb) {
            case "help" -> {
                p.sendMessage("/moba lab author preview <fountain|outpost|bastion|spike> [rigid|follow]");
                p.sendMessage("/moba lab author path from | to (target both endpoints) | inspect | access | apply | undo | cancel");
                p.sendMessage("/moba lab author swarm <mountain_ravager> | fauna <kind> [count]");
                p.sendMessage("Prototype geometry only. Match bindings stay at their certified sites. Preview may be rejected; apply cannot bypass rejection.");
            }
            case "preview" -> {
                if (args.length < 4 || args.length > 5) throw new IllegalArgumentException("Specify a template and optional rigid/follow policy.");
                String mode = args.length == 5 ? args[4] : "rigid";
                if (!Set.of("rigid","follow").contains(mode)) throw new IllegalArgumentException("Policy must be rigid or follow.");
                show(p,new Request(args[3],target(p),null,mode.equals("follow")));
            }
            case "path" -> {
                if (args.length != 4) throw new IllegalArgumentException("Target each endpoint: author path from, then author path to.");
                if (args[3].equals("from")) { pathStart = target(p); p.sendMessage("Path start: " + pathStart + ". Target endpoint and run author path to."); }
                else if (args[3].equals("to")) {
                    if (pathStart == null) throw new IllegalStateException("Set author path from first.");
                    show(p,new Request("path",pathStart,target(p),false));
                } else throw new IllegalArgumentException("Path action must be from or to.");
            }
            case "apply" -> apply(p);
            case "undo" -> {
                cancel(p); scanGeneration++;
                p.sendMessage("Undid " + undo.undo(state -> {
                    if (!state.update(true,false)) throw new IllegalStateException("Block restore failed; undo retained for retry.");
                }, id -> { var e = Bukkit.getEntity(id); if (e != null) e.remove(); }) + ". History: " + undo.depth());
            }
            case "cancel" -> { cancel(p); scanGeneration++; p.sendMessage("Preview cancelled; no blocks placed."); }
            case "access" -> {
                var at = request == null ? target(p) : request.to == null ? request.from : request.to;
                access(p,at);
            }
            case "inspect" -> {
                var at=target(p); var terrain=new Terrain(p.getWorld());
                int lo=Integer.MAX_VALUE,hi=Integer.MIN_VALUE,wet=0;
                for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++) {
                    int y=terrain.height(at.x()+dx,at.z()+dz);lo=Math.min(lo,y);hi=Math.max(hi,y);
                    if(terrain.water(at.x()+dx,at.z()+dz))wet++;
                }
                p.sendMessage("Target "+at+": 9x9-block neighborhood relief="+(hi-lo)+"; water columns="+wet+"/81. Choose a template to evaluate its actual footprint.");
                access(p,at);
            }
            case "swarm", "fauna" -> spawn(p,args,verb.equals("swarm"));
            default -> throw new IllegalArgumentException("Unknown author action. /moba lab author help");
        }
    }

    private LabGeometry.XZ target(Player p) {
        var block = p.getTargetBlockExact(64);
        if (block == null) throw new IllegalArgumentException("Aim at a block within 64 blocks; no position fallback.");
        return new LabGeometry.XZ(block.getX(),block.getZ());
    }

    private List<LabGeometry.TemplateBlock> template(String name) throws IOException {
        if (!Set.of("fountain","outpost","bastion","spike").contains(name))
            throw new IllegalArgumentException("Unknown lab template: " + name);
        if (templates.containsKey(name)) return templates.get(name);
        try (var resource = plugin.getResource("lab-authoring/"+name+".json.gz")) {
            if (resource == null) throw new IOException("Missing template resource: " + name);
            try (var stream = new GZIPInputStream(resource)) {
                var doc = JsonParser.parseString(new String(stream.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();
                var result = new ArrayList<LabGeometry.TemplateBlock>();
                for (var row : doc.getAsJsonArray("blocks")) {
                    var a = row.getAsJsonArray(); result.add(new LabGeometry.TemplateBlock(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt(),a.get(3).getAsString()));
                }
                templates.put(name,List.copyOf(result)); return templates.get(name);
            }
        }
    }

    private final class Terrain implements LabGeometry.Terrain {
        final World world;
        final Map<LabGeometry.XZ,Integer> heights = new HashMap<>();
        final Set<Long> knownChunks = new HashSet<>();
        Terrain(World world) { this.world = world; }
        private void known(int x,int z) {
            long chunk=((long)(x>>4)<<32)^((z>>4)&0xffffffffL);
            if(knownChunks.contains(chunk)) return;
            if (!world.isChunkGenerated(x>>4,z>>4)) throw new IllegalArgumentException("Outside generated scoop terrain; no new chunks will be generated.");
            if (!world.isChunkLoaded(x>>4,z>>4) && !world.loadChunk(x>>4,z>>4,false))
                throw new IllegalArgumentException("Could not load existing terrain.");
            knownChunks.add(chunk);
        }
        public int height(int x,int z) {
            known(x,z);
            return heights.computeIfAbsent(new LabGeometry.XZ(x,z), k -> world.getHighestBlockYAt(x,z,HeightMap.MOTION_BLOCKING_NO_LEAVES));
        }
        public boolean water(int x,int z) {
            return world.getBlockAt(x,height(x,z),z).isLiquid();
        }
        public String block(LabGeometry.Pos pos) {
            known(pos.x(),pos.z());
            if (pos.y() < world.getMinHeight() || pos.y() >= world.getMaxHeight())
                throw new IllegalArgumentException("Placement exceeds world height.");
            return world.getBlockAt(pos.x(),pos.y(),pos.z()).getBlockData().getAsString();
        }
        public boolean protectedAt(LabGeometry.Pos pos) {
            block(pos);
            var b = world.getBlockAt(pos.x(),pos.y(),pos.z());
            return b.getState() instanceof TileState || plugin.provenance().isPlayerPlaced(b)
                    || (!authoredBlocks.contains(pos) && !b.getType().isAir() && !b.isLiquid() && !natural(b.getType()));
        }
    }
    static boolean natural(Material material) {
        String n = material.name();
        return Set.of("STONE","DIRT","GRASS_BLOCK","COARSE_DIRT","ROOTED_DIRT","PODZOL","MYCELIUM","MOSS_BLOCK",
                "GRAVEL","SAND","RED_SAND","SANDSTONE","RED_SANDSTONE","DEEPSLATE","TUFF","GRANITE","DIORITE","ANDESITE",
                "SNOW","SNOW_BLOCK","ICE","PACKED_ICE","BLUE_ICE","CLAY","CALCITE","BEDROCK").contains(n)
                || n.endsWith("_LEAVES") || n.endsWith("_LOG") || n.endsWith("_WOOD") || n.endsWith("_ORE")
                || Tag.FLOWERS.isTagged(material) || n.endsWith("_TERRACOTTA")
                || Set.of("TERRACOTTA","SHORT_GRASS","TALL_GRASS","FERN","LARGE_FERN","VINE","DEAD_BUSH").contains(n);
    }

    private LabGeometry.Plan plan(Player p,Request r) throws IOException {
        var terrain = new Terrain(p.getWorld());
        return r.kind.equals("path") ? LabGeometry.path(terrain,r.from,r.to)
                : LabGeometry.structure(terrain,r.from,template(r.kind),r.follow);
    }
    private void show(Player p,Request r) throws IOException {
        cancel(p); scanGeneration++;
        request = r; preview = plan(p,r); previewPlayer = p.getUniqueId();
        int shown = 0;
        for (var edit : preview.edits().entrySet()) {
            if (shown++ >= 12000) break;
            var pos = edit.getKey(); var at = new Location(p.getWorld(),pos.x(),pos.y(),pos.z());
            previewBefore.put(pos,p.getWorld().getBlockAt(at).getBlockData());
            p.sendBlockChange(at,Bukkit.createBlockData(edit.getValue()));
        }
        report(p,preview);
        // Cyan marks the original surface under the proposed geometry. Preview packets never mutate it.
        var marked=new HashSet<LabGeometry.XZ>();
        for (var pos:preview.edits().keySet()) {
            if(marked.size()>=256) break;
            var column=new LabGeometry.XZ(pos.x(),pos.z());
            if(marked.add(column)) {
                int y=p.getWorld().getHighestBlockYAt(pos.x(),pos.z(),HeightMap.MOTION_BLOCKING_NO_LEAVES);
                p.spawnParticle(Particle.DUST,pos.x()+.5,y+1.1,pos.z()+.5,1,0,0,0,0,new Particle.DustOptions(Color.AQUA,1));
            }
        }
        p.sendMessage("Client-only preview: " + previewBefore.size() + "/" + preview.edits().size()
                + " blocks shown. apply commits a passing plan; cancel restores your view.");
        p.sendMessage("Prototype placement experiment; no live respawn/objective bindings changed. Follow policy is diagnostic for rigid forms.");
        access(p,r.to == null ? r.from : r.to);
    }
    private static void report(Player p,LabGeometry.Plan plan) {
        p.sendMessage("Physical placement: " + (plan.accepted() ? "PASS" : "REJECT") + "; blocks="+plan.edits().size()
                + "; relief="+plan.relief()+"; cut="+plan.cut()+" fill="+plan.fill()
                + "; max cut/fill="+plan.maxCut()+"/"+plan.maxFill());
        if (!plan.centreline().isEmpty() && plan.centreline().size()>1)
            p.sendMessage("Path: length="+plan.centreline().size()+"; modules="+plan.modules()+"; water deck columns="+plan.bridges());
        for (String problem : plan.problems()) p.sendMessage("Rejected: " + problem);
    }
    private void apply(Player p) throws IOException {
        if (request == null || preview == null) throw new IllegalStateException("Preview a placement first.");
        var fresh = plan(p,request); report(p,fresh);
        if (!fresh.accepted()) throw new IllegalStateException("Placement rejected; preview remains inspectable.");
        if (!fresh.edits().equals(preview.edits())) throw new IllegalStateException("Terrain changed since preview; create a new preview.");
        var states = new ArrayList<BlockState>();
        // Capture every state before any write; restoration disables physics, preserving block data.
        for (var pos : fresh.edits().keySet()) states.add(p.getWorld().getBlockAt(pos.x(),pos.y(),pos.z()).getState());
        try {
            for (var edit : fresh.edits().entrySet()) {
                var pos = edit.getKey(); p.getWorld().getBlockAt(pos.x(),pos.y(),pos.z()).setBlockData(Bukkit.createBlockData(edit.getValue()),false);
            }
        } catch (RuntimeException failure) {
            for (int i = states.size()-1; i >= 0; i--) states.get(i).update(true,false);
            throw failure;
        }
        String description = request.kind + " at " + request.from;
        undo.push(description,states,List.of());
        authoredBlocks.addAll(fresh.edits().keySet());
        cancel(p);
        p.sendMessage("Applied " + description + ". /moba lab author undo restores the preceding blocks. History: " + undo.depth());
        access(p,fresh.centreline().getLast());
    }
    private void cancel(Player p) {
        Player recipient = previewPlayer == null ? p : Bukkit.getPlayer(previewPlayer);
        if (recipient != null && plugin.worldInstance().world() != null && recipient.getWorld().equals(plugin.worldInstance().world()))
            for (var pos : previewBefore.keySet()) {
                var at = new Location(recipient.getWorld(),pos.x(),pos.y(),pos.z());
                recipient.sendBlockChange(at,recipient.getWorld().getBlockAt(at).getBlockData());
            }
        previewBefore.clear(); previewPlayer = null; preview = null; request = null;
    }

    private void spawn(Player p,String[] args,boolean swarm) {
        if (args.length < 4 || args.length > (swarm ? 4 : 5)) throw new IllegalArgumentException("Specify a registered swarm or animal kind and optional animal count.");
        cancel(p);
        var at = target(p); var world = p.getWorld();
        var terrain = new WorldTerrain(world,plugin.provenance());
        var rules = new Eligibility.Rules(3,Eligibility.NATURAL_GROUND,1,0,0,true);
        var loci = Eligibility.loci(OpportunityRegion.square(at.x(),at.z(),3),terrain,rules);
        var target = loci.stream().min(Comparator.comparingDouble(l -> Math.hypot(l.x()-at.x(),l.z()-at.z())))
                .orElseThrow(() -> new IllegalArgumentException("No eligible natural ground/headroom within 3 blocks of target."));
        List<org.bukkit.entity.EntityType> types;
        if (swarm) {
            var definition = SwarmDefinitions.require(args[3]);
            if (!definition.eligibleAt(terrain.biomeAt(target.x(),target.z()),WorldTerrain.isNight(world)))
                throw new IllegalArgumentException("Swarm rejected: biome or day/night eligibility fails.");
            types = definition.compose(new Random());
        } else {
            var kind = RenewableKinds.require(args[3]);
            if (kind.type() != Renewables.Type.ANIMAL) throw new IllegalArgumentException("fauna accepts animal kinds only; use author swarm for encounters.");
            int count = args.length == 5 ? Integer.parseInt(args[4]) : 3;
            if (count < 1 || count > 12) throw new IllegalArgumentException("Animal count must be 1..12.");
            types = Collections.nCopies(count,kind.entities().iterator().next());
        }
        var made = new ArrayList<UUID>();
        try {
            for (var type : types) {
                var entity = world.spawnEntity(new Location(world,target.x()+.5,target.y(),target.z()+.5),type);
                made.add(entity.getUniqueId());
                entity.setPersistent(false);
                entity.addScoreboardTag("moba_lab_authoring");
            }
        } catch (RuntimeException ex) { for (UUID id : made) { var e = Bukkit.getEntity(id); if (e != null) e.remove(); } throw ex; }
        undo.push((swarm ? "swarm " : "fauna ")+args[3],List.of(),made);
        p.sendMessage("Spawned "+made.size()+" at eligible locus "+target+". One-off encounter fixture; undo removes surviving spawned members.");
    }

    private void access(Player p,LabGeometry.XZ target) {
        long generation = ++scanGeneration;
        var world = p.getWorld();
        var homes = new EnumMap<Team,Location>(Team.class);
        for (var team : Team.values()) { var home = plugin.match().homeland(team); if (home != null) homes.put(team,home.clone()); }
        if (homes.size()!=2) { p.sendMessage("Access UNMEASURED: both fountain anchors are required."); return; }
        int minX = target.x(), maxX = target.x(), minZ = target.z(), maxZ = target.z();
        for (var home : homes.values()) { minX=Math.min(minX,home.getBlockX()); maxX=Math.max(maxX,home.getBlockX()); minZ=Math.min(minZ,home.getBlockZ()); maxZ=Math.max(maxZ,home.getBlockZ()); }
        minX = Math.floorDiv(minX-64,8)*8; minZ = Math.floorDiv(minZ-64,8)*8;
        int width = Math.floorDiv(maxX+64-minX,8)+1, depth = Math.floorDiv(maxZ+64-minZ,8)+1;
        if ((long)width*depth>40000) { p.sendMessage("Access UNMEASURED: regional scan exceeds 40000 samples."); return; }
        final int originX=minX, originZ=minZ;
        int[] heights=new int[width*depth]; Arrays.fill(heights,Integer.MIN_VALUE);
        boolean[] wet=new boolean[heights.length];
        p.sendMessage("Scanning team access in existing terrain (8-block prototype grid); local PASS is not balance certification.");
        new org.bukkit.scheduler.BukkitRunnable() {
            int cursor;
            public void run() {
                if (generation!=scanGeneration || !world.equals(plugin.worldInstance().world()) || !p.isOnline()) { cancel(); return; }
                try {
                    // Bound disk/chunk work per tick. Never generate terrain beyond the scoop.
                    for (int batch=0;batch<24 && cursor<heights.length;batch++,cursor++) {
                        int x=originX+(cursor%width)*8,z=originZ+(cursor/width)*8;
                        if (!world.isChunkGenerated(x>>4,z>>4) || !world.loadChunk(x>>4,z>>4,false)) continue;
                        int y=world.getHighestBlockYAt(x,z,HeightMap.MOTION_BLOCKING_NO_LEAVES);
                        var block=world.getBlockAt(x,y,z);
                        if (!world.getBlockAt(x,y+1,z).isPassable() || !world.getBlockAt(x,y+2,z).isPassable()) continue;
                        heights[cursor]=y; wet[cursor]=block.isLiquid();
                    }
                    if (cursor<heights.length) return;
                    cancel();
                    int goal=grid(target.x(),target.z(),originX,originZ,width,depth);
                    var costs=new EnumMap<Team,Double>(Team.class);
                    for (var team:Team.values()) {
                        var home=homes.get(team); int start=grid(home.getBlockX(),home.getBlockZ(),originX,originZ,width,depth);
                        costs.put(team,LabAccess.cost(heights,wet,width,start,goal));
                    }
                    double north=costs.get(Team.NORTH),south=costs.get(Team.SOUTH);
                    if (!Double.isFinite(north)||!Double.isFinite(south)) {
                        p.sendMessage("Access UNMEASURED/UNREACHED: sampled corridor blocked or outside scoop. north="+north+" south="+south+"; no symmetry verdict."); return;
                    }
                    p.sendMessage(String.format(Locale.ROOT,"Access proxy: north=%.1f south=%.1f; asymmetry=%.3f. Descriptive, not a universal rejection threshold.",north,south,LabAccess.asymmetry(north,south)));
                    p.sendMessage("Grid excludes unseen/blocked samples; shallow water is penalized. Not full player traversal proof. Rerun author access after edits/undo.");
                } catch (RuntimeException ex) { cancel(); p.sendMessage("Access UNMEASURED: "+ex.getMessage()); }
            }
        }.runTaskTimer(plugin,1,1);
    }
    static int grid(int x,int z,int ox,int oz,int width,int depth) {
        int xx=Math.round((x-ox)/8f),zz=Math.round((z-oz)/8f);
        return xx<0||xx>=width||zz<0||zz>=depth ? -1 : zz*width+xx;
    }
    public void clear() {
        if (previewPlayer != null) { var p=Bukkit.getPlayer(previewPlayer); if(p!=null) cancel(p); }
        scanGeneration++; undo.clear(); authoredBlocks.clear(); request=null;preview=null;pathStart=null;previewBefore.clear();previewPlayer=null;editing=false;
    }
    @org.bukkit.event.EventHandler(ignoreCancelled=true)
    public void flow(org.bukkit.event.block.BlockFromToEvent event) {
        if(editing && event.getBlock().getWorld().equals(plugin.worldInstance().world())) event.setCancelled(true);
    }
    @org.bukkit.event.EventHandler(ignoreCancelled=true)
    public void entityChange(org.bukkit.event.entity.EntityChangeBlockEvent event) {
        if(editing && event.getBlock().getWorld().equals(plugin.worldInstance().world())) event.setCancelled(true);
    }
    @org.bukkit.event.EventHandler(ignoreCancelled=true)
    public void explode(org.bukkit.event.entity.EntityExplodeEvent event) {
        if(editing && event.getLocation().getWorld().equals(plugin.worldInstance().world())) event.setCancelled(true);
    }
    @org.bukkit.event.EventHandler(ignoreCancelled=true)
    public void physics(org.bukkit.event.block.BlockPhysicsEvent event) {
        if(editing && event.getBlock().getWorld().equals(plugin.worldInstance().world())) event.setCancelled(true);
    }
    @org.bukkit.event.EventHandler(ignoreCancelled=true)
    public void blockExplosion(org.bukkit.event.block.BlockExplodeEvent event) {
        if(editing && event.getBlock().getWorld().equals(plugin.worldInstance().world())) event.setCancelled(true);
    }
    @org.bukkit.event.EventHandler
    public void death(org.bukkit.event.entity.EntityDeathEvent event) {
        if(event.getEntity().getScoreboardTags().contains("moba_lab_authoring")) { event.getDrops().clear(); event.setDroppedExp(0); }
    }
    @org.bukkit.event.EventHandler
    public void quit(org.bukkit.event.player.PlayerQuitEvent event) {
        if(event.getPlayer().getUniqueId().equals(previewPlayer)) cancel(event.getPlayer());
    }
    @org.bukkit.event.EventHandler(ignoreCancelled=true)
    public void teleport(org.bukkit.event.player.PlayerTeleportEvent event) {
        if(event.getPlayer().getUniqueId().equals(previewPlayer) && event.getTo()!=null
                && !event.getFrom().getWorld().equals(event.getTo().getWorld())) cancel(event.getPlayer());
    }
}
