package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import java.nio.file.*;
import java.util.*;

/** Physical, conserved workforce. Ordinary workers survive logout/reload; temporary shifts never supply Capacity. */
final class SkeletonCrewState implements Listener {
    static final NamespacedKey OWNER=new NamespacedKey("minecraftmoba","crew_owner");
    private final NamespacedKey workerKey;
    static final class Member {
        final UUID id,owner;final int slots;final boolean temporary,horseman;
        Mob body;SkeletonHorse mount;Location position,source,destination;LivingEntity target;
        final List<ItemStack> cargo=new ArrayList<>();final CrewBurn burn=new CrewBurn();
        String state="IDLE",a1="hard_hat_zone",a2="water_break";int alarmUntil,refreshUntil,armorUntil,armorCount,attackAt;
        long departed;int departedSlots;double health;
        Member(UUID id,UUID owner,int slots,Location at,boolean temporary,boolean horseman,double health){this.id=id;this.owner=owner;this.slots=slots;position=at;this.temporary=temporary;this.horseman=horseman;this.health=health;}
    }
    private static final class Shift {
        final UUID owner;final int until;String mode="WAITING";
        Shift(UUID owner,int until){this.owner=owner;this.until=until;}
    }
    private final MobaPlugin plugin;final ConfigurationSection config;private final Path file;
    private final Map<UUID,Integer> stock=new HashMap<>();private final Map<UUID,Member> members=new LinkedHashMap<>();
    private final WorkforceAllocation allocation=new WorkforceAllocation();private final Map<UUID,Shift> shifts=new HashMap<>();
    private final List<Map<?,?>> unresolved=new ArrayList<>();private int clock;private boolean closing;
    SkeletonCrewState(MobaPlugin plugin){this.plugin=plugin;config=plugin.getConfig().getConfigurationSection("skeletonCrew");workerKey=new NamespacedKey(plugin,"crew_worker");file=plugin.getDataFolder()==null?null:plugin.getDataFolder().toPath().resolve("skeleton-crew.yml");load();}
    static Player owner(Entity e){if(e==null || e.getPersistentDataContainer()==null)return null;String value=e.getPersistentDataContainer().get(OWNER,PersistentDataType.STRING);return value==null?null:Bukkit.getPlayer(UUID.fromString(value));}
    static boolean owned(Entity e){return e!=null && e.getPersistentDataContainer()!=null && e.getPersistentDataContainer().has(OWNER,PersistentDataType.STRING);}
    private Member member(Entity entity){return entity==null?null:members.values().stream().filter(m->m.body!=null&&m.body.equals(entity)||m.mount!=null&&m.mount.equals(entity)).findFirst().orElse(null);}
    int stored(Player p){return stock.getOrDefault(p.getUniqueId(),0);}
    void grant(Player p,int amount){stock.merge(p.getUniqueId(),amount,Integer::sum);save();}
    int capacity(String line){return allocation.capacity(line);}
    boolean supports(SupplyLines.Line line){var method=allocation.get(line.method());return method!=null&&line.id().equals(method.commitment());}
    int active(Player p){return allocation.active(p.getUniqueId());}
    /** Whether this player has any Crew standing in the world (what Burning Out needs). */
    boolean hasActiveCrew(Player p){return !activeMembers(p).isEmpty();}
    List<Member> activeMembers(Player p){return members.values().stream().filter(m->m.owner.equals(p.getUniqueId())&&m.body!=null&&m.body.isValid()).toList();}
    private String branch(Player p,String ability){var d=plugin.data(p);if(d==null)return null;String selected=d.classState.get("branch."+ability);return selected!=null?selected:plugin.inputs().definition(d.classId).branchFor(ability);}
    private boolean allied(Member m,Entity entity){
        if(entity.equals(m.body)||entity.equals(m.mount))return true;
        if(owned(entity)){String owner=entity.getPersistentDataContainer().get(OWNER,PersistentDataType.STRING);if(m.owner.toString().equals(owner))return true;}
        Player p=Bukkit.getPlayer(m.owner);return p!=null && (p.equals(entity)||Targetability.allied(entity,p));
    }
    private boolean hostile(Member m,LivingEntity target){
        return target!=null&&!target.isDead()&&target.isValid()&&target.getWorld()==m.position.getWorld()&&!allied(m,target)
                && (Bukkit.getPlayer(m.owner)!=null || !(target instanceof Player)&&!owned(target));
    }
    private boolean spawn(Member m){
        if(!m.position.getWorld().isChunkLoaded(m.position.getBlockX()>>4,m.position.getBlockZ()>>4))return false;
        Class<? extends Mob> type=m.horseman?WitherSkeleton.class:Skeleton.class;
        m.body=m.position.getWorld().spawn(m.position,type,e->{
            e.setPersistent(false);e.setRemoveWhenFarAway(false);e.setCanPickupItems(false);e.setAI(true);
            e.customName(Component.text(m.horseman?"Headless Horseman":"Crew "+shortId(m.id)));e.setCustomNameVisible(true);
            e.getPersistentDataContainer().set(OWNER,PersistentDataType.STRING,m.owner.toString());e.getPersistentDataContainer().set(workerKey,PersistentDataType.STRING,m.id.toString());
            e.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));e.getEquipment().setItemInMainHandDropChance(0);
        });
        Bukkit.getMobGoals().removeAllGoals(m.body);m.body.getPathfinder().setCanFloat(true);
        var hp=m.body.getAttribute(Attribute.MAX_HEALTH);hp.setBaseValue(config.getDouble(m.horseman?"horsemanHealth":"health"));m.body.setHealth(Math.max(.1,Math.min(hp.getValue(),m.health)));
        var attack=m.body.getAttribute(Attribute.ATTACK_DAMAGE);if(attack!=null)attack.setBaseValue(config.getDouble(m.horseman?"horsemanDamage":"damage"));
        if(m.horseman){m.mount=m.position.getWorld().spawn(m.position,SkeletonHorse.class,e->{e.setPersistent(false);e.setRemoveWhenFarAway(false);e.getPersistentDataContainer().set(OWNER,PersistentDataType.STRING,m.owner.toString());});Bukkit.getMobGoals().removeAllGoals(m.mount);m.mount.addPassenger(m.body);}
        armor(m);return true;
    }
    private Location near(Location at){
        for(int r=1;r<=3;r++)for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++){
            var b=at.getBlock().getRelative(x,0,z);if(b.getWorld().isChunkLoaded(b.getX()>>4,b.getZ()>>4) && b.isPassable() && b.getRelative(0,1,0).isPassable() && b.getRelative(0,-1,0).getType().isSolid())return b.getLocation().add(.5,0,.5);
        }return null;
    }
    boolean raise(Player p,Location at){
        Location position=near(at);if(position==null)return false;
        var committed=allocation.workers().stream().filter(w->w.owner().equals(p.getUniqueId())&&w.commitment()!=null&&members.containsKey(w.id())).findFirst();
        if(committed.isPresent()){Member m=members.get(committed.get().id());m.position=position;if(!spawn(m))return false;allocation.withdraw(m.id);save();return true;}
        if(stored(p)<config.getInt("raiseCost") || allocation.count(p.getUniqueId())>=config.getInt("limit")){p.sendMessage("Crew "+stored(p)+"; deployed/committed "+allocation.count(p.getUniqueId())+"/"+config.getInt("limit"));return false;}
        Member m=new Member(UUID.randomUUID(),p.getUniqueId(),config.getInt("cargoSlots"),position,false,false,config.getDouble("health"));
        if(!spawn(m))return false;allocation.add(m.id,m.owner,m.slots,config.getInt("limit"));members.put(m.id,m);stock.put(m.owner,stored(p)-config.getInt("raiseCost"));save();return true;
    }
    boolean strike(Player p,LivingEntity target,String selected){
        Member friendly=member(target);
        if(friendly!=null && Targetability.allied(target,p) && "hard_hat_zone".equals(selected)){
            double damage=config.getDouble("friendlyDamage");if(target.getHealth()<=damage){target.setHealth(0);return true;}
            target.setHealth(target.getHealth()-damage);friendly.armorCount=Math.min(4,friendly.armorCount+1);friendly.armorUntil=clock+config.getInt("armorTicks");armor(friendly);save();return true;
        }
        if(!Targetability.status(target,p))return false;
        double base=Math.max(1,p.getAttribute(Attribute.ATTACK_DAMAGE).getValue());double bonus=config.getDouble("strikeBonus")*("labored_union".equals(selected)?config.getDouble("unionBonusMultiplier"):1);
        AbilityDamage.deal(target,base+bonus,p);for(var m:activeMembers(p))m.target=target;return true;
    }
    private void armor(Member m){
        if(m.body==null)return;Material[] pieces={Material.IRON_HELMET,Material.IRON_CHESTPLATE,Material.IRON_LEGGINGS,Material.IRON_BOOTS};EquipmentSlot[] slots={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
        for(int i=0;i<4;i++){m.body.getEquipment().setItem(slots[i],i<m.armorCount?new ItemStack(pieces[i]):null);m.body.getEquipment().setDropChance(slots[i],0);}
    }
    boolean burn(Player p,String selected){
        var active=activeMembers(p);if(active.isEmpty())return false;
        for(var m:active){m.a2=selected;
            if("fire_drill".equals(selected)){m.alarmUntil=clock+config.getInt("alarmTicks");m.target=null;m.body.setTarget(null);}
            else m.body.setFireTicks(Math.max(m.body.getFireTicks(),config.getInt("burnTicks")));
        }return true;
    }
    boolean shift(Player p){
        if(shifts.containsKey(p.getUniqueId()))return false;
        var shift=new Shift(p.getUniqueId(),clock+config.getInt("ultimateTicks"));shifts.put(p.getUniqueId(),shift);
        for(int i=0;i<=config.getInt("temporaryCrew");i++){Location at=near(p.getLocation());if(at==null)continue;
            var m=new Member(UUID.randomUUID(),p.getUniqueId(),config.getInt("temporaryCargoSlots"),at,true,i==0,config.getDouble(i==0?"horsemanHealth":"health"));if(spawn(m))members.put(m.id,m);
        }
        if(members.values().stream().noneMatch(m->m.temporary&&m.owner.equals(p.getUniqueId()))){shifts.remove(p.getUniqueId());return false;}choose(shift);return true;
    }
    String specialization(Player p){var s=shifts.get(p.getUniqueId());return s==null?"NONE":s.mode;}
    private void choose(Shift shift){
        if(!shift.mode.equals("WAITING"))return;
        var ordinary=members.values().stream().filter(m->!m.temporary&&m.owner.equals(shift.owner)&&m.body!=null).toList();
        if(ordinary.stream().anyMatch(m->m.state.equals("COMBAT")))shift.mode="COMBAT";
        else if(ordinary.stream().anyMatch(m->m.state.equals("LOGISTICS")))shift.mode="LOGISTICS";
        if(!shift.mode.equals("WAITING"))for(var temp:members.values())if(temp.temporary&&temp.owner.equals(shift.owner)){
            if(shift.mode.equals("COMBAT"))temp.target=ordinary.stream().map(m->m.target).filter(Objects::nonNull).findFirst().orElse(null);
            else{var source=ordinary.stream().filter(m->m.source!=null&&m.destination!=null).findFirst();if(source.isPresent()){temp.source=source.get().source.clone();temp.destination=source.get().destination.clone();}}
        }
    }
    void tick(){
        clock++;if(closing || plugin.inputs()==null)return;resolve();
        for(var s:List.copyOf(shifts.values()))if(clock>=s.until)endShift(s.owner);else choose(s);
        for(var m:List.copyOf(members.values())){
            var ledger=allocation.get(m.id);if(!m.temporary && ledger!=null&&ledger.commitment()!=null)continue;
            if(m.body==null){if(!spawn(m))continue;}if(!m.body.isValid() || m.body.isDead())continue;
            m.position=m.body.getLocation();m.health=m.body.getHealth();Player owner=Bukkit.getPlayer(m.owner);
            if(owner!=null&&plugin.enrolled(owner)&&"skeleton_crew".equals(plugin.data(owner).classId)){m.a1=branch(owner,"graveyard_shift");m.a2=branch(owner,"burning_out");}
            if(m.armorCount>0&&clock>=m.armorUntil){m.armorCount=0;armor(m);}
            boolean alarm=clock<m.alarmUntil;
            if(!hostile(m,m.target) || m.target!=null&&m.target.getLocation().distanceSquared(m.position)>Math.pow(config.getDouble("pursuitRadius"),2))m.target=null;
            var shift=shifts.get(m.owner);String mode=m.temporary&&shift!=null?shift.mode:"ORDINARY";
            if(!alarm && m.target==null && ("labored_union".equals(m.a1)||mode.equals("COMBAT"))){
                m.target=m.body.getNearbyEntities(config.getDouble("aggroRadius"),4,config.getDouble("aggroRadius")).stream().filter(e->e instanceof LivingEntity living&&hostile(m,living)).map(e->(LivingEntity)e).min(Comparator.comparingDouble(e->e.getLocation().distanceSquared(m.position))).orElse(null);
            }
            if(mode.equals("LOGISTICS") || mode.equals("WAITING"))m.target=null;
            boolean burning=m.body.getFireTicks()>0;double speed=burning?config.getDouble("burnSpeed"):1;
            if(alarm)speed=config.getDouble("alarmSpeed");if(clock<m.refreshUntil)speed=Math.max(speed,config.getDouble("refreshedSpeed"));
            if(m.temporary&&!mode.equals("WAITING"))speed*=config.getDouble("ultimateSpeed");
            Mob walker=m.mount==null?m.body:m.mount;
            KitAttributes.set(walker,Attribute.MOVEMENT_SPEED,new NamespacedKey(plugin,"crew_speed"),speed-1,AttributeModifier.Operation.ADD_SCALAR);
            boolean fighting=!alarm&&m.target!=null;
            if(fighting){m.state="COMBAT";navigate(m,m.target.getLocation());m.body.lookAt(m.target);
                if(!plugin.stun().actionsRefused(m.body) && clock>=m.attackAt && m.body.getLocation().distanceSquared(m.target.getLocation())<=Math.pow(config.getDouble(m.horseman?"horsemanReach":"attackReach"),2)){
                    var damage=m.body.getAttribute(Attribute.ATTACK_DAMAGE);if(damage!=null)KitAttributes.set(m.body,Attribute.ATTACK_DAMAGE,new NamespacedKey(plugin,"crew_ultimate_damage"),m.temporary?config.getDouble("ultimateDamage")-1:0,AttributeModifier.Operation.ADD_SCALAR);
                    m.body.attack(m.target);m.attackAt=clock+config.getInt("attackTicks");
                }
            }else{
                Location destination=!m.cargo.isEmpty()?(m.destination!=null?m.destination:plugin.supplyLines().nearestNode(m.owner,m.position)):m.source;
                if(mode.equals("COMBAT")||mode.equals("WAITING")||alarm&&m.cargo.isEmpty())destination=null;
                if(m.cargo.isEmpty()&&destination!=null){var sourceInventory=SupplyLines.inventory(destination);if(sourceInventory==null||Arrays.stream(sourceInventory.getContents()).noneMatch(CargoSlots::eligible))destination=null;}
                if(destination!=null && SupplyLines.inventory(destination)!=null){m.state="LOGISTICS";navigate(m,destination.clone().add(.5,0,.5));
                    if(near(m,destination)){
                        if(m.cargo.isEmpty()){
                            CargoSlots.collect(SupplyLines.inventory(destination),m.cargo,m.slots);if(!m.cargo.isEmpty()){m.departed=Bukkit.getCurrentTick();m.departedSlots=CargoSlots.used(m.cargo);save();}
                        }else{
                            int before=CargoSlots.used(m.cargo);CargoSlots.deposit(SupplyLines.inventory(destination),m.cargo);
                            if(m.cargo.isEmpty() && m.source!=null && m.destination!=null && SupplyLines.key(destination).equals(SupplyLines.key(m.destination)) && m.departed>0)
                                plugin.transportEvidence().delivered(new TransportEvidence.Delivery(m.owner,m.id,m.source.clone(),m.destination.clone(),m.slots,m.departedSlots,m.departed,Bukkit.getCurrentTick(),m.temporary));
                            if(before!=CargoSlots.used(m.cargo)){m.departed=0;save();}
                        }
                    }
                }else if(burning&&!alarm){m.state="SELF_PRESERVATION";Location water=water(m);if(water!=null)navigate(m,water);}
                else {m.state=alarm?"ALARMED":"IDLE";if(owner!=null&&owner.getWorld()==m.position.getWorld()&&!owner.isDead())navigate(m,owner.getLocation());}
                if(mode.equals("ORDINARY")||mode.equals("LOGISTICS"))pickup(m);
            }
            var step=m.burn.tick(burning,fighting&&plugin.combatState().inCombat(m.body),m.body.isInWater());
            if("deadline".equals(m.a2)&&step.sustainedCombat()>=config.getInt("deadlineTicks")){
                Location blast=m.body.getLocation();remove(m,true);blast.getWorld().createExplosion(blast, (float)config.getDouble("deadlinePower"),false,true,owner);continue;
            }
            if("water_break".equals(m.a2)&&step.extinguishedAfter()>0){m.body.setHealth(Math.min(KitAttributes.maxHealth(m.body),m.body.getHealth()+step.extinguishedAfter()*config.getDouble("waterHealPerTick")));m.refreshUntil=clock+config.getInt("refreshTicks");}
        }
        if(clock%config.getInt("checkpointTicks")==0)save();
        if(plugin.supplyLines()!=null)plugin.supplyLines().tick();
    }
    private boolean near(Member m,Location at){return at.getWorld()==m.position.getWorld()&&at.clone().add(.5,.5,.5).distanceSquared(m.mount==null?m.position:m.mount.getLocation())<=Math.pow(config.getDouble("arrivalRadius"),2);}
    private void navigate(Member m,Location at){if(plugin.stun().movementRefused(m.body) || clock%config.getInt("navigationTicks")!=0 || !at.getWorld().isChunkLoaded(at.getBlockX()>>4,at.getBlockZ()>>4))return;(m.mount==null?m.body:m.mount).getPathfinder().moveTo(at,1);}
    private Location water(Member m){
        int r=config.getInt("waterSearchRadius");Block base=m.position.getBlock();Location nearest=null;
        for(int x=-r;x<=r;x++)for(int y=-2;y<=2;y++)for(int z=-r;z<=r;z++){
            Block b=base.getRelative(x,y,z);if(!b.getWorld().isChunkLoaded(b.getX()>>4,b.getZ()>>4)||b.getType()!=Material.WATER)continue;
            Location at=b.getLocation().add(.5,0,.5);if(nearest==null||at.distanceSquared(m.position)<nearest.distanceSquared(m.position))nearest=at;
        }return nearest;
    }
    private void pickup(Member m){
        for(var e:m.body.getNearbyEntities(config.getDouble("pickupRadius"),1,config.getDouble("pickupRadius")))if(e instanceof Item item && item.getPickupDelay()<=0 && (item.getOwner()==null||item.getOwner().equals(m.owner))){
            ItemStack stack=item.getItemStack();int accepted=CargoSlots.accept(m.cargo,stack,m.slots);if(accepted>0){if(accepted==stack.getAmount())item.remove();else item.setItemStack(stack.asQuantity(stack.getAmount()-accepted));m.departed=0;save();}
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void nativePickup(EntityPickupItemEvent e){if(member(e.getEntity())!=null)e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void nativeTarget(EntityTargetLivingEntityEvent e){if(member(e.getEntity())!=null)e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void feed(PlayerInteractEntityEvent e){
        Member m=member(e.getRightClicked());if(m==null||!m.owner.equals(e.getPlayer().getUniqueId())||plugin.inputs().active(e.getPlayer()))return;
        e.setCancelled(true);ItemStack item=e.getPlayer().getInventory().getItem(e.getHand());int accepted=CargoSlots.accept(m.cargo,item,m.slots);
        if(accepted>0){e.getPlayer().getInventory().setItem(e.getHand(),accepted==item.getAmount()?null:item.asQuantity(item.getAmount()-accepted));m.departed=0;save();}
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void death(EntityDeathEvent e){
        Member dead=member(e.getEntity());
        if(dead!=null){e.getDrops().clear();e.setDroppedExp(0);for(var stack:dead.cargo)e.getDrops().add(stack.clone());dead.cargo.clear();members.remove(dead.id);allocation.remove(dead.id);if(dead.mount!=null)dead.mount.remove();if(dead.body!=null&&!dead.body.equals(e.getEntity()))dead.body.remove();save();}
        Member killer=null;var cause=e.getEntity().getLastDamageCause();if(cause instanceof EntityDamageByEntityEvent hit){killer=member(hit.getDamager());if(killer==null&&hit.getDamager() instanceof Projectile shot&&shot.getShooter() instanceof Entity source)killer=member(source);}
        if(killer!=null&&"labored_union".equals(killer.a1)){stock.merge(killer.owner,config.getInt("killCrew"),Integer::sum);save();return;}
        Player player=e.getEntity().getKiller();if(player!=null&&plugin.passives().has(player,"undead_affinity")&&UndeadAffinity.eligible(e.getEntityType())&&!owned(e.getEntity()))grant(player,config.getInt("killCrew"));
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void attacked(EntityDamageByEntityEvent e){Member m=member(e.getEntity());if(m==null||e.getFinalDamage()<=0 || clock<m.alarmUntil)return;LivingEntity attacker=e.getDamager() instanceof LivingEntity living?living:e.getDamager() instanceof Projectile shot&&shot.getShooter() instanceof LivingEntity living?living:null;if(hostile(m,attacker))m.target=attacker;}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void unload(ChunkUnloadEvent e){for(var m:members.values())if(m.body!=null&&m.body.getWorld()==e.getWorld()&&m.body.getLocation().getChunk().equals(e.getChunk())){snapshot(m);m.body.remove();m.body=null;if(m.mount!=null){m.mount.remove();m.mount=null;}}save();}
    private void snapshot(Member m){if(m.body!=null&&m.body.isValid()){m.position=m.mount==null?m.body.getLocation():m.mount.getLocation();m.health=m.body.getHealth();}}
    private void spill(Member m){for(var stack:m.cargo)m.position.getWorld().dropItemNaturally(m.position,stack.clone());m.cargo.clear();}
    private void remove(Member m,boolean spill){snapshot(m);if(spill)spill(m);if(m.body!=null)m.body.remove();if(m.mount!=null)m.mount.remove();members.remove(m.id);allocation.remove(m.id);save();}
    void endShift(UUID owner){shifts.remove(owner);for(var m:List.copyOf(members.values()))if(m.temporary&&m.owner.equals(owner))remove(m,true);}
    void forget(Player p){endShift(p.getUniqueId());if(NmsBodies.isBody(p.getUniqueId()))clearOwner(p.getUniqueId());}
    void clearOwner(UUID owner){unresolved.removeIf(row->owner.toString().equals(row.get("owner")));for(var worker:allocation.workers())if(worker.owner().equals(owner))allocation.remove(worker.id());for(var m:List.copyOf(members.values()))if(m.owner.equals(owner))remove(m,true);shifts.remove(owner);stock.remove(owner);if(plugin.supplyLines()!=null)plugin.supplyLines().removeOwner(owner);save();}
    void reset(){for(var worker:allocation.workers())allocation.remove(worker.id());for(var m:List.copyOf(members.values()))remove(m,false);stock.clear();shifts.clear();unresolved.clear();save();}
    void close(){closing=true;for(var owner:List.copyOf(shifts.keySet()))endShift(owner);for(var m:members.values())snapshot(m);save();for(var m:members.values()){if(m.body!=null){m.body.remove();m.body=null;}if(m.mount!=null){m.mount.remove();m.mount=null;}}}
    private Member find(Player p,String prefix){return members.values().stream().filter(m->m.owner.equals(p.getUniqueId())&&m.id.toString().startsWith(prefix)).findFirst().orElse(null);}
    boolean command(Player p,String[] args){
        String verb=args.length>1?args[1]:"list";
        if(verb.equals("list")||verb.equals("status")){p.sendMessage("Crew stored="+stored(p)+" active="+active(p)+" committed="+(allocation.count(p.getUniqueId())-active(p))+" limit="+config.getInt("limit"));for(var m:members.values())if(m.owner.equals(p.getUniqueId())){var w=allocation.get(m.id);p.sendMessage(shortId(m.id)+" "+(m.temporary?"TEMP "+specialization(p):w!=null&&w.commitment()!=null?"COMMITTED "+SupplyLines.shortId(w.commitment()):m.state)+" cargo="+CargoSlots.used(m.cargo)+"/"+m.slots);}return true;}
        if(args.length<3){p.sendMessage("/moba crew list | source|destination|follow|mobilize <worker> | commit <worker> <line>");return true;}
        Member m=find(p,args[2]);if(m==null){p.sendMessage("No owned worker matches that id.");return true;}
        switch(verb){
            case "source","destination"->{Block b=p.getTargetBlockExact(config.getInt("assignmentRange"));if(!SupplyLines.anchor(b) || b.getWorld()!=m.position.getWorld()){p.sendMessage("Target a Copper Chest in the worker's world.");break;}if(m.temporary){p.sendMessage("The temporary shift takes its assignment from the ordinary workforce.");break;}Location other=verb.equals("source")?m.destination:m.source;if(other!=null&&SupplyLines.overlap(SupplyLines.inventory(other),SupplyLines.inventory(b.getLocation()))){p.sendMessage("Source and Destination must be separate inventories.");break;}if(verb.equals("source"))m.source=b.getLocation();else m.destination=b.getLocation();m.departed=0;m.target=null;save();}
            case "follow"->{m.source=null;m.destination=null;m.departed=0;save();}
            case "commit"->{if(args.length<4){p.sendMessage("Specify an owned proven line.");break;}var line=plugin.supplyLines().find(p.getUniqueId(),args[3]);
                if(m.temporary||line==null||CargoSlots.used(m.cargo)>0||m.body==null||m.slots!=line.ratedSlots()){p.sendMessage("Only an empty ordinary worker of the proven method can commit.");break;}
                if(allocation.commit(m.id,line.id())){snapshot(m);m.body.remove();m.body=null;save();p.sendMessage("Worker committed; physical body withdrawn.");}}
            case "mobilize"->{var w=allocation.get(m.id);if(w==null||w.commitment()==null)break;Location at=near(p.getLocation());if(at==null)break;m.position=at;if(spawn(m)){allocation.withdraw(m.id);save();p.sendMessage("Worker mobilized; line Capacity withdrawn.");}}
            default->p.sendMessage("Unknown workforce action.");
        }return true;
    }
    static String shortId(UUID id){return id.toString().substring(0,8);}
    private Map<String,Object> row(Member m){
        var row=new LinkedHashMap<String,Object>();row.put("id",m.id.toString());row.put("owner",m.owner.toString());row.put("slots",m.slots);row.put("position",SupplyLines.position(m.position));row.put("health",m.health);row.put("cargo",new ArrayList<>(m.cargo));
        row.put("source",m.source==null?null:SupplyLines.position(m.source));row.put("destination",m.destination==null?null:SupplyLines.position(m.destination));row.put("armorCount",m.armorCount);row.put("armorRemaining",Math.max(0,m.armorUntil-clock));row.put("a1",m.a1);row.put("a2",m.a2);var w=allocation.get(m.id);row.put("commitment",w==null?null:w.commitment());return row;
    }
    void save(){if(file==null)return;var y=new YamlConfiguration();y.set("version",1);var stocks=new LinkedHashMap<String,Integer>();stock.forEach((id,amount)->stocks.put(id.toString(),amount));y.set("stock",stocks);List<Map<?,?>> rows=new ArrayList<>(unresolved);for(var m:members.values())if(!m.temporary){snapshot(m);rows.add(row(m));}y.set("members",rows);SupplyLines.write(file,y);}
    private void load(){if(file==null||!Files.exists(file))return;var y=YamlConfiguration.loadConfiguration(file.toFile());if(y.getInt("version")!=1)throw new IllegalStateException("Unsupported Crew state version");var stocks=y.getConfigurationSection("stock");if(stocks!=null)for(var key:stocks.getKeys(false))stock.put(UUID.fromString(key),stocks.getInt(key));unresolved.addAll(y.getMapList("members"));
        for(var row:unresolved){UUID id=UUID.fromString(row.get("id").toString()),owner=UUID.fromString(row.get("owner").toString());int slots=((Number)row.get("slots")).intValue();
            if(!allocation.add(id,owner,slots,Integer.MAX_VALUE))throw new IllegalStateException("Duplicate persisted worker "+id);
            if(row.get("commitment")!=null)allocation.commit(id,row.get("commitment").toString());}
        resolve();}
    private void resolve(){for(var row:List.copyOf(unresolved)){
        Location at=SupplyLines.position((Map<?,?>)row.get("position"));if(at==null)continue;
        UUID id=UUID.fromString(row.get("id").toString()),owner=UUID.fromString(row.get("owner").toString());int slots=((Number)row.get("slots")).intValue();
        Member m=new Member(id,owner,slots,at,false,false,((Number)row.get("health")).doubleValue());
        if(row.get("source") instanceof Map<?,?> source)m.source=SupplyLines.position(source);if(row.get("destination") instanceof Map<?,?> dest)m.destination=SupplyLines.position(dest);
        for(var item:(List<?>)row.get("cargo"))m.cargo.add((ItemStack)item);m.armorCount=((Number)row.get("armorCount")).intValue();m.armorUntil=clock+((Number)row.get("armorRemaining")).intValue();m.a1=row.get("a1").toString();m.a2=row.get("a2").toString();
        members.put(id,m);unresolved.remove(row);
    }}
}
