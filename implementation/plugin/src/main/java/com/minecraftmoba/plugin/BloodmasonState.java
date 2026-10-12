package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import java.util.*;

/** Anatomy is real, destructible terrain; every temporary cell has a restoration lease. */
final class BloodmasonState implements Listener {
    static final class Anatomy {
        final Player owner;
        final UUID token = UUID.randomUUID();
        final Location origin;
        final String kind, branch;
        final int manifestation, until;
        final Map<BlockAddress, Material> cells = new LinkedHashMap<>();
        final Map<UUID, Integer> exposure = new HashMap<>();
        final Set<UUID> healed = new HashSet<>();
        final List<FallingBlock> teeth = new ArrayList<>();
        int rise, nextRise, completion, channelUntil;
        String channel;
        boolean complete;
        Anatomy(Player p, Location at, String kind, String branch, int value, int until) {
            owner=p;origin=at;this.kind=kind;this.branch=branch;manifestation=value;this.until=until;
        }
    }
    private final MobaPlugin plugin;
    private final ConfigurationSection cfg;
    private int clock;
    private final Map<UUID,Hemostasis> states = new HashMap<>();
    private final List<Anatomy> anatomy = new ArrayList<>();
    private final Map<UUID, Map<BlockAddress,Integer>> trails = new HashMap<>();
    BloodmasonState(MobaPlugin plugin) { this.plugin=plugin;cfg=plugin.getConfig().getConfigurationSection("bloodmason"); }
    private boolean has(Player p) { return KitSupport.has(plugin,p,"bloodmason") && plugin.passives().has(p,"hemostasis"); }
    Hemostasis state(Player p) { return states.computeIfAbsent(p.getUniqueId(), id -> new Hemostasis(Integer.parseInt(plugin.data(p).classState.getOrDefault("hemostasis","3")))); }
    private int interval(Player p) { int level=plugin.data(p).level;return (level>=25?3:level>=15?4:5)*20; }
    private int cast(Player p, int delta) {
        var result=state(p).cast(delta,clock,interval(p),cfg.getInt("pathologyTicks"));
        plugin.data(p).classState.put("hemostasis",Integer.toString(state(p).value()));
        if(result.endpoint()) { plugin.stun().stun(p,cfg.getInt("endpointStunTicks"));if(result.relapse())AbilityDamage.deal(p,cfg.getDouble("relapseDamage"),p); }
        return result.manifestation();
    }
    Anatomy tomb(Player p) { return anatomy.stream().filter(a->a.owner.equals(p)&&a.kind.equals("tomb")).findFirst().orElse(null); }
    boolean channel(Player p,String kind) { Anatomy a=tomb(p);if(a==null)return false;a.channel=kind;a.channelUntil=clock+8;return true; }
    void cancelChannel(Player p) { Anatomy a=tomb(p);if(a!=null){a.channel=null;a.channelUntil=0;} }
    boolean flow(Player p,Location at,String branch) {
        if(tomb(p)!=null)return channel(p,"blood");
        if(!KitSupport.loaded(at))return false;
        int nominal=cast(p,-1), value=nominal;
        if(branch.equals("bloodtrail")&&trails.getOrDefault(p.getUniqueId(),Map.of()).getOrDefault(BlockAddress.of(at.getBlock()),0)>clock)value=Math.max(0,value-1);
        Anatomy a=new Anatomy(p,at.getBlock().getLocation(),"flow",branch,value,clock+cfg.getInt(value<=1?"fastFlowTicks":"flowTicks"));
        a.nextRise=clock+cfg.getInt(value==1?"fastRiseInterval":"riseInterval");
        anatomy.add(a);buildFlow(a);return true;
    }
    private void place(Anatomy a,Block b,Material material) {
        if(!KitSupport.loaded(b.getLocation()))return;
        if(a.cells.containsKey(BlockAddress.of(b))&&plugin.displacement().isReserved(b)) { if(b.isEmpty())b.setType(material,false);return; }
        if(plugin.displacement().placeDestructible(b,a.token,material.createBlockData()))a.cells.put(BlockAddress.of(b),material);
    }
    private void buildFlow(Anatomy a) {
        int radius=a.manifestation<=2?1:0;
        int height=a.manifestation==0?10:4;
        for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)for(int y=0;y<height;y++) {
            Block b=a.origin.getBlock().getRelative(x,a.rise+y,z);
            boolean bloodFloor=anatomy.stream().anyMatch(other->other.kind.equals("wall")&&other.manifestation==2&&other.cells.containsKey(BlockAddress.of(b)));
            if(bloodFloor)continue;
            if(!b.isEmpty()&&!a.cells.containsKey(BlockAddress.of(b)))break;
            // Powder snow supplies a permeable, native collision volume for circulation.
            place(a,b,Material.POWDER_SNOW);
            if(a.branch.equals("bloodtrail"))trails.computeIfAbsent(a.owner.getUniqueId(),k->new HashMap<>()).put(BlockAddress.of(b),clock+cfg.getInt("trailTicks"));
        }
    }
    boolean wall(Player p,Location at,String branch) {
        if(tomb(p)!=null)return channel(p,"bone");
        if(!KitSupport.loaded(at))return false;
        int value=cast(p,1);Anatomy a=new Anatomy(p,at.getBlock().getLocation(),"wall",branch,value,clock+cfg.getInt("wallTicks"));
        anatomy.add(a);
        int radius=value>=5?4:value==4?3:2,height=value==2?1:value==3?3:5;
        Vector facing=p.getLocation().getDirection().setY(0).normalize();Vector side=new Vector(-facing.getZ(),0,facing.getX());
        for(int x=-radius;x<=radius;x++)for(int y=0;y<height;y++) {
            if(value==5&&Math.abs(x)%3==1&&y<3)continue; // deliberate walkable choke points
            int curvature=value>=4?(int)Math.round(x*x/(double)Math.max(1,radius*2)):0;
            if(value==6)curvature=-curvature;
            int fracture=branch.equals("fracture")?Integer.signum(x)*(Math.abs(x)/2+1):0;
            Location point=a.origin.clone().add(side.clone().multiply(x+fracture)).add(facing.clone().multiply(curvature)).add(0,y,0);
            Material material=value==2?Material.POWDER_SNOW:y==0||x%2==0?Material.BONE_BLOCK:branch.equals("adipose")?Material.WHITE_WOOL:Material.RED_WOOL;
            if(branch.equals("render_flesh")&&material==Material.RED_WOOL)material=Material.MAGMA_BLOCK;
            place(a,point.getBlock(),material);
        }
        if(value==6) {
            for(int x=-radius;x<=radius;x+=2) {
                Location tooth=a.origin.clone().add(side.clone().multiply(x)).add(0,height,0);
                var falling=p.getWorld().spawnFallingBlock(tooth,Material.POINTED_DRIPSTONE.createBlockData());falling.setDropItem(false);falling.setHurtEntities(true);falling.setPersistent(false);OwnedEntities.tag(falling,p);a.teeth.add(falling);
                place(a,tooth.clone().add(0,-height,0).getBlock(),Material.POINTED_DRIPSTONE);
            }
            if(branch.equals("render_flesh"))place(a,a.origin.getBlock().getRelative(0,1,0),Material.LAVA);
        }
        return true;
    }
    boolean anatomb(Player p) {
        if(tomb(p)!=null)return false;
        Location at=p.getLocation().getBlock().getLocation();int stages=0;
        for(int stage=1;stage<=cfg.getInt("maxStages");stage++) {
            int radius=stage+2,top=stage*4;boolean fits=true;
            for(int x=-radius;x<=radius&&fits;x++)for(int z=-radius;z<=radius&&fits;z++)for(int y=(stage-1)*4;y<=top;y++) {
                Block b=at.getBlock().getRelative(x,y,z);
                if(!KitSupport.loaded(b.getLocation())||!b.isEmpty()||plugin.displacement().isReserved(b)){fits=false;break;}
            }
            if(!fits)break;stages=stage;
        }
        if(stages==0)return false;
        Anatomy a=new Anatomy(p,at,"tomb","base",stages,clock+cfg.getInt("anatombTicks"));anatomy.add(a);plugin.kitGlow().acquire(p,"anatomb");
        for(int stage=1;stage<=stages;stage++) {
            int radius=stage+2;
            for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)for(int y=(stage-1)*4;y<=stage*4;y++) {
                boolean boundary=Math.abs(x)==radius||Math.abs(z)==radius;
                boolean floor=y==(stage-1)*4 && !(Math.abs(x)<=1&&Math.abs(z)<=1);
                if(boundary||floor)place(a,at.getBlock().getRelative(x,y,z),Material.RED_WOOL);
            }
        }
        return true;
    }
    private boolean inside(Anatomy a,Location at) {
        if(at.getWorld()!=a.origin.getWorld())return false;Vector delta=at.toVector().subtract(a.origin.toVector());
        if(a.kind.equals("tomb")) {int stage=Math.max(1,Math.min(a.manifestation,(int)Math.floor(delta.getY()/4)+1));return delta.getY()>=0&&delta.getY()<=a.manifestation*4&&Math.abs(delta.getX())<=stage+2&&Math.abs(delta.getZ())<=stage+2;}
        return a.cells.keySet().stream().anyMatch(k->Math.abs(k.x()-at.getX())<1.3&&Math.abs(k.y()-at.getY())<1.8&&Math.abs(k.z()-at.getZ())<1.3);
    }
    void tick() {
        clock++;if(plugin.inputs()==null)return;
        for(Player p:Bukkit.getOnlinePlayers())if(has(p)&&plugin.data(p).level>=5)state(p).tick(clock,interval(p));
        for(var a:List.copyOf(anatomy)) {
            if(clock>=a.until||!has(a.owner)){remove(a);continue;}
            if(a.kind.equals("flow")) {
                if(a.manifestation>0&&a.manifestation<4&&a.rise<3&&clock>=a.nextRise) {
                    plugin.displacement().restore(a.token);a.cells.clear();a.rise++;a.nextRise=clock+cfg.getInt(a.manifestation==1?"fastRiseInterval":"riseInterval");buildFlow(a);
                }
                for(LivingEntity e:AbilityTargeting.within(a.owner,a.origin,8,16,8,AbilityTargeting.Relation.EITHER,true))if(inside(a,e.getLocation())) {
                    KitSupport.push(plugin,e,new Vector(0,a.manifestation==4?-.12:a.manifestation<=1?.7:.3,0));
                    boolean ally=e.equals(a.owner)||Targetability.allied(e,a.owner);
                    if(!ally&&a.manifestation==0&&clock%10==0)AbilityDamage.deal(e,cfg.getDouble("eruptionDamage"),a.owner);
                    if(!ally&&a.branch.equals("nausea_wave")){KitSupport.potion(e,PotionEffectType.SLOWNESS,12,0);if(e instanceof Player p&&clock%20==0)p.setFoodLevel(Math.max(0,p.getFoodLevel()-1));}
                    if(ally&&a.branch.equals("lifeblood")&&!a.healed.contains(e.getUniqueId())&&a.exposure.merge(e.getUniqueId(),1,Integer::sum)>=cfg.getInt("lifebloodExposure")){KitSupport.heal(plugin,e,cfg.getDouble("lifebloodHeal"));a.healed.add(e.getUniqueId());}
                }
            } else if(a.kind.equals("wall")&&a.branch.equals("adipose")) {
                for(var enemy:KitSupport.enemies(a.owner,a.origin,10))if(inside(a,enemy.getLocation()))plugin.toolboxStatuses().root(enemy,cfg.getInt("adiposeRootTicks"));
            } else if(a.kind.equals("tomb")) {
                KitSupport.potion(a.owner,PotionEffectType.LEVITATION,6,0);
                if(a.channel!=null&&clock<=a.channelUntil&&clock%10==0&&!plugin.stun().isStunned(a.owner)&&!plugin.forced().refusesAbilities(a.owner)) {
                    AbilityDamage.deal(a.owner,cfg.getDouble("channelHealthCost"),a.owner);
                    if(a.owner.isDead()){remove(a);continue;}cast(a.owner,a.channel.equals("blood")?-1:1);a.completion++;
                    if(a.channel.equals("blood")) {
                        for(var enemy:KitSupport.enemies(a.owner,a.origin,20))if(inside(a,enemy.getLocation())){AbilityDamage.deal(enemy,cfg.getDouble("hemorrhageDamage"),a.owner);if(enemy instanceof Player player)player.setFoodLevel(Math.max(0,player.getFoodLevel()-1));}
                        for(var e:AbilityTargeting.within(a.owner,a.origin,20,20,20,AbilityTargeting.Relation.EITHER,true))if(inside(a,e.getLocation()))KitSupport.push(plugin,e,new Vector(0,.65,0));
                    } else {
                        for(var cell:a.cells.entrySet())if(cell.getKey().block()!=null&&cell.getKey().block().isEmpty())cell.getKey().block().setType(cell.getValue(),false);
                        int inset=Math.min(2,a.completion/3);
                        for(int stage=1;stage<=a.manifestation;stage++)for(int y=(stage-1)*4+1;y<stage*4;y++)for(int z=-stage-1;z<=stage+1;z++)for(int sign:new int[]{-1,1})place(a,a.origin.getBlock().getRelative(sign*(stage+2-inset),y,z),y%2==0?Material.BONE_BLOCK:Material.RED_WOOL);
                    }
                    if(!a.complete&&a.completion>=cfg.getInt("completionPulses")){a.complete=true;double missing=1-a.owner.getHealth()/KitAttributes.maxHealth(a.owner);int duration=cfg.getInt("completionStunMin")+(int)(missing*cfg.getInt("completionStunMissing"));for(var enemy:KitSupport.enemies(a.owner,a.origin,20))if(inside(a,enemy.getLocation()))plugin.stun().stun(enemy,duration);}
                }
            }
        }
        if(clock%10==0)for(var entry:trails.entrySet()){Player p=Bukkit.getPlayer(entry.getKey());entry.getValue().entrySet().removeIf(e->e.getValue()<=clock);if(p!=null)for(var cell:entry.getValue().keySet())if(cell.block()!=null)p.spawnParticle(Particle.DUST,cell.block().getLocation().add(.5,.5,.5),1,new Particle.DustOptions(Color.RED,1));}
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void damage(EntityDamageEvent e) {
        if(e.getFinalDamage()<=0||!(e.getEntity() instanceof LivingEntity victim))return;
        for(var a:List.copyOf(anatomy))if(a.kind.equals("tomb")&&inside(a,victim.getLocation())) {
            if(!victim.equals(a.owner)&&Targetability.allied(victim,a.owner)) {
                var cell=a.cells.keySet().stream().filter(k->k.block()!=null&&!k.block().isEmpty()).findFirst().orElse(null);
                if(cell!=null){cell.block().setType(Material.AIR,false);KitSupport.heal(plugin,victim,cfg.getDouble("protectionHeal"));}
            } else if(!victim.equals(a.owner)&&Targetability.status(victim,a.owner))KitSupport.heal(plugin,a.owner,cfg.getDouble("metabolicHeal"));
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void teeth(org.bukkit.event.entity.EntityChangeBlockEvent e) {if(anatomy.stream().anyMatch(a->a.teeth.contains(e.getEntity()))){e.setCancelled(true);e.getEntity().remove();}}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void knockback(io.papermc.paper.event.entity.EntityKnockbackEvent e) {
        if(e.getEntity() instanceof Player p && has(p))e.setKnockback(e.getKnockback().multiply(1+(3-state(p).value())*.12));
    }
    private void remove(Anatomy a) {a.teeth.forEach(Entity::remove);if(a.kind.equals("tomb"))plugin.kitGlow().release(a.owner,"anatomb");plugin.displacement().restore(a.token);anatomy.remove(a);}
    void forget(Player p) {for(var a:List.copyOf(anatomy))if(a.owner.equals(p))remove(a);Hemostasis h=states.remove(p.getUniqueId());if(h!=null&&plugin.data(p)!=null)plugin.data(p).classState.put("hemostasis",Integer.toString(h.value()));trails.remove(p.getUniqueId());}
    List<String> hud(Player p) {Anatomy a=tomb(p);return a==null?List.of("Hemostasis "+state(p).value()+"/6"):List.of("Hemostasis "+state(p).value()+"/6","Anatomb "+a.completion+"/"+cfg.getInt("completionPulses"));}
    void close() {for(var a:List.copyOf(anatomy))remove(a);states.clear();trails.clear();}
}
