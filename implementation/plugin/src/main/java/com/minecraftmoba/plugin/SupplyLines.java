package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.DoubleChestInventory;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import java.util.Comparator;

/** Shared, lab-only single-link recognition. The carrier sends completed deliveries, never a line definition. */
final class SupplyLines implements Listener {
    record Line(String id,UUID owner,Location source,Location destination,UUID method,int ratedSlots,long transitTicks,long intervalTicks) {}
    private static final class Pending {
        final Location source;final long started;final Map<String,TransportProof> samples=new HashMap<>();
        final Map<String,TransportProof.Proof> proved=new HashMap<>();
        Pending(Location source,long started){this.source=source;this.started=started;}
    }
    private final MobaPlugin plugin;private final Path file;
    private final Map<UUID,Pending> pending=new HashMap<>();private final List<Line> lines=new ArrayList<>();
    private final List<Map<?,?>> unresolved = new ArrayList<>();
    private final Map<String,Long> nextPulse=new HashMap<>();
    SupplyLines(MobaPlugin plugin){this.plugin=plugin;file=plugin.getDataFolder().toPath().resolve("supply-lines.yml");load();plugin.transportEvidence().observe(this::delivery);}
    static boolean anchor(Block b){return b!=null && b.getType().name().endsWith("COPPER_CHEST") && b.getState() instanceof Chest;}
    static Inventory inventory(Location at){
        if(at==null || Bukkit.getWorld(at.getWorld().getUID())!=at.getWorld() || !at.getWorld().isChunkLoaded(at.getBlockX()>>4,at.getBlockZ()>>4))return null;
        Block b=at.getBlock();return anchor(b)?((Chest)b.getState()).getInventory():null;
    }
    // Compound double-chest wrappers can be recreated on each lookup. Compare
    // their physical halves, rather than relying on wrapper identity.
    static boolean overlap(Inventory a,Inventory b){
        if(a==null||b==null)return false;
        if(a.equals(b))return true;
        if(a instanceof DoubleChestInventory d)return overlap(d.getLeftSide(),b)||overlap(d.getRightSide(),b);
        if(b instanceof DoubleChestInventory d)return overlap(a,d.getLeftSide())||overlap(a,d.getRightSide());
        return false;
    }
    boolean eligible(Player p){
        if(!plugin.enrolled(p))return false;var d=plugin.data(p);var cls=plugin.inputs().definition(d.classId);if(cls==null || "MONSTER_COMBAT".equals(d.contribution))return false;
        return cls.growth().values().stream().filter(packet->packet.level()<=d.level).flatMap(packet->packet.effects().stream())
                .anyMatch(effect->effect.family()==GrowthPacket.Family.INFRASTRUCTURE && effect.effect().equals("supply_line"));
    }
    private boolean lab(World world){return plugin.getConfig().getStringList("features.supplyLines.prototypeWorlds").contains(world.getName());}
    boolean enabled(){return plugin.getConfig().getBoolean("features.supplyLines.enabled");}
    String designate(Player p,Block block){
        if(!enabled())return "Supply Line prototype disabled";
        if(!lab(p.getWorld()))return "Supply Line recognition is currently a lab prototype; relay/span policy remains Open";
        if(!eligible(p))return "this class has not earned Supply Line recognition";
        if(!anchor(block))return "designate a Copper Chest";
        Pending current=pending.get(p.getUniqueId());
        if(current==null){pending.put(p.getUniqueId(),new Pending(block.getLocation(),Bukkit.getCurrentTick()));return "Source designated. Assign a real carrier and demonstrate two full deliveries, then designate Destination.";}
        if(current.source.getBlock().equals(block) || overlap(inventory(current.source),inventory(block.getLocation())))return "designate a different Destination";
        if(current.source.getWorld()!=block.getWorld() || inventory(current.source)==null)return "Source is no longer valid";
        var proof=current.proved.get(key(block.getLocation()));if(proof==null)return "no repeatable, full-rated-capacity carrier delivery demonstrated to this Destination";
        Line line=new Line(UUID.randomUUID().toString(),p.getUniqueId(),current.source,block.getLocation(),proof.method(),proof.slots(),proof.transitTicks(),proof.intervalTicks());
        lines.add(line);pending.remove(p.getUniqueId());save();return "Supply Line "+shortId(line.id())+" recognized. Commit workers separately; Capacity starts at 0.";
    }
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void interact(PlayerInteractEvent e){
        if(e.getHand()!=org.bukkit.inventory.EquipmentSlot.HAND || e.getAction()!=Action.RIGHT_CLICK_BLOCK || !anchor(e.getClickedBlock()) || plugin.infraMode()==null || !plugin.infraMode().isActive(e.getPlayer()))return;
        e.setCancelled(true);e.getPlayer().sendMessage(designate(e.getPlayer(),e.getClickedBlock()));
    }
    private void delivery(TransportEvidence.Delivery delivery){
        Pending p=pending.get(delivery.owner());if(p==null || delivery.source()==null || !key(p.source).equals(key(delivery.source())) || delivery.departed()<p.started)return;
        if(inventory(delivery.source())==null || inventory(delivery.destination())==null || overlap(inventory(delivery.source()),inventory(delivery.destination())))return;
        String key=key(delivery.destination());
        var proof=p.samples.computeIfAbsent(key,k->new TransportProof()).observe(new TransportProof.Delivery(delivery.method(),delivery.ratedSlots(),delivery.deliveredSlots(),delivery.departed(),delivery.arrived(),delivery.temporary()));
        if(proof!=null)p.proved.put(key,proof);
    }
    void discardPending(Player p){pending.remove(p.getUniqueId());}
    Line find(UUID owner,String prefix){return lines.stream().filter(l->l.owner().equals(owner)&&l.id().startsWith(prefix)).findFirst().orElse(null);}
    Line find(String id){return lines.stream().filter(l->l.id().equals(id)).findFirst().orElse(null);}
    Location nearestNode(UUID owner,Location from){
        return lines.stream().filter(l->l.owner().equals(owner)).flatMap(l->java.util.stream.Stream.of(l.source(),l.destination()))
                .filter(at->at.getWorld()==from.getWorld()&&inventory(at)!=null).min(Comparator.comparingDouble(at->at.distanceSquared(from))).map(Location::clone).orElse(null);
    }
    int count(){return lines.size();}
    List<Line> lines(){return List.copyOf(lines);}
    void tick(){
        if(!enabled() || plugin.inputs()==null)return;resolve();long now=Bukkit.getCurrentTick();
        pending.entrySet().removeIf(e->now-e.getValue().started>plugin.getConfig().getLong("features.supplyLines.proofTimeoutTicks"));
        for(var line:lines){
            int capacity=plugin.inputs().crew().capacity(line.id());if(capacity==0 || !plugin.inputs().crew().supports(line) || now<nextPulse.getOrDefault(line.id(),0L))continue;
            Inventory source=inventory(line.source()),destination=inventory(line.destination());if(source==null || destination==null || overlap(source,destination))continue;
            // No transient buff rewrites the measured Item Rate. Capacity alone comes from the allocation ledger.
            List<org.bukkit.inventory.ItemStack> cargo=new ArrayList<>();CargoSlots.collect(source,cargo,capacity);CargoSlots.deposit(destination,cargo);
            // A blocked destination leaves the unaccepted real cargo upstream; no dropped overflow or item deletion.
            for(var stack:cargo)if(!source.addItem(stack).isEmpty())throw new IllegalStateException("Supply Line source could not reclaim its own cargo");
            nextPulse.put(line.id(),now+line.intervalTicks());
        }
    }
    void removeOwner(UUID owner){unresolved.removeIf(row->owner.toString().equals(row.get("owner")));lines.removeIf(l->l.owner().equals(owner));pending.remove(owner);save();}
    void reset(){unresolved.clear();lines.clear();pending.clear();nextPulse.clear();save();}
    void report(Player p){for(var l:lines)if(l.owner().equals(p.getUniqueId()))p.sendMessage("LINE "+shortId(l.id())+" slots="+plugin.inputs().crew().capacity(l.id())+" transit="+l.transitTicks()+" pulse="+l.intervalTicks()+" source="+key(l.source())+" destination="+key(l.destination()));}
    static String shortId(String id){return id.substring(0,8);}
    static String key(Location l){return l.getWorld().getUID()+":"+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ();}
    static Map<String,Object> position(Location l){return Map.of("world",l.getWorld().getUID().toString(),"x",l.getX(),"y",l.getY(),"z",l.getZ());}
    static Location position(Map<?,?> value){var world=Bukkit.getWorld(UUID.fromString(value.get("world").toString()));return world==null?null:new Location(world,((Number)value.get("x")).doubleValue(),((Number)value.get("y")).doubleValue(),((Number)value.get("z")).doubleValue());}
    private void save(){
        var y=new YamlConfiguration();List<Map<?,?>> rows=new ArrayList<>(unresolved);for(var l:lines)rows.add(Map.of("id",l.id(),"owner",l.owner().toString(),"source",position(l.source()),"destination",position(l.destination()),"method",l.method().toString(),"slots",l.ratedSlots(),"transit",l.transitTicks(),"interval",l.intervalTicks()));y.set("version",1);y.set("lines",rows);write(file,y);
    }
    static void write(Path file,YamlConfiguration y){try{Files.createDirectories(file.getParent());Path temp=file.resolveSibling(file.getFileName()+".tmp");Files.writeString(temp,y.saveToString());Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(IOException ex){throw new IllegalStateException("Cannot save logistical state",ex);}}
    private void load(){
        if(!Files.exists(file))return;YamlConfiguration y=YamlConfiguration.loadConfiguration(file.toFile());
        if(y.getInt("version")!=1)throw new IllegalStateException("Unsupported Supply Line state version");
        unresolved.addAll(y.getMapList("lines"));resolve();
    }
    private void resolve(){for(var row:List.copyOf(unresolved)){
        Location source=position((Map<?,?>)row.get("source")),destination=position((Map<?,?>)row.get("destination"));if(source==null || destination==null)continue;
        lines.add(new Line(row.get("id").toString(),UUID.fromString(row.get("owner").toString()),source,destination,UUID.fromString(row.get("method").toString()),((Number)row.get("slots")).intValue(),((Number)row.get("transit")).longValue(),Math.max(1,((Number)row.get("interval")).longValue())));unresolved.remove(row);
    }}
}
