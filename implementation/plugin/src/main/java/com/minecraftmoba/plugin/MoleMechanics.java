package com.minecraftmoba.plugin;
import org.bukkit.*;import org.bukkit.entity.*;import org.bukkit.event.*;import org.bukkit.event.player.*;import java.util.*;
/** Minimal Mole A2 state; calibration values are provisional. */
public final class MoleMechanics implements Listener{
 private final MobaPlugin plugin;private final Set<UUID> burrowed=new HashSet<>();private final Map<UUID,Location> entries=new HashMap<>();
 public MoleMechanics(MobaPlugin p){plugin=p;}private boolean mole(Player p){return plugin.enrolled(p)&&"mole".equals(plugin.data(p).classId);}
 public boolean drillRush(Player p){if(!mole(p))return false;if(!burrowed.add(p.getUniqueId())){Location at=p.getLocation().clone().add(p.getLocation().getDirection().normalize().multiply(2));if(!at.getBlock().isSolid())return false;p.teleport(at);for(Entity e:at.getWorld().getNearbyEntities(at,2,2,2))if(e instanceof LivingEntity t&&!t.equals(p))t.damage(plugin.getConfig().getDouble("abilities.mole.drillDamage",4),p);burrowed.remove(p.getUniqueId());entries.remove(p.getUniqueId());return true;}entries.put(p.getUniqueId(),p.getLocation().clone());p.setInvulnerable(true);return true;}
 public boolean isBurrowed(Player p){return burrowed.contains(p.getUniqueId());}public void clear(Player p){burrowed.remove(p.getUniqueId());entries.remove(p.getUniqueId());p.setInvulnerable(false);}
 @EventHandler public void quit(PlayerQuitEvent e){clear(e.getPlayer());}
}
