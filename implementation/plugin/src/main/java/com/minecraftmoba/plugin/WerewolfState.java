package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.potion.*;
import java.util.*;

/** Shared form, defense and attack geometry. Health bonuses change capacity while keeping the twenty-point bar. */
final class WerewolfState implements Listener {
    private static final class Wolf {
        final Player player;final Lycanthropy model=new Lycanthropy();
        int hideUntil;double received,extraSize;String hideBranch;boolean frenzy,lifesteal;
        PotionEffect priorGlow;int glowAt;boolean glowing;
        Wolf(Player p){player=p;}
    }
    private final MobaPlugin plugin;final ConfigurationSection config;
    private final Map<UUID,Wolf> wolves=new HashMap<>(); private int clock;private boolean secondary;
    WerewolfState(MobaPlugin plugin){this.plugin=plugin;config=plugin.getConfig().getConfigurationSection("werewolf");}
    private Wolf wolf(Player p){return wolves.computeIfAbsent(p.getUniqueId(),id->new Wolf(p));}
    Lycanthropy.Form form(Player p){var w=wolves.get(p.getUniqueId());return w==null?Lycanthropy.Form.HUMAN:w.model.form();}
    double healthMultiplier(Player p){var w=wolves.get(p.getUniqueId());return w==null?1:multiplier(w);}
    private double multiplier(Wolf w){return w.model.form()==Lycanthropy.Form.HUMAN?1:1+Lycanthropy.bonus(plugin.data(w.player)==null?0:plugin.data(w.player).level)+(w.model.form()==Lycanthropy.Form.UNLEASHED?.10:0);}
    private NamespacedKey key(String s){return new NamespacedKey(plugin,"werewolf_"+s);}
    void tick(){
        clock++;
        if(plugin.passives()==null)return;
        for(var p:plugin.getServer().getOnlinePlayers())if(plugin.passives().has(p,"lycanthropy") && !p.isDead())wolf(p);
        for(var w:List.copyOf(wolves.values())) {
            Player p=w.player;
            if(!p.isOnline() || p.isDead() || !plugin.passives().has(p,"lycanthropy")){forget(p);continue;}
            double old=multiplier(w);var prior=w.model.form();
            int requirement=Lycanthropy.nightRequirement(p.getWorld().getTime(),config.getInt("nightTicks"),config.getInt("dayTicks"));
            boolean exhausted=w.model.tick(clock,plugin.combatState().inCombat(p),p.getHealth()/KitAttributes.maxHealth(p)<.70,requirement,60,200,200);
            if(prior!=w.model.form()) {
                transition(w,old);
                if(w.model.form()!=Lycanthropy.Form.UNLEASHED){w.lifesteal=false;restoreGlow(w);}
            }
            if(exhausted){p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,config.getInt("recoveryTicks"),config.getInt("recoverySlowness")));p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,config.getInt("recoveryTicks"),config.getInt("recoveryWeakness")));}
            if(clock>=w.hideUntil && w.extraSize!=0){w.extraSize=0;size(w);}
        }
    }
    private void transition(Wolf w,double old) {
        Player p=w.player;double current=p.getHealth(),next=multiplier(w);
        boolean scaled=plugin.getConfig().getBoolean("features.vitalsScaling.enabled");
        KitAttributes.set(p,Attribute.MAX_HEALTH,key("health"),scaled?0:next-1,AttributeModifier.Operation.ADD_SCALAR);
        // Prototype: preserve current effective HP, do not grant new HP merely for changing form.
        if(current>0)p.setHealth(Math.min(KitAttributes.maxHealth(p),scaled?current*old/next:current));
        size(w);p.sendActionBar(net.kyori.adventure.text.Component.text("Werewolf: "+w.model.form()));
        p.getWorld().spawnParticle(Particle.CLOUD,p.getLocation().add(0,1,0),12,.5,.6,.5,0);
    }
    private void size(Wolf w){KitAttributes.set(w.player,Attribute.SCALE,key("size"),multiplier(w)-1+w.extraSize,AttributeModifier.Operation.ADD_SCALAR);}
    boolean hide(Player p,String branch){
        Wolf w=wolf(p);
        if(clock<w.hideUntil){if(w.model.form()==Lycanthropy.Form.UNLEASHED && "rabid_frenzy".equals(branch)){w.frenzy=true;return true;}return false;}
        w.hideUntil=clock+60;w.received=0;w.extraSize=0;w.hideBranch=branch;w.frenzy=false;size(w);return true;
    }
    boolean hiding(Player p){var w=wolves.get(p.getUniqueId());return w!=null && clock<w.hideUntil;}
    boolean frenzyRecast(Player p,String branch){return hiding(p)&&form(p)==Lycanthropy.Form.UNLEASHED&&"rabid_frenzy".equals(branch)&&hide(p,branch);}
    boolean howl(Player p){
        Wolf w=wolf(p);var prior=w.model.form();double old=multiplier(w);
        for(var effect:List.copyOf(p.getActivePotionEffects()))if(effect.getType().getEffectCategory()==PotionEffectType.Category.HARMFUL)p.removePotionEffect(effect.getType());
        w.model.unleash(clock,200);if(prior!=w.model.form())transition(w,old);
        if(prior==Lycanthropy.Form.TRANSFORMED)p.setHealth(Math.min(KitAttributes.maxHealth(p),p.getHealth()+(KitAttributes.maxHealth(p)-p.getHealth())*.65));
        if(prior==Lycanthropy.Form.UNLEASHED)w.lifesteal=true;
        if(!w.glowing){w.priorGlow=p.getPotionEffect(PotionEffectType.GLOWING);w.glowAt=clock;w.glowing=true;}
        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,w.model.remaining(clock),0,false,false,true));
        p.getWorld().playSound(p.getLocation(),Sound.ENTITY_WOLF_GROWL,1, .7f);return true;
    }
    private void restoreGlow(Wolf w){
        if(!w.glowing)return;w.glowing=false;
        var current=w.player.getPotionEffect(PotionEffectType.GLOWING);
        if(current!=null && current.getAmplifier()==0 && current.getDuration()<=200){w.player.removePotionEffect(PotionEffectType.GLOWING);
            if(w.priorGlow!=null){int remaining=w.priorGlow.getDuration()-(clock-w.glowAt);if(remaining>0)w.player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,remaining,w.priorGlow.getAmplifier(),w.priorGlow.isAmbient(),w.priorGlow.hasParticles(),w.priorGlow.hasIcon()));}}
    }
    @EventHandler(priority=EventPriority.NORMAL,ignoreCancelled=true)
    public void defend(EntityDamageEvent e){
        if(!(e.getEntity() instanceof Player p))return;Wolf w=wolves.get(p.getUniqueId());if(w==null || clock>=w.hideUntil)return;
        var form=w.model.form();
        if(form==Lycanthropy.Form.HUMAN && "brawlers_mind".equals(w.hideBranch) && e instanceof EntityDamageByEntityEvent by && by.getDamager() instanceof Projectile){e.setCancelled(true);return;}
        if(form==Lycanthropy.Form.TRANSFORMED && "canine_instinct".equals(w.hideBranch) && e instanceof EntityDamageByEntityEvent by && by.isCritical())e.setDamage(e.getDamage()/1.5);
        if(form!=Lycanthropy.Form.UNLEASHED) {
            double reduction=form==Lycanthropy.Form.TRANSFORMED && w.received>config.getDouble("hideThreshold")*plugin.effectiveMaxHealth(p)?.40:.20;
            e.setDamage(e.getDamage()*(1-reduction));
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void projectile(ProjectileHitEvent e){if(e.getHitEntity() instanceof Player p){var w=wolves.get(p.getUniqueId());if(w!=null && clock<w.hideUntil && w.model.form()==Lycanthropy.Form.HUMAN && "brawlers_mind".equals(w.hideBranch))e.setCancelled(true);}}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void received(EntityDamageEvent e){
        if(!(e.getEntity() instanceof Player p))return;Wolf w=wolves.get(p.getUniqueId());if(w==null || clock>=w.hideUntil || e.getFinalDamage()<=0)return;
        double taken=plugin.getConfig().getBoolean("features.vitalsScaling.enabled")?Vitals.toEffective(e.getFinalDamage(),plugin.effectiveMaxHealth(p)):e.getFinalDamage();
        w.received+=taken;
        if(w.model.form()==Lycanthropy.Form.UNLEASHED){w.extraSize=Math.min(config.getDouble("sizeCap"),w.received/plugin.effectiveMaxHealth(p)*config.getDouble("sizePerHealth"));size(w);
            if(w.frenzy && e instanceof EntityDamageByEntityEvent by){LivingEntity attacker=by.getDamager() instanceof LivingEntity living?living:by.getDamager() instanceof Projectile shot && shot.getShooter() instanceof LivingEntity shooter?shooter:null;if(attacker!=null && Targetability.status(attacker,p)){
                w.frenzy=false;plugin.getServer().getScheduler().runTask(plugin,()->{if(p.isOnline()&&!p.isDead()&&wolves.get(p.getUniqueId())==w&&attacker.isValid())attacker.damage(weaponDamage(p)*config.getDouble("maulMultiplier"),p);});}}
        }
    }
    static double weaponDamage(Player p){return Math.max(1,p.getAttribute(Attribute.ATTACK_DAMAGE).getValue());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void dealt(EntityDamageByEntityEvent e){
        if(!(e.getDamager() instanceof Player p) || e.getFinalDamage()<=0 || !Targetability.status(e.getEntity(),p))return;
        Wolf w=wolves.get(p.getUniqueId());if(w==null)return;
        if(w.lifesteal && w.model.form()==Lycanthropy.Form.UNLEASHED){
            double effective=e.getEntity() instanceof Player victim && plugin.getConfig().getBoolean("features.vitalsScaling.enabled")?Vitals.toEffective(e.getFinalDamage(),plugin.effectiveMaxHealth(victim)):e.getFinalDamage();
            double heal=effective*config.getDouble("lifesteal");if(plugin.getConfig().getBoolean("features.vitalsScaling.enabled"))heal=Vitals.scaleHealing(heal,plugin.effectiveMaxHealth(p));
            final double amount=heal;plugin.getServer().getScheduler().runTask(plugin,()->{if(p.isOnline()&&!p.isDead()&&wolves.get(p.getUniqueId())==w&&w.model.form()==Lycanthropy.Form.UNLEASHED)ChefItems.heal(p,amount);});
        }
        if(!secondary && clock<w.hideUntil && w.model.form()==Lycanthropy.Form.UNLEASHED && w.extraSize>0 && e.getCause()==EntityDamageEvent.DamageCause.ENTITY_ATTACK){
            double damage=e.getDamage();
            if(e.getEntity() instanceof Player victim && plugin.getConfig().getBoolean("features.vitalsScaling.enabled"))
                damage=Vitals.toEffective(damage,plugin.effectiveMaxHealth(victim));
            final double sweepDamage=damage;
            // Monitor observes only; the actual swept attacks run next tick outside the event dispatch.
            plugin.getServer().getScheduler().runTask(plugin,()->{
                if(!p.isOnline() || p.isDead() || !hiding(p))return;secondary=true;
                try{for(var other:p.getNearbyEntities(2+w.extraSize,1.5,2+w.extraSize))if(other instanceof LivingEntity target && !other.equals(e.getEntity()) && Targetability.status(target,p))target.damage(sweepDamage,p);}finally{secondary=false;}
            });
        }
    }
    void close(){for(var w:List.copyOf(wolves.values()))forget(w.player);}
    void forget(Player p){Wolf w=wolves.get(p.getUniqueId());if(w==null)return;double old=multiplier(w);wolves.remove(p.getUniqueId());
        KitAttributes.set(p,Attribute.SCALE,key("size"),0,AttributeModifier.Operation.ADD_SCALAR);
        KitAttributes.set(p,Attribute.MAX_HEALTH,key("health"),0,AttributeModifier.Operation.ADD_SCALAR);
        if(!p.isDead() && p.getHealth()>0)p.setHealth(Math.min(KitAttributes.maxHealth(p),plugin.getConfig().getBoolean("features.vitalsScaling.enabled")?p.getHealth()*old:p.getHealth()));
        restoreGlow(w);
    }
}
