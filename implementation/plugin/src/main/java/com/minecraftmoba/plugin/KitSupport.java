package com.minecraftmoba.plugin;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;
import java.util.*;
/** Common units, live class checks and bounded movement used by lab kit implementations. */
final class KitSupport {
 private KitSupport(){}
 static boolean has(MobaPlugin p,Player player,String id){return player!=null&&player.isOnline()&&!player.isDead()&&p.enrolled(player)&&id.equals(p.data(player).classId);}
 static double heal(MobaPlugin p,LivingEntity target,double effective){double amount=target instanceof Player player&&p.getConfig().getBoolean("features.vitalsScaling.enabled")?Vitals.scaleHealing(effective,p.effectiveMaxHealth(player)):effective;double before=target.getHealth();target.setHealth(Math.min(KitAttributes.maxHealth(target),before+amount));return target.getHealth()-before;}
 static String branch(MobaPlugin p,Player player,String id){var data=p.data(player);if(data==null)return "base";return data.classState.getOrDefault("branch."+id,p.inputs().definition(data.classId).branchFor(id));}
 static void potion(LivingEntity target,PotionEffectType type,int ticks,int level){target.addPotionEffect(new PotionEffect(type,ticks,level,true,false,true));}
 static boolean loaded(Location at){return at.getWorld()!=null&&Bukkit.getWorld(at.getWorld().getUID())==at.getWorld()&&at.getWorld().isChunkLoaded(at.getBlockX()>>4,at.getBlockZ()>>4);}
 static boolean standing(Location at){return loaded(at)&&at.getBlock().isPassable()&&at.getBlock().getRelative(0,1,0).isPassable()&&at.getBlock().getRelative(0,-1,0).getType().isSolid();}
 static Location safe(Location at,int radius){for(int y=0;y<=3;y++)for(int r=0;r<=radius;r++)for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++){Location test=at.clone().add(x,y,z);if(standing(test))return test;}return null;}
 static void push(MobaPlugin plugin,LivingEntity entity,Vector velocity){if(!plugin.stun().isStunned(entity))entity.setVelocity(velocity);}
 static List<LivingEntity> enemies(Player player,Location centre,double radius){return AbilityTargeting.within(player,centre,radius,radius,radius,AbilityTargeting.Relation.ENEMY,false);}
}
