package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import java.util.*;

/** Contextual Strike or Raise; Field Work chooses exactly one at its physical impact. */
final class GraveyardShiftAbility implements Ability {
    private record Shot(Player player,ItemDisplay visual,Vector velocity,int until){}
    private final MobaPlugin plugin;private final ConfigurationSection config;private final SkeletonCrewState state;
    private final List<Shot> shots=new ArrayList<>();private final Map<UUID,Boolean> combat=new HashMap<>();private int clock;
    GraveyardShiftAbility(MobaPlugin plugin,ConfigurationSection config,SkeletonCrewState state){this.plugin=plugin;this.config=config;this.state=state;}
    public String id(){return "graveyard_shift";}
    public String displayName(){return "Graveyard Shift";}
    public long cooldownTicks(){return config.getLong("cooldownTicks");}
    public List<AbilityOutput> outputs(){return AbilityOutput.of(new AbilityOutput("strike",TargetForm.UNIT_ENTITY,InputForm.INSTANT,Recipients.ENEMIES),new AbilityOutput("raise",TargetForm.SELF,InputForm.INSTANT,Recipients.NONE));}
    public List<AbilityOutput> outputs(Player p,AbilityContext ctx){
        if("field_work".equals(ctx.branchFor(id())))return AbilityOutput.single("field_work",TargetForm.DIRECTION,InputForm.INSTANT,Recipients.ENEMIES);
        if("hard_hat_zone".equals(ctx.branchFor(id())))return AbilityOutput.of(new AbilityOutput("strike",TargetForm.UNIT_ENTITY,InputForm.INSTANT,Recipients.BOTH),new AbilityOutput("raise",TargetForm.SELF,InputForm.INSTANT,Recipients.NONE));return outputs();
    }
    public Map<String,String> branches(){return Map.of("field_work","Field Work","hard_hat_zone","Hard Hat Zone","labored_union","Labored Union");}
    public List<String> branchIds(){return List.of("field_work","hard_hat_zone","labored_union");}
    public Boolean combatActivation(Player p,AbilityContext ctx){return combat.remove(p.getUniqueId());}
    public boolean execute(Player p,AbilityContext ctx){
        String branch=ctx.branchFor(id());combat.put(p.getUniqueId(),false);
        if("field_work".equals(branch)){
            var visual=p.getWorld().spawn(p.getEyeLocation(),ItemDisplay.class,e->{e.setItemStack(new org.bukkit.inventory.ItemStack(Material.BONE));e.setPersistent(false);});
            shots.add(new Shot(p,visual,p.getEyeLocation().getDirection().multiply(config.getDouble("projectileSpeed")),clock+config.getInt("projectileTicks")));return true;
        }
        LivingEntity target=null;
        if(ctx.onEntity()){if(ctx.entity() instanceof LivingEntity living)target=living;else return false;}
        else if(!ctx.onBlock()){
            var hit=p.getWorld().rayTrace(p.getEyeLocation(),p.getEyeLocation().getDirection(),config.getDouble("strikeRange"),FluidCollisionMode.NEVER,true,.3,e->!e.equals(p)&&e instanceof LivingEntity);
            if(hit!=null&&hit.getHitEntity() instanceof LivingEntity living)target=living;
        }
        if(target!=null && (Targetability.status(target,p) || "hard_hat_zone".equals(branch)&&SkeletonCrewState.owned(target)&&Targetability.allied(target,p))){boolean struck=state.strike(p,target,branch);combat.put(p.getUniqueId(),struck);return struck;}
        return state.raise(p,p.getLocation());
    }
    public void tick(){
        clock++;state.tick();for(var shot:List.copyOf(shots)){
            if(clock>=shot.until()||!shot.player().isOnline()||shot.player().isDead()||shot.player().getWorld()!=shot.visual().getWorld()){remove(shot);continue;}
            Location from=shot.visual().getLocation();var hit=from.getWorld().rayTrace(from,shot.velocity(),shot.velocity().length(),FluidCollisionMode.NEVER,true,.2,e->e instanceof LivingEntity&&Targetability.status(e,shot.player()));
            if(hit!=null){if(hit.getHitEntity() instanceof LivingEntity living)state.strike(shot.player(),living,"field_work");
                else if(hit.getHitBlock()!=null&&hit.getHitBlock().getType().isSolid())state.raise(shot.player(),hit.getHitBlock().getLocation().add(0,1,0));remove(shot);}
            else{Location to=from.clone().add(shot.velocity());if(!to.getWorld().isChunkLoaded(to.getBlockX()>>4,to.getBlockZ()>>4)){remove(shot);continue;}shot.visual().teleport(to);to.getWorld().spawnParticle(Particle.SOUL,to,2,.1,.1,.1,0);}
        }
    }
    private void remove(Shot shot){shot.visual().remove();shots.remove(shot);}
    public void forget(Player p){for(var shot:List.copyOf(shots))if(shot.player().equals(p))remove(shot);combat.remove(p.getUniqueId());state.forget(p);}
    public void close(){for(var shot:List.copyOf(shots))remove(shot);combat.clear();state.close();}
}
