package com.minecraftmoba.plugin;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;
import java.nio.file.*;
import java.util.*;
/** Persistent authored structures and temporary traversal/shoe loans share real world materials. */
final class PaverState implements Listener {
 private record Structure(UUID owner,String branch){}
 static final class Chain {Location anchor;final TwoPitons pitons=new TwoPitons();int until;String branch;Chain(Location at,String branch,int until){anchor=at;this.branch=branch;this.until=until;}}
 private record Loan(UUID token,Player owner,List<ItemStack> inventory,List<BlockDisplay> visuals,Location impact,int until,int mass){}
 private final MobaPlugin plugin;private final ConfigurationSection cfg;private final Path file;private int clock;
 private record Hardening(BlockAddress at,PotionEffect original){}
 private final Map<UUID,Hardening> miners=new HashMap<>();
 private final Map<BlockAddress,Structure> reinforced=new HashMap<>(),chamfered=new HashMap<>();private final Map<UUID,Chain> chains=new HashMap<>();private final Set<UUID> release=new HashSet<>();private final Map<UUID,Integer> moved=new HashMap<>();private final List<Loan> shoes=new ArrayList<>();
 PaverState(MobaPlugin p){plugin=p;cfg=p.getConfig().getConfigurationSection("paver");file=p.getDataFolder()==null?null:p.getDataFolder().toPath().resolve("paver-structures.yml");load();}
 static boolean concrete(Material m){return m.name().endsWith("_CONCRETE");} static boolean powder(Material m){return m.name().endsWith("_CONCRETE_POWDER");}
 private boolean has(Player p){return KitSupport.has(plugin,p,"paver")&&plugin.passives().has(p,"concrete_mixer");}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void place(BlockPlaceEvent e){Block b=e.getBlock();var key=BlockAddress.of(b);reinforced.remove(key);chamfered.remove(key);if(!has(e.getPlayer())){save();return;}
  if(concrete(b.getType()))reinforced.put(key,new Structure(e.getPlayer().getUniqueId(),KitSupport.branch(plugin,e.getPlayer(),"rebar_chain")));
  if(powder(b.getType())){String branch=KitSupport.branch(plugin,e.getPlayer(),"chamfering");boolean supported=key.adjacent().stream().anyMatch(at->reinforced.containsKey(at)&&at.block()!=null&&concrete(at.block().getType())||branch.equals("natural_mix")&&at.block()!=null&&at.block().getType()==Material.GRASS_BLOCK);if(supported)chamfered.put(key,new Structure(e.getPlayer().getUniqueId(),branch));}save();
 }
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void physics(BlockPhysicsEvent e){if(chamfered.containsKey(BlockAddress.of(e.getBlock()))&&powder(e.getBlock().getType()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void falling(EntityChangeBlockEvent e){if(chamfered.containsKey(BlockAddress.of(e.getBlock()))&&powder(e.getBlock().getType())&&e.getTo().isAir())e.setCancelled(true);}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void hardened(BlockFormEvent e){var key=BlockAddress.of(e.getBlock());Structure s=chamfered.get(key);if(s==null||!concrete(e.getNewState().getType()))return;chamfered.remove(key);reinforced.put(key,s);if(s.branch.equals("pozzolan_patch")){Player owner=Bukkit.getPlayer(s.owner);if(owner!=null)for(var enemy:KitSupport.enemies(owner,e.getBlock().getLocation().add(.5,.5,.5),cfg.getDouble("patchRadius")))plugin.toolboxStatuses().root(enemy,cfg.getInt("patchRootTicks"));}save();}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void hardness(BlockDamageEvent e){var s=reinforced.get(BlockAddress.of(e.getBlock()));if(s==null||!concrete(e.getBlock().getType()))return;e.setInstaBreak(false);miners.putIfAbsent(e.getPlayer().getUniqueId(),new Hardening(BlockAddress.of(e.getBlock()),e.getPlayer().getPotionEffect(PotionEffectType.MINING_FATIGUE)));KitSupport.potion(e.getPlayer(),PotionEffectType.MINING_FATIGUE,3,s.branch.equals("galvanized")?cfg.getInt("galvanizedFatigue"):cfg.getInt("reinforcedFatigue"));}
 @EventHandler public void abort(BlockDamageAbortEvent e){restoreMiner(e.getPlayer());}
 private void restoreMiner(Player p){Hardening h=miners.remove(p.getUniqueId());if(h==null)return;PotionEffect current=p.getPotionEffect(PotionEffectType.MINING_FATIGUE);if(current!=null&&current.getDuration()<=3&&current.getAmplifier()<=cfg.getInt("galvanizedFatigue")){p.removePotionEffect(PotionEffectType.MINING_FATIGUE);if(h.original!=null)p.addPotionEffect(h.original);}}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void broken(BlockBreakEvent e){restoreMiner(e.getPlayer());BlockAddress key=BlockAddress.of(e.getBlock());reinforced.remove(key);chamfered.remove(key);save();}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void exploded(BlockExplodeEvent e){for(var b:e.blockList()){reinforced.remove(BlockAddress.of(b));chamfered.remove(BlockAddress.of(b));}save();}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void exploded(EntityExplodeEvent e){for(var b:e.blockList()){reinforced.remove(BlockAddress.of(b));chamfered.remove(BlockAddress.of(b));}save();}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void extend(BlockPistonExtendEvent e){moveStructures(e.getBlocks(),e.getDirection());}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void retract(BlockPistonRetractEvent e){moveStructures(e.getBlocks(),e.getDirection());}
 private void moveStructures(List<Block> blocks,BlockFace direction){Map<BlockAddress,Structure> movedConcrete=new HashMap<>(),movedPowder=new HashMap<>();for(Block b:blocks){BlockAddress at=BlockAddress.of(b);Structure concrete=reinforced.remove(at),powder=chamfered.remove(at);if(concrete!=null)movedConcrete.put(BlockAddress.of(b.getRelative(direction)),concrete);if(powder!=null)movedPowder.put(BlockAddress.of(b.getRelative(direction)),powder);}reinforced.putAll(movedConcrete);chamfered.putAll(movedPowder);save();}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void move(PlayerMoveEvent e){if(e.getTo()==null)return;Player p=e.getPlayer();Block floor=e.getTo().clone().add(0,-.05,0).getBlock();var key=BlockAddress.of(floor);var support=chamfered.get(key);
  if(support!=null&&support.branch.equals("steep_grading")&&p.isSneaking()){Vector direction=e.getTo().toVector().subtract(e.getFrom().toVector()).setY(0);if(direction.lengthSquared()>1e-6){Location beyond=e.getTo().clone().add(direction.normalize().multiply(.5)).add(0,-.05,0);if(beyond.getBlock().isPassable())p.setVelocity(direction.multiply(.22).setY(-.12));}}
  if(!has(p)||e.getFrom().distanceSquared(e.getTo())<.0001)return;
  if(release.contains(p.getUniqueId())&&support!=null&&support.owner.equals(p.getUniqueId())){release.remove(p.getUniqueId());release(key);}
  int value=moved.merge(p.getUniqueId(),p.isInWater()||Arrays.stream(p.getInventory().getStorageContents()).filter(Objects::nonNull).anyMatch(i->i.getType()==Material.WATER_BUCKET)?2:1,Integer::sum);if(value>=cfg.getInt("conversionSteps")){moved.put(p.getUniqueId(),0);mix(p);}
 }
 private void mix(Player p){var inventory=p.getInventory();for(Material dye:Material.values())if(dye.name().endsWith("_DYE")&&inventory.contains(dye)&&inventory.contains(Material.SAND,4)&&inventory.contains(Material.GRAVEL,4)){
   Material out=Material.matchMaterial(dye.name().replace("_DYE","_CONCRETE"));if(out==null)return;var amount=new ItemStack(out,8);if(inventory.firstEmpty()<0)return;inventory.removeItem(new ItemStack(Material.SAND,4),new ItemStack(Material.GRAVEL,4),new ItemStack(dye));inventory.addItem(amount);return;
  }}
 int release(BlockAddress initial){Deque<BlockAddress> queue=new ArrayDeque<>();Set<BlockAddress> seen=new HashSet<>();queue.add(initial);int count=0;while(!queue.isEmpty()&&count<cfg.getInt("releaseCap")){var at=queue.remove();if(!seen.add(at)||!chamfered.containsKey(at))continue;Block b=at.block();chamfered.remove(at);count++;queue.addAll(at.adjacent());if(b!=null&&powder(b.getType())){var data=b.getBlockData();b.setType(Material.AIR,false);b.setBlockData(data,true);}}save();return count;}
 boolean chaining(Player p){return chains.containsKey(p.getUniqueId());}
 boolean chain(Player p,String branch){Chain c=chains.get(p.getUniqueId());if(c!=null&&branch.equals("pitons")){
   if(c.pitons.size()<2){Block b=p.getTargetBlockExact(cfg.getInt("chainRange"));if(b==null||!concrete(b.getType())||!b.getWorld().equals(c.anchor.getWorld()))return false;c.pitons.place(point(b.getLocation().add(.5,1,.5)));return true;}
   var target=c.pitons.farther(point(p.getLocation()));Location to=new Location(c.anchor.getWorld(),target.x(),target.y(),target.z());if(!KitSupport.loaded(to)||plugin.stun().movementRefused(p))return false;var hit=p.getWorld().rayTraceBlocks(p.getEyeLocation(),to.toVector().subtract(p.getEyeLocation().toVector()).normalize(),p.getEyeLocation().distance(to),FluidCollisionMode.NEVER,true);if(hit!=null&&hit.getHitPosition().distance(to.toVector())>1.5)return false;Location safe=KitSupport.safe(to,1);if(safe==null)return false;p.teleport(safe);c.anchor=to;return true;
  }
  Block b=p.getTargetBlockExact(cfg.getInt("chainRange"));if(b==null||!concrete(b.getType()))return false;c=new Chain(b.getLocation().add(.5,1,.5),branch,clock+cfg.getInt("chainTicks"));c.pitons.place(point(c.anchor));chains.put(p.getUniqueId(),c);return true;
 }
 private static TwoPitons.Point point(Location at){return new TwoPitons.Point(at.getX(),at.getY(),at.getZ());}
 boolean release(Player p){release.add(p.getUniqueId());return true;}
 boolean shoes(Player p,Location target){Location impact=KitSupport.safe(target,2);if(impact==null)return false;UUID token=UUID.randomUUID();List<ItemStack> consumed=new ArrayList<>();int mass=0,limit=cfg.getInt("shoeMass");
  // Validate all final placement cells before committing any carried material.
  List<Block> cells=new ArrayList<>();for(int y=0;y<3;y++)for(int x=-2;x<=2;x++)for(int z=-1;z<=1;z++){Block b=impact.clone().add(x,y,z).getBlock();if(b.isEmpty()&&!plugin.displacement().isReserved(b))cells.add(b);}limit=Math.min(limit,cells.size());if(limit==0)return false;
  for(int i=0;i<36&&mass<limit;i++){var item=p.getInventory().getItem(i);if(item==null||!concrete(item.getType()))continue;int take=Math.min(item.getAmount(),limit-mass);consumed.add(item.asQuantity(take));p.getInventory().setItem(i,take==item.getAmount()?null:item.asQuantity(item.getAmount()-take));mass+=take;}
  int inventoryMass=mass;for(var entry:reinforced.entrySet()){if(mass>=cfg.getInt("shoeMass"))break;if(!entry.getValue().owner.equals(p.getUniqueId()))continue;Block b=entry.getKey().block();if(b==null||b.getWorld()!=p.getWorld()||b.getLocation().distanceSquared(p.getLocation())>cfg.getDouble("borrowRadius")*cfg.getDouble("borrowRadius")||!concrete(b.getType()))continue;if(plugin.displacement().take(b,token)!=null)mass++;}
  if(mass==0){plugin.displacement().restore(token);return false;}plugin.kitGlow().acquire(p,"shoes:"+token);
  List<BlockDisplay> displays=new ArrayList<>();for(int side:new int[]{-1,1})displays.add(p.getWorld().spawn(impact.clone().add(side,5,0),BlockDisplay.class,e->{e.setBlock(Material.GRAY_CONCRETE.createBlockData());e.setPersistent(false);e.setTransformation(new org.bukkit.util.Transformation(new org.joml.Vector3f(),new org.joml.Quaternionf(),new org.joml.Vector3f(1.5f,1.5f,2f),new org.joml.Quaternionf()));}));shoes.add(new Loan(token,p,consumed,displays,impact,clock+cfg.getInt("shoeGatherTicks"),mass));return true;
 }
 void tick(){clock++;if(plugin.inputs()==null)return;for(var entry:List.copyOf(miners.entrySet())){Player p=Bukkit.getPlayer(entry.getKey());Block b=entry.getValue().at.block();Structure structure=reinforced.get(entry.getValue().at);if(p==null||b==null||structure==null||!concrete(b.getType())||!b.equals(p.getTargetBlockExact(6))){if(p!=null)restoreMiner(p);else miners.remove(entry.getKey());continue;}KitSupport.potion(p,PotionEffectType.MINING_FATIGUE,3,structure.branch.equals("galvanized")?cfg.getInt("galvanizedFatigue"):cfg.getInt("reinforcedFatigue"));}for(var entry:List.copyOf(chains.entrySet())){Player p=Bukkit.getPlayer(entry.getKey());Chain c=entry.getValue();if(p==null||!has(p)||clock>=c.until||p.getWorld()!=c.anchor.getWorld()||p.getLocation().distanceSquared(c.anchor)>cfg.getDouble("chainRadius")*cfg.getDouble("chainRadius")){chains.remove(entry.getKey());continue;}if(plugin.stun().movementRefused(p))continue;Block near=p.getLocation().clone().add(p.getLocation().getDirection().setY(0).multiply(.65)).getBlock();if(concrete(near.getType())){Vector v=p.getVelocity();v.setY(p.isSneaking()?-.1:cfg.getDouble("climbSpeed"));if(c.branch.equals("beveled_edges")){v.setX(v.getX()*cfg.getDouble("beveledSpeed"));v.setZ(v.getZ()*cfg.getDouble("beveledSpeed"));}p.setVelocity(v);}}
  for(var shoe:List.copyOf(shoes)){if(!has(shoe.owner)){finish(shoe,false);continue;}if(clock<shoe.until)continue;for(var enemy:KitSupport.enemies(shoe.owner,shoe.impact,cfg.getDouble("shoeRadius"))){if(Math.abs(enemy.getLocation().getX()-shoe.impact.getX())<2.5&&Math.abs(enemy.getLocation().getZ()-shoe.impact.getZ())<1.5)AbilityDamage.deal(enemy,shoe.mass*cfg.getDouble("damagePerMass"),shoe.owner);if(enemy.isOnGround())KitSupport.push(plugin,enemy,enemy.getVelocity().setY(shoe.mass*cfg.getDouble("launchPerMass")));}finish(shoe,true);}
 }
 private void finish(Loan shoe,boolean impact){plugin.kitGlow().release(shoe.owner,"shoes:"+shoe.token);plugin.displacement().restore(shoe.token);shoe.visuals.forEach(Entity::remove);shoes.remove(shoe);  int i=0;for(var stack:shoe.inventory)for(int n=0;n<stack.getAmount();n++){Block b=shoe.impact.clone().add(i%5-2,i/15,(i/5)%3-1).getBlock();i++;if(!b.isEmpty()||plugin.displacement().isReserved(b)){b.getWorld().dropItemNaturally(b.getLocation(),stack.asOne());continue;}b.setType(stack.getType(),false);plugin.provenance().mark(b,true);reinforced.put(BlockAddress.of(b),new Structure(shoe.owner.getUniqueId(),KitSupport.branch(plugin,shoe.owner,"rebar_chain")));}save();
 }
 void forget(Player p){restoreMiner(p);chains.remove(p.getUniqueId());release.remove(p.getUniqueId());moved.remove(p.getUniqueId());for(var s:List.copyOf(shoes))if(s.owner.equals(p))finish(s,false);}
 void close(){for(var id:List.copyOf(miners.keySet())){Player p=Bukkit.getPlayer(id);if(p!=null)restoreMiner(p);}miners.clear();for(var s:List.copyOf(shoes))finish(s,false);chains.clear();release.clear();save();}
 private void save(){if(file==null)return;var y=new YamlConfiguration();for(var pair:Map.of("reinforced",reinforced,"chamfered",chamfered).entrySet()){var rows=new ArrayList<Map<String,String>>();pair.getValue().forEach((at,s)->rows.add(Map.of("at",at.encode(),"owner",s.owner.toString(),"branch",s.branch)));y.set(pair.getKey(),rows);}y.set("version",1);SupplyLines.write(file,y);}
 private void load(){if(file==null||!Files.exists(file))return;var y=YamlConfiguration.loadConfiguration(file.toFile());if(y.getInt("version")!=1)throw new IllegalStateException("Unsupported Paver structure version");for(var pair:Map.of("reinforced",reinforced,"chamfered",chamfered).entrySet())for(var row:y.getMapList(pair.getKey()))pair.getValue().put(BlockAddress.decode(row.get("at").toString()),new Structure(UUID.fromString(row.get("owner").toString()),row.get("branch").toString()));}
}
