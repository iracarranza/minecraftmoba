package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import java.util.*;

/** Actual mining progress, earned quota power and an interruptible dual-presence quarry domain. */
final class QuarrymanState implements Listener {
    private record Scan(Location centre,int width,int until,double reduction) {}
    private static final class Mining {Block block;Material tool;int started,last;double progress;Mining(Block b,Material tool,int tick){block=b;this.tool=tool;started=tick;last=tick;}}
    private record Channel(Player owner,Location origin,int ready,int width) {}
    private record Excavation(BlockAddress at,BlockData data) {}
    static final class Mountain {
        final Player owner;final Location origin,peak;final Block benchmark;final Zombie body;final Map<UUID,Location> returns=new HashMap<>();
        final List<BlockAddress> mountain=new ArrayList<>();final List<Excavation> source;int excavated,lastSwing;double progress;
        Mountain(Player p,Location origin,Location peak,Zombie body,List<Excavation> source){owner=p;this.origin=origin;this.peak=peak;benchmark=peak.clone().add(24,-1,24).getBlock();benchmark.setType(Material.STONE,false);this.body=body;this.source=source;}
    }
    private final MobaPlugin plugin;private final ConfigurationSection cfg;private int clock,island;
    private final Map<BlockBreakEvent,Boolean> naturalBreaks=new WeakHashMap<>();private boolean excavating;
    private final Map<UUID,QuarryQuota> quotas=new HashMap<>();private final Map<UUID,Scan> scans=new HashMap<>();
    private final Map<UUID,Mining> mining=new HashMap<>();private final Map<UUID,Channel> channels=new HashMap<>();private final Map<UUID,Mountain> mountains=new HashMap<>();
    QuarrymanState(MobaPlugin plugin){this.plugin=plugin;cfg=plugin.getConfig().getConfigurationSection("quarryman");}
    private boolean has(Player p){return KitSupport.has(plugin,p,"quarryman")&&plugin.passives().has(p,"quarry_quota");}
    QuarryQuota quota(Player p){return quotas.computeIfAbsent(p.getUniqueId(),id->{var d=plugin.data(p).classState;var quota=new QuarryQuota(Integer.parseInt(d.getOrDefault("quota.stage","1")),Integer.parseInt(d.getOrDefault("quota.progress","0")),Integer.parseInt(d.getOrDefault("quota.completed","0")),Double.parseDouble(d.getOrDefault("quota.last","0")),clock+Integer.parseInt(d.getOrDefault("quota.remaining",Integer.toString(cfg.getInt("quotaTicks")))));quota.strike=Boolean.parseBoolean(d.getOrDefault("quota.strike","false"));quota.cleared=Boolean.parseBoolean(d.getOrDefault("quota.cleared","false"));return quota;});}
    private int demand(QuarryQuota q){return cfg.getInt("baseDemand")+Math.max(0,q.stage-1)*cfg.getInt("stageDemand");}
    private int width(Player p,int extra){return Math.min(cfg.getInt("maxWidth"),(int)Math.ceil(quota(p).scanWidth()+extra));}
    private boolean inside(Location at,Location centre,int width){return at.getWorld()==centre.getWorld()&&at.getBlockX()-centre.getBlockX()>=-width/2&&at.getBlockX()-centre.getBlockX()<-width/2+width&&at.getBlockY()-centre.getBlockY()>=-width/2&&at.getBlockY()-centre.getBlockY()<-width/2+width&&at.getBlockZ()-centre.getBlockZ()>=-width/2&&at.getBlockZ()-centre.getBlockZ()<-width/2+width;}
    private Scan scanAt(Location at){return scans.values().stream().filter(s->clock<s.until&&inside(at,s.centre,s.width)).max(Comparator.comparingDouble(s->s.reduction)).orElse(null);}
    boolean scan(Player p){scans.put(p.getUniqueId(),new Scan(p.getLocation().clone(),width(p,0),clock+600,quota(p).reduction()));return true;}
    boolean strike(Player p){quota(p).toggle();return true;}
    boolean striking(Player p){return quota(p).strike;}
    @EventHandler(priority=EventPriority.LOWEST) public void witness(BlockBreakEvent e){if(has(e.getPlayer()))naturalBreaks.put(e,!plugin.provenance().isPlayerPlaced(e.getBlock()));}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void mined(BlockBreakEvent e){Player p=e.getPlayer();if(has(p)&&!excavating&&Boolean.TRUE.equals(naturalBreaks.remove(e))&&e.getBlock().getType().getHardness()>0&&!plugin.displacement().isReserved(e.getBlock()))quota(p).mined();Mining m=mining.get(p.getUniqueId());if(m!=null){m.block=null;m.last=clock;}}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void start(BlockDamageEvent e){if(!has(e.getPlayer())&&scanAt(e.getBlock().getLocation())==null)return;Player p=e.getPlayer();Mining previous=mining.get(p.getUniqueId());Mining m=new Mining(e.getBlock(),p.getInventory().getItemInMainHand().getType(),clock);if(previous!=null&&clock-previous.last<=cfg.getInt("miningGraceTicks")&&previous.tool==m.tool)m.started=previous.started;mining.put(p.getUniqueId(),m);}
    @EventHandler(priority=EventPriority.MONITOR) public void abort(BlockDamageAbortEvent e){Mining m=mining.get(e.getPlayer().getUniqueId());if(m!=null){m.block=null;m.last=clock;}}
    private Material raised(Material tool){String name=tool.name();if(!name.endsWith("_PICKAXE")&&!name.endsWith("_AXE")&&!name.endsWith("_SHOVEL")&&!name.endsWith("_HOE"))return tool;String tier=name.substring(0,name.indexOf('_'));String next=switch(tier){case "WOODEN","GOLDEN"->"STONE";case "STONE"->"IRON";case "IRON"->"DIAMOND";case "DIAMOND"->"NETHERITE";default->tier;};Material result=Material.matchMaterial(next+name.substring(name.indexOf('_')));return result==null?tool:result;}
    private boolean promoted(Player p){Mining m=mining.get(p.getUniqueId());return m!=null&&clock-m.started>=200&&clock-m.last<=cfg.getInt("miningGraceTicks");}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void harvest(BlockBreakEvent e){Player p=e.getPlayer();if(!has(p)||!promoted(p))return;ItemStack original=p.getInventory().getItemInMainHand();if(raised(original.getType())==original.getType())return;ItemStack tier=original.clone();tier.setType(raised(original.getType()));if(e.getBlock().getDrops(original,p).isEmpty()&&!e.getBlock().getDrops(tier,p).isEmpty()){e.setDropItems(false);for(var item:e.getBlock().getDrops(tier,p))e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(),item);}}
    @EventHandler(priority=EventPriority.NORMAL,ignoreCancelled=true) public void hit(EntityDamageByEntityEvent e){if(AbilityDamage.active()||!(e.getDamager() instanceof Player p)||!has(p)||!quota(p).strike||!p.getInventory().getItemInMainHand().getType().name().endsWith("_PICKAXE"))return;Material strongest=Arrays.stream(p.getInventory().getStorageContents()).filter(Objects::nonNull).map(ItemStack::getType).filter(m->m.name().endsWith("_PICKAXE")).max(Comparator.comparingDouble(this::weapon)).orElse(p.getInventory().getItemInMainHand().getType());double charge=p.getAttackCooldown();e.setDamage(quota(p).strikeDamage(weapon(strongest),charge));}
    private double weapon(Material m){return switch(m){case NETHERITE_PICKAXE->6;case DIAMOND_PICKAXE->5;case IRON_PICKAXE->4;case STONE_PICKAXE->3;default->2;};}
    boolean channel(Player p){if(mountains.containsKey(p.getUniqueId())||channels.containsKey(p.getUniqueId()))return false;plugin.kitGlow().acquire(p,"eternity");channels.put(p.getUniqueId(),new Channel(p,p.getLocation().clone(),clock+cfg.getInt("channelTicks"),width(p,2)));return true;}
    boolean channeling(Player p){return channels.containsKey(p.getUniqueId());}
    boolean release(Player p){Channel c=channels.get(p.getUniqueId());if(c==null||clock<c.ready)return false;channels.remove(p.getUniqueId());
        List<Excavation> source=new ArrayList<>();int low=-c.width/2,high=low+c.width;
        for(int x=low;x<high;x++)for(int y=low;y<high;y++)for(int z=low;z<high;z++){Block b=c.origin.getBlock().getRelative(x,y,z);if(removable(b))source.add(new Excavation(BlockAddress.of(b),b.getBlockData().clone()));}
        source.sort(Comparator.comparingDouble(e->e.data.getMaterial().getHardness()));if(source.isEmpty()){plugin.kitGlow().release(p,"eternity");return false;}
        World world=Bukkit.getWorld("moba_eternity_domain");if(world==null)world=new WorldCreator("moba_eternity_domain").generator(new VoidGenerator()).generateStructures(false).createWorld();if(world==null){plugin.kitGlow().release(p,"eternity");return false;}
        world.setAutoSave(false);int centre=island++*64;Location peak=new Location(world,centre+.5,82,.5);
        Zombie body=p.getWorld().spawn(c.origin,Zombie.class,e->{e.setAI(false);e.setSilent(true);e.setPersistent(false);e.setAdult();e.setShouldBurnInDay(false);e.setGravity(false);e.setGlowing(true);e.customName(net.kyori.adventure.text.Component.text(p.getName()+" — World Quarryman"));e.setCustomNameVisible(true);OwnedEntities.tag(e,p);e.getEquipment().setArmorContents(p.getInventory().getArmorContents());e.getEquipment().setItemInMainHand(p.getInventory().getItemInMainHand());});
        Mountain m=new Mountain(p,c.origin,peak,body,source);mountains.put(p.getUniqueId(),m);
        for(int y=0;y<18;y++){int r=9-y/2;for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)if(x*x+z*z<=r*r){Block b=world.getBlockAt(centre+x,64+y,z);b.setType(y>14?Material.OBSIDIAN:y%3==0?Material.DEEPSLATE:Material.STONE,false);m.mountain.add(BlockAddress.of(b));}}
        peak.clone().add(0,-1,0).getBlock().setType(Material.BEDROCK,false);
        for(Player enemy:Bukkit.getOnlinePlayers())if(Targetability.status(enemy,p)&&inside(enemy.getLocation(),c.origin,c.width)){m.returns.put(enemy.getUniqueId(),enemy.getLocation());enemy.teleport(new Location(world,centre+11.5,65,.5));world.getBlockAt(centre+11,64,0).setType(Material.STONE,false);m.mountain.add(BlockAddress.of(world.getBlockAt(centre+11,64,0)));}
        m.returns.put(p.getUniqueId(),p.getLocation());p.teleport(peak);return true;
    }
    private boolean removable(Block b){return KitSupport.loaded(b.getLocation())&&!b.isEmpty()&&!b.isLiquid()&&b.getType().getHardness()>=0&&!plugin.displacement().isReserved(b)&&!Set.of(Material.END_PORTAL,Material.END_PORTAL_FRAME,Material.END_GATEWAY,Material.SPAWNER,Material.TRIAL_SPAWNER,Material.VAULT).contains(b.getType())&&!(b.getState() instanceof org.bukkit.inventory.InventoryHolder);}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void swing(PlayerAnimationEvent e){Mountain m=mountains.get(e.getPlayer().getUniqueId());if(m!=null){Block b=e.getPlayer().getTargetBlockExact(6);if(b!=null&&b.getType()==Material.BEDROCK&&b.getLocation().distanceSquared(m.peak)<4)m.lastSwing=clock;}}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void damage(EntityDamageEvent e){if(e.getFinalDamage()<=0)return;Mountain m=e.getEntity() instanceof Player p?mountains.get(p.getUniqueId()):mountains.values().stream().filter(a->a.body.equals(e.getEntity())).findFirst().orElse(null);if(m!=null){if(e instanceof EntityDamageByEntityEvent hit&&TeamDamage.authored(e.getCause())){Player source=TeamDamage.owner(hit.getDamager());if(source!=null&&Targetability.allied(m.owner,source)&&(!e.getEntity().equals(m.owner)||!source.equals(m.owner))){e.setCancelled(true);return;}}if(e.getEntity().equals(m.body)){e.setCancelled(true);double effective=e.getFinalDamage();end(m);double loss=plugin.getConfig().getBoolean("features.vitalsScaling.enabled")?Vitals.scaleDamage(effective,plugin.effectiveMaxHealth(m.owner)):effective;m.owner.setHealth(Math.max(0,m.owner.getHealth()-loss));if(plugin.combatState()!=null){plugin.combatState().markAbilityActivation(m.owner);if(e instanceof EntityDamageByEntityEvent hit)plugin.combatState().markAbilityActivation(hit.getDamager());}}else end(m);}if(e.getEntity() instanceof Player p)cancel(p);}
    void tick(){clock++;if(plugin.inputs()==null)return;
        for(Player p:Bukkit.getOnlinePlayers())if(has(p)){QuarryQuota q=quota(p);q.refresh(clock,cfg.getInt("quotaTicks"),cfg.getInt("stageFloor"),cfg.getInt("stageCeiling"),demand(q));}
        for(var entry:List.copyOf(mining.entrySet())) {
            Player p=Bukkit.getPlayer(entry.getKey());Mining m=entry.getValue();
            if(p==null){mining.remove(entry.getKey());continue;}
            if(m.block==null||plugin.stun().actionsRefused(p)||p.getInventory().getItemInMainHand().getType()!=m.tool||!m.block.equals(p.getTargetBlockExact(6)))continue;
            m.last=clock;double hardness=m.block.getType().getHardness();Scan scan=scanAt(m.block.getLocation());double reduction=scan==null?0:scan.reduction;double speed=m.block.getBreakSpeed(p);boolean tierRaised=has(p)&&promoted(p);
            if(tierRaised){ItemStack tier=p.getInventory().getItemInMainHand().clone();tier.setType(raised(tier.getType()));if(m.block.getDrops(p.getInventory().getItemInMainHand(),p).isEmpty()&&!m.block.getDrops(tier,p).isEmpty())speed*=100/30.0;}
            if(hardness>0&&reduction>0)speed*=hardness/Math.max(.01,hardness-reduction);
            if(hardness>=0&&(reduction>0||tierRaised)){m.progress+=speed;if(m.progress>=1){Block block=m.block;m.block=null;p.breakBlock(block);}}
        }
        for(var c:List.copyOf(channels.values())){if(!has(c.owner)||c.owner.getWorld()!=c.origin.getWorld()||c.owner.getLocation().distanceSquared(c.origin)>4){cancel(c.owner);continue;}if(clock%10==0){int r=c.width/2;for(int x:new int[]{-r,r})for(int y:new int[]{-r,r})for(int z:new int[]{-r,r})c.origin.getWorld().spawnParticle(Particle.END_ROD,c.origin.clone().add(x,y,z),2);}}
        for(var m:List.copyOf(mountains.values())){if(!has(m.owner)||!m.body.isValid()||m.owner.getWorld()!=m.peak.getWorld()){end(m);continue;}m.body.setGlowing(true);m.body.getEquipment().setItemInMainHand(m.owner.getInventory().getItemInMainHand());
            if(clock-m.lastSwing>cfg.getInt("swingGraceTicks")||plugin.stun().actionsRefused(m.owner))continue;
            ItemStack tool=m.owner.getInventory().getItemInMainHand();if(!tool.getType().name().endsWith("_PICKAXE"))continue;double setup=m.benchmark.getBreakSpeed(m.owner)*Material.STONE.getHardness()*30;m.progress+=Math.max(0,setup)/cfg.getDouble("bedrockWork");
            int desired=Math.min(m.source.size(),(int)(m.source.size()*m.progress));while(m.excavated<desired){Excavation e=m.source.get(m.excavated++);Block b=e.at.block();if(b!=null&&b.getBlockData().equals(e.data)&&removable(b)){BlockBreakEvent event=new BlockBreakEvent(b,m.owner);excavating=true;try{Bukkit.getPluginManager().callEvent(event);if(!event.isCancelled())b.setType(Material.AIR,false);}finally{excavating=false;}}}
            int remove=Math.min(m.mountain.size(),(int)(m.mountain.size()*Math.min(1,m.progress)));for(int i=0;i<remove;i++){Block b=m.mountain.get(i).block();if(b!=null&&b.getType()!=Material.BEDROCK)b.setType(Material.AIR,false);}
            if(m.progress>=1)end(m);
        }
    }
    private void end(Mountain m){if(mountains.remove(m.owner.getUniqueId())==null)return;plugin.kitGlow().release(m.owner,"eternity");m.body.remove();for(var entry:m.returns.entrySet()){Player p=Bukkit.getPlayer(entry.getKey());if(p!=null){Location at=entry.getValue();Location safe=KitSupport.safe(at,2);p.teleport(safe==null?at:safe);}}for(var at:m.mountain){Block b=at.block();if(b!=null)b.setType(Material.AIR,false);}m.peak.clone().add(0,-1,0).getBlock().setType(Material.AIR,false);m.benchmark.setType(Material.AIR,false);}
    void cancel(Player p){if(channels.remove(p.getUniqueId())!=null)plugin.kitGlow().release(p,"eternity");}
    void forget(Player p){cancel(p);Mountain m=mountains.get(p.getUniqueId());if(m!=null)end(m);for(var other:mountains.values()){Location back=other.returns.remove(p.getUniqueId());if(back!=null&&p.isOnline())p.teleport(back);}QuarryQuota q=quotas.remove(p.getUniqueId());if(q!=null&&plugin.data(p)!=null){var d=plugin.data(p).classState;d.put("quota.stage",Integer.toString(q.stage));d.put("quota.progress",Integer.toString(q.progress));d.put("quota.completed",Integer.toString(q.completed));d.put("quota.last",Double.toString(q.last));d.put("quota.remaining",Long.toString(Math.max(1,q.expires-clock)));d.put("quota.strike",Boolean.toString(q.strike));d.put("quota.cleared",Boolean.toString(q.cleared));}scans.remove(p.getUniqueId());mining.remove(p.getUniqueId());}
    List<String> hud(Player p){var q=quota(p);return List.of("Quota "+q.stage+"  "+q.progress+"/"+demand(q)+"  "+Math.max(0,(q.expires-clock+19)/20)+"s", "CQ "+q.completed+"  LQC "+Math.round(q.last*100)+"%"+(q.strike?"  Strike":q.cleared?"  Waiting":""));}
    void close(){for(var m:List.copyOf(mountains.values()))end(m);quotas.clear();scans.clear();mining.clear();channels.clear();}
}
