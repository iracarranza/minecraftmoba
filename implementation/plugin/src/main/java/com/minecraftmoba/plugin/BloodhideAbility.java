package com.minecraftmoba.plugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
final class BloodhideAbility implements Ability {
    private final WerewolfState state;private final ConfigurationSection config;
    BloodhideAbility(WerewolfState state,ConfigurationSection config){this.state=state;this.config=config;}
    public String id(){return "bloodhide";}
    public String displayName(){return "Bloodhide";}
    public long cooldownTicks(){return config.getLong("cooldownTicks");}
    public List<AbilityOutput> outputs(){return AbilityOutput.single("bloodhide", TargetForm.SELF, InputForm.INSTANT, Recipients.NONE, true);}
    public Map<String,String> branches(){return Map.of("brawlers_mind","Brawler's Mind","canine_instinct","Canine Instinct","rabid_frenzy","Rabid Frenzy");}
    public List<String> branchIds(){return List.of("brawlers_mind","canine_instinct","rabid_frenzy");}
    public boolean execute(Player p,AbilityContext ctx){return state.hide(p,ctx.branchFor(id()));}
    public boolean cooldownExemptRecast(Player p,AbilityContext ctx){return state.hiding(p)&&state.form(p)==Lycanthropy.Form.UNLEASHED&&"rabid_frenzy".equals(ctx.branchFor(id()));}
}
