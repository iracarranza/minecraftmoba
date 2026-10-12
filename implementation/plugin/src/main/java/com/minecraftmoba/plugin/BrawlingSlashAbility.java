package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import java.util.*;

/** One weapon-scaled hit, then homing entity-slip or a committed ballistic crush. */
final class BrawlingSlashAbility implements Ability {
    private static final class Leap {
        final Player player;final Player target;final Lycanthropy.Form form;final String branch;
        final double damage;final Vector direction;final int start;final boolean collidable;
        final Set<UUID> hit=new HashSet<>();final Set<LivingEntity> pulled=new HashSet<>();
        int flight,slamAt;Vector velocity;Location last;
        Leap(Player p,Player target,Lycanthropy.Form form,String branch,double damage,Vector direction,int start){
            player=p;this.target=target;this.form=form;this.branch=branch;this.damage=damage;this.direction=direction;this.start=start;
            collidable=p.isCollidable();last=p.getLocation();velocity=direction.clone().multiply(.6).setY(.85);
        }
    }
    private final MobaPlugin plugin;private final ConfigurationSection config;private final WerewolfState state;
    private final Map<UUID,Leap> leaps=new HashMap<>();private int clock;
    BrawlingSlashAbility(MobaPlugin plugin,ConfigurationSection config,WerewolfState state){this.plugin=plugin;this.config=config;this.state=state;}
    public String id(){return "brawling_slash";}
    public String displayName(){return "Brawling Slash";}
    public long cooldownTicks(){return config.getLong("cooldownTicks");}
    public List<AbilityOutput> outputs(){return AbilityOutput.single("brawling_slash", TargetForm.DIRECTION, InputForm.INSTANT, Recipients.ENEMIES);}
    public Map<String,String> branches(){return Map.of("whirling_slash","Whirling Slash","gnashing_slash","Gnashing Slash","lumbering_crash","Lumbering Crash");}
    public List<String> branchIds(){return List.of("whirling_slash","gnashing_slash","lumbering_crash");}
    public boolean execute(Player p,AbilityContext ctx){
        if(leaps.containsKey(p.getUniqueId()))return false;
        var form=state.form(p);Vector direction=p.getEyeLocation().getDirection().normalize();double damage=WerewolfState.weaponDamage(p)*1.05;
        var ray=AbilityTargeting.ray(p,p.getEyeLocation(),direction,config.getDouble("range"),config.getDouble("rayWidth"),AbilityTargeting.Relation.ENEMY);
        LivingEntity first=ray!=null?ray.entity():null;
        if(form!=Lycanthropy.Form.UNLEASHED && first==null)return false;
        p.swingMainHand();Player target=first instanceof Player victim?victim:null;
        if(form==Lycanthropy.Form.UNLEASHED)area(p,p.getLocation(),direction,damage,config.getDouble("sweepRadius"),config.getDouble("sweepConeDot"));
        else {AbilityDamage.deal(first,damage,p);first.setVelocity(direction.clone().multiply(config.getDouble("knockback")).setY(.25));}
        if(form==Lycanthropy.Form.UNLEASHED || target!=null){
            int delay=form==Lycanthropy.Form.UNLEASHED?config.getInt("unleashedDelayTicks"):config.getInt("delayTicks");
            leaps.put(p.getUniqueId(),new Leap(p,target,form,ctx.branchFor(id()),damage,direction,clock+delay));
        }
        return true;
    }
    public void tick(){clock++;for(var leap:List.copyOf(leaps.values())){
        Player p=leap.player;
        if(!p.isOnline() || p.isDead() || !p.getWorld().equals(leap.last.getWorld()) || plugin.stun().isStunned(p) || plugin.toolboxStatuses().isRooted(p)){finish(leap);continue;}
        if(clock<leap.start)continue;
        if(leap.slamAt>0){if(clock>=leap.slamAt){area(p,p.getLocation(),leap.direction,leap.damage*config.getDouble("thirdSlamMultiplier"),config.getDouble("thirdSlamRadius"),config.getDouble("thirdSlamConeDot"));finish(leap);}continue;}
        if(leap.flight++==0)p.setCollidable(false);
        if(leap.flight>config.getInt("travelTicks")){land(leap);continue;}
        Vector delta;
        if(leap.form==Lycanthropy.Form.UNLEASHED){delta=leap.velocity.clone();leap.velocity.setY(leap.velocity.getY()-.08);}
        else {
            if(leap.target==null || leap.target.isDead() || !leap.target.isOnline() || !p.getWorld().equals(leap.target.getWorld()) || !Targetability.status(leap.target,p)){finish(leap);continue;}
            delta=leap.target.getLocation().toVector().subtract(p.getLocation().toVector());
            if(delta.lengthSquared()<.8){finish(leap);continue;}
            delta.normalize().multiply(config.getDouble("travelSpeed"));
        }
        Location at=p.getLocation();int steps=Math.max(1,(int)Math.ceil(delta.length()/.2));Vector step=delta.clone().multiply(1.0/steps);boolean blocked=false;
        for(int i=0;i<steps;i++){
            Location next=at.clone().add(step);
            if(!safe(p,next)){blocked=true;break;}
            at=next;
            if(leap.form!=Lycanthropy.Form.UNLEASHED)pathHits(leap,at);
        }
        at.setDirection(leap.form==Lycanthropy.Form.UNLEASHED?leap.direction:delta);
        if(!p.teleport(at)){finish(leap);continue;}p.setVelocity(new Vector());leap.last=at;
        p.getWorld().spawnParticle(Particle.SWEEP_ATTACK,at.clone().add(0,1,0),1);
        for(var target:leap.pulled)if(target.isValid() && target.getWorld().equals(p.getWorld())) {
            Vector pull=at.toVector().subtract(target.getLocation().toVector());
            double distance=pull.length();if(distance>.01)target.setVelocity(pull.normalize().multiply(Math.min(distance,config.getDouble("pullSpeed"))));
        }
        if(blocked){if(leap.form==Lycanthropy.Form.UNLEASHED && delta.getY()<0)land(leap);else finish(leap);}
    }}
    private boolean safe(Player p,Location next){
        if(!next.getChunk().isLoaded() || next.getY()<next.getWorld().getMinHeight() || next.getY()+p.getHeight()>=next.getWorld().getMaxHeight())return false;
        var box=p.getBoundingBox().clone().shift(next.toVector().subtract(p.getLocation().toVector()));
        for(int x=(int)Math.floor(box.getMinX());x<=Math.floor(box.getMaxX());x++)for(int y=(int)Math.floor(box.getMinY());y<=Math.floor(box.getMaxY());y++)for(int z=(int)Math.floor(box.getMinZ());z<=Math.floor(box.getMaxZ());z++){
            Block b=next.getWorld().getBlockAt(x,y,z);if(!b.isPassable() && box.overlaps(b.getBoundingBox()))return false;
        }return true;
    }
    private void pathHits(Leap leap,Location at){
        if(leap.form==Lycanthropy.Form.HUMAN && !"whirling_slash".equals(leap.branch))return;
        for(var enemy:AbilityTargeting.within(leap.player,at,config.getDouble("pathRadius"),config.getDouble("pathHeight"),config.getDouble("pathRadius"),AbilityTargeting.Relation.ENEMY,false))if(leap.hit.add(enemy.getUniqueId())){
            AbilityDamage.deal(enemy,leap.damage*(leap.form==Lycanthropy.Form.HUMAN?config.getDouble("whirlingMultiplier"):1),leap.player);
            if(leap.form==Lycanthropy.Form.TRANSFORMED){if("gnashing_slash".equals(leap.branch))leap.pulled.add(enemy);else enemy.setVelocity(leap.direction.clone().multiply(.7).setY(.2));}
        }
    }
    private void land(Leap leap){
        if(leap.form!=Lycanthropy.Form.UNLEASHED){finish(leap);return;}
        area(leap.player,leap.player.getLocation(),leap.direction,leap.damage*config.getDouble("crushMultiplier"),config.getDouble("crushRadius"),-1);
        if("lumbering_crash".equals(leap.branch)){leap.slamAt=clock+config.getInt("thirdSlamDelayTicks");leap.player.setCollidable(leap.collidable);}else finish(leap);
    }
    private void area(Player p,Location at,Vector facing,double damage,double radius,double coneDot){
        Vector flat=facing.clone().setY(0);if(flat.lengthSquared()>0)flat.normalize();
        for(var target:AbilityTargeting.within(p,at,radius,config.getDouble("areaHeight"),radius,AbilityTargeting.Relation.ENEMY,false)){
            Vector offset=target.getLocation().toVector().subtract(at.toVector());
            if(!AbilityTargeting.inCone(offset.getX(),offset.getZ(),flat.getX(),flat.getZ(),radius,coneDot))continue;
            AbilityDamage.deal(target,damage,p);target.setVelocity(flat.clone().multiply(.7).setY(.3));
        }
        at.getWorld().spawnParticle(Particle.SWEEP_ATTACK,at.clone().add(0,.4,0),10,radius/2,.2,radius/2,0);
    }
    private void finish(Leap leap){
        leaps.remove(leap.player.getUniqueId());leap.player.setCollidable(leap.collidable);
        for(var target:leap.pulled)if(target.isValid() && target.getWorld().equals(leap.player.getWorld())){
            Vector delta=leap.player.getLocation().toVector().subtract(target.getLocation().toVector());
            if(delta.lengthSquared()>0)target.setVelocity(delta.normalize().multiply(config.getDouble("pullSpeed")));
        }
    }
    public void close(){for(var leap:List.copyOf(leaps.values()))finish(leap);}
    public void cancel(Player p){var leap=leaps.get(p.getUniqueId());if(leap!=null)finish(leap);}
}
