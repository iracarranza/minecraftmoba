package com.minecraftmoba.plugin;
import org.bukkit.entity.*;import org.bukkit.event.*;import org.bukkit.event.player.*;import org.bukkit.util.Vector;import org.bukkit.scheduler.BukkitRunnable;import java.util.*;
/** Shared Daredevil momentum and A1 runtime. Values are provisional calibration. */
public final class DaredevilMechanics implements Listener {
 private final MobaPlugin plugin; private final Map<UUID,Vector> momentum=new HashMap<>(); private final Map<UUID,Integer> airborne=new HashMap<>();
 public DaredevilMechanics(MobaPlugin p){plugin=p;}
 public double momentum(Player p){return momentum.getOrDefault(p.getUniqueId(),p.getVelocity()).clone().setY(0).length();}
 public static boolean qualifies(double forwardMomentum,double threshold){return forwardMomentum>=threshold;}
 public static Vector launch(Vector travel,Vector facing,double horizontal,double upward){Vector d=facing.clone().setY(0).normalize();if(d.lengthSquared()==0)d=travel.clone().setY(0).normalize();return d.multiply(travel.clone().setY(0).length()*horizontal).setY(upward);}
 public int airborneTicks(Player p){return airborne.getOrDefault(p.getUniqueId(),0);}
 public boolean runway(Player p){if(!plugin.enrolled(p)||!"daredevil".equals(plugin.data(p).classId)||!qualifies(momentum(p),plugin.getConfig().getDouble("abilities.daredevil.runwayMomentum",0.35)))return false;Vector v=momentum.getOrDefault(p.getUniqueId(),p.getVelocity()).clone();String b=plugin.getConfig().getString("abilities.daredevil.branch","base");double scale="pop_rocket".equals(b)?1.8:1.0;Vector launch=launch(v,p.getLocation().getDirection(),scale,plugin.getConfig().getDouble("abilities.daredevil.runwayUpward",0.8)*scale);p.setVelocity(launch);if("trampoline".equals(b))p.setVelocity(launch.multiply(1.25));if("suplex".equals(b)){var hit=p.getTargetEntity(2);if(hit instanceof LivingEntity e)e.setVelocity(launch); }return true;}
 @EventHandler public void move(PlayerMoveEvent e){Player p=e.getPlayer();if(!plugin.enrolled(p)||!"daredevil".equals(plugin.data(p).classId))return;momentum.put(p.getUniqueId(),e.getTo().toVector().subtract(e.getFrom().toVector()));if(p.getLocation().getY()>e.getTo().getWorld().getHighestBlockYAt(p.getLocation())+1)airborne.merge(p.getUniqueId(),1,Integer::sum);else airborne.put(p.getUniqueId(),0);if(airborneTicks(p)>=plugin.getConfig().getInt("abilities.daredevil.skydiverTicks",20))p.setVelocity(p.getVelocity().multiply(plugin.getConfig().getDouble("abilities.daredevil.skydiverMultiplier",1.25)));}
 @EventHandler public void quit(PlayerQuitEvent e){clear(e.getPlayer());}public void clear(Player p){momentum.remove(p.getUniqueId());airborne.remove(p.getUniqueId());}
}
