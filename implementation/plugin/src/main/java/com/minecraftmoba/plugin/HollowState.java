package com.minecraftmoba.plugin;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import java.util.*;

/** One body, one external life, with contextual transformations sharing that life. */
final class HollowState implements Listener {
 static final class State {
  final Player player;final HeartLife life;Slime heart;BlockDisplay visual;Location at;String mode="FOLLOW",mawBranch="base",seekBranch="base";
  int mawUntil,seekUntil,emergeAt,thrashAt,routeIndex,peekUntil;boolean heartlessSeek;Vector direction;LivingEntity priority,carried;Location wooden;
  final Set<UUID> attackers=new HashSet<>();final List<Location> path=new ArrayList<>();final Map<UUID,Long> frightSeen=new HashMap<>();
  State(Player p,double maximum){player=p;life=new HeartLife(maximum);at=p.getLocation().clone().add(1,0,0);}
 }
 private final MobaPlugin plugin;private final ConfigurationSection cfg;private final Map<UUID,State> states=new HashMap<>();private int clock;
 HollowState(MobaPlugin p){plugin=p;cfg=p.getConfig().getConfigurationSection("hollow");}
 State state(Player p){return states.computeIfAbsent(p.getUniqueId(),id->{var s=new State(p,plugin.effectiveMaxHealth(p));var d=plugin.data(p);if(d!=null&&d.classState.containsKey("hollow.heart")){double health=Double.parseDouble(d.classState.get("hollow.heart"));if(health<=0)s.life.absorb(s.life.maximum(),Integer.parseInt(d.classState.getOrDefault("hollow.return","800")));else s.life.absorb(Math.max(0,s.life.maximum()-health),returnTicks(p));}return s;});}
 private int returnTicks(Player p){int level=plugin.data(p).level;return (level>=24?16:level>=16?24:level>=8?32:40)*20;}
 private void spawn(State s){if(s.heart!=null||!s.life.intact()||!KitSupport.loaded(s.at))return;
  s.heart=s.at.getWorld().spawn(s.at,Slime.class,e->{e.setSize(1);e.setAI(false);e.setGravity(false);e.setSilent(true);e.setInvisible(true);e.setPersistent(false);OwnedEntities.tag(e,s.player);});
  s.visual=s.at.getWorld().spawn(s.at,BlockDisplay.class,e->{e.setBlock(Material.CREAKING_HEART.createBlockData());e.setPersistent(false);});label(s);
 }
 private void label(State s){if(s.heart!=null){s.heart.customName(Component.text("Hollow Heart "+Math.ceil(s.life.health()*100)+" / "+Math.ceil(s.life.maximum()*100)));s.heart.setCustomNameVisible(true);}}
 private void hideHeart(State s){if(s.heart!=null){s.heart.remove();s.heart=null;}if(s.visual!=null){s.visual.remove();s.visual=null;}}
 private void moveHeart(State s,Location at){if(!KitSupport.loaded(at))return;s.at=at.clone();if(s.heart!=null)s.heart.teleport(at);if(s.visual!=null)s.visual.teleport(at.clone().add(-.5,0,-.5));}
 boolean maw(Player p,String branch){var s=state(p);if(clock<s.mawUntil){endMaw(s);return true;}s.mawBranch=branch;s.mawUntil=clock+cfg.getInt("mawTicks");s.mode="MAW";s.path.clear();s.attackers.clear();s.carried=null;moveHeart(s,p.getLocation());hideHeart(s);return true;}
 private void endMaw(State s){s.mawUntil=0;s.carried=null;s.mode="PARKED";s.at=s.player.getLocation().clone();KitAttributes.set(s.player,Attribute.SCALE,new NamespacedKey(plugin,"hollow_maw"),0,AttributeModifier.Operation.ADD_SCALAR);spawn(s);}
 boolean seek(Player p,String branch){var s=state(p);if(s.mode.equals("SEEK")||s.mode.equals("WOOD")||s.mode.equals("HEARTLESS_SEEK")){
   if(branch.equals("drawn_together")&&s.life.intact()){s.mode="RETURN";s.routeIndex=s.path.size()-1;return true;}
   s.emergeAt=clock+cfg.getInt(s.heartlessSeek?"heartlessDelayTicks":"emergeDelayTicks");s.mode="EMERGING";p.setInvisible(true);p.setCollidable(false);return true;
  }
  if(clock<s.mawUntil)endMaw(s);s.seekBranch=branch;s.heartlessSeek=!s.life.intact();s.seekUntil=clock+cfg.getInt("seekTicks");s.path.clear();s.path.add(s.at.clone());s.direction=p.getLocation().getDirection().setY(0).normalize();s.priority=null;
  if(s.heartlessSeek){s.priority=priority(p,p.getLocation(),true);if(s.priority==null)return false;s.at=s.priority.getLocation().clone();s.mode="HEARTLESS_SEEK";s.emergeAt=clock+cfg.getInt("heartlessDelayTicks");p.setInvisible(true);p.setCollidable(false);s.mode="EMERGING";}
  else{s.mode="SEEK";spawn(s);}return true;
 }
 private LivingEntity priority(Player p,Location at,boolean playersOnly){return KitSupport.enemies(p,at,cfg.getDouble("lockRadius")).stream().filter(e->!playersOnly||e instanceof Player).max(Comparator.comparingDouble(e->e.getHealth()/KitAttributes.maxHealth(e))).orElse(null);}
 boolean peek(Player p){var s=state(p);s.peekUntil=clock+cfg.getInt("frightTicks");plugin.kitGlow().acquire(p,"peekaboo");Location centre=p.getLocation();String mode=s.mode;
  if(mode.equals("SEEK")||mode.equals("WOOD")){s.mode="RUSH";s.routeIndex=0;return true;}
  for(var enemy:KitSupport.enemies(p,centre,cfg.getDouble(mode.equals("MAW")?"mawPeekRadius":"peekRadius"))){Vector offset=enemy.getLocation().toVector().subtract(centre.toVector());if(!mode.equals("MAW")&&!AbilityTargeting.inCone(offset.getX(),offset.getZ(),p.getLocation().getDirection().getX(),p.getLocation().getDirection().getZ(),cfg.getDouble("peekRadius"),cfg.getDouble("coneDot")))continue;fright(s,enemy);if(mode.equals("MAW"))KitSupport.push(plugin,enemy,offset.multiply(-1).normalize().multiply(cfg.getDouble("pullSpeed")));}return true;
 }
 private void fright(State s,LivingEntity enemy){s.peekUntil=clock+cfg.getInt("frightTicks");plugin.forced().apply(ForcedMotion.Kind.FRIGHTENED,enemy,()->s.player.getLocation(),cfg.getInt("frightTicks"));innate(s);s.frightSeen.put(enemy.getUniqueId(),(long)Bukkit.getCurrentTick()+cfg.getInt("frightTicks"));}
 private void innate(State s){KitSupport.heal(plugin,s.player,cfg.getDouble("frightHeal"));s.life.heal(cfg.getDouble("heartHeal"));label(s);}
 private boolean wood(Location at){return Tag.LOGS.isTagged(at.getBlock().getType())||Tag.PLANKS.isTagged(at.getBlock().getType());}
 void tick(){clock++;if(plugin.inputs()==null)return;for(var p:Bukkit.getOnlinePlayers())if(KitSupport.has(plugin,p,"hollow")&&plugin.passives().has(p,"hollowed_heart"))state(p);
  for(var s:List.copyOf(states.values())){Player p=s.player;if(!KitSupport.has(plugin,p,"hollow")){forget(p);continue;}if(clock>=s.peekUntil&&!s.mode.equals("RUSH"))plugin.kitGlow().release(p,"peekaboo");s.life.capacity(plugin.effectiveMaxHealth(p));if(s.life.advance(p.getHealth()>=KitAttributes.maxHealth(p)-.001)){s.mode="FOLLOW";s.at=p.getLocation().clone().add(1,0,0);spawn(s);}if(!s.life.intact())hideHeart(s);else if(!s.mode.equals("MAW"))spawn(s);
   for(var enemy:KitSupport.enemies(p,p.getLocation(),cfg.getDouble("innateRadius"))){if(plugin.forced().kind(enemy)!=ForcedMotion.Kind.FRIGHTENED)continue;long expiry=Bukkit.getCurrentTick()+plugin.forced().remaining(enemy);if(expiry>s.frightSeen.getOrDefault(enemy.getUniqueId(),0L)+1){innate(s);s.frightSeen.put(enemy.getUniqueId(),expiry);}}
   if(clock>=s.mawUntil&&s.mode.equals("MAW"))endMaw(s);
   if(s.mode.equals("MAW")){double size=s.life.intact()?cfg.getDouble("mawScale"):cfg.getDouble("heartlessScale");if(s.mawBranch.equals("soresight"))size+=s.attackers.size()*cfg.getDouble("attentionScale");KitAttributes.set(p,Attribute.SCALE,new NamespacedKey(plugin,"hollow_maw"),Math.min(cfg.getDouble("maxScale"),size)-1,AttributeModifier.Operation.ADD_SCALAR);
    if(s.life.intact())KitSupport.potion(p,PotionEffectType.RESISTANCE,6,cfg.getInt("resistanceLevel"));if(!s.life.intact())KitSupport.potion(p,PotionEffectType.SLOWNESS,6,0);
    if(s.carried!=null){if(s.carried.isDead())s.carried=null;else{s.carried.teleport(p.getLocation());plugin.stun().stun(s.carried,3);}}
    if(clock>=s.thrashAt){s.thrashAt=clock+cfg.getInt("thrashInterval");for(var enemy:KitSupport.enemies(p,p.getLocation(),cfg.getDouble("cavityRadius")*size)){AbilityDamage.deal(enemy,cfg.getDouble("thrashDamage")*(s.life.intact()?1:cfg.getDouble("heartlessStrength")),p);s.life.heal(cfg.getDouble("thrashHeal"));if(s.mawBranch.equals("mangling_branch")&&s.carried==null){s.carried=enemy;plugin.stun().stun(enemy,3);}if(s.mawBranch.equals("eerie_resonance"))for(var ally:AbilityTargeting.within(p,p.getLocation(),4,4,4,AbilityTargeting.Relation.ALLY,false))KitSupport.heal(plugin,ally,cfg.getDouble("allyHeal"));}}
   }
   if(s.mode.equals("RETURN")){if(s.routeIndex<0){moveHeart(s,p.getLocation());s.mode="FOLLOW";s.seekUntil=0;}else{Location at=s.path.get(s.routeIndex--);moveHeart(s,at);for(var enemy:KitSupport.enemies(p,at,cfg.getDouble("dragRadius"))){Vector delta=at.toVector().subtract(enemy.getLocation().toVector());if(delta.lengthSquared()>.01)KitSupport.push(plugin,enemy,delta.normalize().multiply(cfg.getDouble("dragSpeed")));}}}
   if(s.mode.equals("RUSH")){if(plugin.stun().movementRefused(p)||s.routeIndex>=s.path.size()){s.mode="PARKED";s.seekUntil=0;}else{Location at=s.path.get(s.routeIndex);s.routeIndex+=3;Location safe=KitSupport.safe(at,1);if(safe==null){s.mode="PARKED";s.seekUntil=0;}else{p.teleport(safe);for(var enemy:KitSupport.enemies(p,safe,cfg.getDouble("rushRadius")))if(!s.frightSeen.containsKey(enemy.getUniqueId())||s.frightSeen.get(enemy.getUniqueId())<Bukkit.getCurrentTick())fright(s,enemy);}}}
   if(s.mode.equals("EMERGING")&&clock>=s.emergeAt){Location destination=s.heartlessSeek&&s.priority!=null&&!s.priority.isDead()?s.priority.getLocation():s.at;Location safe=KitSupport.safe(destination,2);if(safe!=null&&!plugin.stun().movementRefused(p)){p.teleport(safe);if(s.seekBranch.equals("cursed_canopy"))for(var enemy:KitSupport.enemies(p,safe,cfg.getDouble("canopyRadius"))){KitSupport.potion(enemy,PotionEffectType.BLINDNESS,cfg.getInt("canopyTicks"),0);KitSupport.potion(enemy,PotionEffectType.WEAKNESS,cfg.getInt("canopyTicks"),0);}if(s.seekBranch.equals("creaking_boards")&&s.wooden!=null)for(var enemy:KitSupport.enemies(p,safe,cfg.getDouble("boardsRadius")))if(enemy instanceof Player&&wood(enemy.getLocation().clone().add(0,-1,0)))plugin.stun().stun(enemy,cfg.getInt("boardsStunTicks"));}p.setInvisible(false);p.setCollidable(true);s.mode="PARKED";s.seekUntil=0;}
   if(s.mode.equals("SEEK")){if(clock>=s.seekUntil){s.mode="PARKED";}else{LivingEntity chosen=priority(p,s.at,false);if(chosen!=null)s.priority=chosen;if(s.priority!=null&&!s.priority.isDead()&&s.priority.getWorld()==s.at.getWorld())s.direction=s.priority.getLocation().toVector().subtract(s.at.toVector()).setY(0).normalize();Location next=s.at.clone().add(s.direction.clone().multiply(cfg.getDouble("rollSpeed")));if(KitSupport.loaded(next)){if(!next.getBlock().isPassable()){Vector side=new Vector(-s.direction.getZ(),0,s.direction.getX());Location along=s.at.clone().add(side.multiply(cfg.getDouble("rollSpeed")));if(along.getBlock().isPassable())next=along;else next=s.at.clone();}for(int dy=1;dy>=-2;dy--){Location ground=next.clone().add(0,dy,0);if(KitSupport.standing(ground)){next=ground;break;}}moveHeart(s,next);s.path.add(next.clone());if(s.path.size()>cfg.getInt("pathLimit"))s.path.removeFirst();if(s.seekBranch.equals("creaking_boards")){for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){Location block=next.clone().add(x,0,z);if(wood(block)){s.wooden=block.clone();moveHeart(s,block);s.mode="WOOD";}}}}}}
   if(s.mode.equals("FOLLOW")&&s.life.intact()&&p.getWorld()==s.at.getWorld()){Vector delta=p.getLocation().toVector().subtract(s.at.toVector());if(delta.lengthSquared()>cfg.getDouble("followDistance")*cfg.getDouble("followDistance"))moveHeart(s,s.at.clone().add(delta.normalize().multiply(cfg.getDouble("followSpeed"))));}
   label(s);
  }
 }
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void damage(EntityDamageEvent e){
  State s=e.getEntity() instanceof Player p?states.get(p.getUniqueId()):states.values().stream().filter(v->v.heart!=null&&v.heart.equals(e.getEntity())).findFirst().orElse(null);if(s==null||e.getFinalDamage()<=0)return;if(s.mode.equals("WOOD")&&e.getCause()==EntityDamageEvent.DamageCause.SUFFOCATION&&e.getEntity().equals(s.heart)){e.setCancelled(true);return;}
  if(s.mode.equals("MAW")&&e.getEntity().equals(s.player)&&e instanceof EntityDamageByEntityEvent hit){Entity source=hit.getDamager();if(source instanceof Projectile projectile&&projectile.getShooter() instanceof Entity shooter)source=shooter;if(source instanceof LivingEntity&&!source.equals(s.player)&&!Targetability.allied(source,s.player))s.attackers.add(source.getUniqueId());}
  if(e instanceof EntityDamageByEntityEvent hit&&TeamDamage.authored(e.getCause())){Player source=TeamDamage.owner(hit.getDamager());if(source!=null&&Targetability.allied(s.player,source)&&(!e.getEntity().equals(s.player)||!source.equals(s.player))){e.setCancelled(true);return;}}
  if(!s.life.intact())return;double amount=e.getFinalDamage();if(e.getEntity() instanceof Player&&plugin.getConfig().getBoolean("features.vitalsScaling.enabled"))amount=Vitals.toEffective(amount,plugin.effectiveMaxHealth(s.player));
  s.life.absorb(amount,returnTicks(s.player));e.setCancelled(true);if(plugin.combatState()!=null){plugin.combatState().markAbilityActivation(s.player);if(e instanceof EntityDamageByEntityEvent hit)plugin.combatState().markAbilityActivation(hit.getDamager());}if(!s.life.intact()){hideHeart(s);if(!s.mode.equals("MAW"))s.mode="FOLLOW";}label(s);
 }
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void heartAttack(EntityDamageByEntityEvent e){Player owner=OwnedEntities.owner(e.getEntity());if(owner!=null&&Targetability.allied(e.getDamager(),owner)&&states.values().stream().anyMatch(s->e.getEntity().equals(s.heart)))e.setCancelled(true);}
 @EventHandler public void death(PlayerDeathEvent e){forget(e.getEntity());var d=plugin.data(e.getEntity());if(d!=null){d.classState.remove("hollow.heart");d.classState.remove("hollow.return");}}
 void forget(Player p){var s=states.remove(p.getUniqueId());if(s==null)return;var d=plugin.data(p);if(d!=null){d.classState.put("hollow.heart",Double.toString(s.life.health()));d.classState.put("hollow.return",Integer.toString(s.life.remaining()));}hideHeart(s);plugin.kitGlow().release(p,"peekaboo");KitAttributes.set(p,Attribute.SCALE,new NamespacedKey(plugin,"hollow_maw"),0,AttributeModifier.Operation.ADD_SCALAR);p.setInvisible(false);p.setCollidable(true);}
 List<String> hud(Player p){State s=state(p);return List.of(s.life.intact()?"Heart "+Math.round(s.life.health()*100)+"/"+Math.round(s.life.maximum()*100):"Heart returns "+((s.life.remaining()+19)/20)+"s");}
 void close(){for(var s:List.copyOf(states.values()))forget(s.player);}
}
